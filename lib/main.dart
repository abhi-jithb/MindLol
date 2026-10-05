import 'package:flutter/material.dart';
import 'app/app.dart';
import 'core/platform/mindlol_platform.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  final platform = MindLolPlatform();
  runApp(MindLolApp(platform: platform));
}
