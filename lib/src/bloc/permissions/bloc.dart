import 'dart:async';
import 'package:bloc/bloc.dart' as a;
import 'package:device_info_plus/device_info_plus.dart';
import 'package:floating_volume/generated/native_api.g.dart';
import 'package:floating_volume/src/single.dart';
import 'package:permission_handler/permission_handler.dart';

import 'event.dart' as e;
import 'state.dart' as s;

Future<bool> shouldRequestPermissions() async {
  final info = await DeviceInfoPlugin().androidInfo;
  return info.version.sdkInt >= 23; // Only request on Android 6.0+
}

class Bloc extends a.Bloc<e.Event, s.State> {
  bool? _shouldRequestPermissions;
  Timer? _pollingTimer;

  Bloc(super.initialState) {
    on<e.Event>((event, emit) async {
      if (!(_shouldRequestPermissions ??= await shouldRequestPermissions())) {
        emit(
          state.copyWith(
            operation: s.Operation.none,
            overlayPermission: state.overlayPermission.copyWith(
              isInitialized: true,
              status: PermissionStatus.granted,
            ),
            notificationPermission: state.notificationPermission.copyWith(
              isInitialized: true,
              status: PermissionStatus.granted,
            ),
            batteryOptimizationPermission: state.batteryOptimizationPermission
                .copyWith(
                  isInitialized: true,
                  status: PermissionStatus.granted,
                ),
            notificationAccessPermission: state.notificationAccessPermission
                .copyWith(
                  isInitialized: true,
                  status: PermissionStatus.granted,
                ),
          ),
        );
        return;
      }

      switch (event) {
        case e.Event.initialize:
          emit(state.copyWith(operation: s.Operation.initializing));

          final PermissionStatus overlayPermission =
              await Permission.systemAlertWindow.status;
          final notificationPermission = await Permission.notification.status;
          final batteryOptimizationPermission =
              await Permission.ignoreBatteryOptimizations.status;
          final bool hasNotificationAccess = await nativeApi.hasNotificationAccess();

          emit(
            state.copyWith(
              operation: s.Operation.none,
              overlayPermission: state.overlayPermission.copyWith(
                isInitialized: true,
                status: overlayPermission,
              ),
              notificationPermission: state.notificationPermission.copyWith(
                isInitialized: true,
                status: notificationPermission,
              ),
              batteryOptimizationPermission: state.batteryOptimizationPermission
                  .copyWith(
                    isInitialized: true,
                    status: batteryOptimizationPermission,
                  ),
              notificationAccessPermission: state.notificationAccessPermission
                  .copyWith(
                    isInitialized: true,
                    status: hasNotificationAccess
                        ? PermissionStatus.granted
                        : PermissionStatus.denied,
                  ),
            ),
          );

          _pollingTimer?.cancel();
          _pollingTimer = Timer.periodic(const Duration(seconds: 4), (_) {
             if (!isClosed) add(e.Event.poll);
          });
          break;

        case e.Event.poll:
          final PermissionStatus oPermission = await Permission.systemAlertWindow.status;
          final nPermission = await Permission.notification.status;
          final bPermission = await Permission.ignoreBatteryOptimizations.status;
          final bool hasAccess = await nativeApi.hasNotificationAccess();
          
          emit(
            state.copyWith(
              overlayPermission: state.overlayPermission.copyWith(status: oPermission),
              notificationPermission: state.notificationPermission.copyWith(status: nPermission),
              batteryOptimizationPermission: state.batteryOptimizationPermission.copyWith(status: bPermission),
              notificationAccessPermission: state.notificationAccessPermission.copyWith(
                  status: hasAccess ? PermissionStatus.granted : PermissionStatus.denied),
            )
          );
          break;

        case e.Event.requestOverlayPermission:
          emit(
            state.copyWith(
              overlayPermission: state.overlayPermission.copyWith(
                operation: s.PermissionOperation.requestingPermission,
              ),
            ),
          );

          final newOverlayPermission = await requestPermission(
            Permission.systemAlertWindow,
            state.overlayPermission.status,
          );

          emit(
            state.copyWith(
              overlayPermission: state.overlayPermission.copyWith(
                status: newOverlayPermission,
                operation: s.PermissionOperation.none,
              ),
            ),
          );
          break;

        case e.Event.requestNotificationPermission:
          emit(
            state.copyWith(
              notificationPermission: state.notificationPermission.copyWith(
                operation: s.PermissionOperation.requestingPermission,
              ),
            ),
          );

          final newNotificationPermission = await requestPermission(
            Permission.notification,
            state.notificationPermission.status,
          );

          emit(
            state.copyWith(
              notificationPermission: state.notificationPermission.copyWith(
                status: newNotificationPermission,
                operation: s.PermissionOperation.none,
              ),
            ),
          );
          break;

        case e.Event.requestBatteryOptimizationPermission:
          emit(
            state.copyWith(
              batteryOptimizationPermission: state.batteryOptimizationPermission
                  .copyWith(
                    operation: s.PermissionOperation.requestingPermission,
                  ),
            ),
          );

          final newBatteryPermission = await requestPermission(
            Permission.ignoreBatteryOptimizations,
            state.batteryOptimizationPermission.status,
          );

          emit(
            state.copyWith(
              batteryOptimizationPermission: state.batteryOptimizationPermission
                  .copyWith(
                    status: newBatteryPermission,
                    operation: s.PermissionOperation.none,
                  ),
            ),
          );
          break;

        case e.Event.requestNotificationAccessPermission:
          emit(
            state.copyWith(
              notificationAccessPermission: state.notificationAccessPermission
                  .copyWith(
                    operation: s.PermissionOperation.requestingPermission,
                  ),
            ),
          );

          await nativeApi.requestNotificationAccess();
          
          final bool updatedHasAccess = await nativeApi.hasNotificationAccess();

          emit(
            state.copyWith(
              notificationAccessPermission: state.notificationAccessPermission
                  .copyWith(
                    operation: s.PermissionOperation.none,
                    status: updatedHasAccess ? PermissionStatus.granted : PermissionStatus.denied,
                  ),
            ),
          );
          break;
      }
    });
  }

  @override
  Future<void> close() {
    _pollingTimer?.cancel();
    return super.close();
  }
}

Future<PermissionStatus> requestPermission(
  Permission permission,
  PermissionStatus status,
) async {
  if (status.isGranted) {
    return status;
  }

  if (status.isPermanentlyDenied) {
    await nativeApi.showToast(
      "${permission.toString().split('.').last} permission is permanently denied. Please enable it in settings.",
      ToastDuration.long,
    );
    await openAppSettings();
    return permission.status;
  }

  return permission.request();
}
