import 'dart:async';
import 'package:bloc/bloc.dart' as a;
import 'package:floating_volume/generated/native_api.g.dart';
import 'package:floating_volume/src/single.dart';
import 'package:permission_handler/permission_handler.dart';

import 'event.dart' as e;
import 'state.dart' as s;

class Bloc extends a.Bloc<e.Event, s.State> {
  Timer? _pollingTimer;

  Bloc(super.initialState) {
    on<e.Event>((event, emit) async {
      switch (event) {
        case e.Event.initialize:
          emit(state.copyWith(operation: s.Operation.initializing));

          PermissionStatus overlayPermission = PermissionStatus.denied;
          PermissionStatus notificationPermission = PermissionStatus.denied;
          PermissionStatus batteryOptimizationPermission =
              PermissionStatus.denied;

          try {
            overlayPermission = await Permission.systemAlertWindow.status;
            notificationPermission = await Permission.notification.status;
            batteryOptimizationPermission =
                await Permission.ignoreBatteryOptimizations.status;
          } catch (_) {}

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
            ),
          );

          _pollingTimer?.cancel();
          _pollingTimer = Timer.periodic(const Duration(seconds: 3), (_) {
            if (!isClosed) {
              add(e.Event.refresh);
            }
          });
          break;

        case e.Event.refresh:
          try {
            final oStatus = await Permission.systemAlertWindow.status;
            final nStatus = await Permission.notification.status;
            final bStatus = await Permission.ignoreBatteryOptimizations.status;

            if (oStatus != state.overlayPermission.status ||
                nStatus != state.notificationPermission.status ||
                bStatus != state.batteryOptimizationPermission.status) {
              emit(
                state.copyWith(
                  overlayPermission: state.overlayPermission.copyWith(
                    status: oStatus,
                  ),
                  notificationPermission: state.notificationPermission.copyWith(
                    status: nStatus,
                  ),
                  batteryOptimizationPermission: state
                      .batteryOptimizationPermission
                      .copyWith(status: bStatus),
                ),
              );
            }
          } catch (_) {}
          break;

        case e.Event.requestOverlayPermission:
          emit(
            state.copyWith(
              overlayPermission: state.overlayPermission.copyWith(
                operation: s.PermissionOperation.requestingPermission,
              ),
            ),
          );

          final overlayPermission = await requestPermission(
            Permission.systemAlertWindow,
            state.overlayPermission.status,
          );

          emit(
            state.copyWith(
              overlayPermission: state.overlayPermission.copyWith(
                status: overlayPermission,
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

          final notificationPermission = await requestPermission(
            Permission.notification,
            state.notificationPermission.status,
          );

          emit(
            state.copyWith(
              notificationPermission: state.notificationPermission.copyWith(
                status: notificationPermission,
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

          final batteryOptimizationPermission = await requestPermission(
            Permission.ignoreBatteryOptimizations,
            state.batteryOptimizationPermission.status,
          );

          emit(
            state.copyWith(
              batteryOptimizationPermission: state.batteryOptimizationPermission
                  .copyWith(
                    status: batteryOptimizationPermission,
                    operation: s.PermissionOperation.none,
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
    try {
      await nativeApi.showToast(
        "${permission.toString().split('.').last} permission is permanently denied. Please enable it in settings.",
        ToastDuration.long,
      );
    } catch (_) {}
    final isOpened = await openAppSettings();
    if (!isOpened) {
      return status;
    }
    return permission.status;
  }

  return permission.request();
}
