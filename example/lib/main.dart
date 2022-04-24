import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';

import 'package:flutter/services.dart';
import 'package:muxer/muxer.dart';
import 'package:path/path.dart' as path;

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String? videoPath;
  String? audioPath;
  String? outputPath;
  String? result;

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> muxAudioVideo() async {
    late String _result;
    // Platform messages may fail, so we use a try/catch PlatformException.
    // We also handle the message potentially returning null.
    try {
      _result = await Muxer.muxAudioVideo(
            audioPath: audioPath!,
            videoPath: videoPath!,
            outputPath: '${outputPath!}/muxed.mp4',
          ) ??
          'Unknown error';
    } catch (error) {
      _result = '$error';
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;
    setState(() => result = _result);
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: ListView(
            padding: const EdgeInsets.symmetric(horizontal: 20),
            shrinkWrap: true,
            children: [
              ElevatedButton(
                onPressed: () async {
                  var result =
                      await FilePicker.platform.pickFiles(type: FileType.video);
                  if (result != null) {
                    setState(() => videoPath = result.files.first.path);
                  }
                },
                child: Text(
                  videoPath != null
                      ? path.basename(videoPath!)
                      : "Select Video File",
                  overflow: TextOverflow.ellipsis,
                ),
              ),
              const SizedBox(height: 5),
              ElevatedButton(
                onPressed: () async {
                  var result =
                      await FilePicker.platform.pickFiles(type: FileType.audio);
                  if (result != null) {
                    setState(() => audioPath = result.files.first.path);
                  }
                },
                child: Text(
                  audioPath != null
                      ? path.basename(audioPath!)
                      : "Select Audio File",
                  overflow: TextOverflow.ellipsis,
                ),
              ),
              const SizedBox(height: 5),
              ElevatedButton(
                onPressed: () async {
                  var result = await FilePicker.platform.getDirectoryPath();
                  if (result != null) {
                    setState(() => outputPath = result);
                  }
                },
                child: Text(
                  outputPath != null
                      ? path.dirname(outputPath!)
                      : "Select Output Directory",
                  overflow: TextOverflow.ellipsis,
                ),
              ),
              const SizedBox(height: 20),
              OutlinedButton(
                onPressed:
                    videoPath != null && audioPath != null && outputPath != null
                        ? muxAudioVideo
                        : null,
                child: const Text("Start Muxing"),
              ),
              const SizedBox(height: 20),
              Text(result ?? "NOTE: Selected video will be overwritten!"),
            ],
          ),
        ),
      ),
    );
  }
}
