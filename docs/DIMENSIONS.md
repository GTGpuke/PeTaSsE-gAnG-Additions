# Dimensions — GTG's Backrooms

## Backrooms — Level 0 (`gtgs_backrooms:backrooms_level_0`)

| Propriété | Valeur |
|-----------|--------|
| Type de dimension | `backrooms_level_0_type` : `min_y` 0, `height` 64, sans ciel, plafond, lumière ambiante 0, heure fixe |
| Biome | `gtgs_backrooms:backrooms_level_0` (vide, aucun spawn) |
| Générateur | `gtgs_backrooms:backrooms_level_zero` → `LevelZeroChunkGenerator` |
| Accès | **Portail des Backrooms**, posé avec la baguette de portail (onglet créatif *GTG's Backrooms*) : posé dans le monde normal, il ouvre un portail double face vers le layer 0 ; posé dans le Level 0, il ramène à la surface. Voir [PORTALS.md](PORTALS.md). |

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
/execute in gtgs_backrooms:backrooms_level_0 run teleport @s 1 1 1
```

Le point (1, 1, 1) est le centre de la cellule d'origine du layer 0, toujours ouvert.

### Fichiers

- `data/gtgs_backrooms/dimension/backrooms_level_0.json`
- `data/gtgs_backrooms/dimension_type/backrooms_level_0_type.json`
- `data/gtgs_backrooms/worldgen/biome/backrooms_level_0.json`
- `src/main/java/com/gtgpuke/gtgsbackrooms/backrooms/level0/generation/`
- Test en jeu : `src/gametest/java/com/gtgpuke/gtgsbackrooms/gametest/backrooms/level0/LevelZeroGameTests.java` (`./gradlew runGameTest`)
