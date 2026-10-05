import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mindlol/core/platform/mindlol_platform.dart';
import 'package:mindlol/core/platform/mindlol_status.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  group('MindLolStatus', () {
    test('fromMap creates valid MindLolStatus instance', () {
      final map = {
        'todayCount': 5,
        'dailyLimit': 20,
        'limitReached': false,
        'accessibilityEnabled': true,
        'overlayPermissionGranted': true,
      };

      final status = MindLolStatus.fromMap(map);

      expect(status.todayCount, equals(5));
      expect(status.dailyLimit, equals(20));
      expect(status.limitReached, isFalse);
      expect(status.accessibilityEnabled, isTrue);
      expect(status.overlayPermissionGranted, isTrue);
    });

    test('toMap converts MindLolStatus back to map', () {
      const status = MindLolStatus(
        todayCount: 12,
        dailyLimit: 15,
        limitReached: false,
        accessibilityEnabled: true,
        overlayPermissionGranted: false,
      );

      final map = status.toMap();

      expect(map['todayCount'], equals(12));
      expect(map['dailyLimit'], equals(15));
      expect(map['limitReached'], isFalse);
      expect(map['accessibilityEnabled'], isTrue);
      expect(map['overlayPermissionGranted'], isFalse);
    });
  });

  group('MindLolPlatform', () {
    late MindLolPlatform platform;
    late List<MethodCall> log;

    setUp(() {
      log = <MethodCall>[];
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(
        const MethodChannel('mindlol/platform'),
        (MethodCall methodCall) async {
          log.add(methodCall);
          switch (methodCall.method) {
            case 'getMindLolStatus':
              return {
                'todayCount': 7,
                'dailyLimit': 20,
                'limitReached': false,
                'accessibilityEnabled': true,
                'overlayPermissionGranted': true,
              };
            case 'getTodayCount':
              return 7;
            case 'getDailyLimit':
              return 20;
            case 'setDailyLimit':
              final limit = (methodCall.arguments as Map)['limit'] as int;
              return {
                'todayCount': 7,
                'dailyLimit': limit,
                'limitReached': 7 >= limit,
                'accessibilityEnabled': true,
                'overlayPermissionGranted': true,
              };
            case 'isLimitReached':
              return false;
            case 'isAccessibilityEnabled':
              return true;
            case 'isOverlayPermissionGranted':
              return true;
            case 'openAccessibilitySettings':
              return true;
            case 'openOverlaySettings':
              return true;
            default:
              return null;
          }
        },
      );

      platform = MindLolPlatform();
    });

    tearDown(() {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(
        const MethodChannel('mindlol/platform'),
        null,
      );
    });

    test('getMindLolStatus invokes correct MethodChannel call', () async {
      final status = await platform.getMindLolStatus();
      expect(log, hasLength(1));
      expect(log.first.method, equals('getMindLolStatus'));
      expect(status.todayCount, equals(7));
      expect(status.dailyLimit, equals(20));
    });

    test('setDailyLimit passes limit parameter and returns updated status', () async {
      final status = await platform.setDailyLimit(5);
      expect(log, hasLength(1));
      expect(log.first.method, equals('setDailyLimit'));
      expect(log.first.arguments, equals({'limit': 5}));
      expect(status.dailyLimit, equals(5));
      expect(status.limitReached, isTrue);
    });

    test('openAccessibilitySettings invokes settings command', () async {
      final opened = await platform.openAccessibilitySettings();
      expect(opened, isTrue);
      expect(log.first.method, equals('openAccessibilitySettings'));
    });

    test('openOverlaySettings invokes overlay settings command', () async {
      final opened = await platform.openOverlaySettings();
      expect(opened, isTrue);
      expect(log.first.method, equals('openOverlaySettings'));
    });
  });
}
