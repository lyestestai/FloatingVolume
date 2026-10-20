import 'package:floating_volume/generated/native_api.g.dart';
import 'package:floating_volume/generated/native_events.g.dart';
import 'package:floating_volume/src/app.dart';
import 'package:floating_volume/src/widgets/cool_switch.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:permission_handler_platform_interface/permission_handler_platform_interface.dart';
import 'package:shared_preferences/shared_preferences.dart';

class _MockPermissionHandlerPlatform extends PermissionHandlerPlatform {
  @override
  Future<PermissionStatus> checkPermissionStatus(Permission permission) async {
    return PermissionStatus.granted;
  }

  @override
  Future<Map<Permission, PermissionStatus>> requestPermissions(
    List<Permission> permissions,
  ) async {
    return <Permission, PermissionStatus>{
      for (final p in permissions) p: PermissionStatus.granted,
    };
  }
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late bool mockServiceRunning;
  late bool mockFloatingVisible;
  late bool mockAutoStartEnabled;
  late int mockAutoStartDelay;
  late bool mockLogsEnabled;
  late String mockLogsContent;
  late List<String> recordedToasts;
  late bool mockShareCalled;
  late bool mockClearCalled;

  void emitEventChannelValue(String channelName, Object value) {
    final ByteData encoded = pigeonMethodCodec.encodeSuccessEnvelope(value);
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .handlePlatformMessage(channelName, encoded, (_) {});
  }

  void setupMockChannels() {
    final messenger =
        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;
    const codec = NativeApi.pigeonChannelCodec;

    void mockPigeonMethod(
      String methodName,
      Future<List<Object?>?> Function(List<Object?>? args) handler,
    ) {
      final channelName =
          'dev.flutter.pigeon.floating_volume.NativeApi.$methodName';
      messenger.setMockDecodedMessageHandler<Object?>(
        BasicMessageChannel<Object?>(channelName, codec),
        (Object? message) async {
          return await handler(message as List<Object?>?);
        },
      );
    }

    // EventChannels for serviceStatus and floatingVolumeVisibility
    const serviceStatusChannel =
        'dev.flutter.pigeon.floating_volume.NativeEvents.serviceStatus';
    const visibilityChannel =
        'dev.flutter.pigeon.floating_volume.NativeEvents.floatingVolumeVisibility';

    messenger.setMockMethodCallHandler(
      const MethodChannel(serviceStatusChannel, pigeonMethodCodec),
      (MethodCall call) async {
        if (call.method == 'listen') {
          Future.microtask(
            () => emitEventChannelValue(
              serviceStatusChannel,
              mockServiceRunning,
            ),
          );
        }
        return null;
      },
    );

    messenger.setMockMethodCallHandler(
      const MethodChannel(visibilityChannel, pigeonMethodCodec),
      (MethodCall call) async {
        if (call.method == 'listen') {
          Future.microtask(
            () => emitEventChannelValue(
              visibilityChannel,
              mockFloatingVisible,
            ),
          );
        }
        return null;
      },
    );

    // Permission handler MethodChannel mock (1 = PermissionStatus.granted)
    messenger.setMockMethodCallHandler(
      const MethodChannel('flutter.baseflow.com/permissions/methods'),
      (MethodCall call) async {
        if (call.method == 'checkPermissionStatus') {
          return 1; // granted
        }
        if (call.method == 'requestPermissions') {
          final List<dynamic> permissions = call.arguments as List<dynamic>;
          return <int, int>{for (final p in permissions) p as int: 1};
        }
        return 1;
      },
    );

    // NativeApi methods
    mockPigeonMethod('startService', (_) async {
      mockServiceRunning = true;
      mockFloatingVisible = true;
      emitEventChannelValue(serviceStatusChannel, true);
      emitEventChannelValue(visibilityChannel, true);
      return <Object?>[null];
    });

    mockPigeonMethod('stopService', (_) async {
      mockServiceRunning = false;
      mockFloatingVisible = false;
      emitEventChannelValue(serviceStatusChannel, false);
      emitEventChannelValue(visibilityChannel, false);
      return <Object?>[null];
    });

    mockPigeonMethod('showFloatingVolume', (_) async {
      mockFloatingVisible = true;
      emitEventChannelValue(visibilityChannel, true);
      return <Object?>[null];
    });

    mockPigeonMethod('hideFloatingVolume', (_) async {
      mockFloatingVisible = false;
      emitEventChannelValue(visibilityChannel, false);
      return <Object?>[null];
    });

    mockPigeonMethod('showToast', (args) async {
      if (args != null && args.isNotEmpty && args[0] is String) {
        recordedToasts.add(args[0]! as String);
      }
      return <Object?>[null];
    });

    mockPigeonMethod('getAutoStartEnabled', (_) async {
      return <Object?>[mockAutoStartEnabled];
    });

    mockPigeonMethod('setAutoStartEnabled', (args) async {
      mockAutoStartEnabled = (args?[0] as bool?) ?? false;
      return <Object?>[null];
    });

    mockPigeonMethod('getAutoStartDelay', (_) async {
      return <Object?>[mockAutoStartDelay];
    });

    mockPigeonMethod('setAutoStartDelay', (args) async {
      mockAutoStartDelay = (args?[0] as int?) ?? 10000;
      return <Object?>[null];
    });

    mockPigeonMethod('getLogsEnabled', (_) async {
      return <Object?>[mockLogsEnabled];
    });

    mockPigeonMethod('setLogsEnabled', (args) async {
      mockLogsEnabled = (args?[0] as bool?) ?? false;
      return <Object?>[null];
    });

    mockPigeonMethod('getLogsContent', (_) async {
      return <Object?>[mockLogsContent];
    });

    mockPigeonMethod('saveLogsToDisk', (_) async {
      return <Object?>[
        '/storage/emulated/0/Download/FloatingVolume_Log_test.txt',
      ];
    });

    mockPigeonMethod('exportLogsAndShare', (_) async {
      mockShareCalled = true;
      return <Object?>[null];
    });

    mockPigeonMethod('clearAllLogs', (_) async {
      mockClearCalled = true;
      mockLogsContent = '=== JOURNAL EFFACÉ ===';
      return <Object?>[null];
    });

    mockPigeonMethod('hasNotificationAccess', (_) async {
      return <Object?>[true];
    });
  }

