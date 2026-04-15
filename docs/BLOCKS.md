# Blocks — PeTaSsE_gAnG_Additions

Catalogue de tous les blocs du mod.

---

## Backrooms — Level 0

Ensemble principal de blocs servant de palette visuelle au premier niveau des Backrooms.

Convention visuelle actuelle : les textures de blocs du Level 0 sont en `32×32`.
Cette exception ne change pas automatiquement la résolution des autres blocs du mod.

### Level 0 Wallpaper (`level_zero_wallpaper`)

| Propriété | Valeur |
|-----------|--------|
| ID complet | `petasse_gang_additions:level_zero_wallpaper` |
| Classe | `net.minecraft.block.Block` |
| Hardness | 3.0 |
| Blast resistance | 9.0 |
| Luminosité | 0 |
| Loot table | `data/petasse_gang_additions/loot_table/blocks/level_zero_wallpaper.json` |

Mur principal du Level 0.
Version simple jaune du mur — utilisé directement quand toute la colonne appartient à la variante de base, sans `BlockEntity`.

### Level 0 White Wallpaper (`level_zero_wallpaper_aged`)

| Propriété | Valeur |
|-----------|--------|
| ID complet | `petasse_gang_additions:level_zero_wallpaper_aged` |
| Classe | `net.minecraft.block.Block` |
| Hardness | 3.0 |
| Blast resistance | 9.0 |
| Luminosité | 0 |
| Loot table | `data/petasse_gang_additions/loot_table/blocks/level_zero_wallpaper_aged.json` |

Bloc simple blanc réservé au second biome de surface. Sert aussi de source de modèle pour les transitions mixtes.

### Level 0 Adaptive Wallpaper (`level_zero_wallpaper_adaptive`)

| Propriété | Valeur |
|-----------|--------|
| ID complet | `petasse_gang_additions:level_zero_wallpaper_adaptive` |
| Classe | `com.petassegang.addons.block.LevelZeroWallpaperBlock` |
| Hardness | 3.0 |
| Blast resistance | 9.0 |
| Luminosité | 0 |

Bloc technique utilisé pour les transitions mixtes.
Posé uniquement sur les colonnes de mur vraiment mixtes, quand deux faces visibles doivent afficher des variantes différentes.
Le `faceMask` est calculé à la génération, stocké dans une `BlockEntity` synchronisée, puis relu par le modèle client.
Si la `ModelData` n'est pas encore prête côté client, le modèle adaptatif relit d'abord les blocs de sol déjà posés.
Le cœur des murs non exposés est en `minecraft:bedrock`.

### Level 0 Damp Carpet (`level_zero_damp_carpet`)

| Propriété | Valeur |
|-----------|--------|
| ID complet | `petasse_gang_additions:level_zero_damp_carpet` |
| Classe | `net.minecraft.block.Block` |
| Hardness | 0.8 |
| Blast resistance | 0.8 |
| Luminosité | 0 |
| Loot table | `data/petasse_gang_additions/loot_table/blocks/level_zero_damp_carpet.json` |

Sol humide du Level 0.

### Level 0 Red Carpet (`level_zero_damp_carpet_aged`)

| Propriété | Valeur |
|-----------|--------|
| ID complet | `petasse_gang_additions:level_zero_damp_carpet_aged` |
| Classe | `net.minecraft.block.Block` |
| Hardness | 0.8 |
| Blast resistance | 0.8 |
| Luminosité | 0 |
| Loot table | `data/petasse_gang_additions/loot_table/blocks/level_zero_damp_carpet_aged.json` |

Moquette rouge réservée aux grandes zones cosmétiques du second biome du Level 0.

### Level 0 Ceiling Tile (`level_zero_ceiling_tile`)

| Propriété | Valeur |
|-----------|--------|
| ID complet | `petasse_gang_additions:level_zero_ceiling_tile` |
| Classe | `net.minecraft.block.Block` |
| Hardness | 0.75 |
| Blast resistance | 0.75 |
| Luminosité | 0 |
| Loot table | `data/petasse_gang_additions/loot_table/blocks/level_zero_ceiling_tile.json` |

Plafond du Level 0.

### Level 0 Fluorescent Light (`level_zero_fluorescent_light`)

| Propriété | Valeur |
|-----------|--------|
| ID complet | `petasse_gang_additions:level_zero_fluorescent_light` |
| Classe | `net.minecraft.block.Block` |
| Hardness | 0.3 |
| Blast resistance | 0.3 |
| Luminosité | 15 |
| Loot table | `data/petasse_gang_additions/loot_table/blocks/level_zero_fluorescent_light.json` |

Bloc lumineux placé au plafond dans les zones ouvertes.

---

## Arbre Maudit

Voir [docs/ITEMS.md](ITEMS.md) pour le catalogue des blocs de l'Arbre Maudit (`cursed_log`, `cursed_leaves`, `cursed_sapling`, `cursed_planks`).

---

## Template de documentation de bloc

### NomDuBloc (`nom_du_bloc`)

| Propriété | Valeur |
|-----------|--------|
| ID complet | `petasse_gang_additions:nom_du_bloc` |
| Classe | `com.petassegang.addons.block.NomDuBlocBlock` |
| Hardness | X.X |
| Blast resistance | X.X |
| Luminosité | 0–15 |
| Loot table | `data/petasse_gang_additions/loot_table/blocks/nom_du_bloc.json` |

### Description
...

### Obtenir en jeu

```mcfunction
/give @p petasse_gang_additions:nom_du_bloc
```
