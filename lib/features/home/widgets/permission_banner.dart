import 'package:flutter/material.dart';
import '../../../app/theme/app_colors.dart';
import '../../../core/platform/mindlol_platform.dart';
import '../../../core/platform/mindlol_status.dart';

class PermissionBanner extends StatelessWidget {
  final bool accessibilityEnabled;
  final bool overlayPermissionGranted;
  final MindLolPlatform platform;
  final TrackingHealthState? trackingHealthState;
  final String? trackingHealthMessage;

  const PermissionBanner({
    super.key,
    required this.accessibilityEnabled,
    required this.overlayPermissionGranted,
    required this.platform,
    this.trackingHealthState,
    this.trackingHealthMessage,
  });

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    // 1. Permissions required
    if (!accessibilityEnabled || !overlayPermissionGranted) {
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
                padding:
                    const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                minimumSize: Size.zero,
                tapTargetSize: MaterialTapTargetSize.shrinkWrap,
              ),
              child: const Text('Enable'),
            ),
          ],
        ),
      );
    }

    // 2. Tracking Uncertain: Inform user without alarming or wiping count
    if (trackingHealthState == TrackingHealthState.trackingUncertain) {
      return Container(
        margin: const EdgeInsets.only(bottom: 20),
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        decoration: BoxDecoration(
          color: isDark
              ? AppColors.coral.withValues(alpha: 0.12)
              : AppColors.coral.withValues(alpha: 0.08),
          borderRadius: BorderRadius.circular(12),
          border: Border.all(
            color: AppColors.coral.withValues(alpha: 0.3),
          ),
        ),
        child: Row(
          children: [
            const Icon(
              Icons.warning_amber_rounded,
              color: AppColors.coral,
              size: 20,
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Text(
                trackingHealthMessage ??
                    'Detection uncertain on this screen. Your count is preserved.',
                style: TextStyle(
                  fontSize: 13,
                  fontWeight: FontWeight.w500,
                  color: isDark
                      ? AppColors.darkTextPrimary
                      : AppColors.lightTextPrimary,
                ),
              ),
            ),
          ],
        ),
      );
    }

    // 3. Tracking Paused: User disabled supported apps
    if (trackingHealthState == TrackingHealthState.trackingPaused) {
      return Container(
        margin: const EdgeInsets.only(bottom: 20),
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        decoration: BoxDecoration(
          color: isDark
              ? Colors.grey.withValues(alpha: 0.15)
              : Colors.grey.withValues(alpha: 0.1),
          borderRadius: BorderRadius.circular(12),
          border: Border.all(
            color: Colors.grey.withValues(alpha: 0.3),
          ),
        ),
        child: Row(
          children: [
            const Icon(
              Icons.pause_circle_outline_rounded,
              color: Colors.grey,
              size: 20,
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Text(
                trackingHealthMessage ??
                    'Tracking is paused because apps are disabled in Settings.',
                style: TextStyle(
                  fontSize: 13,
                  fontWeight: FontWeight.w500,
                  color: isDark
                      ? AppColors.darkTextPrimary
                      : AppColors.lightTextPrimary,
                ),
              ),
            ),
          ],
        ),
      );
    }

    // 4. Standby or Tracking active: Clean view, no alarming banners
    return const SizedBox.shrink();
  }
}
