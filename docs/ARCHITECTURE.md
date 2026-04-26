# Architecture — PeTaSsE_gAnG_Additions

## Stack technique

| Couche | Technologie |
|--------|-------------|
| Jeu | Minecraft 1.21.1 (Yarn mappings 1.21.1+build.3) |
| Mod loader | Fabric Loader 0.16.9 |
| API | Fabric API 0.102.0+1.21.1 |
| Langage | Java 21 |
| Build | Gradle 9.3.0 + Fabric Loom 1.9 |
| Tests | JUnit 5 |
| CI/CD | GitHub Actions |

### Dépendances prévues (pas encore activées)

Ces bibliothèques sont documentées dans [docs/DEPENDENCIES.md](DEPENDENCIES.md) et déclarées
en `suggests` dans `fabric.mod.json`, mais pas encore incluses dans le build.

| Bibliothèque | Rôle |
|-------------|------|
| GeckoLib | Animations 3D (mobs Backrooms) |
| Patchouli | Livre/guide in-game (lore Backrooms) |

---

## Arborescence des packages

```text
com.petassegang.addons/
|
|-- PeTaSsEgAnGAdditionsMod.java        <- ModInitializer — point d'entrée principal
|-- PeTaSsEgAnGAdditionsClientMod.java  <- ClientModInitializer — entrée client uniquement
|
|-- config/
|   `-- ModConfig.java                  <- Constantes de configuration (plain booleans)
|
|-- creative/
|   `-- ModCreativeTab.java             <- Onglet créatif (ItemGroup + FabricItemGroupEvents)
|
|-- init/                               <- Registres (un fichier par type)
|   |-- ModBlockEntities.java           <- Registry.register(Registries.BLOCK_ENTITY_TYPE, ...)
|   |-- ModBlocks.java                  <- Registry.register(Registries.BLOCK, ...)
|   |-- ModChunkGenerators.java         <- Registry.register(Registries.CHUNK_GENERATOR, ...)
|   `-- ModItems.java                   <- Registry.register(Registries.ITEM, ...)
|
|-- item/                               <- Classes d'items custom
|   |-- CursedSnackItem.java
|   `-- GangBadgeItem.java
|
|-- block/                              <- Classes de blocs custom
|   |-- LevelZeroWallpaperBlock.java    <- Bloc technique adaptatif (BlockEntity)
|   `-- entity/
|       `-- LevelZeroWallpaperBlockEntity.java <- BlockEntity pour le faceMask adaptatif
|
|-- network/                            <- Packets réseau
|   |-- ModNetworking.java
|   `-- packet/
|       `-- GangBadgeActivatePayload.java
|
|-- world/
|   `-- backrooms/
|       |-- BackroomsConstants.java      <- IDs et hauteurs du Level 0
|       `-- level0/
|           |-- LevelZeroChunkGenerator.java  <- Génération monocouche
|           |-- LevelZeroLayout.java          <- Traduction déterministe du script Python
|           `-- LevelZeroSurfaceBiome.java    <- Biomes cosmétiques internes (BASE, RED)
|
|-- client/                             <- Handlers, renderers, GUI (CLIENT uniquement)
|   |-- handler/
|   |   `-- GangBadgeClientHandler.java
|   `-- model/
|       |-- LevelZeroWallpaperBlockStateModel.java
|       |-- LevelZeroWallpaperBakedModel.java
|       `-- LevelZeroWallpaperModelHandler.java
|
`-- util/
    `-- ModConstants.java               <- MOD_ID, MOD_NAME, LOGGER central
```

---

## Pattern d'enregistrement Fabric

En Fabric, tous les objets sont enregistrés via `Registry.register()` dans des champs `static final`.
Il n'y a pas de `DeferredRegister` ni de `RegistryObject`.

```java
// ModItems.java
public static final Item GANG_BADGE = Registry.register(
        Registries.ITEM,
        Identifier.of(ModConstants.MOD_ID, "gang_badge"),
        new GangBadgeItem(new Item.Settings().maxCount(1).rarity(Rarity.EPIC))
);
```

Le champ contient directement l'objet enregistré — pas de `.get()` nécessaire.

Flow complet :

```text
JVM load -> static fields exécutés (Registration immédiate)
         -> ModInitializer.onInitialize() -> setup réseau, creative tabs
         -> Monde chargé
```

---

## Cycle de vie du mod

```text
1. Chargement JVM
   `-- static initialisers (tous les Registry.register() s'exécutent ici)

2. ModInitializer.onInitialize()
   |-- setup réseau (ModNetworking.register())
   `-- enregistrement onglet créatif

3. ClientModInitializer.onInitializeClient()  [CLIENT seulement]
   `-- renderers, key bindings, model handlers

4. Monde chargé / Serveur démarré
```

---

## Séparation client / serveur

| Règle | Détail |
|-------|--------|
| Tout code dans `item/`, `block/`, `init/` | Compatible dedicated server |
| `@Environment(EnvType.CLIENT)` | Pour renderers, GUI, particles |
| Package `client/` | Tout ce qui est client-only |
| Jamais de `MinecraftClient.getInstance()` hors CLIENT | Évite les crashs serveur |
| `ClientModInitializer` séparé | Point d'entrée client dédié |

---

## Conventions de nommage

| Type | Convention | Exemple |
|------|------------|---------|
| Classe | PascalCase | `GangBadgeItem` |
| Méthode | camelCase | `appendTooltip()` |
| Constante | UPPER_SNAKE_CASE | `MOD_ID`, `GANG_BADGE` |
| Package | lowercase | `com.petassegang.addons.item` |
| Mod ID | lowercase_snake | `petasse_gang_additions` |
| Resource path | lowercase_snake | `gang_badge` |
| Lang key item | `item.<mod_id>.<id>` | `item.petasse_gang_additions.gang_badge` |
| Lang key block | `block.<mod_id>.<id>` | `block.petasse_gang_additions.level_zero_wallpaper` |
| Lang key tab | `itemGroup.<mod_id>.<id>` | `itemGroup.petasse_gang_additions.petassegang` |

---

## Flow d'ajout de contenu

### Item
1. `item/MyCustomItem.java`
2. `init/ModItems.java` — `Registry.register(Registries.ITEM, ...)`
3. `creative/ModCreativeTab.java` — `FabricItemGroupEvents.modifyEntriesEvent()`
4. `assets/.../models/item/my_item.json`
5. `assets/.../textures/item/my_item.png`
6. `lang/en_us.json` + `fr_fr.json`
7. `data/.../recipes/my_item.json` si nécessaire
8. `src/test/.../ItemTest.java`

### Block
1. `block/MyCustomBlock.java`
2. `init/ModBlocks.java` — `Registry.register(Registries.BLOCK, ...)`
3. `init/ModItems.java` — `BlockItem` pour le block item
4. `assets/.../blockstates/my_block.json`
5. `assets/.../models/block/my_block.json` + `models/item/my_block.json`
6. `assets/.../textures/block/my_block.png`
7. `data/.../loot_table/blocks/my_block.json`
8. Lang keys + recette
