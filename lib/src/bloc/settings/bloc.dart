import 'package:flutter_bloc/flutter_bloc.dart' as a;
import 'package:floating_volume/src/single.dart';
import 'event.dart' as e;
import 'state.dart' as s;

class Bloc extends a.Bloc<e.Event, s.State> {
  Bloc(super.initialState) {
    on<e.Event>((event, emit) async {
      switch (event) {
        case e.Initialize():
          await _handleInitialize(emit);
          break;
        case e.ToggleAutoStart():
          await _handleToggleAutoStart(emit);
          break;
        case e.ToggleRestoreState():
          await _handleToggleRestoreState(emit);
          break;
        case e.SetAutoStartDelay(:final delayMs):
          await _handleSetAutoStartDelay(delayMs, emit);
          break;
      }
    });
  }

  Future<void> _handleInitialize(a.Emitter<s.State> emit) async {
    try {
      final autoStart = await nativeApi.getAutoStartEnabled();
      final restoreState = await nativeApi.getRestoreServiceState();
      final delay = await nativeApi.getAutoStartDelay();

      emit(state.copyWith(
        autoStartEnabled: autoStart,
        restoreServiceState: restoreState,
        autoStartDelayMs: delay.toInt(),
      ));
    } catch (e) {
      // Keep default values on error
      print('Error initializing settings: $e');
    }
  }

  Future<void> _handleToggleAutoStart(a.Emitter<s.State> emit) async {
    try {
      final newValue = !state.autoStartEnabled;
      await nativeApi.setAutoStartEnabled(newValue);
      emit(state.copyWith(autoStartEnabled: newValue));
    } catch (e) {
      print('Error toggling auto-start: $e');
    }
  }

  Future<void> _handleToggleRestoreState(a.Emitter<s.State> emit) async {
    try {
      final newValue = !state.restoreServiceState;
      await nativeApi.setRestoreServiceState(newValue);
      emit(state.copyWith(restoreServiceState: newValue));
    } catch (e) {
      print('Error toggling restore state: $e');
    }
  }

  Future<void> _handleSetAutoStartDelay(int delayMs, a.Emitter<s.State> emit) async {
    try {
      await nativeApi.setAutoStartDelay(delayMs);
      emit(state.copyWith(autoStartDelayMs: delayMs));
    } catch (e) {
      print('Error setting auto-start delay: $e');
    }
  }
}
