class MindLolStatus {
  final int todayCount;
  final int dailyLimit;
  final bool limitReached;
  final bool accessibilityEnabled;
  final bool overlayPermissionGranted;
  final bool instagramEnabled;
  final bool youtubeEnabled;
  final String blockingMode;

  const MindLolStatus({
    required this.todayCount,
    required this.dailyLimit,
    required this.limitReached,
    required this.accessibilityEnabled,
    required this.overlayPermissionGranted,
    this.instagramEnabled = true,
    this.youtubeEnabled = true,
    this.blockingMode = 'BLOCK_AT_LIMIT',
  });

  factory MindLolStatus.fromMap(Map<dynamic, dynamic> map) {
    return MindLolStatus(
      todayCount: (map['todayCount'] as num?)?.toInt() ?? 0,
      dailyLimit: (map['dailyLimit'] as num?)?.toInt() ?? 20,
      limitReached: map['limitReached'] as bool? ?? false,
      accessibilityEnabled: map['accessibilityEnabled'] as bool? ?? false,
      overlayPermissionGranted:
          map['overlayPermissionGranted'] as bool? ?? false,
      instagramEnabled: map['instagramEnabled'] as bool? ?? true,
      youtubeEnabled: map['youtubeEnabled'] as bool? ?? true,
      blockingMode: map['blockingMode'] as String? ?? 'BLOCK_AT_LIMIT',
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
    };
  }

  @override
  String toString() {
    return 'MindLolStatus(todayCount: $todayCount, dailyLimit: $dailyLimit, limitReached: $limitReached, accessibilityEnabled: $accessibilityEnabled, overlayPermissionGranted: $overlayPermissionGranted, instagramEnabled: $instagramEnabled, youtubeEnabled: $youtubeEnabled, blockingMode: $blockingMode)';
  }
}
