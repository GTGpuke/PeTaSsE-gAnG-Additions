---
name: add-dimension
description: "Ajouter une dimension au mod PeTaSsE_gAnG_Additions. Déclenche pour 'dimension', 'monde', 'portail', 'téléportation', 'monde custom', 'realm', 'dimension custom'."
---

# Skill — Ajouter une Dimension

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
| Génération | flat, void, custom noise |

---

### 2. Fichiers de data generation (JSON)

Les dimensions dans Forge modernes sont entièrement définies par des fichiers JSON dans le dossier `data/`.

**Structure :**
```
data/petasse_gang_additions/
├── dimension/
│   └── gang_realm.json           ← définit la dimension
├── dimension_type/
│   └── gang_realm_type.json      ← type (lumière, brouillard, hauteur)
└── worldgen/
    ├── biome/
    │   └── gang_biome.json       ← biome custom (si nécessaire)
    ├── noise_settings/
    │   └── gang_realm.json       ← paramètres de génération de terrain
    └── world_preset/
        └── gang_realm.json       ← preset optionnel
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
  "fixed_time": false,
  "monster_spawn_light_level": { "type": "minecraft:uniform", "value": { "min_inclusive": 0, "max_inclusive": 7 } },
  "monster_spawn_block_light_limit": 0,
  "piglin_safe": false,
  "bed_works": true,
  "respawn_anchor_works": false,
  "has_raids": false,
  "min_y": -64,
  "height": 384,
  "logical_height": 384,
  "infiniburn": "#minecraft:infiniburn_overworld",
  "effects": "minecraft:overworld",
  "created_automatically": false
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
// Dans un Item.use() ou Block.use() :
if (!level.isClientSide()) {
    ServerPlayer serverPlayer = (ServerPlayer) player;
    ResourceKey<Level> dimensionKey = ResourceKey.create(
        Registries.DIMENSION,
        new ResourceLocation("petasse_gang_additions", "gang_realm")
    );
    ServerLevel targetLevel = serverPlayer.server.getLevel(dimensionKey);
    if (targetLevel != null) {
        serverPlayer.teleportTo(targetLevel,
            serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
            serverPlayer.getYRot(), serverPlayer.getXRot());
    }
}
```

---

### 6. Enregistrer la dimension

Les dimensions JSON dans `data/` sont automatiquement reconnues par Forge/MC.
Aucune registration Java supplémentaire nécessaire pour une dimension basique.

---

## Checklist finale

- [ ] `data/petasse_gang_additions/dimension_type/my_dim.json`
- [ ] `data/petasse_gang_additions/dimension/my_dim.json`
- [ ] Biome custom si nécessaire
- [ ] Portail/téléportation si souhaité
- [ ] Lang key pour le nom de la dimension
- [ ] `docs/DIMENSIONS.md` + `CHANGELOG.md` mis à jour
- [ ] Testé en jeu : `/execute in petasse_gang_additions:my_dim run teleport @s 0 64 0`
