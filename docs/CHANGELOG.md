# Changelog

All notable changes to PeTaSsE_gAnG_Additions are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).
Versioning follows [Semantic Versioning](https://semver.org/).

---

## [Unreleased]

### Added
- **Backrooms - Level 0** : retour d'une premiere implémentation monocouche basee sur la structure du script Python de reference, avec dimension dediee, generateur custom, palette de 4 blocs et traductions FR/EN.
- **Tests Backrooms** : `BackroomsLevelZeroLayoutTest` et `BackroomsLevelZeroRegistryTest` pour verifier les invariants de base du layout et des registres.
- **Infrastructure JiJ** : plugin `net.minecraftforge.jarjar`, repositories Backrooms et structure `jarJar.register()` ajoutés dans le build.
- **Dépendances optionnelles** : Immersive Portals et Oculus déclarés en soft-dep dans `mods.toml`.
- **`docs/DEPENDENCIES.md`** : documentation de référence des dépendances Backrooms, de leur mode d'inclusion et de leur état actuel.

## [0.3.0] - 2026-04-09

### Added
- **Arbre Maudit (correction croissance)** : `TreeGrower` custom (`CURSED_TREE_GROWER`) avec `ConfiguredFeature` JSON `cursed_tree` - la pousse genere desormais un arbre avec troncs et feuilles maudits, pas un chene vanilla.
- **Tag `minecraft:logs`** : `cursed_log` ajoute aux tags block et item `minecraft:logs` - les feuilles maudites persistent correctement autour du tronc.
- **Loot tables** : tables de butin pour les 4 blocs (`cursed_log`, `cursed_leaves`, `cursed_sapling`, `cursed_planks`) - format MC 26.1 (`loot_table/blocks/`).
- **Tests** : `CursedSnackTest`, `CursedTreeTest` - couverture des nouvelles classes.

### Fixed
- **Noms des BlockItems** : ajout des cles `item.petasse_gang_additions.*` dans `en_us.json` et `fr_fr.json`.
- **Textures Arbre Maudit** : refonte pixel art.
- **Separation client/serveur** : import `net.minecraft.client.Minecraft` deplace de `GangBadgeActivatePacket` vers `GangBadgeClientHandler`.

---

## [0.2.0] - 2026-04-08

### Added
- **Packet reseau `GangBadgeActivatePacket`** : canal `SimpleChannel` Forge (`ModNetworking`).
- **Casse-croute Maudit** (`cursed_snack`) : consommable qui retire 2 points de faim ; toujours mangeable.
- **Arbre Maudit** : 4 blocs au theme violet/or - `cursed_log`, `cursed_leaves`, `cursed_sapling`, `cursed_planks`.
- **Fichiers `items/` MC 26.1** pour tous les nouveaux items.

### Fixed
- **Animation badge** : correction du rendu via un packet `CLIENTBOUND` custom.

---

## [0.1.0] - 2026-04-07

### Added
- **Texture Gang Badge** : badge circulaire dore avec etoile 5 branches, genere en 16x16 pixel art.
- **Clic droit Gang Badge** : son de chat (`CAT_AMBIENT_BABY` pitch 0.6) + animation overlay totem.

### Fixed
- **Texture carre violet** : ajout de `assets/petasse_gang_additions/items/gang_badge.json`.
- **Build incremental crash** : ajout de `doNotTrackState(...)` sur `processResources` dans `build.gradle`.
- **Crash "0 mods constructed"** : ajout de `sourceSets.main.output.resourcesDir = compileJava.destinationDirectory` dans `build.gradle`.
- **Crash `NullPointerException: Item id not set`** : ajout de `.setId(ITEMS.key("gang_badge"))`.
- **Renommage du mod_id** : `petassegang_addons` -> `petasse_gang_additions`.
- **API Forge 26.1** : `IEventBus` -> `BusGroup`, signature `appendHoverText` mise a jour, `GameTestHolder` -> `GameTestNamespace`.
- **gradlew.bat** : correction du bloc de detection `JAVA_EXE` manquant.

---

## [0.1.0] - 2026-04-06

### Added
- **Gang Badge** (`gang_badge`) - official PetasseGang membership token
- Custom creative tab "PetasseGang" with Gang Badge icon
- Server config: `enableGangBadge` (default `true`)
- French (`fr_fr`) and English (`en_us`) localisation
- JUnit 5 unit test suite (`ModLoadTest`, `RegistryTest`, `ItemTest`, `ConfigTest`)
- Forge GameTest integration (`PetasseGangGameTests`)
- GitHub Actions CI/CD (build, test, release workflows)
- Full documentation under `/docs`
- Claude Code skills under `/.skills`

---
