import 'package:pigeon/pigeon.dart';

@ConfigurePigeon(PigeonOptions(
  dartOut: 'lib/pigeons_impl/appearance_api.dart',
  kotlinOut: 'android/app/src/main/kotlin/dev/flutter/floating_volume/AppearanceApi.kt',
  kotlinOptions: KotlinOptions(package: 'com.github.mkalmousli.floating_volume.pigeon_impl'),
  dartPackageName: 'floating_volume',
))
class ThemeSettings {
  bool enableBlur;
  bool enableDynamicColors;
  double cornerRadius;
  double sliderOpacity;

  ThemeSettings({
    required this.enableBlur,
    required this.enableDynamicColors,
    required this.cornerRadius,
    required this.sliderOpacity,
  });
}

@HostApi()
abstract class AppearanceApi {
  void updateSettings(ThemeSettings settings);
}
