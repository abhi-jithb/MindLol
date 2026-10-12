import 'package:flutter/material.dart';
import '../../../app/theme/app_colors.dart';
import '../../../core/models/blob_state.dart';

/// Progress visualization card displaying daily usage against limit.
///
/// Features:
/// - Smooth animated progress track transitions
/// - Accurate un-clamped percentage display
/// - Sentence-case neutral copy for short-form video consumption
/// - Full reduced-motion accessibility support
class ProgressCard extends StatelessWidget {
  final int todayCount;
  final int dailyLimit;
  final BlobState blobState;

  const ProgressCard({
    super.key,
    required this.todayCount,
    required this.dailyLimit,
    required this.blobState,
  });

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final validLimit = dailyLimit <= 0 ? 20 : dailyLimit;
    final rawRatio = todayCount / validLimit;
    final percentage = (rawRatio * 100).round();
    final clampedRatio = rawRatio.clamp(0.0, 1.0);

    final secondaryTextColor =
        isDark ? AppColors.darkTextSecondary : AppColors.lightTextSecondary;
    final trackBackgroundColor = isDark
        ? AppColors.darkBorder.withValues(alpha: 0.6)
        : AppColors.lightBorder;

    final disableAnimations = MediaQuery.maybeDisableAnimationsOf(context) ??
        MediaQuery.of(context).disableAnimations;

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
      decoration: BoxDecoration(
        color: isDark ? AppColors.darkSurface : AppColors.lightSurface,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
          color: isDark ? AppColors.darkBorder : AppColors.lightBorder,
          width: 1,
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                '$percentage% of limit',
                style: TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.w600,
                  letterSpacing: -0.1,
                  color: blobState.color,
                ),
              ),
              Text(
                '$todayCount / $validLimit videos',
                style: TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.w500,
                  color: secondaryTextColor,
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          ClipRRect(
            borderRadius: BorderRadius.circular(6),
            child: disableAnimations
                ? LinearProgressIndicator(
                    value: clampedRatio,
                    minHeight: 8,
                    backgroundColor: trackBackgroundColor,
                    valueColor: AlwaysStoppedAnimation<Color>(blobState.color),
                  )
                : TweenAnimationBuilder<double>(
                    tween: Tween<double>(begin: 0.0, end: clampedRatio),
                    duration: const Duration(milliseconds: 400),
                    curve: Curves.easeOutCubic,
                    builder: (context, value, child) {
                      return LinearProgressIndicator(
                        value: value,
                        minHeight: 8,
                        backgroundColor: trackBackgroundColor,
                        valueColor:
                            AlwaysStoppedAnimation<Color>(blobState.color),
                      );
                    },
                  ),
          ),
        ],
      ),
    );
  }
}
