# Onboarding — Nouveau prompt Claude Code

Tu démarres sans contexte sur ce projet. Lis les fichiers suivants dans cet ordre exact avant de toucher quoi que ce soit.

---

## Lecture obligatoire

### 1. Vue d'ensemble
- [`README.md`](../../README.md) — Stack, structure du projet, quick start, skills disponibles

### 2. Configuration Gradle
- [`gradle.properties`](../../gradle.properties) — Toutes les versions : MC 1.21.1, Fabric, Java 21, mod_id, group, mod_version
- [`build.gradle`](../../build.gradle) — Config Fabric Loom 1.9, runs, dépendances, compileJava
- [`settings.gradle`](../../settings.gradle) — Déclaration du projet
- [`gradle/wrapper/gradle-wrapper.properties`](../../gradle/wrapper/gradle-wrapper.properties) — Version du wrapper (9.3.0+)

### 3. Identité du mod
- [`src/main/resources/fabric.mod.json`](../../src/main/resources/fabric.mod.json) — Déclaration Fabric, entrypoints, dépendances

### 4. Code source principal
- [`src/main/java/com/petassegang/addons/util/ModConstants.java`](../../src/main/java/com/petassegang/addons/util/ModConstants.java) — MOD_ID, MOD_NAME, LOGGER
- [`src/main/java/com/petassegang/addons/PeTaSsEgAnGAdditionsMod.java`](../../src/main/java/com/petassegang/addons/PeTaSsEgAnGAdditionsMod.java) — ModInitializer, init des registres
- [`src/main/java/com/petassegang/addons/init/ModItems.java`](../../src/main/java/com/petassegang/addons/init/ModItems.java) — Pattern Registry.register() pour les items

### 5. Architecture et conventions
- [`docs/ARCHITECTURE.md`](../ARCHITECTURE.md) — Conventions de code, patterns, stack technique

### 6. Skills (conventions d'ajout de contenu)
Parcourir tous les `SKILL.md` dans [`.skills/`](../../.skills/) :
- `add-item` / `add-block` / `add-entity` / `add-dimension`
- `add-recipe` / `add-sound` / `add-creative-tab` / `add-dependency`

---

## Pièges critiques — mémorise-les avant tout

### Fabric vs Forge
| ❌ Forge / incorrect | ✅ Fabric / correct |
|---------------------|---------------------|
| `DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID)` | `Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "id"), obj)` |
| `RegistryObject<Item> ITEM = ITEMS.register(...)` | `Item ITEM = Registry.register(...)` |
| `ModItems.GANG_BADGE.get()` | `ModItems.GANG_BADGE` (pas de `.get()`) |
| `new Item.Properties().stacksTo(1).setId(...)` | `new Item.Settings().maxCount(1)` (pas de `.setId()`) |
| `Item.Properties` | `Item.Settings` |
| `item.isFoil(stack)` | `item.hasGlint(stack)` |
| `item.getDefaultMaxStackSize()` | `item.getMaxCount()` |
| `finishUsingItem(stack, level, entity)` | `finishUsing(stack, world, entity)` |
| `InteractionResultHolder<ItemStack> use(...)` | `TypedActionResult<ItemStack> use(...)` |
| `Component.translatable("key")` | `Text.translatable("key")` |
| `@OnlyIn(Dist.CLIENT)` | `@Environment(EnvType.CLIENT)` |
| `ModList.get().isLoaded("modid")` | `FabricLoader.getInstance().isModLoaded("modid")` |
| `Identifier.fromNamespaceAndPath(ns, path)` | `Identifier.of(ns, path)` |
| `ResourceLocation` | `Identifier` |
| `ServerLevel` | `ServerWorld` |
| `net.minecraft.world.item.Item` | `net.minecraft.item.Item` (Yarn) |
| `net.minecraft.world.level.block.Block` | `net.minecraft.block.Block` (Yarn) |
| `mods.toml` | `fabric.mod.json` |

### Worldgen JSON
- **`dirt_provider`** est **obligatoire** dans `minecraft:tree` configured_feature — `below_trunk_provider` n'existe pas en 1.21.1 → crash `Failed to load registries` au chargement de monde.

### Encodage Windows
- **`-Dfile.encoding=COMPAT`** est **obligatoire** dans `org.gradle.jvmargs` si le chemin contient `é` (`Développement`). Déjà présent dans `gradle.properties`.

---

## Stack en un coup d'œil

| Composant | Version |
|-----------|---------|
| Minecraft | 1.21.1 |
| Yarn mappings | 1.21.1+build.3 |
| Fabric Loader | 0.16.9 |
| Fabric API | 0.102.0+1.21.1 |
| Fabric Loom | 1.9 |
| Gradle | 9.3.0+ |
| Java | 21 (Temurin recommandé) |
| JUnit | 5.10.3 |
| mod_id | `petasse_gang_additions` |
| package racine | `com.petassegang.addons` |

---

## Commandes utiles

```bash
./gradlew build                               # Build → build/libs/petasse_gang_additions-0.6.0.jar
./gradlew test                                # Tests JUnit 5
./gradlew runClient                           # Client dev
./gradlew runServer                           # Serveur dédié
./gradlew benchmarkLevelZeroGeneration        # Benchmark Level 0
./gradlew clean build                         # Rebuild propre
./gradlew --stop && ./gradlew build           # Stop daemon + rebuild (problèmes encodage)
```

---

## Si le build échoue

Voir [`docs/TROUBLESHOOTING.md`](../TROUBLESHOOTING.md).
