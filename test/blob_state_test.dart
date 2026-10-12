import 'package:flutter_test/flutter_test.dart';
import 'package:mindlol/core/models/blob_state.dart';

void main() {
  group('BlobState Centralized Selection & Calculation', () {
    test('count == 0 produces CALM stage with chilling copy', () {
      final state = BlobState.fromCountAndLimit(0, 20);
      expect(state.stage, equals(BlobStage.calm));
      expect(state.title, equals('we\'re chilling.'));
      expect(state.ratio, equals(0.0));
      expect(state.progressRatio, equals(0.0));
    });

    test('count below 25% limit produces CALM stage with nice and easy copy', () {
      final state = BlobState.fromCountAndLimit(3, 20);
      expect(state.stage, equals(BlobStage.calm));
      expect(state.title, equals('nice and easy.'));
      expect(state.ratio, equals(0.15));
      expect(state.progressRatio, equals(0.15));
    });

    test('count between 25% and 75% limit produces WOBBLY stage', () {
      final state = BlobState.fromCountAndLimit(10, 20);
      expect(state.stage, equals(BlobStage.wobbly));
      expect(state.title, equals('okay... that\'s a few.'));
      expect(state.ratio, equals(0.5));
      expect(state.progressRatio, equals(0.5));
    });

    test('count near or at limit (75%-150%) produces CRACKED stage', () {
      // 16 / 20 = 80% (approaching limit)
      final state80 = BlobState.fromCountAndLimit(16, 20);
      expect(state80.stage, equals(BlobStage.cracked));
      expect(state80.title, equals('uh oh.'));
      expect(state80.ratio, equals(0.8));
      expect(state80.progressRatio, equals(0.8));

      // 20 / 20 = 100% (limit reached, cap active -> Blob is cracked/strained, NOT shattered)
      final state100 = BlobState.fromCountAndLimit(20, 20);
      expect(state100.stage, equals(BlobStage.cracked));
      expect(state100.title, equals('uh oh.'));
      expect(state100.ratio, equals(1.0));
      expect(state100.progressRatio, equals(1.0));

      // 28 / 20 = 140% (post-limit strain)
      final state140 = BlobState.fromCountAndLimit(28, 20);
      expect(state140.stage, equals(BlobStage.cracked));
      expect(state140.title, equals('uh oh.'));
      expect(state140.ratio, equals(1.4));
      expect(state140.progressRatio, equals(1.0));
    });

    test('count at 150%+ limit produces SHATTERED stage', () {
      // 30 / 20 = 150%
      final state150 = BlobState.fromCountAndLimit(30, 20);
      expect(state150.stage, equals(BlobStage.shattered));
      expect(state150.title, equals('okay, let\'s breathe.'));
      expect(state150.ratio, equals(1.5));
      expect(state150.progressRatio, equals(1.0));

      // 40 / 20 = 200%
      final state200 = BlobState.fromCountAndLimit(40, 20);
      expect(state200.stage, equals(BlobStage.shattered));
      expect(state200.title, equals('okay, let\'s breathe.'));
      expect(state200.ratio, equals(2.0));
      expect(state200.progressRatio, equals(1.0));
    });

    test('handles zero or negative daily limit gracefully without division by zero', () {
      final state = BlobState.fromCountAndLimit(5, 0);
      expect(state.stage, equals(BlobStage.wobbly)); // 5 / 20 fallback = 0.25 -> Wobbly
    });
  });
}
