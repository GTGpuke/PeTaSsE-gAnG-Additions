# Dépendances — PeTaSsE_gAnG_Additions

Ce document centralise les dépendances externes du projet Backrooms,
leur rôle, leur mode d'inclusion, leur état actuel et les points de vigilance avant activation.

---

## État actuel du projet

Le projet est sur Fabric 1.21.1. Les dépendances Backrooms ne sont pas encore activées dans le build :

- les bibliothèques sont déclarées en `suggests` dans `fabric.mod.json` (soft-dep optionnel) ;
- aucune dépendance externe n'est embarquée dans le JAR pour l'instant ;
- la documentation sert de source de vérité pour l'activation future.

---

## Règles d'inclusion Fabric

| Type | Inclusion | Distribué dans le JAR | Présence requise chez le joueur |
|------|-----------|------------------------|---------------------------------|
| JiJ (Jar-in-Jar) | `include` dans `build.gradle` | Oui | Non |
| Soft dependency | `suggests` dans `fabric.mod.json` | Non | Oui si la feature améliorée est voulue |
| Outil de dev | Hors mod | Non | Non |

---

## Dépendances obligatoires (JiJ à venir)

Ces bibliothèques devront voyager dans le JAR final une fois activées.

### GeckoLib

| Propriété | Valeur |
|-----------|--------|
| Rôle | Animations 3D de toutes les entités custom Backrooms |
| Group | `software.bernie.geckolib` |
| Artifact cible | `geckolib-fabric-1.21.1` (à confirmer) |
| Repository | `https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/` |
| Licence | MIT |
| Inclusion | JiJ (`include`) |
| État actuel | Non activé |

### Patchouli

| Propriété | Valeur |
|-----------|--------|
| Rôle | Livre in-game "Journal du Wanderer" |
| Group | `vazkii.patchouli` |
| Artifact | `Patchouli` (version Fabric à confirmer) |
| Repository | `https://maven.blamejared.com/` |
| Licence | Patchouli License |
| Inclusion | JiJ |
| État actuel | Non activé |

Notes de design :
- le livre servira de bestiaire, guide, lore et notes terrain ;
- les pages devront se débloquer via advancements ;
- la doc de contenu de référence reste [BACKROOMS_CONTENT_RULES.md](./BACKROOMS_CONTENT_RULES.md).

---

## Dépendances optionnelles (soft-dep)

Ces bibliothèques ne doivent pas être embarquées dans le JAR du mod.

### Immersive Portals

| Propriété | Valeur |
|-----------|--------|
| Rôle | Portails seamless et géométrie non-euclidienne si disponible |
| Mode | Soft dependency |
| Inclusion | `compileOnly` |
| Détection runtime | `FabricLoader.getInstance().isModLoaded("imm_ptl_core")` |
| Déclaration `fabric.mod.json` | `suggests` |
| État actuel | Déclaré dans `suggests`, `compileOnly` pas encore ajouté |

Comportement attendu :
- si présent : activer les portails seamless et les espaces non-euclidiens prévus ;
- si absent : fallback sur téléports silencieux et effets VHS.

### Iris Shaders (remplace Oculus en Fabric)

| Propriété | Valeur |
|-----------|--------|
| Rôle | Support shaderpack Backrooms si le joueur l'a installé |
| Mode | Soft dependency |
| Inclusion | `compileOnly` |
| Détection runtime | `FabricLoader.getInstance().isModLoaded("iris")` |
| État actuel | Non déclaré |

Comportement attendu :
- sauvegarder le shader actuel du joueur ;
- appliquer le preset Backrooms à l'entrée d'une dimension Backrooms ;
- restaurer le shader précédent à la sortie ;
- fallback propre si Iris est absent.

---

## Effets sans dépendance

Ces effets doivent fonctionner sans mod externe côté joueur, via le contenu du mod :

- distorsion VHS ;
- vignettage dynamique ;
- chromatic aberration ;
- grain visuel ;
- ambiance sonore native via `SoundEvent`.

Les soft-deps ne doivent jamais devenir obligatoires pour le rendu de base.

---

## Outils de développement

| Outil | Usage |
|------|-------|
| Blockbench | Modélisation 3D et animations GeckoLib |

Ne doivent apparaître ni en JiJ, ni en `compileOnly`, ni dans `fabric.mod.json`.

---

## Activation d'une dépendance JiJ (Fabric)

Checklist avant d'activer une dépendance :

1. Vérifier que le repository répond.
2. Vérifier la version exacte compatible Minecraft 1.21.1 / Fabric.
3. Vérifier la licence pour redistribution JiJ.
4. Ajouter dans `build.gradle` :
   ```groovy
   modImplementation include("group:artifact:version")
   ```
5. Mettre à jour `fabric.mod.json` si le statut `suggests` change en `depends`.
6. Relancer `./gradlew build`.
7. Vérifier que le JAR final contient bien la bibliothèque attendue.
8. Mettre à jour [CHANGELOG.md](./CHANGELOG.md) et cette doc.

---

## Verdict actuel

Au 2026-04-15, la documentation est prête pour les dépendances Backrooms, mais :

- aucun JiJ n'est encore activé ;
- les soft-deps ne sont pas encore ajoutés en `compileOnly` ;
- la validation réelle des coordonnées Maven Fabric reste à faire avant installation.
