import 'package:flutter/material.dart';
import '../core/platform/mindlol_platform.dart';
import '../features/home/home_screen.dart';
import 'theme/app_theme.dart';

class MindLolApp extends StatelessWidget {
  final MindLolPlatform platform;

  const MindLolApp({super.key, required this.platform});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'MindLol',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.lightTheme,
      darkTheme: AppTheme.darkTheme,
      themeMode: ThemeMode.system,
      home: HomeScreen(platform: platform),
    );
  }
}
