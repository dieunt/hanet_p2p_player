import 'package:camera_connect/camera_player.dart';
import 'package:flutter/material.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  @override
  Widget build(BuildContext context) {
    return const MaterialApp(
      home: Scaffold(
        body: CameraPlayer(
          uuid: 'H23R9K94ZULVDVCF111A',
          pass: 'Js2nvQ',
          height: 700,
          width: 1000,
          mic: 1,
          sound: 1,
          type: 0,
        ),
      ),
    );
  }
}
