# Portails — PeTaSsE_gAnG_Additions

Les portails relient le monde normal et le Level 0 à travers une **porte des Backrooms** : on voit
l'autre côté à travers le cadre (blocs, entités, block entities, et les portails qui s'y trouvent), on le
traverse en marchant, on en ressort face à la sortie ; objets, mobs et projectiles passent aussi, et l'on
peut frapper, miner ou poser à travers le cadre. Tout le système vit dans `backrooms/portal/` et ses
mixins ; il est actif par défaut (`ModConfig.ENABLE_PORTAL_FEATURE`,
`-Dpetassegang.portal.enablePortalFeature=false` pour le couper).

---

## En jeu

1. Prendre une **Porte des Backrooms** (onglet créatif *PetasseGang - Portails*) et la poser où l'on veut : le bas du
   cadre se pose sur le sol trouvé devant la porte (porte au sol, encastrée ou sur une marche), et on ressort à la
   même hauteur relative de l'autre côté. La porte n'a pas de collision : on la traverse.
2. Posée **hors du Level 0**, elle mène au layer 0 du Level 0, dans la cellule ouverte la plus proche
   des mêmes X/Z. Posée **dans le Level 0**, elle ramène à la surface du monde normal aux mêmes X/Z.
3. Un portail apparaît devant la face de la porte (1 x 2 blocs) ; un portail de retour apparaît au
   point d'arrivée, orienté vers une cellule ouverte. On ne sort jamais face à un mur.
   On voit l'autre côté dans le cadre, avec ce qui s'y passe (mobs, joueurs, objets au sol, coffres) et les
   autres portails qui s'y trouvent ; on le traverse en marchant : pas d'écran de chargement, pas de saut, et
   en se retournant on voit d'où l'on vient. Un objet jeté ou une flèche tirée dans le cadre ressort de l'autre
   côté ; un mob qui s'y engage aussi.
4. Viser à travers le cadre vise de l'autre côté : le contour du bloc visé s'y dessine, clic gauche frappe ou
   mine, clic droit utilise ou pose. Pas d'écran (coffre, four) ni de monture à travers un portail.
5. Casser la porte retire les deux portails. Un portail dont la porte a disparu se retire seul
   dès que le chunk de la porte est chargé.

Commande de développement (`isDevelopmentEnvironment()` seulement) : `/backrooms portal list`, `/backrooms portal clear`.

---

## Architecture

```text
backrooms/portal/
|-- entity/       PortalFacing (geometrie du passage), BackroomsPortalEntity (+Type, +Registry)
|-- block/        BackroomsDoorBlock, BackroomsDoorBlockEntity, BackroomsDoorTargets
|-- transition/   PortalRelevance, DestinationLoader, TeleportExecutor, TpCooldownTracker
|-- interaction/  RemoteInteractionHandler, RemoteMiningTracker (actions a travers un portail, serveur)
|-- sync/         RemoteDimensionPayload, RemoteChunkPayload, RemoteChunkUnloadPayload, RemoteBlockUpdatePayload,
|                 RemoteEntityPacketPayload, RemoteBlockBreakingPayload, PortalTeleportPayload, PortalCrossPayload,
|                 PortalEntityActionPayload, PortalBlockActionPayload, RemoteChunkWindow, PortalViewTracker, RemoteEntityView
|-- client/
|   |-- world/       RemoteWorld, RemoteViewRenderer, RemoteWorldRegistry, RemoteChunkReceiver, ClientPortalIndex
|   |-- view/        PortalRenderState, PortalViewTransform, ViewAreaRenderer, PortalStencilRenderer
|   |-- transition/  ClientPortalCrossing (prediction et bascule de monde sans coupure)
|   |-- interaction/ CrossPortalTargeting (visee a travers le cadre), CrossPortalActions (clics)
|   `-- entity/      BackroomsPortalEntityRenderer (cadre sombre de repli)
`-- debug/        DebugPortalCommand
mixin/MixinServerChunkManager          <- relais des changements de blocs
mixin/EntityAccessor                   <- changer le monde d'une entity sans respawn, ecouteur de deplacement
mixin/ServerChunkLoadingManagerAccessor, EntityTrackerAccessor <- brancher un joueur sur le suivi vanilla des entities d'une autre dimension
mixin/client/MixinWorldRenderer        <- rendu des portails en fin de frame, camera distante dans setupTerrain,
                                          particules des mondes distants ignorees, sections repercutees sur la vue secondaire
mixin/client/MixinGameRenderer         <- prediction du franchissement avant chaque rendu, visee a travers le cadre apres le raycast
mixin/client/MixinMinecraftClient      <- clics (frapper, miner, utiliser) detournes quand le joueur vise a travers un portail
mixin/client/MixinClientPlayNetworkHandler <- nettoyage vanilla de l'ancien monde pendant une bascule
mixin/client/MixinFramebuffer          <- attachement profondeur + stencil
mixin/client/*Accessor                 <- setupTerrain/updateChunks/renderLayer/renderEntity/scheduleChunkRender, Camera, MinecraftClient, lightmap, gestionnaire de paquets
```

