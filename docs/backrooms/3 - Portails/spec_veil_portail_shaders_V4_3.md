# Backrooms Mod — Documentation Technique V4.3
## Mod Fabric 1.21.1 · Veil 3.x Rendering · Portails Illusionnistes

> **Note sur cette version V4.3** : mise à jour technique ciblée, **pas un round d'audit**. Vérifications faites contre la documentation officielle Veil (wiki FoundryMC) au cours de la rédaction de la spec compagnon `spec_shaders_backrooms_V2.md`. Les changements sont factuels, pas philosophiques :
> - **§3 Stack technique** : cible Veil **3.x compatible Minecraft 1.21.1** (FoundryMC). La V4.2 disait "release stable la plus récente compatible 1.21.1" sans préciser. La version exacte doit être documentée en Phase 0 dans `phase0_findings.md`.
> - **§3** : ajout d'une note sur les capacités Veil 3.x critiques exploitées par le pipeline shader (Dynamic Buffers, Framebuffers JSON-driven, ShaderPreDefinitions, ShaderFeatures pour graceful degradation). Renvoi explicite à la spec compagnon.
> - **§21 Phase 0.1** : précision "Veil 3.x compatible Minecraft 1.21.1" au lieu de "version Veil compatible 1.21.1" générique.
> - **§28 Compatibilité Sodium** : note précisant que la compat Sodium doit être confirmée sur la version Veil verrouillée en Phase 0.
> - **§29 Préambule LLM** : règle anti-hallucination ajoutée — le wiki officiel Veil (`https://github.com/FoundryMC/Veil/wiki`) est l'autorité, pas la mémoire du LLM.
> - **Compagnon shader** : référence ajoutée à `spec_shaders_backrooms_V2.md` qui détaille le Pilier 2 (système de profils qualité Ultra/Low/Minimal, overrides par level, effets baseline et optionnels, exploitation native des primitives Veil 3.x validées en Phase 0).
>
> Aucune autre section n'est modifiée. Architecture, contrats, lois de perception, machines d'état, scoring, sync frame-exact : inchangés depuis V4.2.
>
> **Note sur la version V4.2** (rappel) : ajustements ciblés issus d'une 3e relecture (audit ChatGPT round 2). Aucune refonte, uniquement des ajouts qui durcissent les zones où Codex risque de halluciner :
> - §10 : interdiction explicite de supposer un nom de package Iris (livrable du spike 0.5)
> - §15 : mini-section "Référence API Fabric cible" listant les noms de packages publics et stables uniquement
> - §15 : option de secours mixin `GameRenderer.render()` documentée avec avertissement conflit Sodium/Iris
> - §18.1 et §18.2 : pseudo-code illustratif des boucles principales, encadré "ne pas copier tel quel"
>
> **Note sur la version V4.1** (rappel) : révision mineure de la V4 corrigeant les incohérences résiduelles repérées en relecture (audit Codex et audit ChatGPT) :
> - Cohérence Iris alignée entre §1, §10 et MVP §21 (plus de tension entre "Veil uniquement" et mode WARN_ONLY)
> - §15 frame-exact : politique partialTick tranchée explicitement
> - §27 récursion : drapeau de récursion par-portail (pas global)
> - §18.6 aggro : priorité du `PortalLureGoal` motivée et configurable
> - Phase 0 : spike Sodium ajouté + budget temps indicatif par spike
> - §1 règle de rendu reformulée pour ne pas contredire WARN_ONLY
>
> **Note sur la version V4** (rappel) : par rapport à la V3, durcissement des zones identifiées comme fragiles pour une implémentation par LLM ou par développeur sans connaissance approfondie de Veil/Iris :
> - Scope multi-joueur explicitement tranché (single-player V1)
> - Stratégie Iris adoucie (toggle auto désactivé par défaut, fallback robuste, spike de validation requis)
> - Cible de version Veil verrouillée
> - Hooks Fabric/Veil exacts précisés pour la sync frame-exact
> - Incohérences distances portail corrigées
> - Récursion de portails et masquage couloir bouclé spécifiés
> - Comportement aggro cross-portail détaillé
> - 5e Loi de Perception ajoutée (défaillance silencieuse > artefact visible)
> - Phase 0 (spikes de validation) ajoutée avant la Phase 1
> - Section compatibilité mods ajoutée
> - Préambule pour LLM générant le code (§29)

---

## 1. Vue d'ensemble du projet

Ce mod Minecraft Fabric 1.21.1 implémente une expérience **Backrooms** reposant sur une **illusion sensorielle cohérente** plutôt que sur une vraie continuité physique inter-dimensionnelle.

L'approche centrale : **Veil + illusion de seamless**. On ne relie pas réellement deux espaces dans le moteur. On donne au joueur une impression très forte de continuité via du rendu distant, des téléportations masquées, un design spatial trompeur, des entités proxy et un système audio cross-space.

### Philosophie

Le genre backrooms repose sur la **perception**, la **confusion**, l'**ambiance**, les **faux repères** et les **incohérences spatiales ressenties**. Une illusion bien construite est plus efficace qu'un vrai portail physique pour ce type d'expérience.

### Scope V1 — Single-player uniquement

> **La V1 du mod cible exclusivement le single-player (monde local, intégré).**

Toutes les illusions (vue distante, écho d'entité, audio cross-space, aggro simulée) reposent sur la capacité du client à lire en direct l'état du monde destination. En multi-joueur (serveur dédié), ces lectures nécessiteraient un canal réseau custom de sync entre serveur et client pour pousser l'état des entités/sons à proximité des portails actifs — ce qui constitue un sous-système entier non couvert par cette spec.

- **V1** : single-player local. Le client est aussi le serveur intégré, accès direct à toutes les dimensions.
- **V2+** (hors scope ce doc) : éventuel support multi-joueur via un protocole `PortalSyncPacket` à concevoir séparément.
- **Comportement V1 sur serveur dédié** : le mod détecte qu'il tourne client-only sur un serveur sans le mod côté serveur → désactive proprement les portails illusionnistes, log un message clair, ne crash pas. Aucune feature backrooms n'est tentée dans ce mode.

### Séparation fondamentale

> **Veil = couche de rendu**
>
> **Portail = logique d'illusion**
>
> **Shader = ambiance et masquage**

Ces trois piliers sont strictement indépendants. On peut changer Veil, remplacer le shader source, ou modifier la logique de portails sans impacter les autres.

### Règle de rendu dimensionnel

> **Cible dans les Backrooms : Veil uniquement. Monde normal : shaders du joueur.**
> **Fallback acceptable : coexistence dégradée (mode WARN_ONLY) si le toggle Iris ne peut pas être réalisé proprement (voir §10).**

Les dimensions backrooms utilisent un pipeline visuel dédié. **Quand c'est techniquement possible** (mode `AUTO_TOGGLE` validé en Phase 0), Iris et les shaders externes sont désactivés dans ces dimensions pour garantir l'ambiance voulue et éviter les conflits de rendu. C'est à la fois une décision technique (pas de coexistence de deux systèmes de shaders) et artistique (chaque level a son identité visuelle imposée).

Quand le toggle Iris n'est pas disponible, le mod bascule en `WARN_ONLY` : Veil rend par-dessus Iris, l'ambiance visuelle backrooms est dégradée mais le mod ne crash pas et n'impose rien. Le joueur est informé.

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
| **Pas de support multi-joueur en V1** | L'illusion repose sur l'accès direct du client à l'état du monde destination, impossible sans protocole réseau custom (V2+) |
| Pas de collision inter-espace | Aucun lien physique entre les espaces — l'illusion est visuelle uniquement |
| Pas de raycast cross-space natif | Le moteur ne sait pas tracer un rayon à travers un portail illusionniste |
| Pas de projectile traversant | Un projectile ne peut pas passer nativement d'un espace à l'autre |
| Pas de pathfinding vanilla cross-seuil | L'IA de navigation Minecraft ne connaît pas les portails (un mécanisme custom est prévu pour l'aggro, voir §18.6) |
| Pas de récursion de portail | Pas de vue d'un portail à travers un autre portail (comportement défini en §27) |
| Pas de non-euclidien mathématiquement réel | L'illusion repose sur la duplication/redirection, pas sur une géométrie non-euclidienne |
| Pas de clone logique d'entités | Les entités proxy sont visuelles uniquement — une seule source d'autorité |
| Pas de garantie de compatibilité universelle | Conflits possibles avec d'autres mods de rendu, shaders, ou audio (voir §28) |
| Pas de coexistence Iris + Veil dans les Backrooms | Les shaders Iris sont désactivés dans les dimensions backrooms (si toggle Iris validé, voir §10) |

---

## 3. Stack technique

