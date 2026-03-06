import 'package:equatable/equatable.dart';

class AppearanceState extends Equatable {
  final bool enableBlur;
  final bool enableDynamicColors;
  final double cornerRadius;
  final double sliderOpacity;

  const AppearanceState({
    this.enableBlur = true,
    this.enableDynamicColors = true,
    this.cornerRadius = 100.0,
    this.sliderOpacity = 1.0,
  });

  AppearanceState copyWith({
    bool? enableBlur,
    bool? enableDynamicColors,
    double? cornerRadius,
    double? sliderOpacity,
  }) {
    return AppearanceState(
      enableBlur: enableBlur ?? this.enableBlur,
      enableDynamicColors: enableDynamicColors ?? this.enableDynamicColors,
      cornerRadius: cornerRadius ?? this.cornerRadius,
      sliderOpacity: sliderOpacity ?? this.sliderOpacity,
    );
  }

  @override
  List<Object> get props => [enableBlur, enableDynamicColors, cornerRadius, sliderOpacity];
}
