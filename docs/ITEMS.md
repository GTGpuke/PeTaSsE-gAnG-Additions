# Items — PeTaSsE_gAnG_Additions

Catalogue des items du mod, tous enregistrés dans `init/ModItems.java` et listés dans `ModCreativeTab.ENTRIES`
ou `ModCreativeTab.PORTAL_ENTRIES` (deux onglets créatifs : *PetasseGang* et *PetasseGang - Portails*).

---

## Gang Badge (`gang_badge`)

| Propriété | Valeur |
|-----------|--------|
| Classe | `feature/gang/GangBadgeItem.java` |
| Rareté | EPIC |
| Stack max | 1 |
| Brillance | toujours (`hasGlint` → `true`) |
| Clic droit | miaulement (`ENTITY_CAT_AMBIENT`) + payload `gang_badge_activate` → animation de totem côté client |
| Tooltip | « PetasseGang Official Member » (or, gras) + « Don't lose it, there's no replacement » (gris, italique) |

Fichiers : `GangBadgeItem`, `GangBadgeActivatePayload` (S2C, sans données, enregistré par `ModNetworking`),
`client/GangBadgeClientHandler`, `models/item/gang_badge.json`, `textures/item/gang_badge.png`.

```mcfunction
/give @p petasse_gang_additions:gang_badge
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
`cursed_leaves`, `cursed_sapling`, `cursed_planks` (onglet *PetasseGang*) et `backrooms_portal` (onglet *PetasseGang - Portails*).

---

## Portail des Backrooms (`backrooms_portal`)

| Propriété | Valeur |
|-----------|--------|
| Classe | `BackroomsPortalItem` (un `BlockItem` de `BackroomsPortalBlock`) |
| Composant | `petasse_gang_additions:portal_settings` (`PortalSettings`) : `width` et `height` de 1 à 8 (1 x 2 par défaut), `transmits_light` (oui par défaut), `double_sided` (deux faces par défaut ; à une face, rien de dos, on passe au travers), `one_way` (aller-retour par défaut ; aller simple, rien à l'arrivée pour revenir) ; affiché dans l'info-bulle |
| Onglet | *PetasseGang - Portails* (`ModCreativeTab.PORTAL_TAB`, icône de l'onglet) : 1 x 2, 2 x 3, 3 x 4, 1 x 2 sans lumière, 1 x 2 à une face et 1 x 2 aller simple |
| Pose | comme un bloc, face avant vers le joueur ; refusée si le cadre traverserait un bloc |
| Usage | le portail se pose dès que la block entity a trouvé sa destination ; accroupi, le bloc se vise et se casse, et rend l'item avec ses réglages |
| Détail | [PORTALS.md](PORTALS.md) |

---

## Ajouter un item

1. Champ `static final` dans `ModItems` + entrée dans `ModCreativeTab.ENTRIES` (ou `PORTAL_ENTRIES` pour un item lié aux portails).
2. `models/item/<id>.json` (parent `item/generated` ou `item/handheld`) + texture 16x16.
3. Clé `item.petasse_gang_additions.<id>` en EN et FR.
4. Un test dans `src/test/java/com/petassegang/addons/feature/...` qui vérifie le comportement (pas la réflexion).
