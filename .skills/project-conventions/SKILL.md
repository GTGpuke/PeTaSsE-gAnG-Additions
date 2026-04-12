---
name: project-conventions
description: "Rappelle les conventions du projet PeTaSsE_gAnG_Additions : mod id, structure des packages, nommage, patterns DeferredRegister, imports standards, separation client/serveur et regles qualite. Declenche pour 'conventions', 'structure', 'regles', 'comment ajouter', 'architecture' ou 'comment organiser'."
---

# Conventions du projet PeTaSsE_gAnG_Additions

## Identifiants cles

| Constante | Valeur |
|-----------|--------|
| MOD_ID | `petasse_gang_additions` |
| Package racine | `com.petassegang.addons` |
| Version mod | `0.5.0` |
| Version MC | `26.1` |
| Version Forge | `62.0.x` |
| Java | `25` |

## Structure des packages

```text
com.petassegang.addons/
|- PeTaSsEgAnGAdditionsMod.java
|- block/
|- client/
|- config/
|- creative/ModCreativeTab.java
|- init/
|  |- ModBlockEntities.java
|  |- ModBlocks.java
|  |- ModChunkGenerators.java
|  `- ModItems.java
|- item/
|- network/
|- util/ModConstants.java
`- world/backrooms/
   |- BackroomsConstants.java
   `- level0/
      |- LevelZeroChunkGenerator.java
      |- LevelZeroLayout.java
      `- LevelZeroSurfaceBiome.java
```

## Conventions de nommage

| Type | Convention | Exemple |
|------|-----------|---------|
| Classe Java | PascalCase | `GangBadgeItem` |
| Methode | camelCase | `appendHoverText` |
| Constante | UPPER_SNAKE_CASE | `GANG_BADGE` |
| Resource ID | lowercase_snake | `gang_badge` |
| Lang key item | `item.<mod_id>.<id>` | `item.petasse_gang_additions.gang_badge` |
| Lang key block | `block.<mod_id>.<id>` | `block.petasse_gang_additions.level_zero_wallpaper` |
| Lang key tab | `itemGroup.<mod_id>.<id>` | `itemGroup.petasse_gang_additions.petassegang` |

## Pattern DeferredRegister

```java
public static final DeferredRegister<Block> BLOCKS =
        DeferredRegister.create(ForgeRegistries.BLOCKS, ModConstants.MOD_ID);

public static final RegistryObject<Block> MY_BLOCK = BLOCKS.register(
        "my_block",
        () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE))
);

public static void register(BusGroup modBusGroup) {
    BLOCKS.register(modBusGroup);
}
```

## Imports standards

Ordre attendu :
1. `java`
2. `javax`
3. `net.minecraft`
4. `net.minecraftforge`
5. `com.petassegang`

## Regles qualite

- Zero import wildcard.
- Zero `System.out` et `printStackTrace`.
- Commentaires, logs et messages d'erreur en francais, avec majuscule et point.
- Code client uniquement dans `client/` ou sous garde explicite de dist.
- Utiliser `Component.translatable(...)` pour tout texte visible en jeu.
- Les textures de blocs du Level 0 suivent la convention locale `32x32`.
- Les loot tables MC 26.1 vivent dans `data/<modid>/loot_table/blocks/`.

## Mise a jour obligatoire apres chaque ajout

- `lang/en_us.json`
- `lang/fr_fr.json`
- `docs/CHANGELOG.md`
- La doc metier adaptee : `docs/BLOCKS.md`, `docs/ITEMS.md`, `docs/DIMENSIONS.md`, etc.
- Le test associe dans `src/test/`
