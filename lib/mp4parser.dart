import 'dart:async';

import 'package:flutter/services.dart';

class Mp4parser {
  static const MethodChannel _channel = MethodChannel('mp4parser');

  static Future<String?> muxAudioVideo(
      {required String videoPath, required String audioPath}) async {
    final String? version = await _channel.invokeMethod('muxAudioVideo', {
      "videoPath": videoPath,
      "audioPath": audioPath,
    });
    return version;
  }
}
