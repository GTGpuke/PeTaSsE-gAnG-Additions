# Architecture - PeTaSsE_gAnG_Additions

## Stack technique

| Couche | Technologie |
|--------|-------------|
| Jeu | Minecraft 26.1 (fully deobfuscated) |
| Mod loader | Forge 62.0.x |
| Langage | Java 25 |
| Build | Gradle 9.3.0 + ForgeGradle 7.x |
| Tests | JUnit 5 + Forge GameTest |
| CI/CD | GitHub Actions |

---

## Arborescence des packages

```text
com.petassegang.addons/
|
|-- PeTaSsEgAnGAdditionsMod.java      <- @Mod entry-point, lifecycle wiring
|
|-- config/
|   `-- ModConfig.java                <- ForgeConfigSpec (SERVER + CLIENT)
|
|-- creative/
|   `-- ModCreativeTab.java           <- Creative tab DeferredRegister
|
|-- init/                             <- Registres (un fichier par type)
|   |-- ModBlocks.java                <- DeferredRegister<Block>
|   |-- ModChunkGenerators.java       <- DeferredRegister<MapCodec<? extends ChunkGenerator>>
|   `-- ModItems.java                 <- DeferredRegister<Item>
|
|-- item/                             <- Classes d'items custom
|   |-- CursedSnackItem.java
|   `-- GangBadgeItem.java
|
|-- block/                            <- Classes de blocs custom si necessaire
|-- entity/                           <- Futur contenu d'entites custom
|-- network/                          <- Packets reseau
|-- world/
|   `-- backrooms/
|       |-- BackroomsConstants.java   <- IDs et hauteurs du Level 0
|       `-- level0/
|           |-- LevelZeroChunkGenerator.java <- Generation monocouche
|           `-- LevelZeroLayout.java         <- Traduction deterministe du script Python
|-- client/                           <- Handlers, renderers, GUI
|
`-- util/
    `-- ModConstants.java             <- MOD_ID, MOD_NAME, LOGGER central
```

---

## Pattern DeferredRegister

Tous les objets Minecraft sont enregistres via `DeferredRegister` pour garantir
que l'enregistrement se fait au bon moment dans le cycle de vie de Forge.

```java
public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, ModConstants.MOD_ID);

public static final RegistryObject<Item> GANG_BADGE = ITEMS.register(
        "gang_badge", () -> new GangBadgeItem(new Item.Properties()...));

ModItems.register(modEventBus);
```

Flow complet :

```text
JVM load -> static fields crees
         -> @Mod constructor -> DeferredRegister.register(bus)
         -> Forge fire RegistryEvent -> RegistryObject rempli
         -> FMLCommonSetupEvent -> commonSetup()
         -> Monde charge
```

---

## Cycle de vie du mod

```text
1. Chargement JVM
   `-- static initialisers (DeferredRegister, ForgeConfigSpec)

2. @Mod constructor
   |-- register DeferredRegisters to modEventBus
   |-- addListener(commonSetup)
   |-- addListener(clientSetup) [CLIENT seulement]
   `-- registerConfig(SERVER, CLIENT)

3. RegistryEvents
   `-- Tous les RegistryObject sont resolus

4. FMLCommonSetupEvent
   `-- Logique partagee client/serveur

5. FMLClientSetupEvent [CLIENT seulement]
   `-- Renderers, key bindings, overlays

6. Monde charge / Serveur demarre
```

---

## Separation client / serveur

| Regle | Detail |
|-------|--------|
| Tout code dans `item/`, `block/`, `init/` | Compatible dedicated server |
| `@OnlyIn(Dist.CLIENT)` | Pour renderers, GUI, particles |
| `FMLEnvironment.dist == Dist.CLIENT` | Guard dans le constructeur avant clientSetup |
| Package `client/` | Tout ce qui est client-only |
| Jamais de `Minecraft.getInstance()` hors CLIENT | Evite les crashs serveur |

---

## Conventions de nommage

| Type | Convention | Exemple |
|------|------------|---------|
| Classe | PascalCase | `GangBadgeItem` |
| Methode | camelCase | `appendHoverText()` |
| Constante | UPPER_SNAKE_CASE | `MOD_ID`, `GANG_BADGE` |
| Package | lowercase | `com.petassegang.addons.item` |
| Mod ID | lowercase_snake | `petasse_gang_additions` |
| Resource path | lowercase_snake | `gang_badge` |
| Lang key item | `item.<mod_id>.<id>` | `item.petasse_gang_additions.gang_badge` |
| Lang key block | `block.<mod_id>.<id>` | `block.petasse_gang_additions.example_block` |
| Lang key tab | `itemGroup.<mod_id>.<id>` | `itemGroup.petasse_gang_additions.petassegang` |

---

## Flow d'enregistrement

### Item
1. `item/MyCustomItem.java`
2. `init/ModItems.java`
3. `creative/ModCreativeTab.java`
4. `assets/.../models/item/my_item.json`
5. `assets/.../textures/item/my_item.png`
6. `lang/en_us.json` + `fr_fr.json`
7. `data/.../recipes/my_item.json` si necessaire
8. `src/test/.../ItemTest.java`

### Block
1. `block/MyCustomBlock.java`
2. `init/ModBlocks.java`
3. Item de bloc dans `ModItems.java`
4. `assets/.../blockstates/my_block.json`
5. `assets/.../models/block/my_block.json` + `models/item/my_block.json`
6. `assets/.../textures/block/my_block.png`
7. `data/.../loot_table/blocks/my_block.json`
8. Lang keys + recette
