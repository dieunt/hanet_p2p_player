import 'package:flutter_test/flutter_test.dart';
import 'package:camera_connect/camera_connect.dart';
import 'package:camera_connect/camera_connect_platform_interface.dart';
import 'package:camera_connect/camera_connect_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockCameraConnectPlatform
    with MockPlatformInterfaceMixin
    implements CameraConnectPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final CameraConnectPlatform initialPlatform = CameraConnectPlatform.instance;

  test('$MethodChannelCameraConnect is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelCameraConnect>());
  });

  test('getPlatformVersion', () async {
    CameraConnect cameraConnectPlugin = CameraConnect();
    MockCameraConnectPlatform fakePlatform = MockCameraConnectPlatform();
    CameraConnectPlatform.instance = fakePlatform;

    expect(await cameraConnectPlugin.getPlatformVersion(), '42');
  });
}
