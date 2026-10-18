# Blocs — PeTaSsE_gAnG_Additions

Catalogue des blocs du mod. Tous sont enregistrés dans `init/ModBlocks.java` ; les `BlockItem`
correspondants dans `init/ModItems.java`. Textures du Level 0 en 32x32.

---

## Backrooms — Level 0

| ID | Classe | Copie les réglages de | Rôle |
|----|--------|-----------------------|------|
| `level_zero_wallpaper` | `Block` | `end_stone` | Papier peint jauni, bloc décoratif simple (item créatif). |
| `level_zero_wallpaper_aged` | `Block` | `end_stone` | Papier peint blanc, bloc décoratif simple ; sert aussi de modèle aux faces blanches du mur adaptatif. |
| `level_zero_wallpaper_adaptive` | `LevelZeroWallpaperBlock` | `end_stone` | **Mur posé par le générateur sur toutes les colonnes visibles.** Propriété `face_mask` (0-15) : faces affichant la variante blanche. Les colonnes enfouies sont en `minecraft:bedrock`. |
| `level_zero_damp_carpet` | `Block` | `yellow_wool` | Sol du biome de base. |
| `level_zero_damp_carpet_aged` | `Block` | `yellow_wool` | Sol rouge du biome alternatif (layers 1 et 3). |
| `level_zero_ceiling_tile` | `Block` | `calcite` | Plafond. |
| `level_zero_fluorescent_light` | `Block` | `sea_lantern`, luminance 15 | Néon posé au centre du plafond d'une cellule éclairée. |
| `backrooms_portal` | `BackroomsPortalBlock` (block entity `BackroomsPortalBlockEntity`) | invisible, sans collision, son du verre, résistance 0,5, immobile pour les pistons | **Portail des Backrooms** : pose un portail plein cadre, le plan au milieu du bloc, à la taille, avec la lumière, les faces (une ou deux), le retour (aller-retour ou aller simple) et la sortie (durable, ou fugace : perdue de vue, elle disparaît) réglés par l'item ; face avant vers le joueur ; pose refusée si le cadre traverserait un bloc. Posé hors du Level 0 il y mène, posé dedans il ramène à la surface ; ses portails disparaissent avec lui. Ne se vise qu'accroupi. Loot : l'item avec ses réglages (`copy_components`). Voir [PORTALS.md](PORTALS.md). |

Chaque bloc a un blockstate, un modèle, un modèle d'item et une loot table
(`data/petasse_gang_additions/loot_table/blocks/<id>.json`) ; le mur adaptatif donne `level_zero_wallpaper`.
Le test `ModRegistriesTest` vérifie la présence de ces fichiers.

### Mur adaptatif et détails muraux

Le masque de faces est calculé à la génération par `LevelZeroWallSampler` (biome de la première colonne
d'air en face de chaque face). Un mur posé à la main recalcule son masque depuis le monde (`onBlockAdded`).
Côté client, `LevelZeroWallpaperBakedModel` dessine en overlay :

- une plinthe sur chaque face exposée de la ligne basse de chaque layer (`level_zero_baseboard_{north,...}.json`) ;
- rarement un interrupteur (deux blocs au-dessus du sol) ou une prise (un bloc), déterministes par colonne.

Aucun bloc supplémentaire n'existe dans le monde pour ces détails.

---

## Arbre Maudit

| ID | Classe | Description |
|----|--------|-------------|
| `cursed_log` | `PillarBlock` | Tronc orientable, dans le tag `minecraft:logs` (persistance des feuilles). |
| `cursed_leaves` | `LeavesBlock` | Feuilles. |
| `cursed_sapling` | `SaplingBlock` | Pousse ; génère l'arbre via la configured feature `cursed_tree` (`dirt_provider` obligatoire en 1.21.1). |
| `cursed_planks` | `Block` | Planches. |

---

## Ajouter un bloc

1. Champ `static final` dans `ModBlocks`, puis `BlockItem` dans `ModItems`, puis entrée dans `ModCreativeTab.ENTRIES`.
2. `blockstates/<id>.json`, `models/block/<id>.json`, `models/item/<id>.json`, texture, loot table.
3. Clé `block.petasse_gang_additions.<id>` dans `en_us.json` **et** `fr_fr.json` : l'item du bloc la reprend, pas de clé `item.*`.
4. `./gradlew test` : `ModRegistriesTest` échoue tant qu'un fichier ou une traduction manque.
