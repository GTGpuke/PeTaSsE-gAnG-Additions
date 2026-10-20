# GTG's Backrooms

Mod Fabric qui ouvre les Backrooms dans Minecraft : le Level 0 et des portails sans écran de chargement.

[![CI](https://github.com/GTGpuke/GTGs-Backrooms/actions/workflows/ci.yml/badge.svg)](https://github.com/GTGpuke/GTGs-Backrooms/actions/workflows/ci.yml)
[![Release](https://github.com/GTGpuke/GTGs-Backrooms/actions/workflows/release.yml/badge.svg)](https://github.com/GTGpuke/GTGs-Backrooms/actions/workflows/release.yml)
![MC](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen)
![Fabric](https://img.shields.io/badge/Fabric%20Loader-0.19.5-blue)
![Java](https://img.shields.io/badge/Java-21-orange)
![License](https://img.shields.io/badge/License-All%20Rights%20Reserved-red)

---

## Démarrage rapide

Prérequis : Java 21 et Git. Gradle est fourni par le wrapper.

```bash
git clone https://github.com/GTGpuke/GTGs-Backrooms.git
cd GTGs-Backrooms
./gradlew runClient      # client de dev
./gradlew test           # tests unitaires + couverture JaCoCo
./gradlew build          # JAR dans build/libs/ (après Checkstyle, tests, tests en jeu, couverture, contrôle du JAR)
```

Détails d'installation : [docs/SETUP.md](docs/SETUP.md).

---

## Contenu

| Feature | Description |
|---------|-------------|
| **Backrooms Level 0** | Dimension `gtgs_backrooms:backrooms_level_0` : cinq layers de labyrinthe empilés, générés de façon déterministe par chunk (mini-labyrinthes superposés + salles tamponnées), moquette, papier peint adaptatif, néons en quinconce, deux biomes cosmétiques, plinthes et petits éléments muraux rendus côté client. |
| **Gang Badge** | Item EPIC unique par pile ; clic droit = miaulement + animation de totem. |
| **Casse-croûte Maudit** | Nourriture toujours mangeable qui retire 2 points de faim. |
| **Arbre Maudit** | Tronc, feuilles, pousse (arbre custom), planches. |
| **Portails Backrooms** | **Portail des Backrooms** (onglet créatif dédié) : portail double face, visible et traversable dans les deux sens vers le Level 0, taille, lumière, sens de sortie et arrivée au hasard réglables, cadres debout, au sol ou au plafond (changement de gravité), montures et véhicules emmenés avec leurs passagers, portail de retour, monde distant rendu à travers le cadre avec ses entités et ses propres portails, interaction à travers le cadre (chunks et entités synchronisés serveur → client). Détail : [docs/PORTALS.md](docs/PORTALS.md). |

Accès au Level 0 en jeu :

```mcfunction
/execute in gtgs_backrooms:backrooms_level_0 run teleport @s 1 1 1
```

ou, en jeu, poser un **Portail des Backrooms** (onglet *GTG's Backrooms - Portails*) et le traverser.

---

## Structure du projet

```text
src/main/java/com/gtgpuke/gtgsbackrooms/
├── GtgsBackroomsMod.java          ← point d'entrée commun
├── GtgsBackroomsClientMod.java    ← point d'entrée client
├── core/ModConstants.java                ← MOD_ID, LOGGER
├── config/ModConfig.java                 ← options lues en -D (toggles debug, portails)
├── init/                                 ← ModBlocks, ModItems, ModBlockEntities, ModEntities, ModDataComponents, ModChunkGenerators, ModCreativeTab, ModNetworking
├── feature/gang/, feature/cursed/        ← contenu gameplay
├── backrooms/
│   ├── BackroomsConstants.java
│   ├── level0/
│   │   ├── biome/LevelZeroSurfaceBiome.java
│   │   ├── block/LevelZeroWallpaperBlock.java
│   │   ├── client/model/                 ← rendu adaptatif du mur
│   │   └── generation/
│   │       ├── LevelZeroChunkGenerator.java
│   │       ├── coord/   (coordonnées, pile verticale)
│   │       ├── noise/   (seeds déterministes)
│   │       ├── sector/  (labyrinthe brut + cache LRU)
│   │       ├── layout/  (évaluation des cellules : topologie, lumière, gaps)
│   │       └── write/   (palette, writer, détails muraux)
│   └── portal/                           ← portails (entity, bloc, transition, sync, interaction, propagation, lumière, client, debug)
├── perf/                                 ← moniteur de performance opt-in
└── mixin/                                ← mixins (relais serveur des blocs, sons et particules, collision, mobs ; rendu client des portails)

src/test/java/...                         ← ~380 tests JUnit 5 (logique pure + registres réels via fabric-loader-junit)
src/gametest/java/...                     ← tests en jeu (mod séparé, runGameTest ; jamais dans le JAR)
docs/                                     ← documentation technique, specs Backrooms, audit
```

Architecture détaillée : [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md). Index complet : [docs/INDEX.md](docs/INDEX.md).

---

## Qualité

- `./gradlew test` : JUnit 5 sous le classloader Fabric ; rapport JaCoCo dans `build/reports/jacoco/test/html/`.
- `./gradlew runGameTest` : serveur headless, sous agent JaCoCo : génération du Level 0, portails (synchronisation,
  traversée, interaction), contenu.
- `./gradlew jacocoCoverageVerification` : couverture fusionnée, 100 % des instructions et des branches exigés.
- `./gradlew checkstyleMain checkstyleTest checkstyleGametest` : conventions du code (`config/checkstyle/`).
- `./gradlew verifyJarContents` : le JAR ne contient que le mod, sans dépendance embarquée ni test.
- `./gradlew benchmarkLevelZeroGeneration` : benchmark déterministe de la génération.
- `LevelZeroGoldenTest` verrouille les empreintes de génération : toute évolution de la génération est explicite.

Dernier audit complet du projet : [docs/AUDIT.md](docs/AUDIT.md).

---

## Licence

Tous droits réservés — © 2025-2026 GTGpuke. Licence propriétaire : jouer avec un JAR publié par l'auteur est
permis ; copier, modifier ou redistribuer le code ou les ressources demande son accord écrit. Conditions complètes :
[LICENSE](LICENSE).
