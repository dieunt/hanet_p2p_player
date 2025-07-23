import 'package:camera_connect/channel_app.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';

class CameraPlayer extends StatelessWidget {
  const CameraPlayer({
    super.key,
    required this.uuid,
    required this.pass,
    this.width = 1280,
    this.height = 720,
    this.mic = 1,
    this.sound = 1,
    this.type = 0,
  });

  final String uuid;
  final String pass;
  final int width;
  final int height;
  final int mic;
  final int sound;
  final int type;

  @override
  Widget build(BuildContext context) {
    if (defaultTargetPlatform == TargetPlatform.android) {
      return AndroidView(
        viewType: ChannelApp.viewPlayer,
        creationParams: {
          'uuid': uuid,
          'pass': pass,
          'width': width,
          'height': height,
          'mic': mic,
          'sound': sound,
          'type': type,
        },
        creationParamsCodec: const StandardMessageCodec(),
      );
    } else {
      return const Text('CameraConnectView is only supported on Android');
    }
  }
}
