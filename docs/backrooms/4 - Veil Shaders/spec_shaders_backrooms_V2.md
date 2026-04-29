# Backrooms Mod — Spec Technique Shaders V2
## Architecture native Veil 3.x · 3 profils qualité · Effets cross-level et par-level

> **Note V2** : refonte complète de la V1. La V1 dupliquait inutilement des features Veil natives (système de pipeline, framebuffer pool, includes GLSL) sous forme de couche Java custom. La V2 repose **entièrement** sur les primitives Veil et limite le code Java à ce que Veil ne fait pas.
>
> **Cible** : Minecraft 1.21.1 / Fabric / Java 21 / **Veil 3.x compatible Minecraft 1.21.1 — version exacte verrouillée Phase 0**.
>
> **Patch implicite à V4.3** : la V4.3 cible Veil 3.x compatible Minecraft 1.21.1. Cette V2 précise que la version exacte doit être verrouillée en Phase 0. Voir §29 pour le diff suggéré.
>
> **Compagnons** : `spec_veil_portail_shaders_V4_3.md` (Pilier 2 §1) et `arborescence_v4.md` (§3 — `backrooms/shader/`, `backrooms/bridge/`).
>
> **Scope** : rendu shader **dimensions backrooms uniquement**. Le rendu vanilla n'est pas affecté.
>
> **Non-scope** : portails (V4.3 §18), audio EFX (V4.3 §17), génération de monde, particules (Quasar Veil — V2 future).

---

## Table des matières

