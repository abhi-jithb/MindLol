import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mindlol/app/app.dart';
import 'package:mindlol/core/platform/mindlol_platform.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('MindLolApp smoke test renders MindLol home experience',
      (WidgetTester tester) async {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(
      const MethodChannel('mindlol/platform'),
      (MethodCall methodCall) async {
        if (methodCall.method == 'getMindLolStatus') {
          return {
            'todayCount': 3,
            'dailyLimit': 20,
            'limitReached': false,
            'accessibilityEnabled': true,
            'overlayPermissionGranted': true,
          };
        }
        return null;
      },
    );

    final platform = MindLolPlatform();
    await tester.pumpWidget(MindLolApp(platform: platform));
    await tester.pump(const Duration(milliseconds: 500));

    expect(find.text('MindLol'), findsOneWidget);
    expect(find.text('3'), findsOneWidget);
    expect(find.text('Reels today'), findsOneWidget);
    expect(find.text('nice and easy.'), findsOneWidget);

    // Verify developer terms are NOT visible
    expect(find.textContaining('Platform Bridge'), findsNothing);
    expect(find.textContaining('MethodChannel'), findsNothing);
  });
}
