import 'package:flutter/material.dart';
import '../../app/theme/app_colors.dart';
import 'blob_copy.dart';

enum BlobStage { calm, wobbly, cracked, shattered }

/// Immutable state descriptor for the Blob mascot and usage progress.
///
/// Thresholds follow `.agents/rules/project.md`:
/// - 0–25% of limit → Calm
/// - 25–75% of limit → Wobbly
/// - 75–150% of limit → Cracked (strain/cap reached)
/// - 150%+ of limit → Shattered (overwhelmed)
class BlobState {
  final BlobStage stage;
  final String title;
  final String message;
  final double ratio;
  final double progressRatio;
  final int percentage;
  final Color color;

  const BlobState({
    required this.stage,
    required this.title,
    required this.message,
    required this.ratio,
    required this.progressRatio,
    required this.percentage,
    required this.color,
  });

  factory BlobState.fromCountAndLimit(int count, int limit) {
    final validLimit = limit <= 0 ? 20 : limit;
    final rawRatio = count / validLimit;

    BlobStage stage;
    Color color;

    if (count <= 0) {
      stage = BlobStage.calm;
      color = AppColors.stageCalm;
    } else if (rawRatio < 0.25) {
      stage = BlobStage.calm;
      color = AppColors.stageCalm;
    } else if (rawRatio < 0.75) {
      stage = BlobStage.wobbly;
      color = AppColors.stageWobbly;
    } else if (rawRatio < 1.50) {
      stage = BlobStage.cracked;
      color = AppColors.stageCracked;
    } else {
      stage = BlobStage.shattered;
      color = AppColors.stageShattered;
    }

    final copy = BlobCopy.getCopyForStage(stage, count);

    return BlobState(
      stage: stage,
      title: copy.title,
      message: copy.message,
      ratio: rawRatio,
      progressRatio: rawRatio.clamp(0.0, 1.0),
      percentage: (rawRatio * 100).round(),
      color: color,
    );
  }
}
