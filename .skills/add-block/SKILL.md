---
name: add-block
description: "Ajouter un bloc au mod PeTaSsE_gAnG_Additions (Fabric 1.21.1). Déclenche pour 'bloc', 'minerai', 'dalle', 'mur', 'escalier', 'porte', 'bloc décoratif', 'bloc fonctionnel', 'ore' ou 'block'."
---

# Skill — Ajouter un Bloc (Fabric 1.21.1)

## Quand utiliser ce skill

- "Ajoute un bloc [nom]"
- "Crée un minerai [nom]"
- "Nouveau bloc décoratif [description]"

## Étapes

### 1. Définir les paramètres

| Paramètre | Valeur |
|-----------|--------|
| `BLOCK_ID` | `gangite_ore`, `petasse_block` |
| `ClassName` | `GangiteOreBlock`, `PetasseBlock` |
| Hardness | ex. `3.0f` |
| Blast resistance | ex. `3.0f` |
| Son | `BlockSoundGroup.STONE`, `WOOD`, `METAL`, `GLASS` |
| Drops | lui-même, silk touch, fortune, loot table |

### 2. Créer la classe bloc si un comportement custom est nécessaire

**Fichier :** `src/main/java/com/petassegang/addons/feature/my_feature/block/my_block/MyBlock.java`

```java
package com.petassegang.addons.feature.my_feature.block.my_block;

import net.minecraft.block.Block;
import net.minecraft.block.AbstractBlock;

/**
 * Bloc personnalisé.
 */
public final class MyBlock extends Block {

    public MyBlock(AbstractBlock.Settings settings) {
        super(settings);
    }
}
```

Pour un bloc simple, utiliser directement `Block` dans `ModBlocks`.

**Rangement v4 :**
- Bloc lie a une feature : `feature/<feature>/block/<block_id>/`.
- Bloc Backrooms Level 0 : `backrooms/level/level0/block/`.
- Eviter de recreer un package racine `block/`.

### 3. Enregistrer le bloc dans `ModBlocks.java`

```java
public static final Block MY_BLOCK = Registry.register(
        Registries.BLOCK,
        Identifier.of(ModConstants.MOD_ID, "my_block"),
        new Block(AbstractBlock.Settings.create()
                .hardness(3.0f)
                .resistance(9.0f)
                .sounds(BlockSoundGroup.STONE))
);
```

**Pas de `.setId()`** — Fabric n'en a pas besoin.

### 4. Ajouter le `BlockItem` dans `ModItems.java`

```java
public static final BlockItem MY_BLOCK = Registry.register(
        Registries.ITEM,
        Identifier.of(ModConstants.MOD_ID, "my_block"),
        new BlockItem(ModBlocks.MY_BLOCK, new Item.Settings())
);
```

### 5. Créer les ressources

- `assets/<modid>/blockstates/my_block.json`
- `assets/<modid>/models/block/my_block.json`
- `assets/<modid>/items/my_block.json`
- `assets/<modid>/textures/block/my_block.png`
- `data/<modid>/loot_table/blocks/my_block.json`

### 6. Résolution des textures

- `16×16` pour les blocs standards.
- `32×32` autorisé pour les blocs du Level 0.

### 7. Finaliser

- Ajouter les clés EN et FR.
- Ajouter l'item dans l'onglet créatif si nécessaire (`FabricItemGroupEvents.modifyEntriesEvent()`).
- Mettre à jour `docs/BLOCKS.md` et `docs/CHANGELOG.md`.
- Ajouter ou ajuster les tests de registre.

## Checklist finale

- [ ] Bloc enregistré dans `ModBlocks.java` via `Registry.register(Registries.BLOCK, ...)`
- [ ] BlockItem enregistré dans `ModItems.java` via `Registry.register(Registries.ITEM, ...)`
- [ ] Onglet créatif mis à jour si nécessaire
- [ ] Blockstate, modèle, item model, texture et loot table présents
- [ ] Fichier `items/my_block.json` présent (obligatoire MC 1.21.1)
- [ ] Traductions EN et FR présentes
- [ ] `docs/BLOCKS.md` et `docs/CHANGELOG.md` mis à jour
- [ ] `./gradlew build` passe
