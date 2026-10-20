import 'dart:async';
import 'package:bloc/bloc.dart' as a;
import 'package:floating_volume/generated/native_events.g.dart';
import 'package:floating_volume/src/single.dart';

import 'event.dart' as e;
import 'state.dart' as s;

class Bloc extends a.Bloc<e.Event, s.State> {
  StreamSubscription<bool>? _serviceSub;
  StreamSubscription<bool>? _visibilitySub;

  Bloc(super.initialState) {
    on<e.Event>((event, emit) async {
      switch (event) {
        case e.Event.initialize:
          await _serviceSub?.cancel();
          _serviceSub = serviceStatus().listen(
            (isEnabled) {
              if (!isClosed) {
                add(isEnabled ? e.Event.syncServiceOn : e.Event.syncServiceOff);
              }
            },
            onError: (_) {},
          );

          await _visibilitySub?.cancel();
          _visibilitySub = floatingVolumeVisibility().listen(
            (isVisible) {
              if (!isClosed) {
                add(isVisible ? e.Event.syncVisible : e.Event.syncHidden);
              }
            },
            onError: (_) {},
          );
          break;

        case e.Event.syncServiceOn:
          if (!state.isEnabled) {
            emit(state.copyWith(isEnabled: true));
          }
          break;

        case e.Event.syncServiceOff:
          if (state.isEnabled) {
            emit(state.copyWith(isEnabled: false));
          }
          break;

        case e.Event.syncVisible:
          if (!state.isVisible) {
            emit(state.copyWith(isVisible: true));
          }
          break;

        case e.Event.syncHidden:
          if (state.isVisible) {
            emit(state.copyWith(isVisible: false));
          }
          break;

        case e.Event.enable:
          emit(
            state.copyWith(
              operation: s.Operation.enabling,
              isEnabled: true,
              isVisible: true,
            ),
          );

          try {
            await nativeApi.startService();
          } catch (_) {}

          try {
            await nativeApi.showFloatingVolume();
          } catch (_) {}

          emit(
            state.copyWith(
              isEnabled: true,
              isVisible: true,
              operation: s.Operation.none,
            ),
          );
          break;

        case e.Event.disable:
          emit(
            state.copyWith(
              operation: s.Operation.disabling,
              isEnabled: false,
              isVisible: false,
            ),
          );

          try {
            await nativeApi.hideFloatingVolume();
          } catch (_) {}

          try {
            await nativeApi.stopService();
          } catch (_) {}

          emit(
            state.copyWith(
              isEnabled: false,
              isVisible: false,
              operation: s.Operation.none,
            ),
          );
          break;

        case e.Event.toggle:
          if (state.isEnabled) {
            add(e.Event.disable);
          } else {
            add(e.Event.enable);
          }
          break;
      }
    });
  }

  @override
  Future<void> close() async {
    await _serviceSub?.cancel();
    await _visibilitySub?.cancel();
    return super.close();
  }
}
