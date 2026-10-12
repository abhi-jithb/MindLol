import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mindlol/core/models/blob_state.dart';
import 'package:mindlol/features/home/widgets/progress_card.dart';

void main() {
  group('ProgressCard UI & Motion Tests', () {
    testWidgets('Renders progress metrics accurately below limit',
        (WidgetTester tester) async {
      final state = BlobState.fromCountAndLimit(10, 20);

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ProgressCard(
              todayCount: 10,
              dailyLimit: 20,
              blobState: state,
            ),
          ),
        ),
      );

      await tester.pump(const Duration(milliseconds: 450));

      expect(find.text('50% of limit'), findsOneWidget);
      expect(find.text('10 / 20 videos'), findsOneWidget);
      expect(find.byType(LinearProgressIndicator), findsOneWidget);
    });

    testWidgets('Renders accurate un-clamped percentage when limit is exceeded',
        (WidgetTester tester) async {
      final state = BlobState.fromCountAndLimit(30, 20);

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ProgressCard(
              todayCount: 30,
              dailyLimit: 20,
              blobState: state,
            ),
          ),
        ),
      );

      await tester.pump(const Duration(milliseconds: 450));

      expect(find.text('150% of limit'), findsOneWidget);
      expect(find.text('30 / 20 videos'), findsOneWidget);
    });

    testWidgets('Respects reduced motion preferences by rendering static indicator',
        (WidgetTester tester) async {
      final state = BlobState.fromCountAndLimit(15, 20);

      await tester.pumpWidget(
        MediaQuery(
          data: const MediaQueryData(accessibleNavigation: true),
          child: MaterialApp(
            home: Scaffold(
              body: ProgressCard(
                todayCount: 15,
                dailyLimit: 20,
                blobState: state,
              ),
            ),
          ),
        ),
      );

      await tester.pump(const Duration(milliseconds: 100));

      expect(find.text('75% of limit'), findsOneWidget);
      expect(find.byType(LinearProgressIndicator), findsOneWidget);
    });
  });
}
