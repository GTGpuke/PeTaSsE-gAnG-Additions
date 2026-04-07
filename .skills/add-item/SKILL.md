---
name: add-item
description: "Ajouter un nouvel item au mod PeTaSsE_gAnG_Additions. Utilise ce skill dès qu'on veut créer un item, outil, arme, consommable, ou objet. Déclenche pour 'ajoute', 'crée', 'nouveau' + 'item', 'objet', 'outil', 'arme', 'nourriture', 'carte', 'clé', 'badge', 'épée', 'pioche', 'hache', 'arc'."
---

# Skill — Ajouter un Item

## Quand utiliser ce skill

- "Ajoute un item [nom]"
- "Crée un objet [description]"
- "Nouveau [outil/arme/consommable/badge] appelé [nom]"
- "Je veux un item qui [comportement]"

---

## Étapes

### 1. Définir les paramètres

Avant de coder, détermine :

| Paramètre | Exemples |
|-----------|---------|
| `ITEM_ID` (snake_case) | `gang_sword`, `magic_key`, `mystery_card` |
| `ClassName` (PascalCase) | `GangSwordItem`, `MagicKeyItem`, `MysteryCardItem` |
| Stack max | `1` (unique) ou `64` (stackable) |
| Rareté | `COMMON`, `UNCOMMON`, `RARE`, `EPIC` |
| Comportement custom | tooltip, foil, durabilité, nourriture, etc. |
| Nom EN / Nom FR | "Gang Sword" / "Épée de la Gang" |

---

### 2. Créer la classe item

**Fichier :** `src/main/java/com/petassegang/addons/item/MyItem.java`

```java
package com.petassegang.addons.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * [Nom de l'item] — [Description courte].
 */
public class MyItem extends Item {

    // Pré-allouer les tooltips si nécessaire (jamais dans appendHoverText !)
    private static final Component TOOLTIP_LINE = Component.literal("My tooltip")
            .withStyle(style -> style.withColor(0xFFD700));

    public MyItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack,
                                TooltipContext context,
                                List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(TOOLTIP_LINE);
    }

    // Override isFoil() → true pour le glint enchantement
    // Override getUseDuration() pour les consommables
    // Override finishUsingItem() pour l'effet au consommation
}
```

**Pour un item simple sans comportement custom :** inutile de créer une classe, utiliser directement `Item` dans ModItems :
```java
() -> new Item(new Item.Properties().setId(ITEMS.key("my_item_id")).stacksTo(64))
```

---

### 3. Enregistrer dans ModItems.java

**Fichier :** `src/main/java/com/petassegang/addons/init/ModItems.java`

```java
// Ajouter après la dernière RegistryObject :
public static final RegistryObject<Item> MY_ITEM = ITEMS.register(
        "my_item_id",
        () -> new MyItem(
                new Item.Properties()
                        .setId(ITEMS.key("my_item_id")) // OBLIGATOIRE en MC 26.1
                        .stacksTo(1)                    // ou 64
                        .rarity(Rarity.RARE)            // COMMON / UNCOMMON / RARE / EPIC
        )
);
```

---

### 4. Ajouter au creative tab

**Fichier :** `src/main/java/com/petassegang/addons/creative/ModCreativeTab.java`

```java
// Dans displayItems(), après la dernière ligne output.accept(...) :
output.accept(ModItems.MY_ITEM.get());
```

---

### 5. Créer le modèle JSON

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

### 6. Ajouter la texture

**Fichier :** `src/main/resources/assets/petasse_gang_additions/textures/item/my_item_id.png`

- Format : PNG, 16×16 px, RGBA
- Crée la texture dans ton éditeur préféré (Aseprite, GIMP, Pixilart…)
- Ou génère un placeholder via PowerShell :

```powershell
Add-Type -AssemblyName System.Drawing
$bmp = New-Object System.Drawing.Bitmap(16, 16)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.Clear([System.Drawing.Color]::FromArgb(255, 128, 0, 255))  # Violet
$bmp.Save("src\main\resources\assets\petasse_gang_additions\textures\item\my_item_id.png", [System.Drawing.Imaging.ImageFormat]::Png)
```

---

### 7. Ajouter les traductions

**Fichier :** `src/main/resources/assets/petasse_gang_additions/lang/en_us.json`
```json
"item.petasse_gang_additions.my_item_id": "My Item Name",
```

**Fichier :** `src/main/resources/assets/petasse_gang_additions/lang/fr_fr.json`
```json
"item.petasse_gang_additions.my_item_id": "Nom de Mon Objet",
```

---

### 8. Ajouter une recette (optionnel)

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

### 9. Écrire le test

**Fichier :** `src/test/java/com/petassegang/addons/ItemTest.java`

```java
@Test
@DisplayName("MyItem stack size is X")
void testMyItemStackSize() {
    // Ajuster selon les propriétés réelles
    var item = new MyItem(new Item.Properties().stacksTo(1));
    assertEquals(1, item.getDefaultMaxStackSize());
}

// Et dans RegistryTest.java :
@Test
@DisplayName("MY_ITEM RegistryObject is not null")
void testMyItemRegistryObjectNotNull() {
    assertNotNull(ModItems.MY_ITEM);
}
```

---

### 10. Mettre à jour la documentation

- [ ] `docs/ITEMS.md` — ajouter la fiche de l'item
- [ ] `docs/CHANGELOG.md` — ajouter sous `### Added`
- [ ] Build : `./gradlew build`
- [ ] Tests : `./gradlew test`

---

## Checklist finale

- [ ] Classe `item/MyItem.java` créée (ou pas nécessaire si item simple)
- [ ] `init/ModItems.java` — `RegistryObject` ajouté
- [ ] `creative/ModCreativeTab.java` — `output.accept()` ajouté
- [ ] `models/item/my_item_id.json` créé
- [ ] `textures/item/my_item_id.png` créé (16x16)
- [ ] `lang/en_us.json` mis à jour
- [ ] `lang/fr_fr.json` mis à jour
- [ ] Recette ajoutée (si applicable)
- [ ] Test `ItemTest.java` mis à jour
- [ ] Test `RegistryTest.java` mis à jour
- [ ] `docs/ITEMS.md` mis à jour
- [ ] `docs/CHANGELOG.md` mis à jour
- [ ] `./gradlew build` passe sans erreur
- [ ] `./gradlew test` — tous les tests verts
