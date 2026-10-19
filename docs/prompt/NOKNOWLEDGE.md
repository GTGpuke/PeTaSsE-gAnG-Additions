# Onboarding — nouvel assistant sans contexte

Lis ces fichiers dans l'ordre avant de toucher au code.

## 1. Règles et vue d'ensemble
- `CLAUDE.md` (racine) — règles obligatoires, commandes, pièges.
- `README.md` — contenu du mod, structure, qualité.
- `docs/AUDIT.md` — état du projet après l'audit d'octobre 2026 : ce qui a été décidé et pourquoi.

## 2. Build
- `gradle.properties` — toutes les versions (MC 1.21.1, Loader 0.19.5, API 0.116.17, Loom 1.17.21, Java 21, JUnit 5.11.4).
- `build.gradle` — runs (`client`, `server`, `gametest`), toggles `-P`, tests + JaCoCo, benchmark.
- `src/main/resources/fabric.mod.json` — entrypoints `main`, `client`, `fabric-gametest`, mixins.

## 3. Code
- `core/ModConstants.java`, `config/ModConfig.java`.
- `GtgsBackroomsMod.java` → `registerContent()` ; `init/*`.
- `backrooms/level0/generation/LevelZeroChunkGenerator.java` puis `layout/LevelZeroLayout.java`,
  `layout/LevelZeroCellEvaluator.java`, `write/LevelZeroBlockWriter.java`.

## 4. Architecture et tests
- `docs/ARCHITECTURE.md` — packages, pipeline de génération, rendu client, conventions.
- `docs/TESTING.md` — familles de tests, `MinecraftTestBootstrap`, empreintes, GameTest, benchmark.

## 5. Guide et specs Backrooms
- `docs/GUIDE.md` — guide de conception et de développement du projet (règles d'or, état réel, architecture cible du
  rendu et de l'audio, feuille de route) : à lire au début de chaque session, il complète `CLAUDE.md`.
- `docs/backrooms/backrooms-level0-pipeline-v6.md` (spec du générateur du Level 0) et `backrooms-level0-roadmap.md`.
- `docs/PORTALS.md` pour le système de portails tel qu'implémenté (lecture obligatoire avant de toucher `backrooms/portal/`).

## Pièges à mémoriser

| Piège | Règle |
|-------|-------|
| API Yarn | `Identifier.of`, `Item.Settings`, `.maxCount`, `hasGlint`, `finishUsing`, `TypedActionResult`, `World`/`ServerWorld` |
| Worldgen | `dirt_provider` dans `minecraft:tree` ; `assets/<ns>/items/` ignoré en 1.21.1 |
| Windows | `-Dfile.encoding=COMPAT` dans `org.gradle.jvmargs` |
| Génération | déterministe et verrouillée par `LevelZeroGoldenTest` ; sels de `StageRandom` figés |
| Tests | `MinecraftTestBootstrap.ensure()` pour tout ce qui touche blocs/items/registres |
| Loom | ≥ 1.18 exige Java 25 pour Gradle : rester sur 1.17 tant que le projet est en Java 21 |

## Commandes

```bash
./gradlew test
./gradlew runGameTest
./gradlew checkstyleMain checkstyleTest checkstyleGametest
./gradlew benchmarkLevelZeroGeneration
./gradlew runClient
./gradlew clean build --warning-mode all
```
