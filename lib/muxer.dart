import 'dart:async';

import 'package:flutter/services.dart';

class Muxer {
  static const MethodChannel _channel = MethodChannel('muxer');

  static Future<String?> muxAudioVideo({
    required String videoPath,
    required String audioPath,
    String? outputPath,
  }) async {
    final String? result = await _channel.invokeMethod(
      'muxAudioVideo',
      {
        "videoPath": videoPath,
        "audioPath": audioPath,
        "outputPath": outputPath ?? videoPath,
      },
    );
    return result;
  }
}
