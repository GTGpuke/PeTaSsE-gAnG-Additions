# Architecture — PeTaSsE_gAnG_Additions

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

```
com.petassegang.addons/
│
├── PeTaSsEgAnGAdditionsMod.java      ← @Mod entry-point, lifecycle wiring
│
├── config/
│   └── ModConfig.java             ← ForgeConfigSpec (SERVER + CLIENT)
│
├── creative/
│   └── ModCreativeTab.java        ← Creative tab DeferredRegister
│
├── init/                          ← Registres (un fichier par type)
│   └── ModItems.java              ← DeferredRegister<Item>
│   (futur: ModBlocks, ModEntities, ModSounds, ModDimensions…)
│
├── item/                          ← Classes d'items custom
│   └── GangBadgeItem.java
│
├── block/                         ← (futur) Classes de blocs custom
├── entity/                        ← (futur) Classes d'entités custom
├── world/                         ← (futur) Génération monde / dimensions
├── network/                       ← (futur) Packets réseau
├── client/                        ← (futur) Renderers, GUI — @OnlyIn(CLIENT)
│
└── util/
    └── ModConstants.java          ← MOD_ID, MOD_NAME, LOGGER central
```

---

## Pattern DeferredRegister

Tous les objets Minecraft (items, blocs, entités…) sont enregistrés via
`DeferredRegister` pour garantir que l'enregistrement se fait au bon moment
dans le cycle de vie de Forge.

```java
// 1. Déclarer le registre (static final dans ModItems)
public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, ModConstants.MOD_ID);

// 2. Déclarer les objets
public static final RegistryObject<Item> GANG_BADGE = ITEMS.register(
        "gang_badge", () -> new GangBadgeItem(new Item.Properties()...));

// 3. Connecter à l'event bus (dans le constructeur @Mod)
ModItems.register(modEventBus);  // → appelle ITEMS.register(modEventBus)
```

Flow complet :
```
JVM load → static fields créés (RegistryObject wrappé, pas encore rempli)
         → @Mod constructor → DeferredRegister.register(bus)
         → Forge fire RegistryEvent → RegistryObject rempli
         → FMLCommonSetupEvent → commonSetup()
         → Monde chargé
```

---

## Cycle de vie du mod

```
1. Chargement JVM
   └── static initialisers (DeferredRegister, ForgeConfigSpec)

2. @Mod constructor  [Forge appelle ça]
   ├── register DeferredRegisters to modEventBus
   ├── addListener(commonSetup)
   ├── addListener(clientSetup)  [CLIENT dist seulement]
   └── registerConfig(SERVER, CLIENT)

3. RegistryEvents  [Forge fire automatiquement]
   └── Tous les RegistryObject sont résolus

4. FMLCommonSetupEvent
   └── Logique partagée client/serveur (cross-refs entre registres, etc.)

5. FMLClientSetupEvent  [CLIENT seulement]
   └── Renderers, key bindings, overlay screens

6. Monde chargé / Serveur démarré
```

---

## Séparation client / serveur

| Règle | Détail |
|-------|--------|
| Tout code dans `item/`, `block/`, `init/` | Compatible dedicated server |
| `@OnlyIn(Dist.CLIENT)` | Pour renderers, GUI, particles |
| `FMLEnvironment.dist == Dist.CLIENT` | Guard dans le constructeur avant clientSetup |
| Package `client/` | Tout ce qui est CLIENT-only |
| Jamais de `Minecraft.getInstance()` hors CLIENT | NPE sur serveur dédié |

---

## Conventions de nommage

| Type | Convention | Exemple |
|------|-----------|---------|
| Classe | PascalCase | `GangBadgeItem` |
| Méthode | camelCase | `appendHoverText()` |
| Constante | UPPER_SNAKE_CASE | `MOD_ID`, `GANG_BADGE` |
| Package | lowercase | `com.petassegang.addons.item` |
| Mod ID | lowercase_snake | `petasse_gang_additions` |
| Resource path | lowercase_snake | `gang_badge`, `petasse_gang_additions` |
| Lang key item | `item.<mod_id>.<id>` | `item.petasse_gang_additions.gang_badge` |
| Lang key block | `block.<mod_id>.<id>` | `block.petasse_gang_additions.example_block` |
| Lang key tab | `itemGroup.<mod_id>.<id>` | `itemGroup.petasse_gang_additions.petassegang` |

---

## Flow d'enregistrement — Checklist par type de contenu

### Item
1. `item/MyCustomItem.java` — classe étendant `Item`
2. `init/ModItems.java` — ajouter `RegistryObject<Item> MY_ITEM = ITEMS.register(...)`
3. `creative/ModCreativeTab.java` — `output.accept(ModItems.MY_ITEM.get())`
4. `assets/.../models/item/my_item.json` — modèle
5. `assets/.../textures/item/my_item.png` — texture 16x16
6. `lang/en_us.json` + `fr_fr.json` — traductions
7. `data/.../recipes/my_item.json` — recette (optionnel)
8. `src/test/.../ItemTest.java` — tests de propriétés

### Block
1. `block/MyCustomBlock.java` — classe étendant `Block`
2. `init/ModBlocks.java` — `DeferredRegister<Block>` + `RegistryObject`
3. Item de bloc dans `ModItems.java` — `BlockItem` wrapping the block
4. `assets/.../blockstates/my_block.json`
5. `assets/.../models/block/my_block.json` + `models/item/my_block.json`
6. `assets/.../textures/block/my_block.png`
7. `data/.../loot_tables/blocks/my_block.json`
8. Lang keys + recette

### Entity
1. `entity/MyEntityType.java` — `EntityType.Builder`
2. `init/ModEntities.java` — `DeferredRegister<EntityType<?>>`
3. `client/renderer/MyEntityRenderer.java` — `@OnlyIn(CLIENT)`
4. Registrer renderer dans `clientSetup`
5. Lang key + spawn egg (optionnel)
