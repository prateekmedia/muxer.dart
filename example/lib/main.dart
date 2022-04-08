import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:muxer/muxer.dart';

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
          ) ??
          'Unknown error';
    } on PlatformException {
      _result = 'Failed to perform muxing.';
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
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  ElevatedButton(
                    onPressed: () async {
                      var result = await FilePicker.platform
                          .pickFiles(type: FileType.video);
                      if (result != null) {
                        setState(() => videoPath = result.files.first.path);
                      }
                    },
                    child: const Text("Select Video File"),
                  ),
                  const SizedBox(width: 20),
                  ElevatedButton(
                    onPressed: () async {
                      var result = await FilePicker.platform
                          .pickFiles(type: FileType.audio);
                      if (result != null) {
                        setState(() => audioPath = result.files.first.path);
                      }
                    },
                    child: const Text("Select Audio File"),
                  ),
                ],
              ),
              const SizedBox(height: 20),
              OutlinedButton(
                onPressed: () {
                  if (videoPath != null && audioPath != null) {
                    muxAudioVideo();
                  }
                },
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
