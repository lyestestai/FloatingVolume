# Journal de Mémoire Agent IA — Floating Volume (`v0.9.11+101`)

Ce fichier constitue la mémoire technique permanente destinée aux agents IA / LLM intervenant sur le projet **Floating Volume (Automotive Head-Unit Edition)**. Il doit être lu au début de chaque session et mis à jour systématiquement à chaque release.

---

## 1. Règles d'Or du Projet (Obligatoires)

> **⚠️ RÈGLE D'OR ARCHITECTURALE**
> Si une demande d'implémentation ou d'interface entre en contradiction avec l'architecture en place (**UI Configuration Flutter BLoC ↔ Pont typé Pigeon ↔ Service Overlay Natif Kotlin**) ou avec les contraintes matérielles **Android 8 (API 26/27 - Automotive Head-Unit)** (zéro animation complexe sur l'overlay, zéro flou GPU, protection stricte des appels `AudioManager`), l'agent IA **DOIT** impérativement alerter l'utilisateur et formuler **une ou deux contre-propositions** plus adaptées et sécurisées avant d'écrire la moindre ligne de code.

> **🚀 PROCESSUS DE RELEASE SYSTÉMATIQUE (RÈGLE D'OR)**
> Pour chaque version majeure ou mineure, les étapes suivantes sont **obligatoires et systématiques** :
> 1. **Implémentation** : Frontend Flutter (`lib/`), Pont d'API (`pigeons/`), Backend Natif Android (`android/.../kotlin/`) avec gestion défensive (`try-catch`).
> 2. **Tests & Vérification de Build** : `flutter analyze` et `flutter build apk --release`.
> 3. **Documentation Systématique & Vivante** : Mise à jour impérative de `architecture.md` (2 chapitres + diagrammes Mermaid) et `for_agent.md`.
> 4. **Propagation des Versions** : Synchroniser `pubspec.yaml`, `android/app/build.gradle.kts`, `lib/src/screens/home.dart`, `CHANGELOG.md`, `README.md` et `HANDOFF.md`.
> 5. **Validation & Génération APK** : Copier l'APK compilé sous `APK/FloatingVolume-vX.Y.Z-basic.apk`.
> 6. **Suivi Roadmap** : Cocher les tâches complétées dans `Roadmap.md`.
> 7. **Historique Changelog** : Documenter les changements dans `CHANGELOG.md`.
> 8. **Mémoire Agent & README** : Actualiser `for_agent.md`, `architecture.md` et `README.md`.
> 9. **Livraison** : Nettoyer les fichiers temporaires de build et effectuer un commit/push Git propre.

> **🎯 PHILOSOPHIE EMBARQUÉE AUTOMOTIVE (ANDROID 8 OREO)**
> - **Pérennité & Clean Architecture** : Séparation stricte Flutter (config) ↔ Pigeon (IPC) ↔ Service Kotlin (overlay autonome).
> - **Stabilité Matérielle & Défensive First** : Sur autoradio Android 8 (souvent masqué en Android 12 par les constructeurs chinois), chaque appel `AudioManager`, `WindowManager`, `AlarmManager` ou chargement d'icône système doit être isolé dans un bloc `try-catch`.
> - **UX & Performance Embarquée (Low-GPU)** : Rendu 2D natif ultra-léger (`CoolSlider`), zéro `FLAG_BLUR_BEHIND` par défaut, zéro `ViewPropertyAnimator` sur l'overlay, courbe de volume non-linéaire (logarithmique).

---

## 2. Spécificités Matérielles Identifiées sur l'Autoradio Cible

### A. Masquage d'OS (« Fake Android 12 » sur base Android 8.x Oreo)
- Les autoradios Android chinois (FYT, Topway TS7/TS10, MTCD/MTCE, XYAuto, Rockchip, Allwinner) affichent souvent « Android 12 » dans les paramètres système alors que le système réel repose sur **Android 8.0 / 8.1 (API 26/27)** avec un microcontrôleur (MCU) propriétaire.
- **Conséquence graphique :** Les pilotes GPU de ces postes ne supportent ni le flou matériel (`FLAG_BLUR_BEHIND`), ni le `clipToOutline` complexe, ni les `ViewPropertyAnimator` sur les fenêtres `TYPE_APPLICATION_OVERLAY`. Tout effet de ce type rend la bulle invisible ou fige l'UI.
- **Conséquence cycle de vie (ACC OFF / ACC ON) :**
  - Couper le contact (ACC OFF) plonge souvent le poste en **veille MCU** après avoir tué les processus et services tiers (`stopService` / `killBackgroundProcesses`).
  - Remettre le contact (ACC ON) réveille le poste sans toujours émettre `ACTION_BOOT_COMPLETED`, mais émet `ACTION_SCREEN_ON`, `ACTION_USER_PRESENT`, `QUICKBOOT_POWERON` ou des intents MCU (`com.syu.ms.action.ACC_ON`, `com.tw.action.ACC_ON`, `com.glsx.boot.ACCON`, `com.microntek.bootcheck`, `autochips.intent.action.QB_POWERON`).

### B. Le Mystère du Volume GPS / Waze (« Do Not Disturb »)
- **Symptôme observé (`v0.9.2` à `v0.9.7`) :** Les jauges `STREAM_SYSTEM`, `STREAM_NOTIFICATION` et le son de guidage Waze (`USAGE_ASSISTANCE_NAVIGATION_GUIDANCE`) rebondissaient à `0`.
- **Cause racine prouvée (`v0.9.8`) :** Le mode **« Ne pas déranger » (Do Not Disturb - DND)** d'Android était actif sur l'autoradio. Lorsqu'il est enclenché, Android verrouille ces canaux à `0`. La désactivation du mode DND restaure immédiatement le son de Waze et le contrôle de tous les flux.

---

## 3. Historique Technique des Réalisations (`v0.9.8` $\rightarrow$ `v0.9.11+101`)

### A. Ce qui a été corrigé et réalisé dans la `v0.9.9+99` (Réparation Complète de l'Auto-Start)
Suite au constat que le démarrage automatique ne fonctionnait pas en `v0.9.8`, un audit complet du code a révélé **6 causes cumulées**, toutes résolues dans la `v0.9.9+99` :

1. **Manifest `BootReceiver` (`AndroidManifest.xml`) :**
   - `android:exported` était à `"false"`, empêchant la réception des broadcasts système/MCU externes. Passé à `android:exported="true"`, `android:directBootAware="true"`, `android:priority="999"`.
   - Ajout des broadcasts de réveil d'autoradios chinois (`QUICKBOOT_POWERON`, `USER_PRESENT`, `ACTION_POWER_CONNECTED`, `com.syu.ms.action.ACC_ON`, `com.tw.action.ACC_ON`, `com.glsx.boot.ACCON`, `com.microntek.bootcheck`, `autochips.intent.action.QB_POWERON`).
   - Ajout de `android:stopWithTask="false"` sur `FloatingVolumeService`.

2. **Suppression du verrou bloquant `restore_service_state` / `last_service_state` (`BootReceiver.kt` & `FloatingVolumeService.kt`) :**
   - En `v0.9.8`, quand l'autoradio coupait les services à l'extinction (ACC OFF), `FloatingVolumeService.onDestroy()` écrivait `last_service_state = false`. Au démarrage suivant, `BootReceiver` vérifiait `if (restoreState && !wasServiceRunning) return` et refusait systématiquement de démarrer.
   - Désormais, dès que `auto_start_enabled == true`, `BootReceiver` démarre inconditionnellement le service. De plus, `FloatingVolumeService` distingue un arrêt volontaire de l'utilisateur (`isStoppingIntentionally = true`) d'un kill OS/MCU.

3. **Architecture Auto-Start à Double Détente (`BootReceiver.kt` & `FloatingVolumeService.kt`) :**
   - **Étape 1 (Protection Foreground immédiate + délai interne) :** Dès la réception de `BOOT_COMPLETED` / `ACC_ON`, `BootReceiver` lance immédiatement `FloatingVolumeService` via `startForegroundService` en lui passant `EXTRA_BOOT_DELAY_MS = delayMs` (10s à 60s). Le service appelle **immédiatement et de manière synchrone** `createNotificationChannel()` puis `startForeground()` sur le thread principal (respectant la règle des 5 secondes d'Android 8+ et l'exemption de boot d'Android 12+), puis attend `delay(delayMs)` dans sa coroutine avant d'initialiser `AudioManager` et d'attacher `FloatingVolumeView` au `WindowManager`.
   - **Étape 2 (Alarme monotone `ELAPSED_REALTIME_WAKEUP`) :** En parallèle, `BootReceiver` programme une alarme `AlarmManager.ELAPSED_REALTIME_WAKEUP` basée sur `SystemClock.elapsedRealtime() + delayMs` (insensible aux sauts d'horloge GPS/NTP au démarrage de la voiture, contrairement à `RTC_WAKEUP`). Si le MCU tue les processus à `boot + 5s`, cette alarme ressuscite le service à `+delayMs`.

4. **Création autonome du `NotificationChannel` et `startForeground()` synchrone (`FloatingVolumeService.kt`) :**
   - En `v0.9.8`, seul `MainActivity` créait le `NotificationChannel`, et `FloatingVolumeService` appelait `startForeground()` de manière asynchrone dans un `scope.inIO { VisibilityBloc.state.collectLatest { ... } }`.
   - En `v0.9.9`, `FloatingVolumeService` crée lui-même son `NotificationChannel` et appelle `promoteToForeground()` de manière synchrone dès `onCreate()` et à chaque `onStartCommand()`, et retourne `START_STICKY`.

5. **Réveil d'écran dynamique (`screenWakeReceiver`) & `onTaskRemoved` (`FloatingVolumeService.kt`) :**
   - Le service écoute dynamiquement `ACTION_SCREEN_ON` et `ACTION_USER_PRESENT` pour ré-attacher la bulle si le MCU l'a détachée pendant une veille, et programme une relance automatique dans `onTaskRemoved` / `onDestroy` non intentionnel.

6. **Persistance synchrone `.commit()` et alignement du délai par défaut à `10000 ms` (`NativeApiImpl.kt` & `home.dart`) :**
   - Remplacement de `DEFAULT_DELAY_MS = 3000L` par `10_000L` (borné `10_000L..60_000L`).
   - Utilisation de `.commit()` au lieu de `.apply()` sur le thread IO pour garantir l'écriture physique (`fsync`) sur la mémoire flash de l'autoradio avant une coupure franche du contact.
   - Suppression du flag `FLAG_BLUR_BEHIND` par défaut dans `FloatingVolumeService.layoutParams` et nettoyage des fuites de `CoroutineScope` (`SupervisorJob` unifié).

### B. Ce qui a été réalisé dans la `v0.9.10+100` (Fenêtre de Diagnostic & Journalisation Embarquée)
Pour analyser le comportement exact de l'autoradio au démarrage (distinguer un vrai Cold Boot d'une veille MCU, voir quels broadcasts sont reçus et vérifier l'état des permissions) sans avoir besoin de brancher un PC en ADB dans la voiture :

1. **Journalisation persistante dès la milliseconde 0 du boot (`CrashHandler.kt`) :**
   - Ajout de `CrashHandler.logEvent(ctx, tag, message)` qui écrit de manière synchronisée dans le stockage interne privé de l'application (`ctx.filesDir/logs/diagnostic_log_current.txt`), disponible immédiatement dès `BOOT_COMPLETED` / `LOCKED_BOOT_COMPLETED` avant même que la partition `/sdcard/` ne soit montée.
   - Chaque ligne de log inclut à la fois l'horodatage système (`yyyy-MM-dd HH:mm:ss.SSS`) et l'uptime monotone depuis le dernier Cold Boot (`[up:Xs]`), permettant de détecter immédiatement un saut d'horloge GPS ou de savoir si le poste sort d'une veille MCU (`up` élevé) ou d'un vrai redémarrage (`up` faible).
   - Instrumentation complète de `BootReceiver.kt`, `FloatingVolumeService.kt` et `NativeApiImpl.kt`.

2. **En-tête de Diagnostic Matériel Temps Réel (`CrashHandler.getFullDiagnosticReport`) :**
   - Génère automatiquement en tête du journal : la version Android affichée (`Build.VERSION.RELEASE`) vs le vrai niveau d'API (`Build.VERSION.SDK_INT`), le modèle matériel/carte mère (`Manufacturer`, `Model`, `Device`, `Board`, `Hardware`), l'uptime exact depuis le dernier Cold Boot, l'état des permissions (`Settings.canDrawOverlays`, `isIgnoringBatteryOptimizations`), l'état des `SharedPreferences` et l'état actif de `FloatingVolumeService`.

3. **Fenêtre de Logs pliable dans `HomeScreen` (`DiagnosticLogsWidget` dans `lib/src/screens/home.dart` & contrat Pigeon `pigeons/native_api.dart`) :**
   - Ajout d'un interrupteur `SwitchListTile` *"Journal de diagnostic (Logs)"* persisté via `setLogsEnabled` / `getLogsEnabled`.
   - Lorsqu'il est activé, affiche directement un terminal déroulant (`SelectableText` monospace) accompagné de 5 boutons d'action :
     - **Copier** : copie l'intégralité du rapport dans le presse-papiers Android.
     - **Partager / Email** : ouvre le sélecteur de partage Android (`Intent.ACTION_SEND` `text/plain` avec le rapport dans `Intent.EXTRA_TEXT`) compatible avec les clients email et éditeurs de texte.
     - **Enregistrer (`Download/`)** : écrit directement un fichier `FloatingVolume_Log_<timestamp>.txt` dans `/storage/emulated/0/Download/` sur le disque de l'autoradio.
     - **Actualiser** & **Effacer**.

### C. Ce qui a été corrigé et réalisé dans la `v0.9.11+101` (Correctif Majeur : UI Configuration Non Cliquable / Gel des Boutons une fois sur « ON »)
Suite au retour terrain sur la `v0.9.9` (*« l'interface de configuration n'est pas cliquable : le bouton de démarrage auto ne se lance pas, aucun autre bouton ne fonctionne une fois la barre mise à "ON" »*), un audit de bout en bout (Kotlin + Flutter) a identifié et corrigé **6 causes racines cumulées** :

1. **Exception fatale `IllegalStateException` dans `VisibilityBloc.kt` qui tuait `MainActivity.lifecycleScope` dès la mise sur « ON » (`VisibilityBloc.kt`, `NativeApiImpl.kt`, `MainActivity.kt`) :**
   - **Cause :** Quand l'utilisateur activait la barre (« ON »), `StatusBloc` appelait `await nativeApi.startService()` puis immédiatement (1 ms après) `await nativeApi.showFloatingVolume()`. Or `startForegroundService()` est asynchrone côté Android : `FloatingVolumeService` n'avait pas encore passé `ServiceStatusBloc.state` à `State.On`. `VisibilityBloc` exécutait alors `if (!isServiceRunning) throw IllegalStateException("Service must be running to show the mode.")` à l'intérieur de `NativeApiImpl.showFloatingVolume()` qui tournait sur le `lifecycleScope` de `MainActivity` sans `SupervisorJob`. Cette exception non interceptée annulait définitivement le `CoroutineScope` de `NativeApiImpl` : **tous les appels Pigeon suivants (`setAutoStartEnabled`, `setAutoStartDelay`, `showToast`, etc.) restaient figés pour toujours dès que la barre était mise sur « ON »**.
   - **Correctif :** Suppression du `throw IllegalStateException` dans `VisibilityBloc.kt` (remplacé par des méthodes synchrones `VisibilityBloc.show()` / `hide()` / `toggle()`), remplacement de `lifecycleScope.apply { inMain { NativeApi.setUp(...) } }` dans `MainActivity.kt` par un enregistrement **synchrone** dans `configureFlutterEngine(flutterEngine)` adossé à un `CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)` dédié, et protection de chaque méthode de `NativeApiImpl.kt` par `try-catch`.

2. **Enregistrement tardif des canaux Pigeon au démarrage de `MainActivity` (`MainActivity.kt`) :**
   - **Cause :** En `v0.9.9`, `NativeApi.setUp` était lancé dans `lifecycleScope.inMain { ... }` (posté dans la file d'attente du thread principal Android), donc *après* l'exécution du `initState()` des widgets Flutter (`AutoStartSettingsWidget`).
   - **Correctif :** Enregistrement synchrone immédiat dans `configureFlutterEngine()`.

3. **Boucle infinie de layout à 60 fps dans `CoolSlider.kt` saturant le Main Thread dès l'affichage de la barre (`CoolSlider.kt`) :**
   - **Cause :** `CoolSlider.init` utilisait `addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> updateProgress() }`, et `updateProgress()` appelait systématiquement `progressView.updateLayoutParams { height = progressSize }` (`setLayoutParams` $\rightarrow$ `requestLayout()`), déclenchant un nouveau pass de layout à chaque frame (60 fois par seconde) tant que la barre était affichée (« ON »), affamant le thread UI de `MainActivity`.
   - **Correctif :** `addOnLayoutChangeListener` vérifie désormais si la hauteur a réellement changé (`oldBottom - oldTop != bottom - top`), et `updateProgress()` ne modifie `layoutParams.height` que si `currentParams.height != progressSize`.

4. **Flags `WindowManager.LayoutParams` trop larges et position initiale plein centre (`FloatingVolumeService.kt`, `PositionBloc.kt`, `FloatingVolumeView.kt`) :**
   - **Cause :** En `v0.9.9`, `layoutParams.flags` combinait `FLAG_NOT_FOCUSABLE or FLAG_NOT_TOUCH_MODAL or FLAG_LAYOUT_IN_SCREEN or FLAG_LAYOUT_NO_LIMITS` avec `Gravity.CENTER` et une position par défaut `(x = 0, y = 0)`. Dès l'activation, la barre flottante apparaissait **exactement au milieu de l'écran d'accueil**, par-dessus le bouton ON/OFF, les boutons de thème et le switch Auto-Start, tout en interceptant les touchers.
   - **Correctif :** Retour strict au flag `LayoutParams.FLAG_NOT_FOCUSABLE` (et `FLAG_NOT_FOCUSABLE or FLAG_NOT_TOUCHABLE` quand masqué), position par défaut placée sur le bord gauche de l'écran (`-((screenWidth / 2) - 80)`), hauteur du slider adaptative (`(screenHeight * 0.55f).toInt().coerceIn(220, 800)`) pour les écrans d'autoradio `1024x600`, et sauvegarde disque uniquement au relâchement du doigt (`ACTION_UP` / `ACTION_CANCEL`).

5. **File d'événements bloquée dans `StatusBloc` et `PermissionsBloc` côté Flutter (`lib/src/bloc/status/bloc.dart` & `lib/src/bloc/permissions/bloc.dart`) :**
   - **Cause :** Le handler `on<e.Event>` faisait un `await Future.wait([...])` sur des flux infinis (`await for` et `while(true)`), bloquant tout nouvel événement BLoC si un seul canal natif tardait à répondre.
   - **Correctif :** Remplacement par des `StreamSubscription` non bloquantes et déduplication des émissions du `Timer.periodic`.

6. **Initialisation immédiate des BLoCs (`lazy: false`) & Correction du symlink `images/logo.svg` (`lib/src/app.dart`, `lib/src/screens/home.dart`, `images/logo.svg`) :**
   - **Cause découverte par `flutter test` en résolution autoradio `1024x600` :** Sur un écran paysage de `600px` de haut, la tuile `Manage Permissions` (`BlocBuilder<bpermissions.Bloc>`) située tout en bas de la `ListView` est hors-écran (non construite). Comme `BlocProvider` est `lazy: true` par défaut, `bpermissions.Bloc` n'était jamais initialisé tant que l'utilisateur n'avait pas fait défiler la page jusqu'en bas, bloquant le bouton principal ON/OFF au premier clic. De plus, `images/logo.svg` était un lien symbolique Unix cassé sous Windows (`11 octets`), provoquant une erreur de parsing SVG sur l'écran d'accueil.
   - **Correctif :** Passage des trois `BlocProvider` en `lazy: false` dans `lib/src/app.dart`, autorisation d'extinction immédiate de la barre même sans vérification de permission, et remplacement de `images/logo.svg` par le vrai fichier SVG vectoriel.

7. **Tests Automatisés d'Interactivité (`test/home_interactive_test.dart`) :**
   - Ajout d'une suite de tests de widgets automatisée (`flutter test`) simulant un écran d'autoradio `1024x600`, l'activation de la barre sur « ON », puis le clic sur les boutons de thème, le switch Auto-Start, le glissement du slider de délai (10s–60s), l'ouverture de la fenêtre de logs, les boutons d'export et la remise sur « OFF ».
