---
name: add-item
description: "Ajouter un nouvel item au mod PeTaSsE_gAnG_Additions (Fabric 1.21.1). Utilise ce skill dès qu'on veut créer un item, outil, arme, consommable, ou objet. Déclenche pour 'ajoute', 'crée', 'nouveau' + 'item', 'objet', 'outil', 'arme', 'nourriture', 'carte', 'clé', 'badge', 'épée', 'pioche', 'hache', 'arc'."
---

# Skill — Ajouter un Item (Fabric 1.21.1)

## Quand utiliser ce skill

- "Ajoute un item [nom]"
- "Crée un objet [description]"
- "Nouveau [outil/arme/consommable/badge] appelé [nom]"
- "Je veux un item qui [comportement]"

---

## Étapes

### 1. Définir les paramètres

| Paramètre | Exemples |
|-----------|---------|
| `ITEM_ID` (snake_case) | `gang_sword`, `magic_key`, `mystery_card` |
| `ClassName` (PascalCase) | `GangSwordItem`, `MagicKeyItem`, `MysteryCardItem` |
| Stack max | `1` (unique) ou `64` (stackable) |
| Rareté | `Rarity.COMMON`, `Rarity.UNCOMMON`, `Rarity.RARE`, `Rarity.EPIC` |
| Comportement custom | tooltip, glint, durabilité, nourriture, etc. |
| Nom EN / Nom FR | "Gang Sword" / "Épée de la Gang" |

---

### 2. Créer la classe item

**Fichier :** `src/main/java/com/petassegang/addons/feature/my_feature/item/my_item/MyItem.java`

```java
package com.petassegang.addons.feature.my_feature.item.my_item;

import java.util.List;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * [Nom de l'item] — [Description courte].
 */
public class MyItem extends Item {

    private static final String TOOLTIP_KEY = "item.petasse_gang_additions.my_item_id.tooltip";
    private static final Text TOOLTIP_LINE =
            Text.translatable(TOOLTIP_KEY).formatted(Formatting.GOLD);

    public MyItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context,
                              List<Text> tooltip, TooltipType type) {
        tooltip.add(TOOLTIP_LINE);
    }

    // Override hasGlint() → true pour le glint enchantement.
    // Override getMaxUseTime() pour les consommables.
    // Override finishUsing() pour l'effet à la consommation.
    // Override use() pour le clic droit.
}
```

**Pour un item simple sans comportement custom :** inutile de créer une classe, utiliser directement `Item` dans ModItems.

**Rangement v4 :**
- Item lie a une feature : `feature/<feature>/item/<item_id>/`.
- Item Backrooms Level 0 : `backrooms/level/level0/...` si le code est specifique a ce niveau.
- Eviter de recreer un package racine `item/`.

---

### 3. Enregistrer dans ModItems.java

**Fichier :** `src/main/java/com/petassegang/addons/init/ModItems.java`

```java
// Ajouter après le dernier champ static final :
public static final Item MY_ITEM = Registry.register(
        Registries.ITEM,
        Identifier.of(ModConstants.MOD_ID, "my_item_id"),
        new MyItem(
                new Item.Settings()
                        .maxCount(1)          // ou 64
                        .rarity(Rarity.RARE)  // COMMON / UNCOMMON / RARE / EPIC
        )
);
```

**Important :**
- Pas de `.setId()` — Fabric n'en a pas besoin.
- Le champ contient directement l'objet, pas un `RegistryObject`.
- Utiliser `ModItems.MY_ITEM` directement partout, pas `ModItems.MY_ITEM.get()`.

---

### 4. Ajouter au creative tab

**Fichier :** `src/main/java/com/petassegang/addons/creative/ModCreativeTab.java`

```java
// Dans le FabricItemGroupEvents.modifyEntriesEvent callback :
entries.add(ModItems.MY_ITEM);
```

---

### 5. Créer la définition d'item (obligatoire en MC 1.21.1)

