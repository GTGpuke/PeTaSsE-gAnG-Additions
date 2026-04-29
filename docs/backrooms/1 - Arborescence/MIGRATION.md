# MIGRATION.md — Plan de migration vers la nouvelle architecture

> **Objet** : migrer le repo `PeTaSsE-gAnG-Additions` de sa structure actuelle vers l'arbo cible définie dans `ARCHITECTURE.md` §3 (draft local : `1 - Arborescence/arborescence_v4.md`).
> **Périmètre** : features existantes uniquement (Backrooms Level 0, Gang Badge, Cursed Snack). Aucun code Veil/portail n'est implémenté ici — seuls les packages vides sont créés en prévision.
> **Garantie** : zéro régression fonctionnelle. Le build et `runClient` doivent rester verts à chaque commit.

> **Statut 2026-04-29** : la migration de l'arborescence des features existantes est appliquée dans le code.
> Les anciens chemins présents plus bas sont conservés comme historique de migration et comme aide de rollback,
> pas comme référence active. La référence active est `docs/ARCHITECTURE.md` + `arborescence_v4.md`.

---

## 1. État actuel (snapshot du repo, branche `master`)

### Fichiers Java existants

```
src/main/java/com/petassegang/addons/
├── PeTaSsEgAnGAdditionsMod.java
├── PeTaSsEgAnGAdditionsClientMod.java
├── config/
│   └── ModConfig.java
├── creative/
│   └── ModCreativeTab.java
├── init/
│   ├── ModBlocks.java
│   ├── ModBlockEntities.java
│   ├── ModItems.java
│   └── ModChunkGenerators.java
├── item/
│   ├── GangBadgeItem.java
│   └── CursedSnackItem.java
├── block/
│   ├── LevelZeroWallpaperBlock.java
│   └── entity/
│       └── LevelZeroWallpaperBlockEntity.java
├── network/
│   ├── ModNetworking.java
│   └── packet/
│       └── GangBadgeActivatePayload.java
├── world/
│   └── backrooms/
│       ├── BackroomsConstants.java
│       └── level0/
│           ├── LevelZeroChunkGenerator.java
│           ├── LevelZeroLayout.java
│           └── LevelZeroSurfaceBiome.java
├── client/
│   ├── handler/
│   │   └── GangBadgeClientHandler.java
│   └── model/
│       ├── LevelZeroWallpaperBlockStateModel.java
│       ├── LevelZeroWallpaperBakedModel.java
│       └── LevelZeroWallpaperModelHandler.java
└── util/
    └── ModConstants.java
```

**Total** : 19 fichiers Java.

### Constat

L'organisation actuelle est **horizontale par type technique** (`item/`, `block/`, `init/`, `client/`). Les fichiers Backrooms Level 0 sont éclatés sur 3 packages (`block/`, `client/model/`, `world/backrooms/level0/`).

---

## 2. Cible (extrait de l'arbo §3)

Pour les fichiers existants, voici les destinations :

| Catégorie | Destination |
|-----------|-------------|
| Constantes globales | `core/ModConstants.java` |
| Configuration | `config/ModConfig.java` (inchangé) |
| Creative tab | `creative/ModCreativeTab.java` (inchangé) |
| Registries | `init/Mod*.java` (inchangé) |
| Gang Badge (item + handlers + payload) | `feature/gang/` |
| Cursed Snack | `feature/cursed/` |
| Backrooms Level 0 — bloc wallpaper et son entity | `backrooms/level/level0/block/wallpaper/` |
| Backrooms Level 0 — chunk gen, layout, biome | `backrooms/level/level0/generation/` et `backrooms/level/level0/biome/` |
| Backrooms Level 0 — modèles client | `backrooms/level/level0/block/wallpaper/` (regroupés avec le bloc) |
| Constantes Backrooms | `backrooms/BackroomsConstants.java` |

