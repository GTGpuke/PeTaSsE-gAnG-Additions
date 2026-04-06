---
name: add-dependency
description: "Ajouter une dépendance externe, une librairie, un mod API, ou un framework au mod PétasseGang Addons. Utilise ce skill dès qu'on veut intégrer une librairie Java, un mod API (JEI, Curios, Patchouli, GeckoLib, etc.), ou toute dépendance Maven/CurseForge. Déclenche pour 'ajoute une lib', 'ajoute une dépendance', 'intègre JEI', 'ajoute GeckoLib', 'librairie', 'dépendance', 'API', 'framework', 'intégration', 'import externe', 'dependency'."
---

# Skill — Ajouter une Dépendance Externe

## Quand utiliser ce skill

- "Ajoute une dépendance [nom]"
- "Intègre [JEI / GeckoLib / Curios / Patchouli / autre mod]"
- "J'ai besoin de la librairie [nom]"
- "Le mod doit fonctionner avec [mod API]"
- Tout ce qui implique ajouter une ligne dans `dependencies {}` de `build.gradle`

---

## Types de dépendances

### Type A — Mod Forge (deobfusqué avec `fg.deobf`)

Quand tu intègres un autre mod comme dépendance. Doit être deobfusqué pour que le classpath soit cohérent.

```groovy
// API compile-only (tu codes contre l'API, le joueur doit installer le mod séparément)
compileOnly fg.deobf("mezz.jei:jei-${minecraft_version}-forge:VERSION:api")

// Mod complet aussi au runtime (dev/test local)
runtimeOnly fg.deobf("mezz.jei:jei-${minecraft_version}-forge:VERSION")

// Les deux ensemble (le plus courant)
compileOnly fg.deobf("mezz.jei:jei-${minecraft_version}-forge:VERSION:api")
runtimeOnly fg.deobf("mezz.jei:jei-${minecraft_version}-forge:VERSION")
```

### Type B — Librairie Java (Maven Central)

Libs Java pures déjà présentes dans le classpath Forge ou à inclure.

```groovy
implementation "com.google.code.gson:gson:2.11.0"
implementation "org.apache.commons:commons-lang3:3.14.0"
implementation "com.github.ben-manes.caffeine:caffeine:3.1.8"
```

### Type C — Librairie embarquée dans le JAR (JarJar)

Quand la lib doit voyager avec le JAR et ne pas être installée séparément.

```groovy
// Activer JarJar dans les plugins :
// plugins { id 'net.minecraftforge.jarjar' }

jarJar(group: "com.example", name: "lib", version: "[1.0,2.0)")
```

### Type D — Compile-only

Pour les annotations processors ou les APIs qui ne doivent pas être dans le JAR final.

```groovy
compileOnly "org.jetbrains:annotations:24.0.0"
compileOnly annotationProcessor("com.google.auto.service:auto-service:1.1.1")
```

---

## Mods populaires — Coordonnées prêtes à l'emploi

| Mod | Usage | Type | Coordonnées (adapter VERSION) |
|-----|-------|------|-------------------------------|
| **JEI** | Recettes in-game | compileOnly + runtimeOnly | `mezz.jei:jei-${minecraft_version}-forge:VERSION:api` / `mezz.jei:jei-${minecraft_version}-forge:VERSION` |
| **GeckoLib** | Mobs/items animés (modèles 3D) | implementation | `software.bernie.geckolib:geckolib-forge-${minecraft_version}:VERSION` |
| **Curios** | Slots d'équipement custom | compileOnly + runtimeOnly | `top.theillusivec4.curios:curios-forge:VERSION:api` / `top.theillusivec4.curios:curios-forge:VERSION` |
| **Patchouli** | Livres de doc in-game | compileOnly + runtimeOnly | `vazkii.patchouli:Patchouli:${minecraft_version}-VERSION:api` |
| **Jade** | Info-bulles sur les blocs | compileOnly | `snownee.jade:Jade-${minecraft_version}-forge:VERSION:api` |
| **The One Probe** | Informations sur blocs/entités | compileOnly | `mcjty.theoneprobe:theoneprobe-${minecraft_version}:VERSION:api` |

---

## Étapes

### 1. Identifier la dépendance

- **Nom exact + version** : chercher sur CurseForge, Modrinth, Maven Central, ou le README du mod
- **Type** : mod Forge, lib Java, ou API pure ?
- **Obligatoire ou optionnelle** (soft vs hard dependency) ?

### 2. Trouver les coordonnées Maven

```bash
# Maven Central
# → https://search.maven.org (chercher par groupe:artifactId)

# Mods Forge — souvent sur le repo du mod ou CurseForge Maven
# → https://cursemaven.com (tous les mods CurseForge)
# Format CurseForge : curse.maven:mod-slug-PROJECTID:FILEID
```

Si via CurseForge Maven, ajouter le repository dans `build.gradle` :

```groovy
repositories {
    mavenCentral()
    maven { name = 'MinecraftForge';  url = 'https://maven.minecraftforge.net' }
    maven { name = 'CurseMaven';      url = 'https://cursemaven.com' }          // ← ajouter si besoin
    maven { name = 'ModMaven';        url = 'https://modmaven.dev' }            // ← alternative
    maven { name = 'BlameJared';      url = 'https://maven.blamejared.com' }    // ← JEI, Patchouli
    maven { name = 'TheillusiveC4';   url = 'https://maven.theillusivec4.top' } // ← Curios
}
```

### 3. Ajouter dans build.gradle

Localiser la section `// DÉPENDANCES EXTERNES` dans `build.gradle` et ajouter :

```groovy
// [Nom du mod/lib] — [description courte de pourquoi]
implementation fg.deobf("groupe:artefact:version")
```

### 4. Mettre à jour mods.toml (si mod Forge uniquement)

Ajouter dans `src/main/resources/META-INF/mods.toml` :

```toml
[[dependencies.petassegang_addons]]
    modId        = "nom_du_mod"
    mandatory    = false          # true si requis, false si optionnel
    versionRange = "[VERSION,)"
    ordering     = "NONE"
    side         = "BOTH"         # ou "CLIENT" / "SERVER" selon le cas
```

### 5. Créer un wrapper d'intégration (soft dependency recommandé)

Si la dépendance est optionnelle (`mandatory = false`), isoler l'intégration :

```
src/main/java/com/petassegang/addons/compat/
└── jei/
    └── JEICompat.java
```

```java
package com.petassegang.addons.compat.jei;

// Chargé uniquement si JEI est présent
public final class JEICompat {
    public static void register() {
        // Intégration JEI ici
    }
    private JEICompat() {}
}
```

Dans `PetasseGangAddonsMod.commonSetup()` :

```java
import net.minecraftforge.fml.ModList;

// Dans commonSetup() :
if (ModList.get().isLoaded("jei")) {
    JEICompat.register();
    ModConstants.LOGGER.info("JEI integration enabled");
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
- [ ] Ligne de dépendance ajoutée dans la section `DÉPENDANCES EXTERNES`
- [ ] `mods.toml` mis à jour (si mod Forge)
- [ ] Package `compat/<mod>/` créé si soft dependency
- [ ] `ModList.get().isLoaded()` check si soft dependency
- [ ] `./gradlew build` passe sans erreur
- [ ] Intégration testée en jeu (ou test unitaire ajouté)
- [ ] `docs/ARCHITECTURE.md` mis à jour
- [ ] `docs/CHANGELOG.md` mis à jour
