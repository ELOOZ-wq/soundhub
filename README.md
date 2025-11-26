# SoundHub (JavaFX)

Client lourd JavaFX pour piloter SoundHub, inspiré des exigences du cahier des charges.

## Stack & architecture

- **Java 21**, JavaFX (Controls & Media) et module system `com.example.soundhub`.
- Packages métiers (`model`, `service`) + UI 100 % code (`ui.*`).
- Services en mémoire pour simuler la base MySQL décrite dans `utils/soundhub.sql`
  (hash bcrypt via `HashUtils`, validations génériques, jeux de données seeds).

## Lancer l'application

1. Installer un JDK 21+ et définir `JAVA_HOME`.
2. Depuis la racine du projet :
   ```bash
   .\mvnw.cmd -DskipTests javafx:run
   ```
   (ou `mvn` si Maven est installé).

> ⚠️ Sur cette machine, `JAVA_HOME` n’est pas configuré ; ajoute-le avant de lancer le wrapper.

## Comptes de démonstration

| Rôle        | Identifiant      | Mot de passe |
|-------------|------------------|--------------|
| Super admin | `superadmin`     | `Super#2024` |
| Admin       | `clara-admin`    | `Admin#2024` |
| User        | `marco`          | `User#2024`  |
| User        | `alice`          | `User#2024`  |

Les nouveaux comptes sont créés avec le statut **PENDING** et doivent être validés par un ADMIN ou SUPER ADMIN.

## Fonctionnalités clés

- Tableau de bord réactif (bibliothèque, playlists, favoris, validation).
- Filtrage par recherche + statut, mini-lecteur simulé (play/pause/stop, progression, volume).
- Workflows rôle-dépendants :
  - **USER / ADMIN** : upload de tracks, gestion playlists/favoris.
  - **ADMIN** : validation des utilisateurs et des morceaux.
  - **SUPER ADMIN** : gestion des admins (promotion/déclassement).

## À connecter ensuite

- Brancher `service.*` sur `DBConnection` + MySQL.
- Remplacer le mini-player simulé par `MediaPlayer` relié à de vrais fichiers audio.

