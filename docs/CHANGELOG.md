# Changelog

All notable changes to PeTaSsE_gAnG_Additions are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).
Versioning follows [Semantic Versioning](https://semver.org/).

---

## [Unreleased]

### Changed
- **Architecture v4** : alignement de la documentation publique et des skills sur la nouvelle arborescence.
  - `ModConstants` est documente dans `core/`.
  - Les contenus Gang et Cursed sont documentes sous `feature/`.
  - Le Level 0 est documente sous `backrooms/level/level0/`.
  - Le monitoring de performance debug est documente sous `perf/section/`.

---

## [0.7.0] - 2026-04-21

### Changed
- **Backrooms Level 0** : la generation est maintenant structuree autour d'une pile verticale multi-layer deterministe.
  - Introduction de `LevelZeroLayerStackLayout` et `LevelZeroVerticalSlice` pour cadrer les layers sans modifier la logique historique.
  - `LevelZeroChunkGenerator` orchestre des slices seedees par layer, tout en deleguant toujours la topologie au coeur legacy.
  - `LevelZeroLayout` documente explicitement la hierarchie secteur -> region -> chunk et le statut "legacy" des regles a ne pas casser.
- **Pipeline Level 0** : refactor de l'arborescence interne pour separer plus clairement `coord`, `layout`, `stage`, `write`, `debug` et `noise`.
- **Lumiere Level 0** : la trame active repose maintenant sur un motif global simple et stable, puis sur un filtrage par biome.
  - Le biome standard conserve la densite maximale du motif.
  - Les autres biomes peuvent attenuer cette trame via une densite dediee, sans recreer une grille differente.
  - Certaines grandes pieces peuvent devenir entierement sombres de maniere tres rare et deterministe.
- **Micro-geometrie Level 0** : les demi-murs et alcoves critiques restent maintenant alignes sur les directions semantiques attendues.
- **Writer Level 0** : ajout d'etapes dediees pour les neons, les details muraux et les structures debug, avec constructeurs injectables pour les tests.
- **Performance debug** : ajout d'un monitoring de performance cote client et serveur pour observer les hotspots pendant les sessions de test.
- **Tests** : extension importante de la couverture JUnit du Level 0 et stabilisation des tests JVM purs pour l'audit pre-push.

---

## [0.6.0] - 2026-04-15

### Changed
- **Migration Forge → Fabric** : réécriture complète du projet de Forge 62.0.x / MC 26.1 / Java 25
  vers Fabric Loader 0.16.9 / Minecraft 1.21.1 / Java 21.
  - `build.gradle` et `gradle.properties` migrés vers Fabric Loom 1.9.
  - `mods.toml` remplacé par `fabric.mod.json`.
  - `DeferredRegister` + `RegistryObject` remplacés par `Registry.register()` dans des champs `static final`.
  - `Item.Properties` → `Item.Settings`, `.stacksTo()` → `.maxCount()`, suppression de `.setId()`.
  - `ForgeConfigSpec` → constantes `static final` simples dans `ModConfig`.
  - `Component.translatable()` → `Text.translatable()`.
  - `isFoil()` → `hasGlint()`, `getDefaultMaxStackSize()` → `getMaxCount()`.
  - `finishUsingItem()` → `finishUsing()`.
  - Réseau Forge `SimpleChannel` → `ServerPlayNetworking` / `ClientPlayNetworking` Fabric.
  - `@OnlyIn(Dist.CLIENT)` → `@Environment(EnvType.CLIENT)`.
  - `ModList.get().isLoaded()` → `FabricLoader.getInstance().isModLoaded()`.
  - Imports Yarn mappings (`net.minecraft.item`, `net.minecraft.block`, etc.).
- **Tests** : tous les fichiers de test réécrits pour l'API Fabric/Yarn. `compileTestJava` passe.
- **Encodage Windows** : ajout de `-Dfile.encoding=COMPAT` dans `org.gradle.jvmargs` pour corriger
  le `ClassNotFoundException` causé par la corruption du chemin accentué (`Développement`) dans le
  worker de test Gradle sous Java 21.
- **`cursed_tree.json`** : remplacement de `below_trunk_provider` (inexistant en 1.21.1) par
  `dirt_provider` — corrige le crash `Failed to load registries` au chargement de monde.
- **Documentation** : mise à jour complète de `CLAUDE.md`, `README.md`, `docs/`, `.skills/`.

---

## [0.5.0] - 2026-04-12

### Changed
- **Documentation sync** : `README.md`, `docs/BLOCKS.md`, `docs/TESTING.md` et `docs/TROUBLESHOOTING.md`
  décrivent maintenant explicitement l'état actuel du Level 0 monocouche, du benchmark local et du
  pipeline de papier peint adaptatif.
- **Cache du layout Level 0** : capacité du cache de secteurs réduite à `1024` entrées pour limiter
  la mémoire retenue sur les petites configurations, sans changer la forme du maze.
- **Fallback client du papier peint adaptatif** : si la `ModelData` synchronisée n'est pas encore
  disponible, le modèle client relit d'abord les vrais blocs de sol déjà générés pour déduire la
  bonne palette, puis retombe sur l'échantillonnage déterministe en dernier recours.

---

## [0.4.0] - 2026-04-12

### Added
- **Backrooms - Level 0** : première implémentation monocouche basée sur le script Python de
  référence, avec dimension dédiée, générateur custom, palette de 6 blocs et traductions FR/EN.
- **Biomes cosmétiques du Level 0** : grandes zones de surface qui changent le papier peint et
  la moquette, avec une variation secondaire à murs blancs et tapis rouges.
- **Textures Level 0** : passage de la palette de blocs en `32x32`.
- **Papier peint adaptatif du Level 0** : le mur principal peut afficher une texture différente sur
  chaque face exposée selon le biome de surface adjacent.
- **Benchmark local du Level 0** : `LevelZeroPerformanceCheck` et tâche Gradle
  `benchmarkLevelZeroGeneration`.
- **Tests Backrooms** : `BackroomsLevelZeroLayoutTest` et `BackroomsLevelZeroRegistryTest`.

### Changed
- **Intérieur des murs du Level 0** : volumes non exposés en `bedrock` vanilla.
- **Optimisation des murs** : colonnes 100 % jaunes et 100 % blanches en blocs simples ;
  seul `level_zero_wallpaper_adaptive` utilise une `BlockEntity`.
- **Chargement client** : la `BlockEntity` ne recalcule plus le `faceMask` côté client au `onLoad()`.

---

## [0.3.0] - 2026-04-09

### Added
- **Arbre Maudit** : `TreeGrower` custom (`CURSED_TREE_GROWER`) avec `ConfiguredFeature` JSON
  `cursed_tree` ; la pousse génère un arbre avec troncs et feuilles maudits.
- **Tag `minecraft:logs`** : `cursed_log` ajouté aux tags block et item `minecraft:logs`.
- **Loot tables** : tables de butin pour les 4 blocs (`cursed_log`, `cursed_leaves`,
  `cursed_sapling`, `cursed_planks`).
- **Tests** : `CursedSnackTest`, `CursedTreeTest`.

### Fixed
- **Noms des BlockItems** : clés `item.petasse_gang_additions.*` ajoutées dans `en_us.json` et `fr_fr.json`.
- **Séparation client/serveur** : import `MinecraftClient` déplacé vers `GangBadgeClientHandler`.

---

## [0.2.0] - 2026-04-08

### Added
- **Packet réseau `GangBadgeActivatePayload`**.
- **Casse-croûte Maudit** (`cursed_snack`) : consommable qui retire 2 points de faim.
- **Arbre Maudit** : 4 blocs au thème violet-or (`cursed_log`, `cursed_leaves`, `cursed_sapling`, `cursed_planks`).

### Fixed
- **Animation badge** : correction du rendu via un packet `CLIENTBOUND` custom.

---

## [0.1.0] - 2026-04-07

### Added
- **Texture Gang Badge** : badge circulaire doré avec étoile 5 branches, 16×16 pixel art.
- **Clic droit Gang Badge** : son de chat + animation overlay totem.

### Fixed
- **Texture carré violet** : ajout de `assets/petasse_gang_additions/items/gang_badge.json`.
- **Renommage du mod_id** : `petassegang_addons` → `petasse_gang_additions`.

---

## [0.1.0] - 2026-04-06

### Added
- **Gang Badge** (`gang_badge`) — jeton officiel de membre PetasseGang.
- Onglet créatif "PetasseGang" avec Gang Badge comme icône.
- Config serveur : `enableGangBadge` (défaut `true`).
- Localisation française (`fr_fr`) et anglaise (`en_us`).
- Suite de tests JUnit 5 (`ModLoadTest`, `RegistryTest`, `ItemTest`, `ConfigTest`).
- CI/CD GitHub Actions (build, test, release workflows).
- Documentation complète sous `/docs`.
- Claude Code skills sous `/.skills`.
