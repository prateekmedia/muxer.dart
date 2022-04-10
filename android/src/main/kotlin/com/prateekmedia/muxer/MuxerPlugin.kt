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
import java.io.IOException
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
) {
  lateinit var video: Movie
  try {
      video = MovieCreator.build(videoPath)
  } catch (e: RuntimeException) {
      e.printStackTrace()
      result.error("Runtime Exception", "Runtime error", "Mux failed")
      return
  } catch (e: IOException) {
      e.printStackTrace()
      result.error("IO Exception", "Failed while parsing video", "Mux failed")
      return
  }

  lateinit var audio: Movie
  try {
      audio = MovieCreator.build(audioPath)
  } catch (e: IOException) {
      e.printStackTrace()
      result.error("Runtime Exception", "Runtime error", "Mux failed")
      return
  } catch (e: NullPointerException) {
      e.printStackTrace()
      result.error("IO Exception", "Failed while parsing audio", "Mux failed")
      return
  }

  val audioTrack = audio.getTracks().get(0)
  video.addTrack(audioTrack)

  val out = DefaultMp4Builder().build(video)

  lateinit var fos: FileOutputStream

  try {
      fos = FileOutputStream(outputPath)
  } catch (e: FileNotFoundException) {
      e.printStackTrace()

      result.error("Output failed!", "File not found", "Mux failed")
      return
  }

  val byteBufferByteChannel = BufferedWritableFileByteChannel(fos)

  try {
      out.writeContainer(byteBufferByteChannel)
      byteBufferByteChannel.close()
      fos.close()

      result.success("done")
  } catch (e: IOException) {
      e.printStackTrace()
      result.error("Output failed!", "Failed while muxing files", "Mux failed")
      return
  }
}

private class BufferedWritableFileByteChannel private constructor(private val outputStream: OutputStream) :
WritableByteChannel {
  private var isOpen = true
  private val byteBuffer: ByteBuffer
  private val rawBuffer = ByteArray(BUFFER_CAPACITY)

  init {
      this.byteBuffer = ByteBuffer.wrap(rawBuffer)
  }

  @Throws(IOException::class)
  override fun write(inputBuffer: ByteBuffer): Int {
      val inputBytes = inputBuffer.remaining()

      if (inputBytes > byteBuffer.remaining()) {
          dumpToFile()
          byteBuffer.clear()

          if (inputBytes > byteBuffer.remaining()) {
              throw BufferOverflowException()
          }
      }

      byteBuffer.put(inputBuffer)

      return inputBytes
  }

  override fun isOpen(): Boolean {
      return isOpen
  }

  @Throws(IOException::class)
  override fun close() {
      dumpToFile()
      isOpen = false
  }

  private fun dumpToFile() {
      try {
          outputStream.write(rawBuffer, 0, byteBuffer.position())
      } catch (e: IOException) {
          throw RuntimeException(e)
      }

  }

  companion object {
      private val BUFFER_CAPACITY = 1000000
  }
}