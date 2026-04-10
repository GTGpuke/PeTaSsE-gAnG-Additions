# Dépendances — PeTaSsE_gAnG_Additions

Ce document recense toutes les dépendances externes du mod, leur rôle,
leurs coordonnées Maven, et leur mode d'inclusion (JiJ ou soft-dep).

---

## Mécanisme JiJ (Jar-in-Jar)

Le JiJ permet d'embarquer les JARs des bibliothèques obligatoires directement
dans le JAR du mod. Forge les extrait et les charge automatiquement — les joueurs
n'ont pas à télécharger ces dépendances séparément.

Activé dans `build.gradle` via `jarJar.register()` (plugin `net.minecraftforge.jarjar` 0.2.3).

> **État au 2026-04-10** : L'infrastructure JiJ est opérationnelle (tâches `jarJar` et
> `jarJarMetadata` enregistrées, build réussi). Cependant, **aucune des bibliothèques
> prévues n'a encore été portée sur MC 26.1 / Forge 62**. Les entrées dans `build.gradle`
> sont commentées en attente de mises à jour des mods concernés. Décommenter et
> renseigner la version exacte dès qu'une release compatible est disponible.

---

## Dépendances obligatoires (JiJ)

Ces bibliothèques sont embarquées dans le JAR du mod.

### GeckoLib

| Propriété | Valeur |
|-----------|--------|
| Rôle | Animations 3D pour les entités et blocs custom (mobs des Backrooms) |
| Group | `software.bernie.geckolib` |
| Artifact | `geckolib-forge-26.1` (à confirmer) |
| Repository | `https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/` |
| Licence | MIT |
| JiJ | Oui — dès qu'une version MC 26.1 est publiée |
| **État** | ❌ Aucune version MC 26.1 disponible (vérifié 2026-04-10) |

### Patchouli

| Propriété | Valeur |
|-----------|--------|
| Rôle | Guide/livre in-game pour documenter les Backrooms (lore, dangers, étages) |
| Group | `vazkii.patchouli` |
| Artifact | `Patchouli` |
| Repository | `https://maven.blamejared.com/` |
| Licence | Patchouli License (redistribution JiJ autorisée) |
| JiJ | Oui — dès qu'une version MC 26.1 est publiée |
| **État** | ❌ Dernière version : MC 1.21.1 (NeoForge). Aucune version Forge 62 (vérifié 2026-04-10) |

### Konkrete

| Propriété | Valeur |
|-----------|--------|
| Rôle | Bibliothèque utilitaire requise par FancyMenu |
| Group | `de.keksuccino` |
| Artefact | `konkrete` |
| Repository | `https://maven.keksuccino.de/` (⚠️ domain parked — à surveiller) |
| Licence | GNU GPLv3 |
| JiJ | Oui — si repo rétabli et version MC 26.1 disponible |
| **État** | ❌ `maven.keksuccino.de` est un domain parked, aucune release disponible (vérifié 2026-04-10) |

### FancyMenu

| Propriété | Valeur |
|-----------|--------|
| Rôle | Personnalisation des menus MC (écran titre ambiance Backrooms) |
| Group | `de.keksuccino` |
| Artefact | `fancymenu` |
| Repository | `https://maven.keksuccino.de/` (⚠️ domain parked — à surveiller) |
| Licence | DSMSL v3 |
| JiJ | Oui (usage non-commercial) — si repo rétabli et version MC 26.1 disponible |
| **État** | ❌ Même état que Konkrete (vérifié 2026-04-10) |

### Fusion (Connected Textures)

| Propriété | Valeur |
|-----------|--------|
| Rôle | Textures connectées pour le papier peint et la moquette des Backrooms |
| Group | `com.supermartijn642` |
| Artifact | `fusion` |
| Repository | `https://maven.supermartijn642.com/releases/` |
| Licence | MIT |
| JiJ | Oui — dès qu'une version MC 26.1 est publiée |
| **État** | ❌ Aucune release disponible, repo inaccessible (vérifié 2026-04-10) |

---

## Dépendances optionnelles (soft-dep)

Ces bibliothèques améliorent l'expérience si présentes, mais ne sont pas requises.
Elles sont déclarées en `compileOnly` — non embarquées dans le JAR.

### Immersive Portals

| Propriété | Valeur |
|-----------|--------|
| Rôle | Portails immersifs sans écran de chargement entre dimension et Backrooms |
| Intégration | Détection runtime via `ModList.get().isLoaded("imm_ptl_core")` |
| Source | CurseForge / Modrinth |
| JiJ | Non |

### Oculus (Iris pour Forge)

| Propriété | Valeur |
|-----------|--------|
| Rôle | Support des shaders pour effets visuels des Backrooms (brume, bloom) |
| Intégration | Détection runtime via `ModList.get().isLoaded("oculus")` |
| Source | CurseForge / Modrinth |
| JiJ | Non |

---

## Résoudre les versions exactes

Les plages de version ci-dessus sont indicatives. Pour obtenir les versions
disponibles pour MC 26.1 :

```powershell
# Lister les versions GeckoLib disponibles
./gradlew dependencyInsight --dependency geckolib --configuration runtimeClasspath

# Lister toutes les dépendances résolues
./gradlew dependencies --configuration runtimeClasspath
```

Ou consulter directement les repositories Maven listés ci-dessus.

---

## Ajouter une nouvelle dépendance

1. Vérifier la licence — compatible JiJ ?
2. Ajouter le repository dans `build.gradle` → bloc `repositories {}`
3. Ajouter `jarJar(...)` ou `compileOnly(...)` dans `dependencies {}`
4. Ajouter l'entrée `[[dependencies.${mod_id}]]` dans `mods.toml`
5. Documenter ici
