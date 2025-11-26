# Journal des modifications SoundHub

## Fonctionnalités et architecture ajoutées

- **Modèles métier complets** (`model`): users avec rôles/statuts, tracks (durée/date formatées), playlists, favoris, enums dédiées.
- **Services métiers** (`service`): en mémoire pour simuler MySQL.
  - `UserService` : enregistrement sécurisé (bcrypt), authentification, validation par rôles.
  - `TrackService` : dépôt de morceaux, suivi des statuts (PENDING/APPROVED/REJECTED), filtrage.
  - `PlaylistService` : gestion CRUD + ajout/retrait de morceaux.
  - `FavoriteService` : favoris par utilisateur avec toggle.
- **Utilitaires** (`utils`): `HashUtils` (bcrypt), `ValidationUtils` (emails, durées), `DBConnection` conservé pour future base.
- **Interface JavaFX (100 % code)** :
  - `LoginView` avec création de compte + feedback.
  - `DashboardView` : interface type Spotify (top bar + bibliothèque latérale, chips de filtres, carrousels de cartes, sections Playlists/Favoris/Validation), barre de recherche, workflow upload & modération, mini-player simulé.
  - `MiniPlayer` : play/pause/stop, progression animée, volume.
  - `SoundHubController` : point d’entrée unique vers les services.
  - `SoundHubApp` : navigation login ↔ dashboard + injection d’un thème CSS.
- **Thème futuriste orange** (`src/main/resources/ui/styles.css`) :
  - Palette néon ambrée (#FF7518) façon Spotify : fonds radiaux lumineux, cartes vitrées, halos sur boutons pilules/outlines, mini-player et écran d’auth homogènes.
- **Build & doc** :
  - Maven configuré pour Java 21 + JavaFX Controls/FXML/Media + jBCrypt, plugin JavaFX pointant sur `ui.SoundHubApp`.
  - `README.md` détaillant stack, commandes (`mvnw`, nécessité de `JAVA_HOME`), comptes de démo et roadmap.
  - `UPDATE_LOG.md` (ce fichier) pour tracer les travaux réalisés.

## Logique générale

1. **Authentification & rôles**
   - Comptes créés → statut `PENDING`, seuls ADMIN/SUPER_ADMIN peuvent les activer via l’onglet Validation.
   - `SoundHubController` encapsule login/register et expose les services aux vues.
2. **Bibliothèque & recherche**
   - `DashboardView` filtre les tracks via texte + statut, n’affiche que les APPROVED aux simples users.
   - Menu contextuel sur les lignes : lecture, favoris, ajout direct à la playlist, modération (si droits).
3. **Playlists & favoris**
   - Playlists liées à l’utilisateur courant, dialogues pour nommage/renommage.
   - Favoris listés avec double-clic pour lancer la lecture.
4. **Modération**
   - Vue dédiée permettant d’activer/desactiver users, approuver/rejeter tracks, gérer les admins (promotion/déclassement) selon le rôle connecté.
5. **Mini-player**
   - Simulation d’un player audio (timeline animée, boutons play/pause/stop, volume) en attendant l’intégration de `MediaPlayer` et de vrais fichiers audio.

## Comptes de démonstration (seed)

| Nom d’utilisateur | Rôle         | Mot de passe |
|-------------------|--------------|--------------|
| superadmin        | SUPER_ADMIN  | `Super#2024` |
| clara-admin       | ADMIN        | `Admin#2024` |
| marco             | USER         | `User#2024`  |
| alice             | USER         | `User#2024`  |
| pendingUser       | USER (PENDING) | `User#2024` |

> Les nouveaux comptes créés via l’UI héritent automatiquement du rôle USER avec statut `PENDING`.

