import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:rx_shared_preferences/rx_shared_preferences.dart';
import 'package:floating_volume/pigeons_impl/appearance_api.dart'; // From pigeon
import 'event.dart';
import 'state.dart';

class AppearanceBloc extends Bloc<AppearanceEvent, AppearanceState> {
  final _prefs = RxSharedPreferences.getInstance();
  final AppearanceApi _appearanceApi = AppearanceApi();

  AppearanceBloc() : super(const AppearanceState()) {
    on<InitializeAppearance>(_onInitialize);
    on<SetBlur>(_onSetBlur);
    on<SetDynamicColors>(_onSetDynamicColors);
    on<SetCornerRadius>(_onSetCornerRadius);
    on<SetSliderOpacity>(_onSetSliderOpacity);
  }

  Future<void> _onInitialize(
      InitializeAppearance event, Emitter<AppearanceState> emit) async {
    final blur = await _prefs.getBool('appearance_blur') ?? true;
    final dynamicColors = await _prefs.getBool('appearance_dynamic_colors') ?? true;
    final radius = await _prefs.getDouble('appearance_corner_radius') ?? 100.0;
    final opacity = await _prefs.getDouble('appearance_slider_opacity') ?? 1.0;

    final newState = AppearanceState(
      enableBlur: blur,
      enableDynamicColors: dynamicColors,
      cornerRadius: radius,
      sliderOpacity: opacity,
    );

    emit(newState);
    _syncToNative(newState);
  }

  Future<void> _onSetBlur(SetBlur event, Emitter<AppearanceState> emit) async {
    await _prefs.setBool('appearance_blur', event.enabled);
    final newState = state.copyWith(enableBlur: event.enabled);
    emit(newState);
    _syncToNative(newState);
  }

  Future<void> _onSetDynamicColors(
      SetDynamicColors event, Emitter<AppearanceState> emit) async {
    await _prefs.setBool('appearance_dynamic_colors', event.enabled);
    final newState = state.copyWith(enableDynamicColors: event.enabled);
    emit(newState);
    _syncToNative(newState);
  }

  Future<void> _onSetCornerRadius(
      SetCornerRadius event, Emitter<AppearanceState> emit) async {
    await _prefs.setDouble('appearance_corner_radius', event.radius);
    final newState = state.copyWith(cornerRadius: event.radius);
    emit(newState);
    _syncToNative(newState);
  }

  Future<void> _onSetSliderOpacity(
      SetSliderOpacity event, Emitter<AppearanceState> emit) async {
    await _prefs.setDouble('appearance_slider_opacity', event.opacity);
    final newState = state.copyWith(sliderOpacity: event.opacity);
    emit(newState);
    _syncToNative(newState);
  }

  void _syncToNative(AppearanceState state) {
    _appearanceApi.updateSettings(
      ThemeSettings(
        enableBlur: state.enableBlur,
        enableDynamicColors: state.enableDynamicColors,
        cornerRadius: state.cornerRadius,
        sliderOpacity: state.sliderOpacity,
      ),
    );
  }
}
