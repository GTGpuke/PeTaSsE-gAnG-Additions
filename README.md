# PeTaSsE_gAnG_Additions

Custom content mod for the PetasseGang Minecraft server.

[![CI](https://github.com/GTGpuke/PeTaSsE-gAnG-Additions/actions/workflows/ci.yml/badge.svg)](https://github.com/GTGpuke/PeTaSsE-gAnG-Additions/actions/workflows/ci.yml)
[![Release](https://github.com/GTGpuke/PeTaSsE-gAnG-Additions/actions/workflows/release.yml/badge.svg)](https://github.com/GTGpuke/PeTaSsE-gAnG-Additions/actions/workflows/release.yml)
![MC](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen)
![Fabric](https://img.shields.io/badge/Fabric-0.16.9-blue)
![License](https://img.shields.io/badge/License-MIT-blue)

---

## Quick Start

Prérequis : Java 21 et Git. Gradle est fourni via le wrapper.

```bash
# 1. Clone
git clone https://github.com/PetasseGang/petasse_gang_additions.git
cd petasse_gang_additions

# 2. Run the dev client
./gradlew runClient

# 3. Build the mod
./gradlew build
```

Les détails d'installation sont dans [docs/SETUP.md](docs/SETUP.md).

---

## Current State

Le projet inclut actuellement :

- une installation Fabric 1.21.1 avec Fabric API,
- une première dimension Backrooms Level 0 jouable,
- un générateur de chunk multi-layer custom inspiré du script Python de référence,
- des biomes cosmétiques Level 0 qui changent le papier peint et la moquette sans modifier la topologie du labyrinthe,
- un rendu de papier peint adaptatif par face exposée, réservé aux transitions mixtes entre biomes de surface,
- un cœur de mur en bedrock vanilla pour que le bloc adaptatif ne s'applique qu'aux surfaces visibles,
- une pile verticale canonique Level 0 avec layers seedés indépendamment,
- un pipeline Level 0 réorganisé et documenté par responsabilités,
- un monitoring de performance debug pour observer les coûts en jeu,
- le Gang Badge et le contenu de l'Arbre Maudit d'origine,
- une suite de tests JUnit 5.

---

## Project Structure

```text
petasse_gang_additions/
|- src/main/java/com/petassegang/addons/
|  |- PeTaSsEgAnGAdditionsMod.java
|  |- PeTaSsEgAnGAdditionsClientMod.java
|  |- backrooms/
|  |  |- BackroomsConstants.java
|  |  `- level/level0/
|  |     |- biome/LevelZeroSurfaceBiome.java
|  |     |- block/
|  |     |- client/model/
|  |     `- generation/
|  |        |- LevelZeroChunkGenerator.java
|  |        |- layout/LevelZeroLayout.java
|  |        |- stage/
|  |        |- write/
|  |        `- noise/
|  |- config/
|  |- core/ModConstants.java
|  |- creative/ModCreativeTab.java
|  |- feature/
|  |  |- cursed/item/cursed_snack/CursedSnackItem.java
|  |  `- gang/
|  |     |- client/GangBadgeClientHandler.java
|  |     |- item/gang_badge/GangBadgeItem.java
|  |     `- network/c2s/GangBadgeActivatePayload.java
|  |- init/
|  |  |- ModBlocks.java
|  |  |- ModBlockEntities.java
|  |  |- ModChunkGenerators.java
|  |  `- ModItems.java
|  |- network/
|  `- perf/section/
|- src/main/resources/
|  |- fabric.mod.json
|  |- assets/petasse_gang_additions/
|  `- data/petasse_gang_additions/
|- src/test/
|- docs/
`- build.gradle
```

---

## Main Commands

```bash
# Compiler les sources principales
./gradlew compileJava

# Lancer les tests unitaires
./gradlew test

# Lancer le client dev
./gradlew runClient

# Lancer le client dev avec monitoring de performance actif
./gradlew runClient -PdebugPerformanceMonitor=true

# Lancer le client dev avec monitoring actif et logs plus frequents
./gradlew runClient -PdebugPerformanceMonitor=true -PperformanceLogIntervalSeconds=5

# Build complet (produit build/libs/petasse_gang_additions-<version>.jar)
./gradlew build
```

---

## Level 0 Notes

L'implémentation actuelle du Level 0 repose sur un pipeline de layout déterministe :

- génération de labyrinthe traduite du prototype Python de référence,
- salles rectangulaires, salles à piliers, salles polygonales,
- `1 cellule logique = 3×3 blocs` en monde,
- plafond bas et éclairage fluorescent fort pour l'effet oppressif voulu.

La couche de biomes cosmétiques ne modifie que l'aspect de surface. Elle ne change pas la forme du layout.
Le rendu de papier peint adapte chaque face exposée uniquement sur les vraies transitions mixtes entre biomes de surface adjacents.
Les murs jaunes simples et les murs blancs simples sont des blocs simples sans `BlockEntity`.
Le masque de face exposée est calculé à la génération, stocké dans une `BlockEntity` synchronisée pour les cas mixtes uniquement, et relu par le renderer client.
Si la `ModelData` synchronisée n'est pas encore disponible côté client, le modèle adaptatif relit d'abord les blocs de sol déjà générés, puis retombe sur l'échantillonneur déterministe en dernier recours.
Le cœur non exposé des murs utilise de la bedrock vanilla.
Le cache de layout est intentionnellement borné pour limiter la mémoire retenue.

Les textures de blocs du Level 0 suivent la convention `32×32`.

Les classes Java du Level 0 gardent le prefixe historique `LevelZero*` pour
eviter un renommage massif inutile ; l'arborescence v4 les range toutefois sous
`backrooms/level/level0/`.

---

## Testing

```bash
# Tests JUnit 5
./gradlew test

# Benchmark de performance Level 0
./gradlew benchmarkLevelZeroGeneration

# Benchmark avec budget max autorise par chunk
./gradlew benchmarkLevelZeroGeneration -PlevelZeroPerfBudgetMsPerChunk=0.350
```

Sur Windows avec un chemin de projet contenant des caractères accentués, consulter
[docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md) pour les problèmes d'encodage Gradle connus.

---

## Performance Debug

Le mod inclut un monitoring de performance opt-in pour le runtime general et
pour les sections profilees du pipeline Level 0.

Activation rapide :

```bash
./gradlew runClient -PdebugPerformanceMonitor=true
```

Commandes utiles :

```bash
# Resume perf toutes les 5 secondes dans les logs
./gradlew runClient -PdebugPerformanceMonitor=true -PperformanceLogIntervalSeconds=5

# Monitoring actif pendant le benchmark deterministe Level 0
./gradlew benchmarkLevelZeroGeneration -PdebugPerformanceMonitor=true -PperformanceLogIntervalSeconds=5
```

Quand le monitoring est actif :

- des resumes periodiques `[perf]` sont ecrits dans les logs ;
- le F3/debug HUD affiche une synthese courte client + serveur ;
- les sections profilees du Level 0 remontent dans le top serveur ;
- la RAM JVM, la charge CPU processus/systeme et les FPS client sont echantillonnes.

Limites actuelles :

- le GPU n'est pas exposable proprement via l'API Java/Fabric seule ;
- le benchmark `benchmarkLevelZeroGeneration` mesure surtout le cout CPU du pipeline deterministe ;
- sous Windows, le wrapper Gradle peut finir par `exit /b 1` meme quand `BUILD SUCCESSFUL` est bien affiche.

---

## Documentation

| Document | Description |
|----------|-------------|
| [docs/INDEX.md](docs/INDEX.md) | Index complet de la documentation |
| [docs/SETUP.md](docs/SETUP.md) | Installation locale |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Architecture et conventions |
| [docs/DEPENDENCIES.md](docs/DEPENDENCIES.md) | Plan de dépendances Backrooms |
| [docs/DIMENSIONS.md](docs/DIMENSIONS.md) | Référence des dimensions |
| [docs/BLOCKS.md](docs/BLOCKS.md) | Catalogue des blocs |
| [docs/ITEMS.md](docs/ITEMS.md) | Catalogue des items |
| [docs/TESTING.md](docs/TESTING.md) | Guide des tests |
| [docs/CHANGELOG.md](docs/CHANGELOG.md) | Historique des versions |
| [docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md) | Problèmes courants |

---

## Build Output

```bash
build/libs/petasse_gang_additions-<version>.jar          # JAR principal remappé
build/libs/petasse_gang_additions-<version>-sources.jar  # Sources
```

---

## License

MIT. See [LICENSE](LICENSE).
