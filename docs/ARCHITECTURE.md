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
|-- core/
|   `-- ModConstants.java               <- MOD_ID, MOD_NAME, LOGGER central
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
|-- feature/                            <- Features gameplay rangees verticalement
|   |-- gang/
|   |   |-- item/gang_badge/GangBadgeItem.java
|   |   |-- network/c2s/GangBadgeActivatePayload.java
|   |   `-- client/GangBadgeClientHandler.java
|   `-- cursed/
|       `-- item/cursed_snack/CursedSnackItem.java
|
|-- backrooms/                          <- Feature Backrooms
|   |-- BackroomsConstants.java         <- IDs et hauteurs du Level 0
|   `-- level/level0/
|       |-- biome/LevelZeroSurfaceBiome.java
|       |-- block/                      <- Blocs propres au Level 0
|       |-- client/model/               <- Model handlers client du Level 0
|       `-- generation/
|           |-- LevelZeroChunkGenerator.java
|           |-- layout/LevelZeroLayout.java
|           |-- stage/
|           |-- write/
|           `-- noise/
|
|-- network/                            <- Packets réseau
|   `-- ModNetworking.java
|
`-- perf/section/                       <- Monitoring debug opt-in
    |-- ModPerformanceMonitor.java
    `-- client/ClientPerformanceMonitorHook.java
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
| Tout code dans `feature/*`, `backrooms/*`, `init/` hors sous-dossier client | Compatible dedicated server |
| `@Environment(EnvType.CLIENT)` | Pour renderers, GUI, particles |
| Dossiers `client/` sous une feature | Tout ce qui est client-only pour cette feature |
| Jamais de `MinecraftClient.getInstance()` hors CLIENT | Évite les crashs serveur |
| `ClientModInitializer` séparé | Point d'entrée client dédié |

---

## Conventions de nommage

| Type | Convention | Exemple |
|------|------------|---------|
| Classe | PascalCase | `GangBadgeItem` |
| Méthode | camelCase | `appendTooltip()` |
| Constante | UPPER_SNAKE_CASE | `MOD_ID`, `GANG_BADGE` |
| Package | lowercase | `com.petassegang.addons.feature.gang.item.gang_badge` |
| Mod ID | lowercase_snake | `petasse_gang_additions` |
| Resource path | lowercase_snake | `gang_badge` |
| Lang key item | `item.<mod_id>.<id>` | `item.petasse_gang_additions.gang_badge` |
| Lang key block | `block.<mod_id>.<id>` | `block.petasse_gang_additions.level_zero_wallpaper` |
| Lang key tab | `itemGroup.<mod_id>.<id>` | `itemGroup.petasse_gang_additions.petassegang` |

---

## Flow d'ajout de contenu

### Item
1. `feature/<feature>/item/<item_id>/MyCustomItem.java`
2. `init/ModItems.java` — `Registry.register(Registries.ITEM, ...)`
3. `creative/ModCreativeTab.java` — `FabricItemGroupEvents.modifyEntriesEvent()`
4. `assets/.../models/item/my_item.json`
5. `assets/.../textures/item/my_item.png`
6. `lang/en_us.json` + `fr_fr.json`
7. `data/.../recipes/my_item.json` si nécessaire
8. `src/test/.../ItemTest.java`

### Block
1. `feature/<feature>/block/<block_id>/MyCustomBlock.java`
2. `init/ModBlocks.java` — `Registry.register(Registries.BLOCK, ...)`
3. `init/ModItems.java` — `BlockItem` pour le block item
4. `assets/.../blockstates/my_block.json`
5. `assets/.../models/block/my_block.json` + `models/item/my_block.json`
6. `assets/.../textures/block/my_block.png`
7. `data/.../loot_table/blocks/my_block.json`
8. Lang keys + recette
