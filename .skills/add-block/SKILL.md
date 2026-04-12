---
name: add-block
description: "Ajouter un bloc au mod PeTaSsE_gAnG_Additions. Declenche pour 'bloc', 'minerai', 'dalle', 'mur', 'escalier', 'porte', 'bloc decoratif', 'bloc fonctionnel', 'ore' ou 'block'."
---

# Skill - Ajouter un Bloc

## Quand utiliser ce skill

- "Ajoute un bloc [nom]"
- "Cree un minerai [nom]"
- "Nouveau bloc decoratif [description]"

## Etapes

### 1. Definir les parametres

| Parametre | Valeur |
|-----------|--------|
| `BLOCK_ID` | `gangite_ore`, `petasse_block` |
| `ClassName` | `GangiteOreBlock`, `PetasseBlock` |
| Hardness | ex. `3.0f` |
| Blast resistance | ex. `3.0f` |
| Son | `SoundType.STONE`, `WOOD`, `METAL`, `GLASS` |
| Drops | lui-meme, silk touch, fortune, loot table |

### 2. Creer la classe bloc si un comportement custom est necessaire

**Fichier :** `src/main/java/com/petassegang/addons/block/MyBlock.java`

```java
package com.petassegang.addons.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Bloc personnalise.
 */
public final class MyBlock extends Block {

    public MyBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }
}
```

Pour un bloc simple, utiliser directement `Block` dans `ModBlocks`.

### 3. Enregistrer le bloc dans `ModBlocks.java`

```java
public static final RegistryObject<Block> MY_BLOCK = BLOCKS.register(
        "my_block",
        () -> new Block(BlockBehaviour.Properties
                .ofFullCopy(Blocks.STONE)
                .setId(BLOCKS.key("my_block")))
);
```

### 4. Ajouter le `BlockItem` dans `ModItems.java`

```java
public static final RegistryObject<BlockItem> MY_BLOCK = ITEMS.register(
        "my_block",
        () -> new BlockItem(ModBlocks.MY_BLOCK.get(),
                new Item.Properties().setId(ITEMS.key("my_block")))
);
```

### 5. Creer les ressources

- `assets/<modid>/blockstates/my_block.json`
- `assets/<modid>/models/block/my_block.json`
- `assets/<modid>/items/my_block.json`
- `assets/<modid>/textures/block/my_block.png`
- `data/<modid>/loot_table/blocks/my_block.json`

### 6. Resolution des textures

- `16x16` pour les blocs standards.
- `32x32` autorise pour les blocs du Level 0.

### 7. Finaliser

- Ajouter les cles EN et FR.
- Ajouter l'item dans l'onglet creatif si necessaire.
- Mettre a jour `docs/BLOCKS.md` et `docs/CHANGELOG.md`.
- Ajouter ou ajuster les tests de registre.

## Checklist finale

- [ ] Bloc enregistre dans `ModBlocks.java`
- [ ] BlockItem enregistre dans `ModItems.java`
- [ ] Onglet creatif mis a jour si necessaire
- [ ] Blockstate, modele, item model, texture et loot table presents
- [ ] Traductions EN et FR presentes
- [ ] `docs/BLOCKS.md` et `docs/CHANGELOG.md` mis a jour
- [ ] `./gradlew build` passe
