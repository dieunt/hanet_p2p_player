import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:camera_connect/camera_connect.dart';

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
        body: CameraConnect(
          uuid: 'H23R9K94ZULVDVCF111A',
          pass: 'INcFwR',
          height: 700,
          width: 1000,
        ),
      ),
    );
  }
}
