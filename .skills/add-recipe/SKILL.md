---
name: add-recipe
description: "Ajouter une recette au mod PétasseGang Addons. Déclenche pour 'recette', 'craft', 'fabriquer', 'fondre', 'cuisiner', 'table de craft', 'fourneau', 'smithing', 'recipe'."
---

# Skill — Ajouter une Recette

## Quand utiliser ce skill

- "Ajoute une recette de craft pour [item]"
- "Le [item] se fabrique avec [ingrédients]"
- "Fais fondre [item] dans un fourneau"

---

## Types de recettes

| Type | JSON type | Usage |
|------|-----------|-------|
| Craft ordonné | `minecraft:crafting_shaped` | Table de craft avec pattern |
| Craft libre | `minecraft:crafting_shapeless` | Ordre non important |
| Fusion fourneau | `minecraft:smelting` | Fourneau standard |
| Fusion blast | `minecraft:blasting` | Blast furnace (minerais) |
| Fumage | `minecraft:smoking` | Smoker (nourriture) |
| Campfire | `minecraft:campfire_cooking` | |
| Smithing | `minecraft:smithing_transform` | Upgrade via enclume smithing |
| Stonecutter | `minecraft:stonecutting` | Tailleur de pierre |

---

## Emplacement des fichiers

```
data/petassegang_addons/recipes/
└── my_item.json
```

**Convention :** un fichier par item crafté. Le nom du fichier = l'ID de l'item résultant.

---

## Templates

### Craft ordonné (3x3)

```json
{
  "type": "minecraft:crafting_shaped",
  "category": "misc",
  "pattern": [
    "ABA",
    "BCB",
    "ABA"
  ],
  "key": {
    "A": { "item": "minecraft:gold_ingot" },
    "B": { "item": "minecraft:diamond" },
    "C": { "item": "minecraft:nether_star" }
  },
  "result": {
    "id": "petassegang_addons:my_item",
    "count": 1
  }
}
```

### Craft libre (shapeless)

```json
{
  "type": "minecraft:crafting_shapeless",
  "category": "misc",
  "ingredients": [
    { "item": "minecraft:gold_ingot" },
    { "item": "minecraft:gold_ingot" },
    { "item": "minecraft:diamond" }
  ],
  "result": {
    "id": "petassegang_addons:my_item",
    "count": 1
  }
}
```

### Fusion fourneau

```json
{
  "type": "minecraft:smelting",
  "category": "misc",
  "ingredient": { "item": "petassegang_addons:raw_gangite" },
  "result": { "id": "petassegang_addons:gangite_ingot" },
  "experience": 1.0,
  "cookingtime": 200
}
```

### Blast furnace (minerai)

```json
{
  "type": "minecraft:blasting",
  "category": "misc",
  "ingredient": { "item": "petassegang_addons:gangite_ore" },
  "result": { "id": "petassegang_addons:gangite_ingot" },
  "experience": 0.7,
  "cookingtime": 100
}
```

---

## Checklist finale

- [ ] Fichier JSON créé dans `data/petassegang_addons/recipes/`
- [ ] Ingrédients corrects (vérifier les item IDs)
- [ ] Résultat correct (vérifier l'item ID)
- [ ] Testé en jeu : `/recipe give @p petassegang_addons:my_item`
- [ ] `docs/ITEMS.md` mis à jour (mentionner la recette)
- [ ] `docs/CHANGELOG.md` mis à jour
