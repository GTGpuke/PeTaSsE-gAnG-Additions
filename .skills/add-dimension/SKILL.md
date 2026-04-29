---
name: add-dimension
description: "Ajouter une dimension au mod PeTaSsE_gAnG_Additions (Fabric 1.21.1). Déclenche pour 'dimension', 'monde', 'portail', 'téléportation', 'monde custom', 'realm', 'dimension custom'."
---

# Skill — Ajouter une Dimension (Fabric 1.21.1)

## Quand utiliser ce skill

- "Ajoute une dimension [nom]"
- "Crée un monde custom [description]"
- "Nouveau portail vers [destination]"

---

## Étapes

### 1. Définir les paramètres

| Paramètre | Valeur |
|-----------|--------|
| `DIMENSION_ID` | `gang_realm`, `void_world` |
| Biome | custom ou vanilla (`minecraft:plains`, etc.) |
| Type de dimension | normal, nether-like, end-like |
| Portail | bloc de portail custom ou téléportation par commande |
| Génération | flat, void, custom chunk generator |

---

### 2. Fichiers JSON (data pack)

Les dimensions dans Fabric sont entièrement définies par des fichiers JSON dans `data/`.

**Structure :**
```
data/petasse_gang_additions/
├── dimension/
│   └── gang_realm.json           ← définit la dimension
└── dimension_type/
    └── gang_realm_type.json      ← type (lumière, brouillard, hauteur)
```

---

### 3. dimension_type JSON

**Fichier :** `data/petasse_gang_additions/dimension_type/gang_realm_type.json`

```json
{
  "ultrawarm": false,
  "natural": true,
  "coordinate_scale": 1.0,
  "has_skylight": true,
  "has_ceiling": false,
  "ambient_light": 0.0,
  "monster_spawn_light_level": {
    "type": "minecraft:uniform",
    "value": { "min_inclusive": 0, "max_inclusive": 7 }
  },
  "monster_spawn_block_light_limit": 0,
  "piglin_safe": false,
  "bed_works": true,
  "respawn_anchor_works": false,
  "has_raids": false,
  "min_y": -64,
  "height": 384,
  "logical_height": 384,
  "infiniburn": "#minecraft:infiniburn_overworld",
  "effects": "minecraft:overworld"
}
```

---

### 4. dimension JSON

**Fichier :** `data/petasse_gang_additions/dimension/gang_realm.json`

```json
{
  "type": "petasse_gang_additions:gang_realm_type",
  "generator": {
    "type": "minecraft:noise",
    "biome_source": {
      "type": "minecraft:fixed",
      "biome": "minecraft:plains"
    },
    "settings": "minecraft:overworld"
  }
}
```

---

### 5. Portail de téléportation (optionnel)

Crée un item ou bloc de portail qui téléporte vers la dimension :

```java
// Dans un Item.use() ou Block.onUse() :
if (world instanceof ServerWorld serverWorld) {
    RegistryKey<World> dimensionKey = RegistryKey.of(
            RegistryKeys.WORLD,
            Identifier.of("petasse_gang_additions", "gang_realm")
    );
    ServerWorld targetWorld = serverWorld.getServer().getWorld(dimensionKey);
    if (targetWorld != null && player instanceof ServerPlayerEntity serverPlayer) {
        serverPlayer.teleport(targetWorld,
                player.getX(), player.getY(), player.getZ(),
                player.getYaw(), player.getPitch());
    }
}
```

---

### 6. Enregistrement automatique

Les dimensions JSON dans `data/` sont automatiquement reconnues par Minecraft/Fabric.
Aucune registration Java supplémentaire n'est nécessaire pour une dimension basique.

Pour un chunk generator custom (comme `LevelZeroChunkGenerator`), il faut enregistrer le CODEC :
```java
// Dans ModChunkGenerators.java :
public static final MapCodec<LevelZeroChunkGenerator> LEVEL_ZERO =
        Registry.register(Registries.CHUNK_GENERATOR,
                Identifier.of(ModConstants.MOD_ID, "level_zero"),
                LevelZeroChunkGenerator.CODEC);
```

Dans l'arborescence v4, un generateur de niveau Backrooms doit rester dans
`backrooms/level/<level_id>/generation/`, avec ses helpers proches de sa feature.

---

## Checklist finale

- [ ] `data/petasse_gang_additions/dimension_type/my_dim_type.json`
- [ ] `data/petasse_gang_additions/dimension/my_dim.json`
- [ ] Biome custom si nécessaire
- [ ] Portail/téléportation si souhaité
- [ ] Chunk generator custom enregistré dans `ModChunkGenerators.java` si nécessaire
- [ ] Lang key pour le nom de la dimension
- [ ] `docs/DIMENSIONS.md` + `CHANGELOG.md` mis à jour
- [ ] Testé en jeu : `/execute in petasse_gang_additions:my_dim run teleport @s 0 64 0`
