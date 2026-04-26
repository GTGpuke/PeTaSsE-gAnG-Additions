# Backrooms Mod — Documentation Technique V3
## Mod Fabric 1.21.1 · Veil Rendering · Portails Illusionnistes

---

## 1. Vue d'ensemble du projet

Ce mod Minecraft Fabric 1.21.1 implémente une expérience **Backrooms** reposant sur une **illusion sensorielle cohérente** plutôt que sur une vraie continuité physique inter-dimensionnelle.

L'approche centrale : **Veil + illusion de seamless**. On ne relie pas réellement deux espaces dans le moteur. On donne au joueur une impression très forte de continuité via du rendu distant, des téléportations masquées, un design spatial trompeur, des entités proxy et un système audio cross-space.

### Philosophie

Le genre backrooms repose sur la **perception**, la **confusion**, l'**ambiance**, les **faux repères** et les **incohérences spatiales ressenties**. Une illusion bien construite est plus efficace qu'un vrai portail physique pour ce type d'expérience.

### Séparation fondamentale

> **Veil = couche de rendu**
>
> **Portail = logique d'illusion**
>
> **Shader = ambiance et masquage**

Ces trois piliers sont strictement indépendants. On peut changer Veil, remplacer le shader source, ou modifier la logique de portails sans impacter les autres.

### Règle de rendu dimensionnel

> **Dans les Backrooms = Veil uniquement. Monde normal = shaders du joueur.**

Les dimensions backrooms utilisent un pipeline visuel dédié. Iris et les shaders externes sont désactivés dans ces dimensions pour garantir l'ambiance voulue et éviter les conflits de rendu. C'est à la fois une décision technique (pas de coexistence de deux systèmes de shaders) et artistique (chaque level a son identité visuelle imposée).

### Ce que le mod produit

- Portes/ouvertures montrant un autre lieu en temps réel
- Passages entre zones sans chargement visible
- Couloirs bouclés, salles impossibles par duplication
- Entités visibles et menaçantes à travers les seuils
- Ambiance sonore transmise à travers les ouvertures (OpenAL EFX)
- Effets visuels de distorsion, flicker, glitch
- Transitions entre niveaux quasi-invisibles
- Aggro simulée cross-seuil
- Pipeline visuel dédié par dimension (Iris désactivé dans les Backrooms)

---

## 2. Limitations structurelles

Ces limitations sont **par conception**, pas par manque de temps. Elles définissent le périmètre dur du mod et ne doivent jamais être contournées.

| Limitation | Raison |
|---|---|
| Pas de collision inter-espace | Aucun lien physique entre les espaces — l'illusion est visuelle uniquement |
| Pas de raycast cross-space natif | Le moteur ne sait pas tracer un rayon à travers un portail illusionniste |
| Pas de projectile traversant | Un projectile ne peut pas passer nativement d'un espace à l'autre |
| Pas de pathfinding vanilla cross-seuil | L'IA de navigation Minecraft ne connaît pas les portails |
| Pas de récursion de portail | Pas de vue d'un portail à travers un autre portail |
| Pas de non-euclidien mathématiquement réel | L'illusion repose sur la duplication/redirection, pas sur une géométrie non-euclidienne |
| Pas de clone logique d'entités | Les entités proxy sont visuelles uniquement — une seule source d'autorité |
| Pas de garantie de compatibilité universelle | Conflits possibles avec d'autres mods de rendu, shaders, ou audio |
| Pas de coexistence Iris + Veil dans les Backrooms | Les shaders Iris sont désactivés dans les dimensions backrooms |

---

## 3. Stack technique

| Composant | Version / Technologie |
|---|---|
| Minecraft | 1.21.1 |
| Mod loader | Fabric |
| Fabric API | version compatible 1.21.1 |
| Rendering | **Veil** (rendering framework Fabric) |
| Shaders | Shader custom dérivé d'un shader open source, intégré via Veil |
| Audio | OpenAL EFX (natif Minecraft / LWJGL) |
| Compatibilité Iris | Soft-dependency : détection + désactivation dans les dimensions backrooms |
| Langage | Java 21 |

---

## 4. Règles de perception joueur

Ces règles sont le **contrat de design** de tout le projet. Elles guident chaque décision technique et chaque choix d'implémentation. Quand un dilemme technique se pose, ces règles tranchent.

### Les 4 lois de l'illusion

