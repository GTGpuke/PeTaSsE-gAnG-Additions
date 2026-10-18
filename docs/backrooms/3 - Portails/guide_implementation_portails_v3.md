# Guide d'implémentation — Portails Seamless pour Mod Backrooms

## Fabric 1.21.1 · Architecture ImmersivePortals adaptée à l'arborescence v4

> **Version** : v3 — remplace le guide précédent ET la spec `spec_veil_portail_shaders_V4_3.md` pour tout ce qui concerne le Pilier 3 (Portail). Ajout §24 (mécanismes avancés IP) + Codex Contract I-13/I-14/I-15 + DoD Phase 4C renforcé.
> **Compagnon** : `arborescence_v4.md` (autorité supérieure — règles d'or, contrats, anti-patterns).
> **Cible** : Minecraft 1.21.1 · Fabric · Java 21 · Veil 3.x (pour shaders uniquement) · architecture inspirée d'`iPortalTeam/ImmersivePortalsMod` (**Apache License 2.0**).

---

## Table des matières

**Préambule**
- §0. Pourquoi ce document
- §0.5. Conventions de lecture (PSEUDO-CODE vs CODE TEMPLATE)

**Contrat Codex (lecture obligatoire avant tout code)**
- §1. Codex Contract — interdictions, ordre des fichiers, definitions of done

**Décisions et patches**
- §2. Décisions architecturales tranchées
- §3. Patches à appliquer à `arborescence_v4.md`
- §4. Mapping classes ImmersivePortals → arborescence v4

**Architecture technique**
- §5. Source of Truth — le portail comme Entity
- §6. ClientWorldRegistry — multi-dimension côté client
- §7. Rendu stencil — PortalStencilRenderer
- §8. Récursion à 1 niveau — PortalDepthTracker
- §9. Caméra distante — RemoteCamera + RemoteCameraGuard
- §10. Téléportation — threading et masquage (refondu)
- §11. Chargement des chunks destination — DestinationLoader
- §12. Mixins — emplacements et points d'injection
- §13. Veil — ce qui reste, ce qui disparaît
- §14. Machine d'état des portails
- §15. Audio, entités, aggro, couloirs bouclés (résumés)

**Plan de dev**
- §16. Phase 0 — spikes vérifiables (fichier, log, fallback)
- §17. Phases 1-10 (alignées arbo §12)

**Validation et risques**
- §18. Matrice de risques techniques
- §19. Vérifications mécaniques (greps obligatoires)
- §20. Points de vigilance critiques

**Annexes**
- §21. Dépendances Gradle
- §22. Licence — copier ImmersivePortals
- §23. Résumé exécutif
- §24. Analyse des mécanismes avancés — problèmes résolus par la référence IP

---

## §0. Pourquoi ce document

L'ancienne spec `spec_veil_portail_shaders_V4_3.md` reposait entièrement sur **Veil** pour le rendu offscreen des portails. Cette approche est **abandonnée pour le Pilier 3** (Portail) :

- L'API Veil change entre versions majeures — la Phase 0 imposait un spike entier juste pour valider que les primitives existent
- Réinventer ce qu'`ImmersivePortals` fait depuis des années (framebuffers offscreen, rendu monde distant, gestion multi-dimension client-side) est un risque inutile
- ImmersivePortals est sous **Apache License 2.0**, production-tested sur plusieurs versions Minecraft, compatible Sodium/Iris

**Décision :** copier/adapter l'architecture ImmersivePortals pour le rendu portail. **Veil reste pour le Pilier 2** (shaders custom Backrooms : vignette, distortion, glitch, flicker, masquage TP).

> **Hiérarchie d'autorité (mise à jour)** :
> 1. `arborescence_v4.md` — contrats, règles d'or, arbo canonique
> 2. **Ce document** (`guide_implementation_portails_v3.md`) — Pilier 3 (Portail)
> 3. `spec_shaders_backrooms_V2.md` — Pilier 2 (Shader)
> 4. `spec_veil_portail_shaders_V4_3.md` — **DÉPRÉCIÉ pour le Pilier 3**, gardé en référence pour philosophie/lois/SoT
> 5. Demande utilisateur courante
> 6. Conventions Fabric/Java standard

---

## §0.5. Conventions de lecture

Ce document mélange **architecture cible** (description d'un système à construire) et **squelettes de code** (Java prêt à compiler ou pseudo-code illustratif). Les blocs de code sont **explicitement marqués** :

| Marqueur en commentaire de tête | Sens | Action attendue de Codex |
|---|---|---|
| `// PSEUDO-CODE — DO NOT COPY AS-IS` | Pseudo-code illustratif. Méthodes inventées, signatures probables, ordre logique correct mais détails à valider. | **Ne pas copier-coller.** Reconstruire à partir du code IP réel et de l'API Yarn 1.21.1 réelle. |
| `// CODE TEMPLATE — MUST COMPILE` | Squelette de classe/interface qui DOIT compiler tel quel après remplissage des `// SPIKE-X.Y` listés en §16. | Copier la structure, remplacer chaque `// SPIKE-X.Y` par le code validé par le spike correspondant. |
| `// SPIKE-X.Y` | Référence à un spike Phase 0 spécifique (cf. §16). Le bloc dépendant est non-décidable hors spike. | **Ne pas inventer.** Bloquer si le spike n'est pas validé. |
| `// FALLBACK: <comportement>` | Comportement dégradé volontaire si une dépendance échoue (Loi 5, V4.3). | Implémenter explicitement, pas un `try/catch` muet. |
| `// FIXME(<ticket>):` | Dette technique connue. | Ne pas corriger en passant (AIP-2, arbo §16). |

**Règle absolue** : si un bloc n'a aucun de ces marqueurs, il est `// PSEUDO-CODE — DO NOT COPY AS-IS` par défaut. Codex doit demander confirmation (protocole arbo §21) avant de l'utiliser comme code de production.

> **Pour l'humain qui lit** : les blocs `PSEUDO-CODE` montrent l'intention architecturale. Les `CODE TEMPLATE` sont les vrais squelettes. Les `SPIKE-X.Y` sont les zones où l'humain doit avoir validé un spike avant que Codex puisse remplir.

---

## §1. Codex Contract

> **Cette section est la première lecture obligatoire pour toute IA agentique générant du code à partir de ce document.** Elle complète, sans la remplacer, la Partie E d'`arborescence_v4.md`.

### §1.1 Ordre obligatoire de lecture

Avant **toute action** de génération de code, lire dans cet ordre :

1. `arborescence_v4.md` — intégralement. Autorité supérieure (rang 1).
2. `arborescence_v4.md §6` — règles d'or (12 règles, dont la n°12 ajoutée par §3.5 de ce doc).
3. `arborescence_v4.md §7` — anti-patterns code (AP-1 à AP-9).
4. `arborescence_v4.md §16` — anti-patterns IA (AIP-1 à AIP-7).
5. **Ce document** — intégralement, dans l'ordre.
6. `docs/phase0_findings.md` — **doit exister** pour toute génération de code Phase ≥ 1.

Si `phase0_findings.md` n'existe pas et la demande concerne Phase ≥ 1 : **refuser** et rediriger vers Phase 0 (cf. arbo §18.2).

### §1.2 Interdictions absolues (vérifiables par grep)

| # | Interdit | Vérification |
|---|---|---|
| **I-1** | Recréer la classe `PortalLink` (supprimée DA-2). | `grep -rE "class\s+PortalLink\b\|record\s+PortalLink\b" src/main/java` → vide. |
| **I-2** | Recréer la classe `PortalFramebuffer` (supprimée DA-1). | `grep -rE "class\s+PortalFramebuffer\b" src/main/java` → vide. |
| **I-3** | Recréer la classe `PortalRenderTarget` (supprimée DA-1). | `grep -rE "class\s+PortalRenderTarget\b" src/main/java` → vide. |
| **I-4** | Importer Veil/Iris hors de `backrooms/bridge/`. | `grep -rE "import (foundry\.veil\|net\.coderbot\.iris\|net\.irisshaders)" --include="*.java" src/main/java \| grep -v "/bridge/"` → vide. |
| **I-5** | Utiliser Veil pour le rendu de la vue distante d'un portail. | `grep -rE "VeilFramebufferBridge\|VeilPostProcessingBridge" src/main/java/com/gtgpuke/gtgsbackrooms/backrooms/portal/` → vide (le portail n'utilise pas Veil ; Veil est pour shaders/post-FX uniquement, cf. §13). |
| **I-6** | Appeler `TeleportExecutor.execute()` depuis le render thread (client) ou un WorldRenderEvent. | `grep -rE "WorldRenderEvents.*\.register" src/main/java \| xargs grep -l "TeleportExecutor"` → vide. **Le TP s'exécute uniquement via `server.execute()` ou directement dans un `ServerTickEvents` callback** (cf. §10.3). |
| **I-7** | Appeler `subsystem.tick()` hors de `PortalManager.dispatch()`. | `grep -rnE "\.tick\(" src/main/java/com/gtgpuke/gtgsbackrooms/backrooms/portal/ \| grep -v "PortalManager.java"` → uniquement les déclarations de méthode `void tick(...)`, jamais d'invocation. |
| **I-8** | Allouer une ressource OpenGL dans un constructeur. | Revue manuelle : `glGen*`, `glCreate*`, `new Framebuffer(`, `new Texture(` ne doivent jamais apparaître dans un corps de constructeur. Toujours dans `init()`. |
| **I-9** | Créer un `BackroomsPortalEntity` hors de : `LoopingCorridorManager`, `PortalManager`, ou un spawn explicite via `Level.addFreshEntity` lié à un `RoomGraph` registration. | Revue manuelle. Pas de `new BackroomsPortalEntity(` ailleurs. |
| **I-10** | Synchroniser l'état d'un portail (`PortalState`) ou un Memory Module hors de `TransitionSystem` / `AggroSimulationSystem`. | Revue manuelle. Lecture seule partout ailleurs (règle d'or n°5, AP-2). |
| **I-11** | Inventer une méthode/classe Yarn 1.21.1 dont la signature n'est pas validée par un spike Phase 0. Tout symbole non-vanilla-évident est suspect. | Tout bloc avec `// SPIKE-X.Y` ou `// SPEC-GAP:` non résolu = code non-livrable. |
| **I-12** | Activer `BackroomsConfig.enableBackroomsFeature = true` automatiquement. | Revue manuelle. Reste à `false` ; commit séparé utilisateur (arbo §12 verrou Phase 0). |
| **I-13** | Écrire le mot `ImmersivePortals` dans le code Java source hors headers d'attribution Apache-2.0. | `grep -rn "ImmersivePortals" --include="*.java" src/main/java \| grep -v "Adapted from iPortalTeam"` → vide. Les headers contenant `Adapted from iPortalTeam/ImmersivePortalsMod` sont seuls autorisés (§22). |
| **I-14** | Effectuer le swap `Minecraft.level` / `levelRenderer` / `lightmap` hors d'un bloc `try/finally` garantissant la restauration. | Revue manuelle : toute méthode appelant `backrooms$setWorldRenderer()` ou `ip_setLightmapTextureManager()` ou un équivalent doit avoir un `finally { /* restauration */ }` unconditional. Sinon un crash pendant le rendu distant laisse le client dans la fausse dimension. |
| **I-15** | Rendre le joueur local (`LocalPlayer`) via `EntityEchoSystem` ou `CrossPortalEntityRenderer` en V1. | `grep -rn "LocalPlayer\|client\.player" src/main/java/com/gtgpuke/gtgsbackrooms/backrooms/portal/client/entity_echo/` → vide (sauf lectures read-only nécessaires comme `getPos()`). Le joueur local n'est jamais rendu comme entité cross-dim en V1 (§24.2). |

Si Codex détecte qu'il s'apprête à violer une de ces interdictions, il **doit** appliquer le protocole arbo §21 (demande de clarification).

### §1.3 Definitions of Done par phase

Une phase n'est validée que si **tous** les critères ci-dessous passent. Codex ne propose pas de passer à la phase suivante tant qu'un critère est ❌.

#### Phase 0 (DoD)

- [ ] `docs/phase0_findings.md` existe.
- [ ] Les 11 spikes §16 ont chacun une section dans le findings : méthode utilisée, log de sortie observé, décision (`OK` / `FALLBACK appliqué` / `BLOQUÉ`).
- [ ] Aucun spike n'est en `BLOQUÉ` sans plan de contournement écrit.
- [ ] `./gradlew build` passe.
- [ ] Le mod lance MC sans crash en `runClient`.
- [ ] `BackroomsConfig.enableBackroomsFeature = false` (verrou maintenu).

#### Phase 1 (DoD)

- [ ] Tous les modules de §17 Phase 1 sont créés aux bons emplacements arbo.
- [ ] `./gradlew build` passe.
- [ ] `runClient` lance le mod, log montre l'ordre Lifecycle (init → start) pour : `LifecycleManager`, `BackroomsConfigManager`, `BridgeHealthMonitor`, les 5 Veil bridges, `IrisBridge`, `BackroomsFeature`.
- [ ] Log présent : `[Backrooms] Feature désactivée — Phase 0 non validée` (sauf si l'utilisateur a fait le commit `chore: unlock Backrooms after Phase 0`).
- [ ] Greps I-1 à I-12 verts.

#### Phase 4 (DoD — premier portail rendu) ⭐

- [ ] Modules §17 Phase 4 créés (12 modules listés en §17.3).
- [ ] `./gradlew build` passe.
- [ ] Commande debug `/backrooms portal spawn_test` créée et fonctionnelle.
- [ ] L'invocation crée un `BackroomsPortalEntity` à 5 blocs devant le joueur, dimension destination = même dimension, destination = position 1000 blocs plus loin.
- [ ] Le joueur **voit** la vue distante dans le rectangle du portail (chunks de la destination visibles).
- [ ] Le joueur ne traverse **pas** (pas de TP en Phase 4).
- [ ] Aucun crash quand on s'éloigne / se rapproche / regarde sous un angle oblique (RemoteCameraGuard fait son travail).
- [ ] Greps I-1 à I-12 verts.
- [ ] Récursion testée : un 2e portail dans la vue du 1er → 2e affiche fallback bruit (DA-3, règle d'or n°12).

#### Phase 5 (DoD — TP frame-masqué)

- [ ] `TeleportExecutor` n'est invocable que depuis `ServerTickEvents` ou `server.execute()` (vérifié par grep I-6).
- [ ] Le joueur traverse le portail debug → `TransitionMasker` (shader Veil) couvre la transition.
- [ ] Pas de mismatch visible > 1 frame (subjectif, à filmer en debug).
- [ ] Cooldown anti-double-TP fonctionne (joueur ne re-TP pas immédiatement s'il fait demi-tour dans la zone).

#### Phases 6-10 (DoD)

Critères dans §17 et arbo §12. Chaque milestone arbo doit être démontrée jouable avant phase suivante.

### §1.4 Format de réponse de Codex

Quand Codex propose une modification, il structure sa réponse selon arbo §19 :

```
## Plan
<3-5 étapes max>

## Fichiers touchés
- <fichier> : <action>

## Règles vérifiées
- Règle d'or n°<X> : <comment respecté>
- Codex Contract I-<Y> : <comment respecté>
- Anti-pattern AP-<Z> : <comment évité>

## Spikes Phase 0 référencés
- SPIKE-<X.Y> : <statut OK/FALLBACK/BLOQUÉ dans phase0_findings.md>

## Code

[blocs marqués CODE TEMPLATE — MUST COMPILE ou PSEUDO-CODE — DO NOT COPY AS-IS]

## Vérifications mécaniques
- [ ] `./gradlew build`
- [ ] runClient
- [ ] Greps §19 : I-1, I-4, I-7, ... (les pertinents)

## Suite suggérée
<prochaine action logique, sans la faire>
```

---

## §2. Décisions architecturales tranchées

| # | Décision | Conséquence |
|---|----------|-------------|
| **DA-1** | **Veil hybride** : Veil pour shaders custom et post-FX uniquement. Stencil + Mixin LevelRenderer pour le rendu portail. | `Pilier 2` Veil-natif inchangé. `Pilier 3 view/` n'utilise plus `VeilFramebufferBridge` pour le portail. |
| **DA-2** | **Portal Entity = SoT** (modèle ImmersivePortals). NBT-persisted, sync entity vanilla, ticking serveur. | `portal/shared/PortalLink.java` est **supprimé**. `PortalContext.activePortal` change de type : `PortalLink` → `BackroomsPortalEntity`. |
| **DA-3** | **Récursion autorisée à 1 niveau (`maxPortalLayer = 2`)**. Au-delà, fallback bruit/flou. | `PortalDepthTracker.maxRecursionDepth = 1` (compteur 0-indexé : profondeur 0 = monde réel, 1 = vue distante d'un portail ; profondeur 2 → fallback). |
| **DA-4** | Code ImmersivePortals **copié et adapté** (Apache License 2.0), pas en dépendance Maven. | Pas de bridge `ImmersivePortalsBridge`. Le code IP vit dans `backrooms/portal/*` aux emplacements définis en §4. |
| **DA-5** | **`BackroomsPortalEntity` est instanciée par `PortalManager`** (ou via spawn `Level.addFreshEntity` pour les portails de monde, mais référencées dans `RoomGraph`). | Reste compatible avec règle d'or n°7 (PortalSubsystem package-private). L'Entity **n'est pas** un PortalSubsystem. |
| **DA-6** | `RoomGraph` reste SoT topologique, **mais référence les portails par `UUID`** (= UUID de l'Entity), plus par `PortalLink`. | `RoomSegment.exits()` retourne `List<UUID>` au lieu de `List<PortalLink>`. Résolution via `BackroomsPortalEntityRegistry`. |
| **DA-7** | `BackroomsPortalEntityRegistry` est SoT pour la résolution `UUID → BackroomsPortalEntity`. | Nouveau singleton, vit dans `portal/entity/` (nouveau sous-package). |
| **DA-8** | **Mod ID, packages, conventions arbo intégralement préservés.** | `com.gtgpuke.gtgsbackrooms.backrooms.*`, `gtgs_backrooms:*`, etc. |

---

## §3. Patches à appliquer à `arborescence_v4.md`

Les changements suivants doivent être commit dans `arborescence_v4.md` **avant** d'écrire le code Pilier 3. Ils sont la conséquence directe des décisions §2.

### §3.1 Sous-packages ajoutés

```diff
  backrooms/portal/                                  # Pilier 3 — COMMON
  │
  ├── PortalManager.java
  ├── PortalContext.java
  ├── PortalSubsystem.java
  ├── PortalPriorityScore.java
+ │
+ ├── entity/                                        # ─── NOUVEAU (DA-2, DA-7) ───
+ │   ├── BackroomsPortalEntity.java                # @SourceOfTruth (per-portal data)
+ │   ├── BackroomsPortalEntityType.java            # EntityType<BackroomsPortalEntity>
+ │   ├── BackroomsPortalEntityRegistry.java        # @SourceOfTruth (UUID → entity)
+ │   ├── PortalShape.java                          # record (axisW, axisH, width, height)
+ │   └── PortalTransform.java                      # record (rotation DQuaternion, scaling)
  │
  ├── shared/
  │   ├── PortalState.java                          # enum 6 états — INCHANGÉ
- │   ├── PortalLink.java                           # SUPPRIMÉ (DA-2)
- │   ├── PortalId.java                             # SUPPRIMÉ (DA-2 — UUID Entity remplace)
  │   └── PortalEvent.java
  │
  ├── transition/
  │   ├── TransitionSystem.java
  │   ├── ThresholdDetector.java
  │   ├── TeleportExecutor.java
  │   ├── TransitionContext.java
  │   ├── PortalStateMachine.java
  │   ├── FrameSyncPolicy.java
  │   └── DestinationLoader.java
  ...
  └── client/
      ├── view/
      │   ├── PortalViewSystem.java                 # implements PortalSubsystem
+     │   ├── PortalStencilRenderer.java            # ─── NOUVEAU (DA-1) ───
+     │   ├── ViewAreaRenderer.java                 # ─── NOUVEAU — quad du portail
+     │   ├── RemoteWorldRenderInfo.java            # ─── NOUVEAU — équivalent IP WorldRenderInfo
      │   ├── RemoteCamera.java
      │   ├── RemoteCameraGuard.java
-     │   ├── PortalFramebuffer.java                # SUPPRIMÉ (DA-1 — plus de framebuffer Veil pour portail)
      │   ├── PortalSurfaceRenderer.java            # rôle réduit : fallback bruit/flou
-     │   ├── PortalRenderTarget.java               # SUPPRIMÉ (DA-1)
      │   └── PortalDepthTracker.java               # déjà prévu — limite récursion §7
      │
+     ├── world/                                    # ─── NOUVEAU (DA-1, copié IP) ───
+     │   ├── ClientWorldRegistry.java              # @SourceOfTruth (dim → ClientLevel)
+     │   ├── ClientWorldFactory.java               # création des ClientLevel distants
+     │   └── RemoteLevelRenderer.java              # un LevelRenderer par dimension
      │
      ├── audio/
      │   └── ... (inchangé)
      ├── entity_echo/
      │   └── ... (inchangé)
      └── effect/
          └── ... (inchangé)
```

### §3.2 `mixin/` — emplacements ajoutés

```diff
  mixin/
  ├── client/
+ │   ├── MixinLevelRenderer.java                   # injection rendu portail (§11.1)
+ │   ├── MixinClientLevel.java                     # access aux entités cross-dim
+ │   └── MixinMinecraftClient.java                 # accesseur framebuffer principal
  ├── common/
+ │   ├── MixinServerChunkCache.java                # tickets chunk destination (§11.2)
+ │   └── MixinServerLevel.java                     # tick TP serveur
  └── access/
      └── ... (accessors générés au besoin)
```

### §3.3 SoT — table mise à jour

```diff
  | Donnée | SoT |
  |--------|-----|
- | État courant d'un portail (valeur runtime) | TransitionSystem ; type partagé : PortalState |
+ | Données d'un portail (position, taille, dim, rotation, scaling) | BackroomsPortalEntity (@SoT) — NBT-persisted |
+ | État courant d'un portail (lifecycle) | TransitionSystem (@SoT) ; type partagé : PortalState |
+ | Résolution UUID → BackroomsPortalEntity | BackroomsPortalEntityRegistry (@SoT) |
+ | ClientLevel par dimension (distantes) | ClientWorldRegistry (@SoT, CLIENT-only) |
- | Topologie des salles | RoomGraph |
+ | Topologie des salles (UUIDs de portails) | RoomGraph |
```

### §3.4 Règle d'or n°1 — clarification

La règle d'or n°1 (« aucun import Veil/Iris hors de `backrooms/bridge/` ») est **inchangée**. Le code copié d'ImmersivePortals **n'importe rien** de Veil/Iris (il utilise OpenGL direct via LWJGL et l'API Minecraft vanilla), donc cette règle reste vérifiable telle quelle :

```bash
grep -rE "import (foundry\.veil|net\.coderbot\.iris|net\.irisshaders)" \
     --include="*.java" src/main/java | grep -v "/bridge/"
# Doit retourner vide.
```

Le code OpenGL direct (LWJGL, `org.lwjgl.opengl.GL11`, etc.) **est autorisé partout** — ce n'est pas Veil ni Iris.

### §3.5 Règle d'or n°12 — nouvelle (récursion)

Ajout à `arborescence_v4.md §6` :

> **12. Aucun rendu portail ne dépasse `PortalDepthTracker.maxRecursionDepth` (= 1).**
> Vérifiable : `PortalStencilRenderer.doRenderPortal()` doit appeler `PortalDepthTracker.canRender()` avant tout incrément GL_INCR du stencil. Au-delà, fallback bruit/flou via `PortalSurfaceRenderer.renderFallback()`.

---

## §4. Mapping classes ImmersivePortals → arborescence v4

Ce tableau est la **traduction canonique**. Quand un fichier IP est mentionné dans le code copié, il devient le fichier de droite.

| Classe ImmersivePortals (réf. repo) | Chemin dans l'arbo v4 | Notes |
|---|---|---|
| `Portal extends Entity` | `backrooms/portal/entity/BackroomsPortalEntity` | Renommée. Préfixe `Backrooms*` (arbo §9.2). Constructeur `EntityType, Level` standard MC. |
| `PortalLike` (interface) | `backrooms/portal/entity/BackroomsPortalEntity` (méthodes intégrées) | Interface non extraite — méthodes directement sur l'Entity. |
| `GlobalPortalStorage` | `backrooms/portal/entity/BackroomsPortalEntityRegistry` | Renommé pour clarté. Maintient le mapping `UUID → Entity` au niveau serveur. |
| `ClientWorldLoader` | `backrooms/portal/client/world/ClientWorldRegistry` | Renommé (« Loader » suggère un Lifecycle particulier ; « Registry » est plus juste). Implements `Lifecycle`. |
| `RendererUsingStencil` | `backrooms/portal/client/view/PortalStencilRenderer` | Renommé. Géré comme composant interne de `PortalViewSystem` — pas un `PortalSubsystem` lui-même. |
| `RendererUsingFrameBuffer` | **NON COPIÉ** | Approche framebuffer rejetée (DA-1). Stencil-only. Si Phase 0 révèle un problème stencil, ré-évaluer. |
| `PortalRenderer` (classe abstraite) | **NON COPIÉ** | Pas besoin d'abstraction — un seul renderer (stencil). |
| `ViewAreaRenderer` | `backrooms/portal/client/view/ViewAreaRenderer` | Dessine le quad du portail dans le stencil. Copié tel quel. |
| `WorldRenderInfo` | `backrooms/portal/client/view/RemoteWorldRenderInfo` | Renommé pour clarté. Record immutable. |
| `TransformationManager` | méthodes intégrées dans `backrooms/portal/client/view/RemoteCamera` | Pas de classe `*Manager` séparée — l'arbo a déjà `RemoteCamera`. |
| `ServerTeleportationManager` | `backrooms/portal/transition/TeleportExecutor` (logique) + tick côté `TransitionSystem` | La détection vit dans `ThresholdDetector`. L'exécution dans `TeleportExecutor`. **L'arbo a déjà ces classes — PAS de `ServerTeleportationManager` créé.** |
| `ClientTeleportationManager` | `backrooms/portal/client/effect/TransitionMasker` (animationDelta) | Le smooth post-TP via `animationDelta` vit dans le masker (qui utilise un shader Veil pour le fade). |
| `ImmPtlChunkTickets` | `backrooms/portal/transition/DestinationLoader` (logique tickets) | L'arbo a déjà `DestinationLoader`. Code IP copié dedans. |
| `CrossPortalSound` | `backrooms/portal/client/audio/CrossSpaceAudioSystem` (intégré) | L'arbo a déjà tout le pipeline audio. Code IP fournit la logique de reprojection. |
| `CrossPortalEntityRenderer` | `backrooms/portal/client/entity_echo/EntityEchoSystem` (intégré) | L'arbo a déjà le système. Code IP fournit le rendu read-only des entités cross-dim. |
| `PortalLureGoal` | `backrooms/portal/aggro/PortalLureGoal` | Direct, l'arbo a déjà ce nom. |
| `IPPortingLibCompat` | `backrooms/portal/client/view/StencilEnabler` | Renommé. Utilise un Mixin sur `RenderTarget` pour activer le stencil. |
| `PortalRendering` (statique, isRendering, depth) | `backrooms/portal/client/view/PortalDepthTracker` | L'arbo a déjà ce nom. Statique, scopé par frame. |
| `IEMinecraftClient`, `IECamera` (interfaces des Mixins) | `mixin/client/access/IMinecraftClientAccessor`, `ICameraAccessor` | Conventions Fabric. |
| `MixinLevelRenderer` | `mixin/client/MixinLevelRenderer` | Direct. |
| `MixinServerChunkCache` | `mixin/common/MixinServerChunkCache` | Direct. |
| `BackroomsMemoryModules` (constantes Memory) | `backrooms/portal/aggro/BackroomsMemoryModules` | Référencé dans le guide précédent. |
| `DQuaternion` (utilitaire math) | `util/math/DQuaternion` | Math pure, vit dans `util/math/` selon arbo. |

---

## §5. Source of Truth — le portail comme Entity

### §5.1 `BackroomsPortalEntity`

**Chemin** : `com.gtgpuke.gtgsbackrooms.backrooms.portal.entity.BackroomsPortalEntity`
**SoT pour** : position, dimension destination, taille, rotation, scaling, flags (teleportable, visible).
**Annotations** : `@SourceOfTruth`.
**Lifecycle** : non — l'Entity gère son propre cycle de vie via les hooks vanilla (spawn/discard).

```java
// CODE TEMPLATE — MUST COMPILE (après résolution des SPIKE-X.Y référencés)
package com.gtgpuke.gtgsbackrooms.backrooms.portal.entity;

import com.gtgpuke.gtgsbackrooms.core.annotation.SourceOfTruth;
import com.gtgpuke.gtgsbackrooms.util.math.DQuaternion;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.resource.ResourceKey;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Portail Backrooms — entité serveur sérialisée NBT, synchronisée client via le système Entity vanilla.
 *
 * SoT pour : position, dimension destination, taille, rotation, scaling, teleportable, visible.
 * NE possède PAS l'état lifecycle (PortalState) — celui-ci appartient à TransitionSystem (cf. §3.3 SoT).
 *
 * Adapté de iPortalTeam.ImmersivePortalsMod.Portal (Apache License 2.0, voir §22).
 *
 * FALLBACK: si l'entité est invalide (destination = null, taille = 0), le portail est ignoré
 * par PortalManager.scoreAndSelect() — log warning, jamais de crash.
 */
@SourceOfTruth
public class BackroomsPortalEntity extends Entity {

    // --- Géométrie du portail (plan rectangulaire) ---
    private double width = 0.0;
    private double height = 0.0;
    private Vec3d axisW = new Vec3d(1, 0, 0);   // vecteur "droite" du plan portail
    private Vec3d axisH = new Vec3d(0, 1, 0);   // vecteur "haut" du plan portail

    // --- Destination ---
    private @Nullable ResourceKey<World> dimensionTo;
    private @Nullable Vec3d destination;

    // --- Transformation ---
    private @Nullable DQuaternion rotation;     // null = identity
    private double scaling = 1.0;

    // --- Flags ---
    private boolean teleportable = true;
    private boolean visible = true;
    private boolean crossPortalCollisionEnabled = false; // V1 = false toujours (V4.3 §2)

    public BackroomsPortalEntity(EntityType<? extends BackroomsPortalEntity> type, World world) {
        super(type, world);
    }

    // --- Sérialisation NBT (vanilla MC) ---
    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        // SPIKE-0.11 + R-10 : valider NbtCompound.getDouble / getString signatures Yarn 1.21.1
        //                     et le Codec sérialisation pour ResourceKey<World> et DQuaternion
        this.width = nbt.getDouble("Width");
        this.height = nbt.getDouble("Height");
        // ... axisW, axisH, dimensionTo, destination, rotation, scaling, flags
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putDouble("Width", this.width);
        nbt.putDouble("Height", this.height);
        // ...
    }

    // --- Méthodes de transformation (utilisées partout : view, transition, audio, aggro) ---

    /** Position dans le monde destination correspondant à un point du monde source. */
    public Vec3d transformPoint(Vec3d point) {
        Vec3d relativeToPortal = point.subtract(this.getPos());
        Vec3d rotated = (rotation != null) ? rotation.rotate(relativeToPortal) : relativeToPortal;
        Vec3d scaled = rotated.multiply(scaling);
        return this.destination.add(scaled);
    }

    /** Vélocité transformée (rotation seule, sans translation). */
    public Vec3d transformVelocityRelativeToPortal(Vec3d velocity) {
        return (rotation != null) ? rotation.rotate(velocity) : velocity;
    }

    /** Test : un point est-il du côté "entrée" du portail ? */
    public boolean isInFrontOfPortal(Vec3d point) {
        Vec3d normal = axisW.crossProduct(axisH).normalize();
        return point.subtract(this.getPos()).dotProduct(normal) > 0;
    }

    /** Le projeté du point sur le plan portail tombe-t-il dans le rectangle width×height ? */
    public boolean isPointInPortalProjection(Vec3d point) {
        Vec3d relative = point.subtract(this.getPos());
        double w = relative.dotProduct(axisW);
        double h = relative.dotProduct(axisH);
        return Math.abs(w) <= width / 2.0 && Math.abs(h) <= height / 2.0;
    }

    /**
     * Inverse de transformPoint : point dans le monde destination → position correspondante dans le monde source.
     * Utilisé par SpatialSoundProjector pour reprojeter les sons distants.
     *
     * Inverse mathématique : destination → source =
     *   1. Soustraire portal.destination
     *   2. Appliquer rotation inverse (ou identity si null)
     *   3. Diviser par scaling
     *   4. Ajouter portal.getPos()
     */
    public Vec3d inverseTransformPoint(Vec3d destPoint) {
        Vec3d relToDest = destPoint.subtract(this.destination);
        Vec3d unrotated = (rotation != null) ? rotation.inverse().rotate(relToDest) : relToDest;
        Vec3d unscaled = (scaling != 0) ? unrotated.multiply(1.0 / scaling) : unrotated;
        return this.getPos().add(unscaled);
    }

    // --- Getters (lecture seule pour les sous-systèmes) ---
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public Vec3d getAxisW() { return axisW; }
    public Vec3d getAxisH() { return axisH; }
    public @Nullable ResourceKey<World> getDimensionTo() { return dimensionTo; }
    public @Nullable Vec3d getDestination() { return destination; }
    public @Nullable DQuaternion getRotation() { return rotation; }
    public double getScaling() { return scaling; }
    public boolean isTeleportable() { return teleportable; }
    public boolean isVisible() { return visible; }

    // --- Builder factory (utilisé par LoopingCorridorManager et spawn debug) ---
    // Voir pattern complet en §15.4. Usage : BackroomsPortalEntity.builder(level).position(...).build()
    public static Builder builder(World level) { return new Builder(level); }

    public static class Builder {
        private final World level;
        private Vec3d position, axisW = new Vec3d(1,0,0), axisH = new Vec3d(0,1,0);
        private double width, height, scaling = 1.0;
        private ResourceKey<World> destinationDim;
        private Vec3d destination;
        private @Nullable DQuaternion rotation;
        private boolean visible = true, teleportable = true;

        private Builder(World level) { this.level = level; }
        public Builder position(Vec3d p)               { this.position = p; return this; }
        public Builder axes(Vec3d w, Vec3d h)          { this.axisW = w; this.axisH = h; return this; }
        public Builder size(double w, double h)        { this.width = w; this.height = h; return this; }
        public Builder destinationDim(ResourceKey<World> d) { this.destinationDim = d; return this; }
        public Builder destination(Vec3d d)            { this.destination = d; return this; }
        public Builder rotation(@Nullable DQuaternion r) { this.rotation = r; return this; }
        public Builder scaling(double s)               { this.scaling = s; return this; }
        public Builder visible(boolean v)              { this.visible = v; return this; }
        public Builder teleportable(boolean t)         { this.teleportable = t; return this; }

        public BackroomsPortalEntity build() {
            // SPIKE-0.11 : confirmer BackroomsPortalEntityType.INSTANCE existe et est enregistré
            BackroomsPortalEntity e = new BackroomsPortalEntity(BackroomsPortalEntityType.INSTANCE, level);
            e.setPosition(position);
            e.axisW = axisW; e.axisH = axisH;
            e.width = width; e.height = height;
            e.dimensionTo = destinationDim; e.destination = destination;
            e.rotation = rotation; e.scaling = scaling;
            e.visible = visible; e.teleportable = teleportable;
            return e;
        }
    }
}
```

### §5.2 `BackroomsPortalEntityRegistry`

**Chemin** : `com.gtgpuke.gtgsbackrooms.backrooms.portal.entity.BackroomsPortalEntityRegistry`
**SoT pour** : résolution `UUID → BackroomsPortalEntity`.
**Lifecycle** : oui (init des listeners spawn/discard).

Permet à `RoomGraph`, `PortalManager`, `AggroSimulationSystem` de retrouver une entité par son UUID sans scanner le monde.

```java
// CODE TEMPLATE — MUST COMPILE (après résolution des SPIKE-X.Y référencés)
@SourceOfTruth
public class BackroomsPortalEntityRegistry implements Lifecycle {

    private final Map<UUID, BackroomsPortalEntity> byUuid = new ConcurrentHashMap<>();
    private LifecyclePhase phase = LifecyclePhase.INIT;

    @Override
    public void init() {
        // Hook spawn/discard via ServerEntityEvents.ENTITY_LOAD / ENTITY_UNLOAD
        // SPIKE-0.11 : confirmer signature ServerEntityEvents.ENTITY_LOAD / ENTITY_UNLOAD
        //              dans Fabric API 1.21.1 (peut être renommé en ServerLifecycleEvents)
    }

    @Override
    public void start() { phase = LifecyclePhase.STARTED; }

    @Override
    public void stop() { byUuid.clear(); phase = LifecyclePhase.STOPPED; }

    @Override
    public LifecyclePhase getPhase() { return phase; }

    public @Nullable BackroomsPortalEntity get(UUID uuid) {
        return byUuid.get(uuid);
    }

    public Stream<BackroomsPortalEntity> streamAll() {
        return byUuid.values().stream();
    }
}
```

### §5.3 Conséquence sur `PortalContext`

```java
public record PortalContext(
    PlayerEntity player,
    World world,
    PortalState currentState,
    @Nullable BackroomsPortalEntity activePortal,   // ← changé : était PortalLink
    BackroomsConfig config,
    DebugMetrics metrics,
    long tickCount,
    float partialTick
) {}
```

---

## §6. ClientWorldRegistry — multi-dimension côté client

### §6.1 Principe

En single-player V1, le client a accès direct au serveur intégré et à toutes ses dimensions. Pour rendre la vue d'un portail vers la dimension `dim2` alors que le joueur est dans `dim1`, le client doit maintenir un `ClientLevel` et un `LevelRenderer` pour `dim2`.

C'est exactement ce que fait `ClientWorldLoader` dans ImmersivePortals.

### §6.2 `ClientWorldRegistry`

**Chemin** : `com.gtgpuke.gtgsbackrooms.backrooms.portal.client.world.ClientWorldRegistry`
**SoT pour** : `Map<ResourceKey<Level>, ClientLevel>` côté client.
**Annotations** : `@SourceOfTruth`, `@ClientOnly`.
**Lifecycle** : oui — alloue/libère les ClientLevel.

```java
// CODE TEMPLATE — MUST COMPILE (après résolution des SPIKE-0.3, SPIKE-0.11)
@SourceOfTruth
@ClientOnly
public class ClientWorldRegistry implements Lifecycle {

    private final Map<ResourceKey<World>, ClientLevel> clientLevels = new HashMap<>();
    private final Map<ResourceKey<World>, RemoteLevelRenderer> renderers = new HashMap<>();
    private LifecyclePhase phase = LifecyclePhase.INIT;

    /**
     * Retourne le ClientLevel pour la dimension demandée.
     *
     * En single-player V1, lit directement la dimension côté serveur intégré.
     *
     * FALLBACK: si la dimension n'existe pas côté serveur (impossible en V1 sauf bug config),
     * retourne null et log warning. Le portail concerné affichera fallback bruit.
     */
    public @Nullable ClientLevel getOrCreate(ResourceKey<World> dim) {
        if (phase != LifecyclePhase.STARTED) return null;
        return clientLevels.computeIfAbsent(dim, this::createClientLevel);
    }

    public @Nullable RemoteLevelRenderer getRenderer(ResourceKey<World> dim) {
        return renderers.get(dim);
    }

    private @Nullable ClientLevel createClientLevel(ResourceKey<World> dim) {
        // SPIKE-0.3 + R-1 : adapter IP ClientWorldLoader.createClientWorld() à Yarn 1.21.1.
        //                   Doit construire un ClientLevel avec :
        //                   - le ClientLevel.Properties de la dim cible
        //                   - le RegistryKey<World>
        //                   - une DimensionType
        //                   Le spike 0.3 valide la signature exacte du constructeur ClientLevel.
        throw new UnsupportedOperationException("Pending Phase 0 spike — see IP ClientWorldLoader");
    }

    @Override public void init()  { /* registries */ }
    @Override public void start() { phase = LifecyclePhase.STARTED; }
    @Override public void stop()  {
        clientLevels.values().forEach(ClientLevel::close);
        renderers.values().forEach(RemoteLevelRenderer::close);
        clientLevels.clear();
        renderers.clear();
        phase = LifecyclePhase.STOPPED;
    }
    @Override public LifecyclePhase getPhase() { return phase; }
}
```

### §6.3 Plan A / B / C — `ClientLevel` et `RemoteLevelRenderer`

Selon le résultat de SPIKE-0.3, trois niveaux de fallback sont définis **avant** d'écrire une ligne de Phase 4 :

| Plan | Condition | Implémentation | Contrainte |
|---|---|---|---|
| **Plan A** | SPIKE-0.3 OK : `ClientLevel` créable pour une dimension distante | Un `ClientLevel` + un `LevelRenderer` par dimension active (code IP adapté) | Budget GPU : max 2 dimensions actives simultanées (V4.3 §7) |
| **Plan B** | SPIKE-0.3 KO (constructeur cassé ou registries incompatibles) | Un seul `ClientLevel` partagé, son `level` est swappé avant chaque rendu de portail | Perte de récursion (DA-3 invalidée) ; une seule vue distante à la fois |
| **Plan C** | Plan B impossible (swap niveau instable) | Le rendu portail est désactivé ; portails affichent le fallback bruit/flou | Toujours détectable : portails visibles mais pas transparents |

La décision Plan A/B/C est documentée dans `phase0_findings.md §Spike0.3` et propagée à DA-1 dans §16.2. Codex ne démarre Phase 4 qu'après cette décision.

### §6.4 `RemoteLevelRenderer` — lifecycle détaillé

Un `LevelRenderer` par dimension distante (Plan A uniquement). Permet de reproduire le pipeline de rendu vanilla pour la dimension cible (chunks, entités, ciel, fog).

**Chemin** : `com.gtgpuke.gtgsbackrooms.backrooms.portal.client.world.RemoteLevelRenderer`
**Lifecycle** : oui — alloue les ressources GL dans `init()`, libère dans `stop()`.

**État requis dans `init()` (ordre obligatoire, SPIKE-0.3) :**
1. Le `Minecraft.getInstance()` doit être disponible (donc `init()` ne peut pas être appelé depuis un constructeur statique)
2. La `ClientLevel` pour la dimension cible doit être créée via `ClientWorldFactory.createClientLevel(dim)` (SPIKE-0.3)
3. Le `LevelRenderer` est instancié avec `new LevelRenderer(minecraft, entityRenderDispatcher, blockEntityRenderDispatcher, minecraft.renderBuffers())` — SPIKE-0.3 valide la signature exacte
4. `levelRenderer.setLevel(clientLevel)` (ou équivalent Yarn 1.21.1)
5. `levelRenderer.onResourceManagerReload(minecraft.getResourceManager())` pour charger les shaders/chunks du renderer

**Interaction avec le renderer principal :**
- Le renderer distant est utilisé **uniquement pendant `PortalStencilRenderer.renderRemoteWorld()`**, jamais autrement
- Avant d'appeler `levelRenderer.renderLevel(...)` sur le renderer distant, sauvegarder et restaurer le `Minecraft.level` (via `IMinecraftClientAccessor`) pour que les systèmes internes du renderer pointent sur la bonne dimension
- Après le rendu, restaurer `Minecraft.level` à la dimension du joueur

> **Budget GPU** : un `LevelRenderer` complet est lourd (~50–200 Mo VRAM). SPIKE-0.3 doit mesurer l'empreinte mémoire avec 1 et 2 renderers simultanés. Au-delà de 2 : Plan B.

---

## §7. Rendu stencil — PortalStencilRenderer

### §7.1 Concept (rappel)

Le stencil buffer est un buffer 8 bits par pixel, utilisé comme masque :

1. Dessiner le quad du portail → écrire `outerDepth + 1` dans le stencil aux pixels du quad (mais pas dans color/depth).
2. Effacer la profondeur de la zone stencil (depth = 1.0 = loin) pour que le monde distant puisse s'y dessiner.
3. Configurer `glStencilFunc(GL_EQUAL, depth+1, 0xFF)` → seuls les pixels du masque sont écrits.
4. Rendre le monde destination avec la caméra transformée.
5. Restaurer la profondeur du quad du portail (occlusion correcte).

La valeur stencil = profondeur de nesting (0 = monde réel, 1 = vue dans 1 portail, 2 = vue dans un portail dans un portail).

### §7.2 `PortalStencilRenderer`

**Chemin** : `com.gtgpuke.gtgsbackrooms.backrooms.portal.client.view.PortalStencilRenderer`
**Annotations** : `@ClientOnly`, `@PortalCritical`.
**Lifecycle** : oui (alloue rien directement, mais initialise le stencil sur le main framebuffer).
**Pas un `PortalSubsystem`** — c'est un composant interne utilisé par `PortalViewSystem`.

```java
// PSEUDO-CODE — DO NOT COPY AS-IS
// Pipeline stencil GL complexe. Reconstruire à partir de IP RendererUsingStencil
// + spikes 0.2 + 0.4 + 0.5 validés. Les appels glStencilFunc/glStencilOp sont
// ici l'intention algorithmique, pas l'API exacte.
@ClientOnly
@PortalCritical
public class PortalStencilRenderer implements Lifecycle {

    private final PortalDepthTracker depthTracker;
    private final ClientWorldRegistry worldRegistry;
    private final RemoteCamera remoteCamera;
    private final RemoteCameraGuard cameraGuard;
    private final ViewAreaRenderer viewAreaRenderer;
    private final PortalSurfaceRenderer surfaceRenderer; // pour le fallback
    private final BackroomsConfig config;
    // ... constructeur package-private (règle d'or n°7 ; ce composant est interne)

    /**
     * Appelé par PortalViewSystem.tick() en TickPhase.RENDER, avec la liste des portails
     * sélectionnés par PortalManager (scoring §13 V4.3).
     *
     * FALLBACK:
     *   - si stencil non disponible (Sodium désactive ?) : tous les portails affichent fallback bruit.
     *   - si la dimension destination n'a pas de ClientLevel : fallback bruit pour ce portail.
     *   - si guard.evaluate() == FALLBACK : fallback bruit pour ce portail.
     */
    public void renderPortals(List<BackroomsPortalEntity> portals,
                              Matrix4f modelView, PortalContext ctx) {
        prepareRendering();
        for (BackroomsPortalEntity portal : portals) {
            if (!depthTracker.canRender()) {
                surfaceRenderer.renderFallback(portal);
                continue;
            }
            renderOne(portal, modelView, ctx);
        }
        finishRendering();
    }

    private void prepareRendering() {
        // Activer le stencil sur le framebuffer principal (StencilEnabler)
        // glClearStencil(0); glClear(GL_STENCIL_BUFFER_BIT); glEnable(GL_STENCIL_TEST);
    }

    private void renderOne(BackroomsPortalEntity portal, Matrix4f modelView, PortalContext ctx) {
        // 1. Vérifier la caméra (RemoteCameraGuard)
        var guard = cameraGuard.evaluate(ctx.player(), portal, config);
        if (guard.result() == GuardResult.FALLBACK) {
            surfaceRenderer.renderFallback(portal);
            return;
        }

        // 2. Calculer la valeur stencil pour cette profondeur
        int outerStencilValue = depthTracker.currentDepth();

        // 3. Écrire le quad du portail dans le stencil avec GL_INCR
        //    (stencilFunc EQUAL outerStencilValue, stencilOp KEEP KEEP INCR)
        viewAreaRenderer.writeStencil(portal, modelView, outerStencilValue);

        int thisStencilValue = outerStencilValue + 1;

        depthTracker.push(portal);
        try {
            // 4. Effacer la profondeur dans la zone stencil (depth = 1.0)
            clearDepthInPortalArea(portal, thisStencilValue);

            // 5. Configurer le stencil pour rendre uniquement à thisStencilValue
            //    glStencilFunc(GL_EQUAL, thisStencilValue, 0xFF);
            //    glStencilMask(0x00); // ne pas modifier le stencil pendant le rendu monde

            // 6. Rendre le monde destination
            ClientLevel destLevel = worldRegistry.getOrCreate(portal.getDimensionTo());
            if (destLevel == null) {
                surfaceRenderer.renderFallback(portal);
                return;
            }
            CameraState remoteCam = (guard.result() == GuardResult.CLAMP_APPLIED)
                ? cameraGuard.applyCorrectedCamera(ctx.player().camera(), portal)
                : remoteCamera.compute(ctx.player(), portal);

            var renderInfo = RemoteWorldRenderInfo.builder()
                .level(destLevel)
                .cameraPos(remoteCam.position())
                .cameraTransformation(remoteCam.matrix())
                .renderDistance(config.portalRenderDistance)
                .doRenderSky(false)   // pas de ciel dans Backrooms
                .hasFog(true)
                .build();

            renderRemoteWorld(renderInfo);

            // 7. Restaurer la profondeur du quad du portail (pour occlusion)
            viewAreaRenderer.restoreDepth(portal, modelView, thisStencilValue);
        } finally {
            depthTracker.pop();
        }
    }

    private void renderRemoteWorld(RemoteWorldRenderInfo info) {
        // SPIKE-0.5 + R-2 : adapter IP RendererUsingStencil.renderPortalContent() à Yarn 1.21.1.
        //                   Le LevelRenderer obtenu via worldRegistry.getRenderer(dim) est utilisé
        //                   pour rendre le monde distant pendant que le stencil masque la zone.
        //                   Voir spike 0.5 pour le pipeline complet validé.
        throw new UnsupportedOperationException(
            "SPIKE-0.5 NOT VALIDATED — renderRemoteWorld() non implémentable avant spike §16 spike 0.5");
    }

    private void finishRendering() {
        // glDisable(GL_STENCIL_TEST);
    }

    private void clearDepthInPortalArea(BackroomsPortalEntity portal, int stencilValue) {
        // Triangle plein écran avec depth = 1.0, color mask off, stencil EQUAL stencilValue
        // Voir IP RendererUsingStencil.clearDepthOfThePortalViewArea() — code copié + adapté
        // SPIKE-0.2 : adapter les appels GL (glDepthRange, glColorMask, RenderSystem.*)
        throw new UnsupportedOperationException(
            "SPIKE-0.2 NOT VALIDATED — clearDepthInPortalArea() adapté depuis IP après spike §16 spike 0.2");
    }

    @Override public void init() { /* RAS — le stencil est activé via Mixin sur RenderTarget */ }
    @Override public void start() { /* */ }
    @Override public void stop() { /* */ }
    @Override public LifecyclePhase getPhase() { return /* ... */; }
}
```

### §7.3 `ViewAreaRenderer`

**Chemin** : `com.gtgpuke.gtgsbackrooms.backrooms.portal.client.view.ViewAreaRenderer`

Dessine le quad rectangulaire du portail — utilisé pour écrire dans le stencil (étape 3) et restaurer la depth (étape 7).

```java
// PSEUDO-CODE — DO NOT COPY AS-IS
// Le code réel doit utiliser le Tesselator/BufferBuilder Yarn 1.21.1 (validé SPIKE-0.5).
@ClientOnly
public class ViewAreaRenderer {
    /**
     * Écrit le quad du portail dans le stencil buffer avec GL_INCR.
     *
     * Pré-requis : glStencilFunc(GL_EQUAL, expectedOuterValue, 0xFF) et glStencilOp(KEEP, KEEP, INCR)
     * sont configurés par l'appelant (PortalStencilRenderer).
     */
    public void writeStencil(BackroomsPortalEntity portal, Matrix4f modelView, int expectedOuterValue) {
        // Construire les 4 vertices du rectangle (centre + ±axisW * width/2 + ±axisH * height/2)
        // Désactiver color mask, désactiver depth test
        // Tesselator quad
    }

    public void restoreDepth(BackroomsPortalEntity portal, Matrix4f modelView, int stencilValue) {
        // Comme writeStencil, mais cette fois écrit dans le depth buffer la depth réelle du quad
        // Permet l'occlusion correcte par les blocs devant le portail
    }
}
```

---

## §8. Récursion à 1 niveau — PortalDepthTracker

### §8.1 Décision DA-3 — récursion limitée à 1 niveau

| Profondeur courante | Comportement |
|---|---|
| 0 (monde réel) | Rendu normal de tous les portails sélectionnés |
| 1 (à l'intérieur d'un portail) | Rendu normal des portails dans la vue distante (récursion 1 niveau) |
| 2 (portail dans un portail dans un portail) | **Fallback** — bruit/flou statique |

Concrètement : `maxRecursionDepth = 1` signifie qu'on autorise **2 niveaux de stencil** (0 et 1) ; au passage à 2, le fallback s'enclenche.

> **Pourquoi pas plus** : V4.3 §27 vendait l'absence de récursion comme une feature backrooms (« la perception ne porte qu'à un niveau »). Le compromis 1 niveau permet des setups intéressants (portails face à face dans une même pièce) sans s'engager sur du multi-niveau qui explose le coût GPU.

### §8.2 `PortalDepthTracker`

**Chemin** : `com.gtgpuke.gtgsbackrooms.backrooms.portal.client.view.PortalDepthTracker`
**Annotations** : `@ClientOnly`, `@PortalCritical`.
**Scope** : par-frame, `static` mais reset à 0 en début de chaque frame (defensive).

```java
// CODE TEMPLATE — MUST COMPILE (pure Java, aucun spike requis)
@ClientOnly
@PortalCritical
public final class PortalDepthTracker {

    private static final int MAX_RECURSION_DEPTH = 1; // DA-3
    private static int currentDepth = 0;
    private static final Deque<UUID> stack = new ArrayDeque<>();

    /** Appelé par PortalStencilRenderer.renderPortals() avant la boucle. */
    public static void resetForFrame() {
        currentDepth = 0;
        stack.clear();
    }

    /** True si on peut encore rendre un portail (sinon → fallback). */
    public static boolean canRender() {
        return currentDepth <= MAX_RECURSION_DEPTH;
    }

    public static int currentDepth() { return currentDepth; }

    public static void push(BackroomsPortalEntity portal) {
        currentDepth++;
        stack.push(portal.getUuid());
    }

    public static void pop() {
        currentDepth--;
        stack.pop();
    }

    /** True si on est actuellement à l'intérieur du rendu d'un portail. */
    public static boolean isRendering() { return currentDepth > 0; }

    private PortalDepthTracker() {}
}
```

### §8.3 Reset défensif

`PortalStencilRenderer.prepareRendering()` doit appeler `PortalDepthTracker.resetForFrame()` **avant** la boucle, pour garantir qu'une exception lors d'un précédent frame ne laisse pas le compteur à un état corrompu.

```java
private void prepareRendering() {
    PortalDepthTracker.resetForFrame(); // defensive
    // ... activer stencil, clear, enable test
}
```

---

## §9. Caméra distante — RemoteCamera + RemoteCameraGuard

### §9.1 `RemoteCamera`

**Chemin** : `com.gtgpuke.gtgsbackrooms.backrooms.portal.client.view.RemoteCamera`

Calcule la caméra distante en appliquant la transformation du portail à la caméra du joueur. Intègre les méthodes que `TransformationManager` d'IP exposait.

```java
// CODE TEMPLATE — MUST COMPILE (après SPIKE-0.C : DQuaternion validé)
@ClientOnly
public class RemoteCamera {

    /**
     * Caméra distante = symétrique de la caméra joueur par la transformation du portail.
     *
     *   joueur P1, rotation R1
     *   portail source S, transformation T
     *   →  P2 = portal.transformPoint(P1)
     *   →  R2 = portalRotation * R1   (composition des rotations)
     */
    public CameraState compute(PlayerEntity player, BackroomsPortalEntity portal) {
        // Position distante : transformPoint applique rotation + scaling + translation destination
        Vec3d remotePos = portal.transformPoint(player.getEyePos());

        // SPIKE-0.C IMPORTANT — nommage DQuaternion et formule à valider :
        //
        // En IP, la méthode s'appelle DQuaternion.getCameraRotation(pitch, yaw)
        // (ORDRE : pitch en premier, yaw en second) — pas fromYawPitch.
        //
        // Formule de la rotation distante en IP (TransformationManager.java) :
        //   finalRot = rawCameraRotation * portalRot
        //   soit : remoteRot = playerRot.hamiltonProduct(portalRot)
        //
        // RISQUE : selon la convention de portal.rotation (source→dest ou dest→source),
        // il peut être nécessaire d'inverser. Valider avec spike 0.C en comparant
        // visuellement les deux formules (portail tournant à 90°, à 180°).
        // Référence IP exacte : TransformationManager.java lignes ~50-80.
        //
        // PLACEHOLDER : formule à confirmer via spike avant Phase 4.
        DQuaternion playerRot = DQuaternion.getCameraRotation(player.getPitch(), player.getYaw()); // SPIKE-0.C
        DQuaternion portalRot = (portal.getRotation() != null) ? portal.getRotation() : DQuaternion.identity; // identity = champ statique, pas méthode
        DQuaternion remoteRot = playerRot.hamiltonProduct(portalRot); // SPIKE-0.C : vérifier si portalRot ou portalRot.inverse()

        return new CameraState(remotePos, remoteRot.toMatrix4f(), player.getCameraFov());
    }
}
```

### §9.2 `RemoteCameraGuard`

**Chemin** : `com.gtgpuke.gtgsbackrooms.backrooms.portal.client.view.RemoteCameraGuard`

Inchangé par rapport à V4.3 §14. Protège des cas extrêmes (caméra trop proche du plan, angle trop oblique, FOV > 110°). Voir spec V4.3 §14 pour le détail comportemental.

| Cas | Correction |
|---|---|
| Distance normale, face au portail | OK — rendu normal |
| Très proche (< 0.2 blocs) | `CLAMP_APPLIED` — near plane offset |
| Angle < 15° | `FALLBACK` — surface affiche bruit |
| FOV > 110° | `CLAMP_APPLIED` — FOV clampé à 100° |
| Joueur derrière le portail | invisible (face culling normal) |

---

## §10. Téléportation — threading et masquage

> **Cette section corrige une ambiguïté majeure du V4.3 §15** sur le « TP frame-exact en POST_RENDER ». La formulation « POST_RENDER » suggérait un TP côté **client/render thread**, ce qui est faux et dangereux. Le TP s'exécute côté **serveur/main thread**. Cette section décrit le pipeline retenu pour ce mod.

### §10.0 Adaptation simplifiée vs pipeline IP réel

> ⚠️ **Le `TeleportExecutor` décrit dans ce guide est une adaptation simplifiée du pipeline IP, pas une copie fidèle.** Assumer l'écart explicitement.

**Ce que fait ImmersivePortals réellement** (source : `ServerTeleportationManager.java`) :

| Étape | Acteur | Mécanisme IP |
|---|---|---|
| Détection joueur | **Client** (!) | Le client détecte que l'œil du joueur a croisé le plan, envoie un packet S2C vers le serveur : `onPlayerTeleportedInClient(player, dimensionBefore, eyePosBeforeTeleportation, portalId)` |
| Validation serveur | **Server** | `validatePlayerTeleportationAndGetReason()` — vérifie position, cooldown `lastTeleportGameTime`, dimension cohérente |
| Anti-double-TP | **Server** | `lastTeleportGameTime : WeakHashMap<Entity, Long>` — cooldown basé sur `ServerGameTime` |
| Exécution TP | **Server** | `teleportPlayer(player, dimensionTo, newEyePos)` → changement dimension + reset position + update collision |
| Entités non-joueur | **Server** | Via `Portal.SERVER_PORTAL_TICK_SIGNAL` + `startTeleportingRegularEntity()` → posté dans `ServerTaskList` (tick suivant) |
| Détection entités | **Server** | `portal.isMovedThroughPortal(lastEyePos, nextEyePos)` — mouvement sur 2 ticks |
| Chasers (mobs qui suivent) | **Server** | `notifyChasersForPlayer()` déclenché côté serveur après le TP joueur |

**Ce que ce guide implémente (adaptation simplifiée V1)** :

- **Pas de packet client→serveur** : le serveur détecte lui-même le franchissement via `ThresholdDetector` (tick serveur) — plus simple mais légèrement moins précis (délai 1 tick = 50ms vs détection sub-tick IP)
- **Pas de `lastTeleportGameTime` distincts par entité** : un `cooldownTicks` simple par portail suffit pour V1 single-player
- **Entités non-joueur** : même mécanisme simplifié (pas de `ServerTaskList` deferral)
- **Pas de `ScaleUtils`, `GravityChangerInterface`** : hors scope V1 (portails sans scaling ni changement de gravité)

**Pourquoi c'est acceptable pour V1** : single-player, latence réseau = 0, pas de race condition client/serveur.

**Pour V2 (multi-joueur futur)** : migrer vers le modèle IP client-first + validation serveur. Marqué `// SPIKE-V2:` dans le code.

---

### §10.1 Modèle de threading correct

| Étape | Thread | Phase tick | Composant |
|---|---|---|---|
| 1. Détection franchissement | **Server main thread** | `TickPhase.TICK` (`ServerTickEvents.END_SERVER_TICK`) | `ThresholdDetector` (depuis `TransitionSystem`) |
| 2. Décision TP (`ARMED` → `TRANSITIONING`) | **Server main thread** | `TickPhase.TICK` | `PortalStateMachine` |
| 3. Exécution TP (move + change dim) | **Server main thread** | `TickPhase.TICK` (même tick que la détection) | `TeleportExecutor` |
| 4. Sync packet S2C de TP | **Server → Client** (vanilla MC) | — | Vanilla `EntityTrackerEntry` / `PlayerPositionPacket` |
| 5. Activation masquage visuel client | **Client render thread** | `TickPhase.RENDER` ou `WorldRenderEvents.START` | `TransitionMasker` (shader Veil) |
| 6. Smooth caméra (`animationDelta`) | **Client render thread** | `TickPhase.RENDER` | `ClientTransitionSmoother` (équivalent IP `ClientTeleportationManager`) |

> **Conséquence pratique** : il n'y a **pas** de notion de « TP frame-exact ». Le TP est **server-tick-exact** (50 ms). Le mismatch visuel client est masqué par un shader Veil qui couvre le délai entre le packet S2C de TP et la stabilisation de la caméra. C'est le modèle ImmersivePortals.

### §10.2 Détection — `ThresholdDetector` (server-side)

**Chemin** : `com.gtgpuke.gtgsbackrooms.backrooms.portal.transition.ThresholdDetector`

```java
// CODE TEMPLATE — MUST COMPILE
public class ThresholdDetector {

    /**
     * Évalue si l'entité a franchi le plan du portail entre le tick précédent et le tick courant.
     *
     * Appelé depuis TransitionSystem.tick(TickPhase.TICK, ctx) — server thread.
     * NE doit JAMAIS être appelé depuis un WorldRenderEvent (render thread).
     */
    public ThresholdEvent evaluate(Entity entity, BackroomsPortalEntity portal) {
        // Position du tick précédent : accessor mixin sur Entity (lastX, lastY, lastZ)
        Vec3d last = ((IEntityLastPosAccessor) entity).backrooms$getLastTickPos(); // SPIKE-0.A
        Vec3d cur  = entity.getPos();

        boolean wasFront = portal.isInFrontOfPortal(last);
        boolean isFront  = portal.isInFrontOfPortal(cur);
        boolean crossed  = (wasFront != isFront) && portal.isPointInPortalProjection(cur);

        return new ThresholdEvent(portal.getUuid(), crossed, last, cur);
    }
}
```

> **SPIKE-0.A** : créer l'accessor mixin `IEntityLastPosAccessor` sur `net.minecraft.entity.Entity` (ou son équivalent Yarn 1.21.1) avec `@Accessor("lastRenderX")` ou un nouveau `@Inject` qui capture la position en début de tick. Voir spike §16.

### §10.3 Exécution — `TeleportExecutor` (server main thread, règle d'or n°4)

**Chemin** : `com.gtgpuke.gtgsbackrooms.backrooms.portal.transition.TeleportExecutor`
**Annotations** : `@PortalCritical`, `@ServerOnly`.

**Règle d'or n°4** (arbo §6) + Codex Contract **I-6** : tout TP joueur passe par cette classe, **sur server main thread uniquement**.

Patterns d'invocation autorisés (vérifiés par grep I-6) :

```java
// ✓ Correct — appelé directement depuis TransitionSystem.tick() (TickPhase.TICK)
//   qui tourne sur server main thread via ServerTickEvents.END_SERVER_TICK
@Override
public void tick(TickPhase phase, PortalContext ctx) {
    if (phase != TickPhase.TICK) return;
    // ... détection ...
    if (shouldTeleport) {
        teleportExecutor.execute(serverPlayer, portal, transitionCtx);
    }
}

// ✓ Correct — posté depuis le client (ex: commande debug) vers le server thread
//   via MinecraftServer.execute()
client.getServer().execute(() -> teleportExecutor.execute(serverPlayer, portal, ctx));

// ✗ INCORRECT — sur le render thread (Codex Contract I-6 violation)
WorldRenderEvents.END.register(context -> teleportExecutor.execute(...)); // INTERDIT

// ✗ INCORRECT — sur ClientTickEvents (client thread)
ClientTickEvents.END_CLIENT_TICK.register(c -> teleportExecutor.execute(...)); // INTERDIT
```

**CODE TEMPLATE** :

```java
// CODE TEMPLATE — MUST COMPILE
@PortalCritical
@ServerOnly
public class TeleportExecutor {

    private final DestinationLoader destinationLoader;
    private final ModEventBus modEventBus;

    TeleportExecutor(DestinationLoader destinationLoader, ModEventBus modEventBus) {
        // package-private
        this.destinationLoader = destinationLoader;
        this.modEventBus = modEventBus;
    }

    /**
     * Exécute le TP du joueur à travers le portail.
     *
     * THREAD: server main thread uniquement. Vérifié par @ServerOnly + Codex Contract I-6.
     * Si appelé depuis un autre thread, throw IllegalStateException immédiatement.
     *
     * Code adapté de IP ServerTeleportationManager (Apache-2.0, §22) — adaptation simplifiée, voir §10.7.
     *
     * FALLBACK: si destLevel est null, ou si destinationLoader.isReady(portal) == false :
     *   - log warning
     *   - publie PortalEvent.TeleportFailed sur ModEventBus
     *   - retourne TeleportResult.failed()
     *   - le joueur reste dans sa dimension d'origine (le TransitionSystem retombe en COOLDOWN)
     */
    public TeleportResult execute(ServerPlayerEntity player,
                                   BackroomsPortalEntity portal,
                                   TransitionContext ctx) {
        assertServerThread(player.getServer()); // throw si pas server thread

        ResourceKey<World> destDim = portal.getDimensionTo();
        ServerWorld destLevel = (destDim != null) ? player.getServer().getWorld(destDim) : null;

        if (destLevel == null || !destinationLoader.isReady(portal)) {
            modEventBus.post(new PortalEvent.TeleportFailed(portal.getUuid(), "destination_not_ready"));
            return TeleportResult.failed();
        }

        // 1. Position destination
        Vec3d newPos = portal.transformPoint(player.getPos());
        // 2. Orientation
        float[] newLook = transformLook(portal, player.getYaw(), player.getPitch());
        // 3. Vélocité
        Vec3d newVel = portal.transformVelocityRelativeToPortal(player.getVelocity());

        // 4. TP cross-dimension OU intra-dimension
        if (!destLevel.getRegistryKey().equals(player.getWorld().getRegistryKey())) {
            // SPIKE-0.B : signature exacte de TP cross-dim Yarn 1.21.1.
            // Candidats : ServerPlayerEntity.teleport(ServerWorld, x, y, z, yaw, pitch)
            //             ou FabricDimensions.teleport(...)
            // Voir phase0_findings.md §0.B avant d'écrire ce bloc.
            player.teleport(destLevel, newPos.x, newPos.y, newPos.z, newLook[0], newLook[1]);
        } else {
            player.requestTeleport(newPos.x, newPos.y, newPos.z); // SPIKE-0.B
        }

        player.setVelocity(newVel);
        // Notifier le client pour activer le masquage visuel (§10.4)
        sendTransitionStartedPacket(player, portal, ctx);
        modEventBus.post(new PortalEvent.TransitionCompleted(portal.getUuid(), newPos));
        return TeleportResult.success(newPos, newLook[0], newLook[1]);
    }

    private static void assertServerThread(MinecraftServer server) {
        if (!server.isOnThread()) {
            throw new IllegalStateException(
                "TeleportExecutor.execute() called off server main thread — Codex Contract I-6 violation");
        }
    }

    private float[] transformLook(BackroomsPortalEntity portal, float yaw, float pitch) {
        // SPIKE-0.C : implémentation transform yaw/pitch via DQuaternion
        // Voir IP TransformationManager.transformClientboundLook() / Portal.transformClientboundLook()
        throw new UnsupportedOperationException(
            "SPIKE-0.C NOT VALIDATED — see phase0_findings.md §Spike0.C before implementing");
    }

    private void sendTransitionStartedPacket(ServerPlayerEntity player,
                                              BackroomsPortalEntity portal,
                                              TransitionContext ctx) {
        // Envoie un packet S2C custom qui déclenche TransitionMasker côté client.
        // Voir backrooms/portal/network/s2c/TransitionStartedPayload (à créer Phase 5).
    }
}
```

### §10.4 Masquage visuel client — `TransitionMasker`

**Chemin** : `com.gtgpuke.gtgsbackrooms.backrooms.portal.client.effect.TransitionMasker`
**Annotations** : `@ClientOnly`.
**Lifecycle** : oui (alloue/libère le pipeline Veil).

Le masquage est purement **visuel et client-side**. Il s'enclenche en réception d'un packet S2C `TransitionStartedPayload` envoyé par `TeleportExecutor` après le TP serveur.

```java
// PSEUDO-CODE — DO NOT COPY AS-IS
@ClientOnly
public class TransitionMasker implements Lifecycle {

    private final VeilPostProcessingBridge veilBridge;
    private final ResourceLocation MASKING_PIPELINE = id("backrooms:masking");
    private MaskState state = MaskState.IDLE;
    private long startedAtMs = 0;

    /** Appelé en réception du packet S2C TransitionStartedPayload (client thread). */
    public void onTransitionStarted(MaskType maskType, long durationMs) {
        veilBridge.activatePipeline(MASKING_PIPELINE);
        // Set uniform u_intensity = 0 → ramp to 1 sur durationMs/2, puis ramp to 0 sur durationMs/2
        startedAtMs = System.currentTimeMillis();
        state = MaskState.MASKING;
    }

    /** Appelé chaque frame en TickPhase.RENDER pour mettre à jour l'intensité. */
    public void tick(PortalContext ctx) {
        if (state != MaskState.MASKING) return;
        long elapsed = System.currentTimeMillis() - startedAtMs;
        // ramp triangulaire : 0 → 1 → 0 sur durationMs
        // FALLBACK: si veilBridge.getState() != OK, skip — pas de masquage mais pas de crash
    }
}
```

### §10.5 Smoothing caméra client — `ClientTransitionSmoother`

**Chemin** : `com.gtgpuke.gtgsbackrooms.backrooms.portal.client.effect.ClientTransitionSmoother`
**Équivalent IP** : `ClientTeleportationManager` + `TransformationManager.animationDelta`.

Quand le portail tourne le joueur (rotation ≠ identity), la caméra fait un saut visuel à la réception du packet de TP. Le smoother applique une rotation `animationDelta` qui se dissipe progressivement sur ~300 ms, lissant le saut.

```java
// PSEUDO-CODE — DO NOT COPY AS-IS
@ClientOnly
public class ClientTransitionSmoother {
    private DQuaternion animationDelta = DQuaternion.identity; // champ statique en IP, pas une méthode
    private long lastUpdateMs = 0;

    /** Appelé en réception du packet S2C avec la rotation portail. */
    public void onPortalTeleport(DQuaternion portalRotation) {
        // animationDelta = inverse de la rotation appliquée par le portail
        // → la caméra "résiste" initialement, puis converge vers la nouvelle orientation
        this.animationDelta = portalRotation.inverse();
        this.lastUpdateMs = System.currentTimeMillis();
    }

    /** Appelé chaque frame avant que le rendu utilise la rotation caméra. */
    public DQuaternion applyToCameraRotation(DQuaternion playerRotation) {
        // Décay exponentiel sur ~300 ms
        // SPIKE-0.D : valider que le hook caméra Yarn 1.21.1 (Camera.update ou GameRenderer)
        //             permet d'injecter une rotation supplémentaire avant le rendu
        return animationDelta.hamiltonProduct(playerRotation);
    }
}
```

### §10.6 Pourquoi c'est "frame-masqué" et non "frame-exact"

| Modèle | Fenêtre de mismatch | Comment c'est masqué |
|---|---|---|
| V4.3 « POST_RENDER frame-exact » (incorrect) | 0 frame en théorie, ambigu en pratique sur le threading | Inutilisable tel quel |
| **Ce guide — server-tick + client-mask** | 1-2 frames potentiellement (latence packet S2C local en single-player < 1 ms, mais 1 server tick = 50 ms) | Shader Veil de masquage couvre 150-200 ms (V4.3 §7) — la fenêtre de mismatch est dans cette enveloppe |

L'illusion tient parce que :
1. Le shader de masquage est activé **avant** que la position du joueur ne change visuellement (le packet S2C masquage arrive en même temps que le packet de TP, mais le masquage est rendu avant la mise à jour de la position dans le frame courant — Loi 4 V4.3 §4).
2. La rotation est lissée par `ClientTransitionSmoother` même si le saut serveur est instantané.
3. Le couloir bouclé (le cas dominant Backrooms) ne tourne pas la caméra — donc le smoothing est même rarement nécessaire en pratique.

> **Conséquence sur le `FrameSyncPolicy`** mentionné dans l'arbo : cette classe est **conservée** mais son rôle change. Elle ne « schedule un TP en POST_RENDER » plus — elle gère le timing relatif **packet de masquage** vs **packet de TP** côté serveur, pour s'assurer que le client reçoit le masquage en premier (ou au pire, dans le même frame que le TP).

---

## §11. Chargement des chunks destination — DestinationLoader

### §11.1 `DestinationLoader`

**Chemin** : `com.gtgpuke.gtgsbackrooms.backrooms.portal.transition.DestinationLoader`
**Lifecycle** : oui (allocation des tickets côté serveur).

Reprend l'idée d'`ImmPtlChunkTickets` : pour chaque portail en `PREWARMING` ou `VISIBLE`, charger les chunks autour de `portal.destination` côté serveur, dans la bonne dimension.

```java
public class DestinationLoader implements Lifecycle {

    private static final TicketType<UUID> PORTAL_TICKET =
        TicketType.create("backrooms_portal", UUID::compareTo);

    public void requestPrepare(BackroomsPortalEntity portal, MinecraftServer server) {
        ServerWorld destLevel = server.getWorld(portal.getDimensionTo());
        if (destLevel == null) return;

        ChunkPos destChunk = new ChunkPos(BlockPos.ofFloored(portal.getDestination()));
        int radius = backroomsConfig.portalRenderDistance / 16; // chunks

        for (ChunkPos pos : chunksInRadius(destChunk, radius)) {
            destLevel.getChunkManager().addTicket(
                PORTAL_TICKET, pos, 2, portal.getUuid()
            );
        }
    }

    public void release(BackroomsPortalEntity portal, MinecraftServer server) {
        ServerWorld destLevel = server.getWorld(portal.getDimensionTo());
        if (destLevel == null) return;

        ChunkPos destChunk = new ChunkPos(BlockPos.ofFloored(portal.getDestination()));
        int radius = backroomsConfig.portalRenderDistance / 16;

        for (ChunkPos pos : chunksInRadius(destChunk, radius)) {
            destLevel.getChunkManager().removeTicket(
                PORTAL_TICKET, pos, 2, portal.getUuid()
            );
        }
    }

    public boolean isReady(BackroomsPortalEntity portal, MinecraftServer server) {
        // Vérifie que tous les chunks dans le rayon sont chargés et tickés
        // SPIKE-0.11 : signature ChunkHolder.getCurrentStatus / Yarn equivalent en 1.21.1.
        //              Vérifier que la méthode existe et retourne ChunkStatus comparable à FULL.
        throw new UnsupportedOperationException(
            "SPIKE-0.11 NOT VALIDATED — isReady() signature ChunkHolder Yarn 1.21.1 à confirmer");
    }

    @Override public void init() { /* RAS */ }
    @Override public void start() { /* */ }
    @Override public void stop() {
        // Libérer tous les tickets pour ne pas leak des chunks chargés
    }
    @Override public LifecyclePhase getPhase() { /* ... */ }
}
```

> **V1 single-player** : pas besoin de sync custom des chunks vers le client. Le client a accès direct au serveur intégré, et le `ClientLevel` créé par `ClientWorldRegistry` lit les chunks au fur et à mesure que le serveur les charge.

---

## §12. Mixins — emplacements et points d'injection

### §12.1 `MixinLevelRenderer` — point d'injection rendu portail

**Chemin** : `com.gtgpuke.gtgsbackrooms.mixin.client.MixinLevelRenderer`

Le hook Fabric `WorldRenderEvents.BEFORE_TRANSLUCENT` n'existe pas — il faut un Mixin sur `LevelRenderer.renderLevel()` au moment précis avant le rendu des blocs translucides (juste avant `Sheets.translucentCullBlockSheet()`).

```java
@Mixin(LevelRenderer.class)
public abstract class MixinLevelRenderer {

    @Inject(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/RenderLayer;getTranslucent()Lnet/minecraft/client/render/RenderLayer;"
        )
        // SPIKE-0.4 + R-2 : confirmer la signature exacte Yarn 1.21.1 du target translucent.
        //                   Mapping IP utilise Sheets.translucentCullBlockSheet en mappings officiels.
        //                   En Yarn 1.21.1 : RenderLayer.getTranslucent ? À valider par le spike.
        //                   Si KO : option de secours = Mixin GameRenderer.render @At RETURN (V4.3 §15).
    )
    private void backrooms$beforeTranslucent(/* params renderLevel */ CallbackInfo ci) {
        // Récupérer le PortalManager singleton et déléguer
        PortalManager.instance().beforeTranslucentRendering(/* matrices */);
    }
}
```

### §12.2 `MixinServerChunkCache` — tick des tickets

**Chemin** : `com.gtgpuke.gtgsbackrooms.mixin.common.MixinServerChunkCache`

Permet à `DestinationLoader` de mettre à jour les tickets à chaque tick serveur.

```java
@Mixin(ServerChunkLoadingManager.class) // ou ServerChunkManager selon mapping
public class MixinServerChunkCache {
    @Inject(method = "tick", at = @At("HEAD"))
    private void backrooms$tickPortalTickets(CallbackInfo ci) {
        // SPIKE-0.11 : confirmer le nom de classe (ServerChunkLoadingManager vs ServerChunkManager)
        //              et la signature de la méthode tick() en Yarn 1.21.1.
        //              Au démarrage de Phase 5 : DestinationLoader.tickAll() est appelé d'ici.
    }
}
```

### §12.3 `MixinClientLevel` — accès cross-dimension entités

**Chemin** : `com.gtgpuke.gtgsbackrooms.mixin.client.MixinClientLevel`

Permet à `EntityEchoSystem` de lire les entités d'un `ClientLevel` distant maintenu par `ClientWorldRegistry`. En V1 single-player, le serveur intégré a déjà les entités ; on accède en lecture seule via le `ServerLevel` correspondant.

### §12.4 `MixinMinecraftClient` — accesseur framebuffer principal

**Chemin** : `com.gtgpuke.gtgsbackrooms.mixin.client.access.IMinecraftClientAccessor`

Accessor pour échanger temporairement le framebuffer principal (utilisé par `StencilEnabler` pour activer le stencil).

```java
@Mixin(MinecraftClient.class)
public interface IMinecraftClientAccessor {
    @Mutable
    @Accessor("framebuffer")
    void backrooms$setFramebuffer(Framebuffer fb);
}
```

### §12.5 `MixinRenderTarget` — activation du stencil

**Chemin** : `com.gtgpuke.gtgsbackrooms.mixin.client.MixinRenderTarget`

Active le stencil buffer 8 bits sur le framebuffer principal au démarrage. Sodium désactive le stencil pour ses optimisations — ce mixin force la réactivation. Équivalent d'`IPPortingLibCompat.setIsStencilEnabled` dans IP.

```java
@Mixin(Framebuffer.class)
public class MixinRenderTarget {
    @Inject(method = "init", at = @At("RETURN"))
    private void backrooms$enableStencil(/* ... */ CallbackInfo ci) {
        // Réattacher un GL_DEPTH24_STENCIL8 au lieu de GL_DEPTH_COMPONENT
        // SPIKE-0.2 : adapter le ré-attachement GL_DEPTH24_STENCIL8 à l'API GL Minecraft 1.21.1.
        //             Le spike 0.2 fournit le code exact validé.
        //             ATTENTION (R-3) : Sodium peut écraser cette modification — voir SPIKE-0.10.
    }
}
```

---

## §13. Veil — ce qui reste, ce qui disparaît

### §13.1 Reste (Pilier 2 inchangé, voir `spec_shaders_backrooms_V2.md`)

| Composant | Rôle | Bridge |
|---|---|---|
| `ShaderManager` | Orchestre les shaders Backrooms | — |
| `ShaderEventAdapter` | Handler shader (uniforms, definitions) | — |
| `VeilUniformBridge` | Push uniforms par-pipeline | OK |
| `VeilDefinitionsBridge` | `#define` pour graceful degradation | OK |
| `VeilShaderBridge` | Enregistrement programmes shader | OK |
| `VeilPostProcessingBridge` | Pipelines post-process (vignette, glitch, distortion, masquage TP) | OK |
| `VeilFramebufferBridge` | Framebuffers offscreen pour shaders **non-portail** (post-FX intermédiaires si besoin) | OK |
| `IrisBridge`, `IrisStateStore` | Soft-dep Iris (WARN_ONLY / AUTO_TOGGLE / NO_IRIS) | OK |

### §13.2 Disparaît (DA-1)

| Ancien rôle (V4.3) | Nouveau (ce guide) |
|---|---|
| `PortalFramebuffer` (Veil) | **Supprimé** — le rendu portail utilise stencil sur main framebuffer |
| `PortalRenderTarget` (Veil) | **Supprimé** — idem |
| Veil pour rendre la vue distante | **Remplacé** par `PortalStencilRenderer` + Mixin `LevelRenderer` |

### §13.3 `TransitionMasker` — Veil-natif

Le masquage TP (fade noir, bruit, distortion) reste un **shader Veil** appelé via `VeilPostProcessingBridge`. Plus précisément :

- État `ARMED` → `transitionMasker.prepare(portal)` active le shader Veil `backrooms:masking` avec intensité 0
- État `TRANSITIONING` → intensité ramp à 1.0 sur ~150ms (durée masquage), puis le TP s'exécute
- État `COOLDOWN` → intensité ramp à 0 sur ~150ms

Ainsi Veil garde sa raison d'être : c'est lui qui produit l'effet visuel de transition, pas le rendu portail lui-même.

---

## §14. Machine d'état des portails

Inchangée par rapport à V4.3 §6. C'est `PortalStateMachine` (côté `transition/`) qui calcule les transitions, et `TransitionSystem` (le `PortalSubsystem`) qui les applique.

```
INACTIVE → PREWARMING → VISIBLE → ARMED → TRANSITIONING → COOLDOWN → INACTIVE
```

**Distances de référence** (configurables via `BackroomsConfig`, V4.3 §6) :

| Constante | Défaut | Rôle |
|---|---|---|
| `portalActivationDistance` | 16 blocs | INACTIVE → PREWARMING |
| `portalVisibleDistance` | 10 blocs | PREWARMING → VISIBLE |
| `portalArmDistance` | 3 blocs | VISIBLE → ARMED |
| `teleportTriggerDepth` | 0.3 bloc | ARMED → TRANSITIONING |

Invariant : `portalActivationDistance > portalVisibleDistance > portalArmDistance > teleportTriggerDepth`.

> **Différence avec ImmersivePortals** : IP rend tous les portails visibles à chaque frame, sans machine d'état. La machine d'état Backrooms est un **ajout propre** pour gérer le budget GPU (max 2 vues distantes — V4.3 §7) et les effets de transition. Elle s'implémente au-dessus du stencil renderer en ajoutant une condition dans `getPortalsToRender()` : ne retourner que les portails dont l'état est `>= VISIBLE`.

---

## §15. Audio, entités, aggro, couloirs bouclés (résumés)

> Ces 4 sous-systèmes existent déjà dans `arborescence_v4.md §3` et restent **conceptuellement inchangés** par rapport à `spec_veil_portail_shaders_V4_3.md §§18.4-18.6 + §18.3`. Ce qui change : tout référence à `PortalLink` est remplacé par `BackroomsPortalEntity` (DA-2).

### §15.1 Audio cross-portail — `CrossSpaceAudioSystem`

**Chemin** : `com.gtgpuke.gtgsbackrooms.backrooms.portal.client.audio.*` (déjà dans l'arbo).
**Spec source** : V4.3 §18.5 (inchangé sur la philosophie).

Classes (déjà dans l'arbo, ne pas recréer) : `CrossSpaceAudioSystem` (PortalSubsystem CLIENT), `SpatialSoundProjector`, `EFXFilterManager`, `AudioSourceStabilizer` (hysteresis V4.3 §16), `AcousticZoneProfile`, `ProjectedSound`.

**Adaptation depuis IP** : la logique de reprojection d'`IP CrossPortalSound` est intégrée dans `SpatialSoundProjector.project()`. Utiliser `BackroomsPortalEntity.transformPoint()` (et son inverse) pour reprojeter les positions de sons distants.

```java
// PSEUDO-CODE — DO NOT COPY AS-IS
public class SpatialSoundProjector {
    public ProjectedSound project(SoundInstance distantSound,
                                   BackroomsPortalEntity portal,
                                   PlayerEntity player) {
        // Position du son dans le monde destination
        Vec3d soundPosDest = getSoundPos(distantSound);
        // Reprojection vers le côté source du portail (inverse transform)
        Vec3d soundPosProjected = portal.inverseTransformPoint(soundPosDest);
        // Atténuation par distance source↔portail destination (V4.3 §18.5)
        float gain = computeAttenuation(distantSound, portal);
        return new ProjectedSound(distantSound, soundPosProjected, gain, /* filtres EFX */);
    }
}
```

### §15.2 Entités visibles — `EntityEchoSystem`

**Chemin** : `com.gtgpuke.gtgsbackrooms.backrooms.portal.client.entity_echo.*` (déjà dans l'arbo).
**Spec source** : V4.3 §18.4 (inchangé).
**Règle d'or n°5 + AP-8** : strictement read-only. Aucune mutation d'entité réelle, aucun setter, aucun `tick()`, aucun stockage d'état propre.

```java
// CODE TEMPLATE — MUST COMPILE
@ClientOnly
public class EntityEchoSystem implements PortalSubsystem {

    private final ClientWorldRegistry worldRegistry;
    private final BackroomsConfig config;

    EntityEchoSystem(ClientWorldRegistry worldRegistry, BackroomsConfig config) {
        // package-private (règle d'or n°7)
        this.worldRegistry = worldRegistry;
        this.config = config;
    }

    @Override public String getId() { return "entity_echo"; }
    @Override public Set<TickPhase> getRegisteredPhases() { return Set.of(TickPhase.RENDER); }
    @Override public int getPriority() { return 50; }
    @Override public Set<Bridge> getDependencies() { return Set.of(/* aucun bridge */); }

    @Override
    public void tick(TickPhase phase, PortalContext ctx) {
        // Pas de logique en tick — le rendu se fait par appel direct depuis PortalStencilRenderer
        // pendant le rendu du monde destination (méthode renderEntitiesInPortalView ci-dessous).
    }

    /**
     * Appelé par PortalStencilRenderer pendant le rendu du monde destination.
     * READ-ONLY (règle d'or n°5, AP-8).
     */
    public void renderEntitiesInPortalView(BackroomsPortalEntity portal, MatrixStack matrices) {
        ClientLevel destLevel = worldRegistry.getOrCreate(portal.getDimensionTo());
        if (destLevel == null) return; // FALLBACK: silencieux, pas d'écho rendu

        int budget = config.maxProxyEntitiesPerView; // V4.3 §7 défaut = 4
        int count = 0;

        // SPIKE-0.E : en Yarn 1.21.1, l'accès aux entités d'un ClientLevel est probablement
        // destLevel.entitiesForRendering() (Iterable<Entity>) ou
        // ((IClientLevelEntityAccess)destLevel).backrooms$getEntityLookup().iterate(AABB...)
        // PAS getEntities().getAll() — cette API n'existe pas en Fabric Yarn.
        // Valider le nom exact avant Phase 7. Placeholder throw ci-dessous :
        if (destLevel == null) throw new UnsupportedOperationException("SPIKE-0.E NOT VALIDATED");
        for (Entity e : destLevel.entitiesForRendering()) { // SPIKE-0.E : confirmer API Yarn 1.21.1
            if (count >= budget) break;
            if (!isNearPos(e, portal.getDestination(), 16.0)) continue;
            if (e instanceof PlayerEntity) continue;
            if (e instanceof BackroomsPortalEntity) continue; // pas de portail dans portail visible (DA-3)

            // Lecture seule : position(), getYaw(), animation courante
            // SPIKE-0.E : signature exacte EntityRenderDispatcher.render() Yarn 1.21.1
            renderEntityReadOnly(e, matrices);
            count++;
        }
    }

    private boolean isNearPos(Entity e, Vec3d pos, double radius) {
        return e.squaredDistanceTo(pos.x, pos.y, pos.z) <= radius * radius;
    }

    private void renderEntityReadOnly(Entity e, MatrixStack matrices) {
        // SPIKE-0.E : adapter à l'API EntityRenderDispatcher 1.21.1
    }
}
```

### §15.3 Aggro cross-portail — `AggroSimulationSystem`

**Chemin** : `com.gtgpuke.gtgsbackrooms.backrooms.portal.aggro.*` (déjà dans l'arbo).
**Spec source** : V4.3 §18.6 (inchangé).
**Règle d'or n°6** : le stimulus cible l'entité **réelle** dans son monde, jamais un proxy.

Pipeline (résumé V4.3 §18.6) :

1. `AggroSimulationSystem` (PortalSubsystem SERVER) tick les portails en état `>= VISIBLE`, en `TickPhase.TICK`.
2. Pour chaque portail, lit les mobs hostiles du `ServerLevel` destination dans `aggroDetectionRadius` (défaut 12 blocs).
3. Calcule un stimulus (VISUAL / AUDIO / PROXIMITY) via `StimulusPropagator`.
4. Le stimulus est posé sur l'entité réelle via Memory Module `BackroomsMemoryModules.PORTAL_LURE_TARGET` (`GlobalPos` du seuil source).
5. `PortalLureGoal` (Goal custom) attaché aux mobs « backrooms-aware » lit ce module et pathfind vers le seuil.
6. Quand le mob atteint le seuil, `TeleportExecutor.execute()` (règle d'or n°4 + Codex Contract I-6) le téléporte vers le côté joueur.

**Priorité du `PortalLureGoal`** (V4.3 §18.6 — règle non-magique) : `idleGoalPriority - 1`, **strictement supérieure** au combat actif. Si l'entité n'expose pas ces valeurs (mob mal déclaré), `PortalLureGoal` n'est **pas** inséré et un warning est logué. Pas de défaut magique.

### §15.4 Couloirs bouclés — `LoopingCorridorManager`

**Chemin** : `com.gtgpuke.gtgsbackrooms.backrooms.portal.illusion.LoopingCorridorManager` (déjà dans l'arbo).
**Spec source** : V4.3 §18.3 (inchangé).

Implémentation : un `BackroomsPortalEntity` **invisible** (`visible = false`) au milieu du couloir, destination = entrée du couloir, **même dimension**, rotation = identity, scaling = 1.

```java
// CODE TEMPLATE — MUST COMPILE
public class LoopingCorridorManager {

    private final BackroomsPortalEntityRegistry registry;
    private final RoomGraph roomGraph;

    public LoopingCorridorManager(BackroomsPortalEntityRegistry registry, RoomGraph roomGraph) {
        this.registry = registry;
        this.roomGraph = roomGraph;
    }

    /**
     * Spawn un BackroomsPortalEntity invisible de bouclage.
     * Codex Contract I-9 : ce constructeur d'Entity est l'un des points d'instanciation autorisés.
     */
    public UUID installLoopPortal(RoomSegment segment, ServerWorld level) {
        BackroomsPortalEntity portal = BackroomsPortalEntity.builder(level)
            .position(segment.midpointPos())
            .axes(segment.perpendicularAxis(), new Vec3d(0, 1, 0))
            .size(segment.corridorWidth(), segment.corridorHeight())
            .destinationDim(level.getRegistryKey())          // même dim
            .destination(segment.entrancePos())              // retour à l'entrée
            .rotation(null)                                  // identity
            .scaling(1.0)
            .visible(false)                                  // invisible !
            .teleportable(true)
            .build();

        level.spawnEntity(portal);
        roomGraph.registerLoopPortal(segment.id(), portal.getUuid()); // DA-6
        return portal.getUuid();
    }
}
```

**Contre-mesures anti-vision-longue** (V4.3 §18.3, à appliquer combinées) :

1. **Brouillard agressif** dans les dimensions Backrooms (fog complet 20-32 blocs) — uniform shader Veil dédié.
2. **Masquage géométrique** : coude ou obstacle visuel avant le portail invisible (le joueur ne voit pas la boucle depuis l'entrée).
3. **Variation visuelle** via `RoomVariantRegistry` : variantes légères du même segment.



## §16. Phase 0 — spikes vérifiables (fichier, log, fallback)

> **🔒 VERROU PHASE 0** : `arborescence_v4.md §12` impose `BackroomsConfig.enableBackroomsFeature = false` jusqu'à validation Phase 0. Ce verrou est **maintenu**. Aucune phase ≥ 1 n'est entamée tant que `phase0_findings.md` n'existe pas et n'est pas validé par l'utilisateur dans un commit séparé `chore: unlock Backrooms after Phase 0`.

> **Différence clé avec V4.3** : chaque spike a maintenant **(a) un fichier de spike concret**, **(b) un log attendu vérifiable**, **(c) un fallback documenté si échec**. Plus de critère vague type « ça marche ». Codex doit produire `phase0_findings.md` avec une section par spike — sinon refuser de passer à Phase 1.

### §16.1 Tableau exhaustif des spikes

Chaque spike vit dans `src/test/java/com/gtgpuke/gtgsbackrooms/spike/SXY_<nom>/` ou `src/main/java/.../spike/SXY_<nom>/` selon que c'est runtime ou testable hors MC. Les spikes runtime sont activés via une commande debug `/backrooms spike <id>`.

| # | Nom | Fichier spike | Log attendu | Si OK | Si KO — fallback |
|:--:|---|---|---|---|---|
| **0.1** | Build setup | `build.gradle` + `fabric.mod.json` minimal | `[FabricLoader] Loading mod 'gtgs_backrooms'` au démarrage | Continuer | **STOP** — résoudre avant tout. |
| **0.2** | Stencil buffer | `spike/S0_2_stencil/StencilSpikeMixin.java` (mixin sur `Framebuffer`) + `StencilSpikeCommand.java` | `[SPIKE-0.2] OK Framebuffer reattached with GL_DEPTH24_STENCIL8 (id=<n>)` ; quad de test colore le stencil et un test `glReadPixels` retourne la valeur attendue | Stencil utilisable, continuer | Documenter dans findings : « stencil non disponible sur cette version MC » → DA-1 invalidée → ré-ouvrir choix Veil framebuffer (rejeté DA-1) ou abandonner portails seamless. **STOP architecture decision.** |
| **0.3** | ClientLevel distant | `spike/S0_3_clientlevel/ClientLevelSpike.java` | `[SPIKE-0.3] OK ClientLevel created for dim=<dim_id>, chunk count=<n>` ; le spike crée un `ClientLevel` pour la dim de l'overworld de test, lit un chunk connu, log les blocs en (0,64,0) | Code IP `ClientWorldLoader` portable | Documenter signature manquante. Tenter l'approche « un seul ClientLevel partagé qui swap son `level` à chaque rendu portail » (perte récursion mais fallback acceptable). |
| **0.4** | Mixin LevelRenderer | `mixin/client/MixinLevelRenderer.java` avec un seul `@Inject` qui log | `[SPIKE-0.4] OK before-translucent hook fired, modelView=<matrix>` à chaque frame | Hook utilisable | Tester injection sur `WorldRenderer.render` (autre nom Yarn) ou `GameRenderer.renderWorld`. Si aucun hook ne fonctionne → considérer Mixin sur `GameRenderer.render(@At RETURN)` (option de secours V4.3 §15). |
| **0.5** | Rendu monde dans stencil | `spike/S0_5_stencil_world/StencilWorldSpike.java` combine 0.2+0.3+0.4 | `[SPIKE-0.5] OK rendered remote dim through stencil quad, frame_time=<ms>` ; visuellement : un rectangle de 4×3 blocs montre une vue du sky/chunks d'une autre dim | Architecture validée | Si rendu noir ou crash : isoler via 0.2 puis 0.3 puis 0.4 séparément. Documenter point d'échec exact. **STOP Phase 4 si non résolu.** |
| **0.6** | Veil 3.x charge | Reprise V4.3 spike 0.1+0.2+0.3 | `[SPIKE-0.6] OK Veil version <X.Y.Z> loaded, vignette pipeline activated, fb size=<W>x<H>` | Veil utilisable pour Pilier 2 | Documenter version Veil 3.x compat 1.21.1 manquante → ré-ouvrir choix Pilier 2 (PostChain vanilla ?). |
| **0.7** | Iris détection + toggle | Reprise V4.3 spike 0.4+0.5 | `[SPIKE-0.7] OK iris_loaded=<bool>, toggle_api_present=<bool>, mode_decided=<NO_IRIS\|WARN_ONLY\|AUTO_TOGGLE>` | Mode déterminé pour la session | Forcer `WARN_ONLY` permanent si toggle KO (V4.3 §10 — comportement par défaut). |
| **0.8** | TP server-tick + masquage | Reprise V4.3 spike 0.6 **mais reformulé** : tester TP via `ServerTickEvents` + packet S2C de masquage. Mesurer le délai entre packet masquage et packet TP côté client. | `[SPIKE-0.8] OK TP server-tick exec, mask_packet_arrival=<ms>, tp_packet_arrival=<ms>, delta=<ms>` ; visuellement : pas de saut de caméra > 1 frame perçu | Pipeline §10 validé | Si délai > 50ms : augmenter durée masquage à 250ms. Si saut visuel persistant : tester `ClientTransitionSmoother` pour lisser. |
| **0.9** | OpenAL EFX | Reprise V4.3 spike 0.7 | `[SPIKE-0.9] OK EFX present, low_pass+reverb applied to test source, sound audible` | EFX utilisable | Fallback : atténuation volume simple sans filtres (V4.3 §18.5). |
| **0.10** | Sodium compat | Lancer MC + Sodium + spike 0.5 | `[SPIKE-0.10] OK Sodium <version> + stencil portal rendering work together` | Compat Sodium ✓ | Si conflit stencil/Sodium : option utilisateur `disableStencilWithSodium = true` → fallback bruit/flou pour tous les portails. Documenter. |
| **0.11** | Yarn 1.21.1 mappings | Spike écrit pour valider chaque signature listée dans les `// SPIKE-0.X` du doc | Section dédiée dans findings : table « API symbol → status » | Toutes vertes | Section findings « Symbols à mocker en Phase 1 » + ouvrir issue pour la spike concernée. |
| **0.A** | Entity lastTickPos accessor | `mixin/access/IEntityLastPosAccessor.java` + spike de test | `[SPIKE-0.A] OK Entity.lastX/Y/Z accessible via @Accessor` | §10.2 utilisable | Capturer position en `Entity.tick @Inject HEAD` dans un map `Map<UUID, Vec3d>`. |
| **0.B** | TP cross-dim signature | `spike/S0_B_crossdim/CrossDimTpSpike.java` | `[SPIKE-0.B] OK player teleported overworld → the_nether via <method_name>` | `TeleportExecutor.execute()` utilisable | Si `ServerPlayerEntity.teleport(ServerWorld, ...)` n'existe pas en Yarn 1.21.1 → tester `FabricDimensions.teleport()` ou `Entity.changeDimension()`. |
| **0.C** | DQuaternion transform | Test JUnit pur sans MC | `[SPIKE-0.C] OK quaternion rotation tests pass (10/10)` | Math correct | Refuser §10 — le TP cross-rotation doit fonctionner avant Phase 5. |
| **0.D** | Camera rotation hook | `mixin/client/MixinCamera.java` avec `@Inject` sur `update` | `[SPIKE-0.D] OK camera rotation injectable, smoother delta applied` | `ClientTransitionSmoother` utilisable | Sans smoother : la rotation est instantanée mais masquée par `TransitionMasker` shader (acceptable si masquage bien calibré). |
| **0.E** | EntityRenderDispatcher signature | Spike runtime qui rend une `Entity` lue depuis un `ClientLevel` distant | `[SPIKE-0.E] OK rendered <count> entities from remote ClientLevel without crash` | `EntityEchoSystem` utilisable | Documenter et différer Phase 7 (entity echo) — Phase 4-5-6 ne dépendent pas de ce spike. |

**Total indicatif** : 7-11 jours selon vitesse de validation.

### §16.2 Format de `phase0_findings.md`

Le document final que Codex doit produire **avant** Phase 1 doit avoir cette structure (1 section par spike) :

```markdown
# Phase 0 Findings

## Spike 0.1 — Build setup
- Statut : OK / FALLBACK / BLOQUÉ
- Méthode : <résumé technique 2-3 lignes>
- Log capturé : `<copier-coller du log>`
- Décision : <comment Phase 1+ utilise ce résultat>
- Spike file: <chemin>

## Spike 0.2 — Stencil buffer
- Statut : ...
- ...

[... une section par spike 0.X ...]

## Décisions agrégées
- DA-1 hybride Veil : MAINTENUE / REVUE / ABANDONNÉE
- DA-2 Portal Entity SoT : MAINTENUE / ...
- DA-3 récursion 1 niveau : MAINTENUE / ...
- ...

## Patches supplémentaires à arborescence_v4.md
[si Phase 0 a révélé un besoin de structure non prévu]

## Symboles Yarn 1.21.1 utilisables (validés)
| Spike | Symbole | Signature |
|---|---|---|
| 0.B | ServerPlayerEntity.teleport(ServerWorld, double, double, double, float, float) | OK |
| ...

## Symboles Yarn 1.21.1 à mocker (BLOQUÉ)
| Spike | Symbole attendu | Plan B |
|---|---|---|
| ...
```

Si une section est en `BLOQUÉ` sans plan B, Codex **doit** refuser de passer à Phase 1 et notifier l'utilisateur (protocole arbo §21).

---

## §17. Phases 1-10 (alignées arbo §12)

### §17.1 Tableau de mapping

| Phase | Milestone arbo | Modules clés (chemins arbo v4 + patches §3) | DoD résumé (cf. §1.3) |
|:----:|---|---|---|
| **1** | Pilier 1 — config + bridges + Lifecycle + tick | `core/lifecycle/`, `core/tick/`, `core/annotation/`, `core/event/`, `config/`, `backrooms/config/`, `backrooms/bridge/` (5 Veil bridges + Iris) | `runClient` no-op, lifecycle ordre OK, log « Feature désactivée » |
| **2** | ShaderManager + pipeline Veil natif | `backrooms/shader/*` (Veil-natif first), assets shaders | Pipeline minimal visible (vignette debug) |
| **3** | PortalManager + state machine + scoring + **Portal Entity** | `backrooms/portal/PortalManager`, `portal/PortalContext`, `portal/PortalSubsystem`, `portal/PortalPriorityScore`, **`portal/entity/*`** (DA-2), `portal/shared/PortalState`, `portal/transition/PortalStateMachine`, `portal/transition/ThresholdDetector` | Tests JUnit verts pour les classes pures ; Entity spawn et NBT-persiste |
| **4** | ⭐ **PortalViewSystem stencil — premier portail rendu** | `portal/client/world/*` (DA-1), `portal/client/view/*` (sans `PortalFramebuffer`/`PortalRenderTarget`), Mixins `MixinLevelRenderer`+`MixinRenderTarget` | Cmd `/backrooms portal spawn_test` → portail visible avec vue distante. Pas de TP. |
| **5** | TransitionSystem complet (TP server-tick + masquage) | `portal/transition/*` (TeleportExecutor, FrameSyncPolicy, DestinationLoader, TransitionContext, TransitionSystem), `portal/client/effect/TransitionMasker`, `portal/client/effect/ClientTransitionSmoother`, packets S2C `TransitionStartedPayload` | Traversée masquée, pas de double-TP, cross-dim OK |
| **6** | CrossSpaceAudioSystem | `portal/client/audio/*` complet | Son filtré audible de l'autre côté |
| **7** | EntityEchoSystem + AggroSimulationSystem | `portal/client/entity_echo/*`, `portal/aggro/*`, `BackroomsMemoryModules`, `PortalLureGoal` | Mob visible et le mob réel pathfind vers seuil |
| **8** | SpatialIllusionSystem (couloirs bouclés) | `portal/illusion/*` complet, `RoomGraph` adapté UUID (DA-6) | Couloir bouclé invisible, joueur ne perçoit pas la boucle |
| **9** | Effets visuels avancés | Shaders Veil : distortion, glitch, flicker, fog backrooms | Ambiance Backrooms perçue |
| **10** | Niveaux Backrooms (Level 0) | `backrooms/level/level0/*` complet | Level 0 jouable de bout en bout |

### §17.2 Phase 1 — détail des fichiers

> Sortie attendue : `runClient` lance, log montre l'ordre lifecycle suivant : `LifecycleManager.init` → `BackroomsConfigManager.init` → `BridgeHealthMonitor.init` → 5 Veil bridges `init()` → `IrisBridge.init` → `BackroomsFeature.init` → `start` dans le même ordre. Aucun rendu, aucun portail, aucun shader. `BackroomsConfig.enableBackroomsFeature = false` → log « Feature désactivée ».

Modules à implémenter dans cet ordre (1 par 1, build vert entre chaque) :

1. `core/lifecycle/Lifecycle.java`, `LifecycleManager.java`, `LifecyclePhase.java`, `LifecycleException.java`
2. `core/tick/TickPhase.java`, `TickDispatcher.java`
3. `core/annotation/{SourceOfTruth,ClientOnly,ServerOnly,PortalCritical}.java`
4. `core/event/{ModEventBus,ModEvent}.java`
5. `config/{ModConfig,ConfigManager}.java`
6. `backrooms/config/{BackroomsConfig,BackroomsConfigManager,RenderSystemConfig}.java`
7. `backrooms/bridge/{Bridge,BridgeState,BridgeHealthMonitor}.java`
8. `backrooms/bridge/{VeilFramebufferBridge,VeilPostProcessingBridge,VeilUniformBridge,VeilDefinitionsBridge,VeilShaderBridge}.java` — stubs avec `// SPIKE-0.6` non résolu = `throw UnsupportedOperationException("Pending Phase 0 spike 0.6")`
9. `backrooms/bridge/{IrisBridge,IrisStateStore}.java` — stubs avec `// SPIKE-0.7`
10. `backrooms/BackroomsFeature.java` — court-circuit immédiat si `!config.enableBackroomsFeature`

### §17.3 Phase 4 — découpée 4A / 4B / 4C (point critique ⭐)

> **Pourquoi le découpage** : Phase 4 intègre 4 systèmes indépendants (stencil, ClientLevel, Mixin, caméra). Réunis en une seule phase, un bug est impossible à isoler. Le découpage garantit qu'à chaque sous-étape on a un visuel de validation.

**Pré-requis** : Phases 1-3 validées + `phase0_findings.md` indique spikes 0.2, 0.3, 0.4, 0.5 en `OK`.

#### Phase 4A — Stencil + quad de test (sans rendu monde)

**Objectif** : valider que le stencil buffer fonctionne et que le Mixin LevelRenderer s'injecte au bon point. Pas encore de vue distante — juste un quad coloré dans le masque stencil.

**Milestone** : le joueur voit un rectangle magenta (couleur de debug) à la place du portail. Aucune vue distante.

Modules (ordre) :
1. `mixin/client/MixinRenderTarget.java` — activation GL_DEPTH24_STENCIL8 (SPIKE-0.2)
2. `mixin/client/access/IMinecraftClientAccessor.java` — accessor framebuffer
3. `portal/client/view/StencilEnabler.java`
4. `portal/client/view/PortalDepthTracker.java` (pur Java, aucun spike)
5. `portal/client/view/ViewAreaRenderer.java` — écriture stencil + rendu debug couleur (SPIKE-0.2)
6. `mixin/client/MixinLevelRenderer.java` — injection before-translucent (SPIKE-0.4)
7. Cmd debug `/backrooms portal spawn_test` (version 4A) → portail spawn, quad magenta visible

**Critère 4A** : quad magenta visible dans le rectangle du portail, aucun crash sur angles extrêmes.

#### Phase 4B — Rendu monde distant sans portail intégré

**Objectif** : valider que `ClientWorldRegistry` + `RemoteLevelRenderer` peuvent rendre une dimension distante dans un framebuffer temporaire. Test standalone, pas encore intégré au stencil.

**Milestone** : commande `/backrooms test render_dim <dim>` affiche en HUD debug les chunks rendus (non intégrés dans le jeu).

Modules (ordre) :
1. `portal/client/world/ClientWorldFactory.java` (SPIKE-0.3)
2. `portal/client/world/RemoteLevelRenderer.java` (SPIKE-0.3 — lifecycle §6.4)
3. `portal/client/world/ClientWorldRegistry.java`
4. `portal/client/view/RemoteWorldRenderInfo.java` — record immutable
5. `portal/client/view/RemoteCamera.java` (SPIKE-0.C)
6. `portal/client/view/RemoteCameraGuard.java`
7. `portal/client/view/PortalSurfaceRenderer.java` — fallback bruit uniquement

**Critère 4B** : log `[SPIKE-0.3] Remote world rendered, chunks=<n>` sans crash.

#### Phase 4C — Intégration stencil + monde distant = portail complet

**Objectif** : assembler 4A + 4B dans `PortalStencilRenderer`. C'est la Phase 4 originale réduite à l'intégration.

**Milestone** : vue distante réelle dans le rectangle du portail (4B DoD complet).

Modules (ordre) :

**Objectif** : un `BackroomsPortalEntity` (positions hardcodées via cmd debug) affiche visuellement la vue de l'autre côté via stencil. **Pas de TP** (Phase 5).

**Modules Phase 4C** (build vert entre chaque) :

| # | Fichier | Type | Marqueur | Dépendances |
|:--:|---|---|---|---|
| 1 | `portal/client/view/PortalStencilRenderer.java` | Lifecycle, @PortalCritical | CODE TEMPLATE | §7.2, 4A+4B complets |
| 2 | `portal/client/view/PortalViewSystem.java` | PortalSubsystem (CLIENT) | CODE TEMPLATE | §7.2 + §13-14 |
| 3 | Wiring dans `PortalManager` (Phase 3) — registration de `PortalViewSystem` | modification | — | toutes les précédentes |
| 4 | Cmd debug `/backrooms portal spawn_test` (version finale) | command | CODE TEMPLATE | Entity (Phase 3), 4C wiring |

**Contraintes vérifiées** (Codex Contract) :
- I-1 : pas de `PortalLink` ressuscité.
- I-2, I-3 : pas de `PortalFramebuffer` / `PortalRenderTarget`.
- I-5 : aucun import Veil dans `portal/`.
- I-7 : `PortalViewSystem.tick()` n'est appelé que via `PortalManager.dispatch()`.
- I-8 : aucune alloc GL dans constructeur (textures, framebuffers seulement dans `init()`).

**Critère DoD** (cf. §1.3) :
1. Build vert.
2. `/backrooms portal spawn_test` crée un `BackroomsPortalEntity` à 5 blocs devant le joueur.
3. Le joueur voit la vue distante (chunks visibles dans le rectangle).
4. Test récursion : un 2e portail dans la vue du 1er → 2e affiche bruit (DA-3, règle d'or n°12).
5. Aucun crash en angles extrêmes (RemoteCameraGuard).
6. **Test occlusion** : poser un bloc plein devant le portail → la vue distante disparaît entièrement (occlusion query §24.5). Vérifier en log debug : `[Portal] occlusion_query=false, skipped render`.
7. **Test transparents** : eau ou verre dans la vue distante → pas d'artefact de tri de translucidité (§24.4 — depth effacée avant rendu).
8. **Test swap try/finally** : générer une NPE artificielle dans `renderRemoteWorld()` → le client revient dans sa vraie dimension sans rester bloqué dans la fausse (Codex Contract I-14).
9. Greps I-1 à I-15 verts.

---

## §18. Matrice de risques techniques

> **Cette section est nouvelle (issue du retour ChatGPT).** Elle priorise les zones où Codex peut produire du code plausible mais faux.

| ID | Risque | Probabilité | Impact | Mitigation | Spike de validation |
|---|---|:--:|:--:|---|:--:|
| **R-1** | Création de `ClientLevel` distant en 1.21.1 — l'API `ClientWorld.Properties`, le constructeur, les registries de chunk peuvent avoir changé entre les versions IP testées et 1.21.1. | Haute | Bloquant Phase 4 | Spike 0.3 obligatoire. Si KO : adapter le code IP du commit 1.20.x→1.21.x ou fallback « single shared ClientLevel ». | 0.3 |
| **R-2** | Mixin `LevelRenderer` injection — le nom `renderLevel` peut être `render` ou autre en Yarn 1.21.1 ; les paramètres peuvent être un `LevelRenderState` ou une suite de scalaires ; l'`@At` target peut résoudre vers une méthode inlinée. | Haute | Bloquant Phase 4 | Spike 0.4 obligatoire. Si KO : Mixin sur `WorldRenderer.render` ou `GameRenderer.renderWorld`. **Option de secours : Mixin `GameRenderer.render @RETURN`** (V4.3 §15). | 0.4 |
| **R-3** | Sodium désactive le stencil — Sodium remplace plusieurs choses dont la gestion framebuffer. Le `MixinRenderTarget` peut ne pas s'appliquer ou être écrasé. | Moyenne | Dégradé : portails affichent bruit si Sodium actif | Spike 0.10 obligatoire. Si KO : option utilisateur `disableStencilWithSodium = true` qui désactive le rendu portail (fallback bruit) plutôt que de crasher. | 0.10 |
| **R-4** | Threading TP client vs serveur — risque que Codex appelle `TeleportExecutor.execute()` depuis un `ClientTickEvents` ou `WorldRenderEvents` (race condition + crash). | Haute | Crash + corruption état | Codex Contract I-6 (grep), `assertServerThread()` dans `TeleportExecutor.execute()`, `@ServerOnly` annotation. Spike 0.8 valide le pipeline complet. | 0.8 |
| **R-5** | Mappings Yarn 1.21.1 — Codex peut inventer des signatures plausibles (`ServerPlayerEntity.teleport(ServerWorld, ...)`) qui n'existent pas en Yarn 1.21.1. | Très haute | Erreurs compilation, bugs runtime | Tout symbole non-vanilla-évident est marqué `// SPIKE-0.X` ; Codex Contract I-11 interdit l'invention. Section dédiée dans `phase0_findings.md` (« Symbols à mocker »). | 0.11 |
| **R-6** | Récursion stencil incorrecte — `glStencilOp(GL_INCR)` attend que la valeur reste exactement `outerDepth` ; un mauvais reset entre portails fait chevaucher les masques. | Moyenne | Artefacts visuels (pixels noirs, vue dupliquée) | `PortalDepthTracker.resetForFrame()` defensive en début de frame ; tests visuels en Phase 4 DoD #4 (récursion). | 0.5 |
| **R-7** | Tickets de chunks fuités — si `DestinationLoader.stop()` n'est pas idempotent ou pas appelé, les chunks restent chargés indéfiniment. | Moyenne | Memory leak progressif | Tickets associés à `portal.getUuid()` ; libération automatique sur `Entity.discard()` via hook. `Lifecycle.stop()` idempotent. | — |
| **R-8** | Sync entité cross-dim (Phase 7) — `EntityEchoSystem` lit les entités d'un `ClientLevel` distant ; en multi-joueur (V2+), cela suppose un protocole custom. | Faible (V1) | Hors scope V1 | V1 = single-player. Documentation explicite `// SPIKE-V2:` pour multi-joueur futur. Aucun code multi-joueur en V1. | 0.E |
| **R-9** | Iris API instable — toggle programmatique non documenté. | Haute | Pilier 2 dégradé | Spike 0.7. Mode `WARN_ONLY` permanent par défaut. `enableIrisAutoToggle = false` jusqu'à validation. | 0.7 |
| **R-10** | `BackroomsPortalEntity` NBT serialization — les champs `DQuaternion`, `Vec3d`, `ResourceKey<World>` n'ont pas de `Codec` direct dans MC vanilla. | Moyenne | Persistance KO | Sérialiser composant par composant (3 doubles + chaîne du registry key). Test JUnit pur sur le round-trip NBT. | — |
| **R-11** | Phase 4 milestone non démontrable — sans visuel direct, Codex peut rapporter « Phase 4 OK » sans que le portail rende effectivement quelque chose. | Moyenne | Faux positif validation | DoD §1.3 Phase 4 exige un visuel + screenshot demandé à l'utilisateur, pas seulement un build vert. | — |
| **R-12** | Frame-masqué insuffisant — si délai packet S2C masquage > frame de rendu du TP, le joueur voit 1 frame du nouveau monde sans masquage. | Faible (single-player) | Visible en debug, gênant en build | Spike 0.8 mesure le délai. Si > 50 ms : augmenter `transitionMaskDuration` config à 250 ms. | 0.8 |

---

## §19. Vérifications mécaniques (greps obligatoires)

> **Cette section est nouvelle (issue du retour ChatGPT).** Codex doit lancer ces commandes avant chaque commit Pilier 3 et reporter le résultat dans la section "Vérifications mécaniques" du format de réponse §1.4.

### §19.1 Greps Codex Contract (cf. §1.2)

```bash
# I-1 — pas de PortalLink ressuscité
grep -rE "(class|record|interface)\s+PortalLink\b" \
     --include="*.java" src/main/java
# Attendu : VIDE

# I-2 — pas de PortalFramebuffer
grep -rE "class\s+PortalFramebuffer\b" \
     --include="*.java" src/main/java
# Attendu : VIDE

# I-3 — pas de PortalRenderTarget
grep -rE "class\s+PortalRenderTarget\b" \
     --include="*.java" src/main/java
# Attendu : VIDE

# I-4 — aucun import Veil/Iris hors bridge
grep -rE "import (foundry\.veil|net\.coderbot\.iris|net\.irisshaders)" \
     --include="*.java" src/main/java | grep -v "/bridge/"
# Attendu : VIDE

# I-5 — pas de Veil dans backrooms/portal/
grep -rE "VeilFramebufferBridge|VeilPostProcessingBridge|VeilShaderBridge|VeilUniformBridge|VeilDefinitionsBridge" \
     src/main/java/com/gtgpuke/gtgsbackrooms/backrooms/portal/
# Attendu : VIDE

# I-6 — aucun TeleportExecutor.execute() dans WorldRenderEvents ou ClientTickEvents
grep -rln "TeleportExecutor" src/main/java | while read f; do
  grep -lE "(WorldRenderEvents|ClientTickEvents)\..*\.register" "$f"
done
# Attendu : VIDE

# I-7 — aucun .tick() invoqué hors PortalManager
grep -rnE "\.tick\(" src/main/java/com/gtgpuke/gtgsbackrooms/backrooms/portal/ | \
  grep -v "PortalManager.java" | \
  grep -v "void tick("
# Attendu : VIDE (uniquement les déclarations void tick(...))
```

### §19.2 Greps règles d'or arbo §6

```bash
# Règle d'or n°2 — tick orchestration
grep -rnE "subsystem\.tick\(|\.tick\(phase," src/main/java | \
  grep -v "PortalManager"
# Attendu : VIDE

# Règle d'or n°3 — pas d'auto-démarrage
grep -rB2 "this\.start\(\)" src/main/java | \
  grep -E "(public|private)\s+\w+\s*\("
# Inspection manuelle : start() ne doit pas être dans init() ni constructeur

# Règle d'or n°4 — TP via TeleportExecutor uniquement
grep -rnE "player\.(teleport|changeDimension|requestTeleport)" \
     src/main/java/com/gtgpuke/gtgsbackrooms/backrooms/ | \
  grep -v "TeleportExecutor.java"
# Attendu : VIDE

# Règle d'or n°5 — EntityEcho lecture seule
grep -rnE "(real|target)\.(set|teleport|damage|kill|discard|remove)" \
     src/main/java/com/gtgpuke/gtgsbackrooms/backrooms/portal/client/entity_echo/
# Attendu : VIDE

# Règle d'or n°7 — constructeurs PortalSubsystem package-private
grep -rE "public\s+\w+\s*\([^)]*\)\s*\{" \
     src/main/java/com/gtgpuke/gtgsbackrooms/backrooms/portal/ | \
  grep -E "(System|Manager)\.java"
# Inspection manuelle : aucun constructeur public sur les *System

# Règle d'or n°10 — alloc GL dans init() pas constructeur
grep -rB5 "GL11\.glGen\|RenderSystem\.glGen\|new Framebuffer\(" \
     src/main/java | grep -E "public\s+\w+\s*\("
# Inspection manuelle : ces appels ne doivent jamais être dans un corps de constructeur

# Règle d'or n°12 — récursion bornée
grep -rn "PortalDepthTracker\.canRender\|currentDepth\(\)" \
     src/main/java/com/gtgpuke/gtgsbackrooms/backrooms/portal/client/view/PortalStencilRenderer.java
# Attendu : présence d'un appel canRender() AVANT chaque renderOne()
```

### §19.3 Greps anti-patterns IA (arbo §16)

```bash
# AIP-4 — pas de commentaire qui répète la méthode
grep -B1 "public.*\b\w+\s*\(" src/main/java/com/gtgpuke/gtgsbackrooms/backrooms/ | \
  grep -E "// This (method|function) handles\|// This is the .* method"
# Attendu : VIDE

# AP-7 — pas de TODO nu
grep -rn "// TODO[^:]" src/main/java/com/gtgpuke/gtgsbackrooms/
# Attendu : VIDE (autorisés : // FIXME(...), // SPIKE-X.Y, // FALLBACK:, // SPEC-GAP: §X)
```

### §19.4 Vérifications structurelles

```bash
# Aucun BackroomsPortalEntity construit hors LoopingCorridorManager / PortalManager / spawn explicite
grep -rn "new BackroomsPortalEntity(" src/main/java | \
  grep -v "LoopingCorridorManager.java\|PortalManager.java\|BackroomsPortalEntity.java"
# Inspection manuelle : Codex Contract I-9

# Aucun import IP non-adapté (vérifie que le code copié a bien été renommé)
grep -rE "import qouteall\.\|import com\.qouteall\." src/main/java
# Attendu : VIDE

# Tous les fichiers copiés d'IP ont l'attribution Apache-2.0
for f in $(grep -rl "Adapted from iPortalTeam" src/main/java); do
  head -10 "$f" | grep -q "Apache License" || echo "MISSING APACHE NOTICE: $f"
done
# Attendu : VIDE (pas de "MISSING APACHE NOTICE")

# I-13 — le mot "ImmersivePortals" hors headers d'attribution Apache-2.0
grep -rn "ImmersivePortals" --include="*.java" src/main/java | \
  grep -v "Adapted from iPortalTeam"
# Attendu : VIDE

# I-14 — swap de dimension dans un try/finally
grep -rn "backrooms\$setWorldRenderer\|ip_setWorldRenderer\|ip_setLightmapTextureManager" \
     src/main/java/com/gtgpuke/gtgsbackrooms/backrooms/portal/client/ | \
  grep -v "finally"
# Attendu : VIDE (chaque call de swap est dans un bloc finally)
# Si non-vide : inspection manuelle obligatoire — vérifier que le restaure est dans finally

# I-15 — joueur local non rendu dans EntityEchoSystem
grep -n "LocalPlayer\|client\.player" \
     src/main/java/com/gtgpuke/gtgsbackrooms/backrooms/portal/client/entity_echo/ \
     --include="*.java" 2>/dev/null | \
  grep -v "getPos\(\)\|getEyePos\(\)\|getUuid\(\)"
# Attendu : VIDE (seules les lectures de position/UUID sont autorisées)
```

### §19.5 Script all-in-one

À placer dans `scripts/check_codex_contract.sh` et lancer en pre-commit hook :

```bash
#!/usr/bin/env bash
set -e
echo "=== Codex Contract checks ==="

declare -a CHECKS=(
  "I-1:grep -rE '(class|record)\s+PortalLink\b' --include=*.java src/main/java"
  "I-2:grep -rE 'class\s+PortalFramebuffer\b' --include=*.java src/main/java"
  "I-3:grep -rE 'class\s+PortalRenderTarget\b' --include=*.java src/main/java"
  "I-4:grep -rE 'import (foundry\.veil|net\.coderbot\.iris|net\.irisshaders)' --include=*.java src/main/java | grep -v /bridge/"
  "I-5:grep -rE 'Veil(Framebuffer|PostProcessing|Shader|Uniform|Definitions)Bridge' src/main/java/com/gtgpuke/gtgsbackrooms/backrooms/portal/"
  "I-13:grep -rn 'ImmersivePortals' --include=*.java src/main/java | grep -v 'Adapted from iPortalTeam'"
  "AP-7:grep -rn '// TODO[^:]' src/main/java/com/gtgpuke/gtgsbackrooms/"
)

FAILED=0
for check in "${CHECKS[@]}"; do
  id="${check%%:*}"
  cmd="${check#*:}"
  if eval "$cmd" 2>/dev/null | grep -q .; then
    echo "❌ $id FAILED:"
    eval "$cmd"
    FAILED=1
  else
    echo "✅ $id OK"
  fi
done

exit $FAILED
```

---

## §20. Points de vigilance critiques

### §20.1 Thread safety (V4.3 §11, arbo §11)

Le TP s'exécute **sur le thread principal** (server thread en single-player intégré, jamais sur le render thread).

```java
// ✓ Correct — dans le tick serveur ou via server.execute()
server.execute(() -> teleportExecutor.execute(player, portal, ctx));

// ✗ Incorrect — jamais dans un WorldRenderEvent
WorldRenderEvents.END.register((context) -> {
    teleportExecutor.execute(...); // VIOLATION règle d'or n°4
});
```

### §20.2 Stencil buffer et Sodium

Sodium désactive le stencil buffer pour ses optimisations. Le `MixinRenderTarget` réactive `GL_DEPTH24_STENCIL8`. **Impératif pour Phase 0.10** : valider que ce mixin coexiste avec Sodium sans crash.

> Si conflit Sodium impossible à résoudre : fallback documenté dans `phase0_findings.md` → soit blacklister Sodium dans les dimensions Backrooms (option utilisateur), soit basculer sur l'approche framebuffer (rejetée DA-1, à ré-évaluer si forcé).

### §20.3 Récursion (§7, DA-3, règle d'or n°12)

`PortalDepthTracker.canRender()` doit être appelé **avant** chaque écriture stencil dans `PortalStencilRenderer.renderOne()`. Au-delà de `maxRecursionDepth = 1` → fallback bruit. Le compteur est reset en début de chaque frame (defensive).

### §20.4 Cohérence vue distante ↔ réalité après TP (V4.3 §4, Loi 2)

La vue distante DOIT correspondre exactement à ce que le joueur voit après avoir traversé. Si le rendu distant lag (mise à jour 1 frame sur 2 — V4.3 §7), pendant `ARMED` et `TRANSITIONING` forcer la mise à jour à chaque frame.

### §20.5 EntityEcho — règle d'or n°5 (AP-8)

Les entités rendues dans la vue portail sont des entités **réelles** dans leur monde. Lecture seule. Aucun setter, aucun `tick()`, aucun appel mutable. Vérifiable par `grep` dans `entity_echo/`.

### §20.6 ModEventBus — règle d'or n°11 (AP-9)

Aucun handler `PortalEvent` ne déclenche `PortalManager.dispatch()`, `subsystem.tick()`, `TeleportExecutor.execute()`, ni mute `PortalState`. Le bus est **notification après-coup**, pas commande.

### §20.7 Fallback global — désactivation du système portail

Si un sous-système critique échoue à l'initialisation (stencil KO Phase 0.2, ClientLevel impossible Phase 0.3, Mixin LevelRenderer incompatible Phase 0.4), le portail ENTIER est désactivé plutôt que de crasher ou d'afficher des artefacts.

```java
// BackroomsFeature.java — point d'entrée Phase 1
public class BackroomsFeature implements Lifecycle {

    @Override
    public void start() {
        if (!bridgeHealthMonitor.allCriticalBridgesOk()) {
            LOGGER.error("[Backrooms] Sous-système critique KO : {} — portails désactivés pour cette session",
                bridgeHealthMonitor.getFailedBridges());
            this.portalSystemEnabled = false;
            // Notifier le joueur une fois en chat si déjà en jeu
            return;
        }
        // ... démarrage normal
    }
}
```

Les sous-systèmes critiques (dont l'échec désactive tout) :
- `StencilEnabler` (stencil buffer — §0.2)
- `ClientWorldRegistry` (ClientLevel distant — §0.3)
- `MixinLevelRenderer` (injection hook — §0.4)

Les sous-systèmes non-critiques (dont l'échec dégrade sans tout désactiver) :
- `CrossSpaceAudioSystem` → silence cross-portail
- `EntityEchoSystem` → pas d'entités visibles dans la vue
- `TransitionMasker` → TP sans fondu (toujours fonctionnel, juste moins immersif)

### §20.8 Allocations GL — AP-4

Aucune ressource OpenGL allouée dans un constructeur. Toujours dans `init()` (Lifecycle). Toujours libérée dans `stop()` (idempotent).

```java
// ✗ Incorrect (AP-4)
public PortalStencilRenderer() {
    this.someTextureId = GL11.glGenTextures(); // alloc dans constructeur
}

// ✓ Correct
@Override public void init() {
    this.someTextureId = GL11.glGenTextures();
}
@Override public void stop() {
    if (someTextureId != 0) {
        GL11.glDeleteTextures(someTextureId);
        someTextureId = 0;
    }
}
```

---

## §21. Dépendances Gradle

```groovy
repositories {
    maven { url "https://maven.fabricmc.net/" }
    maven { url "https://api.modrinth.com/maven" }
    // Veil 3.x via FoundryMC (Pilier 2)
    maven { url "https://maven.blamejared.com/" }
}

dependencies {
    minecraft "com.mojang:minecraft:1.21.1"
    mappings  "net.fabricmc:yarn:1.21.1+build.X:v2"
    modImplementation "net.fabricmc:fabric-loader:0.X.X"
    modImplementation "net.fabricmc.fabric-api:fabric-api:X.X.X+1.21.1"

    // Veil 3.x — version exacte verrouillée Phase 0 (Pilier 2 : shaders, post-FX)
    modImplementation "foundry.veil:veil-fabric-1.21.1:<locked-3.x-version>"

    // Iris — soft dependency (Pilier 2 : compat shader pack joueur)
    modCompileOnly "net.irisshaders:iris:X.X.X"

    // PAS de dépendance Maven sur ImmersivePortals — code copié et adapté (cf. §22)

    // LWJGL OpenAL EFX : fourni par MC, pas de dépendance externe (Pilier 3 : audio)
    // org.lwjgl.openal.EXTEfx déjà disponible dans le LWJGL bundlé MC
}
```

---

## §22. Licence — copier ImmersivePortals

ImmersivePortals (`iPortalTeam/ImmersivePortalsMod`) est sous **Apache License 2.0** (Copyright 2020 qouteall). Stratégie retenue (DA-4) :

1. **Copier les fichiers Java pertinents** dans le repo Backrooms, en les adaptant aux noms/packages de l'arbo v4 (cf. §4 mapping)
2. **Conserver le copyright header Apache-2.0** dans chaque fichier copié, avec mention « Adapted from iPortalTeam/ImmersivePortalsMod, Apache License 2.0 »
3. **Ajouter un fichier `THIRD_PARTY_LICENSES.md`** à la racine du repo qui liste l'attribution complète
4. **Ne pas dépendre du JAR ImmersivePortals** — pas de `modImplementation` Maven

> **Apache-2.0 vs MIT** : Apache-2.0 impose en plus de conserver les notices de copyright dans les travaux dérivés (`NOTICE` file si présent) et interdit d'utiliser les marques déposées du licenciant. Les obligations de redistribution (conserver le header + lien vers la licence) s'appliquent à chaque fichier copié.

Composants à copier (liste non exhaustive, basée sur §4 mapping) :

- `Portal.java` → `BackroomsPortalEntity.java`
- `ClientWorldLoader.java` → `ClientWorldRegistry.java`
- `RendererUsingStencil.java` → `PortalStencilRenderer.java`
- `ViewAreaRenderer.java` → `ViewAreaRenderer.java`
- `WorldRenderInfo.java` → `RemoteWorldRenderInfo.java`
- `TransformationManager.java` → méthodes intégrées dans `RemoteCamera.java`
- `ServerTeleportationManager.java` (logique) → méthodes dans `TeleportExecutor.java`
- `ImmPtlChunkTickets.java` (logique) → méthodes dans `DestinationLoader.java`
- `CrossPortalSound.java` (logique) → méthodes dans `SpatialSoundProjector.java`
- `CrossPortalEntityRenderer.java` (logique) → méthodes dans `EntityEchoSystem.java`
- `DQuaternion.java` → `util/math/DQuaternion.java`
- Les Mixins essentiels (LevelRenderer, ServerChunkCache, RenderTarget) → `mixin/client/`, `mixin/common/`

**Garde-fou** : pour chaque fichier copié, une ligne de commentaire en tête :

**Version IP de référence** : ce guide est basé sur la branche `1.21` du repository `iPortalTeam/ImmersivePortalsMod`. Le commit exact utilisé lors de la copie doit être documenté dans chaque fichier adapté (voir guard-fou ci-dessous) ET dans `phase0_findings.md §Version IP`.

> ⚠️ Ne pas copier depuis `main` (peut avoir du code 1.21.4+). Utiliser le tag de release 1.21.1 ou le commit de la branche `1.21` stable au moment du spike.

```java
/*
 * Adapted from iPortalTeam/ImmersivePortalsMod (Apache License 2.0, Copyright 2020 qouteall).
 * Original: <path original> (commit <SHA_A_REMPLIR> on branch 1.21)
 * Adapted for: GTG's Backrooms — arborescence v4 + guide_implementation_portails_v3.
 *
 * Yarn 1.21.1 mappings validés par : SPIKE-0.<X> (voir docs/phase0_findings.md).
 * Si une signature n'a pas été validée → // SPIKE-0.<X> bloquant + Codex Contract I-11.
 */
```

---

## §23. Résumé exécutif

> Le mod construit un **Pilier 3 (Portail)** dont la SoT est `BackroomsPortalEntity` (Entity NBT-persisted, inspirée de ImmersivePortals Apache-2.0, **adaptation simplifiée** — voir §10.0). Le rendu utilise **stencil buffer + Mixin LevelRenderer + raw OpenGL** sur le main framebuffer, avec récursion limitée à **1 niveau** (`PortalDepthTracker.maxRecursionDepth = 1`). La caméra distante est calculée par `RemoteCamera` via `transformPoint()` + composition de quaternions, protégée par `RemoteCameraGuard` aux angles extrêmes. Les `ClientLevel` distants sont maintenus côté client par `ClientWorldRegistry` (`@SourceOfTruth`, Lifecycle). Le TP est **server-tick-exact + client-mask** : exécuté par `TeleportExecutor` (règle d'or n°4 + Codex Contract I-6) sur server main thread dans `TickPhase.TICK`, le mismatch visuel est masqué côté client par un shader Veil (`TransitionMasker`) couvrant 150-200 ms. Les chunks destination sont chargés via `DestinationLoader` (tickets `TicketType<UUID>` côté serveur). L'audio cross-portail (`CrossSpaceAudioSystem`) reprojette via `BackroomsPortalEntity.transformPoint()`. Les entités sont rendues read-only (`EntityEchoSystem`, AP-8 + règle d'or n°5). L'aggro cross-portail passe par un Memory Module + `PortalLureGoal` ciblant l'entité **réelle** (règle d'or n°6). Les couloirs bouclés utilisent un `BackroomsPortalEntity` invisible (`visible = false`), dim source = dim destination, rotation identité, masqué par brouillard agressif et géométrie. **Veil reste pour le Pilier 2** (shaders custom, vignette, distortion, glitch, masquage TP) via les 5 bridges de l'arbo. Single-player V1.

**Architecture en 1 phrase** : Portal Entity (SoT) + ClientLevel multi-dim (SoT client) + stencil rendering 1-niveau + Veil pour les shaders → tous les sous-systèmes orchestrés par `PortalManager` via `PortalSubsystem`, dans le respect strict de l'arborescence v4 et du Codex Contract §1.

**Les 3 plus grands dangers pour Codex** :

1. **Copier le code IP sans adapter aux contrats de l'arbo** (Lifecycle, PortalSubsystem, package-private, SoT). Lire `arborescence_v4.md §4-§7` + `Codex Contract §1.2` **avant** de copier chaque fichier IP.
2. **Recréer `PortalLink` ou `PortalFramebuffer` par réflexe** — DA-1 et DA-2 ont supprimé ces classes. Tout doit passer par `BackroomsPortalEntity` + stencil. Vérifié par grep I-1, I-2, I-3.
3. **Inventer des signatures Yarn 1.21.1** (`ServerPlayerEntity.teleport()`, `ClientLevel.getEntities()`, etc.) plausibles mais fausses. Tout symbole non-vanilla-évident doit avoir un `// SPIKE-0.X` validé dans `phase0_findings.md`. Codex Contract I-11.

**Le pipeline de validation pour chaque commit Pilier 3** :
1. Build vert (`./gradlew build`).
2. Greps §19 (script `scripts/check_codex_contract.sh`) tous verts.
3. DoD de la phase courante (§1.3) coché.
4. Format de réponse §1.4 respecté.

Sans ces 4 points, **le commit n'est pas livrable** — Codex doit refuser de proposer la PR.

---

---

## §24. Analyse des mécanismes avancés — problèmes résolus par la référence IP

> **Objet de cette section.** Elle répond aux questions concrètes apparues lors de la tentative de reproduction du système portail, en expliquant *comment* la référence technique (ImmersivePortalsMod, Apache 2.0, §22) résout chaque problème — et où ce mécanisme atterrit dans l'arbo v4. Tout extrait de code ci-dessous est présenté avec sa correspondance dans l'arbo v4 et marqué `// PSEUDO-CODE — DO NOT COPY AS-IS` ; la version compilable sera dans les fichiers adaptatés (§4).

---

### §24.1 Transition en temps réel sans écran de chargement (intra- ou inter-dimension)

**Problème rencontré :** Appeler `player.teleport()` ou changer la dimension vanilla déclenche l'écran de chargement. Même en intra-dimension, une position distante (1000 blocs plus loin) génère un délai si les chunks ne sont pas chargés.

**Racine du problème :** la vue distante et le téléport sont deux opérations *distinctes*. La vue distante est purement client et ne doit jamais déclencher un changement d'état Minecraft. Le téléport, lui, est serveur-seulement (§10).

**Solution — swap temporaire de dimension côté client :**

Le client maintient un `ClientLevel` et un `LevelRenderer` *séparés* pour chaque dimension active. Avant de rendre la vue d'un portail, les objets `Minecraft.level` et `Minecraft.levelRenderer` sont **swappés temporairement** vers la dimension destination, le frame est rendu, puis l'état original est **restauré dans le même frame**. Aucun changement d'état vanilla n'est déclenché.

```java
// PSEUDO-CODE — DO NOT COPY AS-IS
// Source IP : CrossPortalViewRendering / ClientWorldLoader.switchAndRenderTheWorld()
// Adaptation v4 → ClientWorldRegistry.getOrCreate() + PortalStencilRenderer.renderRemoteWorld()

private static void switchAndRenderTheWorld(
    ClientLevel newWorld,         // ClientLevel de la dimension destination
    Vec3 remoteCameraPos,         // position caméra transformée par portal.transformPoint()
    Consumer<Runnable> invokeWrapper
) {
    // --- Sauvegarder l'état courant ---
    ClientLevel   oldWorld        = client.level;
    LevelRenderer oldRenderer     = client.levelRenderer;
    LightTexture  oldLightmap     = client.gameRenderer.lightTexture();
    Camera        oldCamera       = client.gameRenderer.getMainCamera();
    RenderBuffers oldRenderBuffers = ...;

    // --- Basculer vers la dimension destination ---
    ResourceKey<Level> newDim  = newWorld.dimension();
    LevelRenderer      newRend = ClientWorldRegistry.getRenderer(newDim);   // RemoteLevelRenderer
    DimRenderHelper    helper  = ClientWorldRegistry.getRenderHelper(newDim);

    ((IMinecraftClientAccessor) client).backrooms$setWorldRenderer(newRend);
    client.level = newWorld;
    ((IGameRendererAccessor) client.gameRenderer)
        .backrooms$setLightmapTextureManager(helper.lightmapTexture);
    FogRendererContext.swappingManager.pushSwapping(newDim); // brouillard de la bonne dim

    // --- Rendre la scène distante (pendant que le stencil masque la zone portail) ---
    invokeWrapper.accept(() -> { /* rendu effectif de levelRenderer.renderLevel(...) */ });

    // --- Restaurer immédiatement l'état original ---
    ((IMinecraftClientAccessor) client).backrooms$setWorldRenderer(oldRenderer);
    client.level = oldWorld;
    ((IGameRendererAccessor) client.gameRenderer)
        .backrooms$setLightmapTextureManager(oldLightmap);
    FogRendererContext.swappingManager.popSwapping();
}
```

> **Règle obligatoire (Codex Contract I-14) :** tout le bloc "swap → render → restore" doit être dans un `try/finally`. Exemple de structure imposée :
>
> ```java
> // PSEUDO-CODE — DO NOT COPY AS-IS
> swapToDimension(newWorld, newRenderer, helper);
> try {
>     invokeWrapper.accept(() -> { /* rendu effectif */ });
> } finally {
>     restoreDimension(oldWorld, oldRenderer, oldLightmap); // exécuté même si crash
> }
> ```
>
> Sans ce `finally`, une exception pendant `renderRemoteWorld()` laisse `Minecraft.level` pointant sur la fausse dimension pour le reste de la session — corruptions et crashs en cascade garantis. Cette règle est vérifiable par revue manuelle (grep I-14 sur `backrooms$setWorldRenderer`).

```java
```

**Pré-chargement des chunks :** La vue est fluide car `DestinationLoader` (§11) charge les chunks destination *en avance* via des tickets serveur, dès que le portail passe en état `PREWARMING` (§14). Quand le portail devient visible, les chunks sont déjà en mémoire — aucun délai de génération pendant le rendu.

**Ce qui évite l'écran de chargement :** le swap ne touche pas `Minecraft.player.dimension` ni le registre de dimension vanilla. L'écran de chargement n'est déclenché que par un changement de dimension *du joueur* (packet serveur), qui n'arrive qu'à la traversée réelle du portail (§10), masqué par `TransitionMasker`.

---

### §24.2 Affichage des entités à travers le portail (y compris le joueur)

**Problème rencontré :** Une entité à mi-chemin dans un portail doit être rendue en deux moitiés : une partie côté source, une autre côté destination. Sans cela, l'entité disparaît ou est rendue en double.

**Solution — double rendu avec clip plan GL :**

Chaque entité en collision avec un portail est rendue deux fois avec un clip plan différent.

```java
// PSEUDO-CODE — DO NOT COPY AS-IS
// Source IP : CrossPortalEntityRenderer.java
// Adaptation v4 → EntityEchoSystem + FrontClipping (§15.2)

// --- Rendu côté SOURCE (monde réel) ---
// Activer le clip plan "extérieur" : ne rend que la partie en dehors du portail
public static void beforeRenderingEntity(Entity entity, PoseStack matrixStack) {
    if (entityCollidesWithPortal(entity)) {
        Portal collidingPortal = getCollidingPortal(entity);
        // Clip plan positionné sur le plan du portail, normal vers l'extérieur
        FrontClipping.setupOuterClipping(matrixStack, collidingPortal);
    }
}

// --- Rendu côté DESTINATION (dans la vue distante du portail) ---
// Activer le clip plan "intérieur" : ne rend que la partie à l'intérieur du portail
private static void renderProjectedEntity(
    Entity entity, Portal collidingPortal, PoseStack matrixStack
) {
    Plane innerClip = collidingPortal.getInnerClipping();

    // Clip plan positionné sur le plan du portail, normal vers l'intérieur (inversé)
    FrontClipping.setupInnerClipping(innerClip, matrixStack.last().pose(), 0);
    renderEntityReadOnly(entity, matrixStack);
    FrontClipping.disableClipping();
}

// --- setupInnerClipping : équation du plan GL ---
private static double[] getClipEquation(Vec3 planePos, Vec3 planeNormal) {
    Vec3 cameraPos = getCurrentCameraPos();
    Vec3 posRelative = planePos.subtract(cameraPos);
    // Équation Plan : normal·P + c ≥ 0  →  fragments du bon côté passent
    double c = planeNormal.scale(-1).dot(posRelative);
    return new double[]{ planeNormal.x, planeNormal.y, planeNormal.z, c };
    // Activé via : GL11.glClipPlane(GL_CLIP_PLANE0, eq) + GL11.glEnable(GL_CLIP_PLANE0)
    // ou uniform gl_ClipDistance[0] en GLSL
}
```

**Ce que `FrontClipping` fait au GPU :** il configure `gl_ClipDistance[0]` (GLSL) ou `GL_CLIP_PLANE0` (pipeline fixe). Le GPU rejette automatiquement les fragments du mauvais côté du plan, sans calcul supplémentaire côté CPU.

**Pour le joueur en vue première personne :** quand la caméra est à cheval sur le plan du portail, la vue principale est déjà la vue distante (rendue par `PortalStencilRenderer.renderRemoteWorld()`). L'œil virtuel est positionné à `portal.transformPoint(playerEyePos)`. La coupure au plan du portail évite de voir "derrière soi" dans la dimension source.

> **Périmètre V1 — entités rendues cross-dim (Codex Contract I-15) :**
>
> | Entité | V1 | V2+ |
> |---|---|---|
> | Mobs / entités non-player dans la vue distante | ✓ `EntityEchoSystem` (read-only, budget `maxProxyEntitiesPerView`) | ✓ idem |
> | Joueur local (`LocalPlayer`) | ✗ **jamais rendu comme entité cross-dim** | ✗ idem (la vue est déjà correcte via la caméra miroir) |
> | Autres joueurs (multi) | ✗ hors scope V1 | À définir V2 (nécessite protocole custom de sync entités cross-dim) |
>
> Ne pas tenter de rendre `client.player` dans `EntityEchoSystem` : la vue première personne est gérée par la caméra distante (§24.6), pas par le rendu de l'entité joueur. Tenter de le rendre créerait un doublon visuel ou une boucle de rendu.

**Scaling et rotation portail :** si `portal.getScaling() != 1.0` ou `portal.getRotation() != null`, la transformation de l'entité projetée inclut la mise à l'échelle et la rotation, exactement comme pour la caméra distante (§9.1).

---

### §24.3 Portails visibles dans un portail (récursion stencil)

**Problème rencontré :** En rendant naïvement un portail dans un portail, on obtient soit une boucle infinie, soit un rendu incorrect où le portail interne "écrase" l'externe.

**Solution — stencil buffer comme compteur de profondeur :**

Le stencil buffer 8 bits joue le rôle de compteur de profondeur de nesting. Chaque portail incrémente la valeur (`GL_INCR`) dans sa zone, et le rendu du monde distant à ce niveau est masqué par `glStencilFunc(GL_EQUAL, depth+1, 0xFF)` — seuls les pixels exactement à cette profondeur sont écrits.

```java
// PSEUDO-CODE — DO NOT COPY AS-IS
// Source IP : RendererUsingStencil.doRenderPortal()
// Adaptation v4 → PortalStencilRenderer.renderOne() (§7.2)

private void renderOne(BackroomsPortalEntity portal, Matrix4f modelView) {
    int outerValue = PortalDepthTracker.currentDepth(); // 0 = monde réel, 1 = dans 1 portail, etc.

    // Étape 1 — Écrire le quad portail dans le stencil avec GL_INCR
    // Seuls les pixels déjà à outerValue passent → le nesting est correct
    GL11.glStencilFunc(GL_EQUAL, outerValue, 0xFF);
    GL11.glStencilOp(GL_KEEP, GL_KEEP, GL_INCR); // incrémente sur colorPass
    GL11.glColorMask(false, false, false, false); // pas de couleur — stencil seulement
    viewAreaRenderer.writeStencilQuad(portal, modelView);

    PortalDepthTracker.push(portal); // depth → outerValue + 1

    // Étape 2 — Effacer la depth dans la zone stencil (voir §24.4)
    clearDepthInPortalArea(portal, outerValue + 1);

    // Étape 3 — Rendre le monde distant uniquement sur pixels à depth outerValue + 1
    GL11.glStencilFunc(GL_EQUAL, outerValue + 1, 0xFF);
    GL11.glStencilMask(0x00); // ne pas modifier le stencil pendant le rendu monde
    renderRemoteWorld(portal);  // peut lui-même contenir des portails → récursion

    // Étape 4 — Restaurer la depth du quad portail (voir §24.5)
    viewAreaRenderer.restoreDepth(portal, modelView, outerValue + 1);

    PortalDepthTracker.pop(); // depth → outerValue
}
```

**Limite à 1 niveau (DA-3) :** `PortalDepthTracker.canRender()` vérifie `currentDepth <= maxRecursionDepth (= 1)`. Au-delà, `PortalSurfaceRenderer.renderFallback()` affiche du bruit/flou. Cela correspond à `maxPortalLayer = 2` dans la référence (0 = monde réel, 1 = vue dans 1 portail ; à 2, fallback).

**Pourquoi 8 bits suffisent :** le stencil 8 bits supporte 256 niveaux de nesting — bien au-delà de la limite pratique de 1 niveau en DA-3. La référence limite elle-même à 8 niveaux par défaut dans sa config.

**Reset défensif :** `PortalDepthTracker.resetForFrame()` est appelé en début de chaque frame (§8.3). Si une exception interrompt le pipeline en milieu de rendu, le compteur ne reste pas corrompu au frame suivant.

---

### §24.4 Pas de bugs eau, nuages et blocs transparents

**Problème rencontré :** Sans précaution, les blocs translucides (eau, verre) et les nuages vus dans un portail se trient incorrectement avec les objets du monde source. Le rendu apparaît derrière des blocs qu'il devrait traverser, ou en avant de blocs qu'il devrait cacher.

**Racine du bug :** sans effacement préalable, les valeurs de depth du monde source contaminent la zone portail. Un bloc d'eau distant rendu par le monde destination "pense" qu'il y a déjà des fragments à des profondeurs arbitraires dans sa zone — le tri de translucidité est faussé.

**Solution — effacement de depth dans la zone stencil avant rendu distant :**

```java
// PSEUDO-CODE — DO NOT COPY AS-IS
// Source IP : RendererUsingStencil.clearDepthOfThePortalViewArea()
// Adaptation v4 → PortalStencilRenderer.clearDepthInPortalArea() (§7.2 SPIKE-0.2)

private void clearDepthInPortalArea(BackroomsPortalEntity portal, int stencilValue) {
    // Stencil : seuls les pixels à stencilValue sont affectés
    GL11.glStencilFunc(GL_EQUAL, stencilValue, 0xFF);
    GL11.glStencilMask(0x00); // ne pas modifier le stencil

    // Désactiver le color write — on ne touche que le depth buffer
    GL11.glColorMask(false, false, false, false);

    // Forcer depth = 1.0 (distance maximale = "loin") sur toute la zone stencil
    // Les fragments du monde destination écriront par-dessus avec leurs vraies depths
    int originalDepthFunc = GL11.glGetInteger(GL_DEPTH_FUNC);
    GL11.glDepthFunc(GL_ALWAYS);  // toujours écrire, quelle que soit la depth courante
    GL11.glDepthRange(1.0, 1.0);  // mappage depth → 1.0 uniquement

    renderScreenTriangle(); // triangle plein écran masqué par stencil

    // Restaurer
    GL11.glColorMask(true, true, true, true);
    GL11.glDepthFunc(originalDepthFunc);
    GL11.glDepthRange(0.0, 1.0);
}
```

Après cet effacement, le monde destination est rendu avec des valeurs de depth *fraîches* dans sa zone. Le tri de translucidité (eau, verre) s'effectue correctement, indépendamment du monde source.

**Nuages :** La référence maintient un `CloudContext` séparé par dimension. Le swap de dimension (§24.1) remplace également le contexte cloud : les nuages de la dimension destination sont rendus avec leurs propres buffers de vertex, couleur et position — pas ceux de la dimension source.

```java
// PSEUDO-CODE — DO NOT COPY AS-IS
// Source IP : CloudContext.findAndTakeContext()
// Adaptation v4 → RemoteLevelRenderer (§6.4)

// Réutiliser le buffer vertex cloud si position + dimension + couleur correspondent
CloudContext ctx = CloudContext.findAndTakeContext(
    lastCloudsBlockX, lastCloudsBlockY, lastCloudsBlockZ,
    dimension,   // clé : chaque dimension a son propre contexte
    cloudColor   // clé : la couleur varie selon la dimension (Backrooms = brouillard dense)
);
if (ctx == null) {
    ctx = new CloudContext(); // forcer la reconstruction du buffer pour cette dim
}
```

**Blocs transparents empilés :** le pipeline de rendu translucide de Minecraft trie les quads par distance à la caméra *distante* (§24.1 — la caméra a été swappée). Les quads sont donc triés correctement depuis le point de vue de la caméra virtuelle, pas depuis le joueur réel.

---

### §24.5 Pas de bugs "portail visible à travers les blocs"

**Problème rencontré :** Le quad du portail est écrit dans le stencil sans test de depth — si un bloc est devant le portail, le stencil est quand même incrémenté. Résultat : la vue distante "perce" les murs.

**Solution en deux parties :**

**Partie 1 — Occlusion query : ne pas rendre si le portail est entièrement caché**

Avant d'engager le rendu complet, la référence lance une *occlusion query* OpenGL `GL_ANY_SAMPLES_PASSED` sur le quad du portail, *avec le test de depth actif* (pas de bypass). Si aucun fragment ne passe (portail entièrement derrière un mur), tout le rendu de la vue distante est sauté.

```java
// PSEUDO-CODE — DO NOT COPY AS-IS
// Source IP : PortalRenderInfo.renderAndDecideVisibility()
// Adaptation v4 → PortalStencilRenderer (phase de pré-filtre)

public static boolean renderAndDecideVisibility(Portal portal, Runnable queryRendering) {
    GlQueryObject thisFrameQuery  = visibility.acquireThisFrameQuery();
    GlQueryObject lastFrameQuery  = visibility.lastFrameQuery;

    // Rendre le quad avec GL_ANY_SAMPLES_PASSED actif
    // → le GPU compte combien de fragments passent le test depth ET stencil
    thisFrameQuery.performQueryAnySamplePassed(queryRendering);

    if (lastFrameQuery != null) {
        boolean lastVisible = lastFrameQuery.fetchQueryResult();
        if (!lastVisible) {
            // Portail caché au frame précédent → vérifier ce frame en synchrone
            // (stall GPU acceptable : portail rarement visible = cas rapide)
            return thisFrameQuery.fetchQueryResult();
        }
        // Portail visible au frame précédent → supposer visible maintenant
        // (évite la stall GPU dans le cas courant)
        return true;
    }
    return thisFrameQuery.fetchQueryResult();
}
```

**Impact :** si le portail est entièrement derrière un mur, `renderRemoteWorld()` n'est jamais appelé → économie GPU totale sur ce portail.

**Partie 2 — Restauration de depth après rendu : les blocs devant le portail l'occultent correctement**

Après avoir rendu le monde destination, la depth *réelle* du quad du portail est réécrite dans le depth buffer :

```java
// PSEUDO-CODE — DO NOT COPY AS-IS
// Source IP : ViewAreaRenderer.renderPortalArea() avec doModifyDepth = true
// Adaptation v4 → ViewAreaRenderer.restoreDepth() (§7.3)

public void restoreDepth(BackroomsPortalEntity portal, Matrix4f modelView, int stencilValue) {
    // Stencil : seuls les pixels à stencilValue (zone portail)
    GL11.glStencilFunc(GL_EQUAL, stencilValue, 0xFF);
    GL11.glStencilMask(0x00);

    // Color mask off — on écrit uniquement dans le depth buffer
    GL11.glColorMask(false, false, false, false);
    GL11.glDepthMask(true);
    GL11.glDepthFunc(GL_ALWAYS); // écrire la depth réelle du quad sans test

    // Rendre le quad portail → écrit la depth physique du portail dans le framebuffer
    renderPortalQuad(portal, modelView);

    GL11.glColorMask(true, true, true, true);
    GL11.glDepthFunc(/* restaurer */);
}
```

Après cette restauration :
- Un bloc **devant** le portail a une depth plus petite → il passe le test `GL_LEQUAL` et occulte correctement le portail ✓
- Un bloc **derrière** le portail (côté source) a une depth plus grande → il est caché par le portail ✓
- La vue distante (rendue à depth fraîche, §24.4) est correctement positionnée dans l'espace de depth ✓

---

### §24.6 Perspective correcte — rendu en vraie fenêtre sans distorsion

**Problème rencontré :** Rendre la vue distante avec la même matrice de projection que la caméra joueur donne un résultat correct *uniquement* si la caméra est face au portail. En vue oblique, la vue distante semble "glisser" — elle ne colle pas au rectangle physique du portail.

**Racine du bug :** la vue distante est une fenêtre dans l'espace 3D, pas une image projetée. La caméra distante doit être le *symétrique exact* de la caméra source par la transformation du portail.

**Solution — caméra miroir + near clip plan sur le portail :**

```java
// PSEUDO-CODE — DO NOT COPY AS-IS
// Source IP : TransformationManager + FrontClipping
// Adaptation v4 → RemoteCamera.compute() + FrontClipping (§9.1)

public CameraState compute(PlayerEntity player, BackroomsPortalEntity portal) {

    // 1. Position distante = symétrique de l'oeil par portal.transformPoint()
    //    (rotation + scaling + translation vers la destination)
    Vec3d remotePos = portal.transformPoint(player.getEyePos());

    // 2. Rotation distante = composition de la rotation joueur et de la rotation portail
    //    La même matrice de projection (FOV, aspect) est conservée — seul l'espace de vue change
    DQuaternion playerRot = DQuaternion.getCameraRotation(player.getPitch(), player.getYaw());
    DQuaternion portalRot = (portal.getRotation() != null)
        ? portal.getRotation() : DQuaternion.identity;
    DQuaternion remoteRot = playerRot.hamiltonProduct(portalRot); // SPIKE-0.C : valider l'ordre

    return new CameraState(remotePos, remoteRot.toMatrix4f(), player.getCameraFov());
    // FOV identique → pas de déformation de perspective → rendu comme une vraie fenêtre
}
```

**Near clip plan aligné sur le portail :** Sans clip plan, le monde destination est rendu *derrière* le plan du portail mais aussi *devant* — des fragments parasites peuvent s'afficher entre la caméra distante et le portail. La référence résout cela avec `FrontClipping.setupInnerClipping()` :

```java
// PSEUDO-CODE — DO NOT COPY AS-IS
// Source IP : FrontClipping.setupInnerClipping() / getClipEquationInner()
// Adaptation v4 → FrontClipping (§7.2 — utilisé dans renderRemoteWorld())

public static void setupInnerClipping(Plane clipping, Matrix4f modelView, double adjustment) {
    Vec3 cameraPos   = getCurrentCameraPos();
    Vec3 planeNormal = clipping.normal();
    Vec3 planePos    = clipping.pos().add(planeNormal.scale(adjustment)).subtract(cameraPos);

    // Équation du plan : fragments satisfaisant normal·P + c ≥ 0 sont gardés
    // Le plan est positionné exactement sur la surface du portail (côté destination)
    // → rien ne peut s'afficher "devant" le portail dans la vue distante
    double c = planeNormal.scale(-1).dot(planePos);
    double[] equation = { planeNormal.x, planeNormal.y, planeNormal.z, c };

    // Activer le clip plan custom GL
    GL11.glClipPlane(GL11.GL_CLIP_PLANE0, equation);
    GL11.glEnable(GL11.GL_CLIP_PLANE0);
    // Ou via uniform GLSL : gl_ClipDistance[0] = dot(pos, clipPlane);
}
```

**Résultat :** la vue distante est mathématiquement identique à ce qu'un observateur verrait en regardant physiquement par une fenêtre percée dans le mur, avec le même FOV. Pas de distorsion, pas de décalage en vue oblique, pas de fragments parasites.

**Scaling portail :** si `portal.getScaling() != 1.0`, `portal.transformPoint()` incorpore le facteur d'échelle. La caméra distante est plus proche ou plus éloignée du centre de la destination, simulant un portail qui "rapetisse" ou "agrandit" le monde de l'autre côté.

---

### §24.7 Optimisations performances

**Contexte :** Rendre une scène complète d'une dimension distante à chaque frame est intrinsèquement coûteux. Plusieurs mécanismes amortissent ce coût, chacun avec un impact mesurable.

#### a) Frustum culling portal-aware — ne rendre que ce qui est visible dans le rectangle

La caméra distante ne peut voir *que* ce qui se trouve dans le cône délimité par les 4 coins du portail. La référence construit un frustum précis à partir de ces 4 sommets, plus serré que le frustum normal de la caméra distante.

```java
// PSEUDO-CODE — DO NOT COPY AS-IS
// Source IP : FrustumCuller.getFlatPortalInnerFrustumCullingFunc()
// Adaptation v4 → PortalStencilRenderer — frustum passé à RemoteLevelRenderer

public static BoxPredicateF getFlatPortalInnerFrustumCullingFunc(
    BackroomsPortalEntity portal, Vec3 cameraPos
) {
    // 4 coins du portail, transformés vers le repère de la caméra distante
    Vec3[] corners = getPortalFourCornersCounterClockwise(portal);
    Vec3[] cornersTransformed = Arrays.stream(corners)
        .map(c -> portal.transformPoint(c).subtract(cameraPos))
        .toArray(Vec3[]::new);

    // 4 plans de frustum construits à partir des 4 arrêtes extérieures
    Frustum4Planes fourPlanes = buildFrustumFromCornersCounterClockwise(cornersTransformed);

    // Retourne un prédicat : chunk AABB entièrement hors de ce frustum → ne pas rendre
    return (minX, minY, minZ, maxX, maxY, maxZ) ->
        fourPlanes.isFullyOutside(minX, minY, minZ, maxX, maxY, maxZ);
}

record Frustum4Planes(
    float p0X, float p0Y, float p0Z, float p0W,
    float p1X, float p1Y, float p1Z, float p1W,
    float p2X, float p2Y, float p2Z, float p2W,
    float p3X, float p3Y, float p3Z, float p3W
) {
    public boolean isFullyOutside(
        float minX, float minY, float minZ, float maxX, float maxY, float maxZ
    ) {
        // Un chunk est invisible si toutes ses AABB sont entièrement derrière l'un des 4 plans
        return isFullyBehindPlane(minX, minY, minZ, maxX, maxY, maxZ, p0X, p0Y, p0Z, p0W)
            || isFullyBehindPlane(minX, minY, minZ, maxX, maxY, maxZ, p1X, p1Y, p1Z, p1W)
            || isFullyBehindPlane(minX, minY, minZ, maxX, maxY, maxZ, p2X, p2Y, p2Z, p2W)
            || isFullyBehindPlane(minX, minY, minZ, maxX, maxY, maxZ, p3X, p3Y, p3Z, p3W);
    }
}
```

**Impact pratique :** un portail 2×3 blocs vu de face sur 10 blocs de distance → angle de vue ~11°. Avec un render distance de 16 chunks, le frustum normal de la caméra distante couvrirait ~800 chunks. Le frustum portail-aware en couvre ~4. **Facteur de réduction : ~200×** sur les appels de culling de chunks.

#### b) Occlusion query — éviter le rendu complet si le portail est invisible

Décrit en §24.5 : `GL_ANY_SAMPLES_PASSED` teste si le quad du portail a des pixels visibles *avec le test de depth actif*. Si aucun pixel ne passe (portail entièrement caché par des blocs), `renderRemoteWorld()` n'est pas appelé du tout. La query du frame précédent est réutilisée pour éviter la stall CPU-GPU dans le cas courant (portail visible).

#### c) Réduction dynamique de la distance de rendu selon la profondeur

```java
// PSEUDO-CODE — DO NOT COPY AS-IS
// Source IP : RenderStates.getRenderRange()
// Adaptation v4 → RemoteWorldRenderInfo.renderDistance (§7.2) + BackroomsConfig

public static int computePortalRenderDistance(BackroomsConfig config) {
    int range = config.portalRenderDistance; // distance de rendu de base (config)

    // Lag détecté → réduction à 1 chunk pour rester jouable
    if (isLaggy) {
        range = 16;
    }
    // Récursion : profondeur 1 → distance divisée par 1 (inchangée)
    //             profondeur 2 → distance divisée par 2, etc.
    // En DA-3 (maxDepth = 1) ce cas ne se produit qu'avec le fallback actif, mais
    // la logique est présente pour le cas où DA-3 serait révisée.
    int portalLayer = PortalDepthTracker.currentDepth();
    if (portalLayer > 1) {
        range /= portalLayer;
    }

    return range;
}
```

**Lag detection :** La référence mesure le temps de frame (delta). Si le frame précédent a dépassé un seuil (typiquement > 100 ms), `isLaggy = true` est activé pour 1 frame → réduction à 1 chunk. Cela évite l'emballement : un frame lent n'engendre pas le suivant encore plus lent.

#### d) Chargement progressif des chunks en avance (§11)

`DestinationLoader` charge les chunks destination *bien avant* que le joueur n'atteigne le portail, dès l'état `PREWARMING` (§14, distance 16 blocs). Quand le portail devient réellement visible, les chunks sont déjà construits — aucun stall de génération pendant le rendu.

Les tickets utilisent `TicketType<UUID>` keyed sur l'UUID du portail (§11.1), ce qui garantit leur libération propre sans fuite quand le portail est supprimé (`DestinationLoader.release()`).

#### e) Réutilisation des buffers de chunks par dimension

Chaque `RemoteLevelRenderer` (§6.4) maintient ses propres VBOs de chunks. Ces buffers ne sont pas reconstruits à chaque frame — le `LevelRenderer` distant utilise le même pipeline de chunk dirty/rebuild que le monde principal. Un chunk de la dimension destination n'est retesselé que s'il a changé (placement de bloc, lumière), exactement comme un chunk normal.

**Résumé des mécanismes combinés :**

| Mécanisme | Condition d'activation | Économie GPU typique |
|---|---|---|
| Frustum portail-aware | Toujours (portail visible) | ~90-99% des chunks cullés |
| Occlusion query | Portail derrière un obstacle | 100% du rendu distant évité |
| Réduction distance (lag) | Frame > 100 ms | Distance → 1 chunk |
| Réduction distance (récursion) | Profondeur ≥ 2 (hors DA-3) | Distance / profondeur |
| Pré-chargement chunks | État PREWARMING–VISIBLE | Élimine les stalls de génération |
| Réutilisation VBOs | Toujours (chunks non modifiés) | Retesselation = 0 si stable |

---

*Fin du document. Pour toute ambiguïté, l'autorité est `arborescence_v4.md` (rang 1) puis ce document (rang 2). Cf. arbo §14.1.*
