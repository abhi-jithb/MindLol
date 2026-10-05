import 'package:flutter/material.dart';
import '../../app/theme/app_colors.dart';
import 'blob_copy.dart';

enum BlobStage { calm, wobbly, cracked, shattered }

class BlobState {
  final BlobStage stage;
  final String title;
  final String message;
  final double ratio;
  final Color color;

  const BlobState({
    required this.stage,
    required this.title,
    required this.message,
    required this.ratio,
    required this.color,
  });

  factory BlobState.fromCountAndLimit(int count, int limit) {
    final validLimit = limit <= 0 ? 20 : limit;
    final ratio = count / validLimit;

    BlobStage stage;
    Color color;

    if (count <= 0) {
      stage = BlobStage.calm;
      color = AppColors.stageCalm;
    } else if (ratio < 0.25) {
      stage = BlobStage.calm;
      color = AppColors.stageCalm;
    } else if (ratio < 0.75) {
      stage = BlobStage.wobbly;
      color = AppColors.stageWobbly;
    } else if (ratio < 1.0) {
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
      ratio: ratio.clamp(0.0, 1.0),
      color: color,
    );
  }
}
