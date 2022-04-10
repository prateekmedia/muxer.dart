package com.prateekmedia.muxer

import androidx.annotation.NonNull
import android.util.Log

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

import io.reactivex.Flowable.interval
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers

import org.mp4parser.muxer.FileRandomAccessSourceImpl
import org.mp4parser.muxer.Movie
import org.mp4parser.muxer.builder.DefaultMp4Builder
import org.mp4parser.muxer.container.mp4.MovieCreator

import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit
import java.nio.channels.FileChannel
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileNotFoundException

/** MuxerPlugin */
class MuxerPlugin: FlutterPlugin, MethodCallHandler {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private lateinit var channel : MethodChannel

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "muxer")
    channel.setMethodCallHandler(this)
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    when (call.method) {
      "muxAudioVideo" -> {
        val videoPath = call.argument<Any>("videoPath").toString()
        val audioPath = call.argument<Any>("audioPath").toString()
        val outputPath = call.argument<Any>("outputPath").toString()

        muxAudioVideo(
          videoPath,
          audioPath,
          outputPath,
          result
        )
      }
      else ->result.notImplemented()
    }
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }
}

fun muxAudioVideo(
    videoPath: String,
    audioPath: String,
    outputPath: String,
    result: Result
) : Observable<ProgressResult<File>> {
  return Single.fromCallable { File(outputPath).apply { createNewFile() } }
      .flatMapObservable { output ->
  lateinit var video: Movie
  try {
      video = MovieCreator.build(videoPath)
  } catch (e: RuntimeException) {
      e.printStackTrace()
      result.error("Runtime Exception", "Runtime error", "Mux failed")
  } catch (e: IOException) {
      e.printStackTrace()
      result.error("IO Exception", "Failed while parsing video", "Mux failed")
  }

  lateinit var audio: Movie
  try {
      audio = MovieCreator.build(audioPath)
  } catch (e: IOException) {
      e.printStackTrace()
      result.error("Runtime Exception", "Runtime error", "Mux failed")
  } catch (e: NullPointerException) {
      e.printStackTrace()
      result.error("IO Exception", "Failed while parsing audio", "Mux failed")
  }

  val audioTrack = audio.getTracks().get(0)
  video.addTrack(audioTrack)

  val finalContainer = DefaultMp4Builder().build(video)

  val finalStream = RandomAccessFile(output.absolutePath, "rw").channel

  var threadException: Boolean = false

  // Make the call to write the file on a separate thread from the progress check
  val writeFileThread = Thread(
    Runnable {
      try {
        finalContainer.writeContainer(finalStream)
      } catch (e: InterruptedException) {
        result.error("Output failed!", "Muxing Interuppted", "Mux failed")
        threadException = true
      } catch (e: IOException) {
        Log.e("muxAudioVideo", "IOException within video mux thread")
        if (output.usableSpace == 0L) {
          result.error("Output failed!", "No storage remaining.", "Mux failed")
        } else {
          result.error("Output failed!", e.toString(), "Mux failed")
        }
        threadException = true
      }
    }
  )
  writeFileThread.setUncaughtExceptionHandler { _, e -> threadException = true }

  val finalVideoSize = finalContainer.boxes.map { it.size }.sum()

  Observable.interval(200L, TimeUnit.MILLISECONDS)
    .observeOn(Schedulers.computation())
    .map {
      val currentOutputSize = if (output.exists()) output.length() else 0
      val progress = currentOutputSize / finalVideoSize.toFloat()

      ProgressResult(output, progress)
    }
    .takeUntil {
      val completionProgress = it.progress ?: 0.0f
      completionProgress >= 1f || threadException
    }
    .observeOn(Schedulers.io())
    .doOnSubscribe {
      writeFileThread.start()
    }
    .doFinally {
      writeFileThread.interrupt()
      finalStream.close()
    }

    result.success("done")
  }
    .subscribeOn(Schedulers.io())
    .onErrorResumeNext { error: Throwable ->
    val outputFile = File(outputPath)
    val mappedError = if (outputFile.usableSpace == 0L) {
      OutOfStorageException(
                        "No storage remaining to rotate video",
                        error
                    )
    } else {
      error
    }
    threadException = true
    Observable.error<ProgressResult<File>>(mappedError)
    result.error("Output failed!", e.toString(), "Mux failed")
  }
}

data class ProgressResult<T>(val item: T, val progress: Float?)

class OutOfStorageException : IOException {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(cause: Throwable?) : super(cause)
}