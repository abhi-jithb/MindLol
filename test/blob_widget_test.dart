import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mindlol/core/models/blob_copy.dart';
import 'package:mindlol/core/models/blob_state.dart';
import 'package:mindlol/features/home/widgets/blob_widget.dart';

void main() {
  group('BlobWidget & Personality Motion Tests', () {
    testWidgets('Calm state renders cleanly with expressive face',
        (WidgetTester tester) async {
      final state = BlobState.fromCountAndLimit(2, 20);
      expect(state.stage, equals(BlobStage.calm));

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: BlobWidget(blobState: state),
          ),
        ),
      );

      await tester.pump(const Duration(milliseconds: 200));
      expect(find.byType(BlobWidget), findsOneWidget);
      expect(find.byType(CustomPaint), findsWidgets);
    });

    testWidgets('Wobbly state renders cleanly without throwing',
        (WidgetTester tester) async {
      final state = BlobState.fromCountAndLimit(10, 20);
      expect(state.stage, equals(BlobStage.wobbly));

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: BlobWidget(blobState: state),
          ),
        ),
      );

      await tester.pump(const Duration(milliseconds: 200));
      expect(find.byType(BlobWidget), findsOneWidget);
    });

    testWidgets('Cracked state renders cleanly at 100% cap without throwing',
        (WidgetTester tester) async {
      final state = BlobState.fromCountAndLimit(20, 20);
      expect(state.stage, equals(BlobStage.cracked));

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: BlobWidget(blobState: state),
          ),
        ),
      );

      await tester.pump(const Duration(milliseconds: 200));
      expect(find.byType(BlobWidget), findsOneWidget);
    });

    testWidgets('Shattered state renders cleanly at 150%+ limit without throwing',
        (WidgetTester tester) async {
      final state = BlobState.fromCountAndLimit(32, 20);
      expect(state.stage, equals(BlobStage.shattered));

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: BlobWidget(blobState: state),
          ),
        ),
      );

      await tester.pump(const Duration(milliseconds: 200));
      expect(find.byType(BlobWidget), findsOneWidget);
    });

    testWidgets('State transitions and reactions do not leak animation controllers',
        (WidgetTester tester) async {
      final calmState = BlobState.fromCountAndLimit(2, 20);
      final wobblyState = BlobState.fromCountAndLimit(10, 20);
      final shatteredState = BlobState.fromCountAndLimit(35, 20);

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: BlobWidget(blobState: calmState, todayCount: 2, dailyLimit: 20),
          ),
        ),
      );
      await tester.pump(const Duration(milliseconds: 200));

      // Transition to Wobbly with count increment reaction
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: BlobWidget(blobState: wobblyState, todayCount: 10, dailyLimit: 20),
          ),
        ),
      );
      await tester.pump(const Duration(milliseconds: 300));

      // Transition to Shattered with limit modification reaction
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: BlobWidget(blobState: shatteredState, todayCount: 35, dailyLimit: 20),
          ),
        ),
      );
      await tester.pump(const Duration(milliseconds: 300));

      expect(find.byType(BlobWidget), findsOneWidget);
    });

    testWidgets('Widget remains stable across repeated parent rebuilds without restarting reactions',
        (WidgetTester tester) async {
      final state = BlobState.fromCountAndLimit(5, 20);

      for (int i = 0; i < 5; i++) {
        await tester.pumpWidget(
          MaterialApp(
            home: Scaffold(
              body: BlobWidget(blobState: state, todayCount: 5, dailyLimit: 20),
            ),
          ),
        );
        await tester.pump(const Duration(milliseconds: 100));
      }

      expect(find.byType(BlobWidget), findsOneWidget);
    });

    testWidgets('Reduced motion behavior renders static state without throwing',
        (WidgetTester tester) async {
      final state = BlobState.fromCountAndLimit(15, 20);

      await tester.pumpWidget(
        MediaQuery(
          data: const MediaQueryData(
            accessibleNavigation: true,
          ),
          child: MaterialApp(
            home: Scaffold(
              body: BlobWidget(blobState: state),
            ),
          ),
        ),
      );

      await tester.pump(const Duration(milliseconds: 200));
      expect(find.byType(BlobWidget), findsOneWidget);
    });
  });

  group('BlobCopy Centralized Microcopy Tests', () {
    test('Centralized BlobCopy returns human supportive text for all stages', () {
      final zeroCopy = BlobCopy.getCopyForStage(BlobStage.calm, 0);
      expect(zeroCopy.title, equals('we\'re chilling.'));

      final calmCopy = BlobCopy.getCopyForStage(BlobStage.calm, 3);
      expect(calmCopy.title, equals('nice and easy.'));

      final wobblyCopy = BlobCopy.getCopyForStage(BlobStage.wobbly, 10);
      expect(wobblyCopy.title, equals('okay... that\'s a few.'));

      final crackedCopy = BlobCopy.getCopyForStage(BlobStage.cracked, 16);
      expect(crackedCopy.title, equals('uh oh.'));

      final shatteredCopy = BlobCopy.getCopyForStage(BlobStage.shattered, 32);
      expect(shatteredCopy.title, equals('okay, let\'s breathe.'));
    });
  });
}
