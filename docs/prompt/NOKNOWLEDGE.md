# Onboarding — Nouveau prompt Claude Code

Tu démarres sans contexte sur ce projet. Lis les fichiers suivants dans cet ordre exact avant de toucher quoi que ce soit.

---

## Lecture obligatoire

### 1. Vue d'ensemble
- [`README.md`](../../README.md) — Stack, structure du projet, quick start, skills disponibles

### 2. Configuration Gradle (critique — nombreux pièges FG7)
- [`gradle.properties`](../../gradle.properties) — Toutes les versions : MC, Forge, Java, mod_id, group, mod_version
- [`build.gradle`](../../build.gradle) — Config complète ForgeGradle 7, runs, dépendances, compileJava
- [`settings.gradle`](../../settings.gradle) — Pourquoi foojay est absent (piège Gradle 9.x)
- [`gradle/wrapper/gradle-wrapper.properties`](../../gradle/wrapper/gradle-wrapper.properties) — Version du wrapper (9.3.0+)

### 3. Identité du mod
- [`src/main/resources/META-INF/mods.toml`](../../src/main/resources/META-INF/mods.toml) — Déclaration Forge, dépendances obligatoires/optionnelles

### 4. Code source principal
- [`src/main/java/com/petassegang/addons/util/ModConstants.java`](../../src/main/java/com/petassegang/addons/util/ModConstants.java) — MOD_ID, MOD_NAME, LOGGER (référence partout)
- [`src/main/java/com/petassegang/addons/PetasseGangAddonsMod.java`](../../src/main/java/com/petassegang/addons/PetasseGangAddonsMod.java) — Point d'entrée @Mod, bus d'événements, init des registres
- [`src/main/java/com/petassegang/addons/init/ModItems.java`](../../src/main/java/com/petassegang/addons/init/ModItems.java) — Pattern DeferredRegister pour les items

### 5. Architecture et conventions
- [`docs/ARCHITECTURE.md`](../ARCHITECTURE.md) — Conventions de code, patterns obligatoires, stack technique, où ajouter quoi

### 6. Pièges déjà rencontrés (lire avant tout build ou config)
- [`AUDIT_REPORT.md`](../../AUDIT_REPORT.md) — Historique complet des erreurs ForgeGradle 7 corrigées ; évite de les reproduire

### 7. Skills (conventions d'ajout de contenu)
Parcourir tous les `SKILL.md` dans [`.skills/`](../../.skills/) :
- `add-item` / `add-block` / `add-entity` / `add-dimension`
- `add-recipe` / `add-sound` / `add-creative-tab` / `add-dependency`

---

## Pièges critiques — mémorise-les avant tout

### ForgeGradle 7 (FG7) vs FG6
| ❌ FG6 / incorrect | ✅ FG7 / correct |
|-------------------|-----------------|
| `workingDirectory project.file(...)` | `workingDir = project.file(...)` |
| `programArguments [...]` | `args = [...]` |
| `minecraft "net.minecraftforge:forge:..."` dans `dependencies {}` | `version = "${minecraft_version}-${forge_version}"` dans `minecraft {}` |
| `finalizedBy 'reobfJar'` | supprimé (MC 26.1 est déobfusqué) |
| `copyIdeResources true` | supprimé (n'existe pas en FG7) |
| `genVSCodeRuns` | n'existe pas — utiliser `genEclipseRuns` |
| `runClient` (task Gradle) | n'existe pas comme task directe — passer par IDE après `genEclipseRuns` |

### Gradle 9.x
- **Foojay toolchain resolver absent** : `JvmVendorSpec.IBM_SEMERU` retiré en Gradle 9.x → crash au démarrage. Ne jamais réajouter ce plugin dans `settings.gradle`.
- **Wrapper minimum : 9.3.0** — FG7 le requiert explicitement.

### Minecraft 26.1
- **Java 25 obligatoire** — le build échoue sans lui (toolchain). Installer via `winget install EclipseAdoptium.Temurin.25.JDK`.
- **Mappings :** `channel: 'official', version: minecraft_version` — pas de MCP, pas de Parchment.
- **pack_format :** `55` dans `pack.mcmeta`.
- **`TooltipContext` :** type imbriqué `Item.TooltipContext` — l'import complet est `net.minecraft.world.item.Item.TooltipContext`.

### Compilation Java
- `-Werror` est **absent intentionnellement** : le code généré par Forge produit des warnings qui casseraient le build.

---

## Stack en un coup d'œil

| Composant | Version |
|-----------|---------|
| Minecraft | 26.1 |
| Forge | 62.0.x |
| ForgeGradle | 7.x |
| Gradle | 9.3.0+ |
| Java | 25 (Temurin recommandé) |
| JUnit | 5.10.3 |
| mod_id | `petassegang_addons` |
| package racine | `com.petassegang.addons` |

---

## Commandes utiles

```bash
./gradlew genEclipseRuns   # Générer les run configs IDE (une seule fois)
./gradlew build            # Build → build/libs/petassegang_addons-0.1.0.jar
./gradlew test             # Tests JUnit 5
./gradlew runGameTestServer # Tests in-game Forge
./gradlew runData          # Data generation
./gradlew clean build      # Rebuild propre
./gradlew dependencies     # Voir l'arbre de dépendances complet
```

---

## Si le build échoue

Voir [`docs/TROUBLESHOOTING.md`](../TROUBLESHOOTING.md) et [`AUDIT_REPORT.md`](../../AUDIT_REPORT.md).
