---
name: add-block
description: "Ajouter un bloc au mod PétasseGang Addons. Déclenche pour 'bloc', 'minerai', 'dalle', 'mur', 'escalier', 'porte', 'bloc décoratif', 'bloc fonctionnel', 'ore', 'block'."
---

# Skill — Ajouter un Bloc

## Quand utiliser ce skill

- "Ajoute un bloc [nom]"
- "Crée un minerai [nom]"
- "Nouveau bloc décoratif [description]"

---

## Étapes

### 1. Définir les paramètres

| Paramètre | Valeur |
|-----------|--------|
| `BLOCK_ID` | `gangite_ore`, `petasse_block` |
| `ClassName` | `GangiteOreBlock`, `PetasseBlock` |
| Hardness | float, ex: `3.0f` |
| Blast resistance | float, ex: `3.0f` |
| Tool | `ToolType.PICKAXE` / `AXE` / `SHOVEL` |
| Harvest level | `0`(wood) `1`(stone) `2`(iron) `3`(diamond) |
| Son | `SoundType.STONE` / `WOOD` / `METAL` / `GLASS` |
| Drops | lui-même, silk touch, fortune, loot table |

---

### 2. Créer la classe bloc (si comportement custom)

**Fichier :** `src/main/java/com/petassegang/addons/block/MyBlock.java`

```java
package com.petassegang.addons.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * [Nom du bloc] — [Description].
 */
public class MyBlock extends Block {

    public MyBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    // Override getDrops() pour les drops custom
    // Override onPlace() pour comportement à la pose
    // Override randomTick() pour comportement aléatoire
}
```

Pour un bloc simple : utiliser `Block` directement dans ModBlocks.

---

### 3. Créer ModBlocks.java (si absent)

**Fichier :** `src/main/java/com/petassegang/addons/init/ModBlocks.java`

```java
package com.petassegang.addons.init;

import com.petassegang.addons.util.ModConstants;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, ModConstants.MOD_ID);

    public static final RegistryObject<Block> MY_BLOCK = BLOCKS.register(
            "my_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(3.0f, 3.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops())
    );

    public static void register(IEventBus bus) { BLOCKS.register(bus); }

    private ModBlocks() { throw new UnsupportedOperationException("Registry class"); }
}
```

Ajouter `ModBlocks.register(modEventBus)` dans `PetasseGangAddonsMod` constructor.

---

### 4. Ajouter le BlockItem dans ModItems.java

```java
// Item permettant de tenir/poser le bloc dans l'inventaire
public static final RegistryObject<Item> MY_BLOCK_ITEM = ITEMS.register(
        "my_block",
        () -> new BlockItem(ModBlocks.MY_BLOCK.get(), new Item.Properties())
);
```

---

### 5. Blockstate JSON

**Fichier :** `src/main/resources/assets/petassegang_addons/blockstates/my_block.json`

```json
{
  "variants": {
    "": { "model": "petassegang_addons:block/my_block" }
  }
}
```

---

### 6. Modèles JSON

**Bloc :** `assets/petassegang_addons/models/block/my_block.json`
```json
{
  "parent": "block/cube_all",
  "textures": {
    "all": "petassegang_addons:block/my_block"
  }
}
```

**Item :** `assets/petassegang_addons/models/item/my_block.json`
```json
{
  "parent": "petassegang_addons:block/my_block"
}
```

---

### 7. Texture bloc

**Fichier :** `assets/petassegang_addons/textures/block/my_block.png` (16x16)

---

### 8. Loot table

**Fichier :** `data/petassegang_addons/loot_tables/blocks/my_block.json`

```json
{
  "type": "minecraft:block",
  "pools": [{
    "rolls": 1,
    "entries": [{
      "type": "minecraft:item",
      "name": "petassegang_addons:my_block"
    }],
    "conditions": [{
      "condition": "minecraft:survives_explosion"
    }]
  }]
}
```

---

### 9. Traductions + creative tab + tests

Même pattern que pour les items.
Ajouter `ModBlocks.MY_BLOCK` et `ModItems.MY_BLOCK_ITEM` dans `docs/BLOCKS.md`.

---

## Checklist finale

- [ ] `block/MyBlock.java` (si custom)
- [ ] `init/ModBlocks.java` — RegistryObject ajouté
- [ ] `init/ModItems.java` — BlockItem ajouté
- [ ] `PetasseGangAddonsMod` — `ModBlocks.register(bus)` appelé
- [ ] `creative/ModCreativeTab.java` — output.accept ajouté
- [ ] `blockstates/my_block.json`
- [ ] `models/block/my_block.json` + `models/item/my_block.json`
- [ ] `textures/block/my_block.png`
- [ ] `loot_tables/blocks/my_block.json`
- [ ] Lang keys EN + FR
- [ ] Tests mis à jour
- [ ] `docs/BLOCKS.md` + `CHANGELOG.md` mis à jour
- [ ] `./gradlew build` + `./gradlew test` passent
