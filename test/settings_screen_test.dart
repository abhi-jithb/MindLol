import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mindlol/app/theme/app_theme.dart';
import 'package:mindlol/core/platform/mindlol_platform.dart';
import 'package:mindlol/core/platform/mindlol_status.dart';
import 'package:mindlol/features/home/home_screen.dart';
import 'package:mindlol/features/settings/settings_screen.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late MindLolPlatform platform;
  late List<MethodCall> methodCalls;

  setUp(() {
    methodCalls = <MethodCall>[];
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(
      const MethodChannel('mindlol/platform'),
      (MethodCall methodCall) async {
        methodCalls.add(methodCall);
        switch (methodCall.method) {
          case 'getMindLolStatus':
            return {
              'todayCount': 5,
              'dailyLimit': 20,
              'limitReached': false,
              'accessibilityEnabled': true,
              'overlayPermissionGranted': true,
              'instagramEnabled': true,
              'youtubeEnabled': true,
              'blockingMode': 'BLOCK_AT_LIMIT',
            };
          case 'setAppConfiguration':
            final insta = (methodCall.arguments as Map)['instagramEnabled'] as bool?;
            final yt = (methodCall.arguments as Map)['youtubeEnabled'] as bool?;
            return {
              'todayCount': 5,
              'dailyLimit': 20,
              'limitReached': false,
              'accessibilityEnabled': true,
              'overlayPermissionGranted': true,
              'instagramEnabled': insta ?? true,
              'youtubeEnabled': yt ?? true,
              'blockingMode': 'BLOCK_AT_LIMIT',
            };
          case 'getBlockingMode':
            return 'BLOCK_AT_LIMIT';
          default:
            return null;
        }
      },
    );

    platform = MindLolPlatform();
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(
      const MethodChannel('mindlol/platform'),
      null,
    );
  });

  testWidgets('SettingsScreen renders configuration sections and values',
      (WidgetTester tester) async {
    const status = MindLolStatus(
      todayCount: 5,
      dailyLimit: 20,
      limitReached: false,
      accessibilityEnabled: true,
      overlayPermissionGranted: true,
      instagramEnabled: true,
      youtubeEnabled: true,
      blockingMode: 'BLOCK_AT_LIMIT',
    );

    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.lightTheme,
        home: SettingsScreen(
          platform: platform,
          currentStatus: status,
        ),
      ),
    );

    await tester.pumpAndSettle();

    expect(find.text('Settings'), findsOneWidget);
    expect(find.text('USAGE'), findsOneWidget);
    expect(find.text('SUPPORTED APPS'), findsOneWidget);
    expect(find.text('20 Reels'), findsOneWidget);
    expect(find.text('Instagram Reels'), findsOneWidget);
    expect(find.text('YouTube Shorts'), findsOneWidget);
    expect(find.text('Blocking Mode'), findsOneWidget);
    expect(find.text('Privacy Disclosure'), findsOneWidget);
  });

  testWidgets('Toggling app configuration invokes setAppConfiguration on platform',
      (WidgetTester tester) async {
    const status = MindLolStatus(
      todayCount: 5,
      dailyLimit: 20,
      limitReached: false,
      accessibilityEnabled: true,
      overlayPermissionGranted: true,
      instagramEnabled: true,
      youtubeEnabled: true,
      blockingMode: 'BLOCK_AT_LIMIT',
    );

    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.lightTheme,
        home: SettingsScreen(
          platform: platform,
          currentStatus: status,
        ),
      ),
    );

    await tester.pumpAndSettle();

    final switches = find.byType(Switch);
    expect(switches, findsNWidgets(2));

    // Toggle Instagram
    await tester.tap(switches.first);
    await tester.pumpAndSettle();

    expect(
      methodCalls.any((call) => call.method == 'setAppConfiguration'),
      isTrue,
    );
  });

  testWidgets('Navigating from HomeScreen to SettingsScreen and back',
      (WidgetTester tester) async {
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.lightTheme,
        home: HomeScreen(platform: platform),
      ),
    );

    await tester.pump(const Duration(milliseconds: 500));

    // Tap Settings icon in AppBar
    final settingsIcon = find.byIcon(Icons.settings_outlined);
    expect(settingsIcon, findsOneWidget);
    await tester.tap(settingsIcon);
    await tester.pumpAndSettle();

    // Verify Settings screen visible
    expect(find.text('Settings'), findsOneWidget);
    expect(find.text('USAGE'), findsOneWidget);

    // Tap back button
    final backButton = find.byType(BackButton);
    expect(backButton, findsOneWidget);
    await tester.tap(backButton);
    await tester.pump(const Duration(milliseconds: 500));

    // Verify back on HomeScreen
    expect(find.text('MindLol'), findsOneWidget);
  });
}
