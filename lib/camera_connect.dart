import 'package:camera_connect/channel_app.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';

class CameraConnect extends StatelessWidget {
  const CameraConnect({
    super.key,
    required this.uuid,
    required this.pass,
    this.width = 1280,
    this.height = 720,
  });

  final String uuid;
  final String pass;
  final int width;
  final int height;

  @override
  Widget build(BuildContext context) {
    if (defaultTargetPlatform == TargetPlatform.android) {
      return AndroidView(
        viewType: ChannelApp.channelCameraMulti,
        creationParams: {
          'uuid': uuid,
          'pass': pass,
          'width': width,
          'height': height,
        },
        creationParamsCodec: const StandardMessageCodec(),
      );
    } else {
      return const Text('CameraConnectView is only supported on Android');
    }
  }
}
