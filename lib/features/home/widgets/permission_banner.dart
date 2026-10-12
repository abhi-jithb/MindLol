import 'package:flutter/material.dart';
import '../../../app/theme/app_colors.dart';
import '../../../core/platform/mindlol_platform.dart';
import '../../../core/platform/mindlol_status.dart';

/// Permission and tracking status banner with accurate distinction between
/// counting permissions (Accessibility) and blocking permissions (Overlay).
///
/// Follows compliance and honesty rules:
/// - Never tells the user that counting is broken when only overlay is missing.
/// - Clear, non-alarming sentence-case copy.
/// - Minimum 48dp touch targets on action buttons.
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
    final primaryTextColor =
        isDark ? AppColors.darkTextPrimary : AppColors.lightTextPrimary;

    // 1. Accessibility missing: Counting is inactive
    if (!accessibilityEnabled) {
      final String message = !overlayPermissionGranted
          ? 'MindLol needs accessibility access to count videos and overlay permission to enforce limits.'
          : 'Accessibility access is needed to count short-form videos.';

      return _buildBannerCard(
        context: context,
        icon: Icons.info_outline_rounded,
        iconColor: AppColors.coral,
        borderColor: AppColors.coral.withValues(alpha: 0.35),
        backgroundColor: isDark
            ? AppColors.coral.withValues(alpha: 0.12)
            : AppColors.coral.withValues(alpha: 0.08),
        message: message,
        textColor: primaryTextColor,
        actionLabel: 'Enable',
        onAction: () => platform.openAccessibilitySettings(),
      );
    }

    // 2. Counting active, but Overlay missing: Blocker cannot display
    if (!overlayPermissionGranted) {
      return _buildBannerCard(
        context: context,
        icon: Icons.layers_outlined,
        iconColor: AppColors.coral,
        borderColor: AppColors.coral.withValues(alpha: 0.35),
        backgroundColor: isDark
            ? AppColors.coral.withValues(alpha: 0.12)
            : AppColors.coral.withValues(alpha: 0.08),
        message:
            'Counting is active. Enable overlay permission to block feeds when your limit is reached.',
        textColor: primaryTextColor,
        actionLabel: 'Enable',
        onAction: () => platform.openOverlaySettings(),
      );
    }

    // 3. Tracking Uncertain: Inform user honestly without wiping count
    if (trackingHealthState == TrackingHealthState.trackingUncertain) {
      return _buildBannerCard(
        context: context,
        icon: Icons.warning_amber_rounded,
        iconColor: AppColors.coral,
        borderColor: AppColors.coral.withValues(alpha: 0.3),
        backgroundColor: isDark
            ? AppColors.coral.withValues(alpha: 0.1)
            : AppColors.coral.withValues(alpha: 0.06),
        message: trackingHealthMessage ??
            'Detection uncertain on this screen. Your count is preserved.',
        textColor: primaryTextColor,
      );
    }

    // 4. Tracking Paused: User disabled supported apps in Settings
    if (trackingHealthState == TrackingHealthState.trackingPaused) {
      return _buildBannerCard(
        context: context,
        icon: Icons.pause_circle_outline_rounded,
        iconColor: AppColors.paused,
        borderColor: AppColors.paused.withValues(alpha: 0.3),
        backgroundColor: isDark
            ? AppColors.paused.withValues(alpha: 0.12)
            : AppColors.paused.withValues(alpha: 0.08),
        message: trackingHealthMessage ??
            'Tracking is paused because apps are disabled in Settings.',
        textColor: primaryTextColor,
      );
    }

    // 5. Normal operation: Clean UI
    return const SizedBox.shrink();
  }

  Widget _buildBannerCard({
    required BuildContext context,
    required IconData icon,
    required Color iconColor,
    required Color borderColor,
    required Color backgroundColor,
    required String message,
    required Color textColor,
    String? actionLabel,
    VoidCallback? onAction,
  }) {
    return Container(
      margin: const EdgeInsets.only(bottom: 20),
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      decoration: BoxDecoration(
        color: backgroundColor,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: borderColor, width: 1),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          Icon(
            icon,
            color: iconColor,
            size: 20,
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              message,
              style: TextStyle(
                fontSize: 13,
                fontWeight: FontWeight.w500,
                height: 1.35,
                color: textColor,
              ),
            ),
          ),
          if (actionLabel != null && onAction != null) ...[
            const SizedBox(width: 8),
            TextButton(
              onPressed: onAction,
              style: TextButton.styleFrom(
                foregroundColor: iconColor,
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                minimumSize: const Size(48, 44),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(8),
                ),
              ),
              child: Text(
                actionLabel,
                style: const TextStyle(fontWeight: FontWeight.w600),
              ),
            ),
          ],
        ],
      ),
    );
  }
}
