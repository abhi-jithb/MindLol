import 'package:flutter_test/flutter_test.dart';
import 'package:mindlol/core/models/blob_state.dart';

void main() {
  group('BlobState Centralized Selection & Calculation', () {
    test('count == 0 produces CALM stage with chilling copy', () {
      final state = BlobState.fromCountAndLimit(0, 20);
      expect(state.stage, equals(BlobStage.calm));
      expect(state.title, equals('we\'re chilling.'));
      expect(state.ratio, equals(0.0));
    });

    test('count below 25% limit produces CALM stage with nice and easy copy', () {
      final state = BlobState.fromCountAndLimit(3, 20);
      expect(state.stage, equals(BlobStage.calm));
      expect(state.title, equals('nice and easy.'));
      expect(state.ratio, equals(0.15));
    });

    test('count between 25% and 75% limit produces WOBBLY stage', () {
      final state = BlobState.fromCountAndLimit(10, 20);
      expect(state.stage, equals(BlobStage.wobbly));
      expect(state.title, equals('okay... that\'s a few.'));
      expect(state.ratio, equals(0.5));
    });

    test('count near limit (75%-100%) produces CRACKED stage', () {
      final state = BlobState.fromCountAndLimit(16, 20);
      expect(state.stage, equals(BlobStage.cracked));
      expect(state.title, equals('uh oh.'));
      expect(state.ratio, equals(0.8));
    });

    test('count == limit produces SHATTERED stage with breathe copy', () {
      final state = BlobState.fromCountAndLimit(20, 20);
      expect(state.stage, equals(BlobStage.shattered));
      expect(state.title, equals('okay, let\'s breathe.'));
      expect(state.ratio, equals(1.0));
    });

    test('count above limit clamps ratio to 1.0 and produces SHATTERED stage', () {
      final state = BlobState.fromCountAndLimit(25, 20);
      expect(state.stage, equals(BlobStage.shattered));
      expect(state.title, equals('okay, let\'s breathe.'));
      expect(state.ratio, equals(1.0));
    });

    test('handles zero or negative daily limit gracefully without division by zero', () {
      final state = BlobState.fromCountAndLimit(5, 0);
      expect(state.stage, equals(BlobStage.wobbly)); // 5 / 20 fallback = 0.25 -> Wobbly
    });
  });
}