| Composant | Version / Technologie |
|---|---|
| Minecraft | 1.21.1 |
| Mod loader | Fabric |
| Fabric API | version compatible 1.21.1 |
| Rendering | **Veil 3.x** (rendering framework Fabric par FoundryMC) — **version exacte compatible Minecraft 1.21.1 verrouillée dans `phase0_findings.md`** |
| Shaders | Shader custom dérivé d'un shader open source, intégré via Veil |
| Audio | OpenAL EFX (natif Minecraft / LWJGL) |
| Compatibilité Iris | Soft-dependency : détection + désactivation **opt-in** dans les dimensions backrooms (voir §10) |
| Langage | Java 21 |

> **Important sur Veil** : Veil est un framework activement développé dont l'API peut évoluer entre versions majeures. Aucune classe Veil ne doit être importée hors de `backrooms/bridge/` (voir §10). Veil 3.x compatible Minecraft 1.21.1 — version exacte figée dans `phase0_findings.md` ; toute mise à jour Veil nécessite une revalidation de l'intégralité du Pilier 1. **Le wiki officiel `https://github.com/FoundryMC/Veil/wiki` est l'autorité unique** pour toute question sur l'API Veil — pas la mémoire d'un LLM, pas une recherche web générique.

> **Autorité packages** : en cas de conflit de packages avec une ancienne section héritée de V4.2, `ARCHITECTURE.md` v4 gagne.

> **Capacités Veil 3.x exploitées par le projet** : le pipeline shader des Backrooms repose intégralement sur les primitives natives de la version Veil verrouillée en Phase 0 — Post-Processing pipelines JSON-driven (`pinwheel/post/`), Framebuffers JSON-driven avec MoLang (`pinwheel/framebuffers/`), Dynamic Buffers (`albedo`/`normal`/`light_uv`/`light_color`) pour l'extraction émissive, ShaderPreDefinitions avec recompile auto pour les profils qualité, ShaderFeatures pour graceful degradation des extensions GLSL, événement officiel de pré-post-processing pour les uniforms par-pipeline. **Ces noms sont conceptuels et doivent être validés en Phase 0. Aucun import ou appel direct ne doit être généré depuis cette section.** **Aucune couche d'abstraction custom ne doit dupliquer ces capacités côté Java.** Ces choix sont détaillés dans la spec compagnon `spec_shaders_backrooms_V2.md`.

> **Compagnon — spec shader** : la spec `spec_shaders_backrooms_V2.md` détaille le Pilier 2 (Shader, ambiance et masquage) au niveau implémentation : système de profils qualité Ultra/Low/Minimal, overrides par level, effets baseline (5) et optionnels (6), schemas JSON par effet, anti-patterns Veil. Cette spec V4.3 reste la source de vérité pour l'architecture globale, les piliers, les contrats et la machine d'état des portails.

> **Important sur Iris** : Iris n'expose pas d'API publique stable et documentée pour le toggle runtime du shader pack. La stratégie de §10 doit être **validée par un spike avant la Phase 1** (voir Phase 0 dans §21). Si l'API requise n'existe pas ou est instable, le mod doit retomber sur le mode "warning au joueur" sans tenter de toggle automatique.

---

## 4. Règles de perception joueur

Ces règles sont le **contrat de design** de tout le projet. Elles guident chaque décision technique et chaque choix d'implémentation. Quand un dilemme technique se pose, ces règles tranchent.

### Les 5 lois de l'illusion

