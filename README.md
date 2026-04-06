# PétasseGang Addons

Custom content mod for the PétasseGang Minecraft server.

[![Build](https://github.com/PetasseGang/petassegang_addons/actions/workflows/build.yml/badge.svg)](https://github.com/PetasseGang/petassegang_addons/actions/workflows/build.yml)
[![Tests](https://github.com/PetasseGang/petassegang_addons/actions/workflows/test.yml/badge.svg)](https://github.com/PetasseGang/petassegang_addons/actions/workflows/test.yml)
![MC](https://img.shields.io/badge/Minecraft-26.1-brightgreen)
![Forge](https://img.shields.io/badge/Forge-62.0.x-orange)
![License](https://img.shields.io/badge/License-MIT-blue)

---

## Quick Start

**Prérequis :** Java 25, Git. Gradle est fourni via le wrapper (`gradlew`).

```bash
# 1. Clone
git clone https://github.com/PetasseGang/petassegang_addons.git
cd petassegang_addons

# 2. Générer les run configs VS Code (une seule fois)
./gradlew genEclipseRuns

# 3. Lancer le client Minecraft avec le mod
./gradlew runClient  # ou via IDE après genEclipseRuns

# 4. Build
./gradlew build
# → build/libs/petassegang_addons-0.1.0.jar
```

> **Première fois ?** Voir [docs/SETUP.md](docs/SETUP.md) pour l'installation complète de Java 25 et la configuration VS Code.

---

## Structure du projet

```
petassegang_addons/
├── src/main/java/com/petassegang/addons/
│   ├── PetasseGangAddonsMod.java   ← entry-point @Mod
│   ├── init/ModItems.java          ← registres items
│   ├── item/GangBadgeItem.java     ← Gang Badge (premier item)
│   ├── creative/ModCreativeTab.java
│   ├── config/ModConfig.java
│   └── util/ModConstants.java
├── src/main/resources/
│   ├── META-INF/mods.toml
│   └── assets/petassegang_addons/  ← textures, modèles, lang
├── src/test/                       ← JUnit 5 + GameTests
├── .github/workflows/              ← CI/CD GitHub Actions
├── .skills/                        ← Skills Claude Code
└── docs/                           ← Documentation complète
```

---

## Ajouter du contenu avec Claude Code

Les skills dans `.skills/` guident Claude Code pas-à-pas pour chaque type de contenu.
Il suffit de décrire ce que tu veux :

```
"Ajoute un item épée custom appelée GangSword avec un tooltip violet"
"Crée un bloc de minerai gangite_ore avec une texture dorée"
"Ajoute un mob PetasseMob qui drop le Gang Badge"
"Crée une recette craft pour le Gang Badge avec des lingots d'or"
```

| Skill | Déclenche pour |
|-------|----------------|
| `add-item` | item, outil, arme, badge, carte, consommable |
| `add-block` | bloc, minerai, dalle, mur, porte |
| `add-entity` | mob, entité, boss, NPC, créature |
| `add-dimension` | dimension, portail, monde custom |
| `add-recipe` | recette, craft, fondre, cuisiner |
| `add-sound` | son, bruit, musique, ambiance |
| `add-creative-tab` | onglet créatif, catégorie d'items |

---

## Tests

```bash
# Tests unitaires (JUnit 5)
./gradlew test
# Rapport : build/reports/tests/test/index.html

# Tests in-game (Forge GameTest)
./gradlew runGameTestServer
```

---

## Build & Distribution

```bash
# Build le JAR
./gradlew build

# Artefact :
build/libs/petassegang_addons-0.1.0.jar
```

Pour installer sur le serveur : copier le JAR dans le dossier `mods/` du serveur.
Tous les clients doivent avoir le même JAR.

---

## CI/CD

- **Push** sur `main`/`develop` → build automatique
- **Pull Request** → build + tests, bloque si test échoue
- **Tag `v*.*.*`** → build + GitHub Release + JAR joint automatiquement

Pour créer une release :
```bash
git tag v0.1.0
git push origin v0.1.0
```

Voir [docs/CICD.md](docs/CICD.md) pour les détails.

---

## Documentation

| Document | Description |
|----------|-------------|
| [docs/SETUP.md](docs/SETUP.md) | Installation complète |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Architecture et conventions |
| [docs/CONTRIBUTING.md](docs/CONTRIBUTING.md) | Guide de contribution |
| [docs/TESTING.md](docs/TESTING.md) | Guide des tests |
| [docs/ITEMS.md](docs/ITEMS.md) | Catalogue des items |
| [docs/CHANGELOG.md](docs/CHANGELOG.md) | Historique des versions |
| [docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md) | Résolution de problèmes |

---

## Licence

MIT — voir [LICENSE](LICENSE).
