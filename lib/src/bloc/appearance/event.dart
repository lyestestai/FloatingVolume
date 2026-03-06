import 'package:equatable/equatable.dart';

abstract class AppearanceEvent extends Equatable {
  const AppearanceEvent();

  @override
  List<Object> get props => [];
}

class InitializeAppearance extends AppearanceEvent {
  const InitializeAppearance();
}

class SetBlur extends AppearanceEvent {
  final bool enabled;
  const SetBlur(this.enabled);

  @override
  List<Object> get props => [enabled];
}

class SetDynamicColors extends AppearanceEvent {
  final bool enabled;
  const SetDynamicColors(this.enabled);

  @override
  List<Object> get props => [enabled];
}

class SetCornerRadius extends AppearanceEvent {
  final double radius;
  const SetCornerRadius(this.radius);

  @override
  List<Object> get props => [radius];
}

class SetSliderOpacity extends AppearanceEvent {
  final double opacity;
  const SetSliderOpacity(this.opacity);

  @override
  List<Object> get props => [opacity];
}
