# camera_connect

A Flutter plugin for connecting to IP cameras with TUTK IOTC support.

## Features

- Connect to IP cameras using TUTK IOTC protocol
- Real-time video streaming
- Audio support
- PTZ (Pan/Tilt/Zoom) controls
- Video recording capabilities
- Multi-camera support
- Cross-platform (Android & iOS)

## Getting Started

### Installation

Add this to your package's `pubspec.yaml` file:

```yaml
dependencies:
  camera_connect: ^0.0.1
```

### Usage

```dart
import 'package:camera_connect/camera_connect.dart';

// Initialize camera connection
final cameraConnect = CameraConnect();

// Connect to camera
await cameraConnect.connect(
  deviceId: 'your_device_id',
  username: 'admin',
  password: 'password',
);

// Start video stream
await cameraConnect.startVideoStream();

// Stop video stream
await cameraConnect.stopVideoStream();

// Disconnect
await cameraConnect.disconnect();
```

## Platform Support

- ✅ Android
- ✅ iOS

## Requirements

- Flutter: >=3.3.0
- Dart SDK: ^3.5.4

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Issues

If you encounter any issues, please report them on the [issue tracker](https://github.com/YOUR_ACTUAL_USERNAME/camera_connect/issues).

