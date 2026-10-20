# Items — GTG's Backrooms

Catalogue des items du mod, tous enregistrés dans `init/ModItems.java` et listés dans `ModCreativeTab.ENTRIES`
ou `ModCreativeTab.PORTAL_ENTRIES` (deux onglets créatifs : *GTG's Backrooms* et *GTG's Backrooms - Portails*).

---

## Gang Badge (`gang_badge`)

| Propriété | Valeur |
|-----------|--------|
| Classe | `feature/gang/GangBadgeItem.java` |
| Rareté | EPIC |
| Stack max | 1 |
| Brillance | toujours (`hasGlint` → `true`) |
| Clic droit | miaulement (`ENTITY_CAT_AMBIENT`) + payload `gang_badge_activate` → animation de totem côté client |
| Tooltip | « Official Member » (or, gras) + « Don't lose it, there's no replacement » (gris, italique) |

Fichiers : `GangBadgeItem`, `GangBadgeActivatePayload` (S2C, sans données, enregistré par `ModNetworking`),
`client/GangBadgeClientHandler`, `models/item/gang_badge.json`, `textures/item/gang_badge.png`.

```mcfunction
/give @p gtgs_backrooms:gang_badge
```

---

## Casse-croûte Maudit (`cursed_snack`)

| Propriété | Valeur |
|-----------|--------|
| Classe | `feature/cursed/CursedSnackItem.java` |
| Stack max | 16 |
| Nourriture | nutrition 0, saturation 0, toujours mangeable |
| Effet | retire `HUNGER_PENALTY` = 2 points de faim (jamais sous 0) |
| Tooltip | « Maybe don't eat that. » (rouge sombre, italique) |

---

## Blocs sous forme d'item

Chaque bloc de [BLOCKS.md](BLOCKS.md) possède un `BlockItem` du même identifiant :
`level_zero_wallpaper`, `level_zero_wallpaper_aged`, `level_zero_wallpaper_adaptive`, `level_zero_damp_carpet`,
`level_zero_damp_carpet_aged`, `level_zero_ceiling_tile`, `level_zero_fluorescent_light`, `cursed_log`,
`cursed_leaves`, `cursed_sapling`, `cursed_planks` (onglet *GTG's Backrooms*) et `backrooms_portal` (onglet *GTG's Backrooms - Portails*).

---

## Portail des Backrooms (`backrooms_portal`)

| Propriété | Valeur |
|-----------|--------|
| Classe | `BackroomsPortalItem` (un `BlockItem` de `BackroomsPortalBlock`) |
| Composant | `gtgs_backrooms:portal_settings` (`PortalSettings`) : `width` et `height` de 1 à 8 (1 x 2 par défaut), `transmits_light` (oui par défaut), `double_sided` (deux faces par défaut ; à une face, rien de dos, on passe au travers), `one_way` (aller-retour par défaut ; aller simple, rien à l'arrivée pour revenir), `fleeting_exit` (sortie durable par défaut ; fugace, elle disparaît quand plus aucun joueur ne la regarde, jusqu'au prochain joueur qui passe l'entrée), `exit` (`straight` par défaut : on ressort dans la direction d'entrée ; `north`, `east`, `south` ou `west` imposent le sens de sortie, `floor` et `ceiling` couchent le cadre d'arrivée au sol ou au plafond), `exit_scatter` (0 par défaut : arrivée aux mêmes X/Z ; sinon rayon, jusqu'à 64 blocs, du tirage au sort des X/Z de l'arrivée) ; affiché dans l'info-bulle |
| Onglet | *GTG's Backrooms - Portails* (`ModCreativeTab.PORTAL_TAB`, icône de l'onglet), page 2 des onglets : 37 réglages (`PortalSettings.PRESETS`), le portail par défaut en tête ; les tailles 1 x 1, 2 x 2, 2 x 3, 3 x 3, 3 x 4, 4 x 4 et 8 x 8 ; chaque sens de sortie (nord, est, sud, ouest, sol, plafond) pour un cadre 1 x 2 puis pour un cadre 2 x 2 ; l'arrivée tirée au sort à 8, 32 et 64 blocs, puis chaque sens de sortie tiré au sort à 32 blocs ; sans lumière, à une face, aller simple, à sortie fugace ; enfin quatre combinaisons (trappe 2 x 2 en aller simple, aller simple au hasard à 64 blocs, sortie fugace au hasard à 32 blocs, aller simple à une face sans lumière). Chacun se pose debout comme couché |
| Nom | celui du bloc, suivi entre parenthèses de ce qui l'écarte du portail par défaut : *Portail des Backrooms (2x2, sortie par le plafond)* ; une pile renommée garde son nom |
| Pose | comme un bloc, face avant vers le joueur, ou couché au sol ou au plafond en regardant franchement vers le bas ou vers le haut (l'info-bulle le rappelle) ; refusée si le cadre traverserait un bloc |
| Usage | le portail se pose dès que la block entity a trouvé sa destination ; accroupi, le bloc se vise et se casse, et rend l'item avec ses réglages |
| Détail | [PORTALS.md](PORTALS.md) |

---

## Ajouter un item

1. Champ `static final` dans `ModItems` + entrée dans `ModCreativeTab.ENTRIES` (ou `PORTAL_ENTRIES` pour un item lié aux portails).
2. `models/item/<id>.json` (parent `item/generated` ou `item/handheld`) + texture 16x16.
3. Clé `item.gtgs_backrooms.<id>` en EN et FR.
4. Un test dans `src/test/java/com/gtgpuke/gtgsbackrooms/feature/...` qui vérifie le comportement (pas la réflexion).
