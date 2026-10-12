import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mindlol/core/platform/mindlol_platform.dart';
import 'package:mindlol/core/platform/mindlol_status.dart';
import 'package:mindlol/features/home/widgets/permission_banner.dart';

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
          case 'openAccessibilitySettings':
          case 'openOverlaySettings':
            return true;
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

  group('PermissionBanner Accuracy & Honesty Tests', () {
    testWidgets(
        'When Accessibility is missing, informs user that counting is inactive',
        (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: PermissionBanner(
              accessibilityEnabled: false,
              overlayPermissionGranted: true,
              platform: platform,
            ),
          ),
        ),
      );

      expect(
        find.text('Accessibility access is needed to count short-form videos.'),
        findsOneWidget,
      );
      expect(find.text('Enable'), findsOneWidget);

      await tester.tap(find.text('Enable'));
      await tester.pump();

      expect(
        methodCalls.any((call) => call.method == 'openAccessibilitySettings'),
        isTrue,
      );
    });

    testWidgets(
        'When Overlay is missing but Accessibility is granted, confirms counting is active',
        (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: PermissionBanner(
              accessibilityEnabled: true,
              overlayPermissionGranted: false,
              platform: platform,
            ),
          ),
        ),
      );

      expect(
        find.text(
          'Counting is active. Enable overlay permission to block feeds when your limit is reached.',
        ),
        findsOneWidget,
      );
      expect(find.text('Enable'), findsOneWidget);

      await tester.tap(find.text('Enable'));
      await tester.pump();

      expect(
        methodCalls.any((call) => call.method == 'openOverlaySettings'),
        isTrue,
      );
    });

    testWidgets(
        'When both permissions are granted and health is normal, banner is hidden',
        (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: PermissionBanner(
              accessibilityEnabled: true,
              overlayPermissionGranted: true,
              trackingHealthState: TrackingHealthState.tracking,
              platform: platform,
            ),
          ),
        ),
      );

      expect(find.byType(Container), findsNothing);
    });

    testWidgets(
        'When tracking is uncertain, informs user that count is preserved',
        (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: PermissionBanner(
              accessibilityEnabled: true,
              overlayPermissionGranted: true,
              trackingHealthState: TrackingHealthState.trackingUncertain,
              trackingHealthMessage:
                  'Detection uncertain on this screen. Your count is preserved.',
              platform: platform,
            ),
          ),
        ),
      );

      expect(
        find.text(
          'Detection uncertain on this screen. Your count is preserved.',
        ),
        findsOneWidget,
      );
    });

    testWidgets(
        'When tracking is paused due to disabled apps, displays clean paused notice',
        (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: PermissionBanner(
              accessibilityEnabled: true,
              overlayPermissionGranted: true,
              trackingHealthState: TrackingHealthState.trackingPaused,
              trackingHealthMessage:
                  'Tracking is paused because apps are disabled in Settings.',
              platform: platform,
            ),
          ),
        ),
      );

      expect(
        find.text(
          'Tracking is paused because apps are disabled in Settings.',
        ),
        findsOneWidget,
      );
    });
  });
}