  setUp(() {
    PermissionHandlerPlatform.instance = _MockPermissionHandlerPlatform();
    SharedPreferences.setMockInitialValues(<String, Object>{});
    mockServiceRunning = false;
    mockFloatingVisible = false;
    mockAutoStartEnabled = false;
    mockAutoStartDelay = 15000;
    mockLogsEnabled = false;
    mockLogsContent =
        '=== FLOATING VOLUME v0.9.11+101 — DIAGNOSTIC ===\n[BootReceiver] Test OK';
    recordedToasts = <String>[];
    mockShareCalled = false;
    mockClearCalled = false;
    setupMockChannels();
  });

  testWidgets(
    'Tous les boutons de configuration (Auto-Start, Slider, Thème, Logs) restent 100% cliquables une fois la barre mise sur ON',
    (WidgetTester tester) async {
      // Simuler une résolution d'autoradio Android (1024x600)
      tester.view.physicalSize = const Size(1024, 600);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

      PermissionHandlerPlatform.instance = _MockPermissionHandlerPlatform();
      await tester.pumpWidget(const FloatingVolumeApp());
      PermissionHandlerPlatform.instance = _MockPermissionHandlerPlatform();
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 100));

      // 1. Vérifier que la barre est initialement OFF
      CoolSwitch coolSwitch = tester.widget<CoolSwitch>(
        find.byType(CoolSwitch),
      );
      expect(coolSwitch.value, isFalse);

