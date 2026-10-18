# Architecture — PeTaSsE_gAnG_Additions

## Stack technique

| Couche | Technologie |
|--------|-------------|
| Jeu | Minecraft 1.21.1 (Yarn mappings 1.21.1+build.3) |
| Mod loader | Fabric Loader 0.19.5 |
| API | Fabric API 0.116.17+1.21.1 |
| Langage | Java 21 |
| Build | Gradle 9.8.1 (wrapper) + Fabric Loom 1.17.21 |
| Tests | JUnit 5.11 + fabric-loader-junit, JaCoCo, Fabric GameTest |
| CI/CD | GitHub Actions |

Aucune dépendance runtime externe en dehors de Fabric API. Le cache de secteurs utilise
`java.util.LinkedHashMap` plutôt que Guava pour rester sans dépendance.

---

## Arborescence des packages

```text
com.petassegang.addons/
|
|-- PeTaSsEgAnGAdditionsMod.java        <- ModInitializer : registerContent() + hooks serveur + flag portail
|-- PeTaSsEgAnGAdditionsClientMod.java  <- ClientModInitializer : payloads (badge, chunks distants), modèle adaptatif, renderer portail
|
|-- core/ModConstants.java              <- MOD_ID, MOD_NAME, LOGGER
|-- config/ModConfig.java               <- options lues au démarrage via -D (debug, portails)
|
|-- init/                               <- un registre par type, champs static final
|   |-- ModBlocks.java
|   |-- ModItems.java
|   |-- ModBlockEntities.java
|   |-- ModEntities.java                <- type de l'entity portail, toujours enregistré
|   |-- ModDataComponents.java          <- composant des réglages du bloc portail
|   |-- ModChunkGenerators.java
|   |-- ModCreativeTab.java             <- ENTRIES = liste ordonnée de l'onglet
|   `-- ModNetworking.java              <- types de payloads
|
|-- feature/
|   |-- gang/GangBadgeItem, GangBadgeActivatePayload, client/GangBadgeClientHandler
|   `-- cursed/CursedSnackItem
|
|-- backrooms/
|   |-- BackroomsConstants.java         <- clé de dimension, identifiants, échelle de cellule
|   |-- level0/
|   |   |-- biome/LevelZeroSurfaceBiome          <- palettes cosmétiques par région et par layer
|   |   |-- block/LevelZeroWallpaperBlock        <- mur adaptatif (face_mask 0-15)
|   |   |-- client/model/                        <- LevelZeroWallpaperBakedModel + handler
|   |   `-- generation/
|   |       |-- LevelZeroChunkGenerator          <- orchestre les 5 layers
|   |       |-- coord/   LevelZeroCoords, LevelZeroLayerStackLayout, LevelZeroVerticalSlice
|   |       |-- noise/   StageRandom, LevelZeroSeedResolver
|   |       |-- sector/  LevelZeroSectorGenerator, LevelZeroSectorData, LevelZeroSectorCache, LevelZeroSectorRoomKind
|   |       |-- layout/  LevelZeroLayout, LevelZeroRegionWalkability, LevelZeroCellEvaluator,
|   |       |            LevelZeroTopology, LevelZeroLargeRooms, LevelZeroLightPattern, LevelZeroCorridorGaps,
|   |       |            LevelZeroCellEvaluation, LevelZeroCellContext, enums et masques
|   |       `-- write/   LevelZeroBlockPalette, LevelZeroBlockWriter, LevelZeroWallSampler,
|   |                    LevelZeroWallProps, LevelZeroDebugPalette
|   `-- portal/                          <- portails : entity, block, transition, sync, interaction, propagation, light, client, debug (voir §Portails)
|
|-- perf/ModPerformanceMonitor (+ client/ClientPerformanceMonitorHook)
`-- mixin/                               <- communs (relais de blocs, chunks tenus, sons et particules, collision, mobs, lumière) + client/ (rendu des portails)
```

Les tests en jeu vivent dans un source set séparé, `src/gametest/java/com/petassegang/addons/gametest/`, rangé par
domaine comme le code (`support/`, `backrooms/level0/`, `backrooms/portal/`, `feature/`) : un mod
`petasse_gang_additions_gametest` chargé par `runGameTest` seulement, jamais dans le JAR (`verifyJarContents` le vérifie).

---

## Pipeline de génération du Level 0

```text
NoiseConfig ──LevelZeroSeedResolver──> seed de layout ──> seed par layer (0..4)
                                                                │
   LevelZeroSectorCache (LRU 256) ◄── LevelZeroSectorGenerator  │  secteur 240x135 cellules :
          │   traversabilité + type de salle par cellule        │  mini-labyrinthes (Prim) + salles
          ▼                                                     ▼
   LevelZeroRegionWalkability : fenêtre du chunk + marge de 2 cellules
          │
          ▼
   LevelZeroCellEvaluator (par cellule 3x3 du chunk)
      grande pièce ─> biome ─> connexions/topologie ─> néon ─> gap de couloir ─> motif 3x3
          │
          ▼
   LevelZeroLayout (cellules évaluées du chunk) ──> LevelZeroBlockWriter
                                                      bedrock inter-layer, sol, air|mur, plafond|néon
                                                      murs visibles : LevelZeroWallSampler (exposition, faces blanches)
```

