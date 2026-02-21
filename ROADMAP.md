# 🚀 FloatingVolume - Roadmap d'améliorations

## Version actuelle : 0.5.0-rc1

---

## 📋 Table des matières
- [Règles de gestion des versions](#-règles-de-gestion-des-versions)
- [v0.6.0 - Fondations & Auto-démarrage](#v060---fondations--auto-démarrage)
- [v0.7.0 - Contrôles multimédia](#v070---contrôles-multimédia)
- [v0.8.0 - Design & UX](#v080---design--ux)
- [v0.9.0 - Personnalisation avancée](#v090---personnalisation-avancée)
- [v1.0.0 - Release stable](#v100---release-stable)
- [v1.1.0+ - Fonctionnalités futures](#v110---fonctionnalités-futures)
- [Idées à long terme](#idées-à-long-terme)

---

## 📌 Règles de gestion des versions

### ⚠️ IMPORTANT : Finalisation obligatoire de chaque version

**Avant toute release, les 3 étapes suivantes sont OBLIGATOIRES :**

#### 1️⃣ Mise à jour du CHANGELOG.md
- Ajouter une nouvelle section `## [X.Y.Z] - YYYY-MM-DD`
- Suivre le format [Keep a Changelog](https://keepachangelog.com/):
  - **Added** - Nouvelles fonctionnalités
  - **Changed** - Modifications de fonctionnalités existantes
  - **Deprecated** - Fonctionnalités bientôt supprimées
  - **Removed** - Fonctionnalités supprimées
  - **Fixed** - Corrections de bugs
  - **Security** - Corrections de vulnérabilités
- Exemple :
  ```markdown
  ## [0.6.0] - 2026-03-15

  ### Added
  - Auto-démarrage au boot de l'appareil
  - Export des logs vers répertoire accessible
  - Option de délai configurable avant démarrage

  ### Changed
  - Amélioration de la gestion des permissions

  ### Fixed
  - Correction du crash sur tablettes au démarrage
  ```

#### 2️⃣ Mise à jour du README.md
- Mettre à jour le numéro de version (badge et texte)
- Ajouter/mettre à jour les screenshots si nouvelles fonctionnalités visuelles
- Actualiser la section "Fonctionnalités" avec les nouveautés
- Vérifier que tous les liens fonctionnent
- Mettre à jour la section "Installation" si nécessaire

#### 3️⃣ Tests finaux
- Tests de régression complets
- Validation sur plusieurs appareils (minimum 3)
- Vérification de compatibilité Android (versions supportées)
- Tests de performance (batterie, RAM, temps de démarrage)

### 📦 Versioning sémantique (SemVer)

Nous suivons le [Semantic Versioning 2.0.0](https://semver.org/) :
- **MAJOR** (X.0.0) - Changements incompatibles avec versions précédentes
- **MINOR** (0.X.0) - Nouvelles fonctionnalités rétro-compatibles
- **PATCH** (0.0.X) - Corrections de bugs rétro-compatibles

Exemples :
- `v0.6.0` → Nouvelles fonctionnalités (auto-start, logs)
- `v0.6.1` → Correction de bugs uniquement
- `v1.0.0` → Release stable majeure

### 🏷️ Tags et Releases

- Chaque version doit avoir un **tag git** : `git tag -a v0.6.0 -m "Release v0.6.0"`
- Créer une **GitHub Release** avec :
  - Titre : `FloatingVolume v0.6.0`
  - Description : Copie de la section CHANGELOG correspondante
  - APK attaché (debug pour bêta, release signé pour stable)

---

## v0.6.0 - Fondations & Auto-démarrage
**Focus:** Améliorer la fiabilité et l'accessibilité

### 🎯 Priorité 1 - Critique

#### ✅ Auto-démarrage au boot
- **Fonctionnalité:** Démarrage automatique du service au démarrage de l'appareil
- **Implémentation:**
  - Permission `RECEIVE_BOOT_COMPLETED`
  - BroadcastReceiver pour `ACTION_BOOT_COMPLETED`
  - Option activable/désactivable dans les paramètres
  - Délai configurable avant démarrage (0-30s)
- **Fichiers concernés:**
  - `AndroidManifest.xml` - Déclaration receiver
  - Nouveau: `BootReceiver.kt`
  - `HomeScreen.dart` - Toggle auto-start dans UI
- **Complexité:** Faible
- **Estimation:** 2-3h

#### ✅ Logs accessibles
- **Problème actuel:** Crash logs dans `/Android/data/` (inaccessible sans root depuis Android 11)
- **Solution:**
  - Migrer vers `getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)/FloatingVolume/logs/`
  - Ou utiliser `MediaStore API` pour accès public
  - Ajouter bouton "Exporter les logs" dans l'app (partage via Intent)
  - Ajouter bouton "Effacer les logs"
  - Limite de taille (max 5 MB, rotation automatique)
- **Fichiers concernés:**
  - `CrashHandler.kt:22` - Changer le chemin
  - `HomeScreen.dart` - Ajouter section "Logs & Debug"
- **Complexité:** Faible
- **Estimation:** 1-2h

### 🎯 Priorité 2 - Importante

#### ✅ Gestion améliorée des permissions
- Wizard d'onboarding pour les nouvelles installations
- Explications claires pour chaque permission
- Deep links vers les paramètres Android concernés
- Vérification périodique des permissions (au cas où révoquées)

#### ✅ Persistance améliorée
- Sauvegarder l'état du service (On/Off) dans SharedPreferences
- Restaurer automatiquement l'état après redémarrage
- Option "Toujours démarrer en mode activé/désactivé"

#### ✅ Tests et stabilité
- Correction des warnings Kotlin (upgrade vers 2.1.0+)
- Tests unitaires pour les BLOCs critiques
- Tests d'intégration pour les scenarios d'usage courants

### 📝 Finalisation de version

**Avant release v0.6.0 :**
- [ ] **Mise à jour CHANGELOG.md**
  - Ajouter section `## [0.6.0] - YYYY-MM-DD`
  - Lister toutes les nouvelles fonctionnalités (Added)
  - Lister les modifications (Changed)
  - Lister les corrections de bugs (Fixed)

- [ ] **Mise à jour README.md**
  - Mettre à jour le numéro de version
  - Ajouter screenshots des nouvelles fonctionnalités
  - Mettre à jour la section "Fonctionnalités"
  - Vérifier les liens et badges

- [ ] **Tests finaux**
  - Tester sur plusieurs appareils (min 3 différents)
  - Vérifier compatibilité Android 8-14
  - Tests de régression complets

---

## v0.7.0 - Contrôles multimédia
**Focus:** Contrôle de lecture audio depuis le widget flottant

### 🎯 Priorité 1 - Critique

#### ✅ Boutons de contrôle média
- **Fonctionnalité:** Contrôle de lecture lorsque média actif
- **Implémentation:**
  - 3 boutons compacts: ⏮️ Précédent | ⏯️ Play/Pause | ⏭️ Suivant
  - Affichage conditionnel (seulement si `STREAM_MUSIC` actif)
  - Utilisation de `MediaController` / `MediaSession` Android
  - Détection automatique du lecteur actif (Spotify, YouTube Music, etc.)
  - Animation de transition lors apparition/disparition des boutons

- **Layout proposé:**
  ```
  ┌─────┐
  │  🔊 │ ← Poignée (drag)
  ├─────┤
  │  │  │ ← Slider vertical
  │  │  │
  │  │  │
  ├─────┤
  │ ⏮️  │ ← Previous (si musique active)
  │ ⏯️  │ ← Play/Pause
  │ ⏭️  │ ← Next
  └─────┘
  ```

- **Fichiers concernés:**
  - `FloatingVolumeView.kt` - Ajout des boutons
  - Nouveau: `MediaControlBloc.kt` - Gestion état lecture
  - Nouveau: `MediaControllerManager.kt` - Interface avec MediaSession
  - `FloatingVolumeService.kt` - Intégration

- **Défis techniques:**
  - Détecter le lecteur actif (NotificationListenerService ?)
  - Gérer les lecteurs qui ne supportent pas MediaSession
  - Design compact pour ne pas encombrer l'interface

- **Complexité:** Moyenne-Haute
- **Estimation:** 8-12h

#### ✅ Métadonnées du média en cours
- Affichage optionnel (swipe pour révéler):
  - Titre de la chanson
  - Artiste
  - Pochette d'album (miniature)
- Timeout auto (3s) puis masquage
- Option désactivable dans paramètres

### 🎯 Priorité 2 - Importante

#### ✅ Gestion avancée des lecteurs
- Sélection du lecteur prioritaire si plusieurs actifs
- Liste blanche/noire des apps à contrôler
- Fallback gracieux si lecteur ne répond pas

#### ✅ Feedback visuel
- Animation lors des actions (pulse sur bouton pressé)
- Indicateur de buffering/chargement
- Toast discret pour confirmer l'action

### 📝 Finalisation de version

**Avant release v0.7.0 :**
- [ ] **Mise à jour CHANGELOG.md**
  - Ajouter section `## [0.7.0] - YYYY-MM-DD`
  - Détailler les contrôles multimédia ajoutés
  - Lister les améliorations de l'UI
  - Documenter les nouvelles permissions si nécessaires

- [ ] **Mise à jour README.md**
  - Mettre à jour le numéro de version
  - Ajouter GIF/vidéo des contrôles média en action
  - Mettre à jour la section "Contrôles"
  - Ajouter apps testées compatibles (Spotify, YouTube Music, etc.)

- [ ] **Tests finaux**
  - Tester avec 5+ lecteurs média différents
  - Vérifier les transitions d'affichage/masquage des boutons
  - Tests de performance (pas de lag lors du contrôle)

---

## v0.8.0 - Design & UX
**Focus:** Modernisation de l'interface et personnalisation visuelle

### 🎯 Priorité 1 - Critique

#### ✅ Refonte visuelle du widget flottant
- **Design moderne:**
  - Coins arrondis (border radius configurable)
  - Effet glassmorphism (blur background optionnel)
  - Ombres portées (Material Design 3)
  - Support du Material You (couleurs dynamiques Android 12+)

- **Options de thème:**
  - Thème système (déjà implémenté)
  - Thèmes prédéfinis: Minimal, Colorful, Dark AMOLED, Transparent
  - Création de thèmes custom (couleurs, opacité, formes)

- **Personnalisation du slider:**
  - Choix de forme: Rectangulaire, Arrondi, Circulaire
  - Couleur de la barre de progression
  - Couleur du fond
  - Épaisseur du slider (40-80px)
  - Hauteur du slider (600-1200px)

#### ✅ Amélioration de la poignée de drag
- **Design actuel:** Basique
- **Améliorations:**
  - Indicateur visuel plus clair (icône haptique)
  - Pulse subtil pour indiquer l'interactivité
  - Couleur selon l'état (idle, dragging, active)
  - Option d'affichage du pourcentage de volume

#### ✅ Animations fluides
- Transition douce lors du toggle show/hide (slide + fade)
- Animation de l'icône de volume selon le niveau (🔇 → 🔉 → 🔊)
- Ripple effect sur les boutons
- Spring animation pour le drag (effet élastique)

### 🎯 Priorité 2 - Importante

#### ✅ Refonte de l'écran d'accueil (HomeScreen)
- **Design actuel:** Fonctionnel mais basique
- **Améliorations:**
  - Layout moderne avec Material Design 3
  - Cartes regroupées par catégories:
    - État du service
    - Apparence & Personnalisation
    - Contrôles & Comportement
    - Permissions & Sécurité
    - À propos & Support
  - Animations de transitions entre écrans
  - Bottom navigation ou tabs pour sections

#### ✅ Mode preview en temps réel
- Aperçu du widget dans l'app avant de l'activer
- Modification en direct des paramètres visuels
- Reset vers paramètres par défaut

#### ✅ Presets et templates
- Bibliothèque de configurations prédéfinies
- Import/Export de thèmes (JSON)
- Partage de thèmes avec la communauté (QR code)

### 📝 Finalisation de version

**Avant release v0.8.0 :**
- [ ] **Mise à jour CHANGELOG.md**
  - Ajouter section `## [0.8.0] - YYYY-MM-DD`
  - Détailler la refonte visuelle (Material You, glassmorphism)
  - Lister les nouvelles options de personnalisation
  - Documenter les nouveaux thèmes disponibles

- [ ] **Mise à jour README.md**
  - Mettre à jour le numéro de version
  - Ajouter galerie de screenshots avec différents thèmes
  - Mettre à jour la section "Personnalisation"
  - Ajouter section "Thèmes" avec exemples visuels

- [ ] **Tests finaux**
  - Tester tous les thèmes prédéfinis
  - Vérifier rendu sur Android 12+ (Material You)
  - Tests de performance des animations
  - Validation du mode preview en temps réel

---

## v0.9.0 - Personnalisation avancée
**Focus:** Options de configuration et comportements intelligents

### 🎯 Priorité 1 - Critique

#### ✅ Positionnement intelligent
- **Position initiale configurable:**
  - Gauche/Droite de l'écran
  - Haut/Centre/Bas
  - Position personnalisée (coordonnées X, Y)

- **Magnétisme aux bords:**
  - Snap automatique au bord le plus proche
  - Force du magnétisme configurable (Off, Faible, Forte)
  - Zones d'exclusion (ne pas snap ici)

- **Multi-écrans / Tablettes:**
  - Positions différentes selon orientation (portrait/paysage)
  - Position par écran (principal, secondaire pour foldables)
  - Ajustement automatique de la taille selon DPI

#### ✅ Profils de volume
- **Fonctionnalité:** Switcher rapidement entre profils prédéfinis
- **Exemples de profils:**
  - Silencieux (0% tous canaux)
  - Bureau (30% média, 80% appels, 50% notifications)
  - Maison (80% média, 100% appels, 30% notifications)
  - Nuit (0% média, 100% appels, 10% alarmes)
  - Custom 1-3

- **Activation:**
  - Long press sur la poignée → Menu de sélection rapide
  - Quick Settings Tile
  - Automation via Tasker/Locale

#### ✅ Gestures et raccourcis
- **Gestures supportés:**
  - Tap court: Toggle visibilité (déjà implémenté)
  - Long press: Menu contextuel (profils, paramètres rapides)
  - Double tap: Mute/Unmute
  - Swipe horizontal: Changer de canal audio (option)
  - Swipe vertical sur poignée: Ajustement fin du volume

- **Vibration haptique:**
  - Feedback lors des gestures
  - Vibration au min/max volume
  - Intensité configurable

### 🎯 Priorité 2 - Importante

#### ✅ Règles d'automatisation
- Afficher/masquer automatiquement selon:
  - Application au premier plan (whitelist/blacklist)
  - État du média (masquer si pas de musique depuis 5min)
  - État du téléphone (masquer en appel, en charge, écran verrouillé)
  - Orientation (masquer en paysage pour les vidéos)

- Profils horaires:
  - 8h-18h: Profil Bureau
  - 18h-23h: Profil Maison
  - 23h-8h: Profil Nuit

#### ✅ Quick Settings Tile
- Tile dans le panneau de raccourcis Android
- Actions rapides:
  - Toggle service On/Off
  - Toggle visibilité
  - Switcher entre profils
- Long press → Ouvrir l'app

#### ✅ Widget de bureau Android
- Widget classique (non-flottant) pour l'écran d'accueil
- Slider vertical ou horizontal
- Tailles: 1x2, 1x4, 2x2, 2x4
- Actions rapides (profils, mute)

### 📝 Finalisation de version

**Avant release v0.9.0 :**
- [ ] **Mise à jour CHANGELOG.md**
  - Ajouter section `## [0.9.0] - YYYY-MM-DD`
  - Détailler les profils de volume et gestures
  - Lister les fonctionnalités d'automatisation
  - Documenter le Quick Settings Tile et widget bureau

- [ ] **Mise à jour README.md**
  - Mettre à jour le numéro de version
  - Ajouter guide des gestures supportés
  - Documenter les profils prédéfinis
  - Ajouter section "Automatisation" avec exemples

- [ ] **Tests finaux**
  - Tester tous les gestures sur différents appareils
  - Vérifier le Quick Settings Tile
  - Tests de profils horaires sur 24h
  - Validation du widget bureau (toutes les tailles)

---

## v1.0.0 - Release stable
**Focus:** Stabilisation, performance et documentation

### 🎯 Checklist de release

#### ✅ Code & Performance
- [ ] Upgrade Kotlin vers 2.1.0+
- [ ] Optimisation des coroutines (éviter memory leaks)
- [ ] Profiling et réduction de la consommation batterie
- [ ] Minification R8 optimisée
- [ ] Obfuscation ProGuard
- [ ] Tests de compatibilité Android 8-15
- [ ] Tests sur appareils variés (Samsung, Xiaomi, OnePlus, Google Pixel)

#### ✅ Qualité & Tests
- [ ] Tests unitaires (coverage > 60%)
- [ ] Tests d'intégration (scenarios critiques)
- [ ] Tests UI automatisés (Espresso)
- [ ] Beta testing avec 50+ utilisateurs
- [ ] Correction de tous les bugs critiques
- [ ] Performance: Temps de démarrage < 200ms

#### ✅ Documentation
- [ ] README complet avec screenshots
- [ ] **USER_GUIDE.md - Guide utilisateur simple**
  - Installation et premier démarrage
  - Configuration des permissions
  - Utilisation basique du widget flottant
  - Contrôles multimédia
  - Gestion des profils
  - Personnalisation (thèmes, position, gestures)
  - Automatisation et règles
  - FAQ intégrée
  - Troubleshooting (problèmes courants)
- [ ] Documentation développeur (CONTRIBUTING.md)
- [ ] Changelog détaillé et maintenu
- [ ] Vidéo de démonstration (YouTube, 2-3 min)

#### ✅ Internationalisation (i18n)
- [ ] Anglais (default)
- [ ] Français
- [ ] Espagnol
- [ ] Allemand
- [ ] Italien
- [ ] Portugais (BR)
- [ ] Russe
- [ ] Chinois simplifié
- [ ] Japonais
- [ ] Arabe

#### ✅ Distribution
- [ ] Publication sur F-Droid
- [ ] Publication sur GitHub Releases
- [ ] Page Google Play Store (si souhaité)
- [ ] Site web / Landing page
- [ ] Réseaux sociaux (annonce)

### 📝 Finalisation de version

**Avant release v1.0.0 (MAJEURE) :**
- [ ] **Mise à jour CHANGELOG.md**
  - Ajouter section `## [1.0.0] - YYYY-MM-DD` avec mention **STABLE RELEASE**
  - Récapituler toutes les fonctionnalités majeures depuis v0.5.0
  - Section "Migration depuis v0.x" si breaking changes
  - Lister tous les contributeurs

- [ ] **Mise à jour README.md**
  - **Refonte complète** du README pour version stable
  - Badges : Version, License, F-Droid, Downloads, Stars
  - Screenshots professionnels (au moins 5)
  - Démo GIF animé du widget en action
  - Tableau complet des fonctionnalités
  - Section "Pourquoi FloatingVolume ?"
  - Guides de démarrage rapide (Quick Start)
  - Liens vers USER_GUIDE.md, CHANGELOG.md, CONTRIBUTING.md

- [ ] **Création USER_GUIDE.md** ⭐
  - Guide utilisateur simple et illustré
  - Format : Markdown avec screenshots
  - Langue : Anglais + Français
  - Hébergement : docs/ ou Wiki GitHub

- [ ] **Tests finaux (Release Candidate)**
  - Release v1.0.0-rc1 pour beta testing (2 semaines)
  - Collecte feedback communauté
  - Correction bugs critiques uniquement
  - Validation finale sur 10+ appareils différents
  - Performance : Batterie < 2% par heure, RAM < 50MB

- [ ] **Préparation release**
  - Tag git `v1.0.0`
  - Release notes GitHub détaillées
  - APK signé pour F-Droid
  - Annonce sur forums/réseaux sociaux
  - Communiqué de presse (si souhaité)

---

## v1.1.0+ - Fonctionnalités futures

### 🎯 Priorité 1

#### ✅ Contrôle de luminosité intégré
- Slider additionnel pour la luminosité
- Mode dual: Volume + Luminosité en un seul widget
- Layouts: Côte à côte (horizontal) ou empilés (vertical)
- Toggle rapide entre modes

#### ✅ Égaliseur simple
- Presets: Rock, Pop, Jazz, Classical, Bass Boost, Treble Boost
- Égaliseur 5 bandes (simple)
- Intégration avec l'AudioEffect Android

#### ✅ Historique et statistiques
- Graphique de l'évolution du volume sur 24h
- Temps passé à chaque niveau de volume
- Warnings si volume trop fort trop longtemps (santé auditive)
- Export des données (CSV)

### 🎯 Priorité 2

#### ✅ Support des appareils pliables
- Layouts adaptés aux grands écrans
- Position multi-écrans (écran principal/externe)
- Détection automatique du pliage/dépliage

#### ✅ Mode voiture (Android Auto)
- Interface simplifiée et agrandie
- Gros boutons pour manipulation pendant conduite
- Integration avec Android Auto

#### ✅ Intégration Tasker / Automation
- Plugin Tasker officiel
- Actions exposées:
  - Set Volume (stream, value)
  - Toggle Service
  - Toggle Visibility
  - Switch Profile
  - Show/Hide
- Conditions:
  - Is Service Running
  - Current Volume Level
  - Current Stream Type

#### ✅ Notification avec contrôles
- Notification persistante enrichie
- Mini-slider de volume dans la notification
- Actions rapides (profils, mute)
- Désactivable si préférence pour notification minimale

---

## Idées à long terme

### 🌟 Fonctionnalités avancées

#### IA et Machine Learning
- Apprentissage des habitudes de volume
- Suggestions de profils selon contexte
- Détection automatique de l'environnement sonore (calme, bruyant)
- Ajustement auto du volume selon le bruit ambiant (si micro autorisé)

#### Social & Communauté
- Partage de thèmes sur plateforme dédiée
- Marketplace de configurations
- Upvote/downvote des meilleurs thèmes
- Thèmes saisonniers (Halloween, Noël, etc.)

#### Intégrations tierces
- **Spotify:**
  - Affichage des paroles
  - Contrôle avancé (seek bar, playlists)

- **YouTube Music / YouTube:**
  - Contrôle des vidéos
  - Picture-in-Picture toggle

- **Assistant vocal:**
  - Google Assistant: "Hey Google, monte le volume à 50%"
  - Commandes vocales customisables

#### Accessibilité
- Mode haute visibilité (contraste élevé, gros texte)
- Support TalkBack complet
- Raccourcis clavier (pour appareils avec clavier physique)
- Mode borgne (vibrations uniquement)

#### Personnalisation extrême
- Support des thèmes KWGT/KLWP
- Import de SVG custom pour la poignée
- Scripts Lua pour comportements custom
- API publique pour extensions tierces

---

## 📊 Matrice de priorité

| Fonctionnalité | Impact | Effort | Priorité | Version cible |
|----------------|--------|--------|----------|---------------|
| Auto-démarrage | ⭐⭐⭐⭐⭐ | Faible | **P0** | v0.6.0 |
| Logs accessibles | ⭐⭐⭐⭐ | Faible | **P0** | v0.6.0 |
| Contrôles média | ⭐⭐⭐⭐⭐ | Élevé | **P1** | v0.7.0 |
| Refonte design | ⭐⭐⭐⭐ | Moyen | **P1** | v0.8.0 |
| Profils volume | ⭐⭐⭐⭐ | Moyen | **P1** | v0.9.0 |
| Gestures avancés | ⭐⭐⭐ | Moyen | **P2** | v0.9.0 |
| Quick Settings Tile | ⭐⭐⭐⭐ | Faible | **P1** | v0.9.0 |
| i18n | ⭐⭐⭐⭐ | Élevé | **P1** | v1.0.0 |
| Contrôle luminosité | ⭐⭐⭐ | Moyen | **P2** | v1.1.0 |
| Intégration Tasker | ⭐⭐⭐ | Moyen | **P2** | v1.1.0 |
| IA/ML | ⭐⭐ | Très élevé | **P3** | v2.0.0+ |

**Légende:**
- **P0:** Critique (blocker pour prochaine release)
- **P1:** Haute (souhaitable pour prochaine release)
- **P2:** Moyenne (nice to have)
- **P3:** Basse (future lointaine)

---

## 🛠️ Considérations techniques

### Architecture
- **Migration vers Jetpack Compose** (Flutter UI → Compose pour les écrans natifs ?)
- **Modularisation** du code (feature modules)
- **Repository pattern** pour la persistance
- **Dependency Injection** (Hilt/Koin)

### Performance
- **Lazy loading** des composants lourds
- **Caching** intelligent des ressources
- **Background restrictions** (Android 12+ JobScheduler)
- **Battery optimization** (Doze mode handling)

### Sécurité
- **Code signing** pour F-Droid reproductible
- **ProGuard/R8** pour obfuscation
- **SafetyNet/Play Integrity** (si Google Play)
- **Permissions minimales** (principe du moindre privilège)

### Analytics (optionnel, avec consentement)
- Crashlytics pour crash reporting
- Analytics minimal (usage features, pas de données perso)
- Toggle opt-in/opt-out explicite

---

## 📝 Notes de développement

### Fichiers à créer (nouveaux)
```
FloatingVolume/
├── android/app/src/main/kotlin/.../
│   ├── receiver/
│   │   └── BootReceiver.kt                    # v0.6.0
│   ├── media/
│   │   ├── MediaControlBloc.kt                # v0.7.0
│   │   └── MediaControllerManager.kt          # v0.7.0
│   ├── automation/
│   │   ├── ProfileManager.kt                  # v0.9.0
│   │   └── AutomationRulesEngine.kt           # v0.9.0
│   └── tiles/
│       └── FloatingVolumeQSTile.kt            # v0.9.0
├── lib/src/
│   ├── features/
│   │   ├── profiles/                          # v0.9.0
│   │   ├── themes/                            # v0.8.0
│   │   └── media_controls/                    # v0.7.0
│   └── localization/
│       └── app_localizations.dart             # v1.0.0
└── docs/
    ├── CONTRIBUTING.md                        # v1.0.0
    ├── USER_GUIDE.md                          # v1.0.0 ⭐ Guide simple
    ├── USER_GUIDE_FR.md                       # v1.0.0 (version française)
    └── API.md                                 # v1.1.0+
```

### Dépendances à ajouter
```yaml
# pubspec.yaml (Flutter)
dependencies:
  flutter_localizations:                       # v1.0.0
  intl: ^0.18.0                               # v1.0.0
  animations: ^2.0.0                          # v0.8.0
  flutter_animate: ^4.0.0                     # v0.8.0
  share_plus: ^7.0.0                          # Export logs v0.6.0
  path_provider: ^2.1.0                       # Logs path v0.6.0
```

```gradle
// build.gradle.kts (Android)
dependencies {
    implementation("androidx.media:media:1.7.0")              // v0.7.0
    implementation("com.google.android.material:material:1.11.0") // v0.8.0
}
```

---

## 🎯 Contribution

Cette roadmap est **ouverte aux contributions** ! Si vous souhaitez implémenter une fonctionnalité :

1. **Ouvrez une issue** sur GitHub pour discuter de l'approche
2. **Assignez-vous** la fonctionnalité dans les Projects
3. **Créez une branche** `feature/nom-fonctionnalité`
4. **Documentez** votre code et ajoutez des tests
5. **Ouvrez une PR** avec description détaillée

---

## 📅 Timeline estimée

```
Q1 2026  ██████░░░░  v0.6.0 - Auto-start & Logs
Q2 2026  ░░░░██████  v0.7.0 - Contrôles média
Q3 2026  ░░░░░░██░░  v0.8.0 - Design refresh
Q4 2026  ░░░░░░░░██  v0.9.0 - Personnalisation
Q1 2027  ░░░░░░░░░█  v1.0.0 - Release stable
```

**Note:** Ces dates sont des estimations et peuvent varier selon la disponibilité des contributeurs.

---

## 💬 Feedback

Vos retours sont précieux ! Participez à la définition de la roadmap :
- **GitHub Discussions:** Pour les propositions de fonctionnalités
- **Issues:** Pour les bugs et améliorations techniques
- **Discord/Matrix:** Pour discussions en temps réel (si communauté créée)

---

**Dernière mise à jour:** 2026-02-21
**Maintenu par:** @mkalmousli & contributeurs
**Licence:** GPL-3.0
