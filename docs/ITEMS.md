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
| `item/GangBadgeItem.java` | Logique (tooltip, foil) |
| `init/ModItems.java` | Enregistrement |
| `models/item/gang_badge.json` | Modèle 3D |
| `textures/item/gang_badge.png` | Texture 16x16 |
| `lang/en_us.json` | Nom EN |
| `lang/fr_fr.json` | Nom FR |

---

## Ajouter un nouvel item

Dis à Claude Code :
> "Ajoute un item [description]"

Ou suis le skill `/.skills/add-item/SKILL.md` manuellement.
