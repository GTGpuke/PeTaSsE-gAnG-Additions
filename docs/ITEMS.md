# Items — PeTaSsE_gAnG_Additions

Catalogue de tous les items du mod.

---

## Gang Badge (`gang_badge`)

| Propriété | Valeur |
|-----------|--------|
| ID complet | `petasse_gang_additions:gang_badge` |
| Classe | `com.petassegang.addons.item.GangBadgeItem` |
| Rareté | EPIC (nom violet) |
| Stack max | 1 |
| Glint (foil) | Toujours activé |
| Craftable | Non (obtenu via créatif ou commande) |

### Comportement au clic droit

Clic droit → son de chat + animation du totem (overlay visuel).
L'item **n'est pas consommé**.

### Tooltip

```
Gang Badge
  PétasseGang Official Member        [or/gras]
  Don't lose it, there's no replacement  [gris/italique]
```

### Obtenir en jeu
```
/give @p petasse_gang_additions:gang_badge
```

### Fichiers associés
| Fichier | Rôle |
|---------|------|
| `item/GangBadgeItem.java` | Logique (tooltip, foil, use) |
| `init/ModItems.java` | Enregistrement |
| `items/gang_badge.json` | Définition de rendu (MC 26.1 — OBLIGATOIRE) |
| `models/item/gang_badge.json` | Modèle 3D |
| `textures/item/gang_badge.png` | Texture 16x16 (badge doré avec étoile) |
| `lang/en_us.json` | Nom EN |
| `lang/fr_fr.json` | Nom FR |

---

## Casse-croûte Maudit (`cursed_snack`)

| Propriété | Valeur |
|-----------|--------|
| ID complet | `petasse_gang_additions:cursed_snack` |
| Classe | `com.petassegang.addons.item.CursedSnackItem` |
| Rareté | COMMON |
| Stack max | 16 |
| Consommable | Oui — animation `EAT`, toujours mangeable |
| Effet | Retire 2 points de nourriture (min 0) |

### Comportement à la consommation

Manger le Casse-croûte Maudit déclenche l'animation standard de repas,
puis **soustrait 2 points de faim** (`FoodData.eat(-2, 0)`).
Peut être mangé même le ventre plein (`canAlwaysEat = true`).

### Fichiers associés
| Fichier | Rôle |
|---------|------|
| `item/CursedSnackItem.java` | Logique (finishUsingItem, tooltip) |
| `init/ModItems.java` | Enregistrement |
| `items/cursed_snack.json` | Définition de rendu MC 26.1 |
| `models/item/cursed_snack.json` | Modèle 2D (`item/generated`) |
| `textures/item/cursed_snack.png` | Texture 16x16 (biscuit moisi) |

---

## Arbre Maudit

Ensemble de 4 blocs au thème violet/or. La pousse génère un arbre custom via `ModBlocks.CURSED_TREE_GROWER`.

| ID | Classe | Description |
|----|--------|-------------|
| `cursed_log` | `RotatedPillarBlock` | Tronc orientable (X/Y/Z), écorce violette veinée d'or |
| `cursed_leaves` | `TintedParticleLeavesBlock` | Feuilles violettes semi-transparentes |
| `cursed_sapling` | `SaplingBlock` | Pousse violette (génère un Arbre Maudit) |
| `cursed_planks` | `Block` | Planches violettes |

`cursed_log` est dans le tag `minecraft:logs` — nécessaire pour que les feuilles persistent.

### Fichiers associés (par bloc)
| Pattern | Fichiers |
|---------|---------|
| `blockstates/cursed_*.json` | État (axis pour log, `""` pour les autres) |
| `models/block/cursed_*.json` | Modèle 3D (cube_column, leaves, cross, cube_all) |
| `models/item/cursed_*.json` | Modèle inventaire |
| `items/cursed_*.json` | Définition de rendu MC 26.1 |
| `textures/block/cursed_*.png` | Textures 16x16 |
| `data/petasse_gang_additions/loot_table/blocks/cursed_*.json` | Tables de butin |
| `data/petasse_gang_additions/worldgen/configured_feature/cursed_tree.json` | Structure de l'arbre |
| `data/minecraft/tags/block/logs.json` | Tag logs (feuilles persistance) |

---

## Ajouter un nouvel item

Dis à Claude Code :
> "Ajoute un item [description]"

Ou suis le skill `/.skills/add-item/SKILL.md` manuellement.
