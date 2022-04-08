import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:muxer/muxer.dart';

void main() {
  const MethodChannel channel = MethodChannel('muxer');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getPlatformVersion', () async {
    expect(await Muxer.muxAudioVideo(audioPath: '', videoPath: ''), '42');
  });
}
