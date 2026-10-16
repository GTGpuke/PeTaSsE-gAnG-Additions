# Dimensions — PeTaSsE_gAnG_Additions

## Backrooms — Level 0 (`petasse_gang_additions:backrooms_level_0`)

| Propriété | Valeur |
|-----------|--------|
| Type de dimension | `backrooms_level_0_type` : `min_y` 0, `height` 64, sans ciel, plafond, lumière ambiante 0, heure fixe |
| Biome | `petasse_gang_additions:backrooms_level_0` (vide, aucun spawn) |
| Générateur | `petasse_gang_additions:backrooms_level_zero` → `LevelZeroChunkGenerator` |
| Accès | **Porte des Backrooms** (onglet créatif *PetasseGang - Portails*) : posée dans le monde normal, elle ouvre un portail vers le layer 0 ; posée dans le Level 0, elle ramène à la surface. Voir [PORTALS.md](PORTALS.md). |

### Géographie verticale

Cinq layers de dix blocs à partir de Y=0 (`LevelZeroLayerStackLayout`) :

| Layer | Sol | Air jouable | Plafond | Bedrock inter-layer |
|-------|-----|-------------|---------|---------------------|
| 0 | 0 | 1–4 | 5 | 6–9 |
| 1 | 10 | 11–14 | 15 | 16–19 |
| 2 | 20 | 21–24 | 25 | 26–29 |
| 3 | 30 | 31–34 | 35 | 36–39 |
| 4 | 40 | 41–44 | 45 | 46–49 |

Les Y 50 à 63 restent vides (hauteur de dimension multiple de 16).

### Génération

Chaque layer a sa propre seed dérivée du seed monde et donc son propre labyrinthe. Une cellule logique
fait 3x3 blocs. Le secteur (240x135 cellules) superpose des mini-labyrinthes (algorithme de Prim) et
tamponne des salles rectangulaires, à piliers et polygonales ; il est mis en cache (LRU, 256 secteurs).
Par cellule : grande pièce, biome cosmétique (régions de 48 cellules, le rouge seulement sur les layers 1 et 3),
topologie (couloir, angle, T, carrefour, cul-de-sac), néon (trame en quinconce), gap de couloir
(resserrement rare à un bloc). La zone autour de l'origine (cellules -2..2) est toujours ouverte.

Détails : [ARCHITECTURE.md](ARCHITECTURE.md), spec cible : `backrooms/backrooms-level0-pipeline-v6.md`.

### Accès

```mcfunction
/execute in petasse_gang_additions:backrooms_level_0 run teleport @s 1 1 1
```

Le point (1, 1, 1) est le centre de la cellule d'origine du layer 0, toujours ouvert.

### Fichiers

- `data/petasse_gang_additions/dimension/backrooms_level_0.json`
- `data/petasse_gang_additions/dimension_type/backrooms_level_0_type.json`
- `data/petasse_gang_additions/worldgen/biome/backrooms_level_0.json`
- `src/main/java/com/petassegang/addons/backrooms/level0/generation/`
- Test en jeu : `src/gametest/java/com/petassegang/addons/gametest/backrooms/level0/LevelZeroGameTests.java` (`./gradlew runGameTest`)
