# Dependances - PeTaSsE_gAnG_Additions

Ce document centralise les dependances externes du projet Backrooms,
leur role, leur mode d'inclusion, leur etat actuel dans le depot
et les points de vigilance avant activation.

---

## Etat actuel du projet

Le projet est deja prepare pour le setup des dependances Backrooms :

- le plugin `net.minecraftforge.jarjar` est active dans [build.gradle](../build.gradle) ;
- les repositories Maven de `GeckoLib`, `Patchouli`, `Keksuccino` et `Fusion` sont declares ;
- la structure `jarJar.register()` est en place ;
- les dependances `mods.toml` sont deja posees ;
- les artefacts JiJ sont encore commentes dans `build.gradle`, donc rien n'est embarque pour l'instant.

En clair :

- l'infrastructure JiJ est prete ;
- la documentation doit servir de source de verite pour activer les libs proprement ;
- l'activation effective doit se faire seulement avec des coordonnees et versions validees.

---

## Regles d'inclusion

| Type | Inclusion | Distribue dans le JAR | Presence requise chez le joueur |
|------|-----------|------------------------|---------------------------------|
| JiJ obligatoire | `jarJar` | Oui | Non |
| Soft dependency | `compileOnly` + detection runtime | Non | Oui si la feature amelioree est voulue |
| Outil de dev | Hors mod | Non | Non |

---

## Dependances obligatoires (JiJ)

Ces bibliotheques doivent voyager dans le JAR final du mod une fois activees.

### GeckoLib

| Propriete | Valeur |
|-----------|--------|
| Role | Animations 3D de toutes les entites custom Backrooms |
| Group | `software.bernie.geckolib` |
| Artifact cible | `geckolib-forge-26.1` a confirmer |
| Repository | `https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/` |
| Licence | MIT |
| Inclusion | JiJ |
| Etat actuel | Repository configure, dependance encore commentee dans `build.gradle` |

### Patchouli

| Propriete | Valeur |
|-----------|--------|
| Role | Livre in-game "Journal du Wanderer" |
| Group | `vazkii.patchouli` |
| Artifact | `Patchouli` |
| Repository | `https://maven.blamejared.com/` |
| Licence | Patchouli License |
| Inclusion | JiJ |
| Etat actuel | Repository configure, dependance encore commentee dans `build.gradle` |

Notes de design :

- le livre servira de bestiaire, guide, lore et notes terrain ;
- les pages devront se debloquer via advancements ;
- la doc de contenu de reference reste [BACKROOMS_CONTENT_RULES.md](./BACKROOMS_CONTENT_RULES.md).

### Konkrete

| Propriete | Valeur |
|-----------|--------|
| Role | Bibliotheque utilitaire requise par FancyMenu |
| Group | `de.keksuccino` |
| Artifact | `konkrete` |
| Repository | `https://maven.keksuccino.de/` |
| Licence | GPLv3 |
| Inclusion | JiJ |
| Etat actuel | Repository configure, dependance encore commentee dans `build.gradle` |

### FancyMenu

| Propriete | Valeur |
|-----------|--------|
| Role | Menu principal Backrooms et personnalisation HUD |
| Group | `de.keksuccino` |
| Artifact | `fancymenu` |
| Repository | `https://maven.keksuccino.de/` |
| Licence | DSMSLv3 |
| Inclusion | JiJ |
| Etat actuel | Repository configure, dependance encore commentee dans `build.gradle` |

Point de vigilance licence :

- verifier explicitement que l'embarquement JiJ reste conforme a la licence DSMSLv3 avant activation ;
- ne pas marquer cette dependance comme "embarquee et operationnelle" tant que la validation licence et la resolution Maven ne sont pas confirmees.

### Fusion

| Propriete | Valeur |
|-----------|--------|
| Role | Connected textures pour papier peint, moquette, plafond et autres blocs Backrooms |
| Group | `com.supermartijn642` |
| Artifact | `fusion` |
| Repository | `https://maven.supermartijn642.com/releases/` |
| Licence | MIT |
| Inclusion | JiJ |
| Etat actuel | Repository configure, dependance encore commentee dans `build.gradle` |

Note technique :