| # | Règle | Implication |
|---|---|---|
| 1 | **Ne jamais montrer un changement brutal dans le champ de vision** | Tout changement d'état (TP, swap de salle, apparition/disparition) doit être masqué ou progressif |
| 2 | **Ne jamais contredire une information visuelle immédiate** | Ce que le joueur voit dans le portail = ce qu'il trouvera après. Pas d'écart entre vue distante et réalité |
| 3 | **Toujours privilégier la cohérence locale sur la cohérence globale** | Le joueur ne doit jamais percevoir une incohérence dans son voisinage immédiat, même si la topologie globale est impossible |
| 4 | **Masquer les transitions par bruit sensoriel** | Visuel (distorsion, flicker) ou audio (son soudain, changement d'ambiance) — jamais de transition "à nu" |
| 5 | **Une défaillance silencieuse est toujours préférable à un artefact visible** | En cas de panne d'un sous-système (compilation shader, framebuffer, EFX, sync entité) → bruit, obscurité, fondu, dégradation gracieuse. Jamais de trou transparent, de pop d'entité, de saut de volume, de flicker non voulu |

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
| Rendu distant actif | `PortalViewSystem` | TransitionSystem, ShaderManager |
| État Iris (actif/désactivé) | `IrisBridge` | ShaderManager, VeilPostProcessingBridge |
| Métriques debug runtime | `DebugMetrics` | Debug HUD |

**Règle absolue** : si un système a besoin d'une donnée dont il n'est pas propriétaire, il la demande au propriétaire via une interface de lecture. Il ne la duplique pas, ne la cache pas, et ne la calcule pas lui-même.

---

## 6. Machine d'état des portails

Chaque `PortalLink` suit un cycle de vie strict. Les transitions ne sont pas libres : seuls les chemins définis ici sont autorisés.

### Distances de référence (configurables, voir `BackroomsConfig`)

| Constante | Valeur défaut | Rôle |
|---|---|---|
| `portalActivationDistance` | 16 blocs | INACTIVE → PREWARMING (préparation destination) |
| `portalVisibleDistance` | 10 blocs | PREWARMING → VISIBLE (affichage effectif vue distante, économie GPU) |
| `portalArmDistance` | 3 blocs | VISIBLE → ARMED (entrée zone seuil) |
| `teleportTriggerDepth` | 0.3 bloc | ARMED → TRANSITIONING (franchissement effectif) |

**Règle d'invariant** : `portalActivationDistance > portalVisibleDistance > portalArmDistance > teleportTriggerDepth`. Si la config viole cet invariant, `BackroomsConfig` log un warning et applique les valeurs par défaut.

```
                    ┌──────────────────────────────────┐
                    │                                  │
                    ▼                                  │
              ┌──────────┐                             │
              │ INACTIVE │  ← état initial             │
              └────┬─────┘                             │
                   │ joueur < portalActivationDistance │
                   ▼                                   │
             ┌───────────┐                             │
             │ PREWARMING│  ← préchargement destination│
             └─────┬─────┘    (chunks, framebuffer)    │
                   │ destination prête                  │
                   │ ET joueur < portalVisibleDistance  │
                   ▼                                   │
              ┌─────────┐                              │
              │ VISIBLE  │  ← vue distante rendue      │
              └────┬─────┘    audio cross-space actif   │
                   │ joueur < portalArmDistance         │
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

- `PREWARMING → INACTIVE` : joueur s'éloigne avant que la destination soit prête, OU joueur sort de `portalActivationDistance`
- `VISIBLE → PREWARMING` : joueur s'éloigne au-delà de `portalVisibleDistance` mais reste dans `portalActivationDistance` (économise le rendu sans relâcher le préchargement)
- `VISIBLE → INACTIVE` : joueur s'éloigne au-delà de `portalActivationDistance`
- `ARMED → VISIBLE` : joueur recule hors de la zone seuil sans franchir

**Transitions interdites** :

- Jamais de saut direct `INACTIVE → ARMED` ou `INACTIVE → TRANSITIONING`
- Jamais de `TRANSITIONING → VISIBLE` (le TP est irréversible une fois déclenché)

---

## 7. Budget performance

Ces limites sont les valeurs par défaut. Elles sont configurables via `BackroomsConfig` mais les valeurs ci-dessous sont les plafonds recommandés.

> **Avertissement coût réel** : ces valeurs sont des **estimations à valider par benchmark** lors de la Phase 0. Le rendu d'une vue distante = un rendu monde supplémentaire (même partiel). 2 vues à 50% résolution × 1 frame sur 2 = en moyenne 1 rendu monde supplémentaire par frame, à laquelle s'ajoute le post-FX backrooms. Sur GPU intégré ou config modeste, **le budget réel pourrait être de 1 seule vue active**. Le profileur GPU est un outil de Phase 2 obligatoire, pas optionnel.

| Ressource | Budget max | Notes |
|---|---|---|
| Vues distantes actives simultanées | **2** | Au-delà, impact GPU trop fort. À benchmarker en Phase 2. |
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
├── src/main/java/com/petassegang/addons/backrooms/
│   │
│   ├── config/                       ← PILIER 1 : Configuration
│   │   ├── BackroomsConfig.java
│   │   ├── RenderSystemConfig.java
│   │
│   ├── bridge/                       ← Abstraction Veil/Iris découpée
│   │   ├── Bridge.java
│   │   ├── BridgeState.java
│   │   ├── BridgeHealthMonitor.java
│   │   ├── VeilPostProcessingBridge.java
│   │   ├── VeilUniformBridge.java
│   │   ├── VeilDefinitionsBridge.java
│   │   ├── VeilFramebufferBridge.java
│   │   ├── VeilShaderBridge.java
│   │   ├── IrisBridge.java
│   │   └── IrisStateStore.java
│   │
│   ├── shader/                       ← PILIER 2 : Shaders custom, détail dans spec_shaders_backrooms_V2.md
│   │   ├── ShaderManager.java
│   │   ├── ShaderEventAdapter.java
│   │   ├── uniform/
│   │   ├── definitions/
│   │   ├── profile/
│   │   ├── effect/
│   │   ├── level/
│   │   └── health/
│   │
│   │   # ShaderEventAdapter appelle uniquement les bridges backrooms/bridge/.
│   │   # Il ne doit jamais importer Veil directement.
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

### Abstraction Veil/Iris — liste canonique des bridges

`backrooms/bridge/` contient la couche d'isolation complète. Toute API Veil/Iris réelle est confinée ici.

#### Infrastructure commune

```java
/**
 * Contract common to all optional external bridges.
 */
public interface Bridge {
    BridgeState state();
    void initialize();
    void shutdown();
}

public enum BridgeState {
    OK,
    DEGRADED,
    FAILED
}

/**
 * Source of truth for bridge health and degradation decisions.
 */
public class BridgeHealthMonitor {
    BridgeState stateOf(String bridgeId);
    void report(String bridgeId, BridgeState state, String reason);
}
```

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

#### `VeilPostProcessingBridge.java`

```java
/**
 * Abstraction over Veil post-processing pipelines.
 * Activates, disables, and queries Backrooms post-processing pipelines.
 */
public class VeilPostProcessingBridge {
    void activatePipeline(ResourceLocation id);
    void deactivatePipeline(ResourceLocation id);
    boolean isPipelineAvailable(ResourceLocation id);
    void runAfterPostProcessing(Runnable task);
    int getActivePostProcessCount();
}
```

#### `VeilUniformBridge.java`

```java
/**
 * Abstraction over Veil uniforms.
 * Pushes per-frame shader values after Phase 0 confirms the locked API.
 */
public class VeilUniformBridge {
    void setFloat(ResourceLocation pipelineId, String uniformName, float value);
    void setVec3(ResourceLocation pipelineId, String uniformName, float x, float y, float z);
    void setUbo(ResourceLocation bufferId, ByteBuffer data);
}
```

#### `VeilDefinitionsBridge.java`

```java
/**
 * Abstraction over Veil shader definitions / preprocessor defines.
 * Exact API calls are validated in Phase 0.
 */
public class VeilDefinitionsBridge {
    void enableDefinition(String name);
    void disableDefinition(String name);
    boolean isDefinitionEnabled(String name);
}
```

#### `VeilShaderBridge.java`

```java
/**
 * Abstraction over Veil's shader system.
 * Registers custom shader programs and manages their lifecycle.
 */
public class VeilShaderBridge {
    void registerShaderProgram(ResourceLocation id, ShaderProgram program);
    void unregisterShaderProgram(ResourceLocation id);
    boolean isShaderAvailable(ResourceLocation id);
}
```

#### `IrisBridge.java` et `IrisStateStore.java`

```java
/**
 * Soft-dependency bridge for Iris detection and optional dimension switching.
 */
public class IrisBridge {
    boolean isIrisLoaded();
    IrisMode mode();
    void enterBackroomsDimension();
    void leaveBackroomsDimension();
}

/**
 * Persists Iris state so it can be restored after crash/restart.
 */
public class IrisStateStore {
    void saveCurrentState();
    void restoreIfNeeded();
    void clear();
}
```

**Règle** : aucune classe hors de `backrooms/bridge/` n'importe directement une classe Veil ou Iris. Tout passe par ces bridges.

---

### Stratégie Iris / Veil — Dimension Switching

#### Avertissement préalable — fragilité de l'API Iris

> **Iris n'expose pas (à ce jour) d'API publique stable et documentée pour activer/désactiver le shader pack à la volée depuis un autre mod.** Toute tentative de toggle programmatique repose sur l'inspection des classes internes ou de méthodes semi-publiques susceptibles de changer entre versions Iris.
>
> En conséquence, le toggle automatique d'Iris est traité comme un **feature opt-in désactivé par défaut** (`enableIrisAutoToggle = false` dans `BackroomsConfig`). Le mode par défaut est : **avertir le joueur**, pas toggler.
>
> L'activation du toggle automatique est conditionnée à un **spike de validation** réalisé en Phase 0 (voir §21). Si ce spike échoue, le toggle automatique reste désactivé en production et l'option utilisateur est masquée.

#### Modes de fonctionnement Iris

Le mod fonctionne dans l'un des trois modes suivants, déterminés au démarrage :

| Mode | Conditions | Comportement à l'entrée Backrooms |
|---|---|---|
| **WARN_ONLY** (défaut) | Iris détecté, ou toggle non validé en Phase 0 | Message chat invitant le joueur à désactiver Iris manuellement. Veil se contente de rendre par-dessus, qualité visuelle non garantie mais pas de crash. |
| **AUTO_TOGGLE** | Iris détecté + API toggle validée en Phase 0 + `enableIrisAutoToggle = true` | Désactivation automatique du shader pack Iris à l'entrée, restauration à la sortie |
| **NO_IRIS** | Iris absent | IrisBridge inactif, rien à faire |

Le mode actif est exposé dans le HUD debug (§17) et logué au démarrage.

#### Pipeline de rendu selon le mode

| Contexte | Pipeline de rendu |
|---|---|
| Monde normal (overworld, nether, end, etc.) | Shaders du joueur (Iris) autorisés normalement |
| Dimensions backrooms — mode `AUTO_TOGGLE` | Veil uniquement, Iris désactivé proprement |
| Dimensions backrooms — mode `WARN_ONLY` | Veil + Iris coexistent (résultat dégradé mais fonctionnel), avertissement joueur |
| Dimensions backrooms — mode `NO_IRIS` | Veil uniquement |

#### Pourquoi viser le toggle (quand c'est possible)

- **Technique** : deux systèmes de shaders actifs simultanément = artefacts visuels, conflits de framebuffer, blending incohérent
- **Artistique** : chaque level backrooms a une identité visuelle précise (teinte jaunâtre Level 0, obscurité Level 3, bleu aquatique Poolrooms) que les shaders du joueur casseraient
- **Fiabilité** : le rendu est prévisible et testable quand on contrôle tout le pipeline

Mais aucun de ces points ne justifie un crash ou une dépendance fragile : si le toggle ne marche pas, le `WARN_ONLY` est une dégradation acceptable.

#### `IrisBridge.java`

```java
/**
 * Soft-dependency bridge to Iris.
 * Detects Iris presence at runtime and manages shader pack toggling
 * when the player enters/exits backrooms dimensions.
 *
 * Iris is a SOFT dependency: if Iris is absent, this class does nothing.
 * If Iris is present but its toggle API is incompatible OR untested,
 * the bridge defaults to WARN_ONLY mode (no auto-toggle, just inform the player).
 *
 * Source of Truth for: current Iris state (active/disabled) and current mode.
 *
 * IMPORTANT: aucun import direct de classe Iris hors de cette classe.
 * Tout accès se fait par reflection ou via un éventuel facade compat fournie par Iris.
 */
public class IrisBridge {

    enum Mode {
        NO_IRIS,        // Iris absent
        WARN_ONLY,      // Iris présent mais toggle non utilisé (défaut)
        AUTO_TOGGLE     // Iris présent + toggle validé + opt-in joueur
    }

    // --- Detection (au démarrage) ---
    boolean isIrisPresent();
    boolean isIrisToggleApiValidated();   // résultat du spike Phase 0
    Mode getCurrentMode();

    // --- State ---
    boolean areIrisShadersCurrentlyActive();

    // --- Dimension switching (no-op si mode != AUTO_TOGGLE) ---

    /**
     * Called when the player enters a backrooms dimension.
     * - Mode AUTO_TOGGLE : sauvegarde config Iris (mémoire + disque), désactive shader pack
     * - Mode WARN_ONLY  : envoie un message chat au joueur (1x par session)
     * - Mode NO_IRIS    : no-op
     */
    void onEnterBackrooms();

    /**
     * Called when the player exits a backrooms dimension.
     * - Mode AUTO_TOGGLE : restaure config Iris depuis sauvegarde
     * - Autres modes    : no-op
     */
    void onExitBackrooms();

    // --- Persistence ---

    /**
     * Sauvegarde la config Iris originale sur disque (fichier dédié dans
     * config/backrooms/iris_state.json) AVANT toute modification.
     *
     * Permet la restauration au prochain lancement si le joueur crash
     * pendant qu'il est dans les Backrooms (sinon Iris resterait désactivé
     * de façon persistante).
     */
    void persistOriginalIrisConfig();

    /**
     * Au démarrage du mod, vérifie si un fichier iris_state.json existe.
     * Si oui : restaure la config Iris d'origine et supprime le fichier.
     * (Reprise après crash en Backrooms)
     */
    void restoreIrisConfigOnStartup();

    // --- Fallback ---

    /**
     * Appelé si le toggle programmatique échoue à l'exécution alors que
     * le mode AUTO_TOGGLE était actif (régression Iris, etc.) :
     * - Bascule en WARN_ONLY pour le reste de la session
     * - Log warning détaillé
     * - Message chat au joueur
     * - Pas de crash
     */
    void handleRuntimeFallback(String reason);
}
```

#### Pipeline de dimension switching (mode AUTO_TOGGLE uniquement)

```
Joueur dans le monde normal (Iris actif, ses shaders)
  │
  │ → Entrée dans une dimension backrooms:*
  │
  ▼
IrisBridge.onEnterBackrooms()
  → persistOriginalIrisConfig() (mémoire + disque)
  → Désactivation du shader pack Iris (via API validée)
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
  → Suppression du fichier iris_state.json
  → Réactivation du shader pack Iris du joueur
  → Transition visuelle (fondu bref)
```

#### Points d'attention

- **Détection Iris** : via `FabricLoader.getInstance().isModLoaded("iris")` — jamais d'import direct de classes Iris hors de `IrisBridge`
- **API Iris** : tout accès via reflection encapsulée. Si la signature attendue n'existe pas → bascule WARN_ONLY immédiatement
- **Timing du switch** : le changement de pipeline doit être masqué par un fondu/transition, cohérent avec les Règles de Perception (§4)
- **Persistance** : sauvegarde sur disque AVANT toute modification, restauration au prochain démarrage si fichier présent. Garantie : un crash en Backrooms ne laisse jamais Iris désactivé de façon persistante.
- **Pas de force-disable permanent** : le mod ne modifie jamais la config Iris du joueur sur disque, seulement le state runtime
- **Concurrence multi-instance** : si plusieurs instances de MC tournent en parallèle, le fichier `iris_state.json` peut être en conflit. La V1 ne gère pas ce cas (single-instance assumée). Documenter dans le README.

#### Stratégie reflection Iris — règle anti-hallucination

> **CRITIQUE pour Codex/agent codegen** : la spec **n'indique volontairement aucun nom de package, classe ou méthode Iris cible**. Toute supposition (du type `net.irisshaders.iris.api.*`, `IrisApi.getInstance()`, `setShaderPackEnabled()`, etc.) est une **hallucination interdite**.
>
> Le nom du package, de la classe et de la méthode utilisés pour le toggle Iris sont des **livrables explicites du spike 0.5** (voir §21). Avant ce spike, ces noms n'existent pas dans la spec et ne doivent pas exister dans le code.
>
> Si un agent codegen doit produire `IrisBridge.java` avant la fin du spike 0.5, il doit :
> - Implémenter uniquement la détection (`FabricLoader.isModLoaded("iris")`) et la logique de mode (NO_IRIS / WARN_ONLY)
> - Laisser les méthodes de toggle effectif (désactivation/réactivation du shader pack) en `throw new UnsupportedOperationException("To be implemented after spike 0.5")` avec un commentaire `// SPEC-GAP §10: target API to be identified by spike 0.5`
> - Ne **jamais** inventer un nom de classe Iris pour "faire compiler"

#### Livrables attendus du spike 0.5

À l'issue du spike 0.5, `phase0_findings.md` doit contenir au minimum :
- Nom complet (FQCN) de la classe Iris cible
- Signature exacte de la méthode utilisée (paramètres, type de retour)
- Visibilité de cette méthode (`public`, `protected via accessor`, etc.)
- Versions Iris testées avec succès
- Estimation de stabilité (méthode officielle, accessor, internal API ?)
- Plan B si l'API change : quelle version d'Iris on supporte, à partir de quand on bascule WARN_ONLY

Sans ces données, `IrisBridge.handleRuntimeFallback()` est **incomplet** et le mode `AUTO_TOGGLE` doit rester non disponible.

#### Présentation utilisateur

Le mod peut présenter cette contrainte comme une feature, mais en restant honnête sur la mécanique :

> *"Les Backrooms utilisent un pipeline visuel dédié pour garantir l'ambiance voulue. Si vous utilisez Iris, le mod peut désactiver temporairement votre shader pack à l'entrée et le restaurer à la sortie (option à activer dans les paramètres)."*

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
    boolean enableIrisAutoToggle = false;   // Désactivé par défaut. Activable seulement
                                            // si IrisBridge.isIrisToggleApiValidated() == true.
                                            // Sinon l'option est masquée dans le menu.
    float irisSwitchFadeDuration = 0.3f;    // durée du fondu au changement (secondes)
    boolean irisShowWarnOnEnter = true;     // affiche un message chat à l'entrée Backrooms
                                            // si Iris actif et toggle non utilisé

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

### Contrat Lifecycle

Les managers et features runtime de cette spec (`BackroomsFeature`, `ShaderManager`, `PortalManager`, `PortalViewSystem`, `CrossSpaceAudioSystem`, etc.) implémentent l'interface `Lifecycle` définie dans `core/lifecycle/Lifecycle.java` par `ARCHITECTURE.md` v4. Les allocations GL et ressources externes se font dans `init()`/`start()`, jamais dans les constructeurs, et les libérations sont idempotentes dans `stop()`.

---

## 11. PILIER 2 — Shaders Custom

### Objectif

Gérer le shader custom **dérivé d'un shader open source**, ainsi que tous les effets de post-processing propres au mod. Ce pilier communique avec le Pilier 1 (`VeilShaderBridge`, `VeilPostProcessingBridge`, `VeilUniformBridge`, `VeilDefinitionsBridge`) pour s'injecter dans la pipeline de rendu, mais ne connaît rien du système de portails.

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
public class ShaderManager implements Lifecycle {
    void initialize();
    void reload();
    ShaderProgram get(ResourceLocation id);
    void applyUniforms(ShaderProgram program, UniformContext ctx);
    boolean isOperational();              // false si compilation a échoué
    void cleanup();
}
```

### Pipeline shader Veil natif

```
Pipeline de rendu logique (les stages post-process sont déclarées en JSON Veil, voir `spec_shaders_backrooms_V2.md`) :
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
public class PortalManager implements Lifecycle {

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

### Poids de référence (point de départ, à tuner empiriquement)

```
total = 0.30 * distanceScore        // proche compte beaucoup mais pas tout
      + 0.25 * screenCoverageScore  // surface visible importante
      + 0.20 * facingScore          // direction du regard
      + 0.15 * importanceScore      // intention de design
      + 0.10 * stateBonus           // continuité (ARMED, TRANSITIONING)

Cas particuliers (overrides) :
  - state == TRANSITIONING        → total = +∞ (priorité absolue, jamais droppé)
  - screenCoverageScore == 0      → total = 0 (sauf si state >= ARMED)
  - distance > portalActivationDistance → portail filtré avant scoring
```

Ces poids sont une **base**. Ils sont exposés dans `BackroomsConfig` pour permettre du tuning sans recompilation. Toute modification doit être validée par les scénarios de test §22 (notamment "5 portails dans le même couloir").

### Règles de sélection

1. Filtrer les portails au-delà de `portalActivationDistance`
2. Scorer les portails restants
3. Éliminer ceux avec `screenCoverageScore == 0` **sauf s'ils sont en ARMED ou TRANSITIONING**
4. Trier par score total décroissant
5. Prendre les N premiers (`maxActivePortalViews`)
6. **Jamais dropper** un portail en `TRANSITIONING` — il a toujours la priorité absolue, même si le joueur tourne le dos en cours de TP

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

### Découplage tick/frame dans Minecraft (rappel)

Minecraft a deux boucles distinctes :

- **Game tick** : 20 TPS (50ms), gère la logique (physique, IA, inputs consommés, état monde)
- **Render loop** : FPS variable, gère le rendu (caméra interpolée, frames affichées entre deux ticks)

Entre deux ticks, le renderer interpole la position du joueur via le `partialTick` (0.0 → 1.0). Un TP exécuté au mauvais endroit dans cette interpolation produit le jitter mentionné.

### Règle de synchronisation

```
Le TP doit se produire :
  → APRÈS le rendu du frame courant (le joueur voit encore l'ancien monde)
  → AVANT le prochain calcul de caméra (la prochaine frame utilise la nouvelle position)
  → IDÉALEMENT en phase avec un game tick (pour éviter une interpolation cassée)

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

### Hooks Fabric/Veil à viser

> **À VALIDER en Phase 0** : les noms ci-dessous sont les candidats les plus probables au moment de l'écriture de cette spec. Tous doivent être confirmés contre l'API Fabric 1.21.1 et Veil cible.

| Phase frame | Hook candidat | Usage |
|---|---|---|
| Détection franchissement | `ClientTickEvents.END_CLIENT_TICK` | Évalue la position joueur, décide ARMED → TRANSITIONING |
| Préparation effet masquage | `WorldRenderEvents.START` ou `BEFORE_ENTITIES` | Active le shader de masquage avant le rendu |
| Rendu monde courant | (laissé à Veil) | Le frame N affiche le shader de masquage par-dessus |
| Exécution TP | `WorldRenderEvents.END` ou `VeilPostProcessingBridge.runAfterPostProcessing()` | TP exécuté après rendu+post-FX du frame courant |
| Caméra frame suivante | (next `Camera.update`) | Utilise automatiquement la nouvelle position |

**Risque connu** : `WorldRenderEvents.END` se déclenche avant la composition finale Veil. Il peut être nécessaire de hooker plus tard via `VeilPostProcessingBridge`. Le spike Phase 0 doit identifier le point d'injection effectif zero-mismatch.

### Référence API Fabric (versions publiques stables 1.21.1)

> Ces packages sont **stables et publics** dans Fabric API 1.21.1. Codex peut les utiliser sans hallucination. Les autres packages (Veil, Iris) restent à valider Phase 0 et **ne sont pas listés ici par construction**.

| API | Package complet (FQCN) | Usage dans le mod |
|---|---|---|
| Tick client | `net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents` | Détection franchissement, scoring portails |
| Render world | `net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents` | Hooks rendu monde (START, BEFORE_ENTITIES, AFTER_ENTITIES, END) |
| Hud render | `net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback` | Debug HUD §17 |
| Resource reload | `net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener` | Hot-reload shaders en debug |

> Les noms ci-dessus sont fournis pour empêcher l'invention d'API. Si une de ces API évolue dans Fabric, mettre à jour cette table avant de coder.

### Option de secours : mixin direct sur `GameRenderer.render()`

Si **aucun des hooks Fabric/Veil candidats** n'arrive à produire un TP zero-mismatch en spike 0.6, l'option de secours est un **mixin custom sur `net.minecraft.client.renderer.GameRenderer.render(...)`**, en injection à la fin de la méthode (après le rendu monde et le post-process Veil).

**Caveats à connaître avant de retenir cette option** :
- `GameRenderer.render()` est un point chaud touché par **beaucoup** d'autres mods (Sodium, Iris, mods de rendu, mods de caméra, Replay Mod, etc.). Risque de conflit mixin réel.
- L'injection en fin de méthode doit utiliser `@Inject(at = @At("RETURN"))` sans modifier l'état local du `GameRenderer`.
- Cette voie sort de l'isolation `backrooms/bridge/` : c'est explicitement assumé comme **dette technique en attendant une API Veil stable**.
- Tester en présence de Sodium en Phase 0 (spike 0.8) avant de valider cette option.

Cette option est **un fallback documenté, pas une solution recommandée**. Elle existe pour garantir que le projet ne soit pas bloqué par l'absence de hook Veil approprié, mais le spike 0.6 doit d'abord épuiser les options non-mixin.

### Conditions d'exécution sécurisées

Le TP doit :
- S'exécuter sur le **thread principal** (le client thread Minecraft) — jamais sur un thread de rendu, jamais sur un thread custom
- Être **idempotent** dans la même frame (si le hook se déclenche 2x, le TP ne se fait qu'une)
- Capturer la **vélocité, l'orientation et le partialTick** au moment de la décision (état ARMED → TRANSITIONING)

### Politique partialTick

Une subtilité existe entre l'instant de capture (passage en TRANSITIONING) et l'instant d'exécution (fin du frame courant) : le `partialTick` aura évolué entre les deux.

**Règle tranchée** :
- Le `partialTick` **capturé** sert à calculer la **position visuelle de référence** pour aligner le masquage shader (l'effet doit "coller" à là où le joueur croyait être au moment du franchissement).
- Le `partialTick` **courant au moment de l'exécution** sert à calculer la **position physique d'arrivée** post-TP (cohérence avec le rendu de la frame N+1).
- En cas de doute ou d'écart > 0.5 entre les deux : utiliser le `partialTick` courant. Logger le cas en debug pour inspection.

Cette règle évite à la fois le jitter visuel (Loi 1 §4) et la dérive de position (Loi 2 §4).

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
 * Implementation: hook validé en Phase 0. Candidat principal :
 * VeilPostProcessingBridge.runAfterPostProcessing() ou un mixin custom
 * sur GameRenderer.render() en fin de pipeline.
 *
 * Garanties :
 * - Exécution sur le thread principal uniquement
 * - Idempotence dans la frame (drapeau interne consommé une fois)
 * - État capture au moment du scheduling, pas au moment de l'exécution
 */
public class FrameSyncPolicy {
    /**
     * Capture l'état joueur (position, vélocité, orientation, partialTick)
     * et programme le TP pour la fin du frame courant.
     */
    void scheduleTeleport(TeleportExecutor executor, TransitionContext ctx);

    boolean isTeleportPending();

    /**
     * Appelé par le hook de fin de frame.
     * Exécute le TP si pending, puis consomme le flag.
     * Thread-safe par construction (assertion thread principal).
     */
    void executePendingIfAny();
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

#### Squelette de la méthode principale (PSEUDO-CODE)

> **⚠️ PSEUDO-CODE ILLUSTRATIF.** Ce squelette montre la **structure logique** attendue, pas l'API exacte à utiliser. Les noms `renderWorldFromCamera()`, `framebuffer.bind()`, etc., correspondent à des **opérations à mapper** sur l'API réelle de Veil/MC une fois validée en Phase 0. **Ne pas copier-coller tel quel.** Chaque appel à une méthode Veil ou MC doit être validé contre la signature réelle.

```java
public void render(List<PortalLink> renderedPortals, PlayerState player) {
    // Reset défensif du drapeau de récursion (§27)
    renderingPortalDepth = 0;

    for (PortalLink portal : renderedPortals) {
        // 1. Vérifier que la caméra n'est pas dans un cas extrême (§14)
        GuardResult guard = cameraGuard.evaluate(player.camera(), portal, config);

        if (guard == GuardResult.FALLBACK) {
            surfaceRenderer.renderFallback(portal);  // bruit/flou
            continue;
        }

        // 2. Calculer la caméra distante
        CameraState remoteCam = (guard == GuardResult.CLAMP_APPLIED)
            ? cameraGuard.applyCorrectedCamera(player.camera(), portal)
            : remoteCamera.computeRemoteCamera(player, portal);

        // 3. Rendu de la scène distante dans un framebuffer dédié
        //    (avec gestion récursion par-frame, §27)
        PortalFramebuffer fb = portal.framebuffer();
        renderingPortalDepth++;
        try {
            fb.bind();
            renderWorldFromCamera(remoteCam);  // ← API à mapper Phase 0
            fb.unbind();
        } finally {
            renderingPortalDepth--;  // garantit reset même en cas d'exception
        }

        // 4. Plaquer la texture sur la surface du portail
        surfaceRenderer.render(portal, fb);
    }
}
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

#### Squelette de la méthode principale (PSEUDO-CODE)

> **⚠️ PSEUDO-CODE ILLUSTRATIF.** Montre la structure d'orchestration attendue, pas l'API exacte. La méthode `handleStateTransition()` encapsule la logique de la machine d'état §6 (transitions autorisées/interdites). À implémenter en respectant l'invariant : seules les transitions listées en §6 sont valides.

```java
/**
 * Appelé chaque tick par PortalManager (§12).
 * Met à jour l'état de chaque portail actif et déclenche les actions associées.
 */
public void update(List<PortalLink> activePortals, PlayerState player, long currentTick) {
    for (PortalLink portal : activePortals) {
        // 1. Évaluer la position du joueur par rapport au portail
        ThresholdEvent event = thresholdDetector.evaluate(player, portal);

        // 2. Déterminer la transition d'état applicable (§6)
        PortalState current = portal.state();
        PortalState next = stateMachine.computeNextState(current, event, portal, currentTick);

        if (next != current) {
            handleStateTransition(portal, current, next, player, currentTick);
        }

        // 3. Si en TRANSITIONING : déclencher la planification du TP frame-exact (§15)
        if (portal.state() == PortalState.TRANSITIONING && !frameSync.isTeleportPending()) {
            TransitionContext ctx = buildTransitionContext(portal, player);
            transitionMasker.activate(portal, ctx);
            frameSync.scheduleTeleport(teleportExecutor, ctx);
        }
    }
}

/**
 * Effectue les actions de bord associées à une transition d'état.
 * Centralise les side-effects pour faciliter le debug (un seul point d'entrée).
 */
private void handleStateTransition(PortalLink portal, PortalState from, PortalState to,
                                    PlayerState player, long tick) {
    // Validation : la transition from→to doit être autorisée par §6
    if (!stateMachine.isValidTransition(from, to)) {
        log.warn("Invalid state transition rejected: {} → {} on portal {}", from, to, portal.id());
        return;
    }

    portal.setState(to);
    portal.setLastStateChangeTimestamp(tick);

    // Side-effects par transition (extrait — la liste complète suit §6)
    switch (to) {
        case PREWARMING -> destinationLoader.requestPrepare(portal);
        case VISIBLE    -> { /* PortalViewSystem prendra le relais au prochain frame */ }
        case ARMED      -> transitionMasker.prepare(portal);
        case COOLDOWN   -> transitionMasker.fadeOut(portal);
        case INACTIVE   -> destinationLoader.releaseIfNotShared(portal);
        // TRANSITIONING : géré dans update() ci-dessus
    }
}
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

**Problème de visibilité longue distance** : si le joueur a une render distance vanilla de 12 chunks, dans un long couloir bouclé il pourrait voir au loin le même bout de couloir → l'illusion de boucle se casse instantanément.

**Contre-mesures obligatoires** (à appliquer combinées) :

1. **Brouillard imposé dans les niveaux backrooms** : `LevelTheme` définit un `fogStartDistance` et `fogEndDistance` agressifs (typiquement fog complet à 20-32 blocs). Géré via les uniforms shader (`vignette.fsh` étendu ou shader fog dédié).
2. **Masquage géométrique** : le segment de couloir est conçu avec un coude, un changement d'éclairage, ou un obstacle visuel à mi-longueur. Le portail invisible est placé dans la zone non-visible directement depuis l'entrée du segment.
3. **Variation visuelle entre instances** : `RoomVariantRegistry` peut servir des variantes légèrement différentes du même segment de couloir, ce qui rend la duplication moins flagrante même si visible partiellement.

```java
public class LoopingCorridorManager {
    /**
     * Vérifie qu'un segment de couloir respecte les contraintes anti-vision-longue.
     * Appelé à l'enregistrement des segments dans le RoomGraph.
     */
    boolean validateCorridorSegment(RoomSegment segment, LevelTheme theme);

    /**
     * Place le portail invisible de bouclage dans le segment, en respectant
     * les zones de masquage géométrique définies dans le segment.
     */
    PortalLink installLoopPortal(RoomSegment segment);
}
```

---

### 18.4 EntityEchoSystem — Entités proxy

**Rôle** : Afficher des entités de la zone distante dans la vue du portail.

> **Scope V1** : ce système suppose un accès direct au monde destination (single-player local). En multi-joueur, il devrait passer par un protocole réseau custom (hors scope V1, voir §1).

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

> **Scope V1** : single-player. Ce système accède à la fois aux entités du monde destination (lecture + envoi stimuli) et au monde joueur (téléportation cross-zone).

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

#### Mécanisme de détection (côté entité réelle)

L'IA vanilla ne sait pas qu'un bloc-porte est un portail. Pour qu'une entité de l'autre côté "perçoive" le joueur, le mécanisme est :

1. `AggroSimulationSystem.tick()` énumère les portails en état >= VISIBLE
2. Pour chaque portail, énumère les entités hostiles côté destination dans un rayon `aggroDetectionRadius` (défaut : 12 blocs autour de l'endpoint)
3. Calcule un stimulus selon le type (VISUAL/AUDIO/PROXIMITY)
4. Le stimulus est appliqué via une **entrée custom dans le `Brain` de l'entité** (Memory module dédié au mod, ex: `BackroomsMod.PORTAL_LURE_TARGET`) qui contient :
   - position du seuil destination (point d'attraction)
   - intensité du stimulus
   - timestamp
5. Un **goal custom `PortalLureGoal`** est ajouté au `GoalSelector` de tous les `Mob` déclarés "backrooms-aware". Ce goal :
   - Lit le memory module
   - Si stimulus présent et > seuil → l'entité pathfind vers la position du seuil
   - Quand l'entité atteint le seuil → déclenche `crossPortal()` (cf. ci-dessous)

#### Priorité du `PortalLureGoal`

> **Attention au choix de la valeur de priorité.** Dans Minecraft vanilla, les `MeleeAttackGoal` et `NearestAttackableTargetGoal` utilisent souvent des priorités basses (1-3). Une valeur mal choisie produit des conflits comportementaux silencieux : l'entité ignore le portail parce qu'elle attaque déjà autre chose, ou inversement abandonne sa cible courante pour aller vers le portail.

**Règle V1** : `PortalLureGoal` est inséré avec une **priorité juste supérieure** à celle du goal de wandering/idle de l'entité, mais **strictement inférieure** à tout goal de combat actif (target, attack, flee). Concrètement, la valeur dépend de l'entité concernée et est donnée par sa déclaration dans le registre :

```java
public class BackroomsAwareEntity {
    int idleGoalPriority;        // priorité du WanderGoal de l'entité (lu)
    int combatGoalPriority;      // priorité du AttackGoal de l'entité (lu)
    int portalLureGoalPriority;  // calculé : idleGoalPriority - 1, doit rester > combatGoalPriority
}
```

Si l'entité n'expose pas ces valeurs (mob custom mal déclaré), `PortalLureGoal` n'est **pas** inséré, et un warning est logué. Pas de valeur magique par défaut.

#### Poursuite cross-portail

1. Entité (réelle) atteint le seuil côté destination via `PortalLureGoal`
2. `crossPortal(entity, link)` :
   - Téléporte l'entité dans la dimension du joueur, à la position miroir du seuil source
   - Préserve vélocité, orientation, état de combat
   - Retire le memory module `PORTAL_LURE_TARGET`
3. L'entité devient une entité réelle côté joueur → poursuite normale via les goals vanilla (target sur joueur)

#### Limites assumées

- Seules les entités déclarées "backrooms-aware" reçoivent `PortalLureGoal`. Les mobs vanilla (zombies, etc.) ignorent les portails.
- Le pathfinding vers le seuil utilise le `PathNavigation` vanilla. Si la géométrie côté destination empêche d'atteindre le seuil → l'entité reste bloquée et n'arrive jamais. C'est acceptable.
- Si l'entité est tuée pendant le pathfinding → le memory module est nettoyé naturellement par sa mort.

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
| **Dépendances** | VeilFramebufferBridge, VeilPostProcessingBridge, RemoteCameraGuard |
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
- Gestion Iris : modes `NO_IRIS` et `WARN_ONLY` obligatoires (le mod doit fonctionner avec Iris présent sans crash). Mode `AUTO_TOGGLE` **uniquement si validé en Phase 0** ; sinon l'option reste masquée.

**Le MVP doit être jouable et convaincant avant de passer à la suite.**

### Phase 0 — Spikes de validation API (avant tout code de production)

> **Cette phase n'est PAS optionnelle.** Elle valide les hypothèses techniques sur lesquelles repose toute la spec. Sans ces spikes, le risque de découvrir une impasse en Phase 3 ou 4 est réel.

> **Budget temps** : chaque spike a un budget indicatif. Si le critère de succès n'est pas atteint dans ~2x ce budget, l'option de fallback documentée dans la colonne "critère" doit être adoptée. Pas d'enlisement.

| # | Spike | Budget indicatif | Critère de succès |
|---|---|---|---|
| 0.1 | Vérifier la disponibilité de **Veil 3.x compatible Minecraft 1.21.1** pour Fabric 1.21.1 et verrouiller la version exacte dans `build.gradle` + `phase0_findings.md`. Coordonnées : Maven `https://maven.blamejared.com/`, artefact `foundry.veil:veil-fabric-1.21.1:<version>`. | 1h | Build qui compile et lance MC avec Veil chargé. Si aucune version Veil 3.x compatible n'est disponible → documenter et revalider la spec compagnon shader avant de choisir une alternative. |
| 0.2 | Spike Veil framebuffer : créer un framebuffer offscreen, le remplir d'une couleur, l'afficher sur un quad dans le monde | 0.5j | Quad coloré visible en jeu. Si échec → revoir le choix de Veil avant Phase 1. |
| 0.3 | Spike Veil post-process : injecter un post-process minimal (vignette) et vérifier qu'il s'applique | 0.5j | Vignette visible en jeu |
| 0.4 | Spike Iris détection : `FabricLoader.isModLoaded("iris")` retourne le bon résultat avec/sans Iris | 1h | Log correct au démarrage |
| 0.5 | **Spike Iris toggle (CRITIQUE)** : tester si l'API publique/semi-publique d'Iris permet d'activer/désactiver le shader pack à la volée. Documenter la méthode utilisée et sa stabilité. | 1j | Soit : toggle fonctionne → mode `AUTO_TOGGLE` activable. Soit : toggle ne fonctionne pas → mode `WARN_ONLY` permanent + option masquée. **Pas plus de 2 jours sur ce spike** : si l'API n'est pas trouvée, on accepte WARN_ONLY définitivement. |
| 0.6 | Spike sync frame-exact : tester les hooks Fabric/Veil candidats (§15) avec un TP de test entre deux positions à chaque pression de touche. Mesurer le mismatch visuel. | 1j | Identifier le hook qui produit zéro frame de mismatch. Si aucun candidat ne donne 0 mismatch → documenter le hook au mismatch minimal et adopter un masquage shader légèrement plus long pour compenser. |
| 0.7 | Spike OpenAL EFX : créer un low-pass filter et un reverb effect, les appliquer à un son test | 0.5j | Son filtré audible en jeu. Si EFX indisponible sur la plateforme cible → fallback atténuation simple documenté. |
| 0.8 | Spike Sodium : lancer le mod avec Sodium installé, vérifier qu'aucune incompatibilité bloquante n'apparaît sur le rendu de base et le post-process. | 0.5j | MC démarre, vignette de Phase 0.3 toujours visible avec Sodium actif. Si conflit grave → documenter et envisager Indium. |
| 0.9 | **MILESTONE PHASE 0** : décisions documentées pour Veil, Iris, hook frame-exact, EFX, Sodium. Spec V4.3 amendée si nécessaire. | — | Document `phase0_findings.md` produit, listant pour chaque spike : méthode utilisée, résultat, décision, écart éventuel avec la spec. |

**Total Phase 0 indicatif : 4 à 6 jours.** Si la Phase 0 dépasse 10 jours sans converger, il faut reposer la question du choix technologique (Veil ? autre approche ?) avant d'investir en Phase 1.

### Phase 1 — Fondations

| # | Tâche | Pilier |
|---|---|---|
| 1.1 | Setup projet Fabric 1.21.1 + dépendance Veil (version validée Phase 0) | Config |
| 1.2 | `BackroomsConfig` avec chargement fichier config | Config |
| 1.3 | `VeilFramebufferBridge` + `VeilPostProcessingBridge` + `VeilUniformBridge` + `VeilDefinitionsBridge` + `VeilShaderBridge` | Config |
| 1.4 | `IrisBridge` — détection + mode (NO_IRIS / WARN_ONLY / AUTO_TOGGLE selon validation Phase 0) + persistance disque | Config |
| 1.5 | Shader pipeline basique — charger et appliquer un shader simple | Shader |
| 1.6 | Premier effet post-process fonctionnel (vignette) | Shader |
| 1.7 | **MILESTONE** : vignette visible en jeu dans une dimension de test, IrisBridge log son mode au démarrage, persistance Iris testée (kill jeu en backrooms simulé → relance restaure config) | — |

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
| Deux portails se faisant face dans la même pièce | Comportement défini §27 (un seul rendu, l'autre en fallback bruit) |
| Long couloir bouclé, render distance vanilla élevée | Brouillard / coude masque la duplication visible (cf. §18.3) |

### Tests de fallback

| Scénario | Résultat attendu |
|---|---|
| Shader compilation échoue | Portails fonctionnent sans effets |
| Destination pas chargée | Portail reste en PREWARMING |
| EFX non disponible | Audio en mode atténuation simple |
| Iris API incompatible | Bascule WARN_ONLY, pas de crash |
| Caméra collée au portail en biais extrême | Fallback shader (flou/bruit), pas de rendu cassé |
| Mod côté client uniquement sur serveur dédié | Portails désactivés, message clair au joueur, pas de crash |

### Tests Iris

| Scénario | Résultat attendu |
|---|---|
| Joueur sans Iris entre dans les Backrooms | Mode `NO_IRIS`, aucune action Iris |
| Joueur avec Iris, mode `WARN_ONLY` (défaut) | Message chat à la première entrée de la session, Veil rend par-dessus, pas de crash |
| Joueur avec Iris, mode `AUTO_TOGGLE` validé en Phase 0 | Iris désactivé à l'entrée, restauré à la sortie, fondu de transition |
| Joueur avec Iris, `AUTO_TOGGLE` actif, crash en Backrooms, relance | Fichier `iris_state.json` détecté → config Iris d'origine restaurée → fichier supprimé |
| Joueur avec Iris, `AUTO_TOGGLE` actif, toggle échoue à l'exécution (régression Iris) | Bascule WARN_ONLY pour la session, log warning, pas de crash |
| Iris absent | Aucune erreur, IrisBridge en mode `NO_IRIS` |

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

    // Veil 3.x (FoundryMC) — version exacte verrouillée Phase 0
    // Maven : https://maven.blamejared.com/
    // Wiki : https://github.com/FoundryMC/Veil/wiki
    modImplementation "foundry.veil:veil-fabric-1.21.1:<locked-3.x-version>"

    // Iris — soft dependency (compile only, runtime optional)
    modCompileOnly "net.irisshaders:iris:X.X.X"

    // LWJGL (fourni par Minecraft, pour OpenAL EFX)
    // org.lwjgl.openal.EXTEfx — pas de dépendance supplémentaire
}
```

---

## 25. Conventions de code

- **Package racine** : `com.petassegang.addons`
- **Nommage** : PascalCase classes, camelCase méthodes/champs, SCREAMING_SNAKE constantes
- **Javadoc** : obligatoire sur toutes les classes et méthodes publiques
- **Logs** : SLF4J via `LoggerFactory.getLogger(ClassName.class)`
- **Config** : tout paramètre numérique dans `BackroomsConfig`, jamais en dur
- **Veil** : tout appel API Veil via les classes `Veil*Bridge` uniquement
- **Iris** : tout appel Iris via `IrisBridge` uniquement, jamais d'import direct ailleurs
- **Source of Truth** : chaque donnée a un seul propriétaire (§5), les autres lisent via interface
- **Fallback** : chaque sous-système fonctionne en mode dégradé si une dépendance échoue
- **Perception** : toute décision visuelle/audio respecte les 5 Lois de Perception (§4)

---

## 26. Résumé exécutif

> **Ce mod = un système de portes et transitions illusionnistes, avec rendu distant via Veil, TP masqué frame-exact, shader custom dérivé open source, Iris désactivé dans les dimensions backrooms (si toggle validé en Phase 0, sinon avertissement joueur), entités visibles simulées, aggro scriptée via injection de Brain goal, et son cross-space stabilisé via OpenAL EFX, le tout orchestré par un PortalManager central au service d'un level design backrooms trompeur et cohérent. Single-player V1 uniquement.**

Les trois piliers (Configuration/Veil/Iris, Shader, Portail) sont strictement séparés. La machine d'état des portails, les sources of truth, le budget performance, le scoring de priorité, la synchronisation frame-exact, la stabilité audio et les stratégies de fallback garantissent un système robuste et maintenable.

**Le plus gros danger n'est pas l'idée — c'est de vouloir tout développer trop vite. Verrouiller le MVP (phases 0–4) avant d'aller plus loin. La Phase 0 (spikes de validation) n'est pas négociable.**

---

## 27. Récursion de portails — comportement défini

### Problème

Si deux portails sont mutuellement visibles (par exemple deux portes face à face dans la même salle), un rendu naïf produirait :
- Portail A rendu, qui contient la vue de la destination de A
- Cette vue contient le portail B, qui devrait à son tour rendre la vue de la destination de B
- Récursion infinie ou crash

### Décision : pas de récursion (cf. §2 limitations)

Le mod ne supporte **pas** la récursion de portails. Le comportement défini est :

| Situation | Comportement |
|---|---|
| Un seul portail visible dans le champ | Rendu normal |
| Portail A actif, portail B visible **dans la vue distante de A** | Le portail B affiché dans la vue distante de A est rendu en **fallback shader** (bruit/flou statique), pas en rendu distant. L'illusion est que "le portail dans le portail est éteint" |
| Portails A et B mutuellement visibles dans le monde du joueur (pas via vue distante) | Les deux sont scorés normalement, le ou les top N (selon `maxActivePortalViews`) sont rendus, les autres affichent le fallback shader |
| Portail A en TRANSITIONING, B visible derrière | A garde sa priorité absolue, B affiche fallback |

### Implémentation

Le mécanisme repose sur un **drapeau `renderingPortalDepth` (int) tenu par `PortalViewSystem`**, scopé à la frame de rendu courante :

```
renderingPortalDepth = 0   // état initial chaque frame

Pour chaque portail à rendre (dans la passe principale) :
  - Si renderingPortalDepth == 0 : rendu normal de la vue distante
  - Sinon : afficher fallback shader (bruit/flou) sur la surface

Avant d'entrer dans le rendu d'une vue distante :
  - renderingPortalDepth++
Après le rendu de la vue distante :
  - renderingPortalDepth--

Reset à 0 garanti en début de chaque frame (defensive).
```

**Points d'attention** :
- Le drapeau est **scopé à la frame de rendu**, pas global persistant entre frames. Un crash ou une exception en cours de rendu ne doit pas laisser le drapeau "coincé" — utiliser un try/finally autour de l'incrément/décrément.
- Si `maxActivePortalViews >= 2` et que les deux portails sont rendus séquentiellement dans la même frame, le drapeau revient bien à 0 entre les deux (chacun a son cycle propre dans la passe principale, pas l'un dans l'autre).
- L'utilisation d'un `int` (depth) plutôt que d'un `bool` est défensive : même si on autorisait plus tard une récursion à profondeur 1, le code n'aurait qu'à comparer `< maxRecursionDepth` au lieu de `== 0`.

C'est le mécanisme le plus simple et le moins coûteux. Il évite tout besoin de stack/dépile complexe et garantit une terminaison.

### Présentation utilisateur

Cette limitation peut être justifiée comme une feature backrooms : *"Les portes au-delà d'un seuil deviennent floues et bruitées — la perception du joueur ne porte qu'à un niveau de profondeur."*

---

## 28. Compatibilité mods connue

Cette section liste les mods susceptibles d'interagir (positivement ou négativement) avec le mod Backrooms. Elle est indicative et doit être mise à jour au fil des tests.

### Mods de rendu

| Mod | Compatibilité | Notes |
|---|---|---|
| **Sodium** | Compatible attendue | Compatibilité attendue avec Veil 3.x, à confirmer sur la version verrouillée en Phase 0. **Risque résiduel** à valider en spike Phase 0.8 : Dynamic Buffers Veil + pipeline batching Sodium. À retester en Phase 2 après l'arrivée des effets shader complets. |
| **Indium** | Recommandé si Sodium présent | Indium fournit la compat Fabric Rendering API que certains modules Veil/Quasar peuvent nécessiter. À confirmer Phase 0. |
| **Iris** | Soft-dependency gérée (§10) | Mode AUTO_TOGGLE (validé Phase 0) ou WARN_ONLY |
| **Distant Horizons** | À risque | DH étend la render distance via LOD. Risque de duplication visible des couloirs bouclés. À tester, possible blacklist dans les dimensions backrooms. |
| **Embeddium / Rubidium** | Hors scope (Forge) | Le mod cible Fabric uniquement |
| **Continuity / Connected Textures** | Compatible attendue | Travaille au niveau texture, pas pipeline |

### Mods audio

| Mod | Compatibilité | Notes |
|---|---|---|
| **Sound Physics Remastered** | À risque | SPR ajoute son propre traitement EFX (occlusion, reverb). Conflit possible avec `EFXFilterManager`. Comportement à définir après test. Possiblement : désactiver SPR dans les dimensions backrooms, ou laisser SPR prendre le dessus et désactiver le système audio backrooms. |
| **Presence Footsteps** | Compatible attendue | Travaille sur les sons de pas, pas le pipeline EFX |

### Mods de dimensions / monde

| Mod | Compatibilité | Notes |
|---|---|---|
| **Mods de dimensions custom** | Compatible attendue | Tant qu'ils n'interfèrent pas avec les dimensions `backrooms:*` |
| **Mods de génération de structures** | Indifférent | Le mod gère ses propres structures via le système de levels |

### Mods de caméra / contrôles

| Mod | Compatibilité | Notes |
|---|---|---|
| **First Person Model** | À tester | Modifie le rendu première personne, peut interagir avec la sync frame-exact |
| **Camera Utils, Replay Mod** | À risque | Modifient la caméra, peuvent casser `RemoteCamera` |

### Politique générale

- **Pas de hard incompatibility déclarée par défaut.** Le mod tolère la présence d'autres mods et dégrade plutôt que de refuser.
- **Conflits identifiés** : log warning au démarrage, message au joueur si pertinent, dégradation gracieuse.
- **Outils utilisateur** : `BackroomsConfig` doit permettre de désactiver des sous-systèmes individuels (audio cross-space, écho entité, etc.) pour permettre à l'utilisateur de résoudre lui-même un conflit.

---

## 29. Préambule pour développeur (humain ou LLM)

Cette section s'adresse spécifiquement à toute entité (développeur ou agent IA type Codex/Claude) qui va générer le code à partir de cette spec.

### Règles non négociables

1. **Pas d'invention d'API.** Si la spec mentionne une classe Veil, Iris ou Fabric et que tu ne connais pas sa signature exacte pour la version cible (**Veil 3.x compatible Minecraft 1.21.1 — version exacte verrouillée Phase 0**, Fabric 1.21.1, Iris détecté à l'exécution), tu dois :
   - Soit consulter la doc/source officielle. Pour Veil, l'autorité est le wiki `https://github.com/FoundryMC/Veil/wiki` — pas la mémoire du LLM, pas une recherche web générique
   - Soit marquer le code avec `// SPEC-GAP: Phase 0 — verify against <Library> <Version> API` et signaler l'incertitude dans la réponse
   - Soit demander confirmation avant de coder

   **Cas Veil spécifique** : ne JAMAIS inventer une classe ou méthode Veil à partir d'un nom plausible. Veil expose des primitives natives (Post-Processing pipeline JSON, Framebuffers JSON, Dynamic Buffers, ShaderPreDefinitions, VeilEventPlatform, ShaderUniformAccess, etc.) — toute fonctionnalité demandée doit d'abord être recherchée dans ces primitives avant d'écrire une couche custom. Voir spec compagnon `spec_shaders_backrooms_V2.md` §6 et §21 pour la liste des capacités Veil exploitées et les anti-patterns Veil.

2. **Hypothèses explicites.** Toute supposition non couverte par la spec doit être marquée par un commentaire `// ASSUMPTION: ...` au-dessus du code concerné, et listée en synthèse à la fin de la réponse.

3. **Phase 0 obligatoire.** Aucun code de Phase 1+ n'est produit avant que les findings de Phase 0 (`phase0_findings.md`) soient documentés. Si la Phase 0 n'a pas été faite, signale-le et arrête-toi.

4. **Pas de saut de phase.** Génère phase par phase, en validant le milestone précédent. Si on te demande "génère tout le code du mod", refuse et propose le découpage par phase.

5. **Cohérence inter-fichiers.** Les structures de données (§19), enums (`PortalState`, etc.), interfaces de Source of Truth (§5) sont des contrats. Toute modification d'une de ces structures doit être propagée à tous les fichiers qui la référencent.

6. **Single-player V1.** Tout code qui suppose un contexte multi-joueur (paquets réseau, state sync serveur↔client custom pour les portails, etc.) est hors scope. Si une feature semble nécessiter du réseau, signale-le et propose une dégradation single-player.

7. **Respect des contrats §20.** Chaque sous-système a des inputs, outputs, source of truth, dépendances et **interdits** définis. Ne fais jamais faire à un sous-système ce qui est marqué "Interdit".

8. **Respect des Lois de Perception §4.** Toute décision de design visuel ou audio doit être traçable à l'une des 5 Lois. En cas de conflit entre une Loi et une optimisation perf, la Loi gagne (sauf si explicitement noté).

9. **Fallback obligatoire (Loi 5).** Chaque sous-système doit avoir un mode dégradé. Si tu codes une nouvelle classe, écris explicitement le commentaire `// FALLBACK: <comportement>` qui décrit ce qui se passe quand cette classe échoue.

10. **Pas de TODO silencieux.** Si une fonctionnalité de la spec n'est pas implémentée dans le code généré, c'est un `// SPEC-GAP: <référence §X>` explicite, jamais un silence.

### Anti-patterns à éviter

- Importer directement une classe Veil ou Iris hors de `backrooms/bridge/`
- **Réinventer une primitive Veil 3.x validée Phase 0 déjà disponible** (FramebufferPool Java, PassRegistry custom, système d'includes maison, etc.) — voir `spec_shaders_backrooms_V2.md` §21 pour la liste exhaustive des anti-patterns Veil
- Stocker l'état d'un sous-système dont on n'est pas Source of Truth (§5)
- Lancer un thread custom pour le préchargement de chunks (utiliser les mécanismes Fabric/MC)
- Exécuter le TP sur autre chose que le thread principal
- Cloner une entité pour faire un proxy (c'est une lecture, pas un clone)
- Faire un pop visuel d'entité ou audio (toujours fade)
- Crash si une dépendance optionnelle échoue

### Référence rapide aux pièges identifiés

| Piège | Mitigation |
|---|---|
| API Iris fragile | Validation Phase 0.5, mode WARN_ONLY par défaut (§10) |
| Veil API évolutive | Veil 3.x compatible Minecraft 1.21.1, version exacte verrouillée Phase 0, abstraction bridges (§10), wiki officiel `https://github.com/FoundryMC/Veil/wiki` comme autorité |
| LLM hallucinant des classes Veil | Spec compagnon `spec_shaders_backrooms_V2.md` §6 liste les primitives natives à utiliser ; §21 liste les anti-patterns |
| Sync frame-exact non triviale | Hooks validés Phase 0.6 (§15) |
| Récursion portails | Comportement défini, pas de récursion (§27) |
| Couloir bouclé visible au loin | Brouillard + masquage géométrique (§18.3) |
| Multi-joueur non géré | Désactivation propre sur serveur dédié (§1) |
| Aggro cross-portail | Memory module + Goal custom (§18.6) |

---
