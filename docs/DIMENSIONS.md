# Dimensions - PeTaSsE_gAnG_Additions

Catalogue de toutes les dimensions custom du mod.

---

## Backrooms - Level 0 (`backrooms_level_0`)

| Propriete | Valeur |
|-----------|--------|
| ID complet | `petasse_gang_additions:backrooms_level_0` |
| Portail | Aucun pour l'instant |
| Biome(s) | `minecraft:the_void` |
| Generation | `LevelZeroChunkGenerator` monocouche |
| Gravite | normale |
| Ciel | noir, sans skybox |

### Description

Premiere passe du Level 0 basee sur le script Python de reference.
Le niveau genere un labyrinthe monocouche de couloirs et de salles via :
- superposition de maze,
- salles rectangulaires,
- salles a piliers,
- salles polygonales.

### Fichiers principaux

- `src/main/java/com/petassegang/addons/world/backrooms/BackroomsConstants.java`
- `src/main/java/com/petassegang/addons/world/backrooms/level0/LevelZeroChunkGenerator.java`
- `src/main/java/com/petassegang/addons/world/backrooms/level0/LevelZeroLayout.java`
- `src/main/resources/data/petasse_gang_additions/dimension/backrooms_level_0.json`
- `src/main/resources/data/petasse_gang_additions/dimension_type/backrooms_level_0_type.json`

### Acces

```mcfunction
/execute in petasse_gang_additions:backrooms_level_0 run teleport @s 0 63 0
```

---

## Template de documentation de dimension

### NomDeLaDimension (`nom_de_la_dimension`)

| Propriete | Valeur |
|-----------|--------|
| ID complet | `petasse_gang_additions:nom_de_la_dimension` |
| Portail | bloc `nom_du_portail_block` |
| Biome(s) | ... |
| Generation | `data/petasse_gang_additions/worldgen/` |
| Gravite | normale / reduite / zero |
| Ciel | normal / neant / custom |

### Description
...

### Acces

```mcfunction
/execute in petasse_gang_additions:nom_de_la_dimension run teleport @s 0 64 0
```
