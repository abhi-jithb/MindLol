import 'package:flutter/material.dart';
import '../../../app/theme/app_colors.dart';
import '../../../core/models/blob_state.dart';

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
    final progressRatio = (todayCount / validLimit).clamp(0.0, 1.0);
    final percentage = (progressRatio * 100).round();

    final secondaryTextColor =
        isDark ? AppColors.darkTextSecondary : AppColors.lightTextSecondary;
    final trackColor = isDark
        ? AppColors.darkBorder.withValues(alpha: 0.6)
        : AppColors.lightBorder;

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
      decoration: BoxDecoration(
        color: isDark ? AppColors.darkSurface : AppColors.lightSurface,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(
          color: isDark ? AppColors.darkBorder : AppColors.lightBorder,
        ),
      ),
      child: Column(
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                '$percentage% of limit',
                style: TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.w600,
                  color: blobState.color,
                ),
              ),
              Text(
                '$todayCount / $validLimit Reels',
                style: TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.w500,
                  color: secondaryTextColor,
                ),
              ),
            ],
          ),
          const SizedBox(height: 10),
          ClipRRect(
            borderRadius: BorderRadius.circular(6),
            child: LinearProgressIndicator(
              value: progressRatio,
              minHeight: 8,
              backgroundColor: trackColor,
              valueColor: AlwaysStoppedAnimation<Color>(blobState.color),
            ),
          ),
        ],
      ),
    );
  }
}
