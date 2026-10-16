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

## Phase 3 : Évolutions Futures (Backlog)
- [ ] Validation terrain sur l'autoradio du démarrage automatique en Cold Boot et en sortie de veille ACC ON (`v0.9.9`).
- [ ] Option permettant de choisir quels flux audio inclure dans la rotation du bouton canal (masquer les canaux non utilisés après diagnostic).
- [ ] Mémorisation du dernier canal audio sélectionné dans `SharedPreferences`.
