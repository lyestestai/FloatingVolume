# Handoff — Projet Floating Volume (`v0.9.11+101`)

Ce document résume l'état exact du projet **Floating Volume** (édition Autoradio Android 8 Oreo / Head-Unit) afin de poursuivre le développement dans une nouvelle conversation sans perte de contexte.

---

## 1. Contexte & Règles d'Or du Projet
- **Répertoire du projet :** `c:\Users\mahal\OneDrive - vaganet\ia\Antigravity\FloatingVolume`
- **Cible matérielle :** Autoradio / tablette embarquée sous **Android 8.0/8.1 Oreo (API 26/27)** (souvent masqué en « Android 12+ » sur les autoradios chinois type FYT, Topway, MTCD, XYAuto) et **Android Automotive OS (AAOS)**.
- **Version actuelle :** `0.9.11+101` (`versionCode = 101`, `versionName = "0.9.11"`)
- **Dernier APK généré :** `APK/FloatingVolume-v0.9.11-basic.apk`
- **Documentation de référence à lire impérativement :**
  - `for_agent.md` (Règles d'Or Architecturales, Processus de Release Systématique, Mémoire Agent IA)
  - `architecture.md` (Chapitre 1 : Flutter & Pigeon ; Chapitre 2 : Service Natif, Auto-Start & AudioManager + Diagrammes Mermaid)
  - `Roadmap.md` (Suivi des jalons complétés et backlog)

---

## 2. Découvertes Matérielles Majeures
1. **Le Mystère du Volume GPS / Waze ("Do Not Disturb") (`v0.9.8`) :**
   - Le blocage à zéro de `STREAM_SYSTEM`, `STREAM_NOTIFICATION` et `USAGE_ASSISTANCE_NAVIGATION_GUIDANCE` (Waze) provenait du **mode "Ne pas déranger" (Do Not Disturb - DND)** d'Android enclenché sur l'autoradio. Sa désactivation restaure immédiatement le son de Waze et le contrôle des jauges.
2. **Spécificités Autoradio Android 8 Chinois ("Fake Android 12") (`v0.9.9`) :**
   - Couper le contact (**ACC OFF**) tue de force les services tiers (`onDestroy` appelé par le MCU) avant de plonger le poste en veille profonde.
   - Remettre le contact (**ACC ON**) réveille le poste sans toujours émettre `BOOT_COMPLETED` (utilise `QUICKBOOT_POWERON`, `ACC_ON`, `USER_PRESENT`, `SCREEN_ON`), et la synchronisation GPS au démarrage fait sauter l'horloge système (`System.currentTimeMillis()`).

---

## 3. État Technique Actuel (`v0.9.11+101`)

### A. Réactivité Totale de l'UI de Configuration & Isolation Tactile (`v0.9.11`)
- **Enregistrement Pigeon Synchrone + `SupervisorJob` (`MainActivity.kt`) :** `NativeApi`, `AppearanceApi`, `MediaApi` et les `EventChannel` sont enregistrés de manière synchrone dès `configureFlutterEngine()` avec un `CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)` dédié.
- **Suppression du crash de scope dans `VisibilityBloc.kt` :** Suppression de `throw IllegalStateException("Service must be running to show the mode.")` qui annulait le `lifecycleScope` de `MainActivity` lorsque `StatusBloc` enchaînait `startService()` puis `showFloatingVolume()`.
- **Suppression de la boucle infinie de layout à 60 fps (`CoolSlider.kt`) :** `addOnLayoutChangeListener` ne déclenche plus `requestLayout()` en boucle continue lorsque la barre est affichée.
- **Isolation tactile `WindowManager` & Position par défaut à gauche (`FloatingVolumeService.kt`, `PositionBloc.kt`, `FloatingVolumeView.kt`) :** Retour strict à `FLAG_NOT_FOCUSABLE` (et `FLAG_NOT_TOUCHABLE` si masqué), position initiale par défaut à gauche (`-((screenWidth / 2) - 80)`) au lieu de `0` (plein centre par-dessus les boutons de `HomeScreen`), et hauteur adaptative pour écrans `1024x600`.
- **Tests automatisés (`test/home_interactive_test.dart`) :** Suite `flutter test` validant que tous les boutons (ON/OFF, Thèmes, Auto-Start, Slider 10s-60s, Fenêtre de Logs et Export) restent 100 % cliquables une fois la barre mise sur « ON ».

### B. Fenêtre de Diagnostic Embarquée & Export des Logs (`v0.9.10`)
- **`DiagnosticLogsWidget` (`lib/src/screens/home.dart`) :** Panneau activable/désactivable directement dans l'écran d'accueil affichant l'en-tête matériel temps réel (version Android affichée vs vrai `SDK_INT`, uptime monotone depuis le dernier Cold Boot pour distinguer veille MCU et vrai redémarrage, état des permissions et `SharedPreferences`) ainsi que l'historique horodaté des événements (`BootReceiver`, `FloatingVolumeService`, `NativeApiImpl`).
- **Boutons d'export intégrés :**
  1. **Copier** dans le presse-papiers.
  2. **Partager / Email** via `Intent.ACTION_SEND` (`text/plain`) avec le rapport complet dans `EXTRA_TEXT`.
  3. **Enregistrer (`Download/`)** : crée un fichier `FloatingVolume_Log_<timestamp>.txt` directement dans `/storage/emulated/0/Download/`.
  4. **Actualiser** et **Effacer**.

### B. Démarrage Automatique Blindé (10s – 60s) (`v0.9.9`)
1. **`AndroidManifest.xml` :** `BootReceiver` en `android:exported="true"`, `android:directBootAware="true"`, `android:priority="999"`, avec écoute de `BOOT_COMPLETED`, `LOCKED_BOOT_COMPLETED`, `QUICKBOOT_POWERON`, `USER_PRESENT`, `ACTION_POWER_CONNECTED` et des actions MCU (`com.syu.ms.action.ACC_ON`, `com.tw.action.ACC_ON`, `com.glsx.boot.ACCON`, `com.microntek.bootcheck`, `autochips.intent.action.QB_POWERON`). Ajout de `android:stopWithTask="false"` sur `FloatingVolumeService`.
2. **Suppression du verrou `restore_service_state` :** `BootReceiver` démarre désormais le service dès que `auto_start_enabled == true`, même si le MCU a arrêté le service à la coupure du contact précédente.
3. **Double détente (Foreground Immédiat + Filet de sécurité `ELAPSED_REALTIME_WAKEUP`) :**
   - `BootReceiver` lance immédiatement `FloatingVolumeService` en passant `EXTRA_BOOT_DELAY_MS = delayMs`. Le service appelle immédiatement `createNotificationChannel()` et `startForeground()` de manière synchrone sur le Main Thread, puis attend `delay(delayMs)` dans sa coroutine avant d'attacher `FloatingVolumeView`.
   - En parallèle, `BootReceiver` programme une alarme monotone `AlarmManager.ELAPSED_REALTIME_WAKEUP` (`SystemClock.elapsedRealtime() + delayMs`, insensible aux sauts d'horloge GPS) pour relancer le service si le MCU tue les processus en début de boot.
4. **Réveil d'écran dynamique (`screenWakeReceiver`) & `START_STICKY` :** `FloatingVolumeService` écoute `ACTION_SCREEN_ON` / `ACTION_USER_PRESENT` pour ré-attacher la bulle en sortie de veille MCU.
5. **Persistance synchrone `.commit()` & Délai par défaut `10000 ms` :** `NativeApiImpl.kt` et `home.dart` synchronisent immédiatement le délai (borné entre `10000` et `60000` ms) sur la mémoire flash via `.commit()`.

### C. Palette de 7 Flux Audio & Low-GPU
- Rotation manuelle sur les 7 flux Android (`STREAM_MUSIC`, `STREAM_ALARM`, `STREAM_VOICE_CALL`, `STREAM_RING`, `STREAM_SYSTEM`, `STREAM_NOTIFICATION`, `STREAM_ACCESSIBILITY`) protégée par `try-catch`.
- Zéro `FLAG_BLUR_BEHIND` par défaut et zéro animation complexe sur l'overlay pour une stabilité maximale sur GPU Android 8.

