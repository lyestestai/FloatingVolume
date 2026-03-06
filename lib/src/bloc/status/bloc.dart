import 'dart:async';
import 'package:bloc/bloc.dart' as a;
import 'package:floating_volume/generated/native_api.g.dart';
import 'package:floating_volume/generated/native_events.g.dart';
import 'package:floating_volume/src/single.dart';

import 'event.dart' as e;
import 'state.dart' as s;

class Bloc extends a.Bloc<e.Event, s.State> {
  StreamSubscription<bool>? _serviceSub;
  StreamSubscription<bool>? _visibilitySub;

  Bloc(super.initialState) {
    on<e.Initialize>((event, emit) {
      _serviceSub?.cancel();
      _serviceSub = serviceStatus().listen((isEnabled) {
        if (!isClosed) add(e.ServiceStatusChanged(isEnabled));
      });
      
      _visibilitySub?.cancel();
      _visibilitySub = floatingVolumeVisibility().listen((isVisible) {
        if (!isClosed) add(e.VisibilityChanged(isVisible));
      });
    });

    on<e.ServiceStatusChanged>((event, emit) {
      emit(state.copyWith(isEnabled: event.isEnabled));
    });

    on<e.VisibilityChanged>((event, emit) {
      emit(state.copyWith(isVisible: event.isVisible));
    });

    on<e.Enable>((event, emit) async {
      emit(state.copyWith(operation: s.Operation.enabling));
      await nativeApi.startService();
      emit(state.copyWith(isEnabled: true));
      await nativeApi.showFloatingVolume();
      emit(state.copyWith(isVisible: true, operation: s.Operation.none));
    });

    on<e.Disable>((event, emit) async {
      emit(state.copyWith(operation: s.Operation.disabling));
      await nativeApi.hideFloatingVolume();
      emit(state.copyWith(isVisible: false));
      await nativeApi.stopService();
      emit(state.copyWith(isEnabled: false, operation: s.Operation.none));
    });

    on<e.Toggle>((event, emit) {
      if (state.isEnabled) {
        add(const e.Disable());
      } else {
        add(const e.Enable());
      }
    });
  }

  @override
  Future<void> close() {
    _serviceSub?.cancel();
    _visibilitySub?.cancel();
    return super.close();
  }
}
