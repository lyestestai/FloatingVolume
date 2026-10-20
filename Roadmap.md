# Roadmap — Floating Volume (Automotive Head-Unit Edition)

## Phase 1 : Stabilité Graphique & Audio Embarquée (`v0.9.2` – `v0.9.8`)
- [x] Suppression des effets GPU lourds (`ViewPropertyAnimator`, `clipToOutline`, Material 3 blur) pour compatibilité Android 8 Oreo / Autoradios chinois.
- [x] Courbe d'interpolation du volume non-linéaire (logarithmique) adaptée à la conduite.
- [x] Sélecteur multi-canaux (7 flux audio Android : Musique, Alarme, Appel, Sonnerie, Système, Notification, Accessibilité).
- [x] Encapsulation défensive (`try-catch`) de tous les appels `AudioManager` et chargements d'icônes système.
- [x] Identification et résolution du blocage GPS/Waze lié au mode *Do Not Disturb (DND)* d'Android.
- [x] Interface Flutter de configuration du Démarrage Automatique (Switch + Slider 10s–60s) et contrat Pigeon associé.

## Phase 2 : Fiabilisation Auto-Start Autoradio Android 8+ & Documentation Vivante (`v0.9.9+99`)
- [x] Audit complet de l'application, du code et de l'architecture.
- [x] Passage de `BootReceiver` en `android:exported="true"` et `android:directBootAware="true"`.
- [x] Prise en charge des broadcasts de sortie de veille MCU des autoradios chinois (`QUICKBOOT_POWERON`, `ACC_ON`, `USER_PRESENT`, `SCREEN_ON`).
- [x] Suppression du verrou bloquant `restore_service_state` / `last_service_state` lors des coupures contact (ACC OFF).
- [x] Démarrage Foreground synchrone immédiat + délai interne dans `FloatingVolumeService` + création autonome du `NotificationChannel`.
- [x] Filet de sécurité `AlarmManager` sur horloge monotone (`ELAPSED_REALTIME_WAKEUP`) insensible aux sauts d'horloge GPS au boot.
- [x] Persistance synchrone (`.commit()`) des préférences et alignement du délai par défaut sur 10 secondes (`10000 ms`).
- [x] Création et mise à jour de `architecture.md`, `for_agent.md`, `Roadmap.md`, `CHANGELOG.md`, `README.md` et `HANDOFF.md`.
- [x] Compilation et livraison de `APK/FloatingVolume-v0.9.9-basic.apk`.

## Phase 3 : Fenêtre de Diagnostic Embarquée & Export des Logs (`v0.9.10+100`)
- [x] Journalisation persistante dans le stockage interne (`diagnostic_log_current.txt`) fonctionnelle dès la milliseconde 0 du boot (`BOOT_COMPLETED` / `ACC_ON`).
- [x] En-tête de rapport matériel temps réel (`SDK_INT` réel vs version affichée, uptime monotone `elapsedRealtime` pour distinguer Cold Boot et veille MCU, permissions, préférences).
- [x] Panneau pliable `DiagnosticLogsWidget` activable/désactivable dans l'écran d'accueil Flutter (`HomeScreen`).
- [x] Actions **Copier** (presse-papiers), **Partager / Email** (`ACTION_SEND` `text/plain`), **Enregistrer (`Download/`)** (`.txt` sur le disque), **Actualiser** et **Effacer**.
- [x] Compilation et livraison de `APK/FloatingVolume-v0.9.10-basic.apk`.

## Phase 4 : Réactivité de l'Interface de Configuration & Isolation Tactile (`v0.9.11+101`)
- [x] Enregistrement synchrone des canaux Pigeon dès `MainActivity.configureFlutterEngine` avec `SupervisorJob` dédié.
- [x] Suppression de l'`IllegalStateException` dans `VisibilityBloc.kt` qui annulait le `CoroutineScope` de `MainActivity` lors du passage rapide de `startService()` à `showFloatingVolume()`.
- [x] Suppression de la boucle infinie de layout à 60 fps dans `CoolSlider.kt` (`addOnLayoutChangeListener`).
- [x] Restauration des flags stricts `LayoutParams.FLAG_NOT_FOCUSABLE` et positionnement par défaut sur le bord gauche pour ne jamais recouvrir les contrôles de `HomeScreen`.
- [x] Refactorisation non-bloquante de `StatusBloc` et `PermissionsBloc` (`StreamSubscription`) et suppression du verrou `_isLoading` sur `AutoStartSettingsWidget`.
- [x] Création et validation des tests automatisés de widgets (`test/home_interactive_test.dart` via `flutter test`).
- [x] Compilation et livraison de `APK/FloatingVolume-v0.9.11-basic.apk`.

## Phase 5 : Évolutions Futures (Backlog)
- [ ] Validation terrain sur l'autoradio du démarrage automatique en Cold Boot et en sortie de veille ACC ON (`v0.9.11`).
- [ ] Option permettant de choisir quels flux audio inclure dans la rotation du bouton canal (masquer les canaux non utilisés après diagnostic).
- [ ] Mémorisation du dernier canal audio sélectionné dans `SharedPreferences`.

