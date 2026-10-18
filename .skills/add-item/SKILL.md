---
name: add-item
description: "Ajouter un nouvel item au mod GTG's Backrooms (Fabric 1.21.1). Utilise ce skill dès qu'on veut créer un item, outil, arme, consommable, ou objet. Déclenche pour 'ajoute', 'crée', 'nouveau' + 'item', 'objet', 'outil', 'arme', 'nourriture', 'carte', 'clé', 'badge', 'épée', 'pioche', 'hache', 'arc'."
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

**Fichier :** `src/main/java/com/gtgpuke/gtgsbackrooms/feature/my_feature/MyItem.java`

```java
package com.gtgpuke.gtgsbackrooms.feature.my_feature;

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

    private static final String TOOLTIP_KEY = "item.gtgs_backrooms.my_item_id.tooltip";
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

**Rangement :**
- Item lié à une feature : `feature/<feature>/MaClasseItem.java`, une classe par item, pas de sous-package `item/`.
- Item propre au Level 0 : `backrooms/level0/...`.
- Pas de package racine `item/`.

---

### 3. Enregistrer dans ModItems.java

**Fichier :** `src/main/java/com/gtgpuke/gtgsbackrooms/init/ModItems.java`

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

**Fichier :** `src/main/java/com/gtgpuke/gtgsbackrooms/init/ModCreativeTab.java`

Ajouter `ModItems.MY_ITEM` à la liste `ENTRIES` (onglet principal) ou `PORTAL_ENTRIES` (onglet Portails) :
`register(...)` construit l'onglet à partir de cette liste. `ModCreativeTabTest` vérifie que chaque item y figure.

---

### 5. Créer le modèle JSON

**Fichier :** `src/main/resources/assets/gtgs_backrooms/models/item/my_item_id.json`

```json
{
  "parent": "item/generated",
  "textures": {
    "layer0": "gtgs_backrooms:item/my_item_id"
  }
}
```

Pour un outil/arme (tenu à la main) : utiliser `"parent": "item/handheld"`.

> En 1.21.1, seul `models/item/<id>.json` compte : les définitions `assets/<ns>/items/*.json` n'existent qu'à partir de 1.21.4.

---

### 6. Ajouter la texture

**Fichier :** `src/main/resources/assets/gtgs_backrooms/textures/item/my_item_id.png`

- Format : PNG, 16×16 px, RGBA
- Crée la texture dans ton éditeur préféré (Aseprite, GIMP, Pixilart…)

---

### 7. Ajouter les traductions

**Fichier :** `src/main/resources/assets/gtgs_backrooms/lang/en_us.json`
```json
"item.gtgs_backrooms.my_item_id": "My Item Name",
"item.gtgs_backrooms.my_item_id.tooltip": "My item description."
```

**Fichier :** `src/main/resources/assets/gtgs_backrooms/lang/fr_fr.json`
```json
"item.gtgs_backrooms.my_item_id": "Nom de Mon Objet",
"item.gtgs_backrooms.my_item_id.tooltip": "Description de mon objet."
```

---

### 8. Ajouter une recette (optionnel)

**Fichier :** `data/gtgs_backrooms/recipes/my_item_id.json`

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
    "id": "gtgs_backrooms:my_item_id",
    "count": 1
  }
}
```

---

### 9. Écrire le test

**Fichier :** `src/test/java/com/gtgpuke/gtgsbackrooms/feature/my_feature/MyItemTest.java`

Un test de comportement par méthode (pas de test par réflexion), sous le classloader Fabric avec les vrais registres :

```java
@DisplayName("Mon item")
class MyItemTest {

    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.ensure();
    }

    @Test
    @DisplayName("L'item est unique par pile et rare")
    void properties() {
        ItemStack stack = new ItemStack(ModItems.MY_ITEM);
        assertEquals(1, ModItems.MY_ITEM.getMaxCount(), "Pile de un.");
        assertEquals(Rarity.RARE, stack.get(DataComponentTypes.RARITY), "Rarete RARE.");
    }
}
```

Modèle, texture, traductions EN et FR et onglet créatif sont vérifiés automatiquement par `ModRegistriesTest` et
`ModCreativeTabTest` (`src/test/java/com/gtgpuke/gtgsbackrooms/init/`). La couverture fusionnée exige que chaque
instruction de la classe soit exécutée par un test unitaire ou un test en jeu (`docs/TESTING.md`).

---

### 10. Mettre à jour la documentation

- [ ] `docs/ITEMS.md` — ajouter la fiche de l'item
- [ ] `docs/CHANGELOG.md` — ajouter sous `### Added`
- [ ] Build : `./gradlew build`
- [ ] Tests : `./gradlew test`

---

## Checklist finale

- [ ] Classe `feature/<feature>/MyItem.java` créée (ou pas nécessaire si item simple)
- [ ] `init/ModItems.java` — champ `static final` ajouté avec `Registry.register()`
- [ ] `init/ModCreativeTab.java` — item ajouté à `ENTRIES` ou `PORTAL_ENTRIES`
- [ ] `models/item/my_item_id.json` créé
- [ ] `textures/item/my_item_id.png` créé (16×16)
- [ ] `lang/en_us.json` mis à jour
- [ ] `lang/fr_fr.json` mis à jour
- [ ] Recette ajoutée (si applicable)
- [ ] Test de comportement ajouté (`feature/<feature>/MyItemTest`) ; `ModRegistriesTest` vert
- [ ] `docs/ITEMS.md` mis à jour
- [ ] `docs/CHANGELOG.md` mis à jour
- [ ] `./gradlew build` passe sans erreur
