import 'blob_state.dart';

/// Centralized microcopy dictionary for Blob personality states.
///
/// Tone guidelines:
/// - Human, playful, supportive, slightly weird.
/// - NEVER shame, scold, or sound like a corporate productivity coach.
class BlobCopy {
  final String title;
  final String message;

  const BlobCopy({required this.title, required this.message});

  static BlobCopy getCopyForStage(BlobStage stage, int count) {
    if (count <= 0) {
      return const BlobCopy(
        title: 'we\'re chilling.',
        message: 'No Reels today. Mind is clear and calm.',
      );
    }

    switch (stage) {
      case BlobStage.calm:
        return const BlobCopy(
          title: 'nice and easy.',
          message: 'Mindful consumption so far.',
        );
      case BlobStage.wobbly:
        return const BlobCopy(
          title: 'okay... that\'s a few.',
          message: 'Blob is getting a bit wobbly.',
        );
      case BlobStage.cracked:
        return const BlobCopy(
          title: 'uh oh.',
          message: 'Blob needs a tiny break soon.',
        );
      case BlobStage.shattered:
        return const BlobCopy(
          title: 'okay, let\'s breathe.',
          message: 'Blob is officially overwhelmed. Take a rest.',
        );
    }
  }
}
