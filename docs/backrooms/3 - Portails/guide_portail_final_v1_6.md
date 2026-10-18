# Guide d'implémentation — Portails Backrooms (V1.6, Codex-ready)

## Fabric 1.21.1 · Java 21 · Architecture inspirée d'`iPortalTeam/ImmersivePortalsMod` (Apache License 2.0)

> **Version** : v1.6, Codex-ready. Remplace toutes les versions précédentes (`v3`, `v1`, `v1.1` → `v1.5`) pour le Pilier 3 (Portail).
> **Référence supérieure** : `arborescence_v4.md` (autorité rang 1).
> **Cible** : Minecraft 1.21.1, **Fabric**, Java 21, Veil 3.x (uniquement Pilier 2 — shaders d'ambiance).
> **Public visé** : agent IA codeur (Claude Code, Codex) capable d'implémenter file-by-file à partir de ce document.
> **Changements vs v1.5** : règle critique de Minecraft mod design — `BackroomsPortalEntityType.register()` est **toujours** exécuté au boot, hors du flag `enablePortalFeature`. Le flag gate uniquement les **systèmes runtime** (`Registry.init`, `DestinationLoader.init`, `TeleportExecutor.init`, `DebugPortalCommand.register`). Sans ça, un monde sauvegardé contenant des `BackroomsPortalEntity` planterait au load si on désactive la feature. Le bloc canonique est en §6.3 ; §13 pointe vers lui.
> **Changements vs v1.4** : champ `LOGGER` (slf4j) déclaré dans `PortalStencilRenderer`.
> **Changements vs v1.3** : bug fix runtime intra-dim (`getRenderer()` jamais null pour la dim courante, null-check défensif), pipeline GL aligné sur `GL_REPLACE`.
> **Changements vs v1.2** : marqueur `PortalFacing` honnête, mentions `V1.1` nettoyées, grep C-6 exclut `BackroomsPortalEntity.java`, chemin canonique `portal.block.*`, grep C-4 limité au sous-package portail.
> **Changements vs v1.1** : bug fix `detectionBox()`/`signedDistance()`, numérotation SPIKE-N harmonisée, note `GL_REPLACE`/`GL_INCR`, gate `DebugPortalCommand` visible dans §6.3.
> **Changements vs v1** : convention de marqueurs inversée, Phase 0 portée à 8 spikes, sync entity côté client durcie, `transformLook()` pour UP/DOWN, `try/finally` règle dure, `enableBackroomsFeature` → `enablePortalFeature`, `TpCooldownTracker` config-driven, `DebugPortalCommand` dev-only, Codex Contract minimal grep-able.

---

## Conventions de lecture — IMPORTANT

> **Tout bloc de code Java de ce document est par défaut un `CODE TEMPLATE`.**
> Il **doit** être vérifié contre les sources officielles (Yarn 1.21.1, sources Fabric API, sources IP) **avant** d'être considéré comme compilable.
> Seuls les blocs explicitement marqués `CODE COMPLETE` sont garantis compilables tels quels.

| Marqueur en haut de bloc | Sens | Action attendue |
|---|---|---|
| `// CODE TEMPLATE — VERIFY BEFORE COMPILE` (par défaut, **implicite si rien**) | Squelette logique correct, mais signatures Yarn/Fabric à valider. Peut contenir des appels vers des classes existantes dans le code base ou des SPIKE-N. | Vérifier chaque import et chaque appel de méthode contre Yarn 1.21.1. Confirmer le SPIKE-N référencé en `OK` dans `docs/phase0_findings.md`. |
| `// CODE COMPLETE — pure Java, no Yarn deps` | Bloc 100% Java standard, sans dépendance Minecraft fragile. Compile tel quel. | Implémenter sans modification autre que les imports. |
| `// CODE COMPLETE — compiles after SPIKE-N OK` | Compile tel quel **une fois** que le spike référencé est validé. | Implémenter tel quel, post-spike. |
| `// SPIKE-N` (en ligne) | Référence à un spike Phase 0 §15. La signature exacte ou le comportement à cet endroit dépend du spike. | Si le spike est `BLOQUÉ` ou non joué, ne pas avancer. |
| `// FALLBACK: <comportement>` | Comportement dégradé volontaire si une dépendance échoue. | Implémenter explicitement, jamais un `try/catch` muet. |
| `// IP: <fichier>` | Référence au fichier source d'ImmersivePortals (branche `1.21`) à consulter pour la logique exacte. | Lire le fichier IP avant d'écrire la version adaptée. |

**Conventions du projet GTG's Backrooms** (rappel) :
- Commentaires Java en **français**, **capitalisés**, **terminés par un point**.
- Messages d'erreur en **français**, capitalisés, ponctués.
- Tout texte in-game traduit dans `en_us.json` ET `fr_fr.json`.
- Mod ID : `gtgs_backrooms`.
- Package racine : `com.gtgpuke.gtgsbackrooms.*`.

---

## Table des matières

- §1. Périmètre V1
- §2. Décisions architecturales
- §2bis. **Codex Contract minimal** (10 interdictions grep-able)
- §3. Architecture — schéma des dépendances
- §4. Arborescence cible et setup projet
- §5. `PortalFacing` — enum, math, transformLook
- §6. `BackroomsPortalEntity` — entity, builder, NBT, sync client, type registration
- §7. `BackroomsPortalEntityRegistry` — lookup UUID
- §8. `ClientWorldRegistry` — multi-dimension côté client
- §9. Rendu : `PortalStencilRenderer` + `ViewAreaRenderer` + `RemoteWorldRenderInfo`
- §10. Mixins — `LevelRenderer`, `Framebuffer`, accessors
- §11. `DestinationLoader` — préchargement chunks
- §12. `TeleportExecutor` + `TpCooldownTracker` (config-driven)
- §13. `BackroomsConfig` — `enablePortalFeature` + `PortalConfig`
- §14. Patterns de spawn — direct + `BackroomsDoorBlockEntity` + `DebugPortalCommand` (dev-only)
- §15. Phase 0 — **8 spikes** vérifiables (avec dépendances + ordre)
- §16. Tests JUnit — fichier complet `PortalFacingTest`
- §17. Roadmap + **ordre d'implémentation file-by-file**
- §18. Règles minimales (anti-pièges critiques)
- §19. Licence Apache 2.0

---

## §1. Périmètre V1

### Dans le scope

1. Frame de portail rectangulaire, alignée à l'une des 6 directions cardinales (N, S, E, O, UP, DOWN).
2. Affichage de la vue distante dans le rectangle, intra ou inter-dimension.
3. Téléportation instantanée (1 tick serveur) sans écran de chargement.
4. Conservation de la vélocité, transformée selon la rotation source → destination.
5. Préchargement des chunks destination via tickets serveur.
6. Spawn scripté uniquement (génération de salle ou `BlockEntity` de porte).

### Hors scope V1

| Hors scope | Réintroduction prévue |
|---|---|
| Récursion (portail dans portail) — vue noire au-delà du niveau 0 | V2 |
| `EntityEchoSystem` (entités à travers le portail) | V2 |
| `CrossSpaceAudioSystem` (audio cross-portail) | V2 |
| `AggroSimulationSystem`, `PortalLureGoal` | V2 |
| `LoopingCorridorManager` dédié | Cas d'usage du portail V1 (portail invisible same-dim) |
| `TransitionMasker` (fade TP shader Veil) | V1.5 si le mismatch tick est trop voyant |
| Multi-joueurs | V2+ |

---

## §2. Décisions architecturales

| # | Décision | Conséquence |
|---|---|---|
| **DA-1** | Le portail est une `Entity` Minecraft, NBT-persisted, sync via le système Entity vanilla. | Lifecycle géré par les hooks vanilla. Pas de classe `PortalLink`. |
| **DA-2** | Orientation = enum `PortalFacing` à 6 valeurs (N/S/E/O/UP/DOWN). Pas de quaternion. | Math triviale, pas de `DQuaternion`. Tests exhaustifs en JUnit (36 combinaisons). |
| **DA-3** | Rendu = stencil buffer + Mixin `LevelRenderer` sur main framebuffer. Code adapté d'IP. | Pas de framebuffer offscreen Veil pour le portail. |
| **DA-4** | Pas de récursion (`maxDepth = 0`). Vue distante d'un portail dans un portail = noire. | Retire la complexité fallback bruit/clipping multi-niveau. |
| **DA-5** | TP server-tick-exact via `TeleportExecutor` dans `ServerTickEvents`. Vélocité transformée par `PortalFacing.transformVelocity()`. | Pas de masking en V1 (cf. DA-7). |
| **DA-6** | Code IP copié et adapté depuis branche `1.21` de `iPortalTeam/ImmersivePortalsMod`. Pas en dépendance Maven. | Header Apache 2.0 obligatoire sur chaque fichier. |
| **DA-7** | Mismatch tick TP accepté en V1 (single-player local = quasi invisible). | Si visible en jeu → V1.5 ajoute `TransitionMasker` sans changer l'archi. |
| **DA-8** | Spawn scripté uniquement, deux voies : génération `RoomGraph` ou `BlockEntity` (porte). | Pas d'item, pas de placement joueur. Commande debug `/backrooms portal spawn_test` désactivée en release. |

---

## §2bis. Codex Contract minimal (10 interdictions grep-able)

Ces 10 règles sont **dures**. Chaque PR doit passer la vérif `tools/check_portal_contract.sh` (script à créer en Phase 1) qui exécute les `grep` ci-dessous. Une violation = échec build CI.

| # | Interdiction | Vérif grep |
|---|---|---|
| **C-1** | Aucun `import foundry.veil` dans `backrooms/portal/` (le portail n'utilise pas Veil — DA-3, Pilier 2 only). | `grep -r "import foundry.veil" src/main/java/com/gtgpuke/gtgsbackrooms/backrooms/portal && exit 1` |
| **C-2** | Aucun appel direct à `level()`, `world()`, ou setter sur `Minecraft.client` en dehors d'un bloc `try { ... } finally { /* restore */ }`. | Code review obligatoire pour tout `client.world =` ou `client.worldRenderer =`. Pattern attendu : `try/finally` autour. |
| **C-3** | `TeleportExecutor.execute()` invocable uniquement depuis `ServerTickEvents.END_SERVER_TICK` ou `server.execute()`. Jamais depuis le render thread, `WorldRenderEvents`, ou un Mixin client. | `grep -rn "TeleportExecutor.execute" src/main/java/com/gtgpuke/gtgsbackrooms/backrooms/portal/client && exit 1` (rien attendu côté client) |
| **C-4** | Aucune classe nommée `PortalLink`, `PortalTransform`, `DQuaternion`, `EntityEcho*`, `CrossPortalSound*`, `PortalLureGoal` **dans `backrooms/portal/`**. (Concepts V3 non réintroduits dans le périmètre MVP courant. Le scope est restreint au sous-package portail pour ne pas casser un éventuel `DQuaternion` utilitaire d'un autre Pilier.) | `grep -rn "class PortalLink\|class PortalTransform\|class DQuaternion\|class EntityEcho\|class CrossPortalSound\|class PortalLureGoal" src/main/java/com/gtgpuke/gtgsbackrooms/backrooms/portal && exit 1` |
| **C-5** | Aucune allocation OpenGL (`GlStateManager._glGen*`, `new Framebuffer`, `new SimpleFramebuffer`) dans un constructeur. Toujours dans `init()` appelé depuis le render thread. | Code review : tout `glGen` / `new Framebuffer` doit être dans une méthode `init()` ou équivalent. |
| **C-6** | Création de `BackroomsPortalEntity` autorisée uniquement depuis : `RoomGraph` (génération), `BackroomsDoorBlockEntity`, ou `DebugPortalCommand` (dev-only). Pas ailleurs. **Le fichier `BackroomsPortalEntity.java` lui-même est exclu** car son Builder contient légitimement `new BackroomsPortalEntity(...)`. | `grep -rn "BackroomsPortalEntity.builder\|new BackroomsPortalEntity(" src/main/java \| grep -v "BackroomsPortalEntity\.java\|RoomGraph\|BackroomsDoorBlockEntity\|DebugPortalCommand\|PortalFacingTest" && exit 1` |
| **C-7** | Pas de signature Yarn 1.21.1 inventée. Toute classe ou méthode non-vanilla-évidente est tagguée `// SPIKE-N` validé dans `phase0_findings.md`. | Code review. Si Codex écrit `import foo.bar.NotARealClass`, build casse — la CI Gradle attrape. |
| **C-8** | Sync entity côté client : si `facingSource`, `facingDestination`, `destinationDim`, `destinationPos`, `width`, `height` ne sont pas visibles côté client immédiatement après spawn, **bloqué**. Pas de fallback silencieux. | SPIKE-4 obligatoire en `OK`. Voir §6 pour les 3 stratégies (`TrackedData`, `SpawnExtraData`, `PortalSpawnPayload` custom). |
| **C-9** | `DebugPortalCommand` enregistré uniquement si `FabricLoader.getInstance().isDevelopmentEnvironment() == true`. | `grep -A 2 "DebugPortalCommand.register" src/main/java | grep "isDevelopmentEnvironment"` doit matcher. |
| **C-10** | Aucun `Thread.sleep`, `wait()`, `join()` dans le code portail (TP doit être tick-instant, pas async). | `grep -rn "Thread.sleep\|\.wait(\|\.join(" src/main/java/com/gtgpuke/gtgsbackrooms/backrooms/portal && exit 1` |

> Le script `tools/check_portal_contract.sh` est livré en Phase 1. Tant qu'il n'existe pas, l'agent est responsable de respecter ces 10 règles à la main.

---

## §3. Architecture — schéma des dépendances

```
                     ┌───────────────────────────────┐
                     │ BackroomsPortalEntity (SoT)   │← spawn par RoomGraph
                     │  pos, facing, dest, dim       │  ou BackroomsDoorBlockEntity
                     │  width, height, teleportable  │
                     └───────┬─────────┬─────────────┘
                             │         │
              ┌──────────────┘         └──────────────┐
              │                                       │
              │ resolution UUID                       │
              ▼                                       ▼
   ┌──────────────────────────┐         ┌────────────────────────────┐
   │ BackroomsPortalEntity-   │         │ Détection traversée +      │
   │ Registry (lookup UUID)   │         │ tick système (§12)         │
   │  ServerTickEvents        │         └────┬───────────────────────┘
   └──────────────────────────┘              │
                                             │
                ┌────────────────────────────┼─────────────────────────────┐
                │                            │                             │
                ▼ CLIENT                     ▼ SERVER                      ▼ SERVER
   ┌────────────────────────┐  ┌──────────────────────┐  ┌────────────────────────┐
   │ PortalStencilRenderer  │  │ DestinationLoader    │  │ TeleportExecutor       │
   │  + ViewAreaRenderer    │  │  TicketType<UUID>    │  │  + TpCooldownTracker   │
   │  + Mixin LevelRenderer │  │  préchargement       │  │  TP serveur + vélocité │
   └────────────┬───────────┘  └──────────────────────┘  └────────────────────────┘
                │
                ▼
   ┌────────────────────────┐
   │ ClientWorldRegistry    │
   │  Map<dim, ClientLevel> │
   │  Lifecycle CLIENT      │
   └────────────────────────┘

   PortalFacing (enum) — utilisé par TOUS pour transformPoint / transformVelocity
   Mixins : LevelRenderer (rendu), Framebuffer (stencil), accessors
```

---

## §4. Arborescence cible et setup projet

### §4.1 Fichiers à créer

```
src/main/java/com/gtgpuke/gtgsbackrooms/
│
├── GtgsBackroomsMod.java                              # Main mod class — MAJ §6.3
├── GtgsBackroomsClientMod.java                        # Client init — MAJ §8.5
│
├── backrooms/
│   ├── BackroomsConfig.java                            # MAJ avec section portail (§13)
│   │
│   └── portal/
│       ├── entity/
│       │   ├── BackroomsPortalEntity.java              # §6.1
│       │   ├── BackroomsPortalEntityType.java          # §6.2
│       │   ├── BackroomsPortalEntityRegistry.java      # §7
│       │   └── PortalFacing.java                       # §5
│       │
│       ├── client/
│       │   ├── world/
│       │   │   ├── ClientWorldRegistry.java            # §8.1
│       │   │   └── RemoteLevelRenderer.java            # §8.3
│       │   └── view/
│       │       ├── PortalStencilRenderer.java          # §9.1
│       │       ├── ViewAreaRenderer.java               # §9.2
│       │       ├── RemoteWorldRenderInfo.java          # §9.3
│       │       └── PortalDepthTracker.java             # §9.4
│       │
│       ├── transition/
│       │   ├── DestinationLoader.java                  # §11
│       │   ├── TeleportExecutor.java                   # §12.1
│       │   └── TpCooldownTracker.java                  # §12.2
│       │
│       ├── block/
│       │   ├── BackroomsDoorBlock.java                 # §14.2
│       │   └── BackroomsDoorBlockEntity.java           # §14.2
│       │
│       └── debug/
│           └── DebugPortalCommand.java                 # §14.4 (dev-only)
│
└── mixin/client/
    ├── MixinLevelRenderer.java                         # §10.1
    ├── MixinFramebuffer.java                           # §10.2
    ├── MinecraftClientAccessor.java                    # §10.3
    └── GameRendererAccessor.java                       # §10.4

src/main/resources/
├── fabric.mod.json                                     # MAJ §4.3
├── gtgs_backrooms.mixins.json                  # MAJ §4.4
└── assets/gtgs_backrooms/lang/
    ├── en_us.json                                      # +clés portail
    └── fr_fr.json                                      # +clés portail

src/test/java/com/gtgpuke/gtgsbackrooms/backrooms/portal/entity/
└── PortalFacingTest.java                               # §16

docs/
└── phase0_findings.md                                  # créé après spikes §15
```

### §4.2 `build.gradle` — bloc `dependencies` à compléter

```gradle
dependencies {
    minecraft "com.mojang:minecraft:1.21.1"
    mappings "net.fabricmc:yarn:1.21.1+build.3:v2"   // SPIKE-1 : confirmer build.X exact
    modImplementation "net.fabricmc:fabric-loader:0.16.0"
    modImplementation "net.fabricmc.fabric-api:fabric-api:0.105.0+1.21.1"

    // Veil — Pilier 2 uniquement, déjà présent.
    // modImplementation "foundry.veil:Veil-fabric-1.21.1:VERSION"

    // Tests.
    testImplementation "org.junit.jupiter:junit-jupiter:5.10.0"
}

test { useJUnitPlatform() }
```

### §4.3 `fabric.mod.json` — entrypoints

```json
{
    "entrypoints": {
        "main":   ["com.gtgpuke.gtgsbackrooms.GtgsBackroomsMod"],
        "client": ["com.gtgpuke.gtgsbackrooms.GtgsBackroomsClientMod"]
    },
    "mixins": ["gtgs_backrooms.mixins.json"]
}
```

### §4.4 `gtgs_backrooms.mixins.json`

```json
{
    "required": true,
    "minVersion": "0.8",
    "package": "com.gtgpuke.gtgsbackrooms.mixin",
    "compatibilityLevel": "JAVA_21",
    "client": [
        "client.MixinLevelRenderer",
        "client.MixinFramebuffer",
        "client.MinecraftClientAccessor",
        "client.GameRendererAccessor"
    ],
    "mixins": [],
    "injectors": { "defaultRequire": 1 }
}
```

### §4.5 Clés de traduction à ajouter

`en_us.json` :
```json
{
    "entity.gtgs_backrooms.backrooms_portal": "Backrooms Portal",
    "block.gtgs_backrooms.backrooms_door": "Backrooms Door"
}
```

`fr_fr.json` :
```json
{
    "entity.gtgs_backrooms.backrooms_portal": "Portail des Backrooms",
    "block.gtgs_backrooms.backrooms_door": "Porte des Backrooms"
}
```

---

## §5. `PortalFacing` — enum et math

C'est la pièce qui simplifie tout le système. Pas de quaternion. Code complet, prêt à compiler.

### §5.1 Le fichier complet

```java
package com.gtgpuke.gtgsbackrooms.backrooms.portal.entity;

import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * Orientation d'un portail aligné à l'une des 6 directions cardinales.
 *
 * Convention : la normale pointe vers l'observateur (côté visible du portail).
 * Les trois axes (axisW, axisH, normal) forment une base orthonormée directe.
 *
 * axisW = axe horizontal du rectangle (largeur).
 * axisH = axe vertical du rectangle (hauteur).
 * normal = axisW × axisH (produit vectoriel).
 */
public enum PortalFacing {
    NORTH(Direction.NORTH, new Vec3d(-1,  0,  0), new Vec3d( 0,  1,  0)),
    SOUTH(Direction.SOUTH, new Vec3d( 1,  0,  0), new Vec3d( 0,  1,  0)),
    EAST (Direction.EAST,  new Vec3d( 0,  0, -1), new Vec3d( 0,  1,  0)),
    WEST (Direction.WEST,  new Vec3d( 0,  0,  1), new Vec3d( 0,  1,  0)),
    UP   (Direction.UP,    new Vec3d( 1,  0,  0), new Vec3d( 0,  0,  1)),
    DOWN (Direction.DOWN,  new Vec3d( 1,  0,  0), new Vec3d( 0,  0, -1));

    public final Direction direction;
    public final Vec3d axisW;
    public final Vec3d axisH;
    public final Vec3d normal;

    PortalFacing(Direction direction, Vec3d axisW, Vec3d axisH) {
        this.direction = direction;
        this.axisW = axisW;
        this.axisH = axisH;
        this.normal = axisW.crossProduct(axisH);
    }

    /**
     * Retourne le PortalFacing correspondant à une Direction vanilla.
     * Utile pour BackroomsDoorBlock qui lit son orientation depuis BlockState.
     */
    public static PortalFacing fromDirection(Direction d) {
        for (PortalFacing f : values()) {
            if (f.direction == d) return f;
        }
        throw new IllegalArgumentException("Direction non gérée : " + d + ".");
    }

    /**
     * Transforme un point monde du repère source vers le repère destination.
     *
     * Logique : on décompose le point dans la base locale source, on inverse
     * la composante normale (passage de l'autre côté du plan), puis on recompose
     * dans la base destination.
     */
    public static Vec3d transformPoint(
        PortalFacing src, Vec3d srcCenter,
        PortalFacing dst, Vec3d dstCenter,
        Vec3d worldPoint
    ) {
        Vec3d local = worldPoint.subtract(srcCenter);
        double w = local.dotProduct(src.axisW);
        double h = local.dotProduct(src.axisH);
        double n = local.dotProduct(src.normal);
        Vec3d recomposed = dst.axisW.multiply(w)
            .add(dst.axisH.multiply(h))
            .add(dst.normal.multiply(-n));
        return dstCenter.add(recomposed);
    }

    /**
     * Transforme un vecteur de vélocité (rotation pure, sans translation).
     */
    public static Vec3d transformVelocity(
        PortalFacing src, PortalFacing dst, Vec3d velocity
    ) {
        double w = velocity.dotProduct(src.axisW);
        double h = velocity.dotProduct(src.axisH);
        double n = velocity.dotProduct(src.normal);
        return dst.axisW.multiply(w)
            .add(dst.axisH.multiply(h))
            .add(dst.normal.multiply(-n));
    }

    /**
     * Transforme un yaw (rotation autour de Y, en degrés) entre deux portails horizontaux.
     * Pour les facings UP/DOWN, retourne le yaw inchangé (legacy V1 — voir transformLook
     * pour la version qui supporte UP/DOWN correctement).
     *
     * @deprecated Utiliser {@link #transformLook} qui couvre les 36 combinaisons.
     */
    @Deprecated
    public static float transformYaw(PortalFacing src, PortalFacing dst, float yaw) {
        if (src == dst) return yaw;
        if (src == UP || src == DOWN || dst == UP || dst == DOWN) return yaw;
        float delta = horizontalYaw(dst) - horizontalYaw(src) + 180.0f;
        return ((yaw + delta) % 360.0f + 360.0f) % 360.0f;
    }

    private static float horizontalYaw(PortalFacing f) {
        return switch (f) {
            case NORTH -> 180.0f;
            case SOUTH -> 0.0f;
            case EAST  -> -90.0f;
            case WEST  -> 90.0f;
            default    -> 0.0f;
        };
    }

    /**
     * Transforme l'orientation du regard (yaw, pitch) à travers un portail.
     * Couvre les 36 combinaisons de facings, y compris UP/DOWN.
     *
     * Algo :
     *   1. (yaw, pitch) → vecteur de regard via convention Minecraft.
     *   2. Le vecteur subit la même rotation que la vélocité ({@link #transformVelocity}).
     *   3. Vecteur → (yaw', pitch').
     *
     * Convention Minecraft Yarn 1.21.1 (à vérifier — SPIKE-8) :
     *   x = -sin(yaw_rad) * cos(pitch_rad)
     *   y = -sin(pitch_rad)
     *   z =  cos(yaw_rad) * cos(pitch_rad)
     *
     * Inversion :
     *   pitch = -asin(y)         (degrés)
     *   yaw   = atan2(-x, z)     (degrés)
     *
     * Limitation connue (gimbal lock) : si le vecteur transformé est quasi-vertical
     * (|y| > 0.9999), le yaw devient instable. La méthode clamp dans ces cas pour
     * éviter NaN, mais le yaw retourné peut sauter. Acceptable en V1 — l'utilisateur
     * regarde rarement strictement à la verticale au moment de la traversée.
     *
     * @return Vec2f où x = yaw transformé, y = pitch transformé (degrés).
     */
    public static net.minecraft.util.math.Vec2f transformLook(
        PortalFacing src, PortalFacing dst, float yaw, float pitch
    ) {
        // 1. (yaw, pitch) → vecteur de regard.
        double yawRad   = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        double cosPitch = Math.cos(pitchRad);
        Vec3d look = new Vec3d(
            -Math.sin(yawRad) * cosPitch,
            -Math.sin(pitchRad),
             Math.cos(yawRad) * cosPitch
        );

        // 2. Rotation cross-portail (mêmes axes que transformVelocity).
        Vec3d transformed = transformVelocity(src, dst, look);

        // 3. Vecteur → (yaw', pitch').
        double yClamped = Math.max(-1.0, Math.min(1.0, transformed.y));
        double newPitchRad = -Math.asin(yClamped);
        // Math.atan2 gère le cas (-x, z) = (0, 0) en retournant 0 (pas NaN).
        double newYawRad = Math.atan2(-transformed.x, transformed.z);

        return new net.minecraft.util.math.Vec2f(
            (float) Math.toDegrees(newYawRad),
            (float) Math.toDegrees(newPitchRad)
        );
    }
}
```

> **Note sur le marqueur** : le bloc ci-dessus est `// CODE COMPLETE — compiles after SPIKE-1 OK`
> (build + Yarn 1.21.1 résolu). Il dépend de Yarn (`Vec3d`, `Vec2f`, `Direction`), donc pas
> "pure Java". La logique math elle-même reste testable sans monde Minecraft tournant
> (`PortalFacingTest` ne lance pas de `MinecraftServer`).

### §5.2 Comportement attendu — exemples concrets

**Exemple 1 — porte simple (NORTH → SOUTH)** :
- Portail source à `(0, 64, 0)`, facing NORTH.
- Portail destination à `(100, 64, 0)`, facing SOUTH.
- Joueur à `(0, 65, 0.5)` avec vélocité `(0, 0, 0.5)`.
- Position transformée : `(100, 65, -0.5)`. Vélocité transformée : `(0, 0, -0.5)`.

**Exemple 2 — sol vers mur (UP → NORTH)** :
- Portail source au sol à `(0, 64, 0)`, facing UP.
- Portail destination sur mur à `(100, 100, 0)`, facing NORTH.
- Joueur à `(0, 64, 0)` en chute, vélocité `(0, -0.5, 0)`.
- Position transformée : `(100, 100, 0)`. Vélocité : `(0, 0, +0.5)` (jaillissement horizontal).

### §5.3 Tests JUnit

Voir §16 pour le fichier complet `PortalFacingTest.java` couvrant les 36 combinaisons.

---

## §6. `BackroomsPortalEntity` — entity, builder, NBT, type registration

### §6.1 Le fichier `BackroomsPortalEntity.java`

```java
package com.gtgpuke.gtgsbackrooms.backrooms.portal.entity;

import com.gtgpuke.gtgsbackrooms.core.annotation.SourceOfTruth;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Portail Backrooms — entity NBT-persisted, sync vanilla.
 *
 * Adapté de iPortalTeam.ImmersivePortalsMod.Portal (Apache License 2.0, voir §19).
 * IP: src/main/java/qouteall/imm_ptl/core/portal/Portal.java
 *
 * SoT pour : position, facing source/destination, dim/pos destination, taille,
 * teleportable, visible. Lifecycle = vanilla Entity.
 */
@SourceOfTruth
public class BackroomsPortalEntity extends Entity {

    // --- Clés NBT ---
    private static final String NBT_FACING_SRC  = "FacingSource";
    private static final String NBT_FACING_DST  = "FacingDestination";
    private static final String NBT_DEST_DIM    = "DestinationDim";
    private static final String NBT_DEST_X      = "DestinationX";
    private static final String NBT_DEST_Y      = "DestinationY";
    private static final String NBT_DEST_Z      = "DestinationZ";
    private static final String NBT_WIDTH       = "Width";
    private static final String NBT_HEIGHT      = "Height";
    private static final String NBT_TELEPORTABLE = "Teleportable";
    private static final String NBT_VISIBLE     = "Visible";

    // --- TrackedData (sync client) ---
    //
    // ⚠️ CONTRAT C-8 (§2bis) : si le client ne voit pas TOUS ces champs immédiatement
    // après spawn, le projet portail est BLOQUÉ. SPIKE-4 doit valider cette stratégie A
    // (TrackedData primitifs) AVANT la Phase 1.
    //
    // Stratégie A (par défaut, ci-dessous) : TrackedData primitifs uniquement, pas
    // de handler custom. La sync passe par le mécanisme DataTracker vanilla, qui
    // s'envoie automatiquement au client lors du spawn de l'entity (puis sur diff).
    //
    // Stratégie B (fallback si SPIKE-4 montre que TrackedData ne suffit pas pour les
    // 6 champs) : packet custom Fabric `PortalSpawnPayload` envoyé via
    // ServerPlayNetworking.send() dans un handler EntityTrackingEvents.START_TRACKING.
    // Dans ce cas, créer src/main/java/.../portal/network/PortalSpawnPayload.java
    // et basculer toute la sync dessus.
    //
    // Pas de fallback silencieux : si A et B échouent, projet portail BLOQUÉ.

    private static final TrackedData<Integer> TRACKED_FACING_SRC =
        DataTracker.registerData(BackroomsPortalEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> TRACKED_FACING_DST =
        DataTracker.registerData(BackroomsPortalEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<String> TRACKED_DEST_DIM =
        DataTracker.registerData(BackroomsPortalEntity.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<Float> TRACKED_DEST_X =
        DataTracker.registerData(BackroomsPortalEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> TRACKED_DEST_Y =
        DataTracker.registerData(BackroomsPortalEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> TRACKED_DEST_Z =
        DataTracker.registerData(BackroomsPortalEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> TRACKED_WIDTH =
        DataTracker.registerData(BackroomsPortalEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> TRACKED_HEIGHT =
        DataTracker.registerData(BackroomsPortalEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Boolean> TRACKED_VISIBLE =
        DataTracker.registerData(BackroomsPortalEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    // --- Champs SoT côté serveur uniquement (non synchronisés) ---
    // teleportable : utilisé par TpCooldownTracker pour gating server-side, pas besoin
    // que le client le sache. Tous les autres champs vivent dans dataTracker.
    private boolean teleportable = true;

    public BackroomsPortalEntity(EntityType<? extends BackroomsPortalEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.intersectionChecked = false;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        // Valeurs par défaut (écrasées au build du Builder via les setters DataTracker).
        builder.add(TRACKED_FACING_SRC, PortalFacing.NORTH.ordinal());
        builder.add(TRACKED_FACING_DST, PortalFacing.SOUTH.ordinal());
        builder.add(TRACKED_DEST_DIM,   "minecraft:overworld");
        builder.add(TRACKED_DEST_X,     0.0f);
        builder.add(TRACKED_DEST_Y,     0.0f);
        builder.add(TRACKED_DEST_Z,     0.0f);
        builder.add(TRACKED_WIDTH,      1.0f);
        builder.add(TRACKED_HEIGHT,     2.0f);
        builder.add(TRACKED_VISIBLE,    true);
    }

    // --- Accesseurs (read-only) ---
    // Lecture systématique depuis dataTracker → garantit la cohérence client/serveur.
    public PortalFacing facingSource() {
        return PortalFacing.values()[dataTracker.get(TRACKED_FACING_SRC)];
    }
    public PortalFacing facingDestination() {
        return PortalFacing.values()[dataTracker.get(TRACKED_FACING_DST)];
    }
    public RegistryKey<World> destinationDim() {
        Identifier id = Identifier.tryParse(dataTracker.get(TRACKED_DEST_DIM));
        return id == null ? null : RegistryKey.of(RegistryKeys.WORLD, id);
    }
    public Vec3d destinationPos() {
        return new Vec3d(
            dataTracker.get(TRACKED_DEST_X),
            dataTracker.get(TRACKED_DEST_Y),
            dataTracker.get(TRACKED_DEST_Z)
        );
    }
    public float width()         { return dataTracker.get(TRACKED_WIDTH); }
    public float height()        { return dataTracker.get(TRACKED_HEIGHT); }
    public boolean teleportable(){ return teleportable; }
    public boolean visible()     { return dataTracker.get(TRACKED_VISIBLE); }

    /**
     * Mutation autorisée en runtime : permet à TpCooldownTracker de désactiver
     * temporairement la téléportation après une traversée.
     */
    public void setTeleportable(boolean teleportable) {
        this.teleportable = teleportable;
    }

    /**
     * Box autour du plan du portail (épaisseur 0.5 bloc, dimensions width × height).
     * Utilisée pour la détection de traversée par TeleportExecutor.
     *
     * Les valeurs sont lues depuis le DataTracker (cohérence client/serveur).
     */
    public Box detectionBox() {
        PortalFacing fSrc = facingSource();
        Vec3d c = getPos();
        Vec3d w = fSrc.axisW.multiply(width()  / 2.0);
        Vec3d h = fSrc.axisH.multiply(height() / 2.0);
        Vec3d n = fSrc.normal.multiply(0.25);
        Vec3d c1 = c.subtract(w).subtract(h).subtract(n);
        Vec3d c2 = c.add(w).add(h).add(n);
        return new Box(c1, c2);
    }

    /**
     * Distance signée du point au plan du portail.
     * Positive côté observateur (normal), négative côté arrière.
     * Le changement de signe entre deux ticks → traversée détectée.
     */
    public double signedDistance(Vec3d point) {
        return point.subtract(getPos()).dotProduct(facingSource().normal);
    }

    // --- NBT (persistance disque, côté serveur uniquement) ---
    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        PortalFacing fSrc = PortalFacing.valueOf(nbt.getString(NBT_FACING_SRC));
        PortalFacing fDst = PortalFacing.valueOf(nbt.getString(NBT_FACING_DST));
        String       dim  = nbt.getString(NBT_DEST_DIM);
        float dx = (float) nbt.getDouble(NBT_DEST_X);
        float dy = (float) nbt.getDouble(NBT_DEST_Y);
        float dz = (float) nbt.getDouble(NBT_DEST_Z);
        float w  = nbt.getFloat(NBT_WIDTH);
        float h  = nbt.getFloat(NBT_HEIGHT);
        boolean tp = nbt.getBoolean(NBT_TELEPORTABLE);
        boolean vis = nbt.getBoolean(NBT_VISIBLE);

        // Repopule tous les TrackedData → sync auto au client à la prochaine frame.
        dataTracker.set(TRACKED_FACING_SRC, fSrc.ordinal());
        dataTracker.set(TRACKED_FACING_DST, fDst.ordinal());
        dataTracker.set(TRACKED_DEST_DIM,   dim);
        dataTracker.set(TRACKED_DEST_X,     dx);
        dataTracker.set(TRACKED_DEST_Y,     dy);
        dataTracker.set(TRACKED_DEST_Z,     dz);
        dataTracker.set(TRACKED_WIDTH,      w);
        dataTracker.set(TRACKED_HEIGHT,     h);
        dataTracker.set(TRACKED_VISIBLE,    vis);
        this.teleportable = tp;  // Pas tracké (server-side only).
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putString (NBT_FACING_SRC,   facingSource().name());
        nbt.putString (NBT_FACING_DST,   facingDestination().name());
        nbt.putString (NBT_DEST_DIM,     dataTracker.get(TRACKED_DEST_DIM));
        nbt.putDouble (NBT_DEST_X,       dataTracker.get(TRACKED_DEST_X));
        nbt.putDouble (NBT_DEST_Y,       dataTracker.get(TRACKED_DEST_Y));
        nbt.putDouble (NBT_DEST_Z,       dataTracker.get(TRACKED_DEST_Z));
        nbt.putFloat  (NBT_WIDTH,        width());
        nbt.putFloat  (NBT_HEIGHT,       height());
        nbt.putBoolean(NBT_TELEPORTABLE, teleportable);
        nbt.putBoolean(NBT_VISIBLE,      visible());
    }

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket(EntityTrackerEntry tracker) {
        // Avec stratégie A (TrackedData), le packet vanilla EntitySpawnS2CPacket
        // suffit : DataTracker pousse automatiquement les valeurs au client juste
        // après le spawn. SPIKE-4 doit le confirmer.
        //
        // Si SPIKE-4 KO → basculer sur PortalSpawnPayload custom + override de cette
        // méthode pour envoyer le payload après le spawn vanilla.
        return new EntitySpawnS2CPacket(this, tracker);
    }

    @Override
    public void tick() {
        super.tick();
        // V1 : pas de tick custom. La détection de traversée est centralisée dans
        // TeleportExecutor.tickServer() (cf. §12.1).
    }

    @Override public boolean canHit() { return false; }
    @Override public boolean shouldRender(double cx, double cy, double cz) { return visible(); }

    // --- Builder ---
    public static Builder builder(World world) { return new Builder(world); }

    public static final class Builder {
        private final World world;
        private Vec3d position;
        private PortalFacing facingSource;
        private PortalFacing facingDestination;
        private RegistryKey<World> destinationDim;
        private Vec3d destinationPos;
        private float width = 1.0f;
        private float height = 2.0f;
        private boolean teleportable = true;
        private boolean visible = true;

        private Builder(World world) { this.world = world; }

        public Builder position(Vec3d pos)                        { this.position = pos; return this; }
        public Builder facingSource(PortalFacing f)               { this.facingSource = f; return this; }
        public Builder facingDestination(PortalFacing f)          { this.facingDestination = f; return this; }
        public Builder destinationDim(RegistryKey<World> dim)     { this.destinationDim = dim; return this; }
        public Builder destinationPos(Vec3d pos)                  { this.destinationPos = pos; return this; }
        public Builder size(float w, float h)                     { this.width = w; this.height = h; return this; }
        public Builder teleportable(boolean v)                    { this.teleportable = v; return this; }
        public Builder visible(boolean v)                         { this.visible = v; return this; }

        public BackroomsPortalEntity build() {
            // Validation explicite — messages français, capitalisés, ponctués.
            if (position == null)          throw new IllegalStateException("Position du portail manquante.");
            if (facingSource == null)      throw new IllegalStateException("Facing source du portail manquant.");
            if (facingDestination == null) throw new IllegalStateException("Facing destination du portail manquant.");
            if (destinationDim == null)    throw new IllegalStateException("Dimension destination du portail manquante.");
            if (destinationPos == null)    throw new IllegalStateException("Position destination du portail manquante.");
            if (width <= 0 || height <= 0) {
                throw new IllegalStateException("Taille du portail invalide : " + width + "×" + height + ".");
            }

            BackroomsPortalEntity p = new BackroomsPortalEntity(BackroomsPortalEntityType.INSTANCE, world);
            p.setPosition(position);
            // Populate du dataTracker → sync auto au client à la création.
            p.dataTracker.set(TRACKED_FACING_SRC, facingSource.ordinal());
            p.dataTracker.set(TRACKED_FACING_DST, facingDestination.ordinal());
            p.dataTracker.set(TRACKED_DEST_DIM,   destinationDim.getValue().toString());
            p.dataTracker.set(TRACKED_DEST_X,     (float) destinationPos.x);
            p.dataTracker.set(TRACKED_DEST_Y,     (float) destinationPos.y);
            p.dataTracker.set(TRACKED_DEST_Z,     (float) destinationPos.z);
            p.dataTracker.set(TRACKED_WIDTH,      width);
            p.dataTracker.set(TRACKED_HEIGHT,     height);
            p.dataTracker.set(TRACKED_VISIBLE,    visible);
            p.teleportable = teleportable;
            return p;
        }
    }
}
```

### §6.2 `BackroomsPortalEntityType.java`

```java
package com.gtgpuke.gtgsbackrooms.backrooms.portal.entity;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Enregistrement du type d'entity pour BackroomsPortalEntity.
 * Appelé depuis GtgsBackroomsMod.onInitialize().
 */
public final class BackroomsPortalEntityType {

    public static final Identifier ID = Identifier.of("gtgs_backrooms", "backrooms_portal");

    public static final EntityType<BackroomsPortalEntity> INSTANCE =
        FabricEntityTypeBuilder
            .<BackroomsPortalEntity>create(SpawnGroup.MISC, BackroomsPortalEntity::new)
            .dimensions(EntityDimensions.fixed(0.1f, 0.1f))
            .trackRangeChunks(8)
            .trackedUpdateRate(20)
            .build();

    private BackroomsPortalEntityType() {}

    public static void register() {
        Registry.register(Registries.ENTITY_TYPE, ID, INSTANCE);
    }
}
```

### §6.3 Modification de `GtgsBackroomsMod.java`

Ajouter l'appel d'enregistrement dans `onInitialize()`. **Règle critique** : les enregistrements de types Minecraft (`EntityType`, `BlockEntityType`, `BlockItem`, etc.) sont stables et **toujours faits**, indépendamment du flag de feature. Le flag gate uniquement les **systèmes runtime** (event handlers, commandes, ticks). Sans ça, un monde sauvegardé contenant des `BackroomsPortalEntity` planterait au load si on désactive `enablePortalFeature`.

```java
// CODE TEMPLATE — VERIFY BEFORE COMPILE
// Dans com.gtgpuke.gtgsbackrooms.GtgsBackroomsMod.onInitialize() :

// --- Registrations stables — toujours exécutées, indépendamment du flag ---
// Indispensable : si le monde a déjà des entities portail sauvegardées, le registry
// doit les connaître au load, sinon Minecraft crash. Le flag gate les SYSTÈMES, pas les TYPES.
BackroomsPortalEntityType.register();          // §6.2
// (BackroomsDoorBlock + BackroomsDoorBlockEntity sont registrés dans
//  BackroomsBlocks — la classe existante du projet — et donc également
//  toujours présents indépendamment du flag.)

// --- Systèmes runtime — gated par le flag ---
if (BackroomsConfig.INSTANCE.portal.enablePortalFeature) {
    BackroomsPortalEntityRegistry.init();      // §7  — hooks ServerEntityEvents
    DestinationLoader.init();                  // §11 — TicketType + tick handler
    TeleportExecutor.init();                   // §12.1 — ServerTickEvents

    // ⚠️ Contrat C-9 : DebugPortalCommand est STRICTEMENT dev-only.
    // L'appel doit être enveloppé dans isDevelopmentEnvironment(),
    // sinon le grep CI casse et la PR ne passe pas.
    if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
        DebugPortalCommand.register();         // §14.4 — /backrooms portal spawn_test
        LOGGER.info("[Portal] DebugPortalCommand enregistré (environnement de développement).");
    }

    LOGGER.info("[Portal] Pilier 3 runtime initialisé (enablePortalFeature=true).");
} else {
    // Les types restent enregistrés, seuls les systèmes runtime ne tournent pas.
    // → portails existants en monde sauvegardé : visibles comme entity invisible,
    //   mais pas de TP, pas de rendu stencil, pas de chunk preload.
    LOGGER.info("[Portal] Pilier 3 runtime désactivé (enablePortalFeature=false).");
}
```

> **Note** : le bloc ci-dessus est la **référence canonique** pour le câblage `onInitialize()`.
> Le bloc en §13 mentionne le même pattern mais à titre de contexte config — en cas de
> divergence, ce bloc-ci fait foi.

---
## §7. `BackroomsPortalEntityRegistry` — lookup UUID → Entity

**Rôle** : SoT pour la résolution `UUID → BackroomsPortalEntity` côté serveur. Permet à `RoomGraph` de référencer un portail par UUID au lieu de garder une ref directe (qui peut devenir invalide au reload).

### §7.1 Le fichier complet

```java
package com.gtgpuke.gtgsbackrooms.backrooms.portal.entity;

import com.gtgpuke.gtgsbackrooms.core.annotation.SourceOfTruth;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Registry SoT — résolution UUID → BackroomsPortalEntity côté serveur.
 *
 * Population : automatique via ServerEntityEvents.ENTITY_LOAD / ENTITY_UNLOAD.
 * Le registry suit le cycle de vie des entities — pas de gestion manuelle.
 *
 * Thread-safety : ConcurrentHashMap. Lecture autorisée depuis n'importe quel thread,
 * écriture uniquement depuis les events ENTITY_LOAD/UNLOAD (server thread).
 */
@SourceOfTruth
public final class BackroomsPortalEntityRegistry {

    private static final ConcurrentMap<UUID, BackroomsPortalEntity> PORTALS = new ConcurrentHashMap<>();

    private BackroomsPortalEntityRegistry() {}

    public static void init() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof BackroomsPortalEntity portal) {
                PORTALS.put(portal.getUuid(), portal);
            }
        });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (entity instanceof BackroomsPortalEntity portal) {
                PORTALS.remove(portal.getUuid());
            }
        });
    }

    public static Optional<BackroomsPortalEntity> get(UUID uuid) {
        return Optional.ofNullable(PORTALS.get(uuid));
    }

    /**
     * Retourne tous les portails actuellement chargés sur le serveur.
     * Utilisé par TeleportExecutor.tickServer() pour itérer.
     */
    public static Iterable<BackroomsPortalEntity> all() {
        return PORTALS.values();
    }

    /**
     * Filtre les portails d'une dimension donnée.
     */
    public static Iterable<BackroomsPortalEntity> inDimension(ServerWorld world) {
        return PORTALS.values().stream()
            .filter(p -> p.getWorld() == world)
            ::iterator;
    }

    public static int size() { return PORTALS.size(); }
}
```

---

## §8. `ClientWorldRegistry` — multi-dimension côté client

**Rôle** : maintenir un `ClientLevel` par dimension visible à travers un portail. Création paresseuse au premier portail visible vers cette dim.

**SPIKE-5** : la création d'un `ClientLevel` distant en Yarn 1.21.1 est non triviale. Le `ClientLevel` vanilla attend un `ClientPlayNetworkHandler` + paramètres réseau. Pour une dim distante (côté observation seulement, sans joueur), il faut soit truquer ces paramètres, soit utiliser un proxy. **Lire le code IP `ClientWorldLoader` avant d'implémenter.**

### §8.1 Le fichier `ClientWorldRegistry.java`

```java
package com.gtgpuke.gtgsbackrooms.backrooms.portal.client.world;

import com.gtgpuke.gtgsbackrooms.core.annotation.SourceOfTruth;
import com.gtgpuke.gtgsbackrooms.core.lifecycle.Lifecycle;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * SoT (CLIENT-only) — un ClientWorld par dimension visible à travers un portail.
 *
 * Adapté de iPortalTeam.ImmersivePortalsMod.ClientWorldLoader (Apache 2.0, voir §19).
 * IP: src/main/java/qouteall/imm_ptl/core/ducks/ClientWorldLoader.java
 *
 * Lifecycle :
 *  - init()    : appelé au démarrage client (GtgsBackroomsClientMod.onInitializeClient).
 *  - dispose() : appelé sur disconnect (ClientPlayConnectionEvents.DISCONNECT) → libère tout.
 *
 * Création paresseuse : un ClientWorld distant n'est créé qu'au premier portail visible.
 */
@SourceOfTruth
public final class ClientWorldRegistry implements Lifecycle {

    private static final ClientWorldRegistry INSTANCE = new ClientWorldRegistry();
    public static ClientWorldRegistry get() { return INSTANCE; }

    private final Map<RegistryKey<World>, ClientWorld>          worlds    = new HashMap<>();
    private final Map<RegistryKey<World>, RemoteLevelRenderer>  renderers = new HashMap<>();

    private ClientWorldRegistry() {}

    @Override public void init()  { /* Vide — population paresseuse. */ }
    @Override public void start() { /* Vide. */ }

    @Override
    public void dispose() {
        // Libération de tous les ClientWorld distants à la déconnexion.
        for (RemoteLevelRenderer r : renderers.values()) r.close();
        renderers.clear();
        worlds.clear();
    }

    /**
     * Récupère ou crée le ClientWorld pour la dimension demandée.
     *
     * ⚠️ Intra-dim : si la dim demandée est celle du joueur courant, on retourne
     * `mc.world` directement (pas de duplicat). Le `RemoteLevelRenderer` associé
     * (cf. {@link #getRenderer}) wrappe néanmoins ce world pour rester réentrant-safe.
     */
    public @Nullable ClientWorld getOrCreate(RegistryKey<World> dim) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world != null && mc.world.getRegistryKey().equals(dim)) {
            return mc.world;
        }
        return worlds.computeIfAbsent(dim, this::createRemoteWorld);
    }

    /**
     * Récupère ou crée le {@link RemoteLevelRenderer} pour la dimension demandée.
     *
     * ⚠️ Contrat : retourne TOUJOURS un renderer non-null (jamais null), même pour
     * la dimension courante du joueur. Cela évite la réentrance sur le `WorldRenderer`
     * principal de Minecraft, qui est lui-même en plein rendu pendant qu'on appelle
     * cette méthode (depuis le hook LevelRenderer après translucent).
     *
     * Pour la dim courante, le renderer wrappe `mc.world` mais possède sa propre
     * `WorldRenderer` instance — donc swappable réentrant-safely dans le `try/finally`
     * de {@link PortalStencilRenderer#renderOnePortal}.
     *
     * Si la création échoue (dim distante introuvable, fallback SPIKE-5) → retourne
     * null UNIQUEMENT pour les dims distantes. Le caller doit alors afficher un quad
     * noir (cf. §9.1).
     *
     * @return renderer non-null pour intra-dim ; null possible pour cross-dim si KO.
     */
    public @Nullable RemoteLevelRenderer getRenderer(RegistryKey<World> dim) {
        return renderers.computeIfAbsent(dim, d -> {
            ClientWorld w = getOrCreate(d);
            return w == null ? null : new RemoteLevelRenderer(w);
        });
    }

    private @Nullable ClientWorld createRemoteWorld(RegistryKey<World> dim) {
        // SPIKE-5 : création d'un ClientWorld pour une dim distante.
        // L'approche IP : copier le ClientPlayNetworkHandler du joueur courant
        // et instancier un ClientWorld avec des paramètres adaptés (DimensionType,
        // seed, difficulty hardcoded à PEACEFUL, etc.).
        //
        // Voir IP : ClientWorldLoader.createSecondaryClientWorld()
        //
        // FALLBACK : si la création échoue → retourner null. PortalStencilRenderer
        // affichera un fallback noir pour ce portail (cf. §9.1).
        try {
            return RemoteWorldFactory.create(dim);  // Helper séparé, code IP à adapter.
        } catch (Exception e) {
            // Log warn explicite, ne pas crasher.
            org.slf4j.LoggerFactory.getLogger(ClientWorldRegistry.class)
                .warn("Création du ClientWorld distant échouée pour la dim {}.", dim.getValue(), e);
            return null;
        }
    }
}
```

### §8.2 `RemoteWorldFactory` — helper de création (à adapter de IP)

```java
package com.gtgpuke.gtgsbackrooms.backrooms.portal.client.world;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

/**
 * Factory pour créer un ClientWorld distant (dim observable mais pas habitée par le joueur).
 *
 * IP: Référence : qouteall.imm_ptl.core.ducks.ClientWorldLoader.createSecondaryClientWorld()
 *     Logique : copier le NetworkHandler courant, créer une nouvelle DimensionType ref,
 *     instancier ClientWorld avec ces refs.
 *
 * SPIKE-5 : la signature exacte du constructeur ClientWorld(...) en Yarn 1.21.1
 * doit être validée empiriquement. Le code IP de la branche 1.21 est la référence.
 */
final class RemoteWorldFactory {

    private RemoteWorldFactory() {}

    static ClientWorld create(RegistryKey<World> dim) {
        // SPIKE-5 — implémentation à confirmer après spike validé.
        throw new UnsupportedOperationException(
            "SPIKE-5 non validé : création de ClientWorld distant pour " + dim.getValue() + "."
        );
    }
}
```

### §8.3 `RemoteLevelRenderer` — un LevelRenderer par dim distante

```java
package com.gtgpuke.gtgsbackrooms.backrooms.portal.client.world;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;

/**
 * Wrapper sur WorldRenderer pour la dim distante.
 * Permet à PortalStencilRenderer de swapper Minecraft.worldRenderer temporairement.
 *
 * Adapté de iPortalTeam.ImmersivePortalsMod (Apache 2.0).
 * IP: ClientWorldLoader.getOrCreateClientWorldRenderer()
 */
public final class RemoteLevelRenderer implements AutoCloseable {

    private final WorldRenderer renderer;
    private final ClientWorld world;

    RemoteLevelRenderer(ClientWorld world) {
        this.world = world;
        // SPIKE-5 : le constructeur WorldRenderer en Yarn 1.21.1 prend
        // (MinecraftClient, EntityRenderDispatcher, BlockEntityRenderDispatcher, BufferBuilderStorage).
        // À valider exactement.
        MinecraftClient mc = MinecraftClient.getInstance();
        this.renderer = new WorldRenderer(
            mc,
            mc.getEntityRenderDispatcher(),
            mc.getBlockEntityRenderDispatcher(),
            mc.getBufferBuilders()
        );
        this.renderer.setWorld(world);
    }

    public WorldRenderer renderer() { return renderer; }
    public ClientWorld world()      { return world; }

    @Override
    public void close() {
        // SPIKE-5 : WorldRenderer.close() libère les VBOs des chunks.
        this.renderer.close();
    }
}
```

### §8.4 Modification de `GtgsBackroomsClientMod.java`

```java
// Dans com.gtgpuke.gtgsbackrooms.GtgsBackroomsClientMod.onInitializeClient() :
ClientWorldRegistry.get().init();
ClientWorldRegistry.get().start();

// Et nettoyage à la déconnexion.
net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT
    .register((handler, client) -> ClientWorldRegistry.get().dispose());
```

---

## §9. Rendu — `PortalStencilRenderer` + `ViewAreaRenderer` + `RemoteWorldRenderInfo`

### §9.1 `PortalStencilRenderer.java`

**Pipeline GL en 6 étapes**, par portail visible, par frame :

1. Sauvegarder `Minecraft.world`, `worldRenderer`, `lightmap`, `camera`.
2. Activer test stencil + dessiner le rectangle du portail dans le stencil avec `GL_REPLACE` (valeur 1). **Volontaire en MVP** car `MAX_DEPTH = 0` (DA-4, pas de récursion). Si récursion V2 → revenir à `GL_INCR`.
3. Configurer stencil pour ne dessiner que là où `stencilValue == 1`.
4. Swap `Minecraft.world` vers `ClientWorldRegistry.getOrCreate(destDim)`, `worldRenderer` vers `RemoteLevelRenderer`.
5. Calculer la caméra distante via `PortalFacing.transformPoint()` et appeler `worldRenderer.render()`.
6. Restaurer l'état dans un `finally` (Codex Contract critique : sans ça, crash → fausse dim persistante).

```java
package com.gtgpuke.gtgsbackrooms.backrooms.portal.client.view;

import com.gtgpuke.gtgsbackrooms.backrooms.portal.client.world.ClientWorldRegistry;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.client.world.RemoteLevelRenderer;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.entity.BackroomsPortalEntity;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.entity.PortalFacing;
import com.gtgpuke.gtgsbackrooms.mixin.client.GameRendererAccessor;
import com.gtgpuke.gtgsbackrooms.mixin.client.MinecraftClientAccessor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

/**
 * Renderer stencil-based pour les portails.
 *
 * Adapté de iPortalTeam.ImmersivePortalsMod.RendererUsingStencil (Apache 2.0, voir §19).
 * IP: src/main/java/qouteall/imm_ptl/core/render/RendererUsingStencil.java
 *
 * Appelé depuis MixinLevelRenderer après le rendu translucent (cf. §10.1).
 */
public final class PortalStencilRenderer {

    private static final org.slf4j.Logger LOGGER =
        org.slf4j.LoggerFactory.getLogger(PortalStencilRenderer.class);

    private PortalStencilRenderer() {}

    /**
     * Point d'entrée appelé par MixinLevelRenderer une fois par frame.
     * Itère sur les portails visibles et les rend un par un.
     */
    public static void renderAllVisiblePortals(MatrixStack matrices, float tickDelta) {
        if (PortalDepthTracker.currentDepth() > 0) return;  // Pas de récursion en V1 (DA-4).
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;

        // Itération sur les portails visibles (côté client, charge raisonnable :
        // les BackroomsPortalEntity dans la zone de rendu sont déjà dans mc.world.entities).
        for (var entity : mc.world.getEntities()) {
            if (entity instanceof BackroomsPortalEntity p && p.visible() && shouldRender(p, mc)) {
                renderOnePortal(p, matrices, tickDelta);
            }
        }
    }

    private static boolean shouldRender(BackroomsPortalEntity p, MinecraftClient mc) {
        if (mc.player == null) return false;
        // Distance simple — un frustum culling plus précis pourra être ajouté en V2.
        double dist = mc.player.getPos().squaredDistanceTo(p.getPos());
        return dist < 64.0 * 64.0;
    }

    private static void renderOnePortal(
        BackroomsPortalEntity portal, MatrixStack matrices, float tickDelta
    ) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientWorld destWorld = ClientWorldRegistry.get().getOrCreate(portal.destinationDim());
        if (destWorld == null) {
            // FALLBACK : dim destination indisponible → quad noir pour cacher le mur derrière.
            ViewAreaRenderer.drawBlackQuad(portal, matrices);
            return;
        }
        RemoteLevelRenderer remoteRenderer = ClientWorldRegistry.get().getRenderer(portal.destinationDim());
        if (remoteRenderer == null) {
            // FALLBACK : impossible de créer le renderer (cross-dim avec SPIKE-5 KO,
            // ou allocation GL en panne). On ne crash PAS le rendu principal —
            // le caller affiche un quad noir et la prochaine frame réessaiera.
            //
            // Note intra-dim : ClientWorldRegistry.getRenderer() est garanti
            // de retourner non-null pour la dim courante (cf. §8.1 contrat).
            // Si on tombe ici en intra-dim, c'est un bug du registry → log d'alerte.
            if (mc.world != null && mc.world.getRegistryKey().equals(portal.destinationDim())) {
                LOGGER.error("[Portal] BUG : getRenderer() retourne null pour la dim courante "
                    + portal.destinationDim().getValue() + ". Vérifier ClientWorldRegistry §8.1.");
            }
            ViewAreaRenderer.drawBlackQuad(portal, matrices);
            return;
        }

        // --- 1. Sauvegarde de l'état ---
        ClientWorld oldWorld = mc.world;
        WorldRenderer oldRenderer = mc.worldRenderer;
        LightmapTextureManager oldLightmap = mc.gameRenderer.getLightmapTextureManager();
        Camera oldCamera = mc.gameRenderer.getCamera();

        // --- 2. Stencil setup : dessiner le rectangle du portail dans le stencil ---
        PortalDepthTracker.push();
        try {
            GL11.glEnable(GL11.GL_STENCIL_TEST);
            GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);

            // Phase A : marquer le rectangle du portail dans le stencil.
            //
            // ⚠️ MVP : GL_REPLACE volontaire car PortalDepthTracker.MAX_DEPTH = 0
            // (pas de récursion, cf. DA-4). Le stencil ne contient qu'une valeur
            // possible : 1 = "à l'intérieur du rectangle portail courant".
            //
            // Si récursion réintroduite en V2 : repasser à GL_INCR (chaque niveau
            // imbriqué incrémente la valeur stencil), gérer outerValue par niveau,
            // restaurer la depth entre niveaux. Voir IP `RendererUsingStencil`
            // pour le pipeline GL_INCR complet.
            GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
            GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
            GL11.glColorMask(false, false, false, false);  // Pas d'écriture couleur.
            GL11.glDepthMask(false);                       // Pas d'écriture depth.
            ViewAreaRenderer.drawPortalQuad(portal, matrices);
            GL11.glColorMask(true, true, true, true);
            GL11.glDepthMask(true);

            // Phase B : ne rendre que là où stencil == 1.
            GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF);
            GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);

            // --- 3. Swap dim côté client (Codex Contract C-2 : try/finally INCONDITIONNEL) ---
            //     R-3 : une exception ici NE DOIT JAMAIS laisser le client dans la mauvaise dim.
            try {
                mc.world = destWorld;
                ((MinecraftClientAccessor) mc).backrooms$setWorldRenderer(remoteRenderer.renderer());
                // SPIKE-5 : lightmap par dimension — pour V1 on garde le lightmap principal.
                // Si artefacts visuels (lumières mauvaises côté distant) → impl LightmapPerDim.

                // --- 4. Calcul caméra distante ---
                Vec3d remoteCameraPos = PortalFacing.transformPoint(
                    portal.facingSource(), portal.getPos(),
                    portal.facingDestination(), portal.destinationPos(),
                    oldCamera.getPos()
                );

                // --- 5. Rendu du monde distant ---
                renderRemoteWorld(remoteRenderer, remoteCameraPos, matrices, tickDelta);

            } finally {
                // --- 6. Restauration inconditionnelle ---
                mc.world = oldWorld;
                ((MinecraftClientAccessor) mc).backrooms$setWorldRenderer(oldRenderer);
            }

            GL11.glDisable(GL11.GL_STENCIL_TEST);
        } finally {
            PortalDepthTracker.pop();
        }
    }

    private static void renderRemoteWorld(
        RemoteLevelRenderer remote, Vec3d cameraPos,
        MatrixStack matrices, float tickDelta
    ) {
        // SPIKE-3 : signature exacte de WorldRenderer.render() en Yarn 1.21.1.
        // L'appel doit injecter notre cameraPos custom — soit via un setter sur le Camera,
        // soit en passant directement la matrice de vue calculée à partir de cameraPos.
        //
        // IP: RendererUsingStencil.doRenderPortalContent() — utilise un Camera mock
        // positionné via reflection/Mixin accessor sur Camera.
        //
        // V1 minimal : appeler remote.renderer().render(matrices, tickDelta, ..., camera, ...)
        // avec un Camera dont la position a été setée à cameraPos.
        // Le bloc complet (matrice projection, frustum, etc.) est à reprendre du code IP.
        //
        // Voir IP: RendererUsingStencil.doRenderPortalContentWithContext()
    }
}
```

### §9.2 `ViewAreaRenderer.java` — quad du portail

```java
package com.gtgpuke.gtgsbackrooms.backrooms.portal.client.view;

import com.gtgpuke.gtgsbackrooms.backrooms.portal.entity.BackroomsPortalEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * Dessine le rectangle géométrique d'un portail (4 sommets).
 *
 * Adapté de iPortalTeam.ImmersivePortalsMod.ViewAreaRenderer (Apache 2.0, voir §19).
 * IP: src/main/java/qouteall/imm_ptl/core/render/ViewAreaRenderer.java
 */
public final class ViewAreaRenderer {

    private ViewAreaRenderer() {}

    /**
     * Dessine le quad du portail. Utilisé en mode "stencil-only" : pas d'écriture
     * couleur, juste pour marquer le stencil buffer.
     */
    public static void drawPortalQuad(BackroomsPortalEntity portal, MatrixStack matrices) {
        Vec3d[] corners = computeCorners(portal);
        Vec3d cameraPos = MinecraftClient.getInstance().gameRenderer.getCamera().getPos();

        Tessellator tess = Tessellator.getInstance();
        // SPIKE-3 : signature Tessellator.begin() Yarn 1.21.1.
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        Matrix4f mat = matrices.peek().getPositionMatrix();
        for (Vec3d c : corners) {
            buf.vertex(mat,
                (float)(c.x - cameraPos.x),
                (float)(c.y - cameraPos.y),
                (float)(c.z - cameraPos.z));
        }
        // SPIKE-3 : Tessellator.draw() ou tess.end()/tess.endNullable() en Yarn 1.21.1.
    }

    /**
     * Dessine un quad noir à la place du portail (FALLBACK quand la dim destination
     * est indisponible). Évite de voir le mur derrière le portail.
     */
    public static void drawBlackQuad(BackroomsPortalEntity portal, MatrixStack matrices) {
        // Implémentation similaire à drawPortalQuad mais avec une couleur noire opaque.
        // Détails identiques, ajouter VertexFormats.POSITION_COLOR + .color(0, 0, 0, 255).
    }

    /**
     * Calcule les 4 coins du portail en coordonnées monde, ordre CCW vu de l'observateur.
     */
    public static Vec3d[] computeCorners(BackroomsPortalEntity p) {
        Vec3d c = p.getPos();
        Vec3d w = p.facingSource().axisW.multiply(p.width()  / 2.0);
        Vec3d h = p.facingSource().axisH.multiply(p.height() / 2.0);
        return new Vec3d[]{
            c.subtract(w).subtract(h),
            c.add(w).subtract(h),
            c.add(w).add(h),
            c.subtract(w).add(h)
        };
    }
}
```

### §9.3 `RemoteWorldRenderInfo.java`

```java
package com.gtgpuke.gtgsbackrooms.backrooms.portal.client.view;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Matrix4f;

/**
 * Record immutable encapsulant les paramètres d'un rendu de monde distant.
 * Passé à RemoteLevelRenderer.render().
 *
 * Adapté de iPortalTeam.ImmersivePortalsMod.WorldRenderInfo (Apache 2.0, voir §19).
 */
public record RemoteWorldRenderInfo(
    RegistryKey<World> dimension,
    Vec3d cameraPos,
    Matrix4f projectionMatrix,
    Matrix4f cameraTransform,
    int renderDistance
) {}
```

### §9.4 `PortalDepthTracker.java`

```java
package com.gtgpuke.gtgsbackrooms.backrooms.portal.client.view;

/**
 * Compteur statique scopé par frame : profondeur de récursion du rendu portail.
 *
 * V1 : maxDepth = 0. Un portail visible dans la vue distante d'un autre portail
 * affiche une zone noire (cf. ViewAreaRenderer.drawBlackQuad).
 *
 * Adapté de iPortalTeam.ImmersivePortalsMod.PortalRendering (Apache 2.0, voir §19).
 */
public final class PortalDepthTracker {

    public static final int MAX_DEPTH = 0;

    private static int depth = 0;

    private PortalDepthTracker() {}

    public static int currentDepth() { return depth; }

    public static boolean canRender() { return depth <= MAX_DEPTH; }

    public static void push() { depth++; }
    public static void pop()  { depth--; if (depth < 0) depth = 0; }

    /** Reset en début de frame (appelé par MixinLevelRenderer avant le rendu). */
    public static void reset() { depth = 0; }
}
```

---

## §10. Mixins

### §10.1 `MixinLevelRenderer.java`

**Cible** : injecter après le rendu translucent dans `WorldRenderer.render(...)` pour appeler `PortalStencilRenderer.renderAllVisiblePortals()`.

```java
package com.gtgpuke.gtgsbackrooms.mixin.client;

import com.gtgpuke.gtgsbackrooms.backrooms.portal.client.view.PortalDepthTracker;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.client.view.PortalStencilRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hook après le rendu translucent pour rendre les portails.
 *
 * SPIKE-3 : la méthode cible exacte et son point d'injection en Yarn 1.21.1
 * doivent être validés. Plusieurs candidats :
 *   - WorldRenderer.render(...) avec @At(value="INVOKE", target="...renderTranslucent...")
 *   - GameRenderer.renderWorld(...) en RETURN
 *   - WorldRenderer.render(...) en TAIL
 *
 * IP: src/main/java/qouteall/imm_ptl/core/mixin/client/MixinWorldRenderer.java
 */
@Mixin(WorldRenderer.class)
public abstract class MixinLevelRenderer {

    /**
     * Reset du depth tracker en début de frame.
     */
    @Inject(
        method = "render",
        at = @At("HEAD")
    )
    private void gtgs_backrooms$resetPortalDepth(CallbackInfo ci) {
        PortalDepthTracker.reset();
    }

    /**
     * Rendu des portails après le rendu translucent.
     *
     * SPIKE-3 : confirmer la signature exacte de render() en Yarn 1.21.1.
     * Voici la signature probable (à valider) :
     *   render(MatrixStack, float tickDelta, long limitTime, boolean renderBlockOutline,
     *          Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmap,
     *          Matrix4f projectionMatrix)
     */
    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/WorldRenderer;renderTranslucentLayer(...)V",
            shift = At.Shift.AFTER
        )
    )
    private void gtgs_backrooms$renderPortals(
        MatrixStack matrices, float tickDelta, /* ... autres args à confirmer */
        CallbackInfo ci
    ) {
        PortalStencilRenderer.renderAllVisiblePortals(matrices, tickDelta);
    }
}
```

### §10.2 `MixinFramebuffer.java` — attachement stencil

**SPIKE-2** : le framebuffer principal de Minecraft 1.21.1 utilise par défaut `GL_DEPTH_COMPONENT` sans stencil. Il faut soit :
- Reattacher le buffer en `GL_DEPTH24_STENCIL8` au moment de l'init du framebuffer, OU
- Allouer un renderbuffer stencil séparé attaché au FBO.

```java
package com.gtgpuke.gtgsbackrooms.mixin.client;

import com.mojang.blaze3d.platform.GlConst;
import net.minecraft.client.gl.Framebuffer;
import org.lwjgl.opengl.GL30;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Reattache le framebuffer principal avec un stencil buffer (GL_DEPTH24_STENCIL8).
 *
 * SPIKE-2 : la méthode d'init du framebuffer en Yarn 1.21.1 est probablement
 * Framebuffer.initFbo() ou Framebuffer.resize(). À valider empiriquement.
 *
 * Adapté de iPortalTeam.ImmersivePortalsMod (Apache 2.0).
 * IP: src/main/java/qouteall/imm_ptl/core/mixin/client/MixinFramebuffer.java
 */
@Mixin(Framebuffer.class)
public abstract class MixinFramebuffer {

    @Inject(method = "initFbo", at = @At("RETURN"))
    private void gtgs_backrooms$attachStencil(int width, int height, boolean checkError, CallbackInfo ci) {
        // SPIKE-2 : signature à valider — initFbo() pourrait être resize() en 1.21.1.
        //
        // Logique : reattacher le depth-stencil buffer en format combiné DEPTH24_STENCIL8.
        // Voir IP pour le code exact.
        //
        // FALLBACK : si la reattach échoue → pas de stencil → portail désactivé.
        // PortalStencilRenderer.canUseStencil() retournera false.
    }
}
```

### §10.3 `MinecraftClientAccessor.java`

```java
package com.gtgpuke.gtgsbackrooms.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor pour swapper Minecraft.worldRenderer pendant le rendu portail.
 */
@Mixin(MinecraftClient.class)
public interface MinecraftClientAccessor {

    @Accessor("worldRenderer")
    void backrooms$setWorldRenderer(WorldRenderer renderer);

    @Accessor("worldRenderer")
    WorldRenderer backrooms$getWorldRenderer();
}
```

### §10.4 `GameRendererAccessor.java`

```java
package com.gtgpuke.gtgsbackrooms.mixin.client;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor pour swapper le LightmapTextureManager pendant le rendu portail (V1.5+).
 *
 * V1 : pas utilisé activement (on garde le lightmap principal). Présent pour anticiper V1.5
 * si artefacts visuels.
 */
@Mixin(GameRenderer.class)
public interface GameRendererAccessor {

    @Accessor("lightmapTextureManager")
    void backrooms$setLightmap(LightmapTextureManager lightmap);

    @Accessor("lightmapTextureManager")
    LightmapTextureManager backrooms$getLightmap();
}
```

---

## §11. `DestinationLoader` — préchargement chunks

### §11.1 Le fichier complet

```java
package com.gtgpuke.gtgsbackrooms.backrooms.portal.transition;

import com.gtgpuke.gtgsbackrooms.backrooms.BackroomsConfig;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.entity.BackroomsPortalEntity;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.entity.BackroomsPortalEntityRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Préchargement des chunks autour de la destination de chaque portail proche d'un joueur.
 *
 * Adapté de iPortalTeam.ImmersivePortalsMod.ImmPtlChunkTickets (Apache 2.0, voir §19).
 * IP: src/main/java/qouteall/imm_ptl/core/chunk_loading/ImmPtlChunkTickets.java
 *
 * Cycle de vie d'un ticket :
 *  - Pose : portail dans rayon ACTIVATION_RADIUS d'un joueur → ticket sur chunks dest.
 *  - Maintien : tant que le portail reste dans le rayon, ticket maintenu.
 *  - Libération : portail sort du rayon ou est despawn → ticket libéré.
 *
 * Type de ticket : ChunkTicketType<UUID> avec UUID = portal.getUuid().
 */
public final class DestinationLoader {

    private static final int ACTIVATION_RADIUS = 16;        // blocs
    private static final int CHUNK_LOAD_RADIUS = 2;         // chunks autour de la destination
    private static final int TICKET_LEVEL      = 32;        // FULL (cf. ChunkLevelType)

    /**
     * SPIKE-3 : la création d'un ChunkTicketType<UUID> en Yarn 1.21.1
     * passe probablement par ChunkTicketType.create(name, comparator, expiryTicks).
     */
    private static final ChunkTicketType<UUID> TICKET_TYPE =
        ChunkTicketType.create(
            "gtgs_backrooms_portal",
            Comparator.comparingLong(UUID::getMostSignificantBits)
        );

    /** Map UUID portail → liste de ChunkPos chargés par lui. */
    private static final Map<UUID, ChunkPos[]> ACTIVE_TICKETS = new HashMap<>();

    private DestinationLoader() {}

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(DestinationLoader::tickServer);
    }

    private static void tickServer(MinecraftServer server) {
        // Pour chaque portail chargé :
        //   - S'il est près d'un joueur → s'assurer que ticket actif sur chunks dest.
        //   - Sinon → libérer ticket si actif.
        for (BackroomsPortalEntity portal : BackroomsPortalEntityRegistry.all()) {
            boolean nearPlayer = isPortalNearAnyPlayer(server, portal);
            boolean hasTicket = ACTIVE_TICKETS.containsKey(portal.getUuid());

            if (nearPlayer && !hasTicket) {
                acquire(server, portal);
            } else if (!nearPlayer && hasTicket) {
                release(server, portal);
            }
        }
    }

    private static boolean isPortalNearAnyPlayer(MinecraftServer server, BackroomsPortalEntity portal) {
        ServerWorld world = (ServerWorld) portal.getWorld();
        return world.getPlayers().stream().anyMatch(player ->
            player.getPos().squaredDistanceTo(portal.getPos()) < ACTIVATION_RADIUS * ACTIVATION_RADIUS
        );
    }

    private static void acquire(MinecraftServer server, BackroomsPortalEntity portal) {
        RegistryKey<World> destDim = portal.destinationDim();
        ServerWorld destWorld = server.getWorld(destDim);
        if (destWorld == null) return;  // FALLBACK silencieux : dim absente.

        ChunkPos centerChunk = new ChunkPos(
            (int) Math.floor(portal.destinationPos().x) >> 4,
            (int) Math.floor(portal.destinationPos().z) >> 4
        );

        // Pose le ticket sur chaque chunk dans CHUNK_LOAD_RADIUS autour du centre.
        int radius = CHUNK_LOAD_RADIUS;
        ChunkPos[] chunks = new ChunkPos[(2 * radius + 1) * (2 * radius + 1)];
        int idx = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                ChunkPos cp = new ChunkPos(centerChunk.x + dx, centerChunk.z + dz);
                destWorld.getChunkManager().addTicket(TICKET_TYPE, cp, TICKET_LEVEL, portal.getUuid());
                chunks[idx++] = cp;
            }
        }
        ACTIVE_TICKETS.put(portal.getUuid(), chunks);
    }

    private static void release(MinecraftServer server, BackroomsPortalEntity portal) {
        RegistryKey<World> destDim = portal.destinationDim();
        ServerWorld destWorld = server.getWorld(destDim);
        ChunkPos[] chunks = ACTIVE_TICKETS.remove(portal.getUuid());
        if (destWorld == null || chunks == null) return;
        for (ChunkPos cp : chunks) {
            destWorld.getChunkManager().removeTicket(TICKET_TYPE, cp, TICKET_LEVEL, portal.getUuid());
        }
    }

    /**
     * Libération forcée — appelée quand un portail est despawn.
     * (À hooker sur ServerEntityEvents.ENTITY_UNLOAD ou directement dans BackroomsPortalEntity.remove.)
     */
    public static void releaseForcibly(BackroomsPortalEntity portal) {
        if (portal.getWorld() instanceof ServerWorld sw) {
            release(sw.getServer(), portal);
        }
    }
}
```

---

## §12. `TeleportExecutor` + `TpCooldownTracker`

### §12.1 `TeleportExecutor.java`

**Règle critique** : invocable uniquement depuis `ServerTickEvents.END_SERVER_TICK` ou `server.execute()`. Jamais depuis le render thread.

```java
package com.gtgpuke.gtgsbackrooms.backrooms.portal.transition;

import com.gtgpuke.gtgsbackrooms.backrooms.portal.entity.BackroomsPortalEntity;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.entity.BackroomsPortalEntityRegistry;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.entity.PortalFacing;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Détecte la traversée des portails et exécute les téléportations.
 *
 * Règle critique : invocable uniquement depuis ServerTickEvents.END_SERVER_TICK
 * ou server.execute(). Jamais depuis le render thread (vérifié par grep).
 */
public final class TeleportExecutor {

    /** Position du joueur au tick précédent (pour détecter le changement de signe). */
    private static final Map<UUID, Vec3d> LAST_PLAYER_POS = new HashMap<>();

    private TeleportExecutor() {}

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(TeleportExecutor::tickServer);
    }

    private static void tickServer(MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            for (ServerPlayerEntity player : world.getPlayers()) {
                processPlayer(player);
            }
        }
        // Cleanup des entrées LAST_PLAYER_POS pour les joueurs déconnectés.
        // NOTE : approche simple — on laisse la map grossir, négligeable en single-player.
    }

    private static void processPlayer(ServerPlayerEntity player) {
        Vec3d currentPos = player.getPos();
        Vec3d lastPos = LAST_PLAYER_POS.get(player.getUuid());
        LAST_PLAYER_POS.put(player.getUuid(), currentPos);

        if (lastPos == null) return;  // Premier tick : pas de comparaison possible.
        if (TpCooldownTracker.isOnCooldown(
                player.getUuid(),
                player.getWorld().getTime(),
                BackroomsConfig.INSTANCE.portal.teleportCooldownTicks)) return;

        // Itère sur les portails du monde courant et détecte un changement de signe.
        for (BackroomsPortalEntity portal : BackroomsPortalEntityRegistry.all()) {
            if (portal.getWorld() != player.getWorld()) continue;
            if (!portal.teleportable()) continue;

            // Distance signée : positive côté observateur, négative côté arrière.
            double dLast = portal.signedDistance(lastPos);
            double dCurr = portal.signedDistance(currentPos);

            // Traversée détectée : changement de signe ET passage dans la box.
            if (dLast > 0 && dCurr <= 0 && portal.detectionBox().contains(currentPos)) {
                execute(player, portal);
                return;  // Une seule traversée par tick.
            }
        }
    }

    /**
     * Exécute la téléportation. Public pour tests, normalement appelé par processPlayer().
     */
    public static void execute(ServerPlayerEntity player, BackroomsPortalEntity portal) {
        Vec3d srcVelocity = player.getVelocity();

        Vec3d destPos = PortalFacing.transformPoint(
            portal.facingSource(), portal.getPos(),
            portal.facingDestination(), portal.destinationPos(),
            player.getPos()
        );
        Vec3d destVelocity = PortalFacing.transformVelocity(
            portal.facingSource(), portal.facingDestination(), srcVelocity
        );
        float destYaw = PortalFacing.transformYaw(
            portal.facingSource(), portal.facingDestination(), player.getYaw()
        );

        ServerWorld destWorld = player.getServer().getWorld(portal.destinationDim());
        if (destWorld == null) {
            // FALLBACK : dim absente, on log et on annule.
            org.slf4j.LoggerFactory.getLogger(TeleportExecutor.class)
                .warn("Téléportation annulée — dim destination {} absente.", portal.destinationDim().getValue());
            return;
        }

        if (player.getWorld() == destWorld) {
            // Intra-dim.
            player.requestTeleport(destPos.x, destPos.y, destPos.z);
            player.setYaw(destYaw);
        } else {
            // Inter-dim — SPIKE-8 : signature exacte Yarn 1.21.1 à valider.
            // Probable : player.teleport(ServerWorld, double, double, double, Set<PositionFlag>, float, float)
            player.teleport(destWorld, destPos.x, destPos.y, destPos.z,
                java.util.Set.of(), destYaw, player.getPitch());
        }

        // Réappliquer la vélocité APRÈS le TP — requestTeleport()/teleport() reset la vélocité.
        player.setVelocity(destVelocity);
        player.velocityModified = true;  // Force le sync packet vers le client.

        // Cooldown anti-double-TP.
        TpCooldownTracker.markTeleported(player.getUuid(), player.getWorld().getTime());

        // Update LAST_PLAYER_POS au cas où le joueur traverse plusieurs portails en chaîne.
        LAST_PLAYER_POS.put(player.getUuid(), destPos);
    }
}
```

### §12.2 `TpCooldownTracker.java`

```java
// CODE COMPLETE — pure Java, no Yarn deps
package com.gtgpuke.gtgsbackrooms.backrooms.portal.transition;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Cooldown anti-double-TP : empêche un joueur de retraverser un portail immédiatement
 * après l'avoir traversé (sinon boucle infinie si deux portails sont face à face).
 *
 * Durée lue à chaque appel depuis BackroomsConfig.portal.teleportCooldownTicks.
 * Pas de constante hardcodée — la config peut être modifiée à chaud.
 *
 * Thread-safe : ConcurrentHashMap. Lectures/écritures depuis le server thread
 * principalement, mais l'API est sûre pour des lectures parallèles.
 */
public final class TpCooldownTracker {

    private static final ConcurrentMap<UUID, Long> LAST_TP_TICK = new ConcurrentHashMap<>();

    private TpCooldownTracker() {}

    public static void markTeleported(UUID playerUuid, long currentTick) {
        LAST_TP_TICK.put(playerUuid, currentTick);
    }

    /**
     * Indique si le joueur est encore sous cooldown.
     *
     * @param cooldownTicks durée du cooldown — passer
     *        BackroomsConfig.INSTANCE.portal.teleportCooldownTicks au call site.
     */
    public static boolean isOnCooldown(UUID playerUuid, long currentTick, int cooldownTicks) {
        Long last = LAST_TP_TICK.get(playerUuid);
        return last != null && (currentTick - last) < cooldownTicks;
    }

    /** Reset manuel (commande debug, tests, déconnexion). */
    public static void clear(UUID playerUuid) {
        LAST_TP_TICK.remove(playerUuid);
    }

    /** Reset total (test JUnit teardown, server stop). */
    public static void clearAll() {
        LAST_TP_TICK.clear();
    }
}
```

---
## §13. `BackroomsConfig` — `enablePortalFeature` + `PortalConfig`

Ajout d'une sous-section dans la classe `BackroomsConfig` existante.

```java
// Dans com.gtgpuke.gtgsbackrooms.backrooms.BackroomsConfig — ajouter :

/** Configuration du Pilier 3 (Portail). */
public PortalConfig portal = new PortalConfig();

public static class PortalConfig {
    /**
     * Garde-fou : le système portail ne s'enregistre pas tant que cette valeur
     * est false. Reste à false jusqu'au commit utilisateur explicite
     * "chore: unlock portal after Phase 0".
     *
     * NB : ce flag verrouille UNIQUEMENT le Pilier 3. Le verrou global Backrooms
     * (tous Piliers) reste BackroomsConfig.enableBackroomsFeature à la racine.
     */
    public boolean enablePortalFeature = false;

    /** Distance d'activation : portail commence à charger ses chunks dest dans ce rayon (blocs). */
    public int activationRadius = 16;

    /** Rayon de chargement des chunks autour de la destination (en chunks). */
    public int chunkLoadRadius = 2;

    /** Distance max de rendu d'un portail visible (en blocs). */
    public int renderDistance = 64;

    /** Cooldown anti-double-TP en ticks. Lu par TpCooldownTracker via getter ci-dessous. */
    public int teleportCooldownTicks = 10;

    /** V1.5 : activer un fade shader Veil pendant le mismatch tick TP. */
    public boolean enableTransitionMasker = false;
}
```

**Câblage initial** : voir **§6.3** pour le bloc canonique `onInitialize()`. La règle clé :

- `BackroomsPortalEntityType.register()` est **toujours** exécuté, hors du flag (registry stable obligatoire — un monde sauvegardé contenant des `BackroomsPortalEntity` doit pouvoir charger même avec `enablePortalFeature=false`).
- Les **systèmes runtime** (`Registry.init()`, `DestinationLoader.init()`, `TeleportExecutor.init()`, `DebugPortalCommand.register()`) sont gated par le flag.

Voir §6.3 pour le code complet.

---

## §14. Patterns de spawn

Deux voies autorisées, **pas plus** (cf. DA-8). Toute autre instanciation de `BackroomsPortalEntity` est interdite.

### §14.1 Spawn direct depuis génération de salle

```java
// Dans le code de RoomGraph, lors de la génération scriptée d'une salle :
BackroomsPortalEntity portal = BackroomsPortalEntity.builder(serverWorld)
    .position(roomCenterPos)
    .facingSource(PortalFacing.NORTH)
    .size(1.0f, 2.0f)
    .destinationDim(serverWorld.getRegistryKey())   // même dim
    .destinationPos(otherRoomPos)
    .facingDestination(PortalFacing.SOUTH)
    .visible(true)
    .teleportable(true)
    .build();

serverWorld.spawnEntity(portal);

// Référencement dans le RoomGraph (résolution UUID, pas ref directe) :
roomGraph.linkRooms(roomA.id(), roomB.id(), portal.getUuid());
```

### §14.2 `BackroomsDoorBlock` + `BackroomsDoorBlockEntity`

Une **porte Backrooms** = un bloc placé scriptement avec un `BlockEntity` qui spawne et possède un `BackroomsPortalEntity`. Le couple bloc + portail est cohérent : pas de portail orphelin si le bloc disparaît.

#### `BackroomsDoorBlock.java`

```java
package com.gtgpuke.gtgsbackrooms.backrooms.portal.block;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Bloc-porte Backrooms — placé scriptement uniquement.
 * Possède un BlockEntity qui spawn/despawn un BackroomsPortalEntity attaché.
 */
public class BackroomsDoorBlock extends BlockWithEntity {

    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    public BackroomsDoorBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        // V1 : pas de placement par joueur. Cette méthode existe pour cohérence API
        // mais n'est jamais appelée en production (placement scripté uniquement).
        return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new BackroomsDoorBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }
}
```

#### `BackroomsDoorBlockEntity.java`

```java
package com.gtgpuke.gtgsbackrooms.backrooms.portal.block;

import com.gtgpuke.gtgsbackrooms.backrooms.portal.entity.BackroomsPortalEntity;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.entity.BackroomsPortalEntityType;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.entity.PortalFacing;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * BlockEntity de la porte Backrooms.
 * Spawn un BackroomsPortalEntity à son init, le despawn à sa destruction.
 *
 * Cohérence garantie : pas de portail orphelin si le bloc disparaît.
 */
public class BackroomsDoorBlockEntity extends BlockEntity {

    public static final BlockEntityType<BackroomsDoorBlockEntity> TYPE =
        BlockEntityType.Builder.create(
            BackroomsDoorBlockEntity::new
            // TODO : ajouter le BackroomsDoorBlock comme cible une fois sa Block class enregistrée (Phase 4).
        ).build();

    private @Nullable UUID attachedPortalUuid;
    private @Nullable RegistryKey<World> destinationDim;
    private @Nullable Vec3d destinationPos;
    private @Nullable PortalFacing destinationFacing;

    public BackroomsDoorBlockEntity(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }

    /**
     * Configuration de la destination — appelée par le code de génération
     * AVANT que le BlockEntity ne spawn son portail.
     */
    public void configureTarget(RegistryKey<World> destDim, Vec3d destPos, PortalFacing destFacing) {
        this.destinationDim    = destDim;
        this.destinationPos    = destPos;
        this.destinationFacing = destFacing;
        markDirty();
    }

    /**
     * Spawn du portail — appelé après configureTarget().
     * Idempotent : si déjà spawné, ne fait rien.
     */
    public void spawnPortalIfNeeded(ServerWorld world) {
        if (attachedPortalUuid != null) return;
        if (destinationDim == null || destinationPos == null || destinationFacing == null) {
            throw new IllegalStateException("Porte Backrooms non configurée — appeler configureTarget() avant.");
        }

        BlockState state = world.getBlockState(pos);
        PortalFacing facing = PortalFacing.fromDirection(state.get(BackroomsDoorBlock.FACING));

        BackroomsPortalEntity portal = BackroomsPortalEntity.builder(world)
            .position(Vec3d.ofCenter(pos).add(facing.normal.multiply(0.5)))
            .facingSource(facing)
            .size(1.0f, 2.0f)
            .destinationDim(destinationDim)
            .destinationPos(destinationPos)
            .facingDestination(destinationFacing)
            .visible(true)
            .teleportable(true)
            .build();

        world.spawnEntity(portal);
        this.attachedPortalUuid = portal.getUuid();
        markDirty();
    }

    @Override
    public void markRemoved() {
        // Despawn du portail attaché — déclenché quand le bloc est cassé/retiré.
        if (attachedPortalUuid != null && world instanceof ServerWorld sw) {
            Entity portal = sw.getEntity(attachedPortalUuid);
            if (portal != null) portal.discard();
            attachedPortalUuid = null;
        }
        super.markRemoved();
    }

    @Override
    protected void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        if (nbt.containsUuid("AttachedPortal")) {
            this.attachedPortalUuid = nbt.getUuid("AttachedPortal");
        }
        if (nbt.contains("DestDim")) {
            this.destinationDim = RegistryKey.of(
                net.minecraft.registry.RegistryKeys.WORLD,
                net.minecraft.util.Identifier.tryParse(nbt.getString("DestDim"))
            );
        }
        if (nbt.contains("DestX")) {
            this.destinationPos = new Vec3d(
                nbt.getDouble("DestX"), nbt.getDouble("DestY"), nbt.getDouble("DestZ")
            );
        }
        if (nbt.contains("DestFacing")) {
            this.destinationFacing = PortalFacing.valueOf(nbt.getString("DestFacing"));
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        if (attachedPortalUuid != null) nbt.putUuid("AttachedPortal", attachedPortalUuid);
        if (destinationDim != null)     nbt.putString("DestDim", destinationDim.getValue().toString());
        if (destinationPos != null) {
            nbt.putDouble("DestX", destinationPos.x);
            nbt.putDouble("DestY", destinationPos.y);
            nbt.putDouble("DestZ", destinationPos.z);
        }
        if (destinationFacing != null) nbt.putString("DestFacing", destinationFacing.name());
    }
}
```

### §14.3 Exemple d'usage — porte placée par génération

```java
// Dans le code de génération (ex : BackroomsLevel0Generator) :
BlockPos doorPos = ...;
ServerWorld world = ...;
RegistryKey<World> destDim = ...;
Vec3d destPos = ...;
PortalFacing destFacing = PortalFacing.SOUTH;

// 1. Placer le bloc.
world.setBlockState(doorPos, BackroomsBlocks.BACKROOMS_DOOR.getDefaultState()
    .with(BackroomsDoorBlock.FACING, Direction.NORTH));

// 2. Configurer le BlockEntity.
BlockEntity be = world.getBlockEntity(doorPos);
if (be instanceof BackroomsDoorBlockEntity door) {
    door.configureTarget(destDim, destPos, destFacing);
    door.spawnPortalIfNeeded(world);
}
```

### §14.4 `DebugPortalCommand` — dev-only

Cette commande est **strictement dev-only** : enregistrée uniquement si `FabricLoader.getInstance().isDevelopmentEnvironment() == true`. Elle ne **doit jamais** apparaître dans une release. Le contrat C-9 (§2bis) garde cette invariant grep-able.

**Fichier** : `src/main/java/com/gtgpuke/gtgsbackrooms/backrooms/portal/debug/DebugPortalCommand.java`

```java
// CODE TEMPLATE — VERIFY BEFORE COMPILE (Brigadier API + ServerCommandSource signatures)
package com.gtgpuke.gtgsbackrooms.backrooms.portal.debug;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.entity.BackroomsPortalEntity;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.entity.PortalFacing;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * Commande de debug pour spawn manuel d'un portail. DEV-ONLY.
 *
 * Syntaxe :
 *   /backrooms portal spawn_test <facingSrc> <facingDst> [width] [height]
 *
 * Spawn un portail à 3 blocs devant le joueur, destination = position du joueur
 * + 1000 blocs sur l'axe X, même dimension. Permet de valider le rendu et la
 * traversée sans dépendre de la génération de salles.
 */
public final class DebugPortalCommand {

    private DebugPortalCommand() {}

    /** À appeler depuis GtgsBackroomsMod.onInitialize(), gated par isDevelopmentEnvironment(). */
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) -> {
            dispatcher.register(literal("backrooms")
                .then(literal("portal")
                    .then(literal("spawn_test")
                        .then(argument("facingSrc", StringArgumentType.word())
                            .then(argument("facingDst", StringArgumentType.word())
                                .executes(ctx -> spawn(
                                    ctx.getSource(),
                                    StringArgumentType.getString(ctx, "facingSrc"),
                                    StringArgumentType.getString(ctx, "facingDst"),
                                    1.0f, 2.0f
                                ))
                                .then(argument("width", FloatArgumentType.floatArg(0.5f, 8.0f))
                                    .then(argument("height", FloatArgumentType.floatArg(0.5f, 8.0f))
                                        .executes(ctx -> spawn(
                                            ctx.getSource(),
                                            StringArgumentType.getString(ctx, "facingSrc"),
                                            StringArgumentType.getString(ctx, "facingDst"),
                                            FloatArgumentType.getFloat(ctx, "width"),
                                            FloatArgumentType.getFloat(ctx, "height")
                                        ))
                                    )
                                )
                            )
                        )
                    )
                    .then(literal("clear")
                        .executes(ctx -> clearAll(ctx.getSource()))
                    )
                )
            );
        });
    }

    private static int spawn(ServerCommandSource src, String fSrc, String fDst, float w, float h) {
        ServerPlayerEntity player;
        try {
            player = src.getPlayerOrThrow();
        } catch (Exception e) {
            src.sendError(Text.literal("Cette commande doit être exécutée par un joueur."));
            return 0;
        }

        PortalFacing facingSrc, facingDst;
        try {
            facingSrc = PortalFacing.valueOf(fSrc.toUpperCase());
            facingDst = PortalFacing.valueOf(fDst.toUpperCase());
        } catch (IllegalArgumentException e) {
            src.sendError(Text.literal("Facing invalide. Valeurs autorisées : NORTH, SOUTH, EAST, WEST, UP, DOWN."));
            return 0;
        }

        ServerWorld world = (ServerWorld) player.getWorld();
        Vec3d eye = player.getEyePos();
        Vec3d look = player.getRotationVector();
        Vec3d portalPos = eye.add(look.multiply(3.0)); // 3 blocs devant.
        Vec3d destPos = portalPos.add(1000.0, 0.0, 0.0);

        BackroomsPortalEntity portal = BackroomsPortalEntity.builder(world)
            .position(portalPos)
            .facingSource(facingSrc)
            .facingDestination(facingDst)
            .destinationDim(world.getRegistryKey())
            .destinationPos(destPos)
            .size(w, h)
            .visible(true)
            .teleportable(true)
            .build();

        world.spawnEntity(portal);
        src.sendFeedback(() -> Text.literal(
            "Portail spawné : " + facingSrc + " → " + facingDst
            + " à " + portalPos + ", taille " + w + "×" + h + "."
        ), false);
        return 1;
    }

    private static int clearAll(ServerCommandSource src) {
        // Despawn tous les BackroomsPortalEntity du monde courant.
        // Utile pour reset après une session de test.
        try {
            ServerWorld world = (ServerWorld) src.getPlayerOrThrow().getWorld();
            int[] count = {0};
            world.iterateEntities().forEach(e -> {
                if (e instanceof BackroomsPortalEntity) {
                    e.discard();
                    count[0]++;
                }
            });
            src.sendFeedback(() -> Text.literal(count[0] + " portail(s) supprimé(s)."), false);
            return count[0];
        } catch (Exception e) {
            src.sendError(Text.literal("Cette commande doit être exécutée par un joueur."));
            return 0;
        }
    }
}
```

**Câblage dans `GtgsBackroomsMod.onInitialize()`** : déjà montré en §13. La condition `FabricLoader.getInstance().isDevelopmentEnvironment()` doit envelopper l'appel.

---

## §15. Phase 0 — 8 spikes vérifiables (avec ordre + dépendances)

Avant Phase 1, valider ces 8 spikes dans `docs/phase0_findings.md`. **Aucun spike `BLOQUÉ` ne peut passer sans plan B écrit.**

Chaque spike vit dans une branche `phase0/spike-N` ou un sous-package `spike/SN_<nom>/` qui sera supprimé en fin de Phase 0. Les spikes ne touchent pas `main`.

### Graphe de dépendances

```
S1 (build) ────► S2 (stencil) ──┐
                                ├──► S3 (LevelRenderer hook) ──► S5 (ClientWorld) ──► Phase 2
                                │                                       │
                                │                                       ▼
                                │                                  S6 (rendu remote)
                                │
S4 (entity sync C-8) ─────────► Phase 1 (fondations Entity)
                                                          │
                                                          ▼
S7 (chunk tickets) ───────────────────────────► S8 (TP+velocity+yaw) ─► Phase 3 (TP)
```

**Format pour chaque spike** : nom, fichier spike, quoi tester, log attendu, fallback, bloqué oui/non.

### §15.1 SPIKE-1 — Build + versions

**Bloque** : tout.
**Fichier** : aucun (vérif environnement).

**Quoi tester** :
- `./gradlew build` clean depuis un repo neuf, sans cache.
- Yarn mappings 1.21.1 résolvent (pas de classes manquantes).
- Loom résolu, JUnit 5 OK (test trivial qui passe).

**Log attendu** : `BUILD SUCCESSFUL` + `1 test passed`.

**Fallback** : aucun. Si le build casse, rien ne peut continuer. Investiguer Loom, mappings, Java toolchain.

---

### §15.2 SPIKE-2 — Stencil framebuffer

**Bloque** : Phase 2.
**Fichier** : `src/main/java/com/gtgpuke/gtgsbackrooms/spike/S2_Stencil/StencilSpikeCommand.java` + Mixin temporaire.

**Quoi tester** :
- Le framebuffer principal Minecraft a un attachement stencil (`GL_DEPTH24_STENCIL8`).
- Si non : un Mixin sur `SimpleFramebuffer.initFbo` peut reattacher avec stencil.
- Quad de test : `glStencilOp(KEEP, KEEP, INCR)` + draw → `glReadPixels(GL_STENCIL_INDEX)` retourne la valeur attendue.

**Log attendu** : `[SPIKE-2] OK stencil readback=1 (after 1 increment)`

**Fallback** : Mixin reattach `Framebuffer` avec format `GL_DEPTH24_STENCIL8`. Documenté en `MixinFramebuffer.java` final.

---

### §15.3 SPIKE-3 — Hook `LevelRenderer` après translucent

**Bloque** : Phase 2 (nécessite SPIKE-2 OK pour pouvoir écrire dans le stencil dans le hook).
**Fichier** : `src/main/java/com/gtgpuke/gtgsbackrooms/spike/S3_LevelRenderer/MixinSpikeLevelRenderer.java`.

**Quoi tester** :
- Un `@Inject` après le rendu translucent fire chaque frame.
- Les matrices `modelView` / `projection` sont disponibles (via `LocalCapture` ou `@Local`).
- Le hook permet d'écrire dans le stencil sans casser le z-buffer subséquent.

**Log attendu** : `[SPIKE-3] OK after-translucent fired tick=<n>, matrices captured`

**Fallback** : `@Inject` sur `GameRenderer.renderWorld` en `RETURN`. Si rien ne marche → DA-3 invalidée, archi portail à revoir (framebuffer offscreen, gros refactor).

---

### §15.4 SPIKE-4 — Sync entity côté client (Contrat C-8)

**Bloque** : Phase 1.
**Fichier** : `src/main/java/com/gtgpuke/gtgsbackrooms/spike/S4_EntitySync/SyncSpikeEntity.java` + sa registration + commande `/spike4_spawn`.

**Quoi tester** : stratégie A — `TrackedData` primitifs pour `facingSrc/Dst` (Integer), `destDim` (String), `destPos` (3× Float), `width`/`height` (Float), `visible` (Boolean).
- Spawn côté serveur avec valeurs distinctives (`facingSrc=EAST`, `destDim=minecraft:the_nether`, `destPos=(123,64,-456)`).
- Côté client, lire les TrackedData immédiatement après spawn (un tick après réception du spawn packet).
- Vérifier que **toutes** les valeurs côté client matchent celles côté serveur.

**Log attendu** : `[SPIKE-4] OK entity sync — server: EAST/the_nether/(123,64,-456)  client: EAST/the_nether/(123,64,-456)`

**Fallback (stratégie B)** : créer un `PortalSpawnPayload` (record Fabric custom payload), enregistrer le codec, envoyer via `ServerPlayNetworking.send()` dans un handler `EntityTrackingEvents.START_TRACKING`. Documenter la classe finale en `network/PortalSpawnPayload.java` et basculer la sync dessus.

**Si A et B échouent** : **BLOQUÉ. Pas de fallback silencieux.** Le projet portail est suspendu jusqu'à investigation Yarn/Fabric.

---

### §15.5 SPIKE-5 — `ClientWorld` distant

**Bloque** : Phase 2.
**Fichier** : `src/main/java/com/gtgpuke/gtgsbackrooms/spike/S5_ClientWorld/ClientWorldSpikeCommand.java`.

**Quoi tester** :
- Créer un `ClientLevel` pour une dim qui n'est pas celle du joueur.
- Le `ClientLevel` distant peut recevoir et exposer des chunks (potentiellement vides, c'est OK).
- Lire `getBlockState(BlockPos)` retourne quelque chose de cohérent (air ou bloc), pas un crash.

**Log attendu** : `[SPIKE-5] OK ClientLevel dim=<id> created, block at 0,64,0=<id>`

**Fallback** : un seul `ClientLevel` partagé qui swap son `level` à chaque rendu portail (perte de la propreté multi-dim mais marche). **⚠️ Ce fallback change le contrat de réentrance** : deux portails inter-dim visibles simultanément dans la même frame ne sont plus garantis de rendre la bonne dim chacun (le second swap écrase le premier). **Si ce fallback est appliqué, l'agent ne continue pas seul — il s'arrête et demande un patch du doc** (révision de §8.1 et du pipeline §9.1) avant Phase 2. Pas de décision implicite.

**Si bloqué** : c'est probablement le spike le plus risqué de tout le projet. Lire le code IP `ClientWorldLoader` et copier strictement la stratégie. Si toujours KO → archi portail à revoir.

---

### §15.6 SPIKE-6 — Rendu remote world avec `try/finally`

**Bloque** : Phase 2 (pipeline complet).
**Dépend de** : SPIKE-2, SPIKE-3, SPIKE-5.
**Fichier** : `src/main/java/com/gtgpuke/gtgsbackrooms/spike/S6_RemoteRender/RemoteRenderSpike.java`.

**Quoi tester** :
- Le swap `client.world` / `client.worldRenderer` / `lightmap` peut se faire dans `try { ... } finally { restore; }` sans crash.
- Lever volontairement une `RuntimeException` au milieu du rendu remote → `finally` restaure l'état → frame suivante OK, pas de crash, pas de fausse dim.
- Visuel : on voit un fragment de la dim distante (peut être incomplet, peu importe).

**Log attendu** :
- `[SPIKE-6] OK remote frame rendered`
- `[SPIKE-6] OK exception thrown, state restored, next frame OK`

**Fallback** : aucun acceptable — le `try/finally` doit fonctionner sinon C-2 invalidé. Si KO → BLOQUÉ.

---

### §15.7 SPIKE-7 — `ChunkTicketType<UUID>`

**Bloque** : Phase 3.
**Fichier** : `src/main/java/com/gtgpuke/gtgsbackrooms/spike/S7_Tickets/TicketSpikeCommand.java`.

**Quoi tester** :
- `TicketType.create(name, comparator)` ou équivalent 1.21.1 compile et accepte un `Comparator<UUID>`.
- `ServerChunkManager.addTicket(type, chunkPos, level, uuid)` charge les chunks dans le rayon demandé.
- Suppression du ticket → chunks se déchargent normalement (vérifié au tick suivant).

**Log attendu** : `[SPIKE-7] OK ticket added, 25 chunks loaded; ticket removed, chunks unloaded`

**Fallback** : si `TicketType<UUID>` n'est plus exposé en 1.21.1, utiliser `TicketType<ChunkPos>` avec une map UUID → ChunkPos en parallèle.

---

### §15.8 SPIKE-8 — TP cross-dim + velocity + yaw/pitch

**Bloque** : Phase 3.
**Dépend de** : SPIKE-7 (chunks doivent être chargés à l'arrivée).
**Fichier** : `src/main/java/com/gtgpuke/gtgsbackrooms/spike/S8_Teleport/TeleportSpikeCommand.java`.

**Quoi tester** :
- `ServerPlayerEntity.teleport(ServerWorld, x, y, z, Set<PositionFlag>, yaw, pitch)` (signature 1.21.1 exacte) fonctionne cross-dim.
- `setVelocity(v)` + `velocityModified = true` post-TP fait bien arriver le joueur avec `v`.
- Test UP/DOWN : envoyer le joueur avec `pitch=80°` (regard quasi vertical) à travers un portail UP→NORTH. Le `transformLook()` du §5 doit produire un yaw/pitch cohérent à l'arrivée.

**Log attendu** :
- `[SPIKE-8] OK teleport dim=<from>→<to>`
- `[SPIKE-8] OK velocity in=(0,0,0.5) → out=(0,0.5,0)` (transformation NORTH→UP)
- `[SPIKE-8] OK look in=(yaw=0,pitch=80) → out=(yaw=0,pitch=10)` (post UP→NORTH transformLook)

**Fallback** :
- Si vélocité reset par le TP : envoyer un `EntityVelocityUpdateS2CPacket` explicite après le `setVelocity`.
- Si la signature `teleport()` a changé : utiliser `FabricDimensions.teleport()` (API Fabric dédiée).
- Si `transformLook()` produit du yaw instable (gimbal lock) : clamp pitch à `±89.5°` post-transformation. Documenter la limitation.

---

### §15.9 DoD Phase 0

- [ ] `docs/phase0_findings.md` existe avec **8 sections** (une par spike), chacune en `OK | FALLBACK appliqué | BLOQUÉ`.
- [ ] Aucun spike en `BLOQUÉ` sans plan B écrit.
- [ ] **SPIKE-4 (sync entity C-8) en `OK`** — sinon, projet portail BLOQUÉ.
- [ ] **SPIKE-5 (ClientLevel distant) en `OK` ou avec fallback validé** — sinon Phase 2 BLOQUÉE.
- [ ] **SPIKE-6 (try/finally restore) en `OK`** — pas de fallback acceptable, cf. C-2.
- [ ] `./gradlew build` passe.
- [ ] `runClient` lance le mod sans crash, branches `phase0/spike-N` mergeables proprement.
- [ ] `BackroomsConfig.portal.enablePortalFeature = false` (verrou maintenu jusqu'à Phase 1).
- [ ] Commit utilisateur explicite `chore: unlock portal after Phase 0` requis pour passer à Phase 1.

---

## §16. Tests JUnit — fichier complet `PortalFacingTest.java`

```java
package com.gtgpuke.gtgsbackrooms.backrooms.portal.entity;

import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests exhaustifs de PortalFacing.transformPoint() et transformVelocity().
 * Couvre les 36 combinaisons facing source × facing destination.
 *
 * Critère de validation : la magnitude de la vélocité est conservée pour TOUTES les
 * combinaisons (rotation pure, pas de scaling).
 */
class PortalFacingTest {

    private static final double EPS = 1.0e-9;

    // --- Tests de cohérence sur les axes de l'enum ---

    @Test
    @DisplayName("Chaque PortalFacing a une base orthonormée directe")
    void axesAreOrthonormal() {
        for (PortalFacing f : PortalFacing.values()) {
            assertEquals(1.0, f.axisW.length(),  EPS, f + " axisW unit");
            assertEquals(1.0, f.axisH.length(),  EPS, f + " axisH unit");
            assertEquals(1.0, f.normal.length(), EPS, f + " normal unit");
            assertEquals(0.0, f.axisW.dotProduct(f.axisH),  EPS, f + " W⊥H");
            assertEquals(0.0, f.axisW.dotProduct(f.normal), EPS, f + " W⊥N");
            assertEquals(0.0, f.axisH.dotProduct(f.normal), EPS, f + " H⊥N");
            // Direct (axisW × axisH = normal).
            Vec3d expectedNormal = f.axisW.crossProduct(f.axisH);
            assertEquals(expectedNormal.x, f.normal.x, EPS, f + " normal x");
            assertEquals(expectedNormal.y, f.normal.y, EPS, f + " normal y");
            assertEquals(expectedNormal.z, f.normal.z, EPS, f + " normal z");
        }
    }

    // --- Test paramétré : 36 combinaisons ---

    static Stream<org.junit.jupiter.params.provider.Arguments> all36Combinations() {
        Stream.Builder<org.junit.jupiter.params.provider.Arguments> b = Stream.builder();
        for (PortalFacing src : PortalFacing.values()) {
            for (PortalFacing dst : PortalFacing.values()) {
                b.add(org.junit.jupiter.params.provider.Arguments.of(src, dst));
            }
        }
        return b.build();
    }

    @ParameterizedTest(name = "{0} → {1} : magnitude vélocité conservée")
    @MethodSource("all36Combinations")
    void velocityMagnitudePreserved(PortalFacing src, PortalFacing dst) {
        Vec3d v = new Vec3d(0.3, -0.5, 0.7);   // Vélocité quelconque non-axiale.
        Vec3d transformed = PortalFacing.transformVelocity(src, dst, v);
        assertEquals(v.length(), transformed.length(), EPS,
            "Magnitude doit être conservée pour " + src + " → " + dst);
    }

    @ParameterizedTest(name = "{0} → {1} : transformPoint cohérente avec transformVelocity")
    @MethodSource("all36Combinations")
    void transformPointConsistentWithVelocity(PortalFacing src, PortalFacing dst) {
        Vec3d srcCenter = new Vec3d(10, 64, 20);
        Vec3d dstCenter = new Vec3d(100, 30, -50);
        // Un point à offset (1, 2, 3) du centre source.
        Vec3d offset = new Vec3d(1, 2, 3);
        Vec3d worldPoint = srcCenter.add(offset);

        Vec3d transformedPoint = PortalFacing.transformPoint(src, srcCenter, dst, dstCenter, worldPoint);
        Vec3d transformedOffset = transformedPoint.subtract(dstCenter);
        Vec3d expectedOffset = PortalFacing.transformVelocity(src, dst, offset);
        // Note : transformVelocity et transformPoint utilisent la même rotation, donc
        // l'offset transformé doit être identique (à epsilon près).
        assertEquals(expectedOffset.x, transformedOffset.x, EPS, "offset x");
        assertEquals(expectedOffset.y, transformedOffset.y, EPS, "offset y");
        assertEquals(expectedOffset.z, transformedOffset.z, EPS, "offset z");
    }

    // --- Cas concrets nommés ---

    @Test
    @DisplayName("NORTH → SOUTH : porte simple, vélocité Z inversée")
    void northToSouthPortal() {
        Vec3d srcCenter = new Vec3d(0, 64, 0);
        Vec3d dstCenter = new Vec3d(100, 64, 0);
        Vec3d player = new Vec3d(0, 65, 0.5);
        Vec3d velocity = new Vec3d(0, 0, 0.5);

        Vec3d newPos = PortalFacing.transformPoint(
            PortalFacing.NORTH, srcCenter, PortalFacing.SOUTH, dstCenter, player
        );
        Vec3d newVel = PortalFacing.transformVelocity(
            PortalFacing.NORTH, PortalFacing.SOUTH, velocity
        );

        assertEquals(100.0, newPos.x, EPS);
        assertEquals( 65.0, newPos.y, EPS);
        assertEquals( -0.5, newPos.z, EPS);

        assertEquals( 0.0, newVel.x, EPS);
        assertEquals( 0.0, newVel.y, EPS);
        assertEquals(-0.5, newVel.z, EPS);
    }

    @Test
    @DisplayName("UP → NORTH : sol vers mur, chute → jaillissement horizontal")
    void upToNorthPortal() {
        Vec3d srcCenter = new Vec3d(0, 64, 0);
        Vec3d dstCenter = new Vec3d(100, 100, 0);
        Vec3d player = new Vec3d(0, 64, 0);
        Vec3d velocity = new Vec3d(0, -0.5, 0);   // Chute.

        Vec3d newVel = PortalFacing.transformVelocity(
            PortalFacing.UP, PortalFacing.NORTH, velocity
        );

        // Composante Y devient composante Z (axisH UP est (0,0,1), axisH NORTH est (0,1,0)).
        // La normale UP est (0,1,0), inversée → -0.5 sur normal source.
        // Recomposé sur normal NORTH (0,0,1) avec -(-0.5) = +0.5.
        // Attendu : (0, 0, +0.5) approximativement.
        assertEquals(0.5, newVel.length(), EPS, "Magnitude conservée");
        // Validation directionnelle : la vélocité a une composante Z positive non nulle.
        assertTrue(newVel.z > 0, "Sortie horizontale vers Z+");
    }

    @Test
    @DisplayName("Yaw : NORTH → SOUTH ajoute 0° (le joueur continue tout droit)")
    void yawNorthToSouth() {
        // Joueur regardant vers le sud (yaw = 0) entre dans un portail face NORTH.
        // Côté SOUTH, il doit ressortir face nord (yaw = 180).
        float result = PortalFacing.transformYaw(PortalFacing.NORTH, PortalFacing.SOUTH, 0.0f);
        assertEquals(180.0f, result, 0.01f);
    }

    @Test
    @DisplayName("Yaw : EAST → WEST conserve la direction de marche relative")
    void yawEastToWest() {
        float result = PortalFacing.transformYaw(PortalFacing.EAST, PortalFacing.WEST, 90.0f);
        // EAST horizontalYaw = -90, WEST horizontalYaw = 90. delta = 90 - (-90) + 180 = 360.
        // (90 + 360) % 360 = 90. Le joueur continue avec le même yaw monde.
        assertEquals(90.0f, result, 0.01f);
    }

    @Test
    @DisplayName("fromDirection retourne le bon facing")
    void fromDirection() {
        assertEquals(PortalFacing.NORTH, PortalFacing.fromDirection(net.minecraft.util.math.Direction.NORTH));
        assertEquals(PortalFacing.UP,    PortalFacing.fromDirection(net.minecraft.util.math.Direction.UP));
    }
}
```

---

## §17. Roadmap + ordre d'implémentation file-by-file

5 phases, validables séquentiellement. Pas de Phase N+1 sans DoD de Phase N. À l'intérieur de chaque phase, les fichiers sont **listés dans l'ordre de création**, pas l'ordre d'importance — chaque fichier ne dépend que des fichiers numérotés avant lui dans la liste.

### Phase 0 — Spikes (~3-5 jours, branches `phase0/spike-N`)

Voir §15. Ordre des spikes :

| Ordre | Spike | Bloque | Durée estimée |
|:-:|---|---|:-:|
| 1 | SPIKE-1 (build & versions) | tout | 1h |
| 2 | SPIKE-2 (stencil framebuffer) | Phase 2 | 2-3h |
| 3 | SPIKE-3 (LevelRenderer hook) | Phase 2 | 2h |
| 4 | SPIKE-4 (sync entity C-8) | Phase 1 | 2-3h |
| 5 | SPIKE-5 (ClientLevel distant) | Phase 2 — **plus risqué** | 4-8h |
| 6 | SPIKE-6 (try/finally restore) | Phase 2 (pipeline) | 2h |
| 7 | SPIKE-7 (chunk tickets UUID) | Phase 3 | 2h |
| 8 | SPIKE-8 (TP+velocity+yaw) | Phase 3 | 2-3h |

**DoD Phase 0** : §15.9.

---

### Phase 1 — Fondations Entity (1-2 jours)

**Prérequis** : SPIKE-1, SPIKE-4 en `OK`.

**Ordre file-by-file** :

| # | Fichier | Marqueur | Référence |
|:-:|---|---|:-:|
| 1 | `portal/entity/PortalFacing.java` | `CODE COMPLETE — compiles after SPIKE-1 OK` (Yarn deps : Vec3d/Vec2f/Direction) | §5 |
| 2 | `test/.../portal/entity/PortalFacingTest.java` | `CODE COMPLETE` | §16 |
| 3 | `portal/entity/BackroomsPortalEntity.java` | `CODE TEMPLATE` (Yarn deps) | §6.1 |
| 4 | `portal/entity/BackroomsPortalEntityType.java` | `CODE TEMPLATE` | §6.2 |
| 5 | `portal/entity/BackroomsPortalEntityRegistry.java` | `CODE TEMPLATE` | §7 |
| 6 | `BackroomsConfig.java` (édition — ajouter PortalConfig) | — | §13 |
| 7 | `GtgsBackroomsMod.java` (édition — câbler les inits) | — | §13 |
| 8 | `portal/debug/DebugPortalCommand.java` | `CODE TEMPLATE` | §14.4 |
| 9 | `tools/check_portal_contract.sh` (vérif Codex Contract) | shell | §2bis |

**DoD Phase 1** :
- [ ] `./gradlew build` passe.
- [ ] `./gradlew test` passe (PortalFacingTest verts, 36 combinaisons couvertes).
- [ ] `/backrooms portal spawn_test NORTH SOUTH` spawn un portail visible comme entity (sans rendu stencil encore).
- [ ] **Sync C-8 vérifiée en jeu** : un client connecté voit les valeurs `facingSrc/Dst/destDim/destPos` immédiatement après le spawn (debug log côté client).
- [ ] NBT round-trip : sauvegarder le monde, recharger, le portail est toujours là avec les mêmes paramètres.
- [ ] `BackroomsPortalEntityRegistry.size()` reflète bien le nombre d'entities chargées.
- [ ] `tools/check_portal_contract.sh` passe les 10 vérifs grep (§2bis).

---

### Phase 2 — Rendu stencil (2-3 jours)

**Prérequis** : SPIKE-2, SPIKE-3, SPIKE-5, SPIKE-6 en `OK`.

**Ordre file-by-file** :

| # | Fichier | Marqueur | Référence |
|:-:|---|---|:-:|
| 1 | `mixin/client/MinecraftClientAccessor.java` | `CODE TEMPLATE` | §10 |
| 2 | `mixin/client/GameRendererAccessor.java` | `CODE TEMPLATE` | §10 |
| 3 | `mixin/client/MixinFramebuffer.java` (si SPIKE-2 fallback) | `CODE TEMPLATE` | §10 |
| 4 | `portal/client/world/ClientWorldRegistry.java` | `CODE TEMPLATE` (suit SPIKE-5) | §8 |
| 5 | `mixin/client/MixinLevelRenderer.java` (suit SPIKE-3) | `CODE TEMPLATE` | §10 |
| 6 | `portal/client/view/RemoteWorldRenderInfo.java` (record) | `CODE TEMPLATE` | §9.3 |
| 7 | `portal/client/view/PortalDepthTracker.java` | `CODE COMPLETE — pure Java` | §9.4 |
| 8 | `portal/client/view/ViewAreaRenderer.java` | `CODE TEMPLATE` | §9.2 |
| 9 | `portal/client/view/PortalStencilRenderer.java` | `CODE TEMPLATE` (try/finally rigide C-2) | §9.1 |
| 10 | `gtgs_backrooms.mixins.json` (édition — déclarer les Mixins) | — | §10 |

**DoD Phase 2** :
- [ ] Le joueur **voit** la dim destination dans le rectangle du portail.
- [ ] Pas de TP encore — juste la vue.
- [ ] Pas de crash en regardant sous des angles obliques (45°, 89°, parallèle au plan).
- [ ] **Test C-2 actif** : forcer une `RuntimeException` au milieu de `renderRemoteWorld` → le client retourne dans sa dim normale, frame N+1 OK, pas de crash.
- [ ] Récursion testée : un 2e portail dans la vue du 1er → quad noir (DA-4).

---

### Phase 3 — TP serveur (1-2 jours)

**Prérequis** : SPIKE-7, SPIKE-8 en `OK`.

**Ordre file-by-file** :

| # | Fichier | Marqueur | Référence |
|:-:|---|---|:-:|
| 1 | `portal/transition/TpCooldownTracker.java` | `CODE COMPLETE — pure Java` | §12.2 |
| 2 | `portal/transition/DestinationLoader.java` | `CODE TEMPLATE` (suit SPIKE-7) | §11 |
| 3 | `portal/transition/TeleportExecutor.java` (intègre les 2 ci-dessus) | `CODE TEMPLATE` (suit SPIKE-8) | §12.1 |

**DoD Phase 3** :
- [ ] Le joueur traverse le portail, arrive à destination, vélocité conservée.
- [ ] Pas d'écran de chargement (chunks préchargés via `DestinationLoader`).
- [ ] Pas de double-TP (cooldown actif — test : portails face à face, le joueur traverse une fois et reste).
- [ ] Test cross-dim et intra-dim distincts (overworld → nether et overworld → overworld).
- [ ] **Test UP/DOWN avec yaw/pitch préservés** : portail UP→NORTH, le joueur tombe avec pitch=80°, sort horizontalement avec un pitch cohérent (pas inversé, pas NaN).

---

### Phase 4 — Bloc-porte (1 jour)

**Prérequis** : Phase 3 OK.

**Ordre file-by-file** :

| # | Fichier | Marqueur | Référence |
|:-:|---|---|:-:|
| 1 | `portal/block/BackroomsDoorBlockEntity.java` | `CODE TEMPLATE` | §14.2 |
| 2 | `portal/block/BackroomsDoorBlock.java` | `CODE TEMPLATE` | §14.2 |
| 3 | `BackroomsBlocks.java` (édition — registration) | — | §14.2 |
| 4 | `assets/.../lang/en_us.json` + `fr_fr.json` (édition — clés du bloc) | — | §4 |

**DoD Phase 4** :
- [ ] Placer scriptement un bloc-porte spawn un portail.
- [ ] Casser le bloc despawn le portail (vérifier `BackroomsPortalEntityRegistry.size()`).
- [ ] Pas de portail orphelin après reload du monde.
- [ ] NBT : le BlockEntity persiste sa destination, le portail persiste son état, et au reload le couple est cohérent (pas de duplicate).

---

### Phase 5 — Polish et tests d'usage (1-2 jours)

- Tests d'usage en jeu : 36 combinaisons facing source × destination (manuels avec `/backrooms portal spawn_test`).
- Si le mismatch tick TP est trop voyant en subjectif → activer `BackroomsConfig.portal.enableTransitionMasker = true` et implémenter `TransitionMasker` (shader Veil 100 ms fade) en `portal/client/transition/TransitionMasker.java`.
- Documentation utilisateur du mod (README, screenshots).

**DoD Phase 5** :
- [ ] 36 combinaisons testées sans crash, vélocité correcte à l'arrivée, look correct (yaw/pitch).
- [ ] Décision documentée dans `docs/v1_polish.md` : masking V1.5 ajouté ou non, et pourquoi.
- [ ] Build de release `./gradlew build` passe avec `enablePortalFeature = true`.
- [ ] `tools/check_portal_contract.sh` continue de passer.

---

## §18. Règles minimales (rappel pédagogique)

Les **règles dures grep-able** sont en §2bis (Codex Contract : C-1 à C-10). Cette section les redonne en formulation plus pédagogique pour la PR review humaine.

| # | Règle (rappel pédagogique) | Cf. Codex Contract |
|---|---|:-:|
| **R-1** | Le portail n'utilise pas Veil — `backrooms/portal/` ne contient aucun `import foundry.veil.*`. | C-1 |
| **R-2** | `TeleportExecutor.execute()` ne tourne que côté serveur, jamais depuis le render thread ou un Mixin client. | C-3 |
| **R-3** | **Tout swap d'état client** (`Minecraft.world`, `worldRenderer`, `lightmap`) **est dans un `try/finally` avec restauration inconditionnelle**. Une exception pendant le rendu remote ne doit jamais laisser le client dans la mauvaise dim. **Cette règle est dure** : la PR ne passe pas sans. | C-2 |
| **R-4** | Aucune allocation OpenGL dans un constructeur. Toujours dans une méthode `init()` appelée depuis le render thread. | C-5 |
| **R-5** | Création de `BackroomsPortalEntity` uniquement depuis `RoomGraph`, `BackroomsDoorBlockEntity`, ou `DebugPortalCommand` (dev-only). | C-6, C-9 |
| **R-6** | Pas de signature Yarn 1.21.1 inventée. Symbole non-vanilla-évident → `// SPIKE-N` validé requis. Le défaut de bloc Java = `CODE TEMPLATE`, **pas** "code prêt à compiler". | C-7 |
| **R-7** | Sync entity côté client : tous les champs critiques (`facing*`, `dest*`, `width`, `height`) **doivent** être lisibles côté client immédiatement après spawn. Stratégie A (TrackedData) par défaut, B (PortalSpawnPayload) en fallback. Pas de fallback silencieux. | C-8 |

---

## §19. Licence Apache 2.0 — code copié d'ImmersivePortals

### §19.1 Composants copiés

| Source IP (branche `1.21`) | Destination v1 |
|---|---|
| `qouteall.imm_ptl.core.portal.Portal` | `BackroomsPortalEntity` (allégé : `PortalFacing` au lieu de `DQuaternion`) |
| `qouteall.imm_ptl.core.ducks.ClientWorldLoader` | `ClientWorldRegistry` + `RemoteWorldFactory` |
| `qouteall.imm_ptl.core.render.RendererUsingStencil` | `PortalStencilRenderer` |
| `qouteall.imm_ptl.core.render.ViewAreaRenderer` | `ViewAreaRenderer` |
| `qouteall.imm_ptl.core.render.WorldRenderInfo` | `RemoteWorldRenderInfo` |
| `qouteall.imm_ptl.core.chunk_loading.ImmPtlChunkTickets` (logique) | `DestinationLoader` |
| `qouteall.imm_ptl.core.render.PortalRendering` | `PortalDepthTracker` |
| Mixins (`MixinWorldRenderer`, `MixinFramebuffer`) | `mixin/client/*` |

**Non copié en V1** (réintroduction V2+) : `CrossPortalSound`, `CrossPortalEntityRenderer`, `PortalLureGoal`, `DQuaternion` (remplacé par `PortalFacing`), `ServerTeleportationManager` (logique allégée dans `TeleportExecutor`).

### §19.2 Header obligatoire sur chaque fichier copié

```java
/*
 * Adapté de iPortalTeam/ImmersivePortalsMod (Apache License 2.0, Copyright 2020 qouteall).
 * Original : <chemin original> (commit <SHA> sur la branche 1.21).
 * Adapté pour : GTG's Backrooms — guide_portail_final_v1.
 *
 * Mappings Yarn 1.21.1 validés par : SPIKE-N (voir docs/phase0_findings.md).
 */
```

### §19.3 Obligations

1. Conserver le copyright Apache 2.0 dans chaque fichier copié.
2. Inclure une copie de `LICENSE-APACHE-2.0.txt` à la racine du repo.
3. Lister l'attribution dans `THIRD_PARTY_LICENSES.md` à la racine du repo.
4. Pas de marque déposée d'`iPortalTeam` ou `qouteall` dans le code utilisateur (commentaires d'attribution autorisés).

### §19.4 Version IP de référence

**Commit IP cible** : dernier commit stable de la branche `1.21` du repo `iPortalTeam/ImmersivePortalsMod` au moment du SPIKE-1. Le SHA exact doit être noté dans `docs/phase0_findings.md` section "Version IP de référence".

⚠️ Ne pas copier depuis `main` : peut contenir du code 1.21.4+ avec mappings différents.

---

*Fin du document. Pour toute ambiguïté, l'autorité est `arborescence_v4.md` (rang 1) puis ce document (rang 2).*
