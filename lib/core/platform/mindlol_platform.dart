import 'dart:async';
import 'package:flutter/services.dart';
import 'mindlol_status.dart';

class MindLolPlatform {
  static const MethodChannel _defaultMethodChannel =
      MethodChannel('mindlol/platform');
  static const EventChannel _defaultEventChannel =
      EventChannel('mindlol/events');

  final MethodChannel methodChannel;
  final EventChannel eventChannel;

  MindLolPlatform({
    MethodChannel? methodChannel,
    EventChannel? eventChannel,
  })  : methodChannel = methodChannel ?? _defaultMethodChannel,
        eventChannel = eventChannel ?? _defaultEventChannel;

  Future<MindLolStatus> getMindLolStatus() async {
    final result =
        await methodChannel.invokeMethod<Map<dynamic, dynamic>>('getMindLolStatus');
    if (result == null) {
      return const MindLolStatus(
        todayCount: 0,
        dailyLimit: 20,
        limitReached: false,
        accessibilityEnabled: false,
        overlayPermissionGranted: false,
      );
    }
    return MindLolStatus.fromMap(result);
  }

  Future<int> getTodayCount() async {
    final count = await methodChannel.invokeMethod<int>('getTodayCount');
    return count ?? 0;
  }

  Future<int> getDailyLimit() async {
    final limit = await methodChannel.invokeMethod<int>('getDailyLimit');
    return limit ?? 20;
  }

  Future<MindLolStatus> setDailyLimit(int limit) async {
    final result = await methodChannel.invokeMethod<Map<dynamic, dynamic>>(
      'setDailyLimit',
      {'limit': limit},
    );
    if (result == null) {
      return getMindLolStatus();
    }
    return MindLolStatus.fromMap(result);
  }

  Future<MindLolStatus> setAppConfiguration({
    bool? instagramEnabled,
    bool? youtubeEnabled,
  }) async {
    final result = await methodChannel.invokeMethod<Map<dynamic, dynamic>>(
      'setAppConfiguration',
      {
        'instagramEnabled': ?instagramEnabled,
        'youtubeEnabled': ?youtubeEnabled,
      },
    );
    if (result == null) {
      return getMindLolStatus();
    }
    return MindLolStatus.fromMap(result);
  }

  Future<String> getBlockingMode() async {
    final mode = await methodChannel.invokeMethod<String>('getBlockingMode');
    return mode ?? 'BLOCK_AT_LIMIT';
  }

  Future<MindLolStatus> resetTodayCount() async {
    final result =
        await methodChannel.invokeMethod<Map<dynamic, dynamic>>('resetTodayCount');
    if (result == null) {
      return getMindLolStatus();
    }
    return MindLolStatus.fromMap(result);
  }

  Future<bool> isLimitReached() async {
    final reached = await methodChannel.invokeMethod<bool>('isLimitReached');
    return reached ?? false;
  }

  Future<bool> isAccessibilityEnabled() async {
    final enabled =
        await methodChannel.invokeMethod<bool>('isAccessibilityEnabled');
    return enabled ?? false;
  }

  Future<bool> isOverlayPermissionGranted() async {
    final granted =
        await methodChannel.invokeMethod<bool>('isOverlayPermissionGranted');
    return granted ?? false;
  }

  Future<bool> openAccessibilitySettings() async {
    final opened =
        await methodChannel.invokeMethod<bool>('openAccessibilitySettings');
    return opened ?? false;
  }

  Future<bool> openOverlaySettings() async {
    final opened =
        await methodChannel.invokeMethod<bool>('openOverlaySettings');
    return opened ?? false;
  }

  Stream<MindLolStatus> get statusStream {
    return eventChannel.receiveBroadcastStream().map((dynamic event) {
      if (event is Map<dynamic, dynamic>) {
        return MindLolStatus.fromMap(event);
      }
      return const MindLolStatus(
        todayCount: 0,
        dailyLimit: 20,
        limitReached: false,
        accessibilityEnabled: false,
        overlayPermissionGranted: false,
      );
    });
  }
}
