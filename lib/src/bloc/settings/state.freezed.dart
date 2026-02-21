// dart format width=80
// coverage:ignore-file
// GENERATED CODE - DO NOT MODIFY BY HAND
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'state.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$State {

 bool get autoStartEnabled; bool get restoreServiceState; int get autoStartDelayMs;
/// Create a copy of State
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$StateCopyWith<State> get copyWith => _$StateCopyWithImpl<State>(this as State, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is State&&(identical(other.autoStartEnabled, autoStartEnabled) || other.autoStartEnabled == autoStartEnabled)&&(identical(other.restoreServiceState, restoreServiceState) || other.restoreServiceState == restoreServiceState)&&(identical(other.autoStartDelayMs, autoStartDelayMs) || other.autoStartDelayMs == autoStartDelayMs));
}


@override
int get hashCode => Object.hash(runtimeType,autoStartEnabled,restoreServiceState,autoStartDelayMs);

@override
String toString() {
  return 'State(autoStartEnabled: $autoStartEnabled, restoreServiceState: $restoreServiceState, autoStartDelayMs: $autoStartDelayMs)';
}


}

/// @nodoc
abstract mixin class $StateCopyWith<$Res>  {
  factory $StateCopyWith(State value, $Res Function(State) _then) = _$StateCopyWithImpl;
@useResult
$Res call({
 bool autoStartEnabled, bool restoreServiceState, int autoStartDelayMs
});




}
/// @nodoc
class _$StateCopyWithImpl<$Res>
    implements $StateCopyWith<$Res> {
  _$StateCopyWithImpl(this._self, this._then);

  final State _self;
  final $Res Function(State) _then;

/// Create a copy of State
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? autoStartEnabled = null,Object? restoreServiceState = null,Object? autoStartDelayMs = null,}) {
  return _then(_self.copyWith(
autoStartEnabled: null == autoStartEnabled ? _self.autoStartEnabled : autoStartEnabled // ignore: cast_nullable_to_non_nullable
as bool,restoreServiceState: null == restoreServiceState ? _self.restoreServiceState : restoreServiceState // ignore: cast_nullable_to_non_nullable
as bool,autoStartDelayMs: null == autoStartDelayMs ? _self.autoStartDelayMs : autoStartDelayMs // ignore: cast_nullable_to_non_nullable
as int,
  ));
}

}


/// @nodoc


class _State extends State {
  const _State({this.autoStartEnabled = false, this.restoreServiceState = true, this.autoStartDelayMs = 3000}): super._();
  

@override@JsonKey() final  bool autoStartEnabled;
@override@JsonKey() final  bool restoreServiceState;
@override@JsonKey() final  int autoStartDelayMs;

/// Create a copy of State
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$StateCopyWith<_State> get copyWith => __$StateCopyWithImpl<_State>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _State&&(identical(other.autoStartEnabled, autoStartEnabled) || other.autoStartEnabled == autoStartEnabled)&&(identical(other.restoreServiceState, restoreServiceState) || other.restoreServiceState == restoreServiceState)&&(identical(other.autoStartDelayMs, autoStartDelayMs) || other.autoStartDelayMs == autoStartDelayMs));
}


@override
int get hashCode => Object.hash(runtimeType,autoStartEnabled,restoreServiceState,autoStartDelayMs);

@override
String toString() {
  return 'State(autoStartEnabled: $autoStartEnabled, restoreServiceState: $restoreServiceState, autoStartDelayMs: $autoStartDelayMs)';
}


}

/// @nodoc
abstract mixin class _$StateCopyWith<$Res> implements $StateCopyWith<$Res> {
  factory _$StateCopyWith(_State value, $Res Function(_State) _then) = __$StateCopyWithImpl;
@override @useResult
$Res call({
 bool autoStartEnabled, bool restoreServiceState, int autoStartDelayMs
});




}
/// @nodoc
class __$StateCopyWithImpl<$Res>
    implements _$StateCopyWith<$Res> {
  __$StateCopyWithImpl(this._self, this._then);

  final _State _self;
  final $Res Function(_State) _then;

/// Create a copy of State
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? autoStartEnabled = null,Object? restoreServiceState = null,Object? autoStartDelayMs = null,}) {
  return _then(_State(
autoStartEnabled: null == autoStartEnabled ? _self.autoStartEnabled : autoStartEnabled // ignore: cast_nullable_to_non_nullable
as bool,restoreServiceState: null == restoreServiceState ? _self.restoreServiceState : restoreServiceState // ignore: cast_nullable_to_non_nullable
as bool,autoStartDelayMs: null == autoStartDelayMs ? _self.autoStartDelayMs : autoStartDelayMs // ignore: cast_nullable_to_non_nullable
as int,
  ));
}


}

// dart format on