**Partie A — Vision**
- [§1. Vue d'ensemble](#1-vue-densemble)
- [§2. Lois de Cohérence Visuelle](#2-lois-de-cohérence-visuelle)
- [§3. Limitations structurelles](#3-limitations-structurelles)

**Partie B — Capacités Veil exploitées**
- [§4. Position dans l'arborescence v4](#4-position-dans-larborescence-v4)
- [§5. Stack technique](#5-stack-technique)
- [§6. Capacités Veil utilisées — inventaire et usage](#6-capacités-veil-utilisées--inventaire-et-usage)
- [§7. Sources of Truth](#7-sources-of-truth)

**Partie C — Architecture qualité / level**
- [§8. Modèle de qualité — les 3 profils](#8-modèle-de-qualité--les-3-profils)
- [§9. Système d'overrides par level](#9-système-doverrides-par-level)
- [§10. Hiérarchie de décision runtime](#10-hiérarchie-de-décision-runtime)
- [§11. Pipeline = liste ordonnée de stages Veil](#11-pipeline--liste-ordonnée-de-stages-veil)
- [§12. Système d'uniforms](#12-système-duniforms)

**Partie D — Effets**
- [§13. Conventions GLSL & Veil shader](#13-conventions-glsl--veil-shader)
- [§14. Effets baseline cross-level](#14-effets-baseline-cross-level)
- [§15. Effets optionnels (par profil ou par level)](#15-effets-optionnels-par-profil-ou-par-level)

**Partie E — Robustesse**
- [§16. Hot reload encadré](#16-hot-reload-encadré)
- [§17. Auto-dégradation persistante](#17-auto-dégradation-persistante)
- [§18. Performance et budgets](#18-performance-et-budgets)
- [§19. Tests visuels (régression) avec mode déterministe](#19-tests-visuels-régression-avec-mode-déterministe)
- [§20. Compatibilité](#20-compatibilité)

**Partie F — Interdits**
- [§21. Anti-patterns Veil](#21-anti-patterns-veil)
- [§22. Anti-patterns Java](#22-anti-patterns-java)
- [§23. Anti-patterns GLSL](#23-anti-patterns-glsl)

**Partie G — Plan de dev**
- [§24. Roadmap shaders par phase](#24-roadmap-shaders-par-phase)
- [§25. Critères de validation](#25-critères-de-validation)
- [§26. Préambule développeur (humain ou LLM)](#26-préambule-développeur-humain-ou-llm)

**Annexes**
- [§27. Glossaire](#27-glossaire)
- [§28. Choix tranchés et alternatives écartées](#28-choix-tranchés-et-alternatives-écartées)
- [§29. Notes d'alignement déjà intégrées à la spec V4.3](#29-notes-dalignement-déjà-intégrées-à-la-spec-v43)

---

# PARTIE A — Vision

## §1. Vue d'ensemble

Le pipeline shader des Backrooms est **un consommateur de Veil**, pas un système parallèle à Veil. Il configure Veil via JSON, pousse quelques uniforms dynamiques depuis Java, et laisse Veil gérer la compilation, l'exécution, le hot-reload, les framebuffers et la composition.

### Philosophie

Trois principes structurent toute décision :

1. **Native Veil first.** Toute fonctionnalité que Veil offre est utilisée telle quelle. Pas de wrapper, pas de couche d'abstraction par-dessus, pas de pool custom de framebuffers, pas de pass registry maison. Si Veil le fait, on l'utilise.
2. **L'identité visuelle est non-négociable.** Le mood d'un level (Level 0 jaune-fluo, Level 1 béton-froid…) est imposé par le pipeline. Un joueur en Minimal voit un level moins riche, mais reconnaît immédiatement le level.
3. **Dégradation silencieuse.** Cohérent avec V4.3 §4 Loi 5. Un effet qui ne peut pas charger est skippé sans message d'erreur en jeu. Veil expose `required_features` dans son shader JSON pour ne même pas tenter le compile si le GPU ne supporte pas.

### Trois axes de variabilité

| Axe | Variable | Exemple |
|---|---|---|
| **Qualité** | Profil utilisateur (Ultra / Low / Minimal) | Joueur GPU faible passe en Minimal |
| **Identité** | Level actuel | Level 0 = grading jaune saturé / Level 1 = grading bleu désaturé |
| **État** | Variables runtime (sanity, proximité entité, transition portail) | Sanity bas → vision périphérique floue |

Ces axes se composent au runtime selon une hiérarchie stricte (§10) en **un seul pipeline Veil sélectionné par frame**.

### Ce que le pipeline produit

- Grading colorimétrique imposé par level (LUT 3D)
- Tone mapping ACES en HDR linéaire **avant** le grading LDR
- Vignette adaptative
- Grain stable temporellement
- Flicker de luminance dynamique
- Halos émissifs via Dynamic Buffer Veil (Mode A) ou luminance threshold (Mode B fallback)
- Distorsions screen-space localisées (couloirs tordus, transitions)
- Overlays d'hallucination triggered (sanity, proximité)
- Cohérence visuelle entre les 3 profils

### Ce que le pipeline NE produit PAS (V1)

- Pas de colored block lighting natif (sous-projet hors scope V1)
- Pas de vertex displacement world geometry (conflit Sodium, désync hitbox)
- Pas de raytracing/raymarching plein écran
- Pas de compute shaders (Veil les supporte, réservé V2)
- Pas de Quasar particles GPU (réservé V2 pour effets d'hallucination volumétriques)
- Pas de Necromancer (rigging d'entités) — non pertinent
- Pas de motion blur (décision artistique)
- Pas de DOF dynamique permanent

---

## §2. Lois de Cohérence Visuelle

Cinq lois non-négociables. Tranchent en cas de conflit perf/visuel.

### Loi 1 — Identité par level avant qualité

Un level doit être visuellement reconnaissable même en Minimal. Le grading colorimétrique d'un level est **baseline obligatoire** dans les 3 profils. Les effets accessoires sont optionnels.

### Loi 2 — Continuité du mood global entre profils (reformulée)

Passer d'Ultra à Minimal ne change jamais le **mood global** d'un level. Diffèrent : résolution interne, présence d'effets accessoires, précision du noise.

**Exception explicite** : les effets **zonaux non-identitaires** (zone "couloir tordu" du Level 0, par exemple) peuvent être absents en Minimal **si** :
- Ils ne bloquent pas la progression (aucun objet ramassable, aucune interaction critique)
- Ils ne définissent pas l'identité globale du level (ils sont locaux à une zone)

`ScreenSpaceWarp` rentre dans cette exception (zonal, non-identitaire).

### Loi 3 — Dégradation silencieuse

Si un shader échoue à compiler (Veil refuse à cause de `required_features` non disponibles, ou erreur GLSL) ou échoue runtime, le pipeline saute la stage et continue. Aucun fallback visuel grossier. Veil le fait nativement via le système de `required_features` et son refus de loader.

### Loi 4 — Stabilité temporelle

Les effets de bruit, flicker et grain sont temporellement stables. Conventions :
- Composante temporelle quantifiée pour les noises (typiquement `floor(VeilRenderTime * N)`)
- Flickers cap fréquence à 8 Hz (sauf burst court documenté)
- Grain seedé par `(uv, frameTime % CYCLE)` pour cycler proprement

### Loi 5 — Frame-budget non-négociable

Chaque effet a un budget GPU cible (ms, GPU médian de référence — voir §18). Dépassement → dégradation **persistante** (pas d'oscillation runtime — voir §17).

---

## §3. Limitations structurelles

Limitations par conception. Cohérent avec V4.3 §2.

| Limitation | Raison |
|---|---|
| Pas de colored block lighting natif | Sous-projet hors scope V1 |
| Pas de vertex displacement world geometry | Conflit Sodium, désync hitbox |
| Pas de raytracing | Coût trop élevé même en Ultra |
| Pas de compute shaders V1 | Capacité Veil exploitée en V2 (génération de LUT 3D, blur Kawase grand format) |
| Pas de Quasar V1 | Particules GPU réservées V2 |
| Pas de skybox custom | Backrooms = indoor |
| Pas de motion blur | Décision artistique |
| Pas de SSR | Pas de surfaces réfléchissantes pertinentes en V1 |

---

# PARTIE B — Capacités Veil exploitées

## §4. Position dans l'arborescence v4

Le sous-dossier `backrooms/shader/` de l'arbo v4 §3 est **drastiquement allégé** par rapport à V1. Toute la couche `pipeline/` de la V1 disparaît : Veil en fait nativement le travail.

### Arbo cible (extension du Pilier 2)

```
backrooms/bridge/                                  # Pilier 1 — isolation Veil/Iris stricte
│
├── Bridge.java                                    # interface commune
├── BridgeState.java                               # enum OK / DEGRADED / FAILED
├── BridgeHealthMonitor.java                       # @SourceOfTruth santé bridges
├── VeilPostProcessingBridge.java                  # point unique vers pipelines/post-processing Veil
├── VeilUniformBridge.java                         # point unique pour push/read uniforms Veil
├── VeilDefinitionsBridge.java                     # point unique pour definitions/#define Veil
├── VeilFramebufferBridge.java                     # point unique vers framebuffers Veil
├── VeilShaderBridge.java                          # point unique vers programmes shaders Veil
├── IrisBridge.java                                # soft-dependency Iris
└── IrisStateStore.java                            # persistance/restauration état Iris

backrooms/shader/                                  # Pilier 2 — couche Java MINCE, sans import Veil direct
│
├── ShaderManager.java                            # implements Lifecycle (déjà arbo v4)
│                                                  # rôle : sélectionner pipeline via bridges + push uniforms via bridge
├── ShaderEventAdapter.java                       # NOUVEAU — handler shader qui appelle les bridges, jamais Veil directement
│
├── uniform/                                       # Java pousse uniforms à Veil
│   ├── UniformContext.java                       # déjà arbo v4 — contexte par-frame
│   ├── UniformProvider.java                      # déjà arbo v4 — interface
│   ├── UniformFallbacks.java                     # NOUVEAU — valeurs de secours obligatoires §12
│   └── provider/                                  # déjà arbo v4
│       ├── SanityUniformProvider.java
│       ├── PortalProximityProvider.java          # (collabo Pilier 3)
│       ├── TransitionProgressProvider.java
│       ├── ZoneAmbientProvider.java
│       └── PlayerStateProvider.java
│
├── definitions/                                   # NOUVEAU — pilote definitions via VeilDefinitionsBridge
│   ├── BackroomsShaderDefinitions.java           # @SourceOfTruth #define globaux conceptuels
│   └── DefinitionMapper.java                     # mappe profil/level → definitions via bridge
│
├── profile/                                       # sélection profil utilisateur
│   ├── QualityProfile.java                       # enum ULTRA / LOW / MINIMAL
│   ├── QualityProfileSelector.java               # @SourceOfTruth profil actif
│   ├── ProfileDefinition.java                    # record JSON-loadable (référence pipeline Veil)
│   ├── ProfileRegistry.java                      # charge profiles/*.json
│   └── AutoQualityDetector.java                  # heuristique GPU 1er lancement
│
├── effect/                                        # CATALOGUE d'effets — métadonnées seulement
│   ├── ShaderEffect.java                         # interface — wrapper d'un stage Veil
│   ├── EffectDescriptor.java                     # record (id, schema params, budget)
│   ├── EffectRegistry.java                       # @SourceOfTruth catalogue
│   ├── EffectCondition.java                      # AlwaysActive / StateDriven / Zonal
│   ├── EffectParamSchema.java                    # NOUVEAU — schema strict params §15
│   └── builtin/                                  # 1 classe par effet — porte le schema
│       ├── ColorGradingEffect.java
│       ├── VignetteEffect.java
│       ├── FilmGrainEffect.java
│       ├── ChromaticAberrationEffect.java
│       ├── ToneMappingEffect.java
│       ├── FlickerLightingEffect.java
│       ├── EmissiveHaloEffect.java
│       ├── ScreenSpaceWarpEffect.java
│       ├── BloomEffect.java
│       ├── HallucinationOverlayEffect.java
│       └── PeripheralBlurEffect.java
│
├── level/                                         # composition profil × level × état
│   ├── LevelShaderProfile.java                   # def JSON par level
│   ├── LevelShaderRegistry.java                  # @SourceOfTruth
│   └── LevelShaderResolver.java                  # → quel pipeline Veil activer
│
└── health/                                        # NOUVEAU — auto-dégradation persistante §17
    ├── PipelineHealthMonitor.java                # @SourceOfTruth état effets
    └── DegradationPolicy.java                    # règles de désactivation
```

`backrooms/shader/` ne dépend jamais directement de Veil. Il appelle uniquement les bridges `backrooms/bridge/`.

### Ressources Veil natives

```
src/main/resources/assets/petasse_gang_additions/
│
└── pinwheel/                                      # ← Veil convention
    │
    ├── post/                                       # Pipelines de post-processing (JSON)
    │   ├── backrooms_ultra_level0.json
    │   ├── backrooms_low_level0.json
    │   ├── backrooms_minimal_level0.json
    │   ├── backrooms_ultra_level1.json
    │   ├── backrooms_low_level1.json
    │   └── backrooms_minimal_level1.json
    │   #  ↑ une combinaison profil × level = 1 fichier JSON Veil (cf. §11)
    │
    ├── framebuffers/                               # Framebuffers globaux JSON
    │   ├── backrooms_main.json                    # FB principal MRT
    │   ├── backrooms_emissive.json                # FB pour halos
    │   ├── backrooms_warp.json                    # FB pour distorsions
    │   └── backrooms_bloom_chain.json             # FB descendant pour bloom
    │
    ├── shaders/                                    # Shaders Veil
    │   ├── program/                                # JSON décrivant chaque shader
    │   │   ├── color_grading.json
    │   │   ├── vignette.json
    │   │   ├── film_grain.json
    │   │   ├── chromatic_aberration.json
    │   │   ├── tonemap_aces.json
    │   │   ├── flicker_lighting.json
    │   │   ├── emissive_halo_extract.json
    │   │   ├── emissive_halo_blur.json
    │   │   ├── emissive_halo_composite.json
    │   │   ├── screenspace_warp.json
    │   │   ├── bloom_extract.json
    │   │   ├── bloom_blur.json
    │   │   ├── bloom_composite.json
    │   │   ├── hallucination_overlay.json
    │   │   └── peripheral_blur.json
    │   │
    │   ├── (program GLSL .vsh / .fsh / .comp)      # même répertoire program/ — un par stage
    │   │
    │   └── include/                                # GLSL réutilisables (#include domain:id)
    │       ├── noise.glsl
    │       ├── color.glsl
    │       ├── sampling.glsl
    │       └── time.glsl
    │
    └── shader_modifiers/                           # RÉSERVÉ V2 — modif shaders vanilla
        └── (vide en V1)
```

### Configuration

```
src/main/resources/data/petasse_gang_additions/
│
├── shader_profiles/                                # Mappe profil utilisateur → pipelines Veil
│   ├── ultra.json                                  # Voir §8
│   ├── low.json
│   ├── minimal.json
│   └── _schema.json                                # JSON Schema validation
│
├── level_shaders/                                  # Override par level
│   ├── level0.json                                 # Voir §9
│   ├── level1.json
│   └── _schema.json
│
└── shader_effects/                                 # Schema strict des params par effet §15
    ├── color_grading.schema.json
    ├── vignette.schema.json
    ├── film_grain.schema.json
    ├── ...
    └── _master.schema.json                         # validation au boot
```

---

## §5. Stack technique

| Composant | Choix verrouillé | Justification |
|---|---|---|
| Minecraft | 1.21.1 (V4.3) | Imposé |
| Mod loader | Fabric | Imposé |
| Java | 21 (V4.3) | Imposé |
| **Veil** | **3.x compatible Minecraft 1.21.1 — version exacte verrouillée Phase 0** | Dynamic Buffers, stencil first-person, compat Sodium et includes GLSL à confirmer dans `phase0_findings.md` |
| GLSL cible | `#version 330 core` | Veil supporte 330+, OpenGL 3.3 minimum MC 1.17+ |
| Format LUT | PNG strip 32×32×32 (1024×32) | Standard, lisible Veil via `textures.location`, éditable Photoshop/DaVinci |
| Format profile/level | JSON validé schema (Networknt JSON Schema 2020-12) | Hot-reload Veil natif, audit visuel, pas de DSL maintenance |
| Tone mapping | ACES Approximation (Narkowicz) | Standard, GPU-cheap, look filmique reconnaissable |
| Color space | Linéaire interne, sRGB output | Évite banding sur le grading |
| Précision | `highp float` fragment, `mediump` varyings | Évite banding LUT |
| Validation GLSL CI | `glslangValidator` | Veil ne valide qu'au runtime — on attrape au build |

### Outils dev

| Outil | Rôle |
|---|---|
| `glslangValidator` | Validation GLSL au build (Gradle task) |
| Networknt JSON Schema | Validation profiles/levels/effets au boot et au build |
| JUnit 5 + AssertJ | Tests Java |
| **RenderDoc** | Debug GPU manuel — Veil étant openGL standard, RenderDoc capture sans config spéciale |
| Veil Discord/wiki | Référence vivante de l'API |

---

## §6. Capacités Veil utilisées — inventaire et usage

Cette section liste **chaque capacité Veil exploitée**, avec son usage exact dans le projet. Aucune n'est utilisée à moitié.

### 6.1 Post-Processing Pipeline JSON (`pinwheel/post/`)

**Capacité Veil** : pipelines de post-processing déclarés en JSON, composés de `stages` (liste ordonnée), avec `priority`, `replace`, `dynamicBuffers` et `framebuffers` temporaires/globaux.

**Usage projet** :
- 1 fichier JSON par combinaison profil × level (ex: `backrooms_ultra_level0.json`)
- `priority` permet à un override level de se composer avec un pipeline générique
- `replace: true` pour bypass complet en cas de besoin (`backrooms/bridge/` debug)
- `dynamicBuffers` activé selon profil (Ultra : `albedo` + `light_color` ; Minimal : aucun)
- Stages utilisés : `veil:blit`, `veil:copy`, `veil:mask`, `veil:depth_function`

**Pas réinventé côté Java** : les classes V1 `ShaderPipeline`, `PassExecutionGraph`, `PassRegistry`, `PassthroughPass`, `FramebufferPool` n'existent plus.

### 6.2 Framebuffers JSON (`pinwheel/framebuffers/`)

**Capacité Veil** : framebuffers globaux ou temporaires, format MRT (multi-render-target), formats GL custom (RGBA8, RGB16F, R16F, etc.), depth optionnel, autoClear, **expressions MoLang** pour width/height.

**Usage projet** :
- `backrooms_main.json` : MRT avec `AlbedoSampler` (RGBA8), `EmissiveSampler` (RGBA8), depth — sert pour les passes principales
- `backrooms_emissive.json` : single attachment R11F_G11F_B10F pour halos avec MoLang `q.screen_width / 2` et `q.screen_height / 2` (downsample 50%)
- `backrooms_warp.json` : single RGBA8 pleine résolution
- `backrooms_bloom_chain.json` : 5 mip-levels descendants 50% → 25% → 12.5% → 6.25% → 3.125% via MoLang

**Pas réinventé côté Java** : pas de `FramebufferPool` ni de gestion de cycle de vie. Veil gère.

### 6.3 Dynamic Buffers (`albedo`, `normal`, `light_uv`, `light_color`)

**Capacité Veil** : extension G-buffer-like activable par pipeline. Les shaders vanilla annotés `// #veil:BUFFER` écrivent automatiquement dans le buffer concerné.

**Usage projet — clé pour `EmissiveHaloEffect`** :
- En Ultra : `albedo` + `light_color` activés. Le shader d'extraction halo lit `light_color` (lightmap pré-éclairage Minecraft) ou un `EmissiveSampler` MRT pour identifier les pixels émissifs.
- En Low : `light_color` seul, halo réduit
- En Minimal : aucun dynamic buffer (économie GPU)
- Activation via `VeilDefinitionsBridge` (équivalent conceptuel de `ShaderPreDefinitions.set("VEIL_LIGHT_COLOR")`, à confirmer en Phase 0) qui déclenche recompile auto si l'API verrouillée le supporte

**Mode A (Dynamic Buffer disponible)** = méthode primaire.
**Mode B (luminance threshold pur)** = fallback si on choisit de désactiver dynamic buffers pour économie. Toujours implémenté comme branche `#ifdef` dans le shader d'extraction.

**Conséquence vs V1** : la question "Veil expose-t-il le block ID en G-buffer ?" est résolue. Veil expose les buffers dont on a besoin (light_color, light_uv, normal, albedo). On n'a pas besoin du block ID.

### 6.4 Shader Programs JSON (`pinwheel/shaders/program/`)

**Capacité Veil** : 1 fichier JSON par programme shader, avec `vertex`/`fragment`/`compute`/`geometry`/`tesselation_*`, `definitions`, `textures` (location ou framebuffer), `blend`, `required_features`.

**Usage projet** :
- 1 JSON par effet (`color_grading.json`, `vignette.json`, etc.)
- `definitions` listées dans le JSON, valeurs poussées par `BackroomsShaderDefinitions` selon profil/level/état
- `textures` : LUT 3D référencée par `location`; framebuffers intermédiaires référencés par `name + sampler`
- `blend` configuré stage par stage (composite additif pour bloom/halo)
- `required_features` utilisé pour les effets optionnels Ultra (ex: `BINDLESS_TEXTURE` si on tente une LUT 3D bindless en Ultra) — graceful degradation native

### 6.5 Built-in Uniforms Veil

**Capacité Veil** : uniforms exposés gratuitement dans tout shader Veil, sans Java setup.

| Uniform Veil/MC | Notre usage |
|---|---|
| `VeilRenderTime` (float, secondes) | Animation flicker, grain temporel, warp |
| `GameTime` (float [0..1]) | (non utilisé V1, réservé) |
| `ScreenSize` (vec2) | Vignette, chromatic aberration |
| `FogColor`, `FogStart`, `FogEnd`, `FogShape` | Fog backrooms cohérent |
| `Light0_Direction`, `Light1_Direction` | Pas utilisé V1 (indoor) |

**Conséquence** : on **n'écrit pas** de `TimeUniformProvider` Java en V1. La V1 le faisait inutilement. Veil pousse `VeilRenderTime` automatiquement.

### 6.6 Uniform Blocks (UBO) — `#veil:buffer`

**Capacité Veil** : Uniform Buffer Objects via `VeilShaderBufferRegistry`, avec interface name, importable via `#veil:buffer veil:camera VeilCamera`.

**Usage projet** :
- `#veil:buffer veil:camera VeilCamera` pour les passes screen-space qui ont besoin de matrices vue/projection
- 1 UBO custom `petasse_gang_additions:backrooms_state` enregistré côté Java contenant `sanity`, `portalProximity`, `transitionProgress`, `zoneAmbientColor` — un seul update GPU par frame plutôt que N uniforms scalaires

**Bénéfice perf** : un UBO update = 1 appel GL. N uniforms = N appels.

### 6.7 ShaderPreDefinitions

**Capacité Veil** : `#define` injectables depuis Java via l'API Veil verrouillée en Phase 0, exposée uniquement par `VeilDefinitionsBridge`. Recompile auto des shaders concernés si confirmé par `phase0_findings.md`.

**Usage projet — clé pour le système de profils** :
- `BACKROOMS_PROFILE_ULTRA` / `_LOW` / `_MINIMAL` exposé global selon profil utilisateur
- `BACKROOMS_LEVEL_0` / `_1` / etc. selon level
- `BACKROOMS_HALLU_ENABLED` / `BACKROOMS_WARP_ENABLED` selon état runtime
- Les shaders branchent `#ifdef` sur ces defines pour activer/désactiver des sections

**Conséquence** : pas besoin de recompiler depuis Java manuellement. Veil détecte le changement de definition et recompile les shaders impactés.

### 6.8 Hook pré-post-processing Veil

**Capacité Veil** : hook officiel de pré-post-processing, dont le nom exact doit être confirmé en Phase 0, pour pousser des uniforms juste avant l'exécution d'un pipeline.

**Usage projet** : c'est notre **point d'entrée unique** pour pousser les uniforms dynamiques (sanity, portalProximity, etc.) depuis Java. Le `ShaderEventAdapter` filtre par nom de pipeline (`backrooms_*`) et appelle `VeilUniformBridge`, qui relaie ensuite les `UniformProvider`.

**Pas de couche custom** d'event bus shader. Veil le fait.

### 6.9 Shader Features

**Capacité Veil** : un shader peut déclarer des `required_features` (ex: `BINDLESS_TEXTURE`). Si l'extension GLSL n'est pas supportée, Veil **refuse de loader** et ne crash pas.

**Usage projet** :
- `BloomEffect` Ultra peut utiliser `BINDLESS_TEXTURE` pour samples massifs si dispo
- Si non dispo → Veil ne charge pas → notre `PipelineHealthMonitor` détecte (Veil log explicite) → retire la stage du pipeline actif
- Loi 3 satisfaite nativement

### 6.10 Shader Modification (RÉSERVÉ V2)

**Capacité Veil** : DSL maison pour injecter dans des shaders existants (vanilla ou custom) via `[GET_ATTRIBUTE]`, `[OUTPUT]`, `[UNIFORM]`, `[FUNCTION ... HEAD/TAIL]`.

**Usage projet V1** : **non utilisé**. On rend tout en post-process pur, on ne touche pas aux shaders du world rendering.

**Réservé V2** : si on veut tagger les blocs émissifs côté vertex shader vanilla, on injectera un modifier pour écrire vers le `light_color` dynamic buffer.

### 6.11 Compute Shaders (RÉSERVÉ V2)

**Capacité Veil** : `.comp` shaders supportés, dispatch via `glDispatchCompute` depuis Java.

**Usage projet V1** : **non utilisé**. Tous les effets sont fragment shaders.

**Réservé V2** : génération procédurale de LUT 3D, Kawase blur grand format, simulations procédurales pour hallucinations.

### 6.12 Quasar (RÉSERVÉ V2)

**Capacité Veil** : système de particules GPU avancé, framework dédié.

**Usage projet V1** : non.

**Réservé V2** : particules d'hallucination volumétriques (poussières, micro-flashes, formes fugitives en bord de vue).

### 6.13 Necromancer

**Capacité Veil** : rigging d'entités osseuses.

**Usage projet** : **non pertinent**. Les Backrooms n'ont pas d'animation skelettale custom à ce stade.

---

## §7. Sources of Truth

| Donnée | SoT | Lecteurs |
|---|---|---|
| Profil qualité actif | `QualityProfileSelector` | `LevelShaderResolver`, `BackroomsShaderDefinitions` |
| Catalogue effets | `EffectRegistry` | `LevelShaderResolver`, `ProfileRegistry` |
| Pipeline Veil actif (nom ResourceLocation) | `LevelShaderResolver` | `ShaderManager` |
| État runtime des effets (OK/DEGRADED/DISABLED) | `PipelineHealthMonitor` | `BridgeHealthMonitor` (arbo v4), `LevelShaderResolver` |
| Definitions Veil push | `BackroomsShaderDefinitions` | (push dans Veil — pas relu) |
| Uniforms par-frame | `UniformContext` (lit providers) | shaders Veil via `preVeilPostProcessing` |
| Fallbacks uniforms | `UniformFallbacks` | `UniformContext` (cas missing) |

**Règle** : aucune autre classe ne stocke ces données.

---

# PARTIE C — Architecture qualité / level

## §8. Modèle de qualité — les 3 profils

### Définition

Trois profils. Pas quatre, pas deux. Pas de "Off".

| Profil | Cible matériel | Pipelines Veil utilisés | Audience |
|---|---|---|---|
| **Ultra** | GPU récent (RTX 3060+, RX 6600+) | `backrooms_ultra_<level>.json` — toutes stages, FBs pleine résolution, dynamic buffers actifs, bloom multipass | Joueurs qui veulent le maximum |
| **Low** | GPU médian (GTX 1060, GTX 1650, iGPU récent) | `backrooms_low_<level>.json` — stages réduits, FBs accessoires 50%, halo simplifié, pas de bloom | Majorité |
| **Minimal** | GPU faible / iGPU ancien | `backrooms_minimal_<level>.json` — baseline + grading + tonemap + vignette + grain léger | Configs limites |

### Sélection

- **1er lancement** : `AutoQualityDetector` lit `GL_RENDERER`/`GL_VENDOR` via `VeilShaderBridge` ou un bridge validé Phase 0, match contre une whitelist, sinon **Low** par défaut (jamais Ultra automatique).
- **Override utilisateur** : option dans le menu vidéo, sous-section "Backrooms" (apparaît uniquement quand le mod est chargé en single-player local). Commande `/backrooms quality <ultra|low|minimal>`.
- **Persistance** : `BackroomsConfig` (arbo v4 §3 → `backrooms/config/`).

### Définition d'un profil (JSON)

`shader_profiles/ultra.json` :
```json
{
  "id": "petasse_gang_additions:ultra",
  "displayName": "Ultra",
  "veilPipelinePrefix": "petasse_gang_additions:backrooms_ultra_",
  "shaderDefinitions": [
    "BACKROOMS_PROFILE_ULTRA",
    "BACKROOMS_DYNAMIC_LIGHT_COLOR",
    "BACKROOMS_DYNAMIC_ALBEDO"
  ],
  "frameBudgetMs": 8.0,
  "enabledEffects": [
    "petasse_gang_additions:color_grading",
    "petasse_gang_additions:tonemap",
    "petasse_gang_additions:vignette",
    "petasse_gang_additions:film_grain",
    "petasse_gang_additions:chromatic_aberration",
    "petasse_gang_additions:flicker_lighting",
    "petasse_gang_additions:emissive_halo",
    "petasse_gang_additions:bloom",
    "petasse_gang_additions:screenspace_warp",
    "petasse_gang_additions:hallucination_overlay",
    "petasse_gang_additions:peripheral_blur"
  ]
}
```

`minimal.json` liste uniquement les 5 baseline avec moins de définitions et un budget bas.

### Validation au boot

`ProfileRegistry` charge tous les profils, valide chacun contre `_schema.json`, **refuse de booter** si invalide. Cas crash-explicite.

---

## §9. Système d'overrides par level

Un `LevelShaderProfile` définit la baseline + les overrides + les effets exclusifs **pour un level donné**.

### Définition (JSON)

`level_shaders/level0.json` :
```json
{
  "id": "petasse_gang_additions:level0",
  "displayName": "Level 0",
  "shaderDefinitions": ["BACKROOMS_LEVEL_0"],
  "gradingLut": "petasse_gang_additions:level/level0/grading_lut",
  "ambientColor": [1.0, 0.92, 0.55],
  "effectOverrides": {
    "petasse_gang_additions:vignette": {
      "intensity": 0.4,
      "smoothness": 0.6
    },
    "petasse_gang_additions:flicker_lighting": {
      "frequency": 1.5,
      "amplitude": 0.15
    },
    "petasse_gang_additions:film_grain": {
      "intensity": 0.08
    }
  },
  "exclusiveEffects": [],
  "zonalDistortions": []
}
```

### Garanties cross-level (validées au boot)

- Tout level **doit** définir `gradingLut` (Loi 1)
- Tout level **doit** définir `ambientColor`
- Tout level **peut** override des effets baseline mais pas les supprimer
- `effectOverrides` validés contre les `shader_effects/<effectId>.schema.json` correspondants — si un param n'existe pas pour cet effet, le boot échoue

### Résolution finale

`LevelShaderResolver` compose au runtime :

```
Profil utilisateur (Ultra/Low/Minimal)
    +
LevelShaderProfile (level0/level1/...)
    +
État runtime (sanity, proximité, transition)
    ↓
1) Sélectionne le pipeline Veil de base : "backrooms_<profile>_<level>.json"
2) Pousse les shaderDefinitions cumulées (profile + level + état)
3) Pousse les uniforms via UniformContext
4) Bind LUT et ambientColor du level
```

Recalculé : entrée dimension, changement profil utilisateur, franchissement seuil d'état runtime. **Pas par frame** — résultat caché.

---

## §10. Hiérarchie de décision runtime

En cas de conflit entre composants, la priorité est :

```
1. PipelineHealthMonitor   (santé : un effet KO est désactivé, inviolable)
2. LevelShaderProfile      (identité du level)
3. QualityProfileSelector  (qualité — peut désactiver)
4. EffectCondition         (état runtime active/désactive)
5. UniformContext          (valeurs courantes)
```

**Règle** : la stabilité (pipeline) prime sur l'identité, qui prime sur la qualité, qui prime sur l'état, qui prime sur les valeurs.

**Exemple** : un effet `EmissiveHalo` est marqué KO par `PipelineHealthMonitor` (échec compile à cause d'un GPU manquant `BINDLESS_TEXTURE` en Ultra) → désactivé même si Level 0 le voulait. Un effet `ScreenSpaceWarp` est une exception Loi 2 → désactivé en Minimal même si la zone le demande, parce que profile prime sur condition zonale.

---

## §11. Pipeline = liste ordonnée de stages Veil

**Décision verrouillée** : pas de DAG. Pipeline = **liste ordonnée** de stages dans le JSON Veil (`stages: [...]`). Les dépendances sont implicites : output du stage N = input du stage N+1.

### Ordre canonique du pipeline (contrat)

```
1. World rendering Veil (sortie : minecraft:main / veil:post — HDR linéaire)
2. ToneMapping (ACES Approx)               → conversion HDR → LDR
3. ColorGrading (LUT 3D)                   → grading LDR du level
4. EmissiveHalo (extract → blur → composite) [Ultra/Low]
5. Bloom (extract → blur 5x → composite)   [Ultra uniquement]
6. ScreenSpaceWarp                         [zonal, Ultra/Low]
7. HallucinationOverlay                    [state-driven]
8. PeripheralBlur                          [state-driven]
9. ChromaticAberration                     [Ultra/Low, intensité 0 en Minimal]
10. Vignette
11. FilmGrain
12. → output framebuffer (veil:post)
```

**Ordre verrouillé. ToneMapping est avant ColorGrading. LUT est appliquée en LDR. Pas de discussion runtime, pas d'option Phase shader 1 — c'est cette ligne qu'on suit.**

### Multi-branches gérées en sous-pipelines

`EmissiveHalo` et `Bloom` ont chacun une mini-chaîne extract → blur → composite. Ces mini-chaînes sont des **stages successifs dans le même JSON Veil**, écrivant dans des FBs intermédiaires. Pas de structure DAG, juste des stages qui pointent leur input/output framebuffer par nom.

### Exemple JSON simplifié

`backrooms_ultra_level0.json` (extrait) :
```json
{
  "priority": 1000,
  "dynamicBuffers": ["albedo", "light_color"],
  "framebuffers": {
    "halo_extracted": { "format": "R11F_G11F_B10F", "width": "q.screen_width / 2", "height": "q.screen_height / 2" },
    "halo_blurred": { "format": "R11F_G11F_B10F", "width": "q.screen_width / 2", "height": "q.screen_height / 2" }
  },
  "stages": [
    { "type": "veil:blit", "shader": "petasse_gang_additions:tonemap_aces", "in": "minecraft:main", "out": "veil:post" },
    { "type": "veil:blit", "shader": "petasse_gang_additions:color_grading", "in": "veil:post", "out": "veil:post", "clear": false },
    { "type": "veil:blit", "shader": "petasse_gang_additions:emissive_halo_extract", "in": "veil:post", "out": "halo_extracted" },
    { "type": "veil:blit", "shader": "petasse_gang_additions:emissive_halo_blur", "in": "halo_extracted", "out": "halo_blurred" },
    { "type": "veil:blit", "shader": "petasse_gang_additions:emissive_halo_composite", "in": "halo_blurred", "out": "veil:post", "clear": false },
    { "type": "veil:blit", "shader": "petasse_gang_additions:flicker_lighting", "in": "veil:post", "out": "veil:post" },
    { "type": "veil:blit", "shader": "petasse_gang_additions:chromatic_aberration", "in": "veil:post", "out": "veil:post" },
    { "type": "veil:blit", "shader": "petasse_gang_additions:vignette", "in": "veil:post", "out": "veil:post" },
    { "type": "veil:blit", "shader": "petasse_gang_additions:film_grain", "in": "veil:post", "out": "veil:post" }
  ]
}
```

C'est **lisible humainement**, **modifiable sans recompile Java**, **hot-reloadable par Veil**.

---

## §12. Système d'uniforms

### Principe

L'`UniformContext` est le **bus central** côté Java. Il agrège les `UniformProvider` et est consulté par `ShaderEventAdapter`, qui passe par `VeilUniformBridge`. Pour chaque pipeline backrooms actif, le bridge pousse les valeurs validées par Phase 0.

### Catalogue (V1)

| UniformId | Type | Provider | Fréquence màj | Fallback obligatoire |
|---|---|---|---|---|
| `u_zoneAmbientColor` | vec3 | `ZoneAmbientProvider` | Changement zone | `vec3(1.0)` |
| `u_levelGradingLut` | sampler3D | (texture binding via shader JSON) | Changement level | LUT identité (PNG strip neutre) |
| `u_portalProximity` | float [0..1] | `PortalProximityProvider` | Chaque frame | `0.0` |
| `u_transitionProgress` | float [0..1] | `TransitionProgressProvider` | Chaque frame | `0.0` |
| `u_playerSanity` | float [0..1] | `SanityUniformProvider` | Tick (5 Hz) | `1.0` |
| `u_warpIntensity` | float [0..1] | `ZoneAmbientProvider` (zonal) | Changement zone | `0.0` |
| `u_flickerSeed` | float | (calculé GLSL via VeilRenderTime) | — | — |

**Note** : tout ce qui était `u_time`, `u_resolution`, `u_aspectRatio` en V1 → utilise les built-in Veil (`VeilRenderTime`, `ScreenSize`).

### Règles d'or des uniforms

**Toutes obligatoires, validées au boot.**

1. **Tout uniform requis par un shader doit avoir une entrée dans le catalogue.** Sinon erreur dev au boot.
2. **Tout `UniformProvider` doit fournir une valeur de fallback valide.** Centralisé dans `UniformFallbacks`.
3. **Aucun shader ne peut lire un uniform sans fallback documenté.** Convention : commenter en haut du shader fragment :
   ```glsl
   // Required uniforms:
   //   u_playerSanity (float, fallback 1.0) — semantics: 0=insane, 1=sain
   ```
4. **Si un `pipeline.getUniform(name)` retourne null** (uniform absent du shader) : log warning unique et skipper, pas de NPE. Veil retourne null par contrat.
5. **Pas d'écriture multi-provider du même uniform.** Validé au boot par `UniformContext`.
6. **Pas de lecture directe d'état Java (Player.sanity) depuis un effet** — toujours via uniform. Couplage faible obligatoire.
7. **`UniformProvider` doit déclarer ses uniforms produits** dans son interface (`Set<UniformId> produces()`). Boot vérifie cohérence.

### Cas missing uniform

Si un `UniformProvider` lance une exception lors du calcul :
1. Log warning
2. `UniformContext` utilise `UniformFallbacks.get(uniformId)` pour cette frame
3. Provider mis en quarantaine 1 seconde puis retry (pour ne pas spammer log)

---

# PARTIE D — Effets

## §13. Conventions GLSL & Veil shader

### Header obligatoire de chaque .fsh

```glsl
#version 330 core
// Effect: <Nom de l'effet>
// Pipeline stage: post (after world)
// Required uniforms:
//   <list with type, fallback, semantics>
// Required textures (samplers):
//   <list>
// Output: vec4 fragColor (RGBA, linéaire si pré-tonemap, sRGB si post-tonemap)
```

### Règles

- `#version 330 core` toujours, jamais 410+ (Veil-compatible OpenGL 3.3)
- Précision : `precision highp float;` `precision mediump int;`
- Pas de `gl_FragColor` → toujours `out vec4 fragColor`
- Pas de `texture2D()` → toujours `texture()`
- Includes via convention Veil : `#include petasse_gang_additions:noise` (mappe à `assets/.../pinwheel/shaders/include/noise.glsl`)
- Includes Veil natifs disponibles : `#include veil:fog`, `#include veil:space_helper`, `#include veil:light`
- Imports d'UBO : `#veil:buffer veil:camera VeilCamera`
- 4 espaces, fonctions camelCase, constantes UPPER_SNAKE
- Pas de magic numbers — extraire en `const float NAME = ...`

### Pièges classiques à éviter

- `pow(negative, x)` → undefined GLSL
- `texture()` avec sampler null → driver crash sur certains GPU (Veil retourne sampler0 par défaut, mais c'est rarement ce qu'on veut)
- Lire/écrire le même framebuffer dans un même shader — undefined behavior. Veil ne garde-fou pas, à nous d'éviter
- Boucles à count dynamique sans `[[unroll]]` — perf imprévisible

---

## §14. Effets baseline cross-level

5 effets **présents dans les 3 profils** (qualité variable). Identité de base d'un Backroom.

### 14.1 ToneMapping (ACES)

**Rôle** : ACES Approximation, HDR linéaire → LDR. **Premier dans le pipeline post-world.**

**Implémentation** : 1 stage `veil:blit`. ACES Narkowicz formule standard.

**Profils** : identique 3 profils.

**Schema params** (`shader_effects/tonemap.schema.json`) :
```json
{
  "exposure": { "type": "number", "min": 0.1, "max": 4.0, "default": 1.0 }
}
```

**Budget** : 0.05 ms.

### 14.2 ColorGrading (LUT 3D)

**Rôle** : sample LUT 3D du level pour grading colorimétrique. **Toujours après ToneMapping**, en LDR.

**Implémentation** : 1 stage `veil:blit`. Texture binding via shader JSON `textures.LevelLut.type=location`. Trilinear filtering pour éviter banding.

**Profils** :
- Ultra/Low : LUT 32³, trilinear
- Minimal : LUT 16³ (bake auto au boot Minimal), bilinear

**Schema params** :
```json
{
  "lutTexture": { "type": "string", "format": "resource_location" },
  "intensity": { "type": "number", "min": 0.0, "max": 1.0, "default": 1.0 }
}
```

**Budget** : 0.1 ms.

### 14.3 Vignette

**Rôle** : assombrissement progressif des bords. Distance radiale du centre + smoothstep.

**Profils** : présent dans 3 profils. Intensité variable selon level.

**Schema params** :
```json
{
  "intensity": { "type": "number", "min": 0.0, "max": 1.0, "default": 0.4 },
  "smoothness": { "type": "number", "min": 0.0, "max": 1.0, "default": 0.6 },
  "tint": { "type": "array", "items": "number", "minItems": 3, "maxItems": 3, "default": [0,0,0] }
}
```

**Budget** : 0.02 ms.

### 14.4 FilmGrain

**Rôle** : grain stable temporellement (Loi 4). Hash spatial + composante temporelle quantifiée à 8 Hz via `VeilRenderTime`.

**Profils** :
- Ultra/Low : grain 1px, magnitude full
- Minimal : grain 2px (downsample), magnitude 50%

**Schema params** :
```json
{
  "intensity": { "type": "number", "min": 0.0, "max": 0.3, "default": 0.08 },
  "monochrome": { "type": "boolean", "default": false }
}
```

**Budget** : 0.05 ms.

### 14.5 ChromaticAberration

**Rôle** : décalage radial des canaux R/B. Sensation surveillance / mauvaise caméra.

**Profils** :
- Ultra/Low : intensité full
- Minimal : intensité **0** (effet présent dans le pipeline mais neutre — Loi 2 strict)

**Schema params** :
```json
{
  "intensity": { "type": "number", "min": 0.0, "max": 1.0, "default": 0.3 }
}
```

**Budget** : 0.1 ms (négligeable en Minimal car intensité 0 → branchement skip dans GLSL via `#ifdef BACKROOMS_PROFILE_MINIMAL`).

---

## §15. Effets optionnels (par profil ou par level)

Activés sélectivement. Tous ont un schema strict côté `shader_effects/<id>.schema.json` validé au boot.

### 15.1 FlickerLighting

**Rôle** : moduler luminosité globale par courbe pseudo-aléatoire low-frequency. Néons défaillants. **Effet d'identité fort.**

**Profils** : Ultra et Low. Minimal : version simplifiée (sinus pur) ou désactivé selon level.

**Schema** :
```json
{
  "frequency": { "type": "number", "min": 0.1, "max": 8.0, "default": 1.5 },
  "amplitude": { "type": "number", "min": 0.0, "max": 0.5, "default": 0.15 },
  "chaosFactor": { "type": "number", "min": 0.0, "max": 1.0, "default": 0.3 }
}
```

**Budget** : 0.05 ms.

### 15.2 EmissiveHalo

**Rôle** : faux halo lumineux autour des pixels émissifs. Donne l'impression de lumière qui déborde.

**Implémentation — Mode A (primaire, Ultra/Low)** : Dynamic Buffer Veil `light_color` ou MRT `EmissiveSampler` (selon décision Phase 0). 3 stages : `extract → blur Kawase → composite additif`.

**Implémentation — Mode B (fallback universel)** : extraction par luminance threshold. Branche `#ifdef NO_DYNAMIC_BUFFER` dans le shader extract.

**Mutex avec Bloom** : voir §15.4 — en Low, Halo seul ; en Ultra, Halo + Bloom.

**Profils** :
- Ultra : Kawase 3 itérations, FBs 50%, Mode A
- Low : Kawase 1 itération, FBs 25%, Mode A
- Minimal : skippé

**Schema** :
```json
{
  "haloRadius": { "type": "number", "min": 0.0, "max": 5.0, "default": 1.0 },
  "tintColor": { "type": "array", "items": "number", "minItems": 3, "maxItems": 3, "default": [1,1,1] },
  "intensity": { "type": "number", "min": 0.0, "max": 2.0, "default": 1.0 },
  "mode": { "type": "string", "enum": ["A", "B"], "default": "A" }
}
```

**Budget** : 0.6 ms Ultra / 0.3 ms Low.

### 15.3 ScreenSpaceWarp

**Rôle** : déformation locale screen-space. Couloirs tordus, espaces non-euclidiens. UV warping piloté par noise spatial+temporel.

**Activation** : **zonal**. Joueur dans une zone taguée `looping_corridor` ou `warped_room` du level (système de tags des zones — cf. arbo v4 et Pilier 3 V4.3).

**Loi 2 exception** : non-identitaire et zonal → autorisé absent en Minimal.

**Profils** :
- Ultra : pleine résolution
- Low : 50% upscale (warp masque l'aliasing)
- Minimal : **skippé** (exception Loi 2 §2)

**Schema** :
```json
{
  "intensity": { "type": "number", "min": 0.0, "max": 1.0, "default": 0.4 },
  "frequency": { "type": "number", "min": 0.1, "max": 5.0, "default": 1.0 },
  "temporalSpeed": { "type": "number", "min": 0.0, "max": 5.0, "default": 0.5 }
}
```

**Garde-fou** : zone non-warpable si elle contient une interaction critique (objet ramassable, levier). Validé au build par `ZoneTagRegistry`.

**Budget** : 0.4 ms Ultra / 0.2 ms Low.

### 15.4 Bloom

**Rôle** : bloom HDR sur pixels brights. Sensation luminance "irréelle".

**Mutex Bloom / EmissiveHalo** :

| Profil | Halo | Bloom | Justification |
|---|---|---|---|
| Ultra | ✅ Kawase 3 itérations | ✅ 5 itérations | Halo local + bloom global = look maximal |
| Low | ✅ Kawase 1 itération | ❌ | Halo seul couvre 80% du look pour 30% du coût |
| Minimal | ❌ | ❌ | Trop coûteux |

**Schema** :
```json
{
  "threshold": { "type": "number", "min": 0.0, "max": 5.0, "default": 0.85 },
  "intensity": { "type": "number", "min": 0.0, "max": 2.0, "default": 0.6 },
  "iterations": { "type": "integer", "min": 3, "max": 7, "default": 5 }
}
```

**Budget** : 1.5 ms Ultra.

### 15.5 HallucinationOverlay

**Rôle** : overlay semi-transparent triggered par `u_playerSanity < threshold`. Duplication fantôme, glissement chromatique, scan lines, formes vagues bord de vue.

**Activation** : `StateDriven` — `u_playerSanity < 0.3` (seuil par level).

**Profils** :
- Ultra : 2 stages (overlay + chromatic bleed)
- Low : 1 stage (overlay simple)
- Minimal : skippé

**Schema** :
```json
{
  "palette": { "type": "string", "enum": ["subtle", "aggressive"], "default": "subtle" },
  "triggerThreshold": { "type": "number", "min": 0.0, "max": 1.0, "default": 0.3 }
}
```

**Budget** : 0.3 ms Ultra / 0.15 ms Low.

### 15.6 PeripheralBlur

**Rôle** : blur radial des bords. Complément hallucinations ou transition portail.

**Activation** : `StateDriven` — `u_playerSanity < 0.5` OR `u_transitionProgress > 0`.

**Profils** :
- Ultra : Gaussian 9-tap
- Low : Gaussian 5-tap
- Minimal : skippé

**Schema** :
```json
{
  "radius": { "type": "number", "min": 0.0, "max": 2.0, "default": 0.7 },
  "innerRadius": { "type": "number", "min": 0.0, "max": 1.0, "default": 0.4 }
}
```

**Budget** : 0.3 ms Ultra / 0.15 ms Low.

---

# PARTIE E — Robustesse

## §16. Hot reload encadré

**En production** : aucun hot-reload. Veil compile au boot et garde les programmes.

**En mode dev** (`fabric.development=true` ou flag `-Dpetasse.dev=true`) :
- Veil hot-reload natif des shaders (déclencheur : F3+T MC, ou modification fichier détectée par Veil — Veil le supporte nativement)
- Hot-reload de nos JSON profiles/levels via `WatchService` Java NIO côté nous

### Règles obligatoires

1. **Debounce 300 ms minimum** entre détection et action. Multi-edit (sauvegarde IDE qui touche 5 fichiers) → 1 seule recompile.
2. **Queue de recompilation, 1 à la fois.** Pas de parallélisme — risque de driver issues GL.
3. **Lock du GL thread obligatoire** pendant la recompile. Utiliser `RenderSystem.recordRenderCall()` Minecraft ou le bridge Veil validé en Phase 0.
4. **Echec compile = ancien shader gardé actif** (Veil-natif : refuse de swap, log explicit).

---

## §17. Auto-dégradation persistante

**Décision verrouillée : pas de toggling dynamique runtime.** Une fois un effet désactivé pour cause de performance, il reste désactivé jusqu'à reload manuel ou changement de profil.

### Politique

1. Mesure rolling average sur 60 frames du temps GPU par stage (via timestamp queries OpenGL exposées par Veil)
2. Si une stage dépasse `budgetMs * 1.5` pendant 60 frames consécutives :
   - Log warning unique
   - `PipelineHealthMonitor.disable(effectId)`
   - L'effet est retiré du pipeline JSON Veil actif (via composition côté Java de `replace`/`priority`)
3. **Pas de réactivation auto.**
4. Réactivation possible :
   - Changement de profil utilisateur (recompose pipeline complet)
   - Commande `/backrooms shader effect enable <id>` (force, dev only)
   - Commande `/backrooms shader reload` (reload manuel)

### Ne touche jamais

- Les effets baseline (Loi 1) : si un baseline dépasse son budget, on log mais on **ne désactive pas**. C'est un bug à traiter dev-side.
- L'ordre canonique du pipeline §11 : on retire des stages, on ne réordonne pas.

---

## §18. Performance et budgets

### GPU médian de référence

**GTX 1060 6GB / RX 580 8GB** comme baseline pour la cible Low.

### Budget total par profil

| Profil | Budget post-process (ms) | À 60 FPS, fraction frame |
|---|---|---|
| Ultra | 8.0 ms | 48% |
| Low | 3.5 ms | 21% |
| Minimal | 0.5 ms | 3% |

Le reste du frame (16.6 ms à 60 FPS) couvre rendering monde, logique, etc.

### Budget par effet (référence GPU médian)

| Effet | Ultra | Low | Minimal |
|---|---|---|---|
| ToneMapping | 0.05 | 0.05 | 0.05 |
| ColorGrading | 0.1 | 0.1 | 0.1 |
| Vignette | 0.02 | 0.02 | 0.02 |
| FilmGrain | 0.05 | 0.05 | 0.025 |
| ChromaticAberration | 0.1 | 0.1 | 0 (skip ifdef) |
| FlickerLighting | 0.05 | 0.05 | 0.025 |
| EmissiveHalo | 0.6 | 0.3 | — |
| ScreenSpaceWarp | 0.4 | 0.2 | — |
| Bloom | 1.5 | — | — |
| HallucinationOverlay | 0.3 | 0.15 | — |
| PeripheralBlur | 0.3 | 0.15 | — |

Mesuré au build via Gradle task `benchmarkShaderEffects` (à terme — Phase Shader 5 minimum).

### Stratégies d'optimisation

| Stratégie | Application |
|---|---|
| Downsample passes accessoires | Bloom, halo, blur — toujours |
| Kawase plutôt que Gaussian séparable | Quand qualité < rapidité |
| FBs `RGB10_A2` plutôt que `RGBA16F` | 90% des cas |
| Réutilisation FB Veil natif | Toujours |
| Skipper passes inactives via shader definitions | `#ifdef BACKROOMS_HALLU_ENABLED` |
| `mediump` varyings | Toujours sauf artefact détecté |

---

## §19. Tests visuels (régression) avec mode déterministe

### Principe

Capture screenshots de référence + diff vs nouvelles captures.

### Mode déterministe **obligatoire** pour toute capture

- `VeilRenderTime` figé à 0 (override via Veil API)
- Grain seedé par UV pure (pas de composante temporelle)
- Flicker à 0
- Hallucinations désactivées

Sans ce mode, les diffs sont infiables.

### Workflow

1. `/backrooms shader capture_reference` : prend N screenshots (1 par level × 3 profils × 3 états = 9N images), stock `tests/visual/reference/`
2. `/backrooms shader capture_compare` : génère `tests/visual/current/`
3. Tâche Gradle `visualDiff` : MSE + SSIM. Seuil MSE généreux (~5% de la dynamique) pour absorber variabilité GPU/driver
4. Mise à jour référence : `git mv current/* reference/` après validation visuelle

### Pas en CI bloquant

Variabilité GPU/driver rend la CI fragile. Tests visuels = **dev tool**.

---

## §20. Compatibilité

### Avec Iris

Cohérent V4.3 §10. `AUTO_TOGGLE` validé Phase 0 → Iris désactivé dans backrooms, Veil contrôle. `WARN_ONLY` (fallback) → coexistence dégradée + message au joueur.

### Avec Sodium

Compatibilité Sodium attendue avec Veil 3.x. À valider sur la version exacte verrouillée en Phase 0.

**Spécificité** : Dynamic Buffers Veil interagissent avec le pipeline de batching Sodium. Phase 0 doit valider que `albedo` et `light_color` fonctionnent avec Sodium chargé sur la version Veil verrouillée.

### Avec Distant Horizons

Risque résiduel sur `ColorGrading` si DH render passe avant le tonemap. À tester Phase 0.

### Avec Sound Physics Remastered

Hors-scope shader.

---

# PARTIE F — Interdits

## §21. Anti-patterns Veil

À ne **jamais** commettre. Sont des signes que tu réinventes Veil.

- ❌ **Créer un `FramebufferPool` côté Java.** Veil gère.
- ❌ **Créer un `PassRegistry`/`PassExecutionGraph` côté Java.** Pipeline Veil = JSON.
- ❌ **Compiler un shader manuellement via `glCompileShader`.** Toujours via JSON shader Veil.
- ❌ **Allouer un FBO via `glGenFramebuffers`.** Toujours via JSON `framebuffers/`.
- ❌ **Pousser un uniform en dehors de `preVeilPostProcessing`.** Race conditions garanties.
- ❌ **Utiliser `setShader` puis manipuler GL state directement.** Veil expose `ShaderUniformAccess`.
- ❌ **Importer une classe Veil hors du pattern bridge** (cf. V4.3 §10 §29).
- ❌ **Réinventer un système d'includes** au lieu d'utiliser `#include domain:id`.
- ❌ **Préprocesser GLSL côté Java** au lieu d'utiliser `definitions` Veil.
- ❌ **Ignorer le retour null de `pipeline.getUniform(name)`.** NPE garantie.

## §22. Anti-patterns Java

- ❌ Hardcoder un effet dans `ShaderManager` (court-circuite `EffectRegistry`)
- ❌ Stocker l'état d'un effet ailleurs que dans `ShaderEffect` (viole SoT)
- ❌ Stocker quoi que ce soit lié à Veil (programmes, FBs) côté Java — c'est le job de Veil
- ❌ Lecture directe `Player.sanity` depuis un effet GLSL — toujours via uniform
- ❌ Charger un JSON profile/level autrement que via `ProfileRegistry`/`LevelShaderRegistry`

## §23. Anti-patterns GLSL

- ❌ `#version` autre que 330 core
- ❌ `gl_FragColor` (utiliser `out vec4`)
- ❌ `texture2D()` (utiliser `texture()`)
- ❌ Boucles à count dynamique sans borne basse
- ❌ `if`/`else` lourds sur le main path (utiliser `mix`, `step`, `smoothstep`)
- ❌ Magic numbers (extraire en `const float`)
- ❌ Lire un uniform sans documenter son range/fallback en haut du shader

---

# PARTIE G — Plan de dev

## §24. Roadmap shaders par phase

Aligné sur phases globales V4.3 §21+. Démarre après Phase 1 globale (config + bridges).

### Phase Shader 0 — Spike Veil (parallèle à Phase 0 globale V4.3)

- Verrouiller version Veil 3.x compatible Minecraft 1.21.1 dans `phase0_findings.md`
- Tester un pipeline JSON minimal (1 stage `veil:blit` neutre)
- Tester chargement d'un framebuffer JSON, lecture/écriture
- Tester `VeilDefinitionsBridge` et la recompile auto des definitions
- Tester le hook pré-post-processing via `VeilPostProcessingBridge` : push d'1 uniform
- Tester Dynamic Buffer `light_color` activation/désactivation
- Vérifier `required_features` graceful degradation
- Tester compat Sodium loaded
- **Livrable** : section `shader pipeline` ajoutée à `phase0_findings.md`. Verdict Mode A vs Mode B pour EmissiveHalo.

### Phase Shader 1 — Couche Java mince + 1 pipeline neutre

- Implémenter `ShaderManager`, `ShaderEventAdapter` (handler shader qui appelle les bridges)
- Implémenter `QualityProfileSelector`, `ProfileRegistry` (chargement JSON)
- Implémenter `LevelShaderRegistry`, `LevelShaderResolver`
- Implémenter `EffectRegistry` + `EffectParamSchema` validation
- Implémenter `BackroomsShaderDefinitions` (push definitions via `VeilDefinitionsBridge`)
- Implémenter `UniformContext` + `UniformProvider` interface + `UniformFallbacks`
- Pipeline JSON minimal `backrooms_minimal_level0.json` (1 stage passthrough)
- **Critère** : `/backrooms shader debug show` affiche le pipeline minimal actif. Pas de visuel changé.

### Phase Shader 2 — Effets baseline (les 5)

- Implémenter ToneMapping ACES
- Implémenter ColorGrading + LUT loader 32³
- Implémenter Vignette, FilmGrain, ChromaticAberration
- Pipelines Ultra/Low/Minimal × Level 0 fonctionnels
- Schemas params validés
- **Critère** : Level 0 visible avec son grading dans les 3 profils. Profils visuellement distincts mais cohérents.

### Phase Shader 3 — Effets optionnels Ultra/Low

- FlickerLighting, EmissiveHalo (Mode A et fallback B)
- Tests visuels de régression mis en place
- Hot reload validé en mode dev
- **Critère** : Level 0 Ultra mood riche distinct de Low.

### Phase Shader 4 — State-driven & zonal

- HallucinationOverlay, PeripheralBlur (state-driven sanity)
- ScreenSpaceWarp (zonal)
- Tagging zones validé via `ZoneTagRegistry`
- **Critère** : entrer zone warpée → distorsion visible. Sanity bas debug → overlay actif.

### Phase Shader 5 — Bloom + auto-dégradation

- Bloom Ultra
- `PipelineHealthMonitor` + `DegradationPolicy` testée
- Calibrage budgets vs mesures réelles
- **Critère** : Ultra atteint 60 FPS stable sur GPU médian. Auto-dégradation fonctionne en cas de stress.

### Phase Shader 6 — Multi-level

- `level1.json` + LUT
- Switch Level 0 ↔ Level 1 sans rebuild pipeline complet
- **Critère** : transition instantanée, pas de flash, pas de freeze.

---

## §25. Critères de validation

Chaque phase livre quand :

- Tests Java verts (`./gradlew test`)
- Validation GLSL verte (`./gradlew validateShaders`)
- Validation JSON profiles/levels/effects verte (`./gradlew validateShaderConfigs`)
- Build complet vert
- runClient sans crash, sans warning shader
- Scenario de phase exécuté manuellement
- Mesure budget sur GPU CI (Phase 5+) dans la cible
- Tests visuels OK ou justifiés

---

## §26. Préambule développeur (humain ou LLM)

### Règles non négociables

1. **Veil-natif first.** Si Veil offre la fonctionnalité, tu l'utilises. Toute couche Java qui duplique Veil est un anti-pattern.
2. **Pas d'invention d'API Veil.** Toute classe/méthode Veil utilisée doit correspondre à la version verrouillée en Phase 0. Sinon `// SPEC-GAP: Phase 0 — verify against locked Veil API` et signaler.
3. **Pas d'invention de fonction GLSL.** Built-in 330 ou définie dans `pinwheel/shaders/include/`. Sinon ajouter explicitement.
4. **Pas d'invention d'uniform.** Catalogue §12. Sinon ajouter d'abord le `UniformProvider` + entrée catalogue + fallback.
5. **Phase 0 shader obligatoire** avant Phase 1+ shader.
6. **Respect des Lois §2.** Justifier toute exception explicitement.
7. **Frame budget déclaré dans schema effet.** Mesurable et mesuré Phase 5+.
8. **Pas de TODO silencieux.** `// SPEC-GAP: §X` explicite.
9. **Cohérence avec V4.3.** Pas de contradiction. Si conflit → raise.
10. **Validation glslang locale + JSON Schema avant commit.**
11. **JSON profile/level/effect override → validé contre `_schema.json` correspondant.**
12. **Aucun shader ne lit un uniform sans fallback documenté.**
13. **Aucun handler shader hors `ShaderEventAdapter`, et aucun import Veil direct dans `backrooms/shader/`.**

### Anti-hallucination Veil

Les noms Veil comme `VeilEventPlatform`, `VeilRenderSystem`, `ShaderPreDefinitions`, `required_features`, `dynamicBuffers`, `pinwheel/post` sont conceptuels tant que `phase0_findings.md` ne les confirme pas.

Codex ne doit générer aucun import Veil direct depuis cette spec.

### Anti-patterns IA spécifiques (rappel)

- Hardcoder une couleur (LUT à la place)
- Hardcoder une intensité (uniform à la place)
- Générer un effet sans condition d'activation explicite
- Générer un profil sans documenter ce qu'il coupe par rapport à Ultra
- Ajouter un effet baseline sans justification (baseline = verrouillée Loi 1)
- Inventer un nom de framebuffer sans entrée correspondante dans `pinwheel/framebuffers/`

---

# Annexes

## §27. Glossaire

- **Pinwheel** : convention Veil pour le dossier resources `assets/modid/pinwheel/`.
- **Stage** : étape unitaire d'un pipeline post-processing Veil (`veil:blit`, `veil:copy`, etc.).
- **Pipeline (Veil)** : suite ordonnée de stages, JSON. Notre niveau d'orchestration.
- **Effect (notre projet)** : unité fonctionnelle, peut grouper 1+ stages dans le pipeline JSON. Exposé par `EffectRegistry`.
- **Profile (qualité)** : Ultra/Low/Minimal, sélectionne quel pipeline JSON charger.
- **LevelShaderProfile** : configuration shader pour une dimension backrooms spécifique.
- **Dynamic Buffer** : buffer extra (albedo/normal/light_uv/light_color) tagable depuis shaders Veil pour deferred-light.
- **MRT** : Multi-Render-Target (framebuffer avec plusieurs color attachments).
- **ShaderPreDefinitions** : système Veil de `#define` injectables avec recompile auto.
- **ShaderModification** : DSL Veil pour injecter dans des shaders existants (V2).
- **Quasar** : système de particules GPU Veil (V2).
- **Necromancer** : système de rigging Veil (non utilisé).
- **MoLang** : langage d'expressions Bedrock supporté par Veil pour width/height responsive.
- **LUT** : Look-Up Table 3D 32³ pour grading colorimétrique.
- **ACES** : Academy Color Encoding System, tone-mapping standard.
- **Kawase blur** : blur séparable optimisé alternative à Gaussian, qualité comparable à coût réduit.
- **SoT** : Source of Truth.

---

## §28. Choix tranchés et alternatives écartées

| Choix retenu | Alternative écartée | Raison |
|---|---|---|
| Veil 3.x compatible Minecraft 1.21.1 | Ancienne branche Veil 2 | Dynamic Buffers, fix stencil, compat Sodium et includes natifs à confirmer en Phase 0 |
| Pipeline = liste ordonnée | DAG | Veil natif est une liste. DAG = sur-ingénierie. Multi-branches gérées en stages séquentiels. |
| 3 profils sans Off | Slider continu / mode Off | Plus prévisible, identité préservée |
| Screen-space warp | Vertex displacement | Conflit Sodium, désync hitbox |
| Tint zonal + EmissiveHalo | Colored block lighting custom | Sous-projet hors scope V1 |
| LUT 32³ PNG strip | Color matrix 4×4 | Flexibilité non-linéaire |
| ACES Approximation | Reinhard, Uncharted 2 | Standard industrie, look filmique |
| Ordre verrouillé : Tonemap → LUT en LDR | LUT en HDR | Banding garanti en HDR LUT |
| Auto-dégradation persistante | Toggling dynamique | Oscillation = nausée visuelle |
| JSON Veil natif | DSL Java custom | Veil le fait, on consomme |
| Built-in `VeilRenderTime` | `TimeUniformProvider` Java | Veil le fournit gratis |
| UBO custom `backrooms_state` | N uniforms scalaires | 1 update GPU au lieu de N |
| `ShaderPreDefinitions` pour profil/level | Multiplier fichiers shader | Recompile auto, 1 source par effet |
| Schema JSON par effet | `effectOverrides` libre | Sans schema, Codex invente des params |
| Test visuel = dev tool, pas CI | CI bloquante | Variabilité GPU/driver |
| Mode déterministe obligatoire | Test sur shader normal | Sinon diff infiable |
| Mode A (Dynamic Buffer) primaire + Mode B (luminance) fallback | Mode B uniquement | Précision Mode A nettement meilleure |
| Veil 3.x au lieu de 2.5.1 | Version exacte verrouillée Phase 0 | Trade-off : features critiques pour le projet, à confirmer contre l'API réellement retenue |

---

## §29. Notes d'alignement déjà intégrées à la spec V4.3

Ces modifications étaient le patch suggéré pour la V4.2. Elles sont maintenant intégrées dans `spec_veil_portail_shaders_V4_3.md` et restent ici comme trace d'alignement.

### §3 Stack technique

**Avant** :
> Rendering | **Veil** (rendering framework Fabric) — **version cible verrouillée à la release stable la plus récente compatible 1.21.1 au démarrage du projet**

**Après** :
> Rendering | **Veil 3.x** (rendering framework Fabric par FoundryMC) — **version exacte compatible Minecraft 1.21.1 verrouillée Phase 0**

### §10 Stratégie Iris

Pas de changement nécessaire — la stratégie Iris ne dépend pas de la version Veil.

### §3 (note importante)

**Ajouter après le tableau** :
> **Veil 3.x apporte des features critiques pour le pipeline shader backrooms** : Dynamic Buffers stabilisés (deferred-like), framebuffers JSON-driven avec MoLang, ShaderPreDefinitions avec recompile auto, ShaderFeatures pour graceful degradation des extensions GLSL. Ces features sont à confirmer dans `phase0_findings.md` puis exploitées par la spec shader V2 (compagnon).

### §28 Compatibilité mods

**Ajouter ligne pour Sodium** :
> Compat Sodium à valider sur la version Veil 3.x verrouillée en Phase 0 — risque résiduel sur Dynamic Buffers + batching Sodium.

### §29 Préambule LLM

**Ajouter règle** :
> **N'inventez aucune classe Veil.** Toute classe/méthode Veil utilisée doit correspondre à la doc officielle de la version verrouillée en Phase 0. Si la doc ne couvre pas le cas, signalez et demandez confirmation. Le wiki officiel `https://github.com/FoundryMC/Veil/wiki` est l'autorité.

---

*Fin du document. Spec_Shaders_Backrooms_V2.*