### Géométrie (`PortalFacing`)

Chaque portail a une base orthonormée directe (largeur W, hauteur H, normale N = W x H). La normale
pointe vers le **côté d'entrée** : on se tient en +N, on avance vers -N. Le passage vers le portail
d'arrivée est une **rotation de 180° autour de H** : ce qui est devant la source est derrière la
destination, la droite reste la droite, et entrer dans l'un fait sortir de l'autre le long de +N'.
`facingDestination` est donc toujours l'orientation du portail de retour (normale vers la sortie).

### Serveur

| Classe | Rôle |
|--------|------|
| `BackroomsDoorBlockEntity` | Au premier tick, résout sa destination (`BackroomsDoorTargets`), pose le portail d'entrée devant la porte (bas du cadre sur le sol trouvé devant elle, du niveau de la porte à deux blocs plus bas) et le portail de retour au point d'arrivée (chunk chargé une fois). Veille sur les deux, les retire quand la porte est cassée. |
| `BackroomsDoorTargets` | Level 0 : cellule dont le bloc central est **réellement libre** dans le labyrinthe généré (zone sûre de l'origine et micro-géométrie comprises), sortie vers une cellule ouverte. Overworld : surface aux mêmes X/Z. |
| `BackroomsPortalEntity` | Données canoniques synchronisées par `DataTracker`, porte propriétaire en NBT, boîtes de détection et de traversée en cache, auto-suppression des orphelins (toutes les 100 ticks, seulement si le chunk de la porte est chargé). |
| `PortalRelevance` | Un portail compte pour un joueur s'il est à portée et devant lui (ou à moins de 4 blocs). |
| `DestinationLoader` | Un ticket de chunk par fenêtre d'arrivée regardée, partagé entre portails, retiré quand plus personne ne regarde. Chargement asynchrone. |
| `TeleportExecutor` | **Joueurs** : applique les franchissements annoncés par le client (`portal_cross`) après vérification, en s'alignant sur lui sans paquet de téléportation vanilla (le joueur est déjà de l'autre côté et garde son élan), et en secours détecte lui-même le passage des pieds entre deux ticks (segment position précédente → courante, cadre avec marge 0,5) avec cinq ticks de grâce ; un changement de dimension suit la séquence vanilla sans paquet de respawn. **Objets, mobs, projectiles** : pour chaque portail, les entités de sa boîte de traversée (cadre élargi de trois blocs, la vitesse d'une flèche) dont le déplacement du tick a franchi le plan sont transposées (`Entity.teleportTo` entre dimensions, repositionnement sinon) avec le cooldown de portail vanilla. Joueurs, portails, passagers et montures sont exclus de ce chemin. |
| `PortalViewTracker` | Synchronisation serveur → client des chunks, des blocs et des entités (voir ci-dessous). |
| `RemoteEntityView` | Par joueur et par dimension distante : branche le joueur comme auditeur du suivi vanilla (`EntityTracker.listeners`) de chaque entité de sa fenêtre, lui envoie les paquets d'apparition, enveloppe tout ce que le suivi émet ensuite dans `portal_remote_entity`, et débranche ce qui sort de la fenêtre (destruction envoyée). |
| `RemoteInteractionHandler` | Actions à travers un portail : valide portail (devant le joueur, à portée), dimension et cible (à portée du point où le regard ressort), puis exécute l'action vanilla (`attack`, `interact`, `onUse`/`useOnBlock`) avec le joueur **transposé** de l'autre côté le temps de l'appel, écouteur de déplacement coupé pour que le gestionnaire d'entités ne le voie pas bouger. |
| `RemoteMiningTracker` | Minage à travers un portail, compté par le serveur seul : progression par tick (`calcBlockBreakingDelta`), fissures envoyées au joueur et aux joueurs de la dimension d'arrivée, casse comme `tryBreakBlock` (événements Fabric compris), abandon si le client lâche, si le bloc disparaît ou si le joueur n'est plus à portée. Créatif : casse immédiate. |

### Protocole de synchronisation

Vanilla n'envoie au client que la dimension où il se trouve. Pour montrer l'autre côté, le serveur
envoie lui-même les chunks, les blocs et les entités de la destination :

| Payload | Contenu | Quand |
|---------|---------|-------|
| `portal_remote_dimension` | dimension + type de dimension | une fois par dimension distante et par session de monde |
| `portal_remote_chunk` | `ChunkData` + `LightData` vanilla | chunk entré dans la fenêtre voulue, au plus 8 par joueur toutes les 5 ticks |
| `portal_remote_chunk_unload` | position du chunk | chunk sorti de la fenêtre voulue ; une dimension que plus aucun portail ne montre est gardée 5 secondes avant d'être déchargée (un demi-tour ne renvoie pas tout) |
| `portal_remote_block_update` | position + `BlockState` | changement de bloc dans un chunk déjà envoyé (relayé par `MixinServerChunkManager.markForUpdate`, dédupliqué par tick) |
| `portal_remote_entity` | dimension + un paquet de jeu vanilla (`EntitySpawn`, `EntityS2C`, `EntityTrackerUpdate`, équipement, passagers, effets, animations, destruction…), encodé avec le codec `PlayStateFactories.S2C` des registres de la connexion | tout ce que le suivi vanilla d'une entité de la fenêtre émet ; le client l'applique à son monde distant comme à son propre monde |
| `portal_remote_block_breaking` | dimension, position, étape (-1 efface) | fissures d'un bloc miné à travers un portail |
| `portal_teleport` | dimension, type, position, regard, vitesse | le joueur vient de passer un portail : le client bascule sur son monde distant (ou aligne sa position s'il avait déjà prédit) ; sert aussi à ramener une prédiction refusée |
| `portal_cross` (client → serveur) | UUID du portail, position, vitesse, regard | la caméra du client a franchi le plan ; le serveur vérifie la plausibilité et applique exactement cet état |
| `portal_entity_action` (client → serveur) | portail, dimension, entité, frapper ou utiliser | clic sur une entité vue à travers un portail |
| `portal_block_action` (client → serveur) | portail, dimension, `BlockHitResult`, début/abandon du minage ou utilisation | clic sur un bloc vu à travers un portail |

Toutes les 5 ticks, pour chaque joueur : portails pertinents de son monde menant **ailleurs** →
par dimension distante, un **foyer** (le portail le plus proche) et la fenêtre de chunks
(`PORTAL_PRELOAD_RADIUS_CHUNKS`, 3 par défaut = 7 x 7 chunks, +2 quand le joueur est à moins de 8 blocs du portail) autour de son arrivée, plus celles des
autres portails qui tiennent dans la carte client (`RemoteChunkWindow.fitsAround`). Les déchargements
partent avant les chargements ; les entités de la boîte englobant les chunks envoyés sont ensuite alignées.
À moins de 8 blocs d'un portail, le serveur charge l'arrivée sur toute sa distance de vue (`DestinationLoader`) : la
traversée ne déclenche plus de génération. Au passage, les chunks que le client tient déjà ne lui sont pas renvoyés par
vanilla (`MixinChunkDataSender`) : ils sont à jour et déjà construits, le voisinage immédiat ne clignote ni ne ralentit.
Changement de dimension ou respawn : les deux côtés repartent de zéro.
Côté client, les récepteurs s'exécutent tels que Fabric les appelle, sur le thread de rendu et dans l'ordre des paquets : les
différer avec `client.execute` les ferait passer derrière les paquets vanilla déjà en file (`ReentrantThreadExecutor`), et
`portal_teleport` arriverait après les premiers chunks du nouveau monde.

### Client

| Classe | Rôle |
|--------|------|
| `RemoteWorldRegistry` | Un `RemoteWorld` par dimension annoncée, plus la **vue secondaire** du monde principal (créée à la demande pour les portails qui y ramènent) ; tout est jeté quand le monde principal change. Tique les entités des mondes distants à chaque tick client. |
| `ClientPortalIndex` | Portails chargés par monde client (principal ou distant), tenus à jour par les événements de chargement d'entités : les parcours par image (visibilité, prédiction de traversée, visée) lisent cette liste au lieu de toutes les entités. |
| `RemoteViewRenderer` | Dessin d'un `RemoteWorld` depuis la caméra transposée, avec le brouillard de sa dimension : ciel (shader des positions, projection normale), couches de blocs, entités, block entities, fissures de minage, contour du bloc visé, nuages. Frustum de culling réutilisé d'une image à l'autre (`FrustumAccessor`). |
| `RemoteWorld` | `ClientWorld` + `WorldRenderer` dédié ; état du monde distant (chunks, lumière, paquets d'entités, heure et météo), le dessin étant confié à `RemoteViewRenderer`. Le ciel vanilla se rapporte à la caméra de la vue (redirections dans `renderSky` : couleur du ciel, plan sombre sous l'horizon) et se dessine, comme chez vanilla, avec le shader des positions posé juste avant `renderSky`. Charge les chunks comme `ClientPlayNetworkHandler` (sections, heightmaps, lumière, statut de sections), applique les paquets d'entités avec le gestionnaire de paquets temporairement rebranché sur lui, tique ses entités (interpolation, animations) sans particules, suit l'heure et la météo du monde principal (communes au serveur), et dessine depuis la caméra transposée, avec le brouillard de sa dimension : ciel (soleil, lune, étoiles), couches de blocs, entités, block entities, fissures de minage, contour du bloc visé, nuages. La vue secondaire partage le monde du joueur (entretenu par vanilla) et ne possède que son renderer, dont les sections à reconstruire suivent celles du renderer principal. |
| `ViewAreaRenderer` | Zone de vue d'un portail : une boîte mince (le cadre prolongé de 20 cm derrière son plan), découpée en logiciel contre le plan proche de la caméra et refermée par un polygone. De face, sa silhouette est exactement le cadre ; au ras du plan ou au bord du cadre, elle couvre exactement ce que la caméra voit à travers, sans plein écran forcé ni vide. |
| `PortalStencilRenderer` | À la fin de `WorldRenderer.render` : marque chaque zone de vue dans le stencil (test de profondeur, sans couleur), puis pour chaque portail : profondeur de la zone poussée au plan lointain, fond à la couleur de brouillard de la vue, vue distante depuis la caméra transposée (lightmap recalculée pour cette dimension), **portails imbriqués** (un niveau : bit haut du stencil, projection de la vue qui les contient), puis profondeur du cadre rétablie. |
| `PortalViewTransform` | Caméra distante = image de la caméra par le passage ; matrice de vue construite comme `GameRenderer` ; **projection à plan proche oblique** confondu avec le plan du portail d'arrivée (rien n'est dessiné derrière lui). Cette projection ne sert qu'au dessin : le frustum de culling de `setupTerrain` garde la projection du joueur, car `Frustum.coverBoxAroundSetPosition` boucle sans fin sur une matrice non orthogonale. |
| `PortalRenderState` | Garde de réentrance, position substituée au joueur dans `setupTerrain`, caméras des vues, coupe-circuit : stencil refusé par le pilote ou exception pendant un rendu distant → rendu distant coupé pour la session, portails en cadres sombres. |
| `CrossPortalTargeting` | Après le raycast vanilla de chaque image : si le regard traverse un cadre avant sa cible, rayon transposé dans la vue et poursuivi sur la portée restante (blocs puis entités) ; vanilla reçoit une cible manquée. |
| `CrossPortalActions` | Clics quand une cible existe de l'autre côté : frappe et utilisation envoyées au serveur, minage annoncé (début, abandon) sans prédiction, délais vanilla (4 ticks entre utilisations, 5 entre blocs cassés en créatif) ; sinon vanilla garde la main (coup dans le vide, manger). |

Un portail est **à sens unique** : vu de dos, il n'est ni dessiné ni traversable. Le rendu des portails se
termine comme celui de vanilla, sans brouillard et couleur de shader blanche : la main et les objets de l'interface ne
prennent pas la teinte de la dimension.

### Traversée sans coupure

Le changement de dimension vanilla envoie un paquet de respawn : nouveau `ClientWorld`, écran « chargement
du terrain », rendu reconstruit. Ici rien de tout cela :

1. **Prédiction** (`ClientPortalCrossing`, au début de chaque `GameRenderer.renderWorld`) : si les pieds du
   joueur, à la position affichée, ont franchi le plan d'un portail depuis l'image précédente (même test que le
   serveur, les portails étant verticaux), le client transpose lui-même position, regard et
   vitesse, échange le monde principal et le monde distant déjà chargé (`RemoteWorldRegistry.swapMainWorld`),
   déplace son joueur d'un `ClientWorld` à l'autre et envoie `portal_cross` avant son prochain paquet de
   déplacement. L'image suivante est déjà celle de l'autre côté. L'ancien monde et son renderer deviennent un
   monde distant : le portail de retour montre d'où l'on vient sans rien recharger.
2. **Serveur** (`TeleportExecutor`) : la demande est acceptée si le portail est dans le monde du joueur, hors
   cooldown, et si la position annoncée est à moins de 3 blocs de celle connue du serveur et dans le cadre.
   Le changement de dimension suit alors la séquence de `ServerPlayerEntity.teleportTo` sans paquet de respawn,
   précédée de `portal_teleport`. Une demande refusée renvoie `portal_teleport` avec l'état serveur : le client
   revient en arrière. En secours, le serveur détecte aussi lui-même le passage des pieds entre deux ticks, mais
   laisse cinq ticks au client pour annoncer le sien : la prédiction gagne toujours quand le monde distant est là.
   Aucun délai entre deux traversées : seule la confirmation du serveur est attendue avant la prédiction suivante.
   Le joueur ressort exactement là où il est entré, à 2 cm près : aucun décalage à l'arrivée. Après une prédiction,
   le serveur ne renvoie pas de téléportation vanilla (elle ramènerait le joueur là où il était un tick plus tôt et
   annulerait son élan) : il s'aligne et confirme par `portal_teleport` ; le client ignore une confirmation identique
   à sa prédiction (à 30 cm près, le serveur ayant pu le relever d'un sol irrégulier). Un saut de position
   (commande `/tp`, respawn, plus de 8 blocs d'un coup) n'est jamais une traversée, ni pour le serveur ni pour la prédiction.
3. **Flux de paquets** : tant que le serveur n'a pas confirmé, les paquets vanilla continuent d'alimenter
   l'ancien monde (le gestionnaire de paquets ne change de monde qu'à la confirmation). Vanilla nettoie alors
   l'ancien monde du joueur : `MixinClientPlayNetworkHandler` ignore ces déchargements de chunks (pour garder la
   vue derrière soi) et applique les destructions d'entités à l'ancien monde, jusqu'au recentrage du suivi de
   chunks dans le nouveau monde. Comme le client garde ses mondes distants, le serveur lui retire d'abord les entités
   qu'il y suivait (destructions enveloppées, dimension d'arrivée comprise, envoyées avant que la destination ne suive
   le joueur), puis le rebranche sur les entités de l'ancien monde comme sur n'importe quelle dimension distante.

