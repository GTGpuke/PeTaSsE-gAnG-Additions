# Blocks - PeTaSsE_gAnG_Additions

Catalogue de tous les blocs du mod.

---

## Backrooms - Level 0

Ensemble principal de blocs servant de palette visuelle au premier niveau des Backrooms.

Convention visuelle actuelle : les textures de blocs du Level 0 sont en `32x32`.
Cette exception ne change pas automatiquement la resolution des autres blocs du mod.

### Level 0 Wallpaper (`level_zero_wallpaper`)

| Propriete | Valeur |
|-----------|--------|
| ID complet | `petasse_gang_additions:level_zero_wallpaper` |
| Classe | `net.minecraft.world.level.block.Block` |
| Hardness | 3.0 |
| Blast resistance | 9.0 |
| Luminosite | 0 |
| Loot table | `data/petasse_gang_additions/loot_table/blocks/level_zero_wallpaper.json` |

Mur principal du Level 0.
Ce bloc est maintenant la version simple jaune du mur.
Le generateur l'utilise directement quand toute la colonne appartient a la variante de base, sans `BlockEntity`.

### Level 0 White Wallpaper (`level_zero_wallpaper_aged`)

| Propriete | Valeur |
|-----------|--------|
| ID complet | `petasse_gang_additions:level_zero_wallpaper_aged` |
| Classe | `net.minecraft.world.level.block.Block` |
| Hardness | 3.0 |
| Blast resistance | 9.0 |
| Luminosite | 0 |
| Loot table | `data/petasse_gang_additions/loot_table/blocks/level_zero_wallpaper_aged.json` |

Bloc simple blanc reserve au second biome de surface.
Il sert aussi de source de modele pour les transitions mixtes.
Il reste accessible en creatif pour les tests visuels.

### Level 0 Adaptive Wallpaper (`level_zero_wallpaper_adaptive`)

| Propriete | Valeur |
|-----------|--------|
| ID complet | `petasse_gang_additions:level_zero_wallpaper_adaptive` |
| Classe | `com.petassegang.addons.block.LevelZeroWallpaperBlock` |
| Hardness | 3.0 |
| Blast resistance | 9.0 |
| Luminosite | 0 |

Bloc technique utilise pour les transitions mixtes.
Il n'est pose que sur les colonnes de mur vraiment mixtes, quand deux faces visibles doivent afficher des variantes differentes.
Le `faceMask` y est calcule a la generation, stocke dans une `BlockEntity` synchronisee, puis relu par le modele client.
Si la `ModelData` n'est pas encore prete cote client, le modele adaptatif relit d'abord les blocs de sol deja poses dans le chunk pour retrouver la bonne palette visuelle avant de retomber sur le sampler deterministe.
Cette separation permet d'eviter de payer le cout du rendu adaptatif sur tous les murs simples.
Le coeur des murs non exposes est maintenant rempli en `minecraft:bedrock`, sans bloc dedie supplementaire.
Le bloc reste expose en creatif pour les tests techniques.

### Level 0 Damp Carpet (`level_zero_damp_carpet`)

| Propriete | Valeur |
|-----------|--------|
| ID complet | `petasse_gang_additions:level_zero_damp_carpet` |
| Classe | `net.minecraft.world.level.block.Block` |
| Hardness | 0.8 |
| Blast resistance | 0.8 |
| Luminosite | 0 |
| Loot table | `data/petasse_gang_additions/loot_table/blocks/level_zero_damp_carpet.json` |

Sol humide du Level 0.

### Level 0 Red Carpet (`level_zero_damp_carpet_aged`)

| Propriete | Valeur |
|-----------|--------|
| ID complet | `petasse_gang_additions:level_zero_damp_carpet_aged` |
| Classe | `net.minecraft.world.level.block.Block` |
| Hardness | 0.8 |
| Blast resistance | 0.8 |
| Luminosite | 0 |
| Loot table | `data/petasse_gang_additions/loot_table/blocks/level_zero_damp_carpet_aged.json` |

Moquette rouge reservee aux grandes zones cosmetiques du second biome du Level 0, sans changer le layout.

### Level 0 Ceiling Tile (`level_zero_ceiling_tile`)

| Propriete | Valeur |
|-----------|--------|
| ID complet | `petasse_gang_additions:level_zero_ceiling_tile` |
| Classe | `net.minecraft.world.level.block.Block` |
| Hardness | 0.75 |
| Blast resistance | 0.75 |
| Luminosite | 0 |
| Loot table | `data/petasse_gang_additions/loot_table/blocks/level_zero_ceiling_tile.json` |

Plafond du Level 0.

### Level 0 Fluorescent Light (`level_zero_fluorescent_light`)

| Propriete | Valeur |
|-----------|--------|
| ID complet | `petasse_gang_additions:level_zero_fluorescent_light` |
| Classe | `net.minecraft.world.level.block.Block` |
| Hardness | 0.3 |
| Blast resistance | 0.3 |
| Luminosite | 15 |
| Loot table | `data/petasse_gang_additions/loot_table/blocks/level_zero_fluorescent_light.json` |

Bloc lumineux place au plafond dans les zones ouvertes.

---

## Template de documentation de bloc

### NomDuBloc (`nom_du_bloc`)

| Propriete | Valeur |
|-----------|--------|
| ID complet | `petasse_gang_additions:nom_du_bloc` |
| Classe | `com.petassegang.addons.block.NomDuBlocBlock` |
| Hardness | X.X |
| Blast resistance | X.X |
| Harvest tool | pickaxe / axe / shovel / none |
| Harvest level | 0 / 1 / 2 / 3 |
| Luminosite | 0-15 |
| Loot table | `data/petasse_gang_additions/loot_table/blocks/nom_du_bloc.json` |

### Description
...

### Obtenir en jeu

```mcfunction
/give @p petasse_gang_additions:nom_du_bloc
```
