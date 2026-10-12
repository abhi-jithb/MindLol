enum TrackingHealthState {
  tracking,
  permissionRequired,
  standby,
  trackingUncertain,
  trackingPaused;

  static TrackingHealthState fromString(String? val) {
    switch (val) {
      case 'TRACKING':
        return TrackingHealthState.tracking;
      case 'PERMISSION_REQUIRED':
        return TrackingHealthState.permissionRequired;
      case 'TRACKING_UNCERTAIN':
        return TrackingHealthState.trackingUncertain;
      case 'TRACKING_PAUSED':
        return TrackingHealthState.trackingPaused;
      case 'STANDBY':
      default:
        return TrackingHealthState.standby;
    }
  }

  String toKey() {
    switch (this) {
      case TrackingHealthState.tracking:
        return 'TRACKING';
      case TrackingHealthState.permissionRequired:
        return 'PERMISSION_REQUIRED';
      case TrackingHealthState.trackingUncertain:
        return 'TRACKING_UNCERTAIN';
      case TrackingHealthState.trackingPaused:
        return 'TRACKING_PAUSED';
      case TrackingHealthState.standby:
        return 'STANDBY';
    }
  }
}

class MindLolStatus {
  final int todayCount;
  final int dailyLimit;
  final bool limitReached;
  final bool accessibilityEnabled;
  final bool overlayPermissionGranted;
  final bool instagramEnabled;
  final bool youtubeEnabled;
  final String blockingMode;
  final TrackingHealthState trackingHealthState;
  final String trackingHealthTitle;
  final String trackingHealthMessage;
  final String trackingActionRequired;

  const MindLolStatus({
    required this.todayCount,
    required this.dailyLimit,
    required this.limitReached,
    required this.accessibilityEnabled,
    required this.overlayPermissionGranted,
    this.instagramEnabled = true,
    this.youtubeEnabled = true,
    this.blockingMode = 'BLOCK_AT_LIMIT',
    this.trackingHealthState = TrackingHealthState.standby,
    this.trackingHealthTitle = 'Monitoring ready',
    this.trackingHealthMessage =
        'Tracking activates automatically when you open Reels or Shorts.',
    this.trackingActionRequired = '',
  });

  factory MindLolStatus.fromMap(Map<dynamic, dynamic> map) {
    final access = map['accessibilityEnabled'] as bool? ?? false;
    final overlay = map['overlayPermissionGranted'] as bool? ?? false;

    TrackingHealthState resolvedState;
    if (map.containsKey('trackingHealthState')) {
      resolvedState =
          TrackingHealthState.fromString(map['trackingHealthState'] as String?);
    } else {
      resolvedState = (!access || !overlay)
          ? TrackingHealthState.permissionRequired
          : TrackingHealthState.standby;
    }

    return MindLolStatus(
      todayCount: (map['todayCount'] as num?)?.toInt() ?? 0,
      dailyLimit: (map['dailyLimit'] as num?)?.toInt() ?? 20,
      limitReached: map['limitReached'] as bool? ?? false,
      accessibilityEnabled: access,
      overlayPermissionGranted: overlay,
      instagramEnabled: map['instagramEnabled'] as bool? ?? true,
      youtubeEnabled: map['youtubeEnabled'] as bool? ?? true,
      blockingMode: map['blockingMode'] as String? ?? 'BLOCK_AT_LIMIT',
      trackingHealthState: resolvedState,
      trackingHealthTitle: map['trackingHealthTitle'] as String? ??
          (resolvedState == TrackingHealthState.permissionRequired
              ? 'Permission needed'
              : 'Monitoring ready'),
      trackingHealthMessage: map['trackingHealthMessage'] as String? ??
          (resolvedState == TrackingHealthState.permissionRequired
              ? 'MindLol needs permissions to track and enforce limits.'
              : 'Tracking activates automatically when you open Reels or Shorts.'),
      trackingActionRequired:
          map['trackingActionRequired'] as String? ?? '',
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'todayCount': todayCount,
      'dailyLimit': dailyLimit,
      'limitReached': limitReached,
      'accessibilityEnabled': accessibilityEnabled,
      'overlayPermissionGranted': overlayPermissionGranted,
      'instagramEnabled': instagramEnabled,
      'youtubeEnabled': youtubeEnabled,
      'blockingMode': blockingMode,
      'trackingHealthState': trackingHealthState.toKey(),
      'trackingHealthTitle': trackingHealthTitle,
      'trackingHealthMessage': trackingHealthMessage,
      'trackingActionRequired': trackingActionRequired,
    };
  }

  @override
  String toString() {
    return 'MindLolStatus(todayCount: $todayCount, dailyLimit: $dailyLimit, limitReached: $limitReached, accessibilityEnabled: $accessibilityEnabled, overlayPermissionGranted: $overlayPermissionGranted, instagramEnabled: $instagramEnabled, youtubeEnabled: $youtubeEnabled, blockingMode: $blockingMode, trackingHealthState: $trackingHealthState)';
  }
}
