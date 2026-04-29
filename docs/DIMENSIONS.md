# Dimensions — PeTaSsE_gAnG_Additions

Catalogue de toutes les dimensions custom du mod.

---

## Backrooms — Level 0 (`backrooms_level_0`)

| Propriété | Valeur |
|-----------|--------|
| ID complet | `petasse_gang_additions:backrooms_level_0` |
| Portail | Aucun pour l'instant |
| Biome(s) | `minecraft:the_void` |
| Génération | `LevelZeroChunkGenerator` multi-layer |
| Gravité | normale |
| Ciel | noir, sans skybox |

### Description

Première passe du Level 0 basée sur le script Python de référence.
Le niveau génère un labyrinthe déterministe par layer, avec une pile verticale
canonique et des slices seedées indépendamment. Chaque layer réutilise le coeur
historique du layout pour garantir une topologie stable.

Le layout combine :
- superposition de maze,
- salles rectangulaires,
- salles à piliers,
- salles polygonales.

### Fichiers principaux

- `src/main/java/com/petassegang/addons/backrooms/BackroomsConstants.java`
- `src/main/java/com/petassegang/addons/backrooms/level/level0/generation/LevelZeroChunkGenerator.java`
- `src/main/java/com/petassegang/addons/backrooms/level/level0/generation/layout/LevelZeroLayout.java`
- `src/main/java/com/petassegang/addons/backrooms/level/level0/biome/LevelZeroSurfaceBiome.java`
- `src/main/resources/data/petasse_gang_additions/dimension/backrooms_level_0.json`
- `src/main/resources/data/petasse_gang_additions/dimension_type/backrooms_level_0_type.json`
- `docs/dimensions/backrooms-level0-dimension-type-multilayer.json`

### Accès

```mcfunction
/execute in petasse_gang_additions:backrooms_level_0 run teleport @s 0 63 0
```

---

## Template de documentation de dimension

### NomDeLaDimension (`nom_de_la_dimension`)

| Propriété | Valeur |
|-----------|--------|
| ID complet | `petasse_gang_additions:nom_de_la_dimension` |
| Portail | bloc `nom_du_portail_block` |
| Biome(s) | ... |
| Génération | `data/petasse_gang_additions/worldgen/` |
| Gravité | normale / réduite / zéro |
| Ciel | normal / néant / custom |

### Description
...

### Accès

```mcfunction
/execute in petasse_gang_additions:nom_de_la_dimension run teleport @s 0 64 0
```