| # | Règle | Implication |
|---|---|---|
| 1 | **Ne jamais montrer un changement brutal dans le champ de vision** | Tout changement d'état (TP, swap de salle, apparition/disparition) doit être masqué ou progressif |
| 2 | **Ne jamais contredire une information visuelle immédiate** | Ce que le joueur voit dans le portail = ce qu'il trouvera après. Pas d'écart entre vue distante et réalité |
| 3 | **Toujours privilégier la cohérence locale sur la cohérence globale** | Le joueur ne doit jamais percevoir une incohérence dans son voisinage immédiat, même si la topologie globale est impossible |
| 4 | **Masquer les transitions par bruit sensoriel** | Visuel (distorsion, flicker) ou audio (son soudain, changement d'ambiance) — jamais de transition "à nu" |

### Application concrète

- Avant un TP : toujours un masquage (shader, ombre, couloir étroit)
- Changement de salle : le joueur ne doit pas voir la géométrie changer
- Disparition d'entité proxy : fondu ou sortie de champ, jamais de pop
- Changement acoustique : crossfade progressif, jamais de coupure sèche
- Si un système échoue : afficher du bruit/obscurité plutôt qu'un artefact

---

## 5. Source of Truth

Chaque donnée du système a **un seul propriétaire**. Les autres systèmes lisent, jamais n'écrivent.

| Donnée | Propriétaire (Source of Truth) | Lecteurs |
|---|---|---|
| Topologie réelle des niveaux | `RoomGraph` | TransitionSystem, SpatialIllusionSystem, Audio |
| État d'un portail (cycle de vie) | `TransitionSystem` → `PortalState` | PortalManager, PortalViewSystem, Audio, Shader, EntityEcho |
| Sélection des portails actifs | `PortalManager` | PortalViewSystem, Audio, EntityEcho |
| Position/état réel des entités | **Monde destination/source uniquement** | EntityEchoSystem (lecture seule) |
| Uniforms shader actifs | `ShaderManager` + `UniformProviders` | Pipeline de rendu Veil |
| Acoustique perçue par le joueur | `CrossSpaceAudioSystem` | — |
| Configuration runtime | `BackroomsConfig` | Tous les systèmes |
| Rendu distant actif | `PortalViewSystem` | TransitionSystem, ShaderPipeline |
| État Iris (actif/désactivé) | `IrisBridge` | ShaderPipeline, VeilPipelineBridge |
| Métriques debug runtime | `DebugMetrics` | Debug HUD |

**Règle absolue** : si un système a besoin d'une donnée dont il n'est pas propriétaire, il la demande au propriétaire via une interface de lecture. Il ne la duplique pas, ne la cache pas, et ne la calcule pas lui-même.

---

## 6. Machine d'état des portails

Chaque `PortalLink` suit un cycle de vie strict. Les transitions ne sont pas libres : seuls les chemins définis ici sont autorisés.

```
                    ┌──────────────────────────────────┐
                    │                                  │
                    ▼                                  │
              ┌──────────┐                             │
              │ INACTIVE │  ← état initial             │
              └────┬─────┘                             │
                   │ joueur < distance d'activation    │
                   ▼                                   │
             ┌───────────┐                             │
             │ PREWARMING│  ← préchargement destination│
             └─────┬─────┘    rendu distant préparé    │
                   │ destination prête                  │
                   ▼                                   │
              ┌─────────┐                              │
              │ VISIBLE  │  ← vue distante rendue      │
              └────┬─────┘    audio cross-space actif   │
                   │ joueur entre dans la zone seuil    │
                   ▼                                   │
              ┌────────┐                               │
              │ ARMED   │  ← prêt à déclencher le TP   │
              └────┬────┘    masquage shader prêt       │
                   │ joueur dépasse triggerDepth        │
                   ▼                                   │
          ┌───────────────┐                            │
          │ TRANSITIONING │  ← TP en cours              │
          └───────┬───────┘    effet masquage actif     │
                  │ TP terminé, joueur dans new zone    │
                  ▼                                    │
             ┌──────────┐                              │
             │ COOLDOWN  │  ← stabilisation post-TP    │
             └─────┬─────┘    effet se dissipe          │
                   │ cooldown terminé                   │
                   └───────────────────────────────────┘
                     (retour INACTIVE)
```

**Transitions inversées autorisées** :

- `PREWARMING → INACTIVE` : joueur s'éloigne avant que la destination soit prête
- `VISIBLE → INACTIVE` : joueur s'éloigne du portail
- `ARMED → VISIBLE` : joueur recule hors de la zone seuil sans franchir

**Transitions interdites** :

- Jamais de saut direct `INACTIVE → ARMED` ou `INACTIVE → TRANSITIONING`
- Jamais de `TRANSITIONING → VISIBLE` (le TP est irréversible une fois déclenché)

---

## 7. Budget performance

Ces limites sont les valeurs par défaut. Elles sont configurables via `BackroomsConfig` mais les valeurs ci-dessous sont les plafonds recommandés.

| Ressource | Budget max | Notes |
|---|---|---|
| Vues distantes actives simultanées | **2** | Au-delà, impact GPU trop fort |
| Résolution rendu distant | **50%** de la résolution écran | Configurable (25%, 50%, 75%, 100%) |
| Distance de rendu distant | **32 blocs** | Inférieur au render distance joueur |
| Fréquence update vue distante | **1 frame sur 2** | Passe à 1/3 si FPS < 30 |
| Entités proxy par vue distante | **4** | Au-delà, rendu trop coûteux |
| Sources audio cross-space | **6** | Sélection par priorité (distance, importance) |
| Distance max projection audio | **24 blocs** | Au-delà, le son n'est plus perçu |
| Durée max effet de transition | **150 ms** | Au-delà, le masquage devient visible |

### Presets de performance

| Preset | Vues | Résolution | Entités proxy | Sources audio |
|---|---|---|---|---|
| Low | 1 | 25% | 2 | 3 |
| Medium | 2 | 50% | 4 | 6 |
| High | 2 | 75% | 6 | 8 |

---

## 8. Stratégie de fallback

Quand un sous-système échoue ou surcharge, le mod **dégrade gracieusement** au lieu de crasher ou casser l'illusion.

| Situation | Fallback |
|---|---|
| Rendu distant échoue (framebuffer, shader) | Surface du portail affiche du bruit/obscurité statique — jamais un trou transparent |
| Destination pas prête (chunks non chargés) | Portail reste en `PREWARMING`, transition bloquée — le joueur ne peut pas passer tant que ce n'est pas prêt |
| Framerate chute sous 30 FPS | Réduction automatique : update 1/3 frames, résolution 25%, max 1 vue active |
| Audio cross-space surcharge | Réduction des sources secondaires, conservation uniquement de la source la plus prioritaire |
| Entité proxy incohérente (état désynchronisé) | L'écho est retiré silencieusement (fondu vers invisible) plutôt que de montrer un artefact |
| Shader compilation échoue | Effets post-process désactivés, rendu vanilla Minecraft — le portail fonctionne toujours mais sans effets |
| Iris non détectable / API incompatible | Le mod ne tente pas de désactiver Iris — avertissement en log, l'utilisateur désactive manuellement |
| Caméra en angle extrême sur un portail | Fallback shader (flou, bruit) au lieu d'un rendu distant déformé |

**Principe** : chaque sous-système doit pouvoir fonctionner de manière dégradée. Aucun sous-système en échec ne doit bloquer les autres.

---

## 9. Architecture modulaire — Les 3 piliers

```
backrooms-mod/
│
├── src/main/java/com/backrooms/
│   │
│   ├── config/                       ← PILIER 1 : Configuration & Abstraction Veil
│   │   ├── BackroomsConfig.java
│   │   ├── RenderSystemConfig.java
│   │   ├── veil/                     ← Abstraction Veil découpée
│   │   │   ├── VeilFramebufferBridge.java
│   │   │   ├── VeilPipelineBridge.java
│   │   │   ├── VeilShaderBridge.java
│   │   │   └── VeilRegistration.java
│   │   └── compat/                   ← Compatibilité mods externes
│   │       └── IrisBridge.java
│   │
│   ├── shader/                       ← PILIER 2 : Shaders custom
│   │   ├── ShaderManager.java
│   │   ├── ShaderPipeline.java
│   │   ├── PostProcessRegistry.java
│   │   ├── effects/
│   │   │   ├── DistortionEffect.java
│   │   │   ├── GlitchEffect.java
│   │   │   ├── FlickerEffect.java
│   │   │   ├── VignetteEffect.java
│   │   │   └── TransitionEffect.java
│   │   └── uniforms/
│   │       ├── TimeUniform.java
│   │       ├── PlayerStateUniform.java
│   │       └── PortalProximityUniform.java
│   │
│   ├── portal/                       ← PILIER 3 : Système de portails seamless
│   │   ├── PortalManager.java        ← Orchestrateur global (§12)
│   │   ├── PortalState.java
│   │   ├── PortalLink.java
│   │   ├── PortalEndpoint.java
│   │   ├── PortalPriorityScore.java  ← Scoring de sélection (§13)
│   │   ├── view/
│   │   │   ├── PortalViewSystem.java
│   │   │   ├── RemoteCamera.java
│   │   │   ├── RemoteCameraGuard.java  ← Protection cas caméra extrêmes (§14)
│   │   │   ├── PortalFramebuffer.java
│   │   │   └── PortalSurfaceRenderer.java
│   │   ├── transition/
│   │   │   ├── TransitionSystem.java
│   │   │   ├── TransitionContext.java
│   │   │   ├── ThresholdDetector.java
│   │   │   ├── TeleportExecutor.java
│   │   │   ├── TransitionMasker.java
│   │   │   └── FrameSyncPolicy.java  ← Synchro frame-exact (§15)
│   │   ├── spatial/
│   │   │   ├── SpatialIllusionSystem.java
│   │   │   ├── RoomGraph.java
│   │   │   ├── RoomSegment.java
│   │   │   ├── RoomVariantRegistry.java
│   │   │   ├── RedirectRule.java
│   │   │   ├── RedirectResolver.java ← Résolution avec tie-breaking
│   │   │   └── LoopingCorridorManager.java
│   │   ├── entity/
│   │   │   ├── EntityEchoSystem.java
│   │   │   ├── ProxyEntityRenderer.java
│   │   │   ├── CrossSpaceEntitySync.java
│   │   │   └── AggroSimulationSystem.java
│   │   └── audio/
│   │       ├── CrossSpaceAudioSystem.java
│   │       ├── SpatialSoundProjector.java
│   │       ├── AudioSourceStabilizer.java ← Hysteresis et stabilité (§16)
│   │       ├── EFXFilterManager.java
│   │       ├── OcclusionCalculator.java
│   │       └── AcousticZoneProfile.java
│   │
│   ├── debug/                        ← Métriques et debug HUD
│   │   ├── DebugMetrics.java
│   │   └── DebugHudRenderer.java
│   │
│   ├── level/
│   │   ├── BackroomsLevel.java
│   │   ├── LevelRegistry.java
│   │   └── LevelTheme.java
│   │
│   └── BackroomsMod.java             ← Entry point
│
├── src/main/resources/
│   ├── fabric.mod.json
│   ├── assets/backrooms/
│   │   ├── shaders/
│   │   │   ├── core/
│   │   │   │   ├── portal_view.vsh
│   │   │   │   ├── portal_view.fsh
│   │   │   │   └── portal_view.json
│   │   │   └── post/
│   │   │       ├── distortion.fsh
│   │   │       ├── glitch.fsh
│   │   │       ├── flicker.fsh
│   │   │       ├── vignette.fsh
│   │   │       └── transition_mask.fsh
│   │   ├── textures/
│   │   └── models/
│   └── data/backrooms/
│       └── levels/
│
└── build.gradle
```

---

## 10. PILIER 1 — Configuration, Abstraction Veil & Compatibilité Iris

### Objectif

Isoler **toutes** les dépendances vers Veil derrière une couche d'abstraction. Gérer la coexistence avec Iris via une stratégie de dimension switching. Si Veil change d'API, de nommage, ou si on veut migrer, seul ce pilier est impacté.

### Abstraction Veil — découpée en composants spécialisés

#### `VeilFramebufferBridge.java`

```java
/**
 * Abstraction over Veil's framebuffer management.
 * Creates, resizes, and destroys offscreen render targets used by PortalViewSystem.
 */
public class VeilFramebufferBridge {
    PortalFramebuffer createPortalBuffer(int width, int height);
    void resizePortalBuffer(PortalFramebuffer buffer, int width, int height);
    void destroyPortalBuffer(PortalFramebuffer buffer);
    void bindForRendering(PortalFramebuffer buffer);
    void unbind();
}
```

#### `VeilPipelineBridge.java`

```java
/**
 * Abstraction over Veil's render pipeline.
 * Injects custom render passes and post-processing into Veil's chain.
 */
public class VeilPipelineBridge {
    void registerPostProcess(String id, PostProcessEffect effect);
    void removePostProcess(String id);
    void injectBeforeWorldRender(Runnable task);
    void injectAfterWorldRender(Runnable task);
    int getActivePostProcessCount();
}
```

#### `VeilShaderBridge.java`

```java
/**
 * Abstraction over Veil's shader system.
 * Registers custom shader programs and manages their lifecycle.
 */
public class VeilShaderBridge {
    void registerShaderProgram(ResourceLocation id, ShaderPipeline pipeline);
    void unregisterShaderProgram(ResourceLocation id);
    boolean isShaderAvailable(ResourceLocation id);
}
```

**Règle** : aucune classe hors du package `config.veil` n'importe directement une classe Veil. Tout passe par ces trois ponts.

---

### Stratégie Iris / Veil — Dimension Switching

#### Principe

| Contexte | Pipeline de rendu |
|---|---|
| Monde normal (overworld, nether, end, etc.) | Shaders du joueur (Iris) autorisés normalement |
| Dimensions backrooms (`backrooms:*`) | **Veil uniquement** — Iris désactivé |

C'est une règle binaire, sans exception : dans les Backrooms, le joueur voit le rendu imposé par le mod. Pas de coexistence, pas de mix, pas de "ça dépend du shader du joueur".

#### Pourquoi

- **Technique** : deux systèmes de shaders actifs simultanément = artefacts visuels, conflits de framebuffer, blending incohérent
- **Artistique** : chaque level backrooms a une identité visuelle précise (teinte jaunâtre Level 0, obscurité Level 3, bleu aquatique Poolrooms) que les shaders du joueur casseraient
- **Fiabilité** : le rendu est prévisible et testable quand on contrôle tout le pipeline

#### `IrisBridge.java`

```java
/**
 * Soft-dependency bridge to Iris.
 * Detects Iris presence at runtime and manages shader pack toggling
 * when the player enters/exits backrooms dimensions.
 *
 * Iris is a SOFT dependency: if Iris is absent, this class does nothing.
 * If Iris is present but its API is incompatible, a warning is logged
 * and no automatic toggling occurs (fallback: user disables manually).
 *
 * Source of Truth for: current Iris state (active/disabled).
 */
public class IrisBridge {

    // --- Detection ---
    boolean isIrisPresent();
    boolean isIrisApiCompatible();

    // --- State ---
    boolean areIrisShadersCurrentlyActive();

    // --- Dimension switching ---

    /**
     * Called when the player enters a backrooms dimension.
     * Disables Iris shader pack rendering.
     * Saves the player's current Iris config to restore later.
     */
    void onEnterBackrooms();

    /**
     * Called when the player exits a backrooms dimension.
     * Restores Iris shader pack rendering to the player's saved config.
     */
    void onExitBackrooms();

    // --- Fallback ---

    /**
     * If Iris API is not compatible or toggling fails:
     * - Log a warning
     * - Do NOT crash
     * - Inform the player via chat message that manual Iris disable is recommended
     */
    void handleFallback(String reason);
}
```

#### Pipeline de dimension switching

```
Joueur dans le monde normal (Iris actif, ses shaders)
  │
  │ → Entrée dans une dimension backrooms:*
  │
  ▼
IrisBridge.onEnterBackrooms()
  → Sauvegarde config Iris actuelle
  → Désactivation du shader pack Iris
  → Activation pipeline Veil + shaders custom backrooms
  → Transition visuelle (fondu bref pour masquer le switch)
  │
  │ → Le joueur est dans les Backrooms (Veil uniquement)
  │
  │ → Sortie de la dimension backrooms
  │
  ▼
IrisBridge.onExitBackrooms()
  → Désactivation pipeline Veil backrooms
  → Restauration config Iris sauvegardée
  → Réactivation du shader pack Iris du joueur
  → Transition visuelle (fondu bref)
```

#### Points d'attention

- **Détection Iris** : via `FabricLoader.getInstance().isModLoaded("iris")` — jamais d'import direct de classes Iris hors de `IrisBridge`
- **API Iris** : vérifier la présence de l'API publique de toggling. Si l'API a changé, fallback (log + message joueur)
- **Timing du switch** : le changement de pipeline doit être masqué par un fondu/transition, cohérent avec les Règles de Perception (§4)
- **Sauvegarde config** : si le joueur crash dans les Backrooms, la config Iris doit être restaurée au prochain lancement
- **Pas de force-disable permanent** : le mod ne modifie jamais la config Iris sur disque, seulement en runtime

#### Présentation utilisateur

Le mod peut présenter cette contrainte comme une feature :

> *"Les Backrooms utilisent un pipeline visuel dédié pour garantir l'ambiance voulue."*

---

### `BackroomsConfig.java`

```java
public class BackroomsConfig {
    // --- Performance (voir §7 Budget performance) ---
    int maxActivePortalViews = 2;
    int portalRenderResolutionScale = 50;   // % de la résolution écran
    int portalRenderDistance = 32;           // blocs
    int portalUpdateFrequency = 2;          // 1 frame sur N
    int maxProxyEntitiesPerView = 4;
    int maxCrossSpaceAudioSources = 6;

    // --- Transition ---
    float teleportTriggerDepth = 0.3f;      // profondeur de franchissement avant TP (blocs)
    boolean enableTransitionEffects = true;
    float transitionEffectDuration = 0.15f; // secondes
    float portalActivationDistance = 16.0f; // distance d'activation (PREWARMING)
    float portalVisibleDistance = 10.0f;    // distance de rendu distant
    float portalArmDistance = 3.0f;         // distance de zone seuil

    // --- Audio ---
    boolean enableCrossSpaceAudio = true;
    float audioMaxCrossDistance = 24.0f;
    float doorOcclusionFactor = 0.6f;
    int audioSourceMinLifetimeMs = 200;     // hysteresis : durée min d'une source (§16)
    float audioVolumeSmoothing = 0.15f;     // facteur de lissage volume (0–1)

    // --- Shader ---
    boolean enableGlitchEffects = true;
    float glitchIntensityBase = 0.1f;
    float distortionNearPortal = 0.3f;

    // --- Iris ---
    boolean enableIrisAutoToggle = true;    // toggle auto Iris dans les backrooms
    float irisSwitchFadeDuration = 0.3f;    // durée du fondu au changement (secondes)

    // --- Camera safety ---
    float portalViewMinAngle = 15.0f;       // angle min joueur↔portail avant fallback (degrés)
    float portalViewMaxPlayerDistance = 0.2f; // distance min joueur↔plan portail avant clamp (blocs)

    // --- Fallback ---
    int fpsThresholdForDegradation = 30;

    // --- Debug ---
    boolean debugPortalBounds = false;
    boolean debugShowTransitionZones = false;
    boolean debugAudioSources = false;
    boolean debugShowPortalState = false;
    boolean debugShowMetricsHud = false;     // active le debug HUD (§17)
}
```

### `RenderSystemConfig.java`

Paramètres spécifiques au rendu, séparés de la config gameplay pour permettre des presets de performance (low/medium/high). Contient la logique de sélection automatique de preset basée sur le matériel détecté.

---

## 11. PILIER 2 — Shaders Custom

### Objectif

Gérer le shader custom **dérivé d'un shader open source**, ainsi que tous les effets de post-processing propres au mod. Ce pilier communique avec le Pilier 1 (VeilShaderBridge, VeilPipelineBridge) pour s'injecter dans la pipeline de rendu, mais ne connaît rien du système de portails.

### Source du shader

Le shader de base est dérivé d'un shader open source (licence à documenter dans le repo). Les modifications portent sur :

- Adaptation au pipeline Veil
- Ajout d'uniforms custom (temps, proximité portail, état joueur)
- Effets de distorsion/glitch/flicker spécifiques aux backrooms

### `ShaderManager.java`

```java
/**
 * Manages shader lifecycle: loading, compilation, hot-reload (debug), cleanup.
 * Shaders are loaded from assets/backrooms/shaders/
 *
 * Source of Truth for: shader compilation state, active uniform values.
 * Fallback: if compilation fails, all post-process effects are disabled
 *           and the mod continues with vanilla rendering.
 */
public class ShaderManager {
    void initialize();
    void reload();
    ShaderProgram get(ResourceLocation id);
    void applyUniforms(ShaderProgram program, UniformContext ctx);
    boolean isOperational();              // false si compilation a échoué
    void cleanup();
}
```

### `ShaderPipeline.java`

```
Pipeline de rendu (ordre d'exécution) :
0. [IrisBridge] Si dimension backrooms → Iris désactivé, Veil actif
1. [Veil] World render standard
2. [Portal] Rendu des vues distantes dans les framebuffers portail
3. [Portal] Composition des textures portail sur les surfaces
4. [Veil] Post-processing chain :
   a. Vignette (permanente, légère)
   b. Distortion (proximité portail — uniforme portalProximity)
   c. Glitch (aléatoire, basé sur le temps + événements)
   d. Flicker (luminosité, lié aux zones backrooms)
   e. TransitionMask (actif uniquement pendant une transition)
```

### Fichiers GLSL

**`portal_view.vsh / .fsh`** — Shader core pour le rendu de la texture portail sur la géométrie de la porte. Gère la perspective, le clipping et l'alignement UV.

**`distortion.fsh`** — Post-process de distorsion spatiale. Uniforms : `intensity`, `time`, `center` (position portail en screen space).

**`glitch.fsh`** — Effet de glitch digital. Uniforms : `time`, `intensity`, `blockSize`, `seed`.

**`flicker.fsh`** — Modulation de luminosité simulant des néons instables. Uniforms : `time`, `flickerRate`, `minBrightness`.

**`vignette.fsh`** — Assombrissement des bords. Uniforms : `intensity`, `radius`, `softness`.

**`transition_mask.fsh`** — Masque utilisé pendant le téléport pour cacher le changement. Uniforms : `progress` (0→1), `maskType` (fade/wipe/noise), `noiseScale`.

### Uniforms custom

| Uniform | Type | Source | Utilisé par |
|---|---|---|---|
| `time` | float | Tick counter | Tous les effets |
| `portalProximity` | float | Distance au portail le plus proche (0–1) | distortion, glitch |
| `playerSanity` | float | État du joueur (futur système) | vignette, flicker, glitch |
| `transitionProgress` | float | Avancement de la transition (0–1) | transition_mask |
| `inBackrooms` | bool | Le joueur est dans un niveau backrooms | flicker, vignette |
| `zoneAmbientColor` | vec3 | Couleur ambiante du niveau | vignette, flicker |

---

## 12. PortalManager — Orchestrateur global

### Rôle

Pièce centrale qui coordonne tous les sous-systèmes portail. Avant V3 cette orchestration était implicite — elle est maintenant explicite.

**Source of Truth pour** : quels portails sont actifs, quels portails ont une vue distante, quels portails sont prioritaires.

```java
/**
 * Central orchestrator for the portal system.
 * Runs every tick on the client side.
 *
 * Responsibilities:
 * 1. Maintain the list of all known portals
 * 2. Score and select which portals are active (budget-limited)
 * 3. Delegate to sub-systems (view, transition, audio, entity, aggro)
 * 4. Enforce budget limits
 * 5. Collect debug metrics
 *
 * This is the ONLY entry point into the portal system from the main game loop.
 * Sub-systems do not update themselves — PortalManager calls them.
 */
public class PortalManager {

    // --- Portal registry ---
    List<PortalLink> allPortals;

    // --- Active selection (budget-limited) ---
    List<PortalLink> activePortals;      // State >= PREWARMING
    List<PortalLink> renderedPortals;    // State >= VISIBLE, max = maxActivePortalViews

    // --- Main tick ---
    void tick(PlayerState player) {
        // 1. Score all portals
        // 2. Select top N for rendering (§13 priority)
        // 3. Update TransitionSystem (state machine)
        // 4. Update PortalViewSystem (render distant views)
        // 5. Update CrossSpaceAudioSystem
        // 6. Update EntityEchoSystem
        // 7. Update AggroSimulationSystem
        // 8. Collect metrics
    }

    // --- Query ---
    List<PortalLink> getRenderedPortals();
    PortalLink getNearestActivePortal(PlayerState player);
    PortalState getPortalState(String portalId);

    // --- Metrics ---
    DebugMetrics getMetrics();
}
```

### Flux de contrôle

```
Game Loop (tick client)
  │
  └→ PortalManager.tick(player)
       │
       ├→ score all portals (§13)
       ├→ select top N
       ├→ TransitionSystem.update(activePortals, player)
       ├→ PortalViewSystem.render(renderedPortals, player)
       ├→ CrossSpaceAudioSystem.update(renderedPortals, player)
       ├→ EntityEchoSystem.update(renderedPortals, player)
       ├→ AggroSimulationSystem.tick(activePortals, player)
       └→ DebugMetrics.collect(...)
```

---

## 13. Système de priorité portail

### Problème

Avec `maxActivePortalViews = 2`, il faut décider **quels portails** méritent un rendu distant. Sans système de sélection, on risque de rendre un portail hors écran ou d'ignorer un portail gameplay-critique.

### `PortalPriorityScore.java`

```java
/**
 * Scoring system to rank portals for active rendering.
 * PortalManager uses these scores to select which portals get a distant view.
 *
 * Score = weighted sum of factors. Higher = more priority.
 */
public class PortalPriorityScore {

    float distanceScore;     // Plus proche = plus prioritaire. 0 (loin) → 1 (collé)
    float screenCoverageScore; // Surface en screen-space. 0 (hors écran) → 1 (plein écran)
    float facingScore;       // Le joueur regarde-t-il vers le portail ? 0 (dos tourné) → 1 (face)
    float importanceScore;   // Gameplay : porte narrative = 1.0, décor = 0.3, invisible = 0.0
    float stateBonus;        // Portail ARMED/TRANSITIONING = bonus (ne pas dropper un portail en cours de transition)

    float computeTotal(BackroomsConfig config);

    /**
     * Portail hors écran = score 0 automatiquement.
     * Portail en TRANSITIONING = score max (jamais interrompu).
     */
}
```

### Règles de sélection

1. Scorer tous les portails
2. Éliminer ceux avec `screenCoverageScore == 0` (hors écran) sauf s'ils sont en ARMED ou TRANSITIONING
3. Trier par score total décroissant
4. Prendre les N premiers (`maxActivePortalViews`)
5. **Jamais dropper** un portail en `TRANSITIONING` — il a toujours la priorité absolue

---

## 14. Cas extrêmes de caméra

### Problème

Quand le joueur colle la caméra au portail, regarde en biais extrême, ou utilise un FOV élevé, le rendu distant peut produire des artefacts (distorsion, clipping, illusion cassée).

### `RemoteCameraGuard.java`

```java
/**
 * Protects the remote camera computation from edge cases.
 * Applied BEFORE the remote camera is used for rendering.
 *
 * Guards against:
 * - Player camera too close to portal plane (< portalViewMaxPlayerDistance)
 * - Viewing angle too oblique (< portalViewMinAngle degrees from portal plane)
 * - FOV extreme values (> 110°)
 */
public class RemoteCameraGuard {

    enum GuardResult {
        OK,              // Rendu distant normal
        CLAMP_APPLIED,   // Caméra corrigée (offset near plane, FOV clamp)
        FALLBACK         // Angle trop extrême → ne pas rendre, afficher fallback shader
    }

    /**
     * Évalue la position caméra et applique des corrections si nécessaire.
     *
     * Corrections possibles :
     * - Offset du near plane pour éviter le clipping dans le portail
     * - Clamp FOV de la caméra distante à max 100°
     * - Si angle < portalViewMinAngle : retourne FALLBACK
     *   → le portail affiche du bruit/flou au lieu du rendu distant
     */
    GuardResult evaluate(CameraState playerCamera, PortalLink link, BackroomsConfig config);
    CameraState applyCorrectedCamera(CameraState original, PortalLink link);
}
```

### Comportement par situation

| Cas | Correction |
|---|---|
| Joueur à distance normale, face au portail | Aucune — rendu distant normal |
| Joueur très proche (< 0.2 blocs du plan) | Near plane offset — la caméra distante recule légèrement |
| Angle très oblique (< 15°) | Fallback : surface portail affiche flou/bruit au lieu du rendu distant |
| FOV > 110° | FOV de la caméra distante clampé à 100° |
| Joueur derrière le portail (angle négatif) | Portail invisible (face culling normal) |

---

## 15. Synchronisation frame-exact : transition ↔ rendu

### Problème

Si le TP se produit au mauvais moment du frame, le joueur voit 1 frame de mismatch (position ancienne, monde nouveau) ce qui produit un jitter visible et casse l'illusion.

### Règle de synchronisation

```
Le TP doit se produire :
  → APRÈS le rendu du frame courant (le joueur voit encore l'ancien monde)
  → AVANT le prochain calcul de caméra (la prochaine frame utilise la nouvelle position)

Timeline d'un frame :

  ┌─────────────────────────────────────────────────────────────┐
  │ Frame N                                                     │
  │                                                             │
  │  [Game tick]  [Camera update]  [World render]  [Post-FX]   │
  │       │             │               │              │        │
  │       │             │               │              │        │
  │       │             │               │       TP ICI ←────── après post-FX
  │       │             │               │              │        │
  └─────────────────────────────────────────────────────────────┘
  ┌─────────────────────────────────────────────────────────────┐
  │ Frame N+1                                                   │
  │                                                             │
  │  [Game tick]  [Camera update]  [World render]  [Post-FX]   │
  │                     │                                       │
  │           caméra utilise la                                 │
  │           nouvelle position                                 │
  └─────────────────────────────────────────────────────────────┘
```

### `FrameSyncPolicy.java`

```java
/**
 * Controls the exact timing of the teleport within the frame lifecycle.
 *
 * The TP is scheduled during the TRANSITIONING state and executed
 * at a specific injection point in the render pipeline:
 *
 * - AFTER post-processing of the current frame
 * - BEFORE the next tick's camera update
 *
 * This ensures:
 * - The current frame shows the old world with transition mask active
 * - The next frame shows the new world with correct camera position
 * - Zero frames of mismatch
 *
 * Implementation: uses VeilPipelineBridge.injectAfterWorldRender()
 * to schedule the TP callback at the correct point.
 */
public class FrameSyncPolicy {
    void scheduleTeleport(TeleportExecutor executor, TransitionContext ctx);
    boolean isTeleportPending();
}
```

---

## 16. Règles de stabilité audio

### Problème

Sans règles de stabilité, les sources audio cross-space peuvent clignoter (apparaître/disparaître rapidement) quand une source est à la limite du budget ou de la distance. Le résultat : un son qui coupe et revient de façon irritante.

### `AudioSourceStabilizer.java`

```java
/**
 * Prevents audio source flickering through three mechanisms:
 *
 * 1. HYSTERESIS — Une source qui entre dans le budget doit être
 *    "hors budget" pendant au moins 3 ticks consécutifs avant d'être retirée.
 *    (Évite on/off rapide quand la source oscille autour de la limite)
 *
 * 2. MINIMUM LIFETIME — Une source nouvellement activée reste active
 *    pendant au moins audioSourceMinLifetimeMs (défaut: 200ms),
 *    même si elle sort du budget entre-temps.
 *    (Évite les sons ultra-courts qui font "pop")
 *
 * 3. VOLUME SMOOTHING — Le volume d'une source cross-space ne change jamais
 *    instantanément. Il suit un lerp avec facteur audioVolumeSmoothing.
 *    volume_actuel = lerp(volume_actuel, volume_cible, smoothing)
 *    (Évite les sauts de volume brusques)
 *
 * IMPORTANT : ces règles s'appliquent APRÈS la sélection par priorité.
 * La sélection détermine quelles sources DEVRAIENT être actives.
 * Le stabilizer détermine quand effectuer réellement le changement.
 */
public class AudioSourceStabilizer {
    boolean shouldActivate(AudioSourceId id, long currentTick);
    boolean shouldDeactivate(AudioSourceId id, long currentTick);
    float computeSmoothedVolume(AudioSourceId id, float targetVolume, float deltaTime);
}
```

### Comportement attendu

| Situation | Sans stabilizer | Avec stabilizer |
|---|---|---|
| Source oscille autour de la distance limite | Son coupe/revient chaque frame | Son reste stable (hysteresis) |
| Source entre dans le budget puis sort immédiatement | Pop audible de 1 frame | Son joué pendant au moins 200ms |
| Joueur se rapproche du portail (volume monte) | Saut de volume | Volume monte progressivement |
| Joueur se retourne (source sort du budget) | Coupure sèche | Volume descend → coupure douce |

---

## 17. Métriques debug et HUD runtime

### Objectif

Afficher en temps réel l'état du système pendant le développement. Activé via `debugShowMetricsHud = true`.

### `DebugMetrics.java`

```java
/**
 * Collects runtime metrics from all sub-systems.
 * Updated by PortalManager at each tick.
 * Read by DebugHudRenderer for display.
 */
public class DebugMetrics {
    // --- Portals ---
    int totalPortals;
    int activePortals;            // State >= PREWARMING
    int renderedPortals;          // State >= VISIBLE, vue distante active
    Map<String, PortalState> portalStates;

    // --- Render ---
    float portalRenderTimeMs;     // Temps GPU de rendu des vues distantes
    int currentResolutionScale;   // Résolution effective (peut être dégradée)
    int currentUpdateFrequency;

    // --- Audio ---
    int activeCrossSpaceAudioSources;
    int stabilizedSourceCount;    // Sources maintenues par hysteresis

    // --- Entities ---
    int activeProxyEntities;

    // --- Transition ---
    String currentTransitionPhase; // null si pas de transition en cours
    float transitionProgress;

    // --- Iris ---
    boolean irisDetected;
    boolean irisShadersActive;

    // --- Performance ---
    float currentFps;
    boolean degradationActive;
}
```

### `DebugHudRenderer.java`

Affiche les métriques dans un overlay F3-like. Informations affichées :

```
[Backrooms Debug]
Portals: 2/5 rendered | ARMED: door_17
Render: 2.3ms @ 50% | Update: 1/2 frames
Audio: 4/6 sources | Stabilized: 1
Entities: 3 proxies
Iris: detected, disabled (backrooms)
FPS: 62 | Degradation: OFF
```

---

## 18. PILIER 3 — Système de Portails Seamless

C'est le cœur du mod. Il est subdivisé en 6 sous-systèmes, coordonnés par `PortalManager` (§12).

---

### 18.1 PortalViewSystem — Rendu distant

**Rôle** : Rendre la vue d'une zone distante dans un framebuffer et l'afficher sur la surface d'un portail.

**Source of Truth pour** : contenu des framebuffers portail.

#### Fonctionnement

```
PortalManager sélectionne les portails à rendre (§13)
  → Pour chaque portail sélectionné :
    → RemoteCameraGuard vérifie les cas extrêmes (§14)
    → Si OK : RemoteCamera calcule la caméra distante
    → Rendu de la scène distante dans un framebuffer dédié
    → PortalSurfaceRenderer plaque la texture sur la géométrie
  → Si FALLBACK : surface affiche bruit/flou
```

#### Classes

**`RemoteCamera.java`**

```java
/**
 * Calcule la caméra distante à partir de :
 * - position/orientation du joueur
 * - position/orientation du portail source
 * - position/orientation du portail destination
 *
 * La caméra distante reproduit l'angle de vue relatif
 * du joueur par rapport au portail, transposé côté destination.
 */
public class RemoteCamera {
    CameraState computeRemoteCamera(PlayerState player, PortalLink link);
}
```

**`PortalFramebuffer.java`** — Wrapper autour du framebuffer Veil via `VeilFramebufferBridge`. Gère la création, le resize et le cleanup.

**`PortalSurfaceRenderer.java`** — Dessine la texture du framebuffer sur le quad de la porte dans le monde. Utilise le shader `portal_view`.

---

### 18.2 TransitionSystem — Téléportation invisible

**Rôle** : Détecter le franchissement, exécuter le téléport au bon moment, masquer le changement. C'est le propriétaire de l'état `PortalState` (Source of Truth).

#### Pipeline de transition (correspond aux états §6)

```
1. PREWARMING — Le joueur s'approche (< portalActivationDistance)
   → Préparation anticipée contrôlée des chunks destination
     (orchestration safe vis-à-vis du thread principal,
      via les mécanismes Fabric/MC de forceload ou chargement anticipé —
      PAS de chargement sur un thread libre arbitraire)
   → Rendu distant préparé

2. VISIBLE — Destination prête
   → Vue distante rendue dans le framebuffer
   → Audio cross-space activé

3. ARMED — Le joueur entre dans la zone seuil (< portalArmDistance)
   → TransitionMasker prépare l'effet visuel
   → TeleportExecutor prépare les coordonnées
   → FrameSyncPolicy prêt à scheduler le TP (§15)

4. TRANSITIONING — Le joueur dépasse teleportTriggerDepth
   → Effet de masquage déclenché
   → FrameSyncPolicy schedule le TP au bon moment du frame
   → Téléportation après post-FX du frame courant (§15)
   → Orientation, vélocité, position relative conservées
   → Acoustique ajustée

5. COOLDOWN — Le joueur est dans la nouvelle zone
   → Effet de masquage se dissipe
   → Vue distante côté source désactivée
   → Entités proxy remplacées par entités réelles
   → Retour à INACTIVE après cooldown
```

#### `ThresholdDetector.java`

```java
/**
 * Détecte la position du joueur par rapport à chaque portail actif.
 * Émet des événements de changement d'état portail.
 *
 * NE modifie PAS PortalState directement — signale au TransitionSystem.
 */
public class ThresholdDetector {
    ThresholdEvent evaluate(PlayerState player, PortalLink link);
}
```

#### `TeleportExecutor.java`

```java
/**
 * Exécute la téléportation.
 * Appelé par FrameSyncPolicy au moment exact du frame (§15).
 *
 * ATTENTION au thread safety : cette opération s'exécute sur le thread principal.
 */
public class TeleportExecutor {
    TeleportResult execute(PlayerState player, PortalLink link, TransitionContext ctx);
}
```

#### `TransitionMasker.java`

Types de masquage : **NOISE**, **FADE**, **DISTORT**, **NONE**.

---

### 18.3 SpatialIllusionSystem — Design spatial trompeur

**Rôle** : Gérer la topologie trompeuse des niveaux backrooms. C'est le cœur de l'expérience "backrooms" — plus important que le portail lui-même.

**Source of Truth pour** : `RoomGraph` (topologie), règles de redirection, variantes actives.

#### `RoomGraph.java`

```java
/**
 * Graphe orienté de segments de niveau.
 * Le joueur ne connaît jamais la topologie réelle.
 * Source of Truth pour la topologie réelle.
 */
public class RoomGraph {
    RoomSegment getSegment(String id);
    List<PortalLink> getExits(RoomSegment segment);
    PortalLink resolveExit(RoomSegment segment, ExitId exit, RedirectContext ctx);
}
```

#### `RedirectRule.java` et résolution de conflits

```java
public interface RedirectRule {
    boolean applies(RedirectContext ctx);
    String targetSegmentId();
    int priority();   // plus haut = plus prioritaire
    int insertOrder(); // ordre d'enregistrement, pour tie-breaking
}
```

#### `RedirectResolver.java`

```java
/**
 * Résout quelle RedirectRule s'applique quand plusieurs sont valides.
 *
 * Algorithme de résolution :
 * 1. Filtrer les règles dont applies() == true
 * 2. Trier par priority() décroissant
 * 3. Si plusieurs règles avec la même priorité :
 *    → TIE-BREAKER : la règle avec le plus petit insertOrder() gagne
 *    → C'est un "first registered wins" à priorité égale
 * 4. Si aucune règle valide → destination par défaut du graphe
 *
 * IMPORTANT : le résultat est DÉTERMINISTE.
 * Pas de random dans la résolution elle-même.
 * (RandomRule est un type de règle, pas du random dans le resolver)
 */
public class RedirectResolver {
    PortalLink resolve(RoomSegment segment, ExitId exit, RedirectContext ctx,
                       List<RedirectRule> rules);
}
```

#### Exemples de règles

| Règle | Description |
|---|---|
| `LoopRule` | La sortie ramène au même segment (couloir infini) |
| `DirectionRule` | La destination change selon le sens de traversée |
| `CountRule` | Au Nème passage, la destination change |
| `TimeRule` | Après N minutes, les sorties mutent |
| `StateRule` | Si le joueur a un certain item/état, la sortie change |
| `RandomRule` | Destination aléatoire parmi un pool (le random est dans applies(), pas dans le resolver) |

#### `RoomVariantRegistry.java`

Registre de variantes quasi-identiques. Différences possibles : luminosité, objet déplacé, son, fissure, porte en plus/moins.

#### `LoopingCorridorManager.java`

Couloirs infinis via portail invisible au milieu du segment.

---

### 18.4 EntityEchoSystem — Entités proxy

**Rôle** : Afficher des entités de la zone distante dans la vue du portail.

#### Règles strictes — ce qui est INTERDIT

| Interdit | Raison |
|---|---|
| Pas de simulation complète des entités en double | Deux sources d'état = désynchronisation garantie |
| Pas de "clone logique" de l'entité | L'écho n'a aucune logique de jeu, aucune IA, aucun tick |
| Pas de collision/interaction du proxy avec le monde du joueur | Le proxy est un artefact visuel pur |
| Pas de synchronisation d'état complexe | Seulement : position, rotation, animation courante, état visuel |

**Règle d'or** : une seule source d'autorité pour l'état réel = le monde dans lequel l'entité existe réellement. L'écho lit, il n'écrit jamais et ne calcule jamais.

#### `CrossSpaceEntitySync.java`

```java
/**
 * LECTURE SEULE depuis le monde destination.
 * Ne stocke aucun état propre — re-lit à chaque frame.
 *
 * NE synchronise PAS : HP, inventaire, target, pathfinding, AI state, NBT.
 */
public class CrossSpaceEntitySync {
    EchoState getEchoState(Entity entity);
}
```

#### Règles de cohérence

| Situation | Comportement |
|---|---|
| Joueur franchit le portail, entité était visible | L'entité DOIT être à la même position/animation après transition |
| Entité sort du champ de la vue distante | Disparaît naturellement (pas de pop) |
| Entité tuée côté destination | Écho retiré par fondu ou sortie de champ |
| Entité trop loin du portail côté destination | Pas d'écho affiché (budget : max `maxProxyEntitiesPerView`) |
| État désynchronisé détecté | **Fallback** : écho retiré silencieusement (§4 Règle 1) |

---

### 18.5 CrossSpaceAudioSystem — Audio cross-space

**Rôle** : Simuler que le son traverse les ouvertures entre zones, en utilisant OpenAL EFX.

**Source of Truth pour** : l'acoustique perçue par le joueur.

#### Pipeline audio

```
Zone destination → sources sonores sélectionnées (max: maxCrossSpaceAudioSources)
  → Sélection par priorité
  → AudioSourceStabilizer (hysteresis, lifetime, smoothing) (§16)
  → Position reprojetée relativement à l'ouverture du portail
  → Atténuation distance
  → Occlusion
  → Filtrage fréquentiel (low-pass si occlus)
  → Reverb de la zone source appliquée
  → Mix dans le bus audio du joueur
```

#### Sélection des sources (priorité)

1. Entités en état CHASE ou ALERT
2. Sources les plus proches du portail destination
3. Sons ambiants importants du niveau (néons, gouttes, machinerie)
4. Sons secondaires

#### `SpatialSoundProjector.java`

```java
/**
 * La source sonore distante est projetée comme si elle "venait de l'ouverture".
 * Distance perçue = distance source↔portail_dest + distance joueur↔portail_source
 * Direction = direction vers le portail source
 */
public class SpatialSoundProjector {
    ProjectedSound project(SoundSource distantSource, PortalLink link, PlayerState player);
}
```

#### `EFXFilterManager.java`

```java
/**
 * OpenAL EFX filters & effects.
 * Fallback : si EFX non disponible, atténuation volume simple.
 */
public class EFXFilterManager {
    int createLowPassFilter(float gain, float gainHF);
    int createReverbEffect(AcousticZoneProfile profile);
    void attachToSource(int sourceId, int filterId, int effectSlotId);
    void updateOcclusion(int sourceId, float occlusionFactor);
    boolean isEFXAvailable();
    void cleanup();
}
```

#### `AcousticZoneProfile.java`

| Niveau | Réverb | Caractère |
|---|---|---|
| Level 0 (bureaux) | Court, sec | Moquette, plafond bas, néons |
| Level 1 (parking) | Long, métallique | Béton, écho, gouttes d'eau |
| Level 2 (tuyaux) | Moyen, coloré | Résonance métallique, vapeur |
| Level 3 (obscurité) | Très long, diffus | Espace immense, indéfini |
| The Poolrooms | Moyen, aquatique | Eau, carrelage, écho clair |

#### Transition acoustique

1. Juste avant : profil zone source + sons filtrés destination
2. Pendant le passage : crossfade rapide (100–200ms)
3. Après : profil zone destination + sons filtrés source (s'estompent)

---

### 18.6 AggroSimulationSystem — Aggro cross-seuil

**Rôle** : Simuler la détection du joueur par les entités de l'autre côté d'un portail.

```java
/**
 * IMPORTANT : le stimulus est envoyé à l'ENTITÉ RÉELLE dans son monde,
 * PAS au proxy. Le proxy n'a aucune logique.
 */
public class AggroSimulationSystem {
    void tick(List<PortalLink> activeLinks, PlayerState player);
    void propagateStimulus(Entity target, StimulusType type, float intensity);
}
```

#### Types de stimulus

| Type | Déclencheur | Portée |
|---|---|---|
| `VISUAL` | Joueur visible dans l'ouverture | Taille portail × distance |
| `AUDIO` | Sprint, combat, casse bloc | Bruit + occlusion |
| `PROXIMITY` | Joueur très proche du seuil | Courte portée, toujours actif |

#### Poursuite cross-portail

1. Entité (réelle) se dirige vers le portail côté destination
2. Atteint le seuil → téléportée dans la zone du joueur
3. Devient entité réelle côté joueur → poursuite normale

---

## 19. Structures de données clés

### `PortalState` (enum)

```java
public enum PortalState {
    INACTIVE,
    PREWARMING,
    VISIBLE,
    ARMED,
    TRANSITIONING,
    COOLDOWN
}
```

### `PortalLink`

```java
public class PortalLink {
    String id;
    PortalEndpoint source;
    PortalEndpoint destination;
    boolean bidirectional;
    PortalState state;
    List<RedirectRule> rules;
    long lastStateChangeTimestamp;
    float gameplayImportance;      // Pour PortalPriorityScore.importanceScore
}
```

### `PortalEndpoint`

```java
public class PortalEndpoint {
    ResourceLocation dimension;
    BlockPos position;
    Direction facing;
    int width;
    int height;
    String roomSegmentId;
}
```

### `TransitionContext`

```java
public class TransitionContext {
    PortalLink link;
    Vec3 playerRelativePosition;
    float playerYaw;
    float playerPitch;
    Vec3 playerVelocity;
    long tickTimestamp;
    TransitionMaskType maskType;
}
```

---

## 20. Contrats systèmes

### PortalManager

| | |
|---|---|
| **Inputs** | Tous les PortalLink, PlayerState, BackroomsConfig |
| **Outputs** | Liste des portails actifs/rendus, métriques, appels aux sous-systèmes |
| **Source of Truth** | Sélection des portails actifs (scoring) |
| **Fréquence** | Chaque tick client |
| **Dépendances** | Tous les sous-systèmes portail (les appelle) |
| **Interdit** | Rendre directement, jouer du son directement — il délègue |

### PortalViewSystem

| | |
|---|---|
| **Inputs** | PortalLink sélectionnés par PortalManager, PlayerState |
| **Outputs** | Framebuffers remplis, textures portail prêtes |
| **Source of Truth** | Contenu des framebuffers |
| **Fréquence** | Chaque N frames (config `portalUpdateFrequency`) |
| **Dépendances** | VeilFramebufferBridge, VeilPipelineBridge, RemoteCameraGuard |
| **Interdit** | Modifier l'état des portails, toucher à l'audio, modifier les entités |

### TransitionSystem

| | |
|---|---|
| **Inputs** | ThresholdDetector events, PortalLink, PlayerState |
| **Outputs** | PortalState transitions, TeleportResult |
| **Source of Truth** | État du cycle de vie de chaque portail |
| **Fréquence** | Chaque tick (20 TPS) |
| **Dépendances** | ThresholdDetector, TeleportExecutor, TransitionMasker, FrameSyncPolicy |
| **Interdit** | Rendre quoi que ce soit, jouer du son, modifier RoomGraph |

### SpatialIllusionSystem

| | |
|---|---|
| **Inputs** | Événements de transition, position joueur, état de jeu |
| **Outputs** | Résolution de PortalLink via RedirectResolver |
| **Source of Truth** | RoomGraph, variantes actives |
| **Fréquence** | À chaque résolution de sortie (pas chaque tick) |
| **Dépendances** | RoomGraph, RoomVariantRegistry, RedirectResolver |
| **Interdit** | Rendre, jouer du son, téléporter directement |

### EntityEchoSystem

| | |
|---|---|
| **Inputs** | Entités du monde destination (lecture seule), PortalLink sélectionnés |
| **Outputs** | EchoState pour le rendu dans PortalViewSystem |
| **Source of Truth** | Aucune — lecture seule depuis le monde réel |
| **Fréquence** | Chaque frame de rendu portail |
| **Dépendances** | Monde destination (lecture), BackroomsConfig (budget) |
| **Interdit** | Modifier les entités réelles, stocker de l'état propre, simuler de l'IA |

### CrossSpaceAudioSystem

| | |
|---|---|
| **Inputs** | Sources sonores destination, PortalLink sélectionnés, PlayerState |
| **Outputs** | Sources OpenAL positionnées et filtrées |
| **Source of Truth** | Acoustique perçue par le joueur |
| **Fréquence** | Chaque tick audio (~20 TPS) |
| **Dépendances** | EFXFilterManager, SpatialSoundProjector, AudioSourceStabilizer, AcousticZoneProfile |
| **Interdit** | Modifier les sons du monde destination, rendre quoi que ce soit |

### AggroSimulationSystem

| | |
|---|---|
| **Inputs** | PlayerState, PortalLink actifs, entités proches côté destination |
| **Outputs** | Stimulus envoyés aux entités RÉELLES |
| **Source of Truth** | Aucune — transmet des stimuli |
| **Fréquence** | Chaque tick (20 TPS) |
| **Dépendances** | Monde destination (lecture + envoi stimulus) |
| **Interdit** | Modifier l'état des portails, contrôler directement les entités |

---

## 21. Ordre de développement — MVP d'abord

### Définition du MVP

Le MVP contient **uniquement** :

- 1 type de portail (porte rectangulaire)
- 1 transition seamless fonctionnelle
- 1 couloir bouclé
- 1 profil acoustique (Level 0)
- 0 entité complexe (écho visuel simple au plus)
- Iris toggle basique (si Iris présent)

**Le MVP doit être jouable et convaincant avant de passer à la suite.**

### Phase 1 — Fondations

| # | Tâche | Pilier |
|---|---|---|
| 1.1 | Setup projet Fabric 1.21.1 + dépendance Veil | Config |
| 1.2 | `BackroomsConfig` avec chargement fichier config | Config |
| 1.3 | `VeilFramebufferBridge` + `VeilPipelineBridge` + `VeilShaderBridge` | Config |
| 1.4 | `IrisBridge` — détection + toggle basique | Config |
| 1.5 | Shader pipeline basique — charger et appliquer un shader simple | Shader |
| 1.6 | Premier effet post-process fonctionnel (vignette) | Shader |

### Phase 2 — Rendu de portail basique (MVP core)

| # | Tâche | Pilier |
|---|---|---|
| 2.1 | `PortalFramebuffer` — rendu offscreen via VeilFramebufferBridge | Portail |
| 2.2 | `RemoteCamera` + `RemoteCameraGuard` | Portail |
| 2.3 | `PortalSurfaceRenderer` — affichage texture sur un quad | Portail |
| 2.4 | Shader `portal_view` | Shader |
| 2.5 | `PortalManager` basique (1 seul portail, pas de scoring) | Portail |
| 2.6 | **MILESTONE** : un bloc "portail" qui montre un autre endroit du monde | — |

### Phase 3 — Transition seamless (MVP core)

| # | Tâche | Pilier |
|---|---|---|
| 3.1 | `PortalState` enum + machine d'état | Portail |
| 3.2 | `ThresholdDetector` — détection de franchissement | Portail |
| 3.3 | `TeleportExecutor` + `FrameSyncPolicy` | Portail |
| 3.4 | `TransitionMasker` — effet shader de masquage | Shader + Portail |
| 3.5 | Intégration transition complète INACTIVE→COOLDOWN | Portail |
| 3.6 | **MILESTONE** : traverser un portail sans coupure visible | — |

### Phase 4 — Couloir bouclé (MVP core)

| # | Tâche | Pilier |
|---|---|---|
| 4.1 | `RoomGraph` + `RoomSegment` | Portail |
| 4.2 | `LoopingCorridorManager` — couloir infini | Portail |
| 4.3 | **MILESTONE MVP** : un couloir qui boucle de façon invisible | — |

**→ À ce stade le MVP est jouable. Valider avant de continuer.**

### Phase 5 — Audio cross-space

| # | Tâche | Pilier |
|---|---|---|
| 5.1 | `EFXFilterManager` — wrapper OpenAL EFX | Portail |
| 5.2 | `AcousticZoneProfile` — profil Level 0 | Portail |
| 5.3 | `SpatialSoundProjector` | Portail |
| 5.4 | `OcclusionCalculator` | Portail |
| 5.5 | `AudioSourceStabilizer` — hysteresis, lifetime, smoothing | Portail |
| 5.6 | Transition acoustique au franchissement | Portail |
| 5.7 | **MILESTONE** : entendre des sons filtrés venant de l'autre côté | — |

### Phase 6 — Entités et aggro

| # | Tâche | Pilier |
|---|---|---|
| 6.1 | `CrossSpaceEntitySync` | Portail |
| 6.2 | `ProxyEntityRenderer` | Portail |
| 6.3 | `AggroSimulationSystem` | Portail |
| 6.4 | Logique de poursuite cross-portail | Portail |
| 6.5 | **MILESTONE** : une entité visible à travers le portail qui te traque | — |

### Phase 7 — Multi-portail et scoring

| # | Tâche | Pilier |
|---|---|---|
| 7.1 | `PortalPriorityScore` | Portail |
| 7.2 | `PortalManager` complet (scoring, sélection, budget) | Portail |
| 7.3 | `DebugMetrics` + `DebugHudRenderer` | Debug |
| 7.4 | **MILESTONE** : plusieurs portails, budget respecté, debug HUD | — |

### Phase 8 — Illusion spatiale avancée

| # | Tâche | Pilier |
|---|---|---|
| 8.1 | `RedirectRule` + `RedirectResolver` | Portail |
| 8.2 | `RoomVariantRegistry` | Portail |
| 8.3 | `SpatialIllusionSystem` intégration | Portail |
| 8.4 | **MILESTONE** : portes qui ne mènent pas toujours au même endroit | — |

### Phase 9 — Effets visuels avancés

| # | Tâche | Pilier |
|---|---|---|
| 9.1 | Distorsion proximité portail | Shader |
| 9.2 | Glitch (aléatoire + événementiel) | Shader |
| 9.3 | Flicker (néons) | Shader |
| 9.4 | Intégration shader custom dérivé open source | Shader |
| 9.5 | **MILESTONE** : ambiance visuelle backrooms complète | — |

### Phase 10 — Niveaux et contenu

| # | Tâche | Pilier |
|---|---|---|
| 10.1 | Système de définition de niveaux (JSON) | Level |
| 10.2 | Level 0 — bureaux jaunes | Level |
| 10.3 | Entités spécifiques | Level |
| 10.4 | Niveaux supplémentaires | Level |

---

## 22. Scénarios de test

### Tests de transition

| Scénario | Résultat attendu |
|---|---|
| Le joueur traverse une porte 20 fois de suite | Transition fluide à chaque fois, 0 frame de mismatch |
| Le joueur sprinte à travers le portail | Vélocité conservée, pas de saccade |
| Le joueur recule au dernier moment (ARMED → VISIBLE) | Pas de TP, pas d'effet résiduel |
| Le joueur traverse puis fait immédiatement demi-tour | Cooldown respecté, pas de double TP |

### Tests de performance

| Scénario | Résultat attendu |
|---|---|
| Deux portails visibles simultanément | FPS stable, budget respecté |
| FPS chute sous 30 | Dégradation automatique |
| Joueur très loin de tout portail | Aucun coût de rendu portail |
| 5 portails dans le même couloir | Seulement les 2 meilleurs scorés sont rendus |

### Tests de cohérence

| Scénario | Résultat attendu |
|---|---|
| Entité visible dans la vue distante, joueur traverse | Entité à la même position/animation après |
| Son audible de l'autre côté, joueur traverse | Transition acoustique progressive |
| Couloir bouclé, joueur marche 2 minutes | Illusion maintenue |
| Source audio oscille autour de la distance limite | Pas de flickering (hysteresis) |

### Tests de fallback

| Scénario | Résultat attendu |
|---|---|
| Shader compilation échoue | Portails fonctionnent sans effets |
| Destination pas chargée | Portail reste en PREWARMING |
| EFX non disponible | Audio en mode atténuation simple |
| Iris API incompatible | Warning en log, pas de crash |
| Caméra collée au portail en biais extrême | Fallback shader (flou/bruit), pas de rendu cassé |

### Tests Iris

| Scénario | Résultat attendu |
|---|---|
| Joueur avec Iris entre dans les Backrooms | Iris désactivé, Veil actif, fondu de transition |
| Joueur sort des Backrooms | Iris restauré, config d'avant retrouvée |
| Joueur crash dans les Backrooms, relance | Iris config restaurée au prochain lancement |
| Iris absent | Aucune erreur, IrisBridge inactif |

---

## 23. Points de vigilance critiques

### Timing du téléport

Le moment du TP doit être **imperceptible** et **frame-exact** (§15). C'est le point de rupture numéro 1.

### Alignement spatial

Après TP : même hauteur relative, même orientation, même vélocité, même échelle perçue. Toute erreur casse l'immersion.

### Cohérence vue distante ↔ réalité

Ce que le joueur voit dans le portail **doit correspondre** à ce qu'il trouvera après passage (§4 Règle 2).

### Préchargement anticipé

Orchestration safe vis-à-vis du thread principal. Mécanismes Fabric/MC de forceload. Jamais de thread libre.

### Compatibilité mods

Surveiller les conflits avec : Iris (géré via IrisBridge), autres mods de rendu/shaders, mods de caméra, mods audio (Sound Physics), mods de dimensions custom.

### Sélection de portails

Toujours scorer et sélectionner via `PortalPriorityScore` (§13). Jamais rendre un portail hors écran. Jamais dropper un portail en TRANSITIONING.

---

## 24. Dépendances Gradle

```groovy
dependencies {
    minecraft "com.mojang:minecraft:1.21.1"
    mappings "net.fabricmc:yarn:1.21.1+build.X:v2"
    modImplementation "net.fabricmc:fabric-loader:0.X.X"
    modImplementation "net.fabricmc.fabric-api:fabric-api:X.X.X+1.21.1"

    // Veil — vérifier la version compatible 1.21.1
    modImplementation "foundry.veil:veil-fabric-1.21.1:X.X.X"

    // Iris — soft dependency (compile only, runtime optional)
    modCompileOnly "net.irisshaders:iris:X.X.X"

    // LWJGL (fourni par Minecraft, pour OpenAL EFX)
    // org.lwjgl.openal.EXTEfx — pas de dépendance supplémentaire
}
```

---

## 25. Conventions de code

- **Package racine** : `com.backrooms`
- **Nommage** : PascalCase classes, camelCase méthodes/champs, SCREAMING_SNAKE constantes
- **Javadoc** : obligatoire sur toutes les classes et méthodes publiques
- **Logs** : SLF4J via `LoggerFactory.getLogger(ClassName.class)`
- **Config** : tout paramètre numérique dans `BackroomsConfig`, jamais en dur
- **Veil** : tout appel API Veil via les classes `Veil*Bridge` uniquement
- **Iris** : tout appel Iris via `IrisBridge` uniquement, jamais d'import direct ailleurs
- **Source of Truth** : chaque donnée a un seul propriétaire (§5), les autres lisent via interface
- **Fallback** : chaque sous-système fonctionne en mode dégradé si une dépendance échoue
- **Perception** : toute décision visuelle/audio respecte les 4 Règles de Perception (§4)

---

## 26. Résumé exécutif

> **Ce mod = un système de portes et transitions illusionnistes, avec rendu distant via Veil, TP masqué frame-exact, shader custom dérivé open source, Iris désactivé dans les dimensions backrooms, entités visibles simulées, aggro scriptée, et son cross-space stabilisé via OpenAL EFX, le tout orchestré par un PortalManager central au service d'un level design backrooms trompeur et cohérent.**

Les trois piliers (Configuration/Veil/Iris, Shader, Portail) sont strictement séparés. La machine d'état des portails, les sources of truth, le budget performance, le scoring de priorité, la synchronisation frame-exact, la stabilité audio et les stratégies de fallback garantissent un système robuste et maintenable.

**Le plus gros danger n'est pas l'idée — c'est de vouloir tout développer trop vite. Verrouiller le MVP (phases 1–4) avant d'aller plus loin.**
