# Backrooms Level 0 — Pipeline de génération procédurale (v2)

## Contexte et objectif

## Note de conformité avec l'implémentation actuelle

Ce document reste la spec cible de référence, pas une photographie exacte du runtime actuel.

Ce qui est déjà actif en code :
- génération régionale avec extraction par chunk ;
- pipeline de layout explicite ;
- multi-layer réel via `layerIndex` et slices verticales canoniques ;
- writer dédié, biomes verticaux, lumière modulaire et détails muraux actifs ;
- instrumentation de performance opt-in.

Ce qui reste volontairement partiel ou différé :
- la couche `surface details` connectés est gelée côté rendu ;
- le vrai système de structures manuelles/prefabs n'est pas encore branché ;
- les connexions verticales sont reportées à ces grosses structures manuelles ;
- certaines structures de données de cette spec sont plus ambitieuses que le
  socle runtime actuel.

TODO DOC - À garder visible :
- mettre à jour cette note quand un pan de la spec passe vraiment en runtime actif ;
- ne pas traiter une section comme `faite` tant que son rendu final n'a pas été
  revalidé en jeu.

### Le projet en une phrase

Générer, en Java sur Fabric 1.21.1, une dimension Minecraft custom reproduisant le Level 0 des Backrooms (couloirs jaunes labyrinthiques, moquette, néons fluorescents, sans fenêtres, sans fin) avec une variété et une qualité visuelle supérieures à ce qu'un simple algo de labyrinthe produirait.

### Point de départ

