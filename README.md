# PeTaSsE_gAnG_Additions

Custom content mod for the PetasseGang Minecraft server.

[![Build](https://github.com/PetasseGang/petasse_gang_additions/actions/workflows/build.yml/badge.svg)](https://github.com/PetasseGang/petasse_gang_additions/actions/workflows/build.yml)
[![Tests](https://github.com/PetasseGang/petasse_gang_additions/actions/workflows/test.yml/badge.svg)](https://github.com/PetasseGang/petasse_gang_additions/actions/workflows/test.yml)
![MC](https://img.shields.io/badge/Minecraft-26.1-brightgreen)
![Forge](https://img.shields.io/badge/Forge-62.0.x-orange)
![License](https://img.shields.io/badge/License-MIT-blue)

---

## Quick Start

Prerequisites: Java 25 and Git. Gradle is provided via the wrapper.

```bash
# 1. Clone
git clone https://github.com/PetasseGang/petasse_gang_additions.git
cd petasse_gang_additions

# 2. Run the dev client
./gradlew runClient

# 3. Build the mod
./gradlew build
```

First-time setup details are documented in [docs/SETUP.md](docs/SETUP.md).

---

## Current State

The project currently includes:

- a custom Forge 26.1 setup,
- a first playable Backrooms Level 0 dimension,
- a custom monocouche chunk generator inspired by the reference Python script,
- cosmetic Level 0 surface biomes that change wallpaper and carpet without changing the maze topology,
- adaptive wallpaper rendering per exposed face, now reserved to mixed wall transitions only,
- bedrock-filled inner wall mass so only exposed wall shells use wallpaper logic,
- the original Gang Badge and cursed tree content,
- a JUnit 5 and Forge GameTest test suite.

---

## Project Structure

```text
petasse_gang_additions/
|- src/main/java/com/petassegang/addons/
|  |- PeTaSsEgAnGAdditionsMod.java
|  |- client/
|  |- config/
|  |- creative/ModCreativeTab.java
|  |- init/
|  |  |- ModBlocks.java
|  |  |- ModBlockEntities.java
|  |  |- ModChunkGenerators.java
|  |  `- ModItems.java
|  |- item/
|  |- network/
|  |- util/ModConstants.java
|  `- world/backrooms/
|     |- BackroomsConstants.java
|     `- level0/
|        |- LevelZeroChunkGenerator.java
|        |- LevelZeroLayout.java
|        `- LevelZeroSurfaceBiome.java
|- src/main/resources/
|  |- META-INF/mods.toml
|  |- assets/petasse_gang_additions/
|  `- data/petasse_gang_additions/
|- src/test/
|- docs/
`- build.gradle
```

---

## Main Commands

```bash
# Compile main sources
./gradlew compileJava

# Run unit tests
./gradlew test

# Run the dev client
./gradlew runClient

# Full build
./gradlew build
```

---

## Level 0 Notes

The current Level 0 implementation is built around a deterministic layout pipeline:

- maze generation translated from the reference Python prototype,
- rectangular rooms,
- pillar rooms,
- custom polygon rooms,
- a `1 logical cell = 3x3 blocks` scale in-world,
- a low ceiling and strong fluorescent lighting for the intended oppressive feel.

The current cosmetic biome layer only changes surface appearance. It does not change the layout shape.
Wallpaper rendering now adapts per exposed face only on truly mixed transitions between adjacent surface biomes.
Simple yellow walls and simple white walls are now plain blocks, while an internal adaptive wall block is only used when a column really needs per-face blending.
The exposed wallpaper face mask is computed during generation, stored in synchronized block entities for these mixed cases only, and reused by the client renderer.
If synchronized model data is not available yet on the client, the adaptive wallpaper model now falls back to the generated floor blocks first, then only to the deterministic biome sampler as a last resort. This keeps the lowest visible wall row more stable during chunk loads.
Inner wall mass now uses vanilla bedrock so adaptive wallpaper stays only on visible surfaces.
The layout cache also stays intentionally bounded to reduce retained heap on smaller integrated-graphics machines.

Level 0 block textures currently follow a dedicated `32x32` convention.

---

## Testing

```bash
# JUnit 5 tests
./gradlew test

# Forge GameTests
./gradlew runGameTestServer
```

If you are working on Windows and the project path contains accented characters, also check
[docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md) for known Gradle and encoding edge cases.

---

## Documentation

| Document | Description |
|----------|-------------|
| [docs/SETUP.md](docs/SETUP.md) | Local setup |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Project architecture and conventions |
| [docs/DEPENDENCIES.md](docs/DEPENDENCIES.md) | Backrooms dependency plan |
| [docs/DIMENSIONS.md](docs/DIMENSIONS.md) | Dimension reference |
| [docs/BLOCKS.md](docs/BLOCKS.md) | Block catalog |
| [docs/ITEMS.md](docs/ITEMS.md) | Item catalog |
| [docs/TESTING.md](docs/TESTING.md) | Test guide |
| [docs/CHANGELOG.md](docs/CHANGELOG.md) | Version history |
| [docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md) | Common issues |

---

## Build Output

```bash
build/libs/petasse_gang_additions-<version>.jar
```

---

## License

MIT. See [LICENSE](LICENSE).
