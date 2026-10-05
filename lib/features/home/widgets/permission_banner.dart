import 'package:flutter/material.dart';
import '../../../app/theme/app_colors.dart';
import '../../../core/platform/mindlol_platform.dart';

class PermissionBanner extends StatelessWidget {
  final bool accessibilityEnabled;
  final bool overlayPermissionGranted;
  final MindLolPlatform platform;

  const PermissionBanner({
    super.key,
    required this.accessibilityEnabled,
    required this.overlayPermissionGranted,
    required this.platform,
  });

  @override
  Widget build(BuildContext context) {
    if (accessibilityEnabled && overlayPermissionGranted) {
      return const SizedBox.shrink();
    }

    final isDark = Theme.of(context).brightness == Brightness.dark;
    final message = !accessibilityEnabled
        ? 'MindLol needs Accessibility access to count Reels.'
        : 'MindLol needs Overlay permission to enforce limits.';

    return Container(
      margin: const EdgeInsets.only(bottom: 20),
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      decoration: BoxDecoration(
        color: isDark
            ? AppColors.coral.withValues(alpha: 0.15)
            : AppColors.coral.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(
          color: AppColors.coral.withValues(alpha: 0.4),
        ),
      ),
      child: Row(
        children: [
          const Icon(
            Icons.info_outline_rounded,
            color: AppColors.coral,
            size: 20,
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              message,
              style: TextStyle(
                fontSize: 13,
                fontWeight: FontWeight.w500,
                color: isDark
                    ? AppColors.darkTextPrimary
                    : AppColors.lightTextPrimary,
              ),
            ),
          ),
          TextButton(
            onPressed: () {
              if (!accessibilityEnabled) {
                platform.openAccessibilitySettings();
              } else {
                platform.openOverlaySettings();
              }
            },
            style: TextButton.styleFrom(
              foregroundColor: AppColors.coral,
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
              minimumSize: Size.zero,
              tapTargetSize: MaterialTapTargetSize.shrinkWrap,
            ),
            child: const Text('Enable'),
          ),
        ],
      ),
    );
  }
}