**Fichier :** `src/main/resources/assets/petasse_gang_additions/items/my_item_id.json`

```json
{
  "model": {
    "type": "minecraft:model",
    "model": "petasse_gang_additions:item/my_item_id"
  }
}
```

> **MC 1.21.1 exige ce fichier.** Sans lui, l'item s'affiche comme un carré violet/noir.

---

### 6. Créer le modèle JSON

**Fichier :** `src/main/resources/assets/petasse_gang_additions/models/item/my_item_id.json`

```json
{
  "parent": "item/generated",
  "textures": {
    "layer0": "petasse_gang_additions:item/my_item_id"
  }
}
```

Pour un outil/arme (tenu à la main) : utiliser `"parent": "item/handheld"`.

---

### 7. Ajouter la texture

**Fichier :** `src/main/resources/assets/petasse_gang_additions/textures/item/my_item_id.png`

- Format : PNG, 16×16 px, RGBA
- Crée la texture dans ton éditeur préféré (Aseprite, GIMP, Pixilart…)

---

### 8. Ajouter les traductions

**Fichier :** `src/main/resources/assets/petasse_gang_additions/lang/en_us.json`
```json
"item.petasse_gang_additions.my_item_id": "My Item Name",
"item.petasse_gang_additions.my_item_id.tooltip": "My item description."
```

**Fichier :** `src/main/resources/assets/petasse_gang_additions/lang/fr_fr.json`
```json
"item.petasse_gang_additions.my_item_id": "Nom de Mon Objet",
"item.petasse_gang_additions.my_item_id.tooltip": "Description de mon objet."
```

---

### 9. Ajouter une recette (optionnel)

**Fichier :** `data/petasse_gang_additions/recipes/my_item_id.json`

```json
{
  "type": "minecraft:crafting_shaped",
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
    "id": "petasse_gang_additions:my_item_id",
    "count": 1
  }
}
```

---

### 10. Écrire le test

**Fichier :** `src/test/java/com/petassegang/addons/ItemTest.java`

```java
// Test sans bootstrap (constantes/champs statiques uniquement) :
@Test
@DisplayName("MY_ITEM n'est pas null")
void testMyItemNotNull() {
    assertNotNull(ModItems.MY_ITEM, "Le champ MY_ITEM doit être non-null.");
}

// Test avec bootstrap (instanciation MC) :
@BeforeAll
static void bootstrapMinecraft() {
    SharedConstants.createGameVersion();
    Bootstrap.bootstrap();
}

@Test
@DisplayName("MyItem stack size is 1")
void testMyItemStackSize() {
    var item = new MyItem(new Item.Settings().maxCount(1));
    assertEquals(1, item.getMaxCount(), "La taille de pile doit être 1.");
}
```

---

### 11. Mettre à jour la documentation

- [ ] `docs/ITEMS.md` — ajouter la fiche de l'item
- [ ] `docs/CHANGELOG.md` — ajouter sous `### Added`
- [ ] Build : `./gradlew build`
- [ ] Tests : `./gradlew test`

---

## Checklist finale

- [ ] Classe `feature/<feature>/item/<item_id>/MyItem.java` créée (ou pas nécessaire si item simple)
- [ ] `init/ModItems.java` — champ `static final` ajouté avec `Registry.register()`
- [ ] `creative/ModCreativeTab.java` — `entries.add()` ajouté
- [ ] `items/my_item_id.json` créé (obligatoire MC 1.21.1 — sinon carré violet)
- [ ] `models/item/my_item_id.json` créé
- [ ] `textures/item/my_item_id.png` créé (16×16)
- [ ] `lang/en_us.json` mis à jour
- [ ] `lang/fr_fr.json` mis à jour
- [ ] Recette ajoutée (si applicable)
- [ ] Test ajouté (RegistryTest ou ItemTest)
- [ ] `docs/ITEMS.md` mis à jour
- [ ] `docs/CHANGELOG.md` mis à jour
- [ ] `./gradlew build` passe sans erreur