Points clés :

- **Déterminisme** : tout dérive de `StageRandom.mix(seed, stage, x, z)`. Les sels sont figés.
  `LevelZeroGoldenTest` verrouille les empreintes de génération pour trois seeds et cinq layers.
- **Marge de région** : la fenêtre de traversabilité déborde de deux cellules autour du chunk, ce qui
  rend topologie, néons et gaps identiques des deux côtés d'une frontière de chunk.
- **Biomes sans seed** : `LevelZeroSurfaceBiome` ne dépend pas du seed monde, le client recalcule
  la palette et les détails muraux sans synchronisation.
- **Pile verticale** : cinq layers de dix blocs (sol 0, air 1-4, plafond 5, bedrock 6-9), dimension de 64 blocs.
- **Un seul chemin d'écriture** : `LevelZeroBlockWriter.writeColumn` alimente aussi bien le chunk que
  l'échantillon de colonne demandé par le moteur, via l'interface fonctionnelle `BlockSink`.

---

## Rendu client du Level 0

Le mur visible est toujours `level_zero_wallpaper_adaptive`. Sa propriété `face_mask` (4 bits) dit quelles
faces montrent la variante blanche ; elle est calculée par le générateur et relue par
`LevelZeroWallpaperBakedModel`, qui ajoute en overlay les plinthes (ligne basse de chaque layer) et les
rares interrupteurs ou prises (`LevelZeroWallProps`, déterministes). Aucune block entity, aucun bloc
supplémentaire dans le monde.

---

## Portails

`backrooms/portal/` relie le monde normal et le Level 0 par un bloc portail : entity portail persistante,
block entity qui garde ses réglages (taille, lumière), trouve seule sa destination et pose deux faces dos à dos de
chaque côté (un portail double face), téléportation par traversée du
plan (joueurs prédits par le client, objets et mobs transposés par le serveur), collision à travers le cadre
(`transition/PortalCollision`), synchronisation serveur → client des chunks et des entités de l'autre dimension
(payloads `portal_remote_*`, suivi vanilla des entités), rendu client du monde distant à travers le cadre (stencil,
caméra transposée, plan de découpe dans les shaders, un niveau de portails imbriqués), interaction à travers le
cadre (`interaction/`), et ce qui traverse un portail ouvert sans être une entité : sons et particules relayés
(`MixinPlayerManager`, `MixinServerWorld`, rejoués côté client par `client/sound` et `RemoteParticles`), regard et
poursuite des mobs (`propagation/`), lumière des blocs (`light/`, sources virtuelles lues par le moteur de lumière).
Actif par défaut, `ModConfig.ENABLE_PORTAL_FEATURE` coupe tout (bloc inerte, mixins inertes). Conception, protocole et limites : [PORTALS.md](PORTALS.md).

---

## Cycle de vie

```text
1. Fabric charge le mod ; les mixins communs et client s'appliquent (sans effet si portails désactivés).
2. onInitialize() : registerContent() (générateurs de chunks -> composants -> blocs -> block entities -> entity -> items -> onglets -> payloads),
   hooks de tick serveur (perf), vidage du cache de secteurs à l'arrêt, runtime portail si activé.
3. onInitializeClient() : récepteurs des payloads (badge, chunks distants), hook FPS, modèle adaptatif, renderer portail.
4. Chargement du monde : le datapack déclare la dimension, qui référence le codec du générateur.
```

---

## Séparation client / serveur

| Règle | Détail |
|-------|--------|
| Code commun | `init/`, `feature/*` (hors `client/`), `backrooms/level0/{biome,block,generation}`, `backrooms/portal/{entity,block,transition,sync,interaction,propagation,light,debug}`, `mixin/*` (hors `client/`), `perf/ModPerformanceMonitor` |
| Code client | sous-packages `client/` et `mixin/client/` uniquement ; jamais de `MinecraftClient` ailleurs |
| Point d'entrée | `ClientModInitializer` séparé, déclaré dans `fabric.mod.json` |

---

## Conventions

| Type | Convention | Exemple |
|------|------------|---------|
| Classe | PascalCase | `LevelZeroBlockWriter` |
| Méthode / variable | camelCase | `wallpaperFaceMask` |
| Constante | UPPER_SNAKE_CASE | `REGION_MARGIN_CELLS` |
| Identifiant registre | snake_case | `level_zero_wallpaper_adaptive` |
| Clé de langue | `block.<mod_id>.<id>`, `item.<mod_id>.<id>` | `block.petasse_gang_additions.cursed_log` |
| Commentaires et logs | français, majuscule, point final, ASCII | `// Le cache evince les plus anciens.` |
| Imports | `java` → tiers (`com.mojang`, `com.llamalad7`, `org.*`) → `net.minecraft` → `net.fabricmc` → `com.petassegang` | — |

Pas de couche d'indirection sans second usage réel : une classe = une responsabilité lisible,
pas de stage/builder/façade spéculatifs.
