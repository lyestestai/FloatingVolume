import 'package:pigeon/pigeon.dart';

@ConfigurePigeon(
  PigeonOptions(
    dartOut: 'lib/generated/native_api.g.dart',
    dartOptions: DartOptions(),
    kotlinOut:
        'android/app/src/main/kotlin/dev/flutter/floating_volume/NativeApi.g.kt',
    kotlinOptions: KotlinOptions(),
    dartPackageName: 'floating_volume',
  ),
)
@HostApi()
abstract class NativeApi {
  ///
  /// Starts the floating volume service.
  /// This method starts the android service that manages the floating volume widget.
  ///
  @async
  void startService();

  ///
  /// Stops the floating volume service.
  /// This method stops the android service that manages the floating volume widget.
  ///
  @async
  void stopService();

  ///
  /// Hides the floating volume widget.
  /// This method hides the floating volume widget, but does not stop the service.
  ///
  @async
  void hideFloatingVolume();

  ///
  /// Shows the floating volume widget.
  /// This method shows the floating volume widget, if it is hidden.
  ///
  @async
  void showFloatingVolume();

  ///
  /// Changes the maximum volume level.
  ///
  @async
  void setMaxVolume(int maxVolume);

  ///
  /// Changes the minimum volume level.
  ///
  @async
  void setMinVolume(int minVolume);

  ///
  /// Show a toast
  ///
  @async
  void showToast(
    String message, [
    ToastDuration duration = ToastDuration.short,
  ]);

  // ========== Auto-start Configuration ==========

  ///
  /// Configure si le service doit redémarrer automatiquement au boot
  ///
  @async
  void setAutoStartEnabled(bool enabled);

  ///
  /// Récupère l'état de l'auto-start
  ///
  @async
  bool getAutoStartEnabled();

  ///
  /// Définit le délai avant démarrage automatique (en millisecondes)
  ///
  @async
  void setAutoStartDelay(int delayMs);

  ///
  /// Récupère le délai d'auto-start (en millisecondes)
  ///
  @async
  int getAutoStartDelay();

  ///
  /// Configure si l'état du service doit être restauré après boot
  ///
  @async
  void setRestoreServiceState(bool enabled);

  ///
  /// Récupère l'état du restore
  ///
  @async
  bool getRestoreServiceState();

  // ========== Logs Management ==========

  ///
  /// Exporte les logs et retourne l'Intent de partage
  ///
  @async
  void exportLogsAndShare();

  ///
  /// Efface tous les logs
  ///
  @async
  void clearAllLogs();

  ///
  /// Récupère les statistiques des logs
  ///
  @async
  LogStatsData getLogStats();
}

enum ToastDuration { short, long }

/// Données des stats de logs
class LogStatsData {
  final int totalFiles;
  final int totalSizeBytes;
  final String? oldestLogDate;
  final String? newestLogDate;

  LogStatsData({
    required this.totalFiles,
    required this.totalSizeBytes,
    this.oldestLogDate,
    this.newestLogDate,
  });
}