Plus tous les **packages vides** pour Veil/portails/Lifecycle (créés mais sans code dedans pour l'instant).

---

## 3. Stratégie : 8 commits, chacun garde le build vert

Chaque étape est un commit indépendant. Le build doit passer après chaque commit. Si un commit casse le build, on rollback uniquement celui-ci.

### Préparation

```bash
# Brancher
git checkout master
git pull
git checkout -b refactor/architecture-v4

# Confirmer que le build est vert AVANT toute modification
./gradlew build
./gradlew test
```

Si le build n'est pas vert avant le refactor, **arrêter ici** et corriger d'abord.

---

### Commit 0 : ajouter / mettre à jour les specs

**Objectif** : verrouiller les sources d'autorité avant tout déplacement de code, pour que Codex et les autres agents lisent les bonnes règles dès le début.

**Fichiers à mettre à jour** :
- `docs/ARCHITECTURE.md`
- `docs/spec_veil_portail_shaders_V4_3.md`
- `docs/spec_shaders_backrooms_V2.md`

**Validation docs** :
```bash
grep -R "V4[_]2\|Veil 2[.]x\|config[.]veil\|TODO[:]" docs/
```

Objectif : zéro résultat, sauf historique explicitement marqué.

**Message de commit** :
```
docs(arch): align architecture and shader specs before migration

Update authority order, Veil 3.x Phase 0 locking, bridge-only Veil access,
and shader spec references before any Java refactor.
```

---

### Commit 1 : créer les nouveaux packages vides + `package-info.java`

**Objectif** : créer la structure complète sans déplacer aucun fichier existant. Aucun risque de régression.

**Action** : créer ces dossiers vides et y déposer un `package-info.java` minimal (juste pour que git les commit) :

```
core/
core/lifecycle/
core/tick/
core/annotation/
core/event/

feature/
feature/gang/
feature/gang/item/
feature/gang/item/gang_badge/
feature/gang/block/
feature/gang/network/
feature/gang/network/c2s/
feature/gang/network/codec/
feature/gang/client/
feature/cursed/
feature/cursed/item/
feature/cursed/item/cursed_snack/
feature/cursed/block/

backrooms/
backrooms/config/
backrooms/bridge/
backrooms/shader/
backrooms/shader/uniform/
backrooms/shader/uniform/provider/
backrooms/shader/definitions/
backrooms/shader/profile/
backrooms/shader/effect/
backrooms/shader/effect/builtin/
backrooms/shader/level/
backrooms/shader/health/
backrooms/portal/
backrooms/portal/shared/
backrooms/portal/transition/
backrooms/portal/illusion/
backrooms/portal/illusion/rule/
backrooms/portal/aggro/
backrooms/portal/client/
backrooms/portal/client/view/
backrooms/portal/client/audio/
backrooms/portal/client/entity_echo/
backrooms/portal/client/effect/
backrooms/debug/
backrooms/level/
backrooms/level/common/
backrooms/level/common/noclip/
backrooms/level/common/registry/
backrooms/level/level0/
backrooms/level/level0/block/
backrooms/level/level0/biome/
backrooms/level/level0/generation/
backrooms/level/level0/generation/layer/
backrooms/level/level0/generation/layout/
backrooms/level/level0/generation/layout/room/
backrooms/level/level0/generation/surface/
backrooms/level/level0/generation/perf/
backrooms/level/level0/client/
backrooms/level/level0/client/shader/
backrooms/level/level0/client/ambient/
backrooms/entity/
backrooms/network/
backrooms/network/codec/
backrooms/network/c2s/
backrooms/network/s2c/

system/
system/sanity/
system/sanity/client/
system/ambient/
system/quest/

network/codec/
network/c2s/
network/s2c/

mixin/client/
mixin/common/
mixin/access/

datagen/
datagen/provider/
datagen/builder/

perf/section/

recovery/

tag/
sound/
```

Format des `package-info.java` (un par dossier) :

```java
/**
 * <Description courte du package>.
 *
 * Voir docs/ARCHITECTURE.md §3 pour le rôle exact (draft local : 1 - Arborescence/arborescence_v4.md).
 */
package com.petassegang.addons.<chemin>;
```

**Vérification** :
```bash
./gradlew build  # doit rester vert — aucun code n'a bougé
```

**Message de commit** :
```
chore(arch): create empty target package structure (refactor v4)

Create all target packages with package-info.java placeholders.
No code moved. Build remains green.
```

---

### Commit 2 : déplacer `ModConstants` vers `core/`

**Objectif** : valider le pattern de déplacement sur le fichier le plus simple.

**Action** :

```bash
# Déplacer le fichier
git mv src/main/java/com/petassegang/addons/util/ModConstants.java \
       src/main/java/com/petassegang/addons/core/ModConstants.java
```

**Modifier `ModConstants.java`** :
```java
package com.petassegang.addons.core;  // ← changer ici

// (le reste inchangé)
```

**Mettre à jour les imports** dans tous les fichiers qui utilisent `ModConstants` :

```bash
# Recherche-remplacement global
grep -rl "import com.petassegang.addons.util.ModConstants" src/ \
  | xargs sed -i 's|com\.petassegang\.addons\.util\.ModConstants|com.petassegang.addons.core.ModConstants|g'
```

**Note Windows** : si tu es sous Windows sans `sed`, utilise PowerShell :
```powershell
Get-ChildItem -Path src -Recurse -Filter *.java |
  ForEach-Object {
    (Get-Content $_.FullName) -replace 'com\.petassegang\.addons\.util\.ModConstants', 'com.petassegang.addons.core.ModConstants' |
    Set-Content $_.FullName
  }
```

**Vérification** :
```bash
./gradlew build
./gradlew test
```

**Message de commit** :
```
refactor(arch): move ModConstants from util/ to core/

Validates the move pattern on the simplest file.
All imports updated. Build and tests green.
```

---

### Commit 3 : migrer le **Gang Badge** (Phase pilote)

**Objectif** : valider le pattern feature-vertical sur la feature la plus simple.

**Fichiers à déplacer** :

| Source | Destination |
|--------|-------------|
| `item/GangBadgeItem.java` | `feature/gang/item/gang_badge/GangBadgeItem.java` |
| `network/packet/GangBadgeActivatePayload.java` | `feature/gang/network/c2s/GangBadgeActivatePayload.java` |
| `client/handler/GangBadgeClientHandler.java` | `feature/gang/client/GangBadgeClientHandler.java` |

**Actions** :

```bash
# Déplacements
git mv src/main/java/com/petassegang/addons/item/GangBadgeItem.java \
       src/main/java/com/petassegang/addons/feature/gang/item/gang_badge/GangBadgeItem.java

git mv src/main/java/com/petassegang/addons/network/packet/GangBadgeActivatePayload.java \
       src/main/java/com/petassegang/addons/feature/gang/network/c2s/GangBadgeActivatePayload.java

git mv src/main/java/com/petassegang/addons/client/handler/GangBadgeClientHandler.java \
       src/main/java/com/petassegang/addons/feature/gang/client/GangBadgeClientHandler.java
```

**Mettre à jour les `package` dans chaque fichier déplacé** :

- `GangBadgeItem.java` → `package com.petassegang.addons.feature.gang.item.gang_badge;`
- `GangBadgeActivatePayload.java` → `package com.petassegang.addons.feature.gang.network.c2s;`
- `GangBadgeClientHandler.java` → `package com.petassegang.addons.feature.gang.client;`

**Mettre à jour les imports** dans tous les fichiers qui utilisent ces 3 classes :

```bash
# GangBadgeItem
grep -rl "com.petassegang.addons.item.GangBadgeItem" src/ \
  | xargs sed -i 's|com\.petassegang\.addons\.item\.GangBadgeItem|com.petassegang.addons.feature.gang.item.gang_badge.GangBadgeItem|g'

# GangBadgeActivatePayload
grep -rl "com.petassegang.addons.network.packet.GangBadgeActivatePayload" src/ \
  | xargs sed -i 's|com\.petassegang\.addons\.network\.packet\.GangBadgeActivatePayload|com.petassegang.addons.feature.gang.network.c2s.GangBadgeActivatePayload|g'

# GangBadgeClientHandler
grep -rl "com.petassegang.addons.client.handler.GangBadgeClientHandler" src/ \
  | xargs sed -i 's|com\.petassegang\.addons\.client\.handler\.GangBadgeClientHandler|com.petassegang.addons.feature.gang.client.GangBadgeClientHandler|g'
```

**Vérification critique** :
```bash
./gradlew build
./gradlew test
./gradlew runClient   # ← obligatoire à cette étape : tester le Gang Badge en jeu
```

**Test in-game manuel** : créer un Gang Badge, l'activer (clic droit), vérifier que la fonctionnalité marche exactement comme avant.

**Message de commit** :
```
refactor(arch): migrate Gang Badge to feature/gang/ vertical structure

Moves:
  item/GangBadgeItem -> feature/gang/item/gang_badge/
  network/packet/GangBadgeActivatePayload -> feature/gang/network/c2s/
  client/handler/GangBadgeClientHandler -> feature/gang/client/

All references updated. Build, tests, and in-game test green.
No GangFeature.java entry point yet (Phase 1 dev).
```

---

### Commit 4 : migrer le **Cursed Snack**

**Objectif** : appliquer le même pattern, plus court.

**Fichiers à déplacer** :

| Source | Destination |
|--------|-------------|
| `item/CursedSnackItem.java` | `feature/cursed/item/cursed_snack/CursedSnackItem.java` |

**Actions** :

```bash
git mv src/main/java/com/petassegang/addons/item/CursedSnackItem.java \
       src/main/java/com/petassegang/addons/feature/cursed/item/cursed_snack/CursedSnackItem.java
```

**Modifier le `package`** dans le fichier déplacé :
```java
package com.petassegang.addons.feature.cursed.item.cursed_snack;
```

**Mettre à jour les imports** :
```bash
grep -rl "com.petassegang.addons.item.CursedSnackItem" src/ \
  | xargs sed -i 's|com\.petassegang\.addons\.item\.CursedSnackItem|com.petassegang.addons.feature.cursed.item.cursed_snack.CursedSnackItem|g'
```

**Note** : le repo actuel n'a pas de `CursedLogBlock` séparé d'après le README. Si l'arbre maudit existe ailleurs (ex: dans les ressources ou via tags), aucun déplacement Java supplémentaire n'est nécessaire ici. Les blocs vanilla utilisés (logs réskinnés via texture pack ou similaire) ne déplacent rien côté Java.

**Vérification** :
```bash
./gradlew build
./gradlew test
./gradlew runClient   # tester la consommation du Cursed Snack
```

**Message de commit** :
```
refactor(arch): migrate Cursed Snack to feature/cursed/ vertical structure

Moves:
  item/CursedSnackItem -> feature/cursed/item/cursed_snack/

All references updated. Build, tests, and in-game test green.
```

---

### Commit 5 : migrer **Backrooms Level 0 — bloc wallpaper et son BlockEntity**

**Objectif** : regrouper le code wallpaper (Block + BlockEntity + 3 modèles client) dans un seul package `wallpaper/`.

**C'est le plus gros déplacement** : 5 fichiers vers un seul dossier vertical.

**Fichiers à déplacer** :

| Source | Destination |
|--------|-------------|
| `block/LevelZeroWallpaperBlock.java` | `backrooms/level/level0/block/wallpaper/Level0WallpaperBlock.java` |
| `block/entity/LevelZeroWallpaperBlockEntity.java` | `backrooms/level/level0/block/wallpaper/Level0WallpaperBlockEntity.java` |
| `client/model/LevelZeroWallpaperBlockStateModel.java` | `backrooms/level/level0/block/wallpaper/Level0WallpaperBlockStateModel.java` |
| `client/model/LevelZeroWallpaperBakedModel.java` | `backrooms/level/level0/block/wallpaper/Level0WallpaperBakedModel.java` |
| `client/model/LevelZeroWallpaperModelHandler.java` | `backrooms/level/level0/block/wallpaper/Level0WallpaperModelHandler.java` |

**Important** : on en profite pour **renommer** `LevelZero*` → `Level0*` (cohérence avec l'arbo cible). Si tu préfères garder `LevelZero*` pour l'instant, c'est OK aussi — tu changes juste les destinations. **Décide maintenant** :

- Option A : renommer en `Level0*` pendant le déplacement (cohérence parfaite avec l'arbo)
- Option B : garder `LevelZero*` (zéro renommage)

> **Recommandation** : Option A. Le renommage est plus simple à faire en un coup pendant qu'on déplace que séparément plus tard.

**Actions (Option A — renommage)** :

```bash
# Block
git mv src/main/java/com/petassegang/addons/block/LevelZeroWallpaperBlock.java \
       src/main/java/com/petassegang/addons/backrooms/level/level0/block/wallpaper/Level0WallpaperBlock.java

# BlockEntity
git mv src/main/java/com/petassegang/addons/block/entity/LevelZeroWallpaperBlockEntity.java \
       src/main/java/com/petassegang/addons/backrooms/level/level0/block/wallpaper/Level0WallpaperBlockEntity.java

# Client models
git mv src/main/java/com/petassegang/addons/client/model/LevelZeroWallpaperBlockStateModel.java \
       src/main/java/com/petassegang/addons/backrooms/level/level0/block/wallpaper/Level0WallpaperBlockStateModel.java

git mv src/main/java/com/petassegang/addons/client/model/LevelZeroWallpaperBakedModel.java \
       src/main/java/com/petassegang/addons/backrooms/level/level0/block/wallpaper/Level0WallpaperBakedModel.java

git mv src/main/java/com/petassegang/addons/client/model/LevelZeroWallpaperModelHandler.java \
       src/main/java/com/petassegang/addons/backrooms/level/level0/block/wallpaper/Level0WallpaperModelHandler.java
```

**Modifier dans chaque fichier déplacé** :

1. Changer le `package` :
   ```java
   package com.petassegang.addons.backrooms.level.level0.block.wallpaper;
   ```

2. Renommer la **classe** : `LevelZeroWallpaperXxx` → `Level0WallpaperXxx`

**Mettre à jour les imports + références au nom de classe** dans tout le codebase :

```bash
# Renommage classes (ordre important : faire l'imports d'abord, puis les références)
grep -rl "com.petassegang.addons.block.LevelZeroWallpaperBlock" src/ \
  | xargs sed -i 's|com\.petassegang\.addons\.block\.LevelZeroWallpaperBlock|com.petassegang.addons.backrooms.level.level0.block.wallpaper.Level0WallpaperBlock|g'

grep -rl "com.petassegang.addons.block.entity.LevelZeroWallpaperBlockEntity" src/ \
  | xargs sed -i 's|com\.petassegang\.addons\.block\.entity\.LevelZeroWallpaperBlockEntity|com.petassegang.addons.backrooms.level.level0.block.wallpaper.Level0WallpaperBlockEntity|g'

grep -rl "com.petassegang.addons.client.model.LevelZeroWallpaperBlockStateModel" src/ \
  | xargs sed -i 's|com\.petassegang\.addons\.client\.model\.LevelZeroWallpaperBlockStateModel|com.petassegang.addons.backrooms.level.level0.block.wallpaper.Level0WallpaperBlockStateModel|g'

grep -rl "com.petassegang.addons.client.model.LevelZeroWallpaperBakedModel" src/ \
  | xargs sed -i 's|com\.petassegang\.addons\.client\.model\.LevelZeroWallpaperBakedModel|com.petassegang.addons.backrooms.level.level0.block.wallpaper.Level0WallpaperBakedModel|g'

grep -rl "com.petassegang.addons.client.model.LevelZeroWallpaperModelHandler" src/ \
  | xargs sed -i 's|com\.petassegang\.addons\.client\.model\.LevelZeroWallpaperModelHandler|com.petassegang.addons.backrooms.level.level0.block.wallpaper.Level0WallpaperModelHandler|g'

# Renommage des références au nom de classe (LevelZero -> Level0 pour les classes wallpaper)
grep -rl "LevelZeroWallpaperBlock" src/ | xargs sed -i 's|LevelZeroWallpaperBlock|Level0WallpaperBlock|g'
grep -rl "LevelZeroWallpaperBlockEntity" src/ | xargs sed -i 's|LevelZeroWallpaperBlockEntity|Level0WallpaperBlockEntity|g'
grep -rl "LevelZeroWallpaperBlockStateModel" src/ | xargs sed -i 's|LevelZeroWallpaperBlockStateModel|Level0WallpaperBlockStateModel|g'
grep -rl "LevelZeroWallpaperBakedModel" src/ | xargs sed -i 's|LevelZeroWallpaperBakedModel|Level0WallpaperBakedModel|g'
grep -rl "LevelZeroWallpaperModelHandler" src/ | xargs sed -i 's|LevelZeroWallpaperModelHandler|Level0WallpaperModelHandler|g'
```

⚠️ **Attention aux Identifiers Minecraft** : `Identifier.of(MOD_ID, "level_zero_wallpaper")` utilise un **path resource**, pas un nom de classe Java. Les paths de ressources et le mod ID restent **inchangés** côté game. On ne renomme **que** les classes Java. Si tu trouves dans le code une chaîne `"level_zero_wallpaper"` (en string), tu **ne** la touches pas. Le grep ci-dessus ne matche que les noms de classes Java, donc safe.

**Vérification critique** :
```bash
./gradlew build
./gradlew test
./gradlew runClient   # ← test in-game obligatoire
```

**Test in-game obligatoire** :
1. Téléporter dans la dimension Level 0 (`/execute in petasse_gang_additions:level_0 ...`)
2. Vérifier que les murs wallpaper s'affichent correctement (rendu adaptatif par face exposée)
3. Vérifier qu'aucun warning ne sort dans les logs concernant des resources manquantes
4. Lancer `./gradlew benchmarkLevelZeroGeneration` — vérifier que le temps de génération n'a pas changé significativement

**Message de commit** :
```
refactor(arch): migrate Level0 wallpaper to backrooms/level/level0/block/wallpaper/

Co-locates the 5 wallpaper-related files (Block, BlockEntity, BakedModel,
BlockStateModel, ModelHandler) in a single vertical package.

Renamed classes LevelZeroWallpaper* -> Level0Wallpaper* (consistency).
Resource identifiers (level_zero_wallpaper) unchanged — game state preserved.

In-game test: rendering OK, benchmark stable.
```

---

### Commit 6 : migrer **Backrooms Level 0 — chunk gen + layout + biome**

**Objectif** : regrouper le pipeline de génération.

**Fichiers à déplacer** :

| Source | Destination |
|--------|-------------|
| `world/backrooms/level0/LevelZeroChunkGenerator.java` | `backrooms/level/level0/generation/Level0ChunkGenerator.java` |
| `world/backrooms/level0/LevelZeroLayout.java` | `backrooms/level/level0/generation/layout/Level0Layout.java` |
| `world/backrooms/level0/LevelZeroSurfaceBiome.java` | `backrooms/level/level0/biome/Level0SurfaceBiome.java` |
| `world/backrooms/BackroomsConstants.java` | `backrooms/BackroomsConstants.java` |

**Actions** :

```bash
# ChunkGenerator
git mv src/main/java/com/petassegang/addons/world/backrooms/level0/LevelZeroChunkGenerator.java \
       src/main/java/com/petassegang/addons/backrooms/level/level0/generation/Level0ChunkGenerator.java

# Layout
git mv src/main/java/com/petassegang/addons/world/backrooms/level0/LevelZeroLayout.java \
       src/main/java/com/petassegang/addons/backrooms/level/level0/generation/layout/Level0Layout.java

# Biome
git mv src/main/java/com/petassegang/addons/world/backrooms/level0/LevelZeroSurfaceBiome.java \
       src/main/java/com/petassegang/addons/backrooms/level/level0/biome/Level0SurfaceBiome.java

# BackroomsConstants
git mv src/main/java/com/petassegang/addons/world/backrooms/BackroomsConstants.java \
       src/main/java/com/petassegang/addons/backrooms/BackroomsConstants.java
```

**Modifier les `package`** :
- `Level0ChunkGenerator` → `package com.petassegang.addons.backrooms.level.level0.generation;`
- `Level0Layout` → `package com.petassegang.addons.backrooms.level.level0.generation.layout;`
- `Level0SurfaceBiome` → `package com.petassegang.addons.backrooms.level.level0.biome;`
- `BackroomsConstants` → `package com.petassegang.addons.backrooms;`

**Renommer classes** : `LevelZero*` → `Level0*`

**Mettre à jour imports + références** :

```bash
# Imports
grep -rl "com.petassegang.addons.world.backrooms.level0.LevelZeroChunkGenerator" src/ \
  | xargs sed -i 's|com\.petassegang\.addons\.world\.backrooms\.level0\.LevelZeroChunkGenerator|com.petassegang.addons.backrooms.level.level0.generation.Level0ChunkGenerator|g'

grep -rl "com.petassegang.addons.world.backrooms.level0.LevelZeroLayout" src/ \
  | xargs sed -i 's|com\.petassegang\.addons\.world\.backrooms\.level0\.LevelZeroLayout|com.petassegang.addons.backrooms.level.level0.generation.layout.Level0Layout|g'

grep -rl "com.petassegang.addons.world.backrooms.level0.LevelZeroSurfaceBiome" src/ \
  | xargs sed -i 's|com\.petassegang\.addons\.world\.backrooms\.level0\.LevelZeroSurfaceBiome|com.petassegang.addons.backrooms.level.level0.biome.Level0SurfaceBiome|g'

grep -rl "com.petassegang.addons.world.backrooms.BackroomsConstants" src/ \
  | xargs sed -i 's|com\.petassegang\.addons\.world\.backrooms\.BackroomsConstants|com.petassegang.addons.backrooms.BackroomsConstants|g'

# Noms de classes
grep -rl "LevelZeroChunkGenerator" src/ | xargs sed -i 's|LevelZeroChunkGenerator|Level0ChunkGenerator|g'
grep -rl "LevelZeroLayout" src/ | xargs sed -i 's|LevelZeroLayout|Level0Layout|g'
grep -rl "LevelZeroSurfaceBiome" src/ | xargs sed -i 's|LevelZeroSurfaceBiome|Level0SurfaceBiome|g'
```

**Vérification critique** :
```bash
./gradlew build
./gradlew test
./gradlew benchmarkLevelZeroGeneration   # ← obligatoire ici
./gradlew runClient                       # téléport dans Level 0, vérifier génération
```

**Message de commit** :
```
refactor(arch): migrate Level0 generation pipeline + BackroomsConstants

Moves:
  world/backrooms/level0/LevelZeroChunkGenerator -> backrooms/level/level0/generation/
  world/backrooms/level0/LevelZeroLayout         -> backrooms/level/level0/generation/layout/
  world/backrooms/level0/LevelZeroSurfaceBiome   -> backrooms/level/level0/biome/
  world/backrooms/BackroomsConstants             -> backrooms/

Renamed classes LevelZero* -> Level0*. Resource identifiers unchanged.
benchmarkLevelZeroGeneration: stable.
```

---

### Commit 7 : nettoyer les anciens packages vides

**Objectif** : supprimer les dossiers source qui sont désormais vides après les déplacements.

**Anciens dossiers maintenant vides** (à vérifier avant suppression) :
- `src/main/java/com/petassegang/addons/util/` (ModConstants déplacé en commit 2)
- `src/main/java/com/petassegang/addons/item/` (les 2 items déplacés en commits 3-4)
- `src/main/java/com/petassegang/addons/block/entity/`
- `src/main/java/com/petassegang/addons/block/`
- `src/main/java/com/petassegang/addons/network/packet/`
- `src/main/java/com/petassegang/addons/client/handler/`
- `src/main/java/com/petassegang/addons/client/model/`
- `src/main/java/com/petassegang/addons/client/` (si vide après les sous-dossiers)
- `src/main/java/com/petassegang/addons/world/backrooms/level0/`
- `src/main/java/com/petassegang/addons/world/backrooms/`
- `src/main/java/com/petassegang/addons/world/`

**Actions** :

```bash
# Lister tous les dossiers vides
find src/main/java/com/petassegang/addons -type d -empty

# Si la liste correspond à ce qu'on attend, les supprimer
find src/main/java/com/petassegang/addons -type d -empty -delete
```

**Note** : `network/` n'est **pas** vide — il contient toujours `ModNetworking.java`. Ce fichier sert de hub global, on le laisse là.

`client/` est dans une zone grise : si `ModelLoaderRegistry.java` n'existe pas encore, le dossier est vide après cleanup. Si tu veux, tu peux soit le supprimer (il sera recréé plus tard quand on en aura besoin), soit y laisser un `package-info.java` du commit 1.

**Vérification finale** :
```bash
./gradlew clean
./gradlew build
./gradlew test
./gradlew runClient
./gradlew benchmarkLevelZeroGeneration
```

**Message de commit** :
```
chore(arch): remove now-empty legacy packages

Cleanup of packages emptied by previous migration commits:
  util/, item/, block/, block/entity/,
  network/packet/, client/handler/, client/model/,
  world/, world/backrooms/, world/backrooms/level0/

Final architecture matches docs/ARCHITECTURE.md §3 for existing features.
Veil/portal/lifecycle packages remain empty (filled in later phases).
```

---

## 4. Récap final

À la fin des 8 commits, l'état du repo est :

```
src/main/java/com/petassegang/addons/
├── PeTaSsEgAnGAdditionsMod.java
├── PeTaSsEgAnGAdditionsClientMod.java
│
├── core/
│   └── ModConstants.java
│   └── (lifecycle/, tick/, annotation/, event/ vides)
│
├── config/
│   └── ModConfig.java
│
├── creative/
│   └── ModCreativeTab.java
│
├── init/
│   ├── ModBlocks.java
│   ├── ModBlockEntities.java
│   ├── ModItems.java
│   └── ModChunkGenerators.java
│
├── network/
│   └── ModNetworking.java
│   └── (codec/, c2s/, s2c/ vides)
│
├── feature/
│   ├── gang/
│   │   ├── item/gang_badge/GangBadgeItem.java
│   │   ├── network/c2s/GangBadgeActivatePayload.java
│   │   └── client/GangBadgeClientHandler.java
│   └── cursed/
│       └── item/cursed_snack/CursedSnackItem.java
│
├── backrooms/
│   ├── BackroomsConstants.java
│   ├── (config/, bridge/, shader/, debug/ vides)
│   ├── portal/                                  ← TOUT VIDE
│   │   ├── shared/
│   │   ├── transition/
│   │   ├── illusion/rule/
│   │   ├── aggro/
│   │   └── client/{view,audio,entity_echo,effect}/
│   ├── level/
│   │   ├── common/{noclip,registry}/            ← VIDE
│   │   └── level0/
│   │       ├── block/
│   │       │   └── wallpaper/
│   │       │       ├── Level0WallpaperBlock.java
│   │       │       ├── Level0WallpaperBlockEntity.java
│   │       │       ├── Level0WallpaperBakedModel.java
│   │       │       ├── Level0WallpaperBlockStateModel.java
│   │       │       └── Level0WallpaperModelHandler.java
│   │       ├── biome/
│   │       │   └── Level0SurfaceBiome.java
│   │       ├── generation/
│   │       │   ├── Level0ChunkGenerator.java
│   │       │   └── layout/
│   │       │       └── Level0Layout.java
│   │       └── client/{shader,ambient}/         ← VIDE
│   ├── entity/                                  ← VIDE
│   └── network/{codec,c2s,s2c}/                 ← VIDE
│
├── system/{sanity,ambient,quest}/               ← TOUT VIDE
├── client/                                      ← VIDE pour l'instant
├── sound/, tag/, mixin/, datagen/, perf/, recovery/  ← VIDES
└── util/                                        ← SUPPRIMÉ
```

**Statut** :
- ✅ Tous les fichiers existants sont à leur place cible
- ✅ Aucune régression (build vert, runClient OK, benchmark stable, in-game test OK)
- ✅ Tous les packages futurs sont créés et prêts à recevoir le code Veil/portails/Lifecycle quand tu attaqueras ces phases
- ✅ Les renommages `LevelZero*` → `Level0*` sont cohérents avec l'arbo cible

---

## 5. Validation finale (avant merge)

Avant de merger `refactor/architecture-v4` dans `master` :

```bash
# Build complet
./gradlew clean build

# Tests
./gradlew test

# Test in-game complet
./gradlew runClient
# → vérifier : Gang Badge fonctionne, Cursed Snack fonctionne,
#   Level 0 se génère correctement avec wallpaper adaptatif

# Benchmark de non-régression
./gradlew benchmarkLevelZeroGeneration
# → comparer avec les valeurs de référence avant refactor
```

Si tout est vert → **merge**.

```bash
git checkout master
git merge --no-ff refactor/architecture-v4
git push origin master
```

---

## 6. Ce qui n'est PAS fait dans cette migration

Volontairement laissé pour plus tard :

- ❌ Implémentation `Lifecycle`, `PortalSubsystem`, `PortalContext`, `TickPhase` (Phase 1 dev)
- ❌ Bridges Veil/Iris (Phase 0 + 1)
- ❌ Création des `XxxFeature.java` (`GangFeature`, `CursedFeature`, `BackroomsFeature`, `Level0Feature`) — viendra avec Lifecycle
- ❌ Ajout du PBR sur les textures Level 0 (Phase 8 PBR)
- ❌ Création de `phase0_findings.md`
- ❌ `ARCHITECTURE.md` doit être mis à jour AVANT la migration.
- ❌ Datagen Java
- ❌ Splitting des `init/Mod*.java` en sous-classes par feature

Ces étapes viennent **après** le refactor d'arbo. Le refactor est un préalable propre, pas un changement fonctionnel.

---

## 7. Notes Windows / encodage

Si tu travailles sous Windows avec un chemin de projet contenant des caractères accentués (cf. `docs/TROUBLESHOOTING.md` du repo) :

- Préfère **PowerShell** plutôt que `bash` pour les recherche-remplacement
- Les commandes `git mv` marchent normalement
- Le `find ... -delete` doit être remplacé par :
  ```powershell
  Get-ChildItem -Path src -Recurse -Directory |
    Where-Object { (Get-ChildItem $_.FullName -Force | Measure-Object).Count -eq 0 } |
    Remove-Item
  ```

---

## 8. Rollback en cas de problème

Si un commit casse quelque chose :

```bash
# Annuler le dernier commit (mais garder les modifications)
git reset --soft HEAD^

# Ou annuler complètement (perte des modifs)
git reset --hard HEAD^

# Ou revert un commit déjà pushé
git revert <hash-commit>
```

Si tout part en vrille, retour à master :
```bash
git checkout master
git branch -D refactor/architecture-v4
```

---

*Fin du plan de migration. Une fois ces 8 commits terminés et validés, le refactor d'arbo est complet et tu peux attaquer Phase 0 (spikes Veil) ou les autres détails que tu avais en tête.*
