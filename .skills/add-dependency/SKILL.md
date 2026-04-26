---
name: add-dependency
description: "Ajouter une dépendance externe, une librairie, un mod API, ou un framework au mod PeTaSsE_gAnG_Additions (Fabric 1.21.1). Utilise ce skill dès qu'on veut intégrer une librairie Java, un mod API (REI, GeckoLib, Patchouli, etc.), ou toute dépendance Maven/Modrinth. Déclenche pour 'ajoute une lib', 'ajoute une dépendance', 'intègre REI', 'ajoute GeckoLib', 'librairie', 'dépendance', 'API', 'framework', 'intégration', 'import externe', 'dependency'."
---

# Skill — Ajouter une Dépendance Externe (Fabric 1.21.1)

## Quand utiliser ce skill

- "Ajoute une dépendance [nom]"
- "Intègre [REI / GeckoLib / Patchouli / autre mod]"
- "J'ai besoin de la librairie [nom]"
- "Le mod doit fonctionner avec [mod API]"
- Tout ce qui implique ajouter une ligne dans `dependencies {}` de `build.gradle`

---

## Types de dépendances

### Type A — Mod Fabric (hard dependency)

```groovy
// API compile-only (le joueur installe le mod séparément)
modCompileOnly "maven.modrinth:rei:VERSION:api"

// Mod complet aussi au runtime (dev/test local)
modRuntimeOnly "maven.modrinth:rei:VERSION"

// Les deux ensemble (le plus courant pour les APIs)
modCompileOnly "maven.modrinth:rei:VERSION:api"
modRuntimeOnly "maven.modrinth:rei:VERSION"
```

### Type B — Librairie Java (Maven Central)

Libs Java pures incluses dans le classpath ou à embarquer.

```groovy
implementation "com.google.code.gson:gson:2.11.0"
implementation "org.apache.commons:commons-lang3:3.14.0"
```

### Type C — Librairie embarquée dans le JAR (JiJ — Jar-in-Jar)

Quand la lib doit voyager avec le JAR et ne pas être installée séparément.
Fabric Loom l'extrait et la charge automatiquement.

```groovy
// Dans le bloc dependencies {} :
include modImplementation("software.bernie.geckolib:geckolib-fabric-1.21.1:VERSION")
```

> **Note Fabric :** JiJ s'active via `include` directement — pas de `jarJar.enable()` ni de plugin supplémentaire.

### Type D — Compile-only

Pour les annotations processors ou les APIs qui ne doivent pas être dans le JAR final.

```groovy
compileOnly "org.jetbrains:annotations:24.0.0"
```

---

## Mods populaires — Coordonnées prêtes à l'emploi

| Mod | Usage | Type | Coordonnées (adapter VERSION) |
|-----|-------|------|-------------------------------|
| **REI** | Recettes in-game (remplace JEI) | modCompileOnly + modRuntimeOnly | `me.shedaniel.cloth:cloth-config-fabric:VERSION` |
| **GeckoLib** | Mobs/items animés (JiJ) | include modImplementation | `software.bernie.geckolib:geckolib-fabric-1.21.1:VERSION` |
| **Patchouli** | Livres de doc in-game | modCompileOnly + modRuntimeOnly | `vazkii.patchouli:Patchouli-1.21.1-FABRIC:VERSION` |
| **Iris** | Shaders (JiJ optionnel) | modCompileOnly | via ModrinthMaven |
| **Trinkets** | Slots d'équipement custom | modCompileOnly + modRuntimeOnly | `dev.emi:trinkets:VERSION+1.21.1` |
| **Jade** | Info-bulles sur les blocs | modCompileOnly | via ModrinthMaven |
| **Sodium** | Optimisation rendu | modCompileOnly | via ModrinthMaven |

---

## Étapes

### 1. Identifier la dépendance

- **Nom exact + version** : chercher sur Modrinth, Maven Central, ou le README du mod
- **Type** : mod Fabric, lib Java, ou API pure ?
- **Obligatoire ou optionnelle** (hard vs soft dependency) ?

### 2. Trouver les coordonnées Maven

```bash
# Maven Central
# → https://search.maven.org (chercher par groupe:artifactId)

# Mods Fabric — souvent sur Modrinth Maven
# → https://modrinth.com/maven (tous les mods Modrinth)
# Format Modrinth : maven.modrinth:mod-slug:VERSION
```

Si via Modrinth Maven, ajouter le repository dans `build.gradle` :

```groovy
repositories {
    maven { name = 'Modrinth'; url = 'https://api.modrinth.com/maven' }
    maven { name = 'GeckoLib'; url = 'https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/' }
    maven { name = 'BlameJared'; url = 'https://maven.blamejared.com' }
    maven { name = 'TerraformersMC'; url = 'https://maven.terraformersmc.com/releases' }
    maven { name = 'Shedaniel'; url = 'https://maven.shedaniel.me/' }
}
```

### 3. Ajouter dans build.gradle

Localiser la section `dependencies {}` dans `build.gradle` et ajouter :

```groovy
// [Nom du mod/lib] — [description courte de pourquoi]
modImplementation "groupe:artefact:version"
```

### 4. Mettre à jour fabric.mod.json (si mod Fabric)

Ajouter dans `src/main/resources/fabric.mod.json` :

```json
{
  "depends": {
    "nom_du_mod": ">=VERSION"
  }
}
```

Pour une dépendance optionnelle, utiliser `"recommends"` ou `"suggests"` à la place de `"depends"`.

### 5. Créer un wrapper d'intégration (soft dependency recommandé)

Si la dépendance est optionnelle (`"suggests"`), isoler l'intégration :

```
src/main/java/com/petassegang/addons/compat/
└── rei/
    └── REICompat.java
```

```java
package com.petassegang.addons.compat.rei;

public final class REICompat {
    public static void register() {
        // Intégration REI ici
    }
    private REICompat() {}
}
```

Dans `PeTaSsEgAnGAdditionsMod.onInitialize()` :

```java
import net.fabricmc.loader.api.FabricLoader;

// Dans onInitialize() :
if (FabricLoader.getInstance().isModLoaded("roughlyenoughitems")) {
    REICompat.register();
    ModConstants.LOGGER.info("Intégration REI activée.");
}
```

### 6. Sync et vérification

```bash
./gradlew --refresh-dependencies build
```

Si erreur de résolution :
- Vérifier l'URL du repository
- Vérifier le `group:artifact:version` exact
- Essayer `./gradlew dependencies` pour voir l'arbre complet

### 7. Mettre à jour la documentation

- `docs/ARCHITECTURE.md` — ajouter dans la section "Stack technique"
- `docs/SETUP.md` — si ça change l'installation (ex: mod requis par le joueur)
- `docs/CHANGELOG.md` — `### Added: Intégration [nom]`

---

## Checklist finale

- [ ] Coordonnées Maven vérifiées (groupe + artefact + version exacts)
- [ ] Repository Maven ajouté dans `build.gradle` (si non-standard)
- [ ] Ligne de dépendance ajoutée (`modImplementation` / `modCompileOnly` / `include`)
- [ ] `fabric.mod.json` mis à jour (`depends` / `recommends` / `suggests`)
- [ ] Package `compat/<mod>/` créé si soft dependency
- [ ] `FabricLoader.getInstance().isModLoaded()` check si soft dependency
- [ ] `./gradlew build` passe sans erreur
- [ ] Intégration testée en jeu (ou test unitaire ajouté)
- [ ] `docs/ARCHITECTURE.md` mis à jour
- [ ] `docs/CHANGELOG.md` mis à jour
