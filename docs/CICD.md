# CI/CD — PeTaSsE_gAnG_Additions

## Vue d'ensemble des workflows

| Workflow | Fichier | Trigger | Ce qu'il fait |
|----------|---------|---------|--------------|
| Build | `build.yml` | push main/develop, PR | Compile + produit le JAR |
| Tests | `test.yml` | push main/develop, PR | JUnit 5, rapport HTML |
| Release | `release.yml` | tag `v*.*.*` | Build + GitHub Release + JAR joint |

---

## Workflow Build (`build.yml`)

**Trigger :** push sur `main` ou `develop`, PR vers `main`

**Étapes :**
1. Checkout du code
2. Java 25 (Temurin) + cache Gradle
3. `./gradlew build`
4. Upload du JAR comme artifact GitHub (conservé 14 jours)

**Artifact :** `petasse_gang_additions-{sha}` → contient les JARs compilés.

---

## Workflow Tests (`test.yml`)

**Trigger :** push sur `main` ou `develop`, PR vers `main`

**Étapes :**
1. Checkout + Java 25 + cache Gradle
2. `./gradlew test`
3. Publier les résultats comme check PR (vert/rouge)
4. Upload du rapport HTML (conservé 30 jours)

**La PR échoue automatiquement si un test JUnit échoue.**

---

## Workflow Release (`release.yml`)

**Trigger :** push d'un tag `v*.*.*` (ex: `v0.1.0`, `v1.2.3`)

**Étapes :**
1. Tests unitaires (bloque si fail)
2. `./gradlew build`
3. Extraction des release notes depuis `docs/CHANGELOG.md`
4. Création d'une GitHub Release avec le JAR joint

**Tags pre-release :** si le tag contient `-` (ex: `v0.2.0-beta.1`), la release est marquée pre-release.

---

## Créer une release

```bash
# 1. Mettre à jour docs/CHANGELOG.md
# 2. Mettre à jour mod_version dans gradle.properties
# 3. Commit
git add gradle.properties docs/CHANGELOG.md
git commit -m "chore: bump version to 0.2.0"

# 4. Pousser + tagger
git push origin main
git tag v0.2.0
git push origin v0.2.0

# → GitHub Actions build et publie automatiquement
```

---

## Badges pour le README

```markdown
[![Build](https://github.com/PetasseGang/petasse_gang_additions/actions/workflows/build.yml/badge.svg)](https://github.com/PetasseGang/petasse_gang_additions/actions/workflows/build.yml)
[![Tests](https://github.com/PetasseGang/petasse_gang_additions/actions/workflows/test.yml/badge.svg)](https://github.com/PetasseGang/petasse_gang_additions/actions/workflows/test.yml)
```

---

## Débugger un build qui échoue

1. **Onglet Actions → workflow en échec → cliquer sur le job**
2. **Expand l'étape qui a échoué** pour voir les logs complets
3. **Reproduire localement :**
   ```bash
   ./gradlew build --info 2>&1 | grep -E "ERROR|error:|FAILED"
   ```
4. **Cache Gradle corrompu ?** → Actions → Caches → supprimer le cache du projet
5. **Problème Java version ?** → vérifier que `java_version=25` dans `gradle.properties`
