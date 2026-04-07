# Changelog

All notable changes to PeTaSsE_gAnG_Additions are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).
Versioning follows [Semantic Versioning](https://semver.org/).

---

## [0.1.0] — 2026-04-07

### Fixed
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
