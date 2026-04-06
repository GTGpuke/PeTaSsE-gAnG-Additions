# Changelog

All notable changes to PétasseGang Addons are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).
Versioning follows [Semantic Versioning](https://semver.org/).

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
