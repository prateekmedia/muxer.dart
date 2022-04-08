import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mp4parser/mp4parser.dart';

void main() {
  const MethodChannel channel = MethodChannel('mp4parser');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('muxAudioVideo', () async {
    expect(await Mp4parser.muxAudioVideo(videoPath: '', audioPath: ''), 'done');
  });
}