- Fusion doit etre pilote via les ressources JSON du pack integre ;
- aucune logique Java dediee n'est attendue pour les connected textures.

---

## Dependances optionnelles (soft-dep)

Ces bibliotheques ne doivent pas etre embarquees dans le JAR du mod.

### Immersive Portals

| Propriete | Valeur |
|-----------|--------|
| Role | Portails seamless et geometrie non-euclidienne si disponible |
| Mode | Soft dependency |
| Inclusion | `compileOnly` |
| Detection runtime | `ModList.get().isLoaded("imm_ptl_core")` |
| Declaration `mods.toml` | Oui |
| Etat actuel | Entree `mods.toml` deja presente, coordonnee `compileOnly` encore absente |

Comportement attendu :

- si present : activer les portails seamless et les espaces non-euclidiens prevus ;
- si absent : fallback sur teleports silencieux et effets VHS.

### Oculus

| Propriete | Valeur |
|-----------|--------|
| Role | Support shaderpack Backrooms si le joueur l'a installe |
| Mode | Soft dependency |
| Inclusion | `compileOnly` |
| Detection runtime | `ModList.get().isLoaded("oculus")` |
| Declaration `mods.toml` | Oui |
| Etat actuel | Entree `mods.toml` deja presente, coordonnee `compileOnly` encore absente |

Comportement attendu :

- sauvegarder le shader actuel du joueur ;
- appliquer le preset Backrooms a l'entree d'une dimension Backrooms ;
- restaurer le shader precedent a la sortie ;
- fallback propre si Oculus est absent.

---

## Effets sans dependance

Ces effets doivent fonctionner sans mod externe cote joueur, via le contenu du mod :

- distorsion VHS ;
- vignettage dynamique ;
- chromatic aberration ;
- grain visuel ;
- ambiance sonore native Forge via `SoundEvent`.

Cela signifie que les soft-deps ne doivent jamais devenir obligatoires pour le rendu de base.

---

## Outils de developpement

Ces outils ne sont pas des dependances runtime du mod :

| Outil | Usage |
|------|-------|
| Blockbench | Modelisation 3D et animations GeckoLib |

Ils ne doivent apparaitre ni en `jarJar`, ni en `compileOnly`, ni dans `mods.toml`.

---

## Build.gradle - etat attendu

Le fichier [build.gradle](../build.gradle) doit respecter ce schema :

- `jarJar.register()` actif ;
- repositories declares ;
- dependances JiJ commentees tant que les versions exactes ne sont pas confirmees ;
- dependances optionnelles en `compileOnly` uniquement ;
- aucune dependance optionnelle embarquee par erreur.

Situation actuelle :

- conforme pour l'infrastructure ;
- incomplet pour l'activation reelle des bibliotheques.

---

## mods.toml - etat attendu

Le fichier [mods.toml](../src/main/resources/META-INF/mods.toml) declare deja :

- `geckolib`
- `patchouli`
- `konkrete`
- `fancymenu`
- `fusion`
- `imm_ptl_core`
- `oculus`

Point de vigilance :

- tant que les JiJ ne sont pas actives dans le build, les dependances embarquees ne doivent pas etre presentees comme effectivement incluses ;
- le statut `mandatory` doit rester coherent avec l'etat reel du packaging.

---

## Activation d'une dependance JiJ

Checklist avant de decommenter une dependance :

1. Verifier que le repository repond.
2. Verifier la version exacte compatible Minecraft `26.1` / Forge `62`.
3. Verifier la licence pour redistribution JiJ.
4. Activer la dependance dans `build.gradle`.
5. Mettre a jour `mods.toml` si le statut change.
6. Relancer `./gradlew build`.
7. Verifier que le JAR final contient bien la bibliotheque attendue.
8. Mettre a jour [CHANGELOG.md](./CHANGELOG.md) et cette doc.

---

## Verdict actuel

Au 2026-04-11, la documentation et la structure du projet sont pretes pour les dependances Backrooms, mais :

- les JiJ ne sont pas encore activees ;
- les soft-deps ne sont pas encore ajoutees en `compileOnly` ;
- la validation reelle des coordonnees Maven reste a faire avant installation.
