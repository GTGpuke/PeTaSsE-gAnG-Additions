---
name: project-conventions
description: "Rappelle les conventions du projet PeTaSsE_gAnG_Additions (Fabric 1.21.1) : mod id, structure des packages, nommage, patterns Registry.register(), imports standards, séparation client/serveur et règles qualité. Déclenche pour 'conventions', 'structure', 'règles', 'comment ajouter', 'architecture' ou 'comment organiser'."
---

# Conventions du projet PeTaSsE_gAnG_Additions (Fabric 1.21.1)

## Identifiants clés

| Constante | Valeur |
|-----------|--------|
| MOD_ID | `petasse_gang_additions` |
| Package racine | `com.petassegang.addons` |
| Version mod | `0.6.0` |
| Version MC | `1.21.1` |
| Fabric Loader | `0.16.9` |
| Fabric API | `0.102.0+1.21.1` |
| Java | `21` |

## Structure des packages

```text
com.petassegang.addons/
|- PeTaSsEgAnGAdditionsMod.java       ← ModInitializer (entrypoint principal)
|- PeTaSsEgAnGAdditionsClientMod.java ← ClientModInitializer (entrypoint client)
|- backrooms/
|  |- BackroomsConstants.java
|  `- level/level0/
|     |- biome/
|     |- block/
|     |- client/model/
|     `- generation/
|        |- LevelZeroChunkGenerator.java
|        |- coord/
|        |- layout/
|        |  `- sector/
|        |- noise/
|        |- stage/
|        |  |- biome/
|        |  |- geometry/
|        |  |- light/
|        |  |- region/
|        |  `- topology/
|        `- write/
|           |- profiling/
|           `- structure/
|- config/
|- core/ModConstants.java
|- creative/ModCreativeTab.java
|- feature/
|  |- cursed/item/cursed_snack/
|  `- gang/
|     |- client/
|     |- item/gang_badge/
|     `- network/c2s/
|- init/
|  |- ModBlockEntities.java
|  |- ModBlocks.java
|  |- ModChunkGenerators.java
|  `- ModItems.java
|- network/
`- perf/section/
   `- client/
```

Pour le Level 0, l'arborescence ci-dessus doit rester lisible et refléter la séparation
actuelle `coord / layout / noise / stage / write / debug`.

## Conventions de nommage

| Type | Convention | Exemple |
|------|-----------|---------|
| Classe Java | PascalCase | `GangBadgeItem` |
| Méthode | camelCase | `appendTooltip` |
| Constante | UPPER_SNAKE_CASE | `GANG_BADGE` |
| Resource ID | lowercase_snake | `gang_badge` |
| Lang key item | `item.<mod_id>.<id>` | `item.petasse_gang_additions.gang_badge` |
| Lang key block | `block.<mod_id>.<id>` | `block.petasse_gang_additions.level_zero_wallpaper` |
| Lang key tab | `itemGroup.<mod_id>.<id>` | `itemGroup.petasse_gang_additions.petassegang` |

## Pattern d'enregistrement Fabric

```java
// Dans ModItems.java — pas de DeferredRegister, pas de RegistryObject :
public static final Item GANG_BADGE = Registry.register(
        Registries.ITEM,
        Identifier.of(ModConstants.MOD_ID, "gang_badge"),
        new GangBadgeItem(new Item.Settings().maxCount(1).rarity(Rarity.EPIC))
);

// Le champ contient directement l'objet — pas de .get() :
ModItems.GANG_BADGE  // ✅
ModItems.GANG_BADGE.get()  // ❌ n'existe pas
```

## API Fabric vs Forge — correspondances

| Forge | Fabric / Yarn |
|-------|---------------|
| `Item.Properties` | `Item.Settings` |
| `.stacksTo(n)` | `.maxCount(n)` |
| `.setId(key)` | (supprimé) |
| `isFoil(stack)` | `hasGlint(stack)` |
| `getDefaultMaxStackSize()` | `getMaxCount()` |
| `finishUsingItem()` | `finishUsing()` |
| `Component.translatable()` | `Text.translatable()` |
| `ResourceLocation` | `Identifier` |
| `Identifier.fromNamespaceAndPath()` | `Identifier.of()` |
| `@OnlyIn(Dist.CLIENT)` | `@Environment(EnvType.CLIENT)` |
| `ModList.get().isLoaded()` | `FabricLoader.getInstance().isModLoaded()` |
| `net.minecraft.world.item.Item` | `net.minecraft.item.Item` |
| `net.minecraft.world.level.block.Block` | `net.minecraft.block.Block` |
| `ServerLevel` | `ServerWorld` |
| `InteractionResultHolder` | `TypedActionResult` |

## Imports standards

Ordre attendu :
1. `java`
2. `javax`
3. `net.minecraft`
4. `net.fabricmc`
5. `com.petassegang`

## Règles qualité

- Zéro import wildcard.
- Zéro `System.out` et `printStackTrace`.
- Commentaires, logs et messages d'erreur en français, avec majuscule et point.
- Code client uniquement dans `client/` ou sous `@Environment(EnvType.CLIENT)`.
- Utiliser `Text.translatable(...)` pour tout texte visible en jeu.
- Les batches touchant `backrooms/level/level0/` doivent relire :
  `docs/backrooms/backrooms-level0-roadmap.md`,
  `docs/backrooms/backrooms-level0-pipeline-v6.md`
  et `docs/backrooms/to-check/TO CHECK.md`.
- Toute réorganisation interne du Level 0 doit être journalisée dans `docs/audit/CLEANUP_LOG.md`.
- Le monitor de performance reste un outil debug opt-in ; ne jamais le traiter comme
  une feature gameplay.
- Les textures de blocs du Level 0 suivent la convention locale `32×32`.
- Les loot tables vivent dans `data/<modid>/loot_table/blocks/`.
- Les murs du Level 0 sont pensés comme fixes et indestructibles en survie.
- `level_zero_wallpaper_adaptive` est réservé aux transitions mixtes ; les murs simples restent en blocs simples.
- Le cœur non exposé des murs du Level 0 reste en `minecraft:bedrock`.
- `dirt_provider` (pas `below_trunk_provider`) dans `minecraft:tree` configured_feature.

## Mise à jour obligatoire après chaque ajout

- `lang/en_us.json`
- `lang/fr_fr.json`
- `docs/CHANGELOG.md`
- La doc métier adaptée : `docs/BLOCKS.md`, `docs/ITEMS.md`, `docs/DIMENSIONS.md`, etc.
- Le test associé dans `src/test/`