Les **autres entités** ne sont pas prédites : le serveur transpose seul objets, mobs et projectiles d'après leur
déplacement du tick, et le client les voit apparaître de l'autre côté par la synchronisation ordinaire.

### Entités à travers les portails

Le serveur ne réécrit pas le suivi des entités : il réutilise celui de vanilla. Pour chaque entité de la fenêtre
d'un joueur, un `RemoteEntityView` (une implémentation de `PlayerAssociatedNetworkHandler` par joueur et par
dimension) est ajouté aux auditeurs du `EntityTracker` vanilla de cette entité. Le joueur reçoit alors les mêmes
paquets que les joueurs présents dans la dimension, enveloppés avec leur dimension (`portal_remote_entity`), et
son client les applique à son monde distant avec le gestionnaire de paquets temporairement rebranché dessus :
les mêmes classes d'entités, la même interpolation, les mêmes animations. Les mondes distants sont tiqués à chaque
tick client (`ClientWorld.tickEntities`), sans particules (le gestionnaire de particules est celui du monde
principal) ni sons de l'autre dimension. Le rendu reprend l'ordre de vanilla : couches opaques, entités et block
entities (dispatchers configurés sur la vue le temps du rendu), couches translucides.

### Portails imbriqués et vue secondaire

Chaque portail visible dans une vue est à son tour dessiné, **un niveau plus loin** : son cadre reçoit le bit haut
du stencil là où il passe le test de profondeur contre la vue (projetée comme elle), sa profondeur est poussée au
plan lointain, et sa propre vue est rendue depuis la caméra transposée deux fois, avant que le cadre intérieur ne
reprenne sa profondeur (un autre portail imbriqué derrière lui n'y sera pas marqué). Au-delà de ce niveau, un
portail est transparent. Quand la destination d'un portail (direct ou imbriqué) est la dimension **du joueur**, la
vue est une **vue secondaire** du monde principal : un second `WorldRenderer` sur le même `ClientWorld`, centré sur
le joueur comme le renderer principal (pas de redécoupage de sa carte de sections à chaque image), dont les sections
à reconstruire suivent celles du renderer principal (`MixinWorldRenderer.scheduleChunkRender`). Les portails
intra-dimension se dessinent donc aussi. Un portail imbriqué dont le chunk d'arrivée n'est pas disponible dans la
vue (hors fenêtre synchronisée, hors distance de rendu) reste un cadre sombre.

### Interaction à travers un portail

1. **Visée** (`CrossPortalTargeting`, après `GameRenderer.updateCrosshairTarget`) : le rayon du regard est testé
   contre le cadre de chaque portail visible ; s'il en traverse un avant la cible vanilla, il repart du point
   transposé dans la vue (un millième de bloc devant le plan d'arrivée) sur la portée restante, contre les blocs
   (`World.raycast`) puis les entités de la vue. `MinecraftClient.crosshairTarget` devient une cible manquée :
   vanilla ne touche plus au bloc caché par le cadre (la porte elle-même, par exemple).
2. **Contour** : la vue dessine le contour du bloc visé (`WorldRenderer.drawShapeOutline`, car Fabric API intercepte `drawBlockOutline` hors de `render`) quand c'est elle
   que le regard traverse, si vanilla dessine lui-même les contours dans cette image.
3. **Clics** (`CrossPortalActions`, en tête de `doAttack`, `handleBlockBreaking`, `doItemUse`) : frappe et utilisation
   partent au serveur avec portail, dimension et cible ; le minage annonce son début et son abandon, le serveur
   fait le reste ; le minage vanilla d'un bloc du monde du joueur est annulé quand il vise à travers un portail.
   Sans cible de l'autre côté, vanilla garde la main (coup dans le vide, manger, lancer).
4. **Serveur** (`RemoteInteractionHandler`) : portail dans le monde du joueur, visible, menant à la dimension
   annoncée, plan devant le joueur à portée de bloc ; cible à portée (critères vanilla, +1 bloc de marge) du point
   où le regard ressort ; `canPlayerModifyAt` pour les blocs. L'action vanilla s'exécute avec le joueur **transposé**
   (position et regard dans la dimension d'arrivée, écouteur de déplacement coupé puis rétabli) : recul, orientation
   de pose, portée se calculent comme s'il était sur place. Frappe : `PlayerEntity.attack`, son joué au joueur.
   Utilisation d'entité : créatures seulement (ni monture, ni véhicule). Utilisation de bloc : `onUseWithItem`,
   `onUse` (sauf blocs à block entity, dont l'écran se fermerait aussitôt), puis `useOnBlock` avec un
   `ItemUsageContext` du monde d'arrivée, main principale puis secondaire.

---

## Configuration (`ModConfig`, propriétés `-D`)

| Propriété | Défaut | Rôle |
|-----------|--------|------|
| `petassegang.portal.enablePortalFeature` | `true` | Coupe tout (la porte devient décorative, les mixins ne font rien). |
| `petassegang.portal.preloadRadiusChunks` | `3` | Rayon de la fenêtre chargée et synchronisée autour de chaque arrivée (7 x 7 chunks). |
| `petassegang.portal.renderDistanceBlocks` | `64` | Distance de rendu et de synchronisation d'un portail. |
| `petassegang.portal.activationRadiusBlocks` | `16` | Distance minimale de préchargement (le max des deux est retenu). |
| `petassegang.portal.teleportCooldownTicks` | `0` | Délai minimal entre deux traversées d'un joueur ; zéro par défaut, on enchaîne aller et retour librement (les objets et mobs gardent un cooldown vanilla fixe de 10 ticks). |
| `petassegang.portal.debugRendering` | `false` | Une ligne de log par seconde et par portail rendu (chunks chargés, entités, état du renderer). |

`./gradlew runClient -PportalPreloadRadiusChunks=2 -PportalDebugRendering=true` passe ces propriétés au client de dev.

---

## Limites connues

- **Un niveau d'imbrication** : un portail vu à travers un portail est dessiné, un portail vu à travers celui-là est transparent.
- **Ni particules, ni sons, ni pluie** de l'autre côté : les entités distantes tiquent sans particules, les sons d'une dimension ne sont joués qu'à ses joueurs, la météo n'est pas dessinée dans la vue (le ciel, lui, suit l'heure et la pluie).
- **Interaction** : pas d'écran (coffre, four, enclume…) ni de monture à travers un portail ; le balayage d'épée est évalué dans le monde du joueur ; les sons et particules du coup partent dans le monde du joueur, seul le son de frappe lui est rejoué.
- **Un foyer par dimension distante** : deux portails vers la même dimension dont les arrivées sont à plus de ~500 blocs l'une de l'autre ne sont pas rendus en même temps.
- **Portes qui se recouvrent** : poser une porte exactement au point d'arrivée d'une autre porte superpose leurs portails, et le joueur peut être renvoyé d'un côté à l'autre. Décaler l'une des deux.
- **Traversée** : la prédiction client n'a lieu que si le monde distant est déjà chargé ; en sprintant vers une porte qu'on vient de poser, c'est le serveur qui bascule, avec un tick de latence. Montures et passagers ne traversent pas.
- **Mods de rendu** : Sodium, Iris et consorts remplacent le pipeline de terrain ; non supportés, désactiver la feature avec eux.
- **Mémoire** : un `WorldRenderer` supplémentaire par dimension distante et pour la vue secondaire (quelques Mo de tampons de construction chacun).

---

## Tests

| Niveau | Tests |
|--------|-------|
| Unitaires | `PortalFacingTest` (rotation, pas de miroir, entrée = sortie, sur les 36 paires), `BackroomsPortalEntityTest` (dont distance signée sans allocation, boîte de traversée), `BackroomsDoorBlockEntityTest`, `BackroomsDoorTargetsTest` (cellule réellement libre, sortie ouverte), `PortalRelevanceTest`, `TeleportExecutorTest` (traversée du plan, variante sans allocation), `TpCooldownTrackerTest`, `PortalPayloadsTest` (dix payloads, dont un paquet d'entité vanilla enveloppé et les actions à travers un portail), `RemoteChunkWindowTest`, `PortalViewTransformTest` (plan proche oblique) |
| En jeu (`runGameTest`) | `PortalSyncGameTests` (fenêtre de chunks et entités vers le Nether, traversée entre dimensions sans renvoi des chunks tenus, annonces client, marche détectée par le serveur, changements de monde vanilla, portails orphelins, commande de debug), `PortalInteractionGameTests` (coups, tonte, clics, pose et minage à travers le cadre, refus), `BackroomsDoorGameTests` (porte : pose, cibles par dimension, deux portails liés et propriétaires retirés avec elle, destination absente, sol irrégulier, portails perdus) ; `PortalGameTests` : traversée avec un joueur serveur (position, regard, cooldown) ; un objet lancé dans le cadre ressort de l'autre côté avec sa vitesse transposée et le cooldown vanilla ; `RemoteChunkPayload` d'un vrai chunk octet pour octet |
| Non couvert automatiquement | le rendu (stencil, projection oblique, lightmap, entités, portails imbriqués) et l'interaction se valident en jeu : `./gradlew runClient`, poser une porte, regarder à travers (un mob de l'autre côté bouge), jeter un objet dedans, frapper ou miner à travers, traverser, revenir |
| Reproduction en jeu | `./gradlew runClient -PquickPlayWorld="Nom du monde" -PportalDebugRendering=true` charge un monde solo directement ; en cas de gel, `jstack` sur le PID `devlaunchinjector` donne la pile du thread de rendu |
