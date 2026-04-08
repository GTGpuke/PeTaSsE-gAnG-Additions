# Changelog

All notable changes to PeTaSsE_gAnG_Additions are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).
Versioning follows [Semantic Versioning](https://semver.org/).

---

## [0.3.0] — 2026-04-09

### Added
- **Arbre Maudit (correction croissance)** : `TreeGrower` custom (`CURSED_TREE_GROWER`) avec `ConfiguredFeature` JSON `cursed_tree` — la pousse génère désormais un arbre avec troncs et feuilles maudits, non un chêne vanilla.
- **Tag `minecraft:logs`** : `cursed_log` ajouté aux tags block et item `minecraft:logs` — les feuilles maudites persistent correctement autour du tronc.
- **Loot tables** : tables de butin pour les 4 blocs (`cursed_log`, `cursed_leaves`, `cursed_sapling`, `cursed_planks`) — format MC 26.1 (`loot_table/blocks/`).
- **Tests** : `CursedSnackTest`, `CursedTreeTest` — couverture des nouvelles classes.

### Fixed
- **Noms des BlockItems** : ajout des clés `item.petasse_gang_additions.*` dans `en_us.json` et `fr_fr.json` — les blocs avaient leurs noms affichés comme clé de traduction brute en jeu.
- **Textures Arbre Maudit** : refonte pixel art — tronc avec veines dorées diagonales, dessus en anneaux concentriques, feuilles semi-transparentes, planches avec grain et nœuds.
- **Séparation client/serveur** : import `net.minecraft.client.Minecraft` déplacé de `GangBadgeActivatePacket` vers `GangBadgeClientHandler` (package `client/handler/`).

---

## [0.2.0] — 2026-04-08

### Added
- **Packet réseau `GangBadgeActivatePacket`** : canal `SimpleChannel` Forge (`ModNetworking`) — remplace `broadcastEntityEvent(35)` pour afficher l'animation avec la texture du badge, non celle du totem vanilla.
- **Casse-croûte Maudit** (`cursed_snack`) : consommable qui retire 2 points de faim (`FoodData.eat(-2, 0)`) ; toujours mangeable.
- **Arbre Maudit** : 4 blocs au thème violet/or — `cursed_log`, `cursed_leaves`, `cursed_sapling`, `cursed_planks`. Textures générées en pixel-art 16x16.
- **Fichiers `items/` MC 26.1** pour tous les nouveaux items (exigence de rendu découverte lors du debug texture).

### Fixed
- **Animation badge** : `broadcastEntityEvent(35)` utilisait `findTotem()` côté client (hardcodé dans `ClientPacketListener`) → affichait la texture du totem vanilla. Corrigé par un packet `CLIENTBOUND` custom qui appelle directement `gameRenderer.displayItemActivation(mainHandItem)`.

---

## [0.1.0] — 2026-04-07

### Added
- **Texture Gang Badge** : badge circulaire doré avec étoile 5 branches, généré en 16x16 pixel art.
- **Clic droit Gang Badge** : son de chat (`CAT_AMBIENT_BABY` pitch 0.6) + animation overlay totem.

### Fixed
- **Texture carré violet** : ajout de `assets/petasse_gang_additions/items/gang_badge.json` — MC 26.1 exige ce fichier de définition pour rendre chaque item ; sans lui, la texture est ignorée même si elle est présente.
- **Build incrémental crash** : ajout de `doNotTrackState(...)` sur `processResources` dans `build.gradle` — empêche Gradle de supprimer les `.class` lors d'un build incrémental sur le répertoire de sortie partagé.
- **Crash "0 mods constructed"** : ajout de `sourceSets.main.output.resourcesDir = compileJava.destinationDirectory` dans `build.gradle` — le `ClasspathLocator` de FML ne voyait pas les classes compilées car elles étaient dans un répertoire séparé du `mods.toml`.
- **Crash `NullPointerException: Item id not set`** : ajout de `.setId(ITEMS.key("gang_badge"))` sur les `Item.Properties` — requis en MC 26.1.
- **Renommage du mod_id** : `petassegang_addons` → `petasse_gang_additions` (cohérence avec le nom du mod).
- **API Forge 26.1** : `IEventBus` → `BusGroup`, signature `appendHoverText` mise à jour, `GameTestHolder` → `GameTestNamespace`.
- **gradlew.bat** : correction du bloc de détection `JAVA_EXE` manquant.

---

## [0.1.0] — 2026-04-06

### Added
- **Gang Badge** (`gang_badge`) — official PétasseGang membership token
  - Stack size 1, Rarity EPIC
  - Always displays enchantment glint (`isFoil = true`)
  - Tooltip: "PétasseGang Official Member" (gold) + flavour text (grey italic)
- Custom creative tab "PétasseGang" with Gang Badge icon
- Server config: `enableGangBadge` (default `true`)
- French (`fr_fr`) and English (`en_us`) localisation
- JUnit 5 unit test suite (`ModLoadTest`, `RegistryTest`, `ItemTest`, `ConfigTest`)
- Forge GameTest integration (`PetasseGangGameTests`)
- GitHub Actions CI/CD (build, test, release workflows)
- Full documentation under `/docs`
- Claude Code skills under `/.skills`

---

<!-- Template for next release:
## [X.Y.Z] — YYYY-MM-DD

### Added
-

### Changed
-

### Fixed
-

### Removed
-
-->
