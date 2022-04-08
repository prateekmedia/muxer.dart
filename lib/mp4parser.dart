
import 'dart:async';

import 'package:flutter/services.dart';

class Mp4parser {
  static const MethodChannel _channel = MethodChannel('mp4parser');

  static Future<String?> get platformVersion async {
    final String? version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }
}
