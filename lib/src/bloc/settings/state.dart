import 'package:freezed_annotation/freezed_annotation.dart';

part 'state.freezed.dart';

@freezed
abstract class State with _$State {
  const factory State({
    @Default(false) bool autoStartEnabled,
    @Default(true) bool restoreServiceState,
    @Default(3000) int autoStartDelayMs, // 3s default
  }) = _State;

  const State._();
}
