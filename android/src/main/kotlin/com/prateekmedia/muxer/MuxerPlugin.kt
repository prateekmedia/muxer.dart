package com.prateekmedia.muxer

import androidx.annotation.NonNull

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

import org.mp4parser.muxer.FileRandomAccessSourceImpl
import org.mp4parser.muxer.Movie
import org.mp4parser.muxer.builder.DefaultMp4Builder
import org.mp4parser.muxer.container.mp4.MovieCreator
import java.io.File
import java.nio.channels.FileChannel
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile

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

        muxAudioVideo(
          videoPath,
          audioPath,
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
    videoFile: String,
    audioFile: String,
    result: Result
) {
  try {
    val movie = MovieCreator.build(videoFile)
    val audio = MovieCreator.build(audioFile)

    val audioTrack = audio.tracks.first()
    movie.addTrack(audioTrack)

    val outContainer = DefaultMp4Builder().build(movie)

    var fileChannel: FileChannel = RandomAccessFile(File(videoFile), "rw").getChannel()
    outContainer.writeContainer(fileChannel)
    fileChannel.close();

    result.success("done")
  } catch (e: Exception) {
      result.error("1", "Something went wrong!", "Mux failed");
  }
}