      // 2. Activer la barre principale (passage à "ON")
      final mainToggle = find.byKey(const Key('main_service_toggle'));
      expect(mainToggle, findsOneWidget);
      await tester.ensureVisible(mainToggle);
      await tester.pump();
      await tester.tap(mainToggle);
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 300));

      expect(mockServiceRunning, isTrue);
      expect(mockFloatingVisible, isTrue);
      coolSwitch = tester.widget<CoolSwitch>(find.byType(CoolSwitch));
      expect(coolSwitch.value, isTrue);

      // 3. Tester les boutons de thème pendant que la barre est ON
      final darkThemeBtn = find.byKey(const Key('theme_btn_dark'));
      expect(darkThemeBtn, findsOneWidget);
      await tester.tap(darkThemeBtn);
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 200));

      final lightThemeBtn = find.byKey(const Key('theme_btn_light'));
      await tester.tap(lightThemeBtn);
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 200));

      // 4. Tester le bouton Auto-Start pendant que la barre est ON
      final autoStartSwitchFinder = find.byKey(const Key('auto_start_switch'));
      await tester.ensureVisible(autoStartSwitchFinder);
      await tester.pump();

      SwitchListTile autoStartTile = tester.widget<SwitchListTile>(
        autoStartSwitchFinder,
      );
      expect(autoStartTile.value, isFalse);

      await tester.tap(autoStartSwitchFinder);
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 100));

      expect(mockAutoStartEnabled, isTrue);
      autoStartTile = tester.widget<SwitchListTile>(autoStartSwitchFinder);
      expect(autoStartTile.value, isTrue);
      expect(
        recordedToasts.any((t) => t.contains('Démarrage auto activé')),
        isTrue,
      );

      // 5. Tester le Slider de délai Auto-Start apparu après activation
      final delaySliderFinder = find.byKey(
        const Key('auto_start_delay_slider'),
      );
      expect(delaySliderFinder, findsOneWidget);
      await tester.ensureVisible(delaySliderFinder);
      await tester.drag(delaySliderFinder, const Offset(150, 0));
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 100));
      expect(mockAutoStartDelay, greaterThan(15000));

      // 6. Tester l'activation de la fenêtre de Logs de diagnostic pendant que la barre est ON
      final logsSwitchFinder = find.byKey(const Key('diagnostic_logs_switch'));
      await tester.ensureVisible(logsSwitchFinder);
      await tester.pump();

      await tester.tap(logsSwitchFinder);
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 100));

      expect(mockLogsEnabled, isTrue);
      expect(find.textContaining('FLOATING VOLUME v0.9.11+101'), findsOneWidget);

      // 7. Tester les boutons d'action de la fenêtre de logs (Enregistrer, Partager, Effacer)
      final saveBtn = find.byKey(const Key('logs_save_btn'));
      await tester.ensureVisible(saveBtn);
      await tester.tap(saveBtn);
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 100));
      expect(
        recordedToasts.any((t) => t.contains('FloatingVolume_Log_test.txt')),
        isTrue,
      );

      final shareBtn = find.byKey(const Key('logs_share_btn'));
      await tester.ensureVisible(shareBtn);
      await tester.tap(shareBtn);
      await tester.pump();
      expect(mockShareCalled, isTrue);

      final clearBtn = find.byKey(const Key('logs_clear_btn'));
      await tester.ensureVisible(clearBtn);
      await tester.tap(clearBtn);
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 100));
      expect(mockClearCalled, isTrue);
      expect(find.textContaining('JOURNAL EFFACÉ'), findsOneWidget);

      // 8. Remonter en haut de la ListView et éteindre la barre principale (passage à "OFF")
      await tester.drag(find.byType(ListView), const Offset(0, 600));
      await tester.pumpAndSettle();
      await tester.ensureVisible(mainToggle);
      await tester.pump();
      await tester.tap(mainToggle);
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 300));

      expect(mockFloatingVisible, isFalse);
      coolSwitch = tester.widget<CoolSwitch>(find.byType(CoolSwitch));
      expect(coolSwitch.value, isFalse);
    },
  );
}
