# ARCHITECTURE.md — PeTaSsE_gAnG_Additions

> **Version** : v4 unifiée — spec exécutable par IA agentique (Codex / Claude Code / Cursor) ET référence humaine
> **Cible** : Minecraft 1.21.1 / Fabric / Java 21 / Veil 3.x — version exacte verrouillée en Phase 0
> **Compagnon** : `spec_veil_portail_shaders_V4_3.md` + `spec_shaders_backrooms_V2.md` (specs sources portail + shader)

---

## Table des matières

**Partie A — Comprendre le projet**
- [§1. Comment lire ce document](#1-comment-lire-ce-document)
- [§2. Décisions architecturales fondamentales](#2-décisions-architecturales-fondamentales)
- [§3. Arborescence canonique](#3-arborescence-canonique)

**Partie B — Contrats et invariants (lecture obligatoire pour IA)**
- [§4. Contrats fondamentaux](#4-contrats-fondamentaux)
- [§5. Sources of Truth (SoT)](#5-sources-of-truth-sot)
- [§6. Règles d'or non-négociables](#6-règles-dor-non-négociables)
- [§7. Anti-patterns code](#7-anti-patterns-code)

**Partie C — Conventions transverses**
- [§8. Lois de Perception (V4.3 §4)](#8-lois-de-perception-v43-4)
- [§9. Conventions de nommage](#9-conventions-de-nommage)
- [§10. Data-driven vs code-driven](#10-data-driven-vs-code-driven)
- [§11. Threading model](#11-threading-model)

**Partie D — Plan de dev**
- [§12. Phases de développement](#12-phases-de-développement)
- [§13. Mapping spec V4.3 → arbo](#13-mapping-spec-v43--arbo)

**Partie E — Protocole IA (lecture obligatoire pour Codex/Claude Code)**
- [§14. Principes opératoires](#14-principes-opératoires)
- [§15. Workflow de génération](#15-workflow-de-génération)
- [§16. Anti-patterns IA](#16-anti-patterns-ia)
- [§17. Checklist de revue avant commit](#17-checklist-de-revue-avant-commit)
- [§18. Prompts d'amorçage par phase](#18-prompts-damorçage-par-phase)
- [§19. Format de réponse attendu](#19-format-de-réponse-attendu)
- [§20. Limites connues et zones grises](#20-limites-connues-et-zones-grises)
- [§21. Protocole de demande de clarification](#21-protocole-de-demande-de-clarification)

**Annexes**
- [§22. Glossaire](#22-glossaire)

---

# PARTIE A — Comprendre le projet

## §1. Comment lire ce document

Ce document est une **spécification**, pas un tutoriel. Il décrit :

- Une **arborescence canonique** (§3)
- Des **contrats** (interfaces et invariants) (§4)
- Des **règles d'or** non-négociables (§6)
- Des **anti-patterns** à ne jamais commettre (§7, §16)
- Un **protocole de génération de code par IA** (§14-21)

Tout code généré par une IA agentique **doit** respecter ce document. En cas de conflit entre une intention de l'utilisateur et ce document, l'IA **doit** demander confirmation (cf. §21).

**Lecture humaine recommandée** : Partie A + B en première lecture. C + D quand tu attaques le dev. E quand tu utilises une IA.

**Lecture IA obligatoire** : tout le document à chaque session. Le prompt universel (§18.1) le rappelle.

---

## §2. Décisions architecturales fondamentales

| # | Décision | Justification |
|---|----------|---------------|
| D1 | Mod ID = `petasse_gang_additions` | Stabilité — ne change jamais |
| D2 | Package racine = `com.petassegang.addons` | Convention Java standard |
| D3 | Backrooms est une feature dans `com.petassegang.addons.backrooms` | Pas de mod séparé |
| D4 | Approche portail = **illusion** (pas non-euclidienne) | Spec V4.3 §2 |
| D5 | Pattern Bridge strict pour Veil/Iris | Spec V4.3 §10, §25 |
| D6 | Split client/common pour les sous-systèmes portail | V2-multi-joueur ready |
| D7 | Toutes les features implémentent `Lifecycle` | Évite leaks framebuffers/audio |
| D8 | Tous les sous-systèmes portail implémentent `PortalSubsystem` | Tick centralisé non-contournable |
| D9 | Convention "1 dossier par bloc/item/entity multi-fichiers" | Pragmatique, pas strict |
| D10 | PBR LabPBR (suffixes `_n`, `_s`, `_e`) | Compat shader packs externes |

---

## §3. Arborescence canonique

```
src/main/java/com/petassegang/addons/
│
├── PeTaSsEgAnGAdditionsMod.java                # ModInitializer
├── PeTaSsEgAnGAdditionsClientMod.java          # ClientModInitializer
│
├── core/
│   ├── ModConstants.java
│   ├── ModEnvironment.java
│   ├── lifecycle/                              # ─── CONTRAT LIFECYCLE GLOBAL ───
│   │   ├── Lifecycle.java
│   │   ├── LifecycleManager.java
│   │   ├── LifecyclePhase.java                 # enum INIT/STARTED/STOPPED
│   │   └── LifecycleException.java
│   ├── tick/                                   # ─── CONTRAT TICK PHASES ───
│   │   ├── TickPhase.java
│   │   └── TickDispatcher.java
│   ├── annotation/
│   │   ├── SourceOfTruth.java
│   │   ├── ClientOnly.java
│   │   ├── ServerOnly.java
│   │   └── PortalCritical.java
│   └── event/
│       ├── ModEventBus.java
│       └── ModEvent.java
│
├── config/
│   ├── ModConfig.java
│   ├── ConfigManager.java                      # implements Lifecycle
│   └── perf/
│       └── PerformanceConfig.java
│
├── creative/
│   └── ModCreativeTab.java
│
├── init/
│   ├── ModBlocks.java
│   ├── ModBlockEntities.java
│   ├── ModItems.java
│   ├── ModEntities.java
│   ├── ModSoundEvents.java
│   ├── ModParticles.java
│   ├── ModChunkGenerators.java
│   ├── ModBiomes.java
│   ├── ModDimensions.java
│   ├── ModRecipes.java
│   ├── ModLootTables.java
│   ├── ModDamageTypes.java
│   └── ModTags.java
│
├── network/
│   ├── ModNetworking.java
│   ├── codec/
│   ├── c2s/
│   └── s2c/
│
├── feature/
│   ├── gang/
│   │   ├── GangFeature.java                    # implements Lifecycle
│   │   ├── item/gang_badge/
│   │   │   ├── GangBadgeItem.java
│   │   │   ├── GangBadgeItemRenderer.java
│   │   │   └── GangBadgeTooltipData.java
│   │   ├── block/gang_badge_pedestal/
│   │   │   ├── GangBadgePedestalBlock.java
│   │   │   ├── GangBadgePedestalBlockEntity.java
│   │   │   └── GangBadgePedestalRenderer.java
│   │   ├── network/
│   │   │   ├── codec/
│   │   │   ├── c2s/
│   │   │   │   ├── GangBadgeActivatePayload.java
│   │   │   │   └── GangBadgeActivateHandler.java
│   │   │   └── s2c/
│   │   └── client/
│   │       └── GangBadgeClientHandler.java
│   │
│   └── cursed/
│       ├── CursedFeature.java                  # implements Lifecycle
│       ├── item/cursed_snack/
│       └── block/cursed_log/
│
├── backrooms/                                   # ════ FEATURE PRINCIPALE ════
│   │
│   ├── BackroomsFeature.java                   # implements Lifecycle
│   ├── BackroomsConstants.java
│   │
│   ├── config/                                  # Pilier 1
│   │   ├── BackroomsConfig.java                # @SourceOfTruth
│   │   ├── BackroomsConfigManager.java         # implements Lifecycle
│   │   ├── RenderSystemConfig.java
│   │   └── preset/
│   │       └── PerformancePresetSelector.java
│   │
│   ├── bridge/                                  # Pilier 1 — bridges Veil/Iris
│   │   ├── Bridge.java                         # interface commune
│   │   ├── BridgeState.java                    # enum OK / DEGRADED / FAILED
│   │   ├── BridgeHealthMonitor.java            # @SourceOfTruth santé bridges
│   │   ├── VeilPostProcessingBridge.java       # pipelines/post-processing Veil
│   │   ├── VeilUniformBridge.java              # uniforms Veil
│   │   ├── VeilDefinitionsBridge.java          # definitions/#define Veil
│   │   ├── VeilFramebufferBridge.java
│   │   ├── VeilShaderBridge.java
│   │   ├── IrisBridge.java
│   │   └── IrisStateStore.java
│   │
│   ├── shader/                                  # Pilier 2 — couche Java mince, Veil-natif first
│   │   ├── ShaderManager.java                  # implements Lifecycle
│   │   ├── ShaderEventAdapter.java             # handler shader ; appelle uniquement les bridges
│   │   ├── uniform/
│   │   │   ├── UniformContext.java
│   │   │   ├── UniformProvider.java
│   │   │   ├── UniformFallbacks.java
│   │   │   └── provider/
│   │   │       ├── SanityUniformProvider.java
│   │   │       ├── PortalProximityProvider.java
│   │   │       ├── PlayerStateProvider.java
│   │   │       ├── TransitionProgressProvider.java
│   │   │       └── ZoneAmbientProvider.java
│   │   ├── definitions/
│   │   │   ├── BackroomsShaderDefinitions.java
│   │   │   └── DefinitionMapper.java
│   │   ├── profile/
│   │   │   ├── QualityProfile.java
│   │   │   ├── QualityProfileSelector.java
│   │   │   ├── ProfileDefinition.java
│   │   │   ├── ProfileRegistry.java
│   │   │   └── AutoQualityDetector.java
│   │   ├── effect/
│   │   │   ├── ShaderEffect.java
│   │   │   ├── EffectDescriptor.java
│   │   │   ├── EffectRegistry.java
│   │   │   ├── EffectCondition.java
│   │   │   ├── EffectParamSchema.java
│   │   │   └── builtin/
│   │   ├── level/
│   │   │   ├── LevelShaderProfile.java
│   │   │   ├── LevelShaderRegistry.java
│   │   │   └── LevelShaderResolver.java
│   │   └── health/
│   │       ├── PipelineHealthMonitor.java
│   │       └── DegradationPolicy.java
│   │
│   │   # ShaderEventAdapter appelle uniquement les bridges backrooms/bridge/.
│   │   # Il ne doit jamais importer Veil directement.
│   │
│   ├── portal/                                  # Pilier 3 — COMMON
│   │   │
│   │   ├── PortalManager.java                  # implements Lifecycle ; UNIQUE entry point tick
│   │   ├── PortalContext.java                  # contexte unifié (record immuable)
│   │   ├── PortalSubsystem.java                # interface — TOUT subsystem implémente
│   │   ├── PortalPriorityScore.java
│   │   │
│   │   ├── shared/                             # DTO traversant client/common
│   │   │   ├── PortalState.java                # enum 6 états §6 (TYPE partagé, pas SoT)
│   │   │   ├── PortalLink.java                 # immutable record
│   │   │   ├── PortalId.java
│   │   │   └── PortalEvent.java
│   │   │
│   │   ├── transition/                         # §18.2 spec V4.3
│   │   │   ├── TransitionSystem.java           # implements PortalSubsystem
│   │   │   ├── ThresholdDetector.java
│   │   │   ├── TeleportExecutor.java           # @PortalCritical
│   │   │   ├── TransitionContext.java
│   │   │   ├── PortalStateMachine.java
│   │   │   ├── FrameSyncPolicy.java
│   │   │   └── DestinationLoader.java
│   │   │
│   │   ├── illusion/                           # §18.3 spec V4.3
│   │   │   ├── SpatialIllusionSystem.java      # implements PortalSubsystem
│   │   │   ├── RoomGraph.java                  # @SourceOfTruth
│   │   │   ├── RoomSegment.java
│   │   │   ├── ExitId.java
│   │   │   ├── RedirectContext.java
│   │   │   ├── RedirectResolver.java
│   │   │   ├── RoomVariantRegistry.java
│   │   │   ├── LoopingCorridorManager.java
│   │   │   └── rule/
│   │   │       ├── RedirectRule.java
│   │   │       ├── LoopRule.java
│   │   │       ├── DirectionRule.java
│   │   │       ├── CountRule.java
│   │   │       ├── TimeRule.java
│   │   │       ├── StateRule.java
│   │   │       └── RandomRule.java
│   │   │
│   │   ├── aggro/                              # §18.6 spec V4.3
│   │   │   ├── AggroSimulationSystem.java      # implements PortalSubsystem
│   │   │   ├── StimulusType.java
│   │   │   ├── StimulusPropagator.java
│   │   │   └── PortalLureGoal.java
│   │   │
│   │   └── client/                             # Pilier 3 — CLIENT
│   │       │
│   │       ├── view/                           # §18.1 spec V4.3
│   │       │   ├── PortalViewSystem.java       # implements PortalSubsystem (CLIENT)
│   │       │   ├── RemoteCamera.java
│   │       │   ├── RemoteCameraGuard.java
│   │       │   ├── PortalFramebuffer.java
│   │       │   ├── PortalSurfaceRenderer.java
│   │       │   ├── PortalRenderTarget.java
│   │       │   └── PortalDepthTracker.java
│   │       │
│   │       ├── audio/                          # §18.5 spec V4.3
│   │       │   ├── CrossSpaceAudioSystem.java  # implements PortalSubsystem (CLIENT)
│   │       │   ├── SpatialSoundProjector.java
│   │       │   ├── EFXFilterManager.java
│   │       │   ├── AcousticZoneProfile.java
│   │       │   ├── ProjectedSound.java
│   │       │   └── AudioSourceStabilizer.java
│   │       │
│   │       ├── entity_echo/                    # §18.4 spec V4.3
│   │       │   ├── EntityEchoSystem.java       # implements PortalSubsystem (CLIENT)
│   │       │   ├── CrossSpaceEntitySync.java   # READ-ONLY (cf. AP-8)
│   │       │   └── EchoState.java
│   │       │
│   │       └── effect/
│   │           ├── TransitionMasker.java
│   │           └── MaskType.java
│   │
│   ├── debug/
│   │   ├── DebugMetrics.java                   # @SourceOfTruth
│   │   ├── DebugHud.java
│   │   └── DebugCommands.java
│   │
│   ├── level/
│   │   ├── LevelTheme.java
│   │   ├── LevelRegistry.java
│   │   ├── BackroomsDimensions.java
│   │   │
│   │   ├── common/
│   │   │   ├── BackroomsBaseChunkGenerator.java
│   │   │   ├── BackroomsPipeline.java
│   │   │   ├── noclip/
│   │   │   │   ├── NoClipDetector.java
│   │   │   │   ├── NoClipTransition.java
│   │   │   │   └── NoClipRegistry.java
│   │   │   └── registry/
│   │   │       └── BackroomsLevelRegistry.java
│   │   │
│   │   └── level0/
│   │       ├── Level0Constants.java
│   │       ├── Level0Feature.java              # implements Lifecycle
│   │       ├── block/
│   │       │   ├── wallpaper/
│   │       │   │   ├── Level0WallpaperBlock.java
│   │       │   │   ├── Level0WallpaperBlockEntity.java
│   │       │   │   ├── Level0WallpaperBakedModel.java
│   │       │   │   ├── Level0WallpaperBlockStateModel.java
│   │       │   │   └── Level0WallpaperModelHandler.java
│   │       │   ├── carpet/
│   │       │   ├── ceiling/
│   │       │   ├── fluorescent_light/
│   │       │   │   ├── Level0FluorescentLightBlock.java
│   │       │   │   └── Level0FluorescentLightFlickerHandler.java
│   │       │   └── wall/
│   │       │       ├── Level0YellowWallBlock.java
│   │       │       └── Level0WhiteWallBlock.java
│   │       ├── biome/
│   │       │   ├── Level0SurfaceBiome.java
│   │       │   └── Level0BiomeSampler.java
│   │       ├── generation/
│   │       │   ├── Level0ChunkGenerator.java
│   │       │   ├── Level0PipelineConfig.java
│   │       │   ├── layer/
│   │       │   │   ├── Level0LayerStack.java
│   │       │   │   ├── Level0FloorLayer.java
│   │       │   │   ├── Level0CarpetLayer.java
│   │       │   │   ├── Level0WallLayer.java
│   │       │   │   ├── Level0CeilingLayer.java
│   │       │   │   └── Level0LightingLayer.java
│   │       │   ├── layout/
│   │       │   │   ├── Level0Layout.java
│   │       │   │   ├── Level0MazeBuilder.java
│   │       │   │   ├── Level0LayoutCache.java
│   │       │   │   ├── Level0LayoutSeed.java
│   │       │   │   └── room/
│   │       │   │       ├── Room.java
│   │       │   │       ├── RectangularRoom.java
│   │       │   │       ├── PillarRoom.java
│   │       │   │       └── PolygonalRoom.java
│   │       │   ├── surface/
│   │       │   │   ├── Level0SurfaceSampler.java
│   │       │   │   ├── Level0FaceMaskComputer.java
│   │       │   │   └── Level0SurfaceTransition.java
│   │       │   └── perf/
│   │       │       ├── Level0GenerationProfiler.java
│   │       │       └── Level0BenchmarkRunner.java
│   │       └── client/
│   │           ├── shader/
│   │           │   ├── Level0ShaderRegistry.java
│   │           │   ├── Level0WallpaperShaderHook.java
│   │           │   └── Level0FlickerEffect.java
│   │           └── ambient/
│   │               └── Level0AmbientLoop.java
│   │
│   ├── entity/                                  # mobs Backrooms (futur)
│   │
│   └── network/
│       ├── codec/
│       ├── c2s/
│       └── s2c/
│
├── system/                                      # SYSTÈMES GAMEPLAY TRANSVERSES
│   ├── sanity/
│   │   ├── SanityFeature.java                  # implements Lifecycle
│   │   ├── SanityComponent.java                # @SourceOfTruth (per-player)
│   │   ├── SanityEffects.java
│   │   ├── SanityEventHandler.java
│   │   └── client/
│   │       └── SanityHudOverlay.java
│   ├── ambient/
│   │   ├── AmbientDirector.java
│   │   └── AmbientState.java
│   └── quest/                                   # à venir
│
├── client/                                      # client transverse non-Backrooms
│   ├── render/
│   │   ├── block/
│   │   ├── entity/
│   │   ├── item/
│   │   └── particle/
│   ├── model/
│   │   └── ModelLoaderRegistry.java
│   ├── handler/
│   ├── gui/
│   │   ├── overlay/
│   │   └── screen/
│   └── sound/
│
├── sound/
├── tag/
│   ├── ModBlockTags.java
│   ├── ModItemTags.java
│   └── ModBiomeTags.java
│
├── mixin/
│   ├── client/
│   ├── common/
│   └── access/
│
├── datagen/
│   ├── ModDataGenerator.java
│   ├── provider/
│   │   ├── ModBlockLootTableProvider.java
│   │   ├── ModRecipeProvider.java
│   │   ├── ModBlockTagProvider.java
│   │   ├── ModItemTagProvider.java
│   │   ├── ModBiomeTagProvider.java
│   │   ├── ModLanguageProvider.java
│   │   ├── ModBlockModelProvider.java
│   │   ├── ModItemModelProvider.java
│   │   └── ModAdvancementProvider.java
│   └── builder/
│       └── PbrModelBuilder.java
│
├── perf/
│   ├── PerformanceMonitor.java
│   ├── PerformanceSection.java
│   ├── PerformanceLogger.java
│   └── section/
│       ├── ServerTickSection.java
│       └── ClientTickSection.java
│
├── recovery/                                    # crash recovery LOCALISÉ — scope strict
│   │                                            # ⚠️ AUCUN système global de recovery.
│   │                                            # Seuls cas autorisés : persistence d'état
│   │                                            # côté mod externe instable (ex: Iris).
│   │                                            # Tout nouveau fichier ici nécessite une
│   │                                            # justification documentée + commit séparé.
│   └── (limité à IrisStateStore — voir backrooms/bridge/IrisStateStore.java)
│
└── util/
    ├── IdentifierHelper.java
    ├── BlockPosHelper.java
    ├── DirectionHelper.java
    └── math/
        └── DeterministicHash.java
```

---

# PARTIE B — Contrats et invariants

## §4. Contrats fondamentaux

### §4.1 `Lifecycle` (D7)

```java
package com.petassegang.addons.core.lifecycle;

/**
 * Implémenté par toute classe possédant des ressources allouées (framebuffers,
 * audio sources, listeners, threads). Orchestré par LifecycleManager.
 *
 * Ordre garanti : init() → start() → [runtime] → stop()
 * reload() peut être appelé entre start() et stop().
 *
 * Une classe Lifecycle NE DOIT PAS s'auto-démarrer.
 */
public interface Lifecycle {
    void init() throws LifecycleException;
    void start() throws LifecycleException;
    void stop() throws LifecycleException;

    default void reload() throws LifecycleException {
        stop();
        start();
    }

    LifecyclePhase getPhase();
}
```

**Invariants** :
- Aucune méthode publique ne doit être utilisable avant `start()` (no-op ou exception).
- `stop()` est idempotent.
- Toutes les ressources OpenGL sont libérées dans `stop()`.
- Toutes les ressources OpenGL sont allouées dans `init()`, jamais dans le constructeur.

### §4.2 `PortalSubsystem` (D8)

```java
package com.petassegang.addons.backrooms.portal;

/**
 * Tout sous-système portail implémente cette interface.
 *
 * RÈGLE D'OR : aucune méthode tick() d'un PortalSubsystem ne doit être appelée
 * en dehors de PortalManager.dispatch(phase, ctx).
 *
 * Pour le forcer techniquement, les implémentations ont un constructeur
 * package-private dans backrooms.portal et ne sont instanciées que par
 * PortalManager.
 */
public interface PortalSubsystem {
    String getId();
    Set<TickPhase> getRegisteredPhases();
    int getPriority();                              // lower = first
    void tick(TickPhase phase, PortalContext ctx);
    Set<Bridge> getDependencies();                  // skip si DEGRADED/FAILED
}
```

### §4.3 `PortalContext`

```java
package com.petassegang.addons.backrooms.portal;

/**
 * Contexte unique passé à tous les PortalSubsystem.tick().
 * Construit par PortalManager au début de chaque tick. Immuable pendant le tick.
 */
public record PortalContext(
    PlayerEntity player,
    World world,
    PortalState currentState,
    PortalLink activePortal,        // nullable
    BackroomsConfig config,
    DebugMetrics metrics,
    long tickCount,
    float partialTick
) {}
```

### §4.4 `TickPhase`

```java
package com.petassegang.addons.core.tick;

public enum TickPhase {
    PRE_TICK,        // avant logique gameplay
    TICK,            // logique gameplay (server tick)
    POST_TICK,       // post-logique, avant render
    RENDER,          // pendant render world
    POST_RENDER      // après render — frame-exact safe pour TP
}
```

`TickDispatcher` route les events Fabric (`ServerTickEvents`, `ClientTickEvents`, `WorldRenderEvents`) vers les `TickPhase` correspondantes.

### §4.5 `Bridge` + `BridgeHealthMonitor`

```java
package com.petassegang.addons.backrooms.bridge;

public interface Bridge {
    String getId();
    BridgeState getState();
    void onStateChange(Consumer<BridgeState> listener);
}

public enum BridgeState { OK, DEGRADED, FAILED }
```

`BridgeHealthMonitor` est interrogé par `PortalManager` avant chaque tick pour skip les sous-systèmes dont les bridges ne sont pas `OK`.

### §4.6 Annotations documentaires

```java
@SourceOfTruth     // marque la classe qui DÉTIENT une donnée
@ClientOnly        // équivalent court de @Environment(EnvType.CLIENT)
@ServerOnly
@PortalCritical    // code sur chemin frame-exact, ne pas modifier sans benchmark
```

Annotations enforced par revue (humaine ou IA) — pas d'annotation processor.

---

## §5. Sources of Truth (SoT)

Liste exhaustive. Toute donnée listée a **une seule classe propriétaire**. Tout autre code la **lit**, ne la duplique pas, ne la cache pas.

| Donnée | SoT | Lecteurs autorisés |
|--------|-----|--------------------|
| Mod ID, version, logger | `core.ModConstants` | tout le mod |
| Config Backrooms | `backrooms.config.BackroomsConfig` | tout `backrooms/*` |
| Config mod globale | `config.ModConfig` | tout le mod |
| État courant d'un portail (valeur runtime) | `portal.transition.TransitionSystem` (`@SourceOfTruth`) — type partagé : `portal.shared.PortalState` (enum) | tout `portal/*` (lecture via `PortalContext.currentState()`) |
| Topologie des salles | `portal.illusion.RoomGraph` | `SpatialIllusionSystem`, `PortalManager` (lecture) |
| Métriques runtime portail | `backrooms.debug.DebugMetrics` | `DebugHud`, logs |
| Sanité par joueur | `system.sanity.SanityComponent` | `SanityEffects`, `SanityHudOverlay` |
| Santé des bridges | `backrooms.bridge.BridgeHealthMonitor` | `PortalManager` |
| Phase lifecycle d'un composant | la classe `Lifecycle` elle-même | `LifecycleManager` |

**Règle** : si tu écris du code qui maintient une copie d'une donnée listée, tu commets une violation. Lis-la à chaque fois.

---

## §6. Règles d'or non-négociables

1. **Aucun import Veil ou Iris hors de `backrooms/bridge/`.**
   - Vérifiable : `grep -rE "import (foundry\.veil|net\.coderbot\.iris|net\.irisshaders)" --include="*.java" src/main/java | grep -v "/bridge/"` doit retourner vide.

2. **Aucun appel à `subsystem.tick()` hors de `PortalManager.dispatch()`.**
   - Vérifiable : recherche de `\.tick(` dans `backrooms/portal/**/*.java` ne doit matcher que `PortalManager.java`.

3. **Aucune classe `Lifecycle` ne s'auto-démarre.**
   - Vérifiable : `start()` n'est appelé ni dans `init()` ni dans le constructeur.

4. **Tout TP joueur passe par `TeleportExecutor`** sur thread principal.
   - Vérifiable : aucun `player.teleport(`, `player.changeDimension(` dans `backrooms/*` hors de `TeleportExecutor`.

5. **EntityEcho est lecture seule.**
   - Vérifiable : aucun setter, aucun `tick()`, aucun appel mutable d'`Entity` dans `entity_echo/`.

6. **AggroSimulationSystem cible l'entité réelle**, jamais le proxy.
   - Vérifiable : `StimulusPropagator.propagate(Entity, ...)` reçoit `Entity` réelle, pas `EchoState`.

7. **`PortalManager` est le seul à instancier les `PortalSubsystem`.**
   - Vérifiable : constructeurs des subsystems sont package-private.

8. **Pas de TODO silencieux.** Marqueurs autorisés : `// SPEC-GAP: §X`, `// FIXME(<ticket>):`, `// FALLBACK: <comportement>`. Pas de `// TODO` nu.

9. **Tout shader appelable par le mod est enregistré dans `ShaderManager`.** Pas d'appel direct à Veil pour un shader ad-hoc.

10. **Aucune ressource OpenGL allouée dans un constructeur.** Toujours dans `init()`.

11. **`ModEventBus` ne contourne JAMAIS `PortalManager`.**
    - Aucun `PortalEvent` ne déclenche directement un `tick()`, une transition d'état, ou un TP.
    - Le bus sert à **notifier** (logs, debug HUD, sanity, observers passifs), pas à **commander**.
    - Liste fermée d'usages autorisés pour `PortalEvent` : `PortalSpawned`, `PortalDespawned`, `TransitionStarted`, `TransitionCompleted`, `TransitionFailed`. Tous **après-coup**, jamais en injonction.
    - Vérifiable : aucun handler de `PortalEvent` ne doit appeler `PortalManager.dispatch()`, `subsystem.tick()`, `TeleportExecutor.execute()`, ni muter `PortalState`.

---

## §7. Anti-patterns code

### AP-1 : Le sous-système qui s'auto-tick

```java
// ❌ INTERDIT
public class TransitionSystem implements PortalSubsystem {
    public TransitionSystem() {
        ClientTickEvents.END_CLIENT_TICK.register(c -> tick(...));
    }
}
```
Pourquoi : viole D8. `PortalManager` ne sait pas qu'il tick.

```java
// ✅ CORRECT
public class TransitionSystem implements PortalSubsystem {
    TransitionSystem() { /* package-private */ }

    @Override public Set<TickPhase> getRegisteredPhases() {
        return Set.of(TickPhase.POST_RENDER);
    }
    @Override public void tick(TickPhase phase, PortalContext ctx) { ... }
}
```

### AP-2 : Le cache local d'une SoT

```java
// ❌ INTERDIT
public class PortalSurfaceRenderer {
    private PortalState cachedState;  // copie d'une SoT
}
```
Pourquoi : viole §5. `cachedState` se désynchronise.

```java
// ✅ CORRECT
public class PortalSurfaceRenderer {
    public void render(PortalContext ctx) {
        if (ctx.currentState() == PortalState.TRANSITIONING) { ... }
    }
}
```

### AP-3 : L'import Veil clandestin

```java
// ❌ INTERDIT (dans portal/client/view/PortalFramebuffer.java)
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
```
Pourquoi : viole règle d'or n°1.

```java
// ✅ CORRECT
import com.petassegang.addons.backrooms.bridge.VeilFramebufferBridge;

public class PortalFramebuffer {
    private final VeilFramebufferBridge bridge;
    private final FramebufferHandle handle;  // type abstrait défini par bridge
}
```

### AP-4 : L'allocation GL dans le constructeur

```java
// ❌ INTERDIT
public class PortalFramebuffer {
    private final int textureId;
    public PortalFramebuffer() {
        this.textureId = GL11.glGenTextures();
    }
}
```
Pourquoi : viole D7. Si l'objet n'est jamais `init()`, l'alloc fuit.

### AP-5 : Le state mutable partagé

```java
// ❌ INTERDIT
public class PortalManager {
    public static PortalLink CURRENT_PORTAL;
}
```

```java
// ✅ CORRECT
public class PortalManager {
    private final TransitionSystem transitionSystem;

    public PortalLink getActivePortal() {
        return transitionSystem.getActivePortal();
    }
}
```

### AP-6 : Le mock de `World` ou `BlockPos`

Ne pas mocker Minecraft dans les tests unitaires. Tester uniquement les classes pures (`RoomGraph`, `RedirectRule`, `PortalPriorityScore`, `DeterministicHash`). Le reste est testé en `runClient` avec scénarios reproductibles.

### AP-7 : Le TODO nu

```java
// ❌ INTERDIT
// TODO nu interdit : gérer la sortie

// ✅ CORRECT
// SPEC-GAP: §18.3 — gestion sortie multi-portails non implémentée (Phase 7)
// FIXME(#42): race condition possible si tick > 200ms
// FALLBACK: en cas d'échec ChunkLoad, joueur reste dans la dimension d'origine
```

### AP-8 : EntityEcho qui mute l'entité réelle

```java
// ❌ INTERDIT
public class CrossSpaceEntitySync {
    public void sync(Entity real, EchoState echo) {
        real.setPosition(echo.x, echo.y, echo.z);  // mutation !
    }
}
```
Pourquoi : viole règle d'or n°5. `EchoState` reflète l'entité réelle, pas l'inverse.

### AP-9 : Le contournement de `PortalManager` via event bus

```java
// ❌ INTERDIT
public class SomeListener {
    @Subscribe
    public void onPlayerEnterPortal(PlayerEnterPortalEvent e) {
        teleportExecutor.execute(e.player(), e.target());  // bypass PortalManager !
    }
}

// ❌ INTERDIT (variante plus subtile)
public class SomeHandler {
    public void onTick(ClientTickEvent e) {
        modEventBus.post(new PortalEvent.RequestTick());  // injonction déguisée
    }
}
```
Pourquoi : viole règle d'or n°11. `ModEventBus` est un canal de **notification**, pas un canal de **commande**. Tout TP, tick ou transition d'état passe par `PortalManager`.

```java
// ✅ CORRECT — l'event bus notifie après-coup
public class TransitionSystem implements PortalSubsystem {
    @Override
    public void tick(TickPhase phase, PortalContext ctx) {
        // ... logique transition ...
        if (transitionJustCompleted) {
            modEventBus.post(new PortalEvent.TransitionCompleted(portalId, dest));
        }
    }
}

// ✅ CORRECT — un handler observe, ne commande pas
public class SanityEventHandler {
    @Subscribe
    public void onTransition(PortalEvent.TransitionCompleted e) {
        sanityComponent.applyDecay(0.1f);  // mute son propre state, pas le portail
    }
}
```

---

# PARTIE C — Conventions transverses

## §8. Lois de Perception (V4.3 §4)

Ces règles ne sont pas vérifiables par grep ; elles sont à respecter dans la **conception** de toute feature liée aux Backrooms ou aux portails.

| Loi | Énoncé |
|-----|--------|
| L1 | Le joueur ne doit jamais voir une transition de TP. Tout TP est masqué visuellement. |
| L2 | L'illusion prime sur la cohérence physique. Une boucle infinie est valide si elle est crédible. |
| L3 | Le son traverse les portails comme la vue. Un portail muet rompt l'illusion. |
| L4 | Une entité hostile vue à travers un portail doit pouvoir réagir au joueur réel. |
| L5 | La perception du joueur est la SoT du gameplay. Ce que le code "sait" est secondaire. |

Si une décision de design viole une de ces lois, l'IA **doit** flagger la violation.

---

## §9. Conventions de nommage

### §9.1 Java

- Classes : `PascalCase`
- Méthodes/variables : `camelCase`
- Constantes : `SCREAMING_SNAKE_CASE`
- Packages : `lowercase` (pas de `_`, pas de `camelCase`)
- Records : `PascalCase`, suffixés `*Context`, `*State`, `*Event` selon le rôle
- Enums : `PascalCase`, valeurs `SCREAMING_SNAKE_CASE`

### §9.2 Préfixes spécifiques

- Classes Backrooms-spécifiques : préfixées `Backrooms*` ou `Level<N>*`
- Bridges : suffixés `Bridge`
- Subsystems portail : suffixés `System` (sauf `PortalManager` qui est l'orchestrateur)
- Features (root d'une feature) : suffixées `Feature`

### §9.3 Shaders / uniforms

- Fichiers shader : `snake_case.fsh` / `snake_case.vsh` / `snake_case.json`
- Uniforms GLSL : `u_camelCase` (préfixe `u_`)
- Samplers : `s_camelCase` (préfixe `s_`)
- Constantes shader : `SCREAMING_SNAKE_CASE`

### §9.4 Identifiers Minecraft

- Tous les `Identifier` du mod : namespace `petasse_gang_additions`
- Paths : `snake_case`, hiérarchie miroir du package Java
- Exemple : `Level0WallpaperBlock` → `Identifier.of("petasse_gang_additions", "backrooms/level0/wallpaper")`

### §9.5 Marqueurs de commentaires

- `// SPEC-GAP: §X` — feature spec non implémentée, X = section spec
- `// FIXME(<ticket>):` — bug ou dette technique connue
- `// FALLBACK: <comportement>` — comportement dégradé volontaire
- `// PERF: <note>` — optimisation potentielle
- `// SAFETY: <note>` — point de vigilance threading/lifecycle

`// TODO` nu est **interdit**.

---

## §10. Data-driven vs code-driven

| Type | Code (Java) | JSON statique | Runtime |
|------|:-----------:|:-------------:|:-------:|
| Blocks/Items registration | ✅ | | |
| Models, blockstates | datagen | ✅ output | |
| Loot tables, recipes | datagen | ✅ output | |
| Tags | datagen | ✅ output | |
| Dimensions, biomes | datagen | ✅ output | |
| Shaders | | ✅ `assets/.../veil/shaders/` | hot-reload |
| Framebuffers Veil | | ✅ `assets/.../veil/framebuffers/` | hot-reload |
| Pipeline post-process | | ✅ `assets/.../veil/pipelines/` | hot-reload |
| RoomGraph topologie | ✅ | | ✅ build runtime |
| RedirectRules | ✅ classes | | ✅ |
| Configuration utilisateur | | ✅ `config/petasse_gang_additions.json` | ✅ reload |
| PerformancePresets | ✅ enum | | ✅ |

**Règle** : si listé "JSON statique", la version Java est **générée par datagen** uniquement. Pas d'édition manuelle des sorties.

---

## §11. Threading model

| Thread | Code autorisé | Interdit |
|--------|---------------|----------|
| Main client | UI, render, GL, TP, `PortalSubsystem.tick` | I/O bloquant |
| Server (single-player) | logique entité, ChunkGenerator, RoomGraph mutations | rendering |
| Worker pool (datagen) | datagen providers | runtime |
| OpenAL (géré MC) | | tout |

**Règle** : tout `@PortalCritical` s'exécute sur main client thread. Toute opération longue (>5ms) est éclatée sur plusieurs ticks via `WorkScheduler` (à créer si besoin).

---

# PARTIE D — Plan de dev

## §12. Phases de développement

| Phase | Milestone | Modules clés | Validation |
|:----:|-----------|--------------|------------|
| **0** | Spikes Veil/Iris/Sodium | `bridge/`, `docs/phase0_findings.md` | `phase0_findings.md` complet |
| **1** | Pilier 1 — config + bridges + Lifecycle + tick | `config/`, `bridge/`, `core/lifecycle/`, `core/tick/` | `runClient` no-op fonctionne |
| **2** | ShaderManager + pipeline Veil natif | `shader/`, assets shaders | Pipeline minimal visible |
| **3** | PortalManager + state machine + scoring | `portal/`, `portal/transition/` | Tests unitaires verts |
| **4** | **PortalViewSystem — premier portail rendu** ⭐ | `portal/client/view/` | Portail visuel jouable |
| **5** | TransitionSystem complet (TP frame-exact) | `portal/transition/`, `portal/client/effect/` | TP sans flicker |
| **6** | CrossSpaceAudioSystem | `portal/client/audio/` | Son traverse portail |
| **7** | EntityEchoSystem + AggroSimulationSystem | `portal/client/entity_echo/`, `portal/aggro/` | Entité visible et réactive |
| **8** | SpatialIllusionSystem | `portal/illusion/` | Couloirs bouclés |
| **9** | Effets visuels avancés | `shader/`, providers | Distortion + glitch |
| **10** | Niveaux Backrooms | `level/level0/`, etc. | Level 0 jouable |

**🔒 VERROU PHASE 0 — non-négociable** :

Tant que `docs/phase0_findings.md` n'existe pas et n'est pas validé, la feature Backrooms entière est **désactivée par défaut** :

```java
// Dans backrooms/config/BackroomsConfig.java
public boolean enableBackroomsFeature = false;  // FORCED FALSE jusqu'à validation Phase 0
```

`BackroomsFeature.start()` doit court-circuiter immédiatement si `enableBackroomsFeature == false`, en loggant :

```
[Backrooms] Feature désactivée — Phase 0 non validée (phase0_findings.md manquant ou incomplet).
```

Cette protection ne peut être levée qu'**après** :
1. Création de `docs/phase0_findings.md` complet (5 sections obligatoires, cf. §18.2).
2. Édition explicite de `BackroomsConfig.enableBackroomsFeature = true` par l'utilisateur, dans un commit séparé titré `chore: unlock Backrooms after Phase 0`.

L'IA **ne doit jamais** activer ce flag elle-même, même sur demande. Si l'utilisateur le demande, l'IA répond selon le protocole §21 et exige un commit séparé.

Aucune phase ≥ 1 n'est entamée tant que la précédente n'est pas validée par `runClient` + tests.

---

## §13. Mapping spec V4.3 → arbo

| Section spec | Implémenté dans |
|--------------|----------------|
| §1 Vue d'ensemble | `BackroomsFeature` |
| §3 Stack technique | `build.gradle`, `bridge/IrisBridge` |
| §4 Lois de Perception | §8 ce doc |
| §5 Source of Truth | §5 ce doc |
| §6 Machine d'état | `portal/shared/PortalState` + `portal/transition/PortalStateMachine` |
| §7 Budget perf | `config/BackroomsConfig` + `RenderSystemConfig` |
| §8 Stratégies fallback | `// FALLBACK:` partout |
| §10 Bridges | `bridge/*Bridge` |
| §11 Shaders | `shader/ShaderManager` + `shader/ShaderEventAdapter` + JSON Veil via bridges |
| §12 PortalManager | `portal/PortalManager` |
| §13 Priorité portail | `portal/PortalPriorityScore` |
| §14 Cas extrêmes caméra | `portal/client/view/RemoteCameraGuard` |
| §15 Sync frame-exact | `portal/transition/FrameSyncPolicy` + `core/tick/TickPhase.POST_RENDER` |
| §16 Hysteresis audio | `portal/client/audio/AudioSourceStabilizer` |
| §17 Debug HUD | `debug/DebugHud` + `debug/DebugMetrics` |
| §18.1 PortalViewSystem | `portal/client/view/` |
| §18.2 TransitionSystem | `portal/transition/` |
| §18.3 SpatialIllusionSystem | `portal/illusion/` |
| §18.4 EntityEchoSystem | `portal/client/entity_echo/` |
| §18.5 CrossSpaceAudioSystem | `portal/client/audio/` |
| §18.6 AggroSimulationSystem | `portal/aggro/` |
| §27 Pas de récursion | `portal/client/view/PortalDepthTracker` |
| §28 Compat mods | `bridge/` + `BackroomsConfig` |

---

# PARTIE E — Protocole IA

> 🤖 **Cette partie est lue obligatoirement par toute IA agentique générant du code pour ce projet.**

## §14. Principes opératoires

### §14.1 Hiérarchie d'autorité

Ordre d'autorité des documents :

1. `ARCHITECTURE.md` (contrats, arbo, règles globales)
2. `spec_veil_portail_shaders_V4_3.md` (portails + piliers)
3. `spec_shaders_backrooms_V2.md` (shader system)
4. `MIGRATION.md` (procédure technique)
5. Demande utilisateur courante
6. Conventions Fabric/Java standard

En cas de conflit, le document de rang supérieur gagne. Si la demande utilisateur contredit (1), (2), (3) ou (4), l'IA **doit demander confirmation** avant de générer le code, en citant la règle violée (cf. §21).

### §14.2 Principe du "diff minimal"

Une PR ne touche qu'un sous-ensemble cohérent du code. Une IA ne refactore pas spontanément du code voisin "en passant". Si elle voit un problème adjacent, elle l'ajoute à une liste `// FIXME(<ticket>):` mais ne le corrige pas dans la même session.

### §14.3 Principe de la double lecture

Avant de modifier un fichier, l'IA :

1. Lit le fichier complet (pas un extrait).
2. Lit son `package-info.java` s'il existe.
3. Liste les classes qui l'importent (`grep -r "import .*<ClassName>"`).
4. Relit §6 (règles d'or) et §7 (anti-patterns).

### §14.4 Principe de l'exécutabilité

Tout code généré **doit** :

- Compiler (`./gradlew build`).
- Passer les tests existants (`./gradlew test`).
- Lancer `runClient` sans crash si la phase concerne un module runtime.

Si l'IA génère du code qu'elle ne peut pas vérifier, elle le **dit explicitement** :

> ⚠️ Je n'ai pas pu vérifier que ce code compile. Lance `./gradlew build` avant de commit.

---

## §15. Workflow de génération

### §15.1 Étapes obligatoires pour toute nouvelle classe

1. **Localiser** : trouver le bon package via §3.
2. **Vérifier les contrats** : la classe doit-elle implémenter `Lifecycle`, `PortalSubsystem`, ou autre ?
3. **Vérifier la SoT** : la donnée que la classe manipule a-t-elle déjà un propriétaire ? (cf. §5).
4. **Annoter** : `@SourceOfTruth`, `@ClientOnly`, `@PortalCritical` selon le rôle.
5. **Documenter** : javadoc obligatoire avec :
   - Rôle de la classe (1 phrase)
   - SoT possédée (si applicable)
   - Phase de lifecycle (si applicable)
   - `// FALLBACK:` si la classe a un comportement dégradé
6. **Tester** : si la classe est pure (pas de dépendance Minecraft), créer un test JUnit dans `src/test/java`.

### §15.2 Étapes obligatoires pour modifier une classe existante

1. Lire le fichier complet.
2. Relire §7 (anti-patterns code) et §16 (anti-patterns IA).
3. Vérifier que la modification ne crée pas de violation des règles d'or (§6).
4. Si oui, refuser la modification et expliquer pourquoi (cf. §21).

### §15.3 Étapes obligatoires pour ajouter un `PortalSubsystem`

1. Implémenter `PortalSubsystem` avec constructeur **package-private**.
2. Déclarer le subsystem dans `PortalManager.SUBSYSTEMS_REGISTRATION` (liste constante).
3. Implémenter `getRegisteredPhases()` et `getPriority()` sans collision avec subsystems existants.
4. Documenter quels `Bridge` sont requis.
5. Définir un `// FALLBACK:` si le subsystem skip à cause d'un bridge `DEGRADED`.

---

## §16. Anti-patterns IA

Au-delà des anti-patterns code (§7), voici les anti-patterns d'**IA générative** à éviter.

### AIP-1 : L'invention de classe Veil/Iris

L'IA ne **devine** pas les noms de classes Veil ou Iris. Si elle a besoin de `AdvancedFbo` ou `ShaderInstance`, elle :

1. Vérifie dans `phase0_findings.md` que la version Veil retenue expose bien cette classe.
2. Si non disponible, refuse de générer le code et signale le manque.

### AIP-2 : Le refactor invisible

L'IA ne renomme pas une classe ou un package "pour la cohérence" sans demande explicite. Tout refactor structurel passe par une demande utilisateur séparée.

### AIP-3 : L'optimisation prématurée

L'IA n'ajoute pas de cache, de pool d'objets, de lazy init "au cas où". L'optimisation se fait après mesure (`PerformanceMonitor`) et avec un `// PERF:` justifiant.

### AIP-4 : Le commentaire générateur

```java
// ❌ INTERDIT
// This method handles the portal teleportation
public void teleport(...) { ... }
```

Les commentaires qui répètent le nom de la méthode sont **interdits**. Une javadoc utile dit pourquoi, pas quoi.

```java
// ✅ CORRECT
/**
 * Frame-exact TP. Doit être appelé en POST_RENDER pour éviter le flicker
 * entre le rendu et le déplacement du joueur.
 *
 * FALLBACK: si le chunk de destination n'est pas chargé, le TP est annulé
 * et un PortalEvent.TeleportFailed est publié.
 */
public void teleport(...) { ... }
```

### AIP-5 : L'invention de méthode utilitaire

Avant de créer un helper dans `util/`, l'IA vérifie qu'il n'existe pas déjà (`grep -r "<methodName>" src/main/java/com/petassegang/addons/util/`). Pas de duplication.

### AIP-6 : L'extension hors scope

Si l'utilisateur demande "ajoute Phase 1", l'IA ne génère **pas** Phase 2 par anticipation. Elle s'arrête au scope demandé et propose Phase 2 comme prochaine action.

### AIP-7 : Le mock implicite

Si l'IA n'a pas accès à la classe Veil réelle, elle crée des **stubs typés** dans le bridge plutôt que des `Object` ou `null`.

```java
// ❌ INTERDIT
public class VeilFramebufferBridge {
    public Object createFramebuffer(...) { return null; }
}

// ✅ CORRECT
public class VeilFramebufferBridge {
    public FramebufferHandle createFramebuffer(...) {
        // SPEC-GAP: implémentation Veil à valider en Phase 0
        throw new UnsupportedOperationException("Pending Phase 0 spike");
    }
}
public record FramebufferHandle(int id, int width, int height) {}
```

---

## §17. Checklist de revue avant commit

À parcourir mentalement par l'IA avant de proposer un commit. Si une case est ❌, le code ne doit pas être commit en l'état.

### Architecture

- [ ] Aucun import Veil/Iris hors de `backrooms/bridge/`
- [ ] Aucun appel `subsystem.tick()` hors de `PortalManager.dispatch()`
- [ ] Aucune classe `Lifecycle` ne s'auto-démarre
- [ ] Tout TP passe par `TeleportExecutor`
- [ ] EntityEcho ne mute pas l'entité réelle
- [ ] AggroSimulationSystem cible l'entité réelle, pas le proxy
- [ ] Constructeurs des `PortalSubsystem` sont package-private
- [ ] Aucune ressource OpenGL allouée dans un constructeur
- [ ] Aucun handler `ModEventBus` ne commande `PortalManager` / `TeleportExecutor` / `tick()` (cf. AP-9)
- [ ] `recovery/` ne contient rien d'autre que `IrisStateStore` (sauf justification documentée)

### Sources of Truth

- [ ] Aucune copie locale d'une donnée listée dans §5
- [ ] Toute classe propriétaire d'une SoT est annotée `@SourceOfTruth`
- [ ] Les lecteurs lisent via la SoT, pas via un cache

### Conventions

- [ ] Pas de `// TODO` nu
- [ ] Tous les nouveaux fichiers ont une javadoc de classe
- [ ] Nommage respecté (§9)
- [ ] Identifiers utilisent `petasse_gang_additions` comme namespace

### Lifecycle

- [ ] Toute classe avec ressources implémente `Lifecycle`
- [ ] `stop()` est idempotent
- [ ] Allocations GL dans `init()`, libérations dans `stop()`

### Tests

- [ ] Classes pures : tests JUnit créés
- [ ] Aucun mock de `World`, `BlockPos`, `Entity`
- [ ] Build vert : `./gradlew build`

### Documentation

- [ ] `// FALLBACK:` présent si comportement dégradé prévu
- [ ] `// SPEC-GAP: §X` si feature spec partielle
- [ ] Mapping spec → code maintenu si nouvelle classe Backrooms

---

## §18. Prompts d'amorçage par phase

À coller dans Codex/Claude Code/Cursor au début de chaque session.

### §18.1 Prompt universel

```
Contexte : projet Minecraft Fabric "PétasseGang Addons" — mod ambitieux avec
intégration Veil pour shaders custom et portails illusionnistes (V4.3).

AVANT toute action de code, lis ces 3 fichiers dans cet ordre :
1. docs/ARCHITECTURE.md (ce fichier — spec exécutable)
2. docs/spec_veil_portail_shaders_V4_3.md (spec portail détaillée)
3. docs/spec_shaders_backrooms_V2.md (spec shader Backrooms)

Tu DOIS respecter :
- L'arborescence canonique §3
- Les règles d'or §6 et anti-patterns §7
- Les conventions de nommage §9
- Les anti-patterns IA §16
- La checklist de revue §17 avant chaque commit

Si une demande utilisateur viole une règle, demande confirmation avant
d'exécuter, en citant la règle (protocole §21).

Ma première demande est : <DEMANDE ICI>
```

### §18.2 Prompt Phase 0 (spikes)

```
PHASE 0 — Spikes obligatoires.

Objectif : produire `docs/phase0_findings.md` qui documente :
1. Version Veil retenue (lock définitif)
2. Liste des hooks Fabric utilisables avec ordre d'exécution
3. Verdict Iris toggle : AUTO_TOGGLE validé ou WARN_ONLY forcé
4. Compat Sodium : OK/KO/restrictions
5. Snippets de code de référence pour chaque bridge

Ne génère AUCUN code dans backrooms/* tant que ce fichier n'existe pas.
Le code de Phase 0 vit dans :
- backrooms/bridge/ (stubs minimaux : infrastructure commune, 5 bridges Veil, IrisBridge, IrisStateStore)
- src/test/java/.../bridge/ (tests des stubs)

Critère de succès : `./gradlew build` vert + phase0_findings.md complet.

Action immédiate : lis le build.gradle existant et propose une stratégie
d'investigation.
```

### §18.3 Prompt Phase 1 (Pilier 1)

```
PHASE 1 — Pilier 1 : config + bridges + Lifecycle + Tick.

Pré-requis non-négociable : `docs/phase0_findings.md` doit exister et être
validé. Vérifie sa présence AVANT toute action. S'il est absent, refuse
d'avancer et redirige vers le prompt Phase 0.

Modules à implémenter dans cet ordre :
1. core/lifecycle/ : Lifecycle, LifecycleManager, LifecyclePhase, LifecycleException
2. core/tick/ : TickPhase, TickDispatcher
3. core/annotation/ : SourceOfTruth, ClientOnly, ServerOnly, PortalCritical
4. core/event/ : ModEventBus, ModEvent
5. config/ + backrooms/config/ : config Lifecycle-aware
6. backrooms/bridge/ : infrastructure commune, 5 bridges Veil, IrisBridge, IrisStateStore

IMPORTANT : `BackroomsConfig.enableBackroomsFeature` reste à `false` à la fin
de cette phase. Ne le passe PAS à `true` — c'est un commit séparé utilisateur
après validation Phase 0 (cf. §12 verrou Phase 0).

Critère de succès : runClient lance le mod sans crash, logs montrent
l'ordre Lifecycle correct (init → start), bridges remontent un BridgeState,
log "[Backrooms] Feature désactivée" est affiché.

Aucun rendu, aucun portail, aucun shader pour cette phase.

Action immédiate : génère core/lifecycle/Lifecycle.java en partant des
contrats §4.1.
```

### §18.4 Prompt Phase 4 (premier portail rendu)

```
PHASE 4 — PortalViewSystem : premier portail visuellement rendu.

Pré-requis : Phases 1-3 validées.

Objectif : un portail (paire de positions hardcodées en debug) affiche
visuellement la vue de l'autre côté via render-to-texture Veil.

Modules à implémenter :
1. backrooms/portal/client/view/PortalFramebuffer (via VeilFramebufferBridge)
2. backrooms/portal/client/view/RemoteCamera + RemoteCameraGuard
3. backrooms/portal/client/view/PortalViewSystem (implements PortalSubsystem)
4. backrooms/portal/client/view/PortalSurfaceRenderer
5. backrooms/portal/client/view/PortalDepthTracker (pas de récursion §27 spec)
6. assets/.../veil/shaders/portal/portal_view.{fsh,vsh,json}

Contraintes :
- PortalViewSystem est instancié et tickké UNIQUEMENT par PortalManager
- PortalDepthTracker bloque toute récursion (renderingPortalDepth > 1)
- RemoteCameraGuard gère les cas extrêmes §14 spec V4.3

Critère de succès : runClient → commande debug `/backrooms portal spawn_test`
→ voir le portail afficher la vue distante. Pas de TP encore.

Action immédiate : implémente PortalViewSystem.java en partant du contrat
PortalSubsystem (§4.2).
```

### §18.5 Prompt audit

```
AUDIT du codebase — pas de génération de code.

Parcours tout src/main/java/com/petassegang/addons/ et liste les violations
de la checklist §17.

Format de sortie :
- Une violation par ligne
- Format : <fichier>:<ligne> — <règle violée> — <suggestion>

Ne corrige RIEN. Produis juste la liste.

Si la liste est vide, retourne "OK — aucune violation détectée".
```

### §18.6 Prompt "implémente une feature"

```
Tâche : implémenter <FEATURE>.

Avant de coder :
1. Localise le package cible dans §3
2. Identifie les contrats à implémenter (Lifecycle ? PortalSubsystem ?)
3. Liste les SoT impactées (§5)
4. Vérifie les anti-patterns à éviter (§7 + §16)
5. Propose un plan en 3-5 étapes AVANT de coder

Attends ma validation du plan avant de générer le code.
```

---

## §19. Format de réponse attendu

Quand l'IA propose une modification, elle structure sa réponse ainsi :

```
## Plan
<3-5 étapes maximum>

## Fichiers touchés
- <fichier 1> : <action>
- <fichier 2> : <action>

## Règles vérifiées
- Règle d'or n°<X> : <comment respect>
- Anti-pattern AP-<Y> : <comment éviter>

## Code

[blocs de code]

## Vérifications à faire
- [ ] `./gradlew build`
- [ ] runClient
- [ ] <test spécifique>

## Suite suggérée
<prochaine action logique, sans la faire>
```

---

## §20. Limites connues et zones grises

L'IA est consciente de ces limites :

1. **API Veil exacte** : à valider en Phase 0. Tant que `phase0_findings.md` n'existe pas, l'IA utilise des stubs `// SPEC-GAP: §10`.
2. **API Iris toggle** : potentiellement absente. Si absente → `WARN_ONLY` (cf. spec V4.3 §10).
3. **Performances réelles** : non mesurables sans benchmark. L'IA ne fait pas de claim "ce code est rapide" — elle propose une mesure.
4. **Compat Sodium** : à valider en Phase 0. Si KO, certaines features sont désactivées par config.
5. **Frame-exact réel** : dépend des hooks Fabric précis disponibles. Phase 0.5 doit identifier le bon hook.

Quand l'IA travaille dans une zone grise, elle le **dit** dans sa réponse.

---

## §21. Protocole de demande de clarification

Si l'IA ne peut pas exécuter une demande sans violer une règle, elle répond :

```
🛑 Cette demande entre en conflit avec <règle violée>.

Contexte : <pourquoi le conflit>

Options possibles :
1. <option qui respecte la règle>
2. <option alternative>
3. Modifier la règle (nécessite un commit séparé sur ARCHITECTURE.md)

Quelle option veux-tu ?
```

Elle ne génère **jamais** de code qui viole une règle d'or sans confirmation explicite.

---

# ANNEXES

## §22. Glossaire

- **SoT** : Source of Truth. Classe unique propriétaire d'une donnée.
- **Bridge** : isolation d'une dépendance externe (Veil, Iris).
- **PortalSubsystem** : sous-système orchestré par `PortalManager`.
- **TickPhase** : moment du frame où un code s'exécute.
- **Frame-exact** : exécution garantie sur le même frame que le rendu visuel correspondant.
- **Echo** : représentation visuelle (lecture seule) d'une entité distante vue à travers un portail.
- **Aggro stimulus** : signal sensoriel envoyé à une IA pour déclencher une réaction.
- **NoClip** : franchissement involontaire d'un mur déclenchant un changement de dimension.
- **PBR** : Physically Based Rendering. Convention LabPBR dans ce projet.
- **LabPBR** : standard de packing texture spécifique au modding Minecraft (suffixes `_n`, `_s`, `_e`).

---

*Fin du document.*