- Un algo Python de référence : `https://github.com/davidpcahill/The-Backrooms-Map-Generator`, qui génère une grille 2D via **overlay de mini-labyrinthes indépendants** (Prim's algorithm, chaque mini-labyrinthe s'arrête avec une proba de 50 % s'il rencontre un couloir existant) combiné à du **stamping de rooms** (rectangulaires, à piliers, polygonales).
- Ce résultat est **cohérent visuellement mais manque de variété**. La solution est une **pipeline en étapes discrètes** qui enrichit progressivement la grille.

### Architecture retenue

- **Génération régionale avec extraction par chunk** : chaque chunk est reconstructible de façon déterministe sans dépendre de l'ordre de génération des chunks voisins. Les données macro (labyrinthe, noise, grandes zones, biomes) sont calculées à l'échelle d'une région de 8×8 chunks (128×128 blocs) puis extraites localement par chunk. Les données locales (lumières, détails, structures) sont calculées par chunk.
- **Multi-layer** : 3 à 5 étages empilés verticalement, chacun généré par la même pipeline avec son propre RNG seedé par `layerIndex`.
- **ChunkGenerator custom** qui extends `ChunkGenerator` et court-circuite entièrement la génération vanilla dans la dimension Backrooms.
- **Grille de cellules** : chaque cellule = 3×3 blocs Minecraft. Le labyrinthe est généré sur cette grille, puis converti en blocs.

### Géométrie physique d'un layer

Chaque layer (étage) a la structure verticale suivante :

```
Y+5  ┌─────────────────┐  ← plafond (1 bloc, bloc plafond du biome)
     │  espace intérieur │
     │  4 blocs de haut  │  ← air + murs (wallpaper du biome) + néons + détails
     │                   │
Y+1  ├─────────────────┤
Y+0  └─────────────────┘  ← sol (1 bloc, moquette/sol du biome)
```

**Hauteur intérieure : 4 blocs** entre le sol et le plafond (Y+1 à Y+4 inclus).

**Blocs utilisés :**
- **Sol (Y+0)** : bloc de sol du biome (moquette jaune, moquette tachée, béton, etc.)
- **Murs (Y+1 à Y+4)** : bloc wallpaper du biome sur les faces exposées à l'air. **Si un bloc wallpaper ne touche aucun bloc d'air** (c'est-à-dire qu'il est entouré d'autres murs de tous les côtés), **le remplacer par de la bedrock**. Cela économise du rendu (la bedrock n'a pas de modèle complexe) et renforce l'impression qu'il n'y a rien derrière les murs.
- **Plafond (Y+5)** : bloc de plafond du biome (dalles blanches, dalles jaunies, béton, etc.)

**Séparation entre layers : 4 blocs de bedrock.**

```
Layer N+1 sol      Y = baseY_N + 6 + 4
                   ┌──── 4 blocs bedrock ────┐
Layer N plafond    Y = baseY_N + 5
Layer N intérieur  Y = baseY_N + 1 à baseY_N + 4
Layer N sol        Y = baseY_N
```

Donc l'empreinte verticale totale d'un layer = **6 blocs** (1 sol + 4 intérieur + 1 plafond), et l'espacement entre deux layers consécutifs = **4 blocs de bedrock** entre le plafond d'un layer et le sol du suivant. Total par layer + séparation = **10 blocs de Y**.

**Exemple avec 3 layers :**
```
Bedrock pleine : Y = -64 à -1  (sous-sol de la dimension)
Layer 0 : Y = 0 à 5            (sol=0, intérieur=1-4, plafond=5)
Bedrock : Y = 6 à 9
Layer 1 : Y = 10 à 15          (sol=10, intérieur=11-14, plafond=15)
Bedrock : Y = 16 à 19
Layer 2 : Y = 20 à 25          (sol=20, intérieur=21-24, plafond=25)
Bedrock : Y = 26               (1 bloc au-dessus du dernier layer)
Air : Y = 27 à 319             (vide, jamais atteint)
```

**Bornes de la dimension :** cette section décrit la cible théorique. Dans l'implémentation active actuelle, la dimension du Level 0 tourne sur `min_y = 0` et `height = 64`, avec `5` layers utiles dans cette enveloppe technique compatible avec Minecraft. Le joueur n'a pas accès à des outils de minage dans les Backrooms, mais la bedrock empêche toute exploitation.

TODO DOC PLUS TARD :
- revalider ces bornes si le système final de structures multi-layer impose une
  hauteur technique différente ;
- garder la compatibilité avec les contraintes Minecraft réelles avant de
  revenir à une formulation plus ambitieuse.

**Règle de remplacement wallpaper → bedrock :**
Lors du placement final des blocs (`BlockWriter.write`), pour chaque bloc de mur aux positions Y+1 à Y+4 :
1. Placer le bloc wallpaper du biome.
2. Vérifier les 4 voisins horizontaux (N/E/S/W) et les 2 voisins verticaux (haut/bas) **à l'intérieur du chunk uniquement**.
3. Si **aucun** de ces voisins n'est de l'air → remplacer par de la bedrock.

**Bords de chunk :** un bloc wallpaper au bord exact du chunk ne peut pas vérifier le voisin dans le chunk adjacent (pas encore généré). Il reste wallpaper par défaut. Ce n'est pas un problème : si le voisin est de l'air (couloir de l'autre côté), le wallpaper est visible et DOIT rester wallpaper. Si le voisin est un mur, le wallpaper est invisible et la "non-optimisation" ne coûte qu'un bloc de rendu invisible en plus — négligeable.

Cette vérification se fait **après** le placement complet de tous les blocs du layer dans le chunk, en une passe de nettoyage.

---

## Contraintes non-négociables

### 1. Déterminisme total

Même seed + mêmes coordonnées → même résultat, toujours, peu importe l'ordre de génération des chunks.

**Règles concrètes :**

- **Un RNG par étape**, seedé via `hash(worldSeed, stageId, layerIndex, chunkX, chunkZ)`. Pas de `Random` global partagé. Pas de `Math.random()`. Pas de `new Random()` sans seed.
- **Pas d'itération sur `HashMap` / `HashSet`** — l'ordre d'itération varie entre JVMs. Utiliser `LinkedHashMap`, `TreeMap`, ou trier explicitement avant d'itérer.
- **Pas de parallélisme intra-étape** sauf avec synchronisation stricte de l'ordre d'écriture.
- **Chaque chunk est reconstructible sans dépendre de l'ordre de génération des voisins**. Les données macro sont calculées à l'échelle régionale (8×8 chunks) via hash déterministe des coordonnées de la région. Les données locales (étapes 5-7) sont calculables à partir des coordonnées du chunk et de la grille régionale en cache.
- **Test de non-régression obligatoire dès le début** : un test qui génère le chunk (0,0) avec seed 42, hash le résultat, et compare à une valeur attendue.

**API RNG normée — obligatoire partout :**

Toute création de `Random` dans le code de génération passe par une API unique. Cela garantit que le même algo de hash est utilisé partout — si deux étapes utilisent des méthodes de hash différentes, le déterminisme est cassé.

```java
public final class StageRandom {

    // Identifiants de stage : constantes long fixes, JAMAIS renommer ou réordonner.
    // Ajouter de nouveaux stages à la fin uniquement.
    public enum Stage {
        MAZE            (0x4A2B8C1D3E5F7A90L),
        DENSITY_MAP     (0xD3E5F7A90B1C2D4EL),
        NOISE_GEOMETRY  (0x1B3D5F7A9C0E2B4DL),
        LARGE_ROOMS     (0x7F6E5D4C3B2A1908L),
        BIOMES          (0x2C4E6A8B0D1F3759L),
        LIGHTS          (0x8A9B0C1D2E3F4567L),
        DETAILS         (0x3D5F7A9B1C2E4068L),
        STRUCTURES      (0x6B8D0F2A4C6E8A1BL),
        BORDER_CELLS    (0x9E1A3C5D7F0B2D4EL),
        VERTICAL_ANCHOR (0x5C7E9A1B3D5F7082L);

        public final long id;
        Stage(long id) { this.id = id; }
    }

    /**
     * Crée un Random déterministe pour une étape donnée.
     * Utilise un mix stable 64 bits (stafford variant 13)
     * pour combiner les composantes en un seed unique.
     */
    public static Random create(long worldSeed, Stage stage, int layer, int x, int z) {
        long hash = worldSeed;
        hash = mix(hash ^ stage.id);
        hash = mix(hash ^ Integer.toUnsignedLong(layer));
        hash = mix(hash ^ (Integer.toUnsignedLong(x) << 32 | Integer.toUnsignedLong(z)));
        return new Random(hash);
    }

    // Variante pour un salt supplémentaire (ex: index d'un mini-labyrinthe)
    public static Random create(long worldSeed, Stage stage, int layer, int x, int z, int salt) {
        long hash = worldSeed;
        hash = mix(hash ^ stage.id);
        hash = mix(hash ^ Integer.toUnsignedLong(layer));
        hash = mix(hash ^ (Integer.toUnsignedLong(x) << 32 | Integer.toUnsignedLong(z)));
        hash = mix(hash ^ Integer.toUnsignedLong(salt));
        return new Random(hash);
    }

    private static long mix(long x) {
        x = (x ^ (x >>> 30)) * 0xbf58476d1ce4e5b9L;
        x = (x ^ (x >>> 27)) * 0x94d049bb133111ebL;
        return x ^ (x >>> 31);
    }
}
```

**Règle dure :** `new Random(...)` est INTERDIT dans le code de génération sauf via `StageRandom.create()`. Les identifiants de stage sont des constantes `long` fixes dans un enum — jamais des strings. Un renommage de méthode ou de classe ne peut pas casser le déterminisme silencieusement.

### 2. Représentation intermédiaire taggée

Chaque cellule porte un **tag sémantique** :

```
WALL, CORRIDOR, ROOM_LARGE, ROOM_PILLAR,
OPEN_SPACE, ALCOVE, JUNCTION, DEAD_END
```

En plus du tag principal, chaque cellule porte des métadonnées :
- Biome visuel (index)
- Type de mur détaillé (`FULL`, `HALF_N/E/S/W`, `PILLAR_ONLY`)
- Flags : lumière (type + état), détails décoratifs

### 3. Approche 2D par layer

La génération est **strictement 2D par layer**. La 3D apparaît uniquement via :
- Des **connexions verticales ponctuelles** entre layers (escaliers, trous) traitées comme des structures préfabriquées aux points d'ancrage.
- Des **structures multi-layer** qui écrivent dans les grilles de plusieurs layers à leur emplacement, comme une exception locale.

TODO IMPLÉMENTATION PLUS TARD :
- reprendre ces connexions verticales uniquement via de grosses structures
  manuelles contrôlées ;
- ne pas réintroduire de petit escalier procédural autonome tant que
  l'immersion et l'encastrement ne sont pas garantis.

### 4. Système de coordonnées canonique

Toutes les conversions entre espaces de coordonnées doivent utiliser ces formules exactes. Les off-by-one entre bloc, cellule, chunk et région sont la source de bug n°1 dans les projets worldgen.

```
CONSTANTES :
  CELL_SIZE   = 3        (blocs par cellule)
  CHUNK_SIZE  = 16       (blocs par chunk)
  REGION_SIZE = 8        (chunks par région)
  REGION_BLOCKS = 128    (blocs par région = 8 × 16)
  REGION_CELLS  = 42     (cellules par région ≈ 128 / 3, arrondi bas)

PADDING RÉGIONAL (128 blocs vs 42×3 = 126 blocs) :
  Les 42 cellules couvrent les blocs 0 à 125 (126 blocs).
  Les blocs 126-127 (2 blocs restants) ne font partie d'aucune cellule.
  Ils sont remplis de WALL (bedrock après le remplacement wallpaper→bedrock).
  Ce padding est inerte : pas de CORRIDOR, pas de structure, pas de lumière.
  Le BlockWriter traite ces 2 colonnes comme du mur plein, systématiquement.
  Ce padding existe sur les bords EST et SUD de chaque région (X max et Z max).

CONVERSIONS :
  bloc → cellule :        cellX = blockX / 3          (division entière, floor)
                          cellZ = blockZ / 3

  cellule → bloc (coin) : blockX = cellX * 3
                          blockZ = cellZ * 3

  bloc → chunk :          chunkX = blockX >> 4         (division par 16, floor)
                          chunkZ = blockZ >> 4

  chunk → région :        regionX = chunkX >> 3        (division par 8, floor)
                          regionZ = chunkZ >> 3

  bloc → région :         regionX = blockX >> 7        (division par 128, floor)
                          regionZ = blockZ >> 7

COORDONNÉES LOCALES (dans une région) :
  bloc local :            localBlockX = blockX - (regionX * 128)
                          localBlockZ = blockZ - (regionZ * 128)
                          → valeurs de 0 à 127

  cellule locale :        localCellX = localBlockX / 3
                          localCellZ = localBlockZ / 3
                          → valeurs de 0 à 41

  chunk local :           localChunkX = chunkX - (regionX * 8)
                          localChunkZ = chunkZ - (regionZ * 8)
                          → valeurs de 0 à 7

COORDONNÉES VERTICALES :
  layer → baseY :         baseY = layer * 10
  baseY → sol :           Y = baseY
  baseY → intérieur :     Y = baseY + 1 à baseY + 4
  baseY → plafond :       Y = baseY + 5
  baseY → bedrock dessus: Y = baseY + 6 à baseY + 9 (sauf dernier layer)
```

**Conventions de bornes :** toutes les plages sont **inclusives aux deux extrémités** sauf mention contraire. La cellule (0,0) de la région correspond au coin nord-ouest (coordonnées X et Z minimales). Les coordonnées négatives sont supportées (les divisions utilisent `floor`, pas troncature vers zéro).

---

## Flux de données global

### Approche hybride : région cachée + étapes locales par chunk

Les étapes 1 à 4 (maze, noise, grandes zones, biomes) sont **calculées une fois par région** de 8×8 chunks (128×128 blocs) et stockées dans un **cache mémoire LRU**. Les étapes 5 à 7 (lumières, détails, structures) sont calculées **par chunk** puisqu'elles sont purement locales.

```
Quand Minecraft demande un chunk (cX, cZ) :

1. Calculer la région parente : regionX = cX >> 3, regionZ = cZ >> 3

2. La région (regionX, regionZ) est-elle en cache ?
   NON → Générer la grille de la région entière (128×128 blocs) :
     Pour chaque layer :
       [Étape 1] Maze generation (carte de densité macro + labyrinthes) sur toute la région
       [Étape 2] Noise geometry sur toute la région
       [Étape 3] Grandes zones sur toute la région
       [Étape 4] Biomes visuels sur toute la région
     → Stocker en cache LRU (clé = regionX, regionZ)
   OUI → Lire le cache

3. Extraire la portion du chunk (cX, cZ) depuis la grille région cachée

4. Pour chaque layer, sur ce chunk uniquement :
     [Étape 5] Lumières → positions et états des néons, zones de pénombre
     [Étape 6] Détails → plinthes, interrupteurs, haut-parleurs, salissures, taches
     [Étape 7] Structures → préfabriqués gameplay + zones liminales + connexions verticales

5. Placement des blocs dans le chunk (une passe unique)
```

**Pourquoi cette approche :**
- Les étapes 1-4 sont les plus lourdes (simulation de labyrinthes, Voronoï) et bénéficient d'une vue large. Les calculer par région élimine toute la redondance de simulation des mini-labyrinthes voisins.
- Les étapes 5-7 sont légères et purement locales (un néon ne dépend que du tag et du biome de sa cellule). Elles n'ont pas besoin d'une vue régionale.
- Minecraft génère les chunks en spirale autour du joueur. Les 64 chunks d'une même région sont demandés quasi consécutivement → le cache hit rate est très élevé.
- Le cache LRU évacue les régions éloignées du joueur. Taille recommandée : 8-16 régions (= 512-1024 chunks en mémoire). Empreinte mémoire : ~8-16 MB pour les grilles taggées.

**Déterminisme :** la grille régionale est seedée par `hash(worldSeed, stageId, layerIndex, regionX, regionZ)`. Que la région soit calculée depuis le cache ou recalculée de zéro, le résultat est identique.

Chaque étape prend la grille en entrée, l'enrichit, et la passe à la suivante.
Chaque étape est déterministe (RNG seedé via `StageRandom.create()`).

### Structures de données et mutabilité

**Trois types de données distincts — ne jamais les mélanger :**

```java
// === ENUMS ===

enum CellTag {
    WALL, CORRIDOR, ROOM_LARGE, ROOM_PILLAR,
    OPEN_SPACE, ALCOVE, JUNCTION, DEAD_END
}

enum WallType {
    FULL, HALF_WALL_N, HALF_WALL_E, HALF_WALL_S, HALF_WALL_W, PILLAR_ONLY, NONE
}

enum DensityZone {
    DENSE, NORMAL, SPARSE
}

// === STRUCTURES DE DONNÉES ===

class Cell {
    CellTag tag;           // tag sémantique principal
    WallType wallType;     // type de mur détaillé (défaut FULL pour WALL, NONE pour CORRIDOR)
    int biomeIndex;        // index du biome visuel (-1 = non assigné)
    DensityZone density;   // zone de densité macro (lu depuis la carte)
    boolean isAnchor;      // true si cette cellule est un point d'ancrage vertical
    // Flags étapes 5-7 (uniquement dans ChunkGrid, pas dans RegionGrid) :
    byte lightType;        // 0=aucun, 1=normal, 2=cassé, 3=clignotant
    byte detailFlags;      // bitmask : plinthe, interrupteur, haut-parleur, etc.
}

class CellGrid {
    final int width;       // en cellules
    final int height;      // en cellules
    final Cell[] cells;    // tableau linéaire, accès par [z * width + x]

    Cell get(int x, int z) { return cells[z * width + x]; }
    void set(int x, int z, Cell cell) { cells[z * width + x] = cell; }
}

// IMMUABLE après étapes 1-4. Stocké dans le cache LRU.
// Jamais modifié par les étapes 5-7 ni par le BlockWriter.
class RegionGrid {
    final CellGrid[] layers;       // une grille par layer (chaque grille = ~42×42 cellules)
    // La carte de densité est stockée dans Cell.density de chaque cellule,
    // PAS dans un tableau séparé. Source de vérité unique.

    // Retourne une COPIE de la portion chunk — pas une vue, pas une référence.
    ChunkGrid extractChunk(int layer, int chunkX, int chunkZ) {
        return new ChunkGrid(layers[layer], chunkX, chunkZ); // deep copy
    }
}

// MUTABLE. Copie locale pour un chunk, consommée par les étapes 5-7.
// Peut être modifiée librement — n'affecte jamais le cache.
class ChunkGrid {
    final Cell[] cells;    // copie des cellules de la région pour ce chunk (~5×5 cellules)
    final int width;       // en cellules (= ceil(16/3) = 6 au max)
    final int height;
    // Enrichi par étapes 5-7 : lightType, detailFlags, structures placées
}

// === INTERFACES DES ÉTAPES ===

// Étapes régionales (1-4) : travaillent sur la CellGrid entière de la région.
interface RegionStage {
    StageRandom.Stage getStageId();
    void apply(CellGrid regionGrid, long worldSeed, int layer, int regionX, int regionZ);
}

// Étapes locales (5-7) : travaillent sur le ChunkGrid extrait (copie mutable).
interface ChunkStage {
    StageRandom.Stage getStageId();
    void apply(ChunkGrid chunkGrid, long worldSeed, int layer, int chunkX, int chunkZ);
}

// Implémentations attendues :
// RegionStage : MazeGenerator, NoiseGeometry, LargeRooms, BiomeMapper
// ChunkStage  : LightPlacer, DetailPass, StructurePlacer

// Placement final des blocs — lit le ChunkGrid et écrit dans le Chunk Minecraft.
interface BlockWriter {
    void write(Chunk chunk, ChunkGrid grid, int baseY, int layer);
    void replaceHiddenWalls(Chunk chunk, int baseY, int layer);
}
```

### Ancrages verticaux — formule exacte

Les points d'ancrage verticaux sont calculés par hash sur une grille virtuelle grossière, indépendamment de toute génération. N'importe quel code peut savoir si un point est un ancrage en calculant le hash.

```
GRILLE D'ANCRAGES :
  Taille de cellule : 64×64 blocs (fixe)
  → chaque cellule d'ancrage couvre 4×4 chunks

Pour chaque cellule de la grille d'ancrages (anchorGridX, anchorGridZ) :
  rng = StageRandom.create(worldSeed, VERTICAL_ANCHOR, 0, anchorGridX, anchorGridZ)

  1. Cette cellule contient-elle un ancrage ?
     rng.nextFloat() < 0.15 → OUI (probabilité 15%, ~1 ancrage tous les 6-7 cellules)
     sinon → NON, passer à la cellule suivante

  2. Position exacte dans la cellule :
     localX = rng.nextInt(64)
     localZ = rng.nextInt(64)
     → position absolue en blocs :
       blockX = anchorGridX * 64 + localX
       blockZ = anchorGridZ * 64 + localZ
     → position en cellules :
       cellX = blockX / 3
       cellZ = blockZ / 3

  3. Portée verticale :
     startLayer = 0 (toujours depuis le layer 0)
     endLayer = rng.nextInt(NUM_LAYERS - 1) + 1
     → l'ancrage relie les layers startLayer à endLayer inclus
     → tous les ancrages ne traversent pas toute la pile

Pour savoir si un chunk contient un ancrage :
  calculer quelles cellules de la grille d'ancrages chevauchent le chunk
  (le chunk + les 8 cellules voisines de la grille, pour couvrir les bords)
  → re-calculer le hash pour chacune → O(9) par chunk, déterministe
```

**Règle dure :** `RegionGrid` est **immuable** une fois en cache. `extractChunk()` retourne toujours une **copie**. Si un chunk modifie sa grille locale (ajout de lumière, de détail, de structure), ça ne touche jamais le cache régional. Violer cette règle = bugs de corruption de données entre chunks de la même région.

### Invariants par étape

Chaque étape **garantit** certaines propriétés et **n'a pas le droit** d'en casser d'autres. Ces invariants doivent être respectés dans le code et sont testables.

```
ÉTAPE 1 — Maze generation
  GARANTIT :
    - Toute cellule est WALL, CORRIDOR, JUNCTION, ou DEAD_END
    - Les ancrages verticaux sont CORRIDOR et connectés au réseau
    - Les cellules de bord de région respectent le hash partagé
    - Connectivité locale minimale (pas d'îlots CORRIDOR isolés > 3 cellules)
    - La carte de densité macro (DENSE/NORMAL/SPARSE) est calculée et stockée
    - Les paramètres du maze (N, cap itérations, collision stop) varient selon la densité
  N'A PAS LE DROIT DE :
    - Modifier les cellules en dehors de la grille région

ÉTAPE 2 — Noise geometry
  GARANTIT :
    - Les cellules WALL peuvent devenir HALF_WALL_*, PILLAR_ONLY, ou rester WALL
    - Les cellules CORRIDOR adjacentes à un mur peuvent devenir ALCOVE
  N'A PAS LE DROIT DE :
    - Modifier les cellules CORRIDOR (seulement les WALL et les adjacentes)
    - Toucher aux ancrages verticaux (une cellule ancrage reste CORRIDOR)
    - Créer de nouvelles cellules WALL (seulement transformer des WALL existants)

ÉTAPE 3 — Grandes zones
  GARANTIT :
    - Les zones stampées ont au moins 1 ouverture vers un CORRIDOR adjacent
    - Les zones ont un biome pré-calculé (stocké en métadonnée de la zone)
    - L'espacement Poisson entre zones est respecté
  N'A PAS LE DROIT DE :
    - Écraser un ancrage vertical (la zone contourne ou l'intègre)

ÉTAPE 4 — Biomes visuels
  GARANTIT :
    - Toute cellule non-WALL a un biome_index valide
    - Aucune frontière de biome ne traverse une grande zone
    - Les cellules de grandes zones utilisent le biome de la zone, pas le Voronoï
  N'A PAS LE DROIT DE :
    - Modifier les tags topologiques (CORRIDOR, JUNCTION, ROOM_LARGE, etc.)
    - Modifier la géométrie (noise, murs, ouvertures)

--- Fin des étapes régionales (RegionGrid verrouillé, immuable) ---

ÉTAPE 5 — Lumières (sur ChunkGrid mutable)
  GARANTIT :
    - < 20 sources lumineuses par chunk
    - Néons cassés : émission = 0
    - Néons clignotants : émission fixe
  N'A PAS LE DROIT DE :
    - Modifier les tags topologiques ou les biomes

ÉTAPE 6 — Détails (sur ChunkGrid mutable)
  GARANTIT :
    - Tous les détails sont déterministes par hash de position
  N'A PAS LE DROIT DE :
    - Modifier les tags topologiques, biomes, ou lumières
    - Bloquer un passage (aucun détail ne rend une cellule CORRIDOR intraversable)

ÉTAPE 7 — Structures (sur ChunkGrid mutable)
  GARANTIT :
    - Les distances minimales entre structures sont respectées
    - Les connexions verticales tombent sur des ancrages CORRIDOR
    - Les structures n'écrasent pas d'autres structures
  N'A PAS LE DROIT DE :
    - Violer la traversabilité globale (une structure peut bloquer un couloir
      seulement si elle fournit un passage alternatif dans son .nbt)
```

---

## Les étapes en détail

### Étape 1 — Maze generation (corridors taggés)

**Input :** seed mondiale, coordonnées de la région, index du layer.
**Output :** grille de cellules taggées `WALL` / `CORRIDOR` / `JUNCTION` / `DEAD_END` pour toute la région (128×128 blocs = ~42×42 cellules de 3×3).

**Adaptation de l'algo Cahill à la génération par région :**

**Référence :** [`davidpcahill/The-Backrooms-Map-Generator`](https://github.com/davidpcahill/The-Backrooms-Map-Generator) — script Python (pygame) qui implémente l'algo original. Le code source fait ~200 lignes et constitue la base algorithmique à adapter en Java.

L'algo original lance ~1000 mini-labyrinthes à des positions aléatoires sur une grille finie. Chaque mini-labyrinthe grow via Prim's algorithm (voisins à distance 2 pour garder des murs entre les passages), et s'arrête avec 50 % de proba quand il touche un couloir existant.

Grâce à l'approche par région, on peut appliquer l'algo Cahill **quasi directement** sur la grille 128×128 de la région :

```
Pour générer la région (rX, rZ) sur le layer L :

1. Initialiser la grille région (128×128 blocs → ~42×42 cellules) à WALL

2. Sous-phase — Carte de densité macro :
   Avant de lancer les labyrinthes, générer une carte de densité
   basse fréquence qui module le comportement du maze par zone.

   Implémentation :
     - Voronoï basse résolution (1 macro-cellule = 32×32 blocs, soit ~4 par région)
     - Pour chaque macro-cellule : hash(worldSeed, L, macroCellX, macroCellZ)
       → type de densité pondéré :
         DENSE  (30%) : couloirs serrés, beaucoup de branches, claustro
         NORMAL (50%) : comportement par défaut
         SPARSE (20%) : labyrinthes étalés, plus de murs conservés, espace respirant
     - Calculer le type de chaque cellule de la grille par Voronoï
       (9 macro-cellules voisines testées, O(9), même technique que les biomes)

   Effet sur le maze (étape 3 ci-dessous) :
     DENSE :
       - nombre local de mini-labyrinthes × 1.4
       - MAX_ITERATIONS_PER_MAZE réduit à 150 (labyrinthes courts, denses)
       - STOP_COLLISION_PROBABILITY = 0.3 (s'arrête moins → plus de connexions)
     NORMAL :
       - paramètres par défaut (N, 250 itérations, 0.5 collision stop)
     SPARSE :
       - nombre local de mini-labyrinthes × 0.6
       - MAX_ITERATIONS_PER_MAZE augmenté à 350 (labyrinthes longs, étalés)
       - STOP_COLLISION_PROBABILITY = 0.7 (s'arrête plus vite → plus de murs)

   La carte de densité NE remplace PAS le maze — elle l'influence statistiquement.
   Le résultat reste organique et cohérent avec l'esthétique Cahill.
   La carte est stockée dans la RegionGrid pour être lisible par les étapes suivantes
   (les grandes zones à l'étape 3 peuvent préférer les zones SPARSE pour se placer).

3. Pré-placer les ancrages verticaux comme seeds de labyrinthes :
   Pour chaque point d'ancrage vertical dans la région
   (calculé par hash sur une grille grossière ~64×64 blocs) :
     - Utiliser cet ancrage comme position de départ du PREMIER
       mini-labyrinthe lancé dans cette zone.
     - Le labyrinthe qui démarre sur l'ancrage se connecte naturellement
       au reste du réseau → pas besoin de A* post-hoc.
     - Résultat : l'ancrage est garanti CORRIDOR et connecté organiquement.

4. Lancer N mini-labyrinthes sur la grille :
   N_base = hash(worldSeed, L, rX, rZ, "count") dans [80..150]

   Le N effectif par zone de la grille est modulé par la carte de densité :
     N_local = N_base × multiplicateur de densité de la zone

   Les premiers labyrinthes (étape 3) partent des ancrages.
   Les suivants partent de positions aléatoires par hash.

   Pour i = 0..N :
     - Position de départ : ancrage si disponible, sinon
       hash(worldSeed, L, rX, rZ, i) → (startX, startZ)
     - RNG du labyrinthe : seedé par hash(worldSeed, L, rX, rZ, i)
     - Grow via Prim (voisins à distance 2)
     - Proba d'arrêt sur collision : lue depuis la carte de densité
       à la position de départ du labyrinthe
     - **CAP D'ITÉRATIONS : lu depuis la carte de densité**
       (DENSE=150, NORMAL=250, SPARSE=350)
       Les derniers labyrinthes lancés sur une grille déjà dense
       trouvent peu d'espace libre et leurs listes de frontières
       grossissent inutilement. Le cap évite la dégénérescence
       en flood-fill de la zone restante et stabilise les perfs.
     - Pas de rayon max nécessaire (la grille région est bornée + le cap suffit)

5. Post-pass tagging :
   - CORRIDOR avec 3+ voisins CORRIDOR → JUNCTION
   - CORRIDOR avec 1 seul voisin CORRIDOR → DEAD_END

6. Gestion des bords de région :
   Les mini-labyrinthes sont bornés à la grille de la région.
   Pour assurer la connectivité entre régions adjacentes, les cellules
   de bord (1-2 cellules) sont traitées comme des "portes" :
     - Pour chaque cellule de bord, un hash déterministe
       hash(worldSeed, L, borderCellX, borderCellZ) décide si c'est
       un CORRIDOR ou un WALL.
     - La même cellule de bord est partagée par les deux régions
       adjacentes (mêmes coordonnées absolues → même hash → même résultat).
     - Les mini-labyrinthes qui touchent un bord CORRIDOR s'y connectent.
```

**Pourquoi c'est mieux que l'approche chunk-par-chunk :**
- Pas de simulation redondante des labyrinthes voisins. Chaque labyrinthe est simulé exactement une fois.
- L'algo Cahill s'applique quasi tel quel (overlay de N labyrinthes sur une grille finie).
- La grille régionale est calculée une seule fois et mise en cache pour les 64 chunks de la région.

**Paramètres clés (valeurs par défaut = NORMAL, modulées par la carte de densité) :**

| Paramètre | DENSE | NORMAL | SPARSE |
|---|---|---|---|
| `N` (mini-labyrinthes) | base × 1.4 | base (~80-150) | base × 0.6 |
| `MAX_ITERATIONS_PER_MAZE` | 150 | 250 | 350 |
| `STOP_COLLISION_PROBABILITY` | 0.3 | 0.5 | 0.7 |

- `REGION_SIZE` = 8 chunks = 128 blocs (fixe, indépendant de la densité)

**Spécification exacte de Prim (pour garantir le déterminisme entre implémentations) :**

```
Structure de données :
  frontier = ArrayList<Cell>  (PAS un HashSet, PAS un TreeSet)

Sélection du prochain candidat :
  index = rng.nextInt(frontier.size())
  cell = frontier.get(index)
  frontier.remove(index)  (swap-remove avec le dernier élément pour O(1))

Ajout des voisins :
  tester les 4 voisins à distance 2 (N/E/S/W) dans cet ordre FIXE : N, E, S, W
  pour chaque voisin non visité et dans les bornes de la grille :
    ajouter en fin de frontier (append)

Marquage du passage :
  la cellule intermédiaire (entre cell et le voisin choisi) est marquée CORRIDOR
  la cellule destination est marquée CORRIDOR et ajoutée à visited

Arrêt sur collision :
  si la cellule intermédiaire est DÉJÀ CORRIDOR (labyrinthe précédent) :
    collisionProba = STOP_COLLISION_PROBABILITY de la zone de densité
                     (DENSE=0.3, NORMAL=0.5, SPARSE=0.7)
    rng.nextFloat() < collisionProba → arrêter ce labyrinthe
    sinon → continuer normalement
```

Cette spécification garantit que deux implémentations produisent le même résultat bit-à-bit avec la même seed.

---

### Étape 2 — Noise geometry

**Input :** grille avec `CORRIDOR` / `WALL`.
**Output :** grille enrichie avec types de murs détaillés, colonnes orphelines, renfoncements.

**Principe :** du bruit Simplex 2D seuillé pour transformer les murs uniformes en géométrie variée.

```
Pour chaque cellule WALL du chunk :
  valeur = SimplexNoise(cellX * scale, cellZ * scale, seed_noise)

  Si valeur > SEUIL_HAUT → mur plein, pas de changement

  Si SEUIL_BAS < valeur < SEUIL_HAUT → demi-mur
    Direction selon le gradient du noise ou le voisinage
    Métadonnée : HALF_WALL_N / HALF_WALL_E / HALF_WALL_S / HALF_WALL_W

  Si valeur < SEUIL_BAS → mur supprimé, pilier 1 bloc conservé au coin
    Tag : PILLAR_ONLY
    Ressenti "parking souterrain" / "entrepôt"

Pour chaque cellule CORRIDOR adjacente à un mur :
  valeur2 = SimplexNoise(cellX * scale2, cellZ * scale2, seed_noise2)
  Si valeur2 > SEUIL_RENFONCEMENT → renfoncement de 1 bloc (alcôve)
    Tag : ALCOVE
```

**Outils :** `net.minecraft.util.math.noise.SimplexNoiseSampler` seedé via le RNG de l'étape. Calculable indépendamment par chunk (ne dépend que des coordonnées absolues).

**Note :** le noise décide du TAG de la cellule. Le placement réel des blocs dans le 3×3 est déterminé au moment du placement final. Exemple : `HALF_WALL_N` place des murs sur la rangée z=0 du 3×3 et laisse z=2 vide.

**Note sur les grandes zones (étape 3) :** certaines cellules modifiées par le noise seront écrasées par le stamp de grandes zones à l'étape suivante. C'est du calcul "gaspillé" mais négligeable : les grandes zones couvrent ~5-10 % de la surface, et le noise est O(1) par cellule. Ne pas ajouter de vérification "est-ce que cette cellule sera dans une grande zone" — ça couplerait les étapes 2 et 3 pour économiser des microsecondes.

---

### Étape 3 — Grandes zones (stamp)

**Input :** grille avec corridors, murs, noise geometry + carte de densité macro (depuis l'étape 1).
**Output :** grille avec zones `ROOM_LARGE`, `ROOM_PILLAR`, `OPEN_SPACE` carvées par-dessus le labyrinthe.

**Principe :** tamponner des espaces ouverts pour créer des landmarks spatiaux et casser la monotonie corridor-corridor. Les grandes zones se placent **préférentiellement dans les zones SPARSE** de la carte de densité (plus d'espace disponible, résultat plus naturel).

```
Pour chaque région (rX, rZ), layer L :

1. Calculer les grandes zones candidates dans la région.
   Les zones sont ancrées par hash sur une grille virtuelle grossière
   (~128×128 blocs par macro-cellule, soit 1 macro-cellule = 1 région) :

     M = 3 (FIXE — nombre de zones candidates testées par macro-cellule)

     pour j = 0..M-1 :
       anchorX = StageRandom.create(worldSeed, LARGE_ROOMS, L, macroX, macroZ, j).nextInt(128)
       anchorZ = StageRandom.create(worldSeed, LARGE_ROOMS, L, macroX, macroZ, j + 100).nextInt(128)
       zoneType = hash → GREAT_HALL (40%) | PILLAR_ROOM (35%) | OPEN_SPACE (25%)
       zoneWidth = hash :
         GREAT_HALL → 20-40 cellules
         PILLAR_ROOM → 8-15 cellules
         OPEN_SPACE → 10-25 cellules
       zoneHeight = hash (même plage que width, indépendant)

   Tester également les macro-cellules des 8 régions voisines (même formule,
   coordonnées de la région voisine) pour détecter les zones qui débordent
   dans la région courante.

2. Espacement Poisson par hash :
   Chaque zone candidate est acceptée seulement si aucune autre zone
   (calculable par hash dans le voisinage) n'est trop proche.
   Distance minimale entre zones : 80 cellules (FIXE).
   L'ordre d'évaluation est déterministe : triées par hash croissant.
   La première zone valide dans l'ordre "gagne", les suivantes trop proches sont rejetées.

3. Pour chaque zone acceptée qui chevauche la région :
   - Écraser toutes les cellules dans l'emprise → tag ROOM_LARGE, ROOM_PILLAR, ou OPEN_SPACE
   - Si ROOM_PILLAR : rétablir des piliers réguliers (tous les N cellules, N par hash)
   - Percer des ouvertures vers les CORRIDOR adjacents aux bords de la zone
   - Stocker le biome de la zone : hash(worldSeed, zoneAnchorX, zoneAnchorZ, L) → biome_index
```

---

### Étape 4 — Biomes visuels

**Input :** grille géométrique complète (maze + noise + grandes zones), index du layer.
**Output :** grille avec `biome_index` par cellule.

**Principe :** chaque biome visuel définit la palette : type de moquette, couleur du papier peint, variantes de néon, état d'usure. Les biomes varient entre layers — plus on descend, plus c'est dégradé.

```
1. Carte Voronoï déterministe basse résolution :
   - Grille virtuelle de macro-cellules (32-64 blocs)
   - Pour chaque macro-cellule : hash → position du seed Voronoï + type de biome
   - Biome de chaque cellule = seed Voronoï le plus proche (9 voisins testés, O(9))
   - Tirage pondéré par palette du layer :
     Layer 0 → 60 % pristine, 30 % stained, 10 % brown
     Layer 1 → 40 % stained, 30 % damp, 20 % concrete, 10 % rust
     Layer 2 → 30 % damp, 30 % concrete, 25 % rust, 15 % dark
   - RNG seedé : hash(worldSeed, "biomes", layerIndex, chunkX, chunkZ)

2. Règle de transition (frontières dans les couloirs uniquement) :
   Pour chaque cellule :
     Si la cellule appartient à une grande zone (ROOM_LARGE, ROOM_PILLAR, OPEN_SPACE) :
       → le biome est IMPOSÉ par la zone elle-même, pas par le Voronoï.
         Chaque grande zone a un biome calculé par hash(worldSeed, zoneAnchorX,
         zoneAnchorZ, layer) lors de son stamp à l'étape 3.
         Le Voronoï est ignoré pour ces cellules.
       → Résultat : une grande pièce a toujours un biome uniforme,
         les transitions ne peuvent se produire que dans les CORRIDOR.
     Si la cellule est CORRIDOR, WALL, JUNCTION, DEAD_END, ALCOVE :
       → le biome vient du Voronoï normalement.
   Pas de flood-fill nécessaire — 100 % chunk-indépendant.

3. Chaque cellule reçoit biome_index en métadonnée.
   Au placement final, biome_index détermine :
   - Bloc sol (moquette jaune, moquette tachée, béton, etc.)
   - Bloc murs (papier peint jaune, papier peint marron, béton brut, etc.)
   - Bloc plafond (dalles blanches, dalles jaunies, béton, etc.)
```

**Biomes Minecraft natifs :** mapper chaque `biome_index` sur un vrai biome Minecraft custom (via `BiomeSource` custom). Avantage : fog color, sons ambiants (`ambient_sound`), spawns de mobs sont configurés nativement par biome en JSON. Le BiomeSource lit la carte Voronoï.

**Connexions verticales :** quand un préfabriqué de transition verticale (escalier, trou) est placé à un ancrage, les deux extrémités peuvent tomber dans des biomes très différents. C'est voulu — ça signale visuellement le changement d'étage. Le préfabriqué lui-même est biome-neutre (béton, métal).

---

### Étape 5 — Lumières

**Input :** grille complète avec tags et biomes.
**Output :** positions et états des sources lumineuses.

**Principe :** les néons buzzants = ~50 % de l'identité visuelle du Level 0. Traiter comme une feature majeure.

```
1. Carte de pénombre (zones d'ombre) :
   noise_dark = SimplexNoise(cellX * scale_dark, cellZ * scale_dark, seed_dark)
   Si noise_dark > SEUIL_DARK → "poche sombre", aucun néon placé ici
   Fréquence cible : ~5 % de la surface, peu importe le biome

2. Pour chaque cellule NON dans une poche sombre :

   CORRIDOR :
     Néon au centre du plafond, 1 tous les 4-6 cellules le long du couloir
     Espacement N modulé par biome :
       pristine → N=4 (bien éclairé)
       damp/rust → N=6 (plus sombre)
     Chaque néon, par hash(seed, cellX, cellZ, L) :
       75 % → normal
       15 % → cassé (structure visible, émission lumineuse = 0)
       10 % → clignotant (texture animée, émission FIXE)

   ROOM_LARGE / OPEN_SPACE :
     Pattern en grille : 1 néon tous les M cellules en X et Z
     M = proportionnel à la taille de la pièce

   DEAD_END :
     70 % → aucun néon (poche sombre naturelle)
     30 % → 1 néon, souvent clignotant

   JUNCTION :
     1 néon systématique (repère visuel pour le joueur)

3. RÈGLES DURES :
   - Néons cassés : émission = 0 (pas level 1, ZÉRO)
   - Néons clignotants : texture alterne, émission FIXE — jamais de relight dynamique
   - Max ~1 source tous les 3-4 blocs linéaires
   - < 20 sources lumineuses custom par chunk en moyenne
```

---

### Étape 6 — Détails

**Input :** grille complète avec tags, biomes, lumières.
**Output :** flags de décoration par bloc.

**Principe :** micro-variations à l'échelle du bloc individuel. Toutes déterministes par `hash(seed, blockX, blockY, blockZ)`.

```
MURS :
  Plinthes (systématique) :
    1 bloc de haut en bas de chaque mur
    Variante par biome (bois clair en pristine, gris en concrete)

  Interrupteurs (rares) :
    ~1 % par segment de mur de 3 blocs, à ~1.2m du sol
    Purement décoratif

  Haut-parleurs (très rares) :
    ~0.5 % par segment de mur, sous le plafond
    Modèle custom petit, collé au mur

  Papier peint décollé / mur abîmé :
    Blockstate variant (même bloc, texture différente)
    Plus fréquent dans les biomes damp/rust
    ~2-5 % des murs selon le biome

SOL :
  Moquette bosselée : ~0.5 % → blockstate variant
  Taches de saleté : ~1 % → bloc variant ou overlay
  Flaques d'eau : ~0.2 %, probabilité ×5 sous un néon cassé
  Taches de sang : ~0.05 %, uniquement dans DEAD_END
    et dans un rayon de 10 blocs autour des structures gameplay

PLAFOND :
  Dalles manquantes : ~1 % → bloc "plafond cassé"
  Taches d'humidité : ~2 % dans biome damp, 0 % en pristine
```

---

### Étape 7 — Structures (préfabriqués)

**Input :** grille complète.
**Output :** structures `.nbt` placées.

TODO IMPLÉMENTATION PLUS TARD :
- cette étape reste aujourd'hui une cible, pas une réalité runtime complète ;
- le socle sémantique de structures existe déjà, mais le vrai placement de
  prefabs manuels reste à brancher ;
- le format documentaire `level0-manual-structures-template.json` devra rester
  aligné avec le futur format réellement chargé par le jeu.

**Principe :** trois catégories de structures.

```
1. STRUCTURES GAMEPLAY (petites, fréquentes) :
   Coffre avec loot table, bureau avec chaise, distributeur, etc.
   - Placement : Poisson disk sampling par hash
   - min_distance par catégorie (ex: 80 blocs entre deux coffres)
   - Contrainte de tag : coffre → DEAD_END ou ROOM_LARGE,
     distributeur → CORRIDOR, etc.

2. ZONES LIMINALES (grandes, rares) :
   Structures multi-cellules .nbt (la piscine, le théâtre, etc.)
   - min_distance = 200-500 blocs entre deux landmarks
   - Écrasent le labyrinthe sur leur emprise
   - Placement déterministe par hash global

3. CONNEXIONS VERTICALES entre layers :
   Escalier, trou dans le sol, trappe
   - Placées sur les points d'ancrage calculés à l'étape 1
     (grille virtuelle ~64×64 blocs, hash déterministe)
   - Le layer du dessus ET du dessous ont du CORRIDOR à cet endroit
     (forcé à l'étape 1)
   - Structure biome-neutre (béton, métal)
   - GÉOMÉTRIE VERTICALE : le préfab doit couvrir toute la hauteur entre
     les deux layers, c'est-à-dire :
       intérieur layer N  (4 blocs) +
       plafond layer N    (1 bloc)  +
       bedrock inter-layer (4 blocs) +
       sol layer N+1      (1 bloc)  +
       intérieur layer N+1 (4 blocs)
       = 14 blocs de haut total
     Le préfab perce le plafond, la bedrock, et le sol pour créer le passage.
     Les blocs de bedrock percés sont remplacés par les blocs du préfab
     (escalier, rampe, échelle, vide pour un trou, etc.)
```

**Métadonnées par structure (JSON, data-driven) :**
```json
{
  "id": "vending_machine_01",
  "category": "detail",
  "size": [1, 2, 1],
  "min_self_distance": 40,
  "min_distance_to": { "detail": 10, "landmark": 20 },
  "allowed_tags": ["CORRIDOR", "JUNCTION"],
  "allowed_biomes": ["*"],
  "layer_range": [0, 4]
}
```

**Placement via StructureTemplate vanilla :**
`template.place(world, pos, pivot, settings, random, Block.NOTIFY_LISTENERS)`

**Ordre de résolution des structures — STRICT et non négociable :**

Les structures sont placées dans cet ordre exact. Une catégorie ne peut JAMAIS écraser une structure d'une catégorie précédente.

```
1. Connexions verticales (priorité maximale — ancrages topologiques)
2. Zones liminales (grandes structures rares)
3. Structures gameplay (coffres, distributeurs, etc.)
4. Détails structurels (petits objets décoratifs s'ils sont des .nbt)
```

TODO IMPLÉMENTATION PLUS TARD :
- cet ordre est utile comme règle cible, mais il ne faut l'activer qu'une fois
  le vrai système de prefabs branché ;
- ajouter alors les règles anti-collage et de distance minimale dès le premier
  branchement runtime.

Si une structure candidate entre en conflit spatial avec une structure déjà placée d'une catégorie supérieure ou égale, elle est **rejetée** (pas déplacée, pas adaptée — rejetée). Le Poisson spacing et les `min_distance_to` réduisent la probabilité de conflit, mais l'ordre de résolution est le filet de sécurité final.

---

## Résumé du flux dans le ChunkGenerator

```java
// Hauteur par layer : 6 blocs (1 sol + 4 intérieur + 1 plafond)
// Séparation entre layers : 4 blocs de bedrock
// Total par layer + séparation = 10 blocs de Y
// Région = 8×8 chunks (128×128 blocs)

private static final int REGION_SIZE = 8; // chunks par côté

private int getLayerBaseY(int layer) {
    return layer * 10; // Layer 0 → Y=0, Layer 1 → Y=10, Layer 2 → Y=20...
}

// Cache LRU : clé = (regionX, regionZ), valeur = RegionGrid (grilles taggées de tous les layers)
private final LRUCache<RegionKey, RegionGrid> regionCache = new LRUCache<>(16);

// Appelé en interne : génère ou récupère la grille régionale (étapes 1-4)
private RegionGrid getOrGenerateRegion(int regionX, int regionZ) {
    RegionKey key = new RegionKey(regionX, regionZ);
    RegionGrid cached = regionCache.get(key);
    if (cached != null) return cached;

    // Générer la grille de toute la région (128×128 blocs) pour chaque layer
    RegionGrid region = new RegionGrid(regionX, regionZ, NUM_LAYERS);
    for (int layer = 0; layer < NUM_LAYERS; layer++) {
        CellGrid grid = region.getLayer(layer);
        MazeGenerator.generate(grid, worldSeed, layer, regionX, regionZ);  // Étape 1
        NoiseGeometry.apply(grid, worldSeed, layer);                       // Étape 2
        LargeRooms.stamp(grid, worldSeed, layer, regionX, regionZ);        // Étape 3
        BiomeMapper.assign(grid, worldSeed, layer, regionX, regionZ);      // Étape 4
    }

    regionCache.put(key, region);
    return region;
}

public void generateNoise(StructWorldAccess world, Chunk chunk) {
    int cX = chunk.getPos().x;
    int cZ = chunk.getPos().z;
    int regionX = cX >> 3;  // cX / 8
    int regionZ = cZ >> 3;

    // Récupérer la grille régionale (étapes 1-4 déjà calculées ou en cache)
    RegionGrid region = getOrGenerateRegion(regionX, regionZ);

    // Remplir la bedrock sous le layer 0 (Y = -64 à -1)
    for (int y = -64; y < 0; y++) {
        fillSlice(chunk, y, Blocks.BEDROCK);
    }

    // Remplir la séparation bedrock entre layers
    for (int layer = 0; layer < NUM_LAYERS - 1; layer++) {
        int bedrockStart = getLayerBaseY(layer) + 6;
        for (int y = bedrockStart; y < bedrockStart + 4; y++) {
            fillSlice(chunk, y, Blocks.BEDROCK);
        }
    }

    // Remplir la bedrock au-dessus du dernier layer (1 bloc)
    int lastLayerCeiling = getLayerBaseY(NUM_LAYERS - 1) + 5;
    fillSlice(chunk, lastLayerCeiling + 1, Blocks.BEDROCK);

    for (int layer = 0; layer < NUM_LAYERS; layer++) {
        int baseY = getLayerBaseY(layer);

        // Extraire la portion chunk depuis la grille régionale (copie mutable)
        ChunkGrid chunkGrid = region.extractChunk(layer, cX, cZ);

        // Étapes 5-7 : locales au chunk
        LightPlacer.compute(chunkGrid, worldSeed, layer);         // Étape 5
        DetailPass.apply(chunkGrid, worldSeed, layer);             // Étape 6
        StructurePlacer.place(chunkGrid, worldSeed, layer, cX, cZ); // Étape 7

        // Placement des blocs dans le chunk :
        // 1) Sol à baseY (bloc sol du biome)
        // 2) Murs à baseY+1 → baseY+4 (wallpaper du biome)
        // 3) Plafond à baseY+5 (bloc plafond du biome)
        // 4) Air dans les espaces intérieurs (CORRIDOR, ROOM_*, etc.)
        BlockWriter.write(chunk, chunkGrid, baseY, layer);

        // Passe de nettoyage : remplacer les wallpaper non exposés à l'air
        // par de la bedrock (optimisation rendu + cohérence visuelle)
        BlockWriter.replaceHiddenWalls(chunk, baseY, layer);
    }
}
```

---

## Performance et optimisation

### Budgets cibles

| Métrique | Cible | Seuil critique |
|---|---|---|
| Temps total de génération par chunk (tous layers) | < 50 ms | > 150 ms |
| Allocations éphémères par chunk | < 5 MB | > 20 MB |
| Empreinte mémoire persistante par chunk chargé | < 2 KB | > 50 KB |
| Sources lumineuses custom par chunk | < 20 | > 50 |
| Block entities par chunk | < 10 | > 30 |
| TPS serveur pendant exploration active | ≥ 19.5 | < 15 |

Seules quelques couches Y sont occupées par les layers, donc l'essentiel du chunk est vide — les budgets reflètent cet usage.

### Pièges spécifiques à ce projet

**1. Densité de sources lumineuses — piège n°1.**
Chaque bloc émetteur déclenche un calcul de propagation qui touche jusqu'à 15 blocs dans chaque direction. 100 néons dans un chunk peut coûter plus cher que tout le reste de la génération.

Règles dures :
- Jamais plus d'une source tous les 3-4 blocs.
- Néons cassés : émission = 0, pas level 1.
- Néons clignotants : changent UNIQUEMENT leur texture, JAMAIS leur émission. Un changement de light level à runtime force un relight de toute la zone à chaque tick.
- S'appuyer sur la propagation vanilla : une source tous les 6 blocs suffit à éclairer un couloir (la lumière se propage de 15 blocs).

**2. Block entities.**
Chaque block entity coûte du CPU à chaque tick, même inactive. Un chunk avec 100 block entities lag.

- PAS de block entity pour le clignotement des néons. Utiliser une blockstate animée (modèle client-side) ou un shader.
- Si une block entity est nécessaire pour un effet ponctuel, la rendre `tickable = false` par défaut et n'activer le tick qu'avec un joueur à proximité.

**3. Voronoï et bruits recalculés.**
Calculer le Voronoï des biomes à chaque chunk = recalcul massif répété.

- Voronoï déterministe par hash : pour chaque point, échantillonner les 9 cellules de grille virtuelle voisines → O(9). Exactement la technique du Worley noise.
- Cache LRU par région : stocker le résultat des macro-cellules Voronoï. Une région couvre plusieurs chunks, l'amortissement est naturel.

**4. Multi-layer = multiplication des coûts.**
3 layers × 20 ms = 60 ms par chunk. Le multi-layer n'est pas gratuit.

- Les données calculées globalement (ancrages verticaux, seeds Voronoï) doivent être calculées **une fois par région**, pas une fois par layer par chunk. Cache obligatoire.
- Les layers vides (sol entre deux niveaux) doivent être **skipés entièrement**, pas générés puis remplis de vide.

**5. Placement de blocs en bulk.**
`world.setBlockState(pos, state, flags)` en boucle sur 10000 blocs est catastrophique (neighbor updates + light updates à chaque appel).

- Pendant la génération, utiliser les APIs de niveau `ChunkGenerator` qui écrivent directement dans la structure du chunk **sans** déclencher d'updates.
- Utiliser `Block.NOTIFY_LISTENERS` (valeur 1) au lieu de `Block.NOTIFY_ALL` (valeur 3).
- Tout le placement dans une seule passe, pas étape par étape.

**6. Préfabriqués `.nbt` volumineux.**
Chaque `StructureTemplate` chargé reste en mémoire.

- Garder les préfas < 50 KB. Si une structure est grosse, la découper.
- **Lazy loading** : ne charger un préfa qu'au premier usage, pas au démarrage.
- Décharger les préfas non utilisés (LRU cache).

**7. Allocations dans les boucles chaudes.**
`new BlockPos(x,y,z)` × 10000 = 10000 allocations = GC qui lag.

- Utiliser `BlockPos.Mutable` pour les boucles d'itération.
- Pas de `new int[]`, `new ArrayList` dans les hot paths.
- Réutiliser les buffers entre appels.

**8. Coût de génération d'une région entière (spécifique à l'approche hybride).**
La première fois qu'un chunk d'une région est demandé, toute la région (128×128 blocs × N layers) est générée d'un coup. Ça peut causer un lag spike ponctuel.

- Profiler le temps de génération d'une région complète. Cible : < 200 ms pour 3 layers.
- Minecraft demande les chunks en spirale → le premier chunk d'une région est souvent un chunk de bord, pas le centre. Le lag spike est réparti sur les régions traversées, pas concentré.
- Si trop lent : réduire la taille de la région (4×4 au lieu de 8×8), au prix d'un cache moins efficace.
- Le cache LRU doit être assez grand pour couvrir la render distance. Avec render distance 12 chunks → ~3-4 régions de 8×8 visibles → cache de 8-16 régions est confortable.

### Système de mesure

**Instrumentation par étape, intégrée au code dès le début.**

```java
abstract class PipelineStage {
    private static final boolean PROFILING_ENABLED = Config.isProfilingEnabled();

    public final void applyProfiled(CellGrid grid, long seed, int layer) {
        if (!PROFILING_ENABLED) {
            apply(grid, seed, layer);
            return;
        }
        long startTime = System.nanoTime();
        long startMem = getAllocatedBytes();
        apply(grid, seed, layer);
        long elapsed = System.nanoTime() - startTime;
        long allocated = getAllocatedBytes() - startMem;
        ProfilingRegistry.record(getId(), elapsed, allocated);
    }

    protected abstract void apply(CellGrid grid, long seed, int layer);
}
```

`ProfilingRegistry` maintient des histogrammes par étape (min / p50 / p95 / p99 / max), dumpable via `/backrooms profile dump`.

**Pourquoi p95 et p99** : la moyenne masque les pires cas. Un chunk à 500 ms une fois sur 100 cause un stutter visible même si la moyenne est à 20 ms. Surveiller les queues de distribution.

**Métriques à logger par étape :**
- Temps CPU (ns)
- Allocations (bytes)
- Nombre de cellules touchées
- Nombre de blocs écrits (pour l'étape de placement)

### Outils de profiling externes

**Spark (lucko/spark)** — profiler Minecraft de facto, version Fabric. **Installer dès le début.**
- `/spark profiler start` → `/spark profiler stop` → flame graph
- `/spark health` → TPS, CPU, allocations
- `/spark heapdump` → analyse mémoire

**async-profiler** — gratuit, vue plus fine que Spark pour les investigations ciblées.

**VisualVM** — inclus avec le JDK, utile pour monitorer les GC en live.

**`/debug start` → `/debug stop`** — timer de génération de chunks vanilla. Moins détaillé mais zero-install.

### Benchmark reproductible

Un **benchmark automatisé** dès la première étape :

1. Test JUnit qui génère une région 4×4 chunks avec seed fixe.
2. Mesure temps total, temps par étape, allocations.
3. Compare aux baselines stockées dans le repo (`benchmark-baseline.json`).
4. **Fail si une métrique dépasse +20 %** — détecte les régressions dès le commit.

```json
{
  "seed": 42,
  "region": [0, 0, 4, 4],
  "metrics": {
    "total_time_ms": { "p50": 180, "p95": 240 },
    "stages": {
      "maze": { "p50": 60, "p95": 90 },
      "noise_geometry": { "p50": 8, "p95": 12 },
      "large_rooms": { "p50": 10, "p95": 15 },
      "biomes": { "p50": 5, "p95": 8 },
      "lights": { "p50": 20, "p95": 35 },
      "details": { "p50": 15, "p95": 25 },
      "structures": { "p50": 12, "p95": 20 }
    },
    "allocations_mb": 42
  }
}
```

### Optimisations à NE PAS faire tout de suite

- **N'optimise pas une étape qui n'est pas dans le top 3 des plus lentes.** Réduire 2 ms à 1 ms n'améliore rien.
- **N'ajoute pas de cache avant d'avoir prouvé que le recalcul est coûteux.** Un cache mal conçu est pire qu'un recalcul propre.
- **Ne parallélise rien** tant que le monothread n'est pas saturé. La parallélisation casse le déterminisme si elle est mal faite.
- **Pas de code natif (JNI)** sauf si Spark montre un hotspot précis qui le justifie.

### Red flags à surveiller

- **TPS < 19** pendant exploration de zone nouvellement générée → étape de placement trop lourde.
- **Spikes > 200 ms** dans Spark → hot path dans une boucle, ou cache qui se recalcule.
- **Courbe mémoire en dents de scie + full GC fréquents** → allocations dans un hot path.
- **Ralentissement progressif au fil de l'exploration** → fuite mémoire, probablement un cache sans politique d'éviction.
- **FPS client qui chute dans les grandes pièces** → trop de sources lumineuses ou trop de block entities visibles. C'est un problème client, pas serveur.
- **Lag spikes aux frontières de biome ou aux ancrages verticaux** → calcul trop coûteux dans ces cas limites.

---

## Librairies et outils

### Dépendances obligatoires
- **Fabric API** — `BiomeModifications`, `ServerChunkEvents`, `ServerWorldEvents`, registres worldgen.

### Outils vanilla Minecraft à utiliser
- `net.minecraft.util.math.noise.SimplexNoiseSampler` / `PerlinNoiseSampler`
- `StructureTemplate` / `.nbt` files pour les préfabriqués
- `ChunkGenerator` custom
- `BiomeSource` custom

### Outils de dev (pas en runtime)
- **worldgen-devtools (jacobsjo)** — reload datapacks in-game, reset de chunks sans relancer le monde.
- **Spark** — profiling.

### Librairies à ÉVITER
- **YUNG's API, TerraBlender, Cristel Lib, Berezka Library** : servent à modifier la worldgen overworld vanilla, pas à écrire un ChunkGenerator full custom.
- Règle : n'ajouter aucune dépendance worldgen supplémentaire sans besoin concret.

---

## Spawns et entités

**Les biomes custom de la dimension Backrooms doivent désactiver tous les spawns vanilla.** Sans configuration explicite, Minecraft va spawner des creepers, zombies, etc. dans les couloirs — ce n'est pas le comportement voulu.

**Configuration par biome (JSON datapack) :**
```json
{
  "spawn_settings": {
    "creature_spawn_probability": 0.0,
    "spawners": {}
  }
}
```

Chaque biome custom (pristine, stained, damp, concrete, rust, dark...) doit avoir des `spawners` vides. Aucun mob vanilla ne spawn dans la dimension.

**Entités custom :** les mobs custom des Backrooms (entités hostiles, PNJ) ont leur propre logique de spawn, indépendante du système vanilla :
- Spawn contrôlé par le mod : un système qui vérifie périodiquement la distance au joueur et le tag de la cellule. Les entités hostiles spawn dans les DEAD_END, les poches sombres (zones sans néon), et près des structures gameplay.
- Pas de spawn dans les JUNCTION ou les grandes pièces bien éclairées — le joueur doit se sentir en relative sécurité dans les espaces ouverts et en danger dans les culs-de-sac et les zones sombres.
- Densité faible : le Level 0 canonique est presque vide. La peur vient de l'isolement, pas du combat. 1-2 entités max par région chargée.

---

## Limitations connues

**Grille 3×3 rigide :** la grille de cellules 3×3 blocs impose que tous les couloirs font 3 blocs de large (1 cellule). Pas de couloir étroit (1 bloc) ni de couloir large (5 blocs) sans consommer 2 cellules. Le joueur finira par sentir la régularité sous-jacente après une exploration prolongée. L'étape 2 (noise geometry : demi-murs, alcôves, piliers) compense partiellement en cassant la symétrie, mais le rythme fondamental reste "multiple de 3". C'est acceptable pour une v1 — une v2 pourrait introduire une grille variable ou un post-processing qui déplace des blocs individuels hors grille.

---

## Checklist de validation

Avant de considérer la pipeline terminée :
- [ ] Chaque étape reçoit un RNG seedé indépendamment via hash.
- [ ] Régénérer la même seed deux fois produit un résultat identique (hash check).
- [ ] Aucun `HashMap`/`HashSet` itéré sans tri préalable.
- [ ] Aucun `Math.random()` ou `new Random()` sans seed.
- [ ] Les connexions verticales tombent sur du CORRIDOR des deux côtés à 100 %.
- [ ] Aucune transition de biome visuel ne coupe une grande pièce.
- [ ] Chaque layer utilise sa propre palette de biomes.
- [ ] Les préfabriqués respectent leurs distances minimales.
- [ ] Performance : génération < 50 ms/chunk en moyenne, p95 < 150 ms.
- [ ] Spark installé et profil baseline enregistré.
- [ ] Benchmark automatisé qui fail à +20 % de régression.
- [ ] TPS ≥ 19.5 pendant 10 minutes d'exploration.
- [ ] Aucun full GC pendant exploration active.
- [ ] < 20 sources lumineuses custom par chunk en moyenne.
- [ ] Dump PNG de la grille taggée fonctionne pour debug visuel.
- [ ] Aucun mob vanilla ne spawn dans la dimension (biomes configurés avec spawners vides).
- [ ] Les entités custom ne spawnent que dans les zones appropriées (DEAD_END, poches sombres).
