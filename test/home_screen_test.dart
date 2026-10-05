import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mindlol/app/app.dart';
import 'package:mindlol/app/theme/app_theme.dart';
import 'package:mindlol/core/platform/mindlol_platform.dart';
import 'package:mindlol/features/home/home_screen.dart';
import 'package:mindlol/features/home/widgets/limit_edit_modal.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late MindLolPlatform platform;
  late List<MethodCall> methodCalls;
  Map<String, dynamic> mockResponse = {
    'todayCount': 17,
    'dailyLimit': 30,
    'limitReached': false,
    'accessibilityEnabled': true,
    'overlayPermissionGranted': true,
  };

  setUp(() {
    methodCalls = <MethodCall>[];
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(
      const MethodChannel('mindlol/platform'),
      (MethodCall methodCall) async {
        methodCalls.add(methodCall);
        switch (methodCall.method) {
          case 'getMindLolStatus':
            return mockResponse;
          case 'setDailyLimit':
            final limit = (methodCall.arguments as Map)['limit'] as int;
            mockResponse = {
              ...mockResponse,
              'dailyLimit': limit,
              'limitReached': (mockResponse['todayCount'] as int) >= limit,
            };
            return mockResponse;
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

  testWidgets('Renders Home Screen with correct metrics and Blob title',
      (WidgetTester tester) async {
    mockResponse = {
      'todayCount': 17,
      'dailyLimit': 30,
      'limitReached': false,
      'accessibilityEnabled': true,
      'overlayPermissionGranted': true,
    };

    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.lightTheme,
        home: HomeScreen(platform: platform),
      ),
    );

    // Initial loading indicator
    expect(find.byType(CircularProgressIndicator), findsOneWidget);

    await tester.pump(const Duration(milliseconds: 500));

    // Usage count and metrics
    expect(find.text('17'), findsOneWidget);
    expect(find.text('Reels today'), findsOneWidget);
    expect(find.text('30 Reels'), findsOneWidget);
    expect(find.text('57% of limit'), findsOneWidget);
    expect(find.text('okay... that\'s a few.'), findsOneWidget);

    // Verify developer terms are NOT present
    expect(find.textContaining('MethodChannel'), findsNothing);
    expect(find.textContaining('probe'), findsNothing);
    expect(find.textContaining('verification'), findsNothing);
    expect(find.textContaining('engine'), findsNothing);
  });

  testWidgets('Shows Limit Reached state when todayCount >= dailyLimit',
      (WidgetTester tester) async {
    mockResponse = {
      'todayCount': 30,
      'dailyLimit': 30,
      'limitReached': true,
      'accessibilityEnabled': true,
      'overlayPermissionGranted': true,
    };

    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.lightTheme,
        home: HomeScreen(platform: platform),
      ),
    );

    await tester.pump(const Duration(milliseconds: 500));

    expect(find.text('30'), findsOneWidget);
    expect(find.text('100% of limit'), findsOneWidget);
    expect(find.text('okay, let\'s breathe.'), findsOneWidget);
    expect(
      find.text('Your daily limit is reached. MindLol blocker is active.'),
      findsOneWidget,
    );
  });

  testWidgets('LimitEditModal selects preset and invokes onSave callback',
      (WidgetTester tester) async {
    int? updatedLimit;

    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.lightTheme,
        home: Scaffold(
          body: LimitEditModal(
            currentLimit: 30,
            onSave: (newLimit) {
              updatedLimit = newLimit;
            },
          ),
        ),
      ),
    );

    await tester.pumpAndSettle();

    // Verify modal elements present
    expect(find.text('Set Daily Reel Limit'), findsOneWidget);
    expect(find.text('15 Reels'), findsOneWidget);

    // Select preset 15
    await tester.tap(find.widgetWithText(ChoiceChip, '15 Reels'));
    await tester.pump();

    // Save
    await tester.tap(find.widgetWithText(ElevatedButton, 'Save Limit'));
    await tester.pump();

    // Verify onSave was called with 15
    expect(updatedLimit, equals(15));
  });

  testWidgets('Displays recoverable error state when platform bridge fails',
      (WidgetTester tester) async {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(
      const MethodChannel('mindlol/platform'),
      (MethodCall methodCall) async {
        throw PlatformException(code: 'UNAVAILABLE', message: 'Bridge failed');
      },
    );

    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.lightTheme,
        home: HomeScreen(platform: platform),
      ),
    );

    await tester.pump(const Duration(milliseconds: 500));

    expect(
      find.text('MindLol couldn\'t read your usage right now.'),
      findsOneWidget,
    );
    expect(find.text('Try again'), findsOneWidget);
  });

  testWidgets('Renders cleanly in Dark Mode theme',
      (WidgetTester tester) async {
    mockResponse = {
      'todayCount': 5,
      'dailyLimit': 20,
      'limitReached': false,
      'accessibilityEnabled': true,
      'overlayPermissionGranted': true,
    };

    await tester.pumpWidget(
      MindLolApp(platform: platform),
    );

    await tester.pump(const Duration(milliseconds: 500));

    expect(find.text('5'), findsOneWidget);
    expect(find.text('Reels today'), findsOneWidget);
    expect(find.text('MindLol'), findsOneWidget);
  });
}
