# Portails — PeTaSsE_gAnG_Additions

Les portails relient le monde normal et le Level 0 à travers un **bloc portail** : on voit l'autre
côté à travers le cadre (blocs, entités, block entities, et les portails qui s'y trouvent), par ses deux faces, on le
traverse en marchant dans les deux sens, on en ressort de l'autre côté sans tourner ; objets, mobs et projectiles
passent aussi, la lumière des blocs aussi si le portail la transmet, et l'on peut frapper, miner ou poser à travers le
cadre. Tout le système vit dans `backrooms/portal/` et ses mixins ; il est actif par défaut
(`ModConfig.ENABLE_PORTAL_FEATURE`, `-Dpetassegang.portal.enablePortalFeature=false` pour le couper). La porte des
Backrooms, retirée pour l'instant, reviendra sur la base du bloc portail.

---

## En jeu

1. Prendre un **Portail des Backrooms** (onglet créatif *PetasseGang - Portails*, un par réglage proposé) et le poser
   comme un bloc. Il ne se voit pas et ne bloque rien : seul le portail compte, plein cadre, le plan au milieu du bloc.
   Sa face avant regarde le joueur qui l'a posé.
2. **Réglages**, portés par l'item (composant `petasse_gang_additions:portal_settings`, affichés dans l'info-bulle) et
   gardés par le bloc : largeur et hauteur du cadre (1 à 8 blocs, un sur deux par défaut), passage de la lumière des
   blocs (oui par défaut), faces (`double_sided`, deux par défaut), retour (`one_way`, aller-retour par défaut) et sortie
   (`fleeting_exit`, durable par défaut). L'onglet propose 1 x 2, 2 x 3, 3 x 4, 1 x 2 sans lumière, 1 x 2 à une face,
   1 x 2 aller simple et 1 x 2 à sortie fugace ; les autres se
   donnent par commande, par exemple
   `/give @s petasse_gang_additions:backrooms_portal[petasse_gang_additions:portal_settings={width:4,height:3,transmits_light:false}]`.
   Le cadre monte de la hauteur réglée depuis le bloc et s'étend de part et d'autre de sa colonne (une largeur paire
   prend une colonne de plus d'un côté) ; la pose est refusée si le cadre traverserait un bloc.
3. Posé **hors du Level 0**, il mène au layer 0 du Level 0, près des mêmes X/Z : dans la cellule ouverte la plus
   proche où le cadre d'arrivée tient sur toute sa largeur avec un bloc libre devant et derrière chaque colonne, à
   défaut au centre de la cellule ouverte la plus proche, face à un bloc libre. Posé **dans le Level 0**, il ramène à
   la surface du monde normal aux mêmes X/Z, posé sur le haut réel du sol : sur une dalle, un chemin ou un tapis, le
   cadre d'arrivée se pose dessus, au demi-bloc près.
4. Le portail est **double face**, comme un trou dans l'espace : chaque côté tient deux portails dos à dos dans le
   même plan, et chaque face mène à la face correspondante de l'arrivée. On le voit et on le traverse par ses deux
   faces, il ne disparaît jamais en tournant autour, et traverser ne fait pas tourner : on entre en allant vers le sud,
   on ressort en allant vers le sud. On ne sort jamais dans un mur : engagé de biais dans le cadre, on bute sur ce qui
   est de l'autre côté. On n'entre que si son centre passe dans la largeur du cadre : contourner le portail en frôlant
   son bord ne fait ni traverser, ni buter sur ce qui est de l'autre côté.
   Réglé **à une face**, seule la face avant existe, de chaque côté : de dos il n'y a rien, on passe au travers du plan
   sans traverser. Réglé **aller simple**, rien n'est posé à l'arrivée : une fois passé, on ne revient pas par là.
   Réglé **à sortie fugace**, la sortie (le retour, à l'arrivée) ne tient que sous le regard : tant qu'un joueur côté
   arrivée la voit à son écran (troisième personne comprise, rien si un mur la cache), on peut revenir ; dès qu'un
   joueur se tient à 16 blocs de l'arrivée sans qu'aucun ne la voie, elle disparaît pour de bon. Qui passe en marchant
   droit devant lui tourne le dos à l'arrivée : elle a disparu quand il se retourne ; qui passe à reculons, ou la garde
   en vue, peut revenir.
   On voit l'autre côté dans le cadre, avec ce qui s'y passe (mobs, joueurs, objets au sol, coffres) et les
   autres portails qui s'y trouvent ; l'eau et le verre s'y voient comme sans portail, et l'eau ou le verre posés
   devant le cadre le laissent voir par transparence. On le traverse en marchant : pas d'écran de chargement, pas
   de saut, la main tenue et les animations continuent, et en se retournant on voit d'où l'on vient. En troisième
   personne, la caméra qui suit le joueur reste de l'autre côté tant que son recul passe par le cadre. Un objet jeté ou une flèche tirée dans le cadre ressort de l'autre
   côté ; un mob qui s'y engage aussi.
5. Viser à travers le cadre vise de l'autre côté : le contour du bloc visé s'y dessine, clic gauche frappe ou
   mine, clic droit utilise ou pose, clic molette choisit le bloc ou la créature. Pas d'écran (coffre, four) ni de
   monture à travers un portail.
6. Un portail ouvert laisse passer le **son** (on entend ce qui se passe de l'autre côté, venant du cadre), les
   **particules** (flammes, fumée, coups, blocs cassés, dans la vue), le **regard des mobs** (un mob hostile qui voit un
   joueur par le portail vient le chercher en le traversant, et un mob frappé à travers lui aussi) et, s'il la
   transmet, la **lumière des blocs** : une torche d'un côté éclaire l'autre côté, d'un niveau de moins, comme à
   travers un bloc d'air. La lumière du ciel ne passe pas.
7. Pour casser le bloc, s'accroupir : accroupi, le regard s'arrête sur le plan du portail ; debout, il passe à travers.
   Casser le bloc retire ses portails et rend l'item avec ses réglages. Un portail dont le bloc a disparu se
   retire seul dès que le chunk du bloc est chargé ; ceux d'une ancienne porte des Backrooms disparaissent ainsi.

Commande de développement (`isDevelopmentEnvironment()` seulement) : `/backrooms portal list`, `/backrooms portal clear`.

Mesurer une traversée (`-PdebugPerformanceMonitor=true`) : à chaque bascule de monde, le client journalise une ligne
`[perf] traversee: vers <dimension> bascule=<ms> pic=<ms> lentes=<n>/119 chunks=<n>` (durée de la bascule elle-même,
pire image et images de plus de 50 ms sur les 120 suivantes, chunks vanilla et distants reçus pendant ce temps), reprise
au F3 en solo. Côté serveur, les sections `portal.teleport.player`, `portal.sync.tick`, `portal.destination.tick` et
`portal.light` (lumière transmise ; `portal.light.client` pour la passe du client, en solo) entrent dans le résumé `[perf] serveur:`, et le journal de traversée indique combien de chunks de l'arrivée le client
tenait déjà (ceux-là ne lui sont pas renvoyés).

---

## Architecture

```text
backrooms/portal/
|-- entity/       PortalFacing (geometrie du passage), BackroomsPortalEntity (+Registry ; type dans init/ModEntities)
|-- block/        BackroomsPortalBlock, BackroomsPortalBlockEntity, BackroomsPortalItem, PortalSettings, PortalArea, PortalTargets
|-- light/        PortalLightTransmission, VirtualLightSources, VirtualLightView (lumiere des blocs a travers le cadre)
|-- transition/   PortalRelevance, DestinationLoader, TeleportExecutor, PortalCollision
|-- interaction/  RemoteInteractionHandler, RemoteMiningTracker (actions a travers un portail, serveur)
|-- propagation/  PortalPursuit, PortalPursuer, PortalPursuitGoal (regard et poursuite des mobs a travers le cadre)
|-- sync/         RemoteDimensionPayload, RemoteChunkPayload, RemoteChunkUnloadPayload, RemoteBlockUpdatePayload,
|                 RemoteEntityPacketPayload, RemoteBlockBreakingPayload, PortalTeleportPayload, PortalCrossPayload,
|                 PortalEntityActionPayload, PortalBlockActionPayload, PortalSightPayload, RemoteChunkWindow, PortalViewTracker,
|                 RemoteEntityView, PortalSight (portails que chaque joueur voit, rapportes par son client)
|-- client/
|   |-- world/       RemoteWorld, RemoteViewRenderer, ViewSectionWalker, RemoteParticles, RemoteWorldRegistry, RemoteSyncReceiver,
|   |                ClientPortalIndex, ClientPortalLight (lumiere transmise, cote client), PortalSightReporter (portails vus),
|   |                StraddlingEntityRenderer (entities a cheval sur un portail, en deux moities)
|   |-- sound/       FrameSoundPlayer (son d'un monde distant rejoue au cadre d'arrivee), PortalSound (portee et volume)
|   |-- view/        PortalRenderState, PortalViewTransform, ViewCone, PortalClipping, ShaderClipInjector, FrameAreaRenderer, PortalStencilRenderer,
|   |                ThirdPersonPortal (camera a la troisieme personne derriere un portail)
|   |-- transition/  ClientPortalCrossing (prediction et bascule de monde sans coupure), EntityHandover (continuite des entities recreees)
|   |-- interaction/ RemoteTargeting (visee a travers le cadre), RemoteActions (clics)
|   `-- entity/      BackroomsPortalEntityRenderer (cadre sombre de repli)
`-- debug/        DebugPortalCommand
mixin/MixinServerChunkManager          <- relais des changements de blocs
mixin/MixinChunkDataSender             <- chunks deja tenus par le client non renvoyes apres une traversee
mixin/EntityAccessor                   <- changer le monde d'une entity sans respawn, ecouteur de deplacement
mixin/ServerChunkLoadingManagerAccessor, EntityTrackerAccessor <- brancher un joueur sur le suivi vanilla des entities d'une autre dimension
mixin/EntityTrackerEntryAccessor       <- position absolue aux clients apres un saut par un portail du meme monde
mixin/WorldAccessor                    <- pluie et orage bruts d'une dimension, envoyes a ceux qui la voient par un portail
mixin/MixinPlayerManager, MixinServerWorld <- sons, evenements de monde et particules relayes a ceux qui voient la dimension
mixin/MixinEntity                      <- collision a travers les portails ouverts (blocs de l'autre cote ajoutes au deplacement)
mixin/MixinActiveTargetGoal, ActiveTargetGoalAccessor, MixinMobEntity, MixinZombieAttackGoal <- regard des mobs par les portails, poursuite a travers
mixin/MixinWorld, MixinChunkBlockLightProvider, ChunkLightProviderAccessor <- sources de lumiere virtuelles d'un monde, ajoutees aux blocs lumineux
mixin/client/MixinWorldRenderer        <- rendu des portails avant la couche translucide (stencil relache en fin d'image), centre de grille d'une vue dans setupTerrain,
                                          particules des mondes distants confiees a leur contenu, sections repercutees sur la vue secondaire,
                                          entities a cheval sur un portail laissees a StraddlingEntityRenderer
mixin/client/MixinClientWorld          <- sons des mondes distants rejoues par le portail qui les rapproche du joueur, copie perimee
                                          d'une entity retiree avant l'ajout de sa remplacante (EntityHandover.replaceStaleCopy)
mixin/client/MixinShaderStage, MixinShaderProgram <- plan de decoupe des vues ajoute aux vertex shaders au chargement, transmis a chaque shader active
mixin/client/MixinGameRenderer         <- prediction du franchissement avant chaque rendu, visee a travers le cadre apres le raycast
mixin/client/MixinMinecraftClient      <- clics (frapper, miner, utiliser, clic molette) detournes quand le joueur vise a travers un portail
mixin/client/MixinClientPlayNetworkHandler <- nettoyage vanilla de l'ancien monde pendant une bascule, renderer vanilla rendu avant une connexion ou un respawn dans une autre dimension, sons de suivi des entities distantes coupes
mixin/client/MixinFramebuffer          <- attachement profondeur + stencil
mixin/client/*Accessor                 <- setupTerrain/updateChunks/renderLayer/renderEntity/scheduleChunkRender, grille de sections, Frustum, Camera
                                          (et hauteur des yeux lissee), MinecraftClient, lightmap, gestionnaire de paquets, renderer d'un
                                          ClientWorld, particules, balancement des membres (LimbAnimator), couleur du brouillard
                                          (BackgroundRenderer, rendue aux nuages apres les vues)
```

### Géométrie (`PortalFacing`)

Chaque portail a une base orthonormée directe (largeur W, hauteur H, normale N = W x H). La normale
pointe vers le **côté d'entrée** : on se tient en +N, on avance vers -N. Le passage vers le portail
d'arrivée est une **rotation de 180° autour de H** : ce qui est devant la source est derrière la
destination, la droite reste la droite, et entrer dans l'un fait sortir de l'autre le long de +N'.
`facingDestination` est donc toujours l'orientation du portail de retour (normale vers la sortie). La face arrière
d'un portail (`opposite()`, même plan, normale opposée) vers la face arrière de l'arrivée fait exactement la même
transformation : deux portails dos à dos de chaque côté font un trou qui se traverse dans les deux sens.

### Serveur

| Classe | Rôle |
|--------|------|
| `BackroomsPortalBlock` | Bloc invisible, sans collision, orienté vers le joueur qui le pose ; ne se vise qu'accroupi (`ShapeContext.isDescending`), sinon le regard passe et vise par le portail. Refuse la pose si le cadre traverserait un bloc. |
| `BackroomsPortalBlockEntity` | Garde les réglages (composant de l'item, NBT, rendus à la copie et au butin), résout sa destination (`PortalTargets`) et pose ses portails : à deux faces (par défaut), quatre, à l'entrée et à l'arrivée une face avant et une face arrière dos à dos, chaque face menant à la face correspondante de l'autre côté ; à une face, la seule face avant de chaque côté ; aller simple, rien à l'arrivée ; à sortie fugace, toutes les 4 ticks, retire pour de bon les retours (`ExitLost`) dès qu'un joueur s'en tient à 16 blocs sans qu'aucun joueur de leur monde ne les voie (`PortalSight`). Le chunk d'arrivée se charge par un ticket, sans bloquer le serveur, et les retours y sont posés dès qu'il est là. Remplace un portail qui ne correspond plus (bloc tourné, réglages ou destination relus), repose un retour disparu après deux vérifications espacées, les retire tous quand le bloc est cassé. Relu à une autre taille, le cadre d'arrivée garde son pied et son centre suit la nouvelle taille. |
| `PortalSettings`, `PortalArea` | Réglages d'un portail (largeur et hauteur de 1 à 8, lumière transmise, faces `double_sided`, retour `one_way`, sortie `fleeting_exit` ; format du composant et du réseau) ; blocs occupés par un cadre, son centre et son pied (plan au milieu des blocs, une colonne de plus d'un côté pour une largeur paire). |
| `PortalTargets` | Level 0 : cellule ouverte la plus proche où le cadre tient sur toute sa largeur, un bloc libre devant et derrière chaque colonne (labyrinthe réellement généré, zone sûre de l'origine et micro-géométrie comprises), sortie à l'opposé de l'entrée si elle tient ; à défaut, centre de la cellule ouverte la plus proche, face à un bloc libre. Overworld : surface aux mêmes X/Z. |
| `PortalLightTransmission` | Toutes les 2 ticks, pour chaque monde (serveur, et client par `ClientPortalLight`) : chaque bloc du cadre d'un portail ouvert, visible et qui transmet la lumière devient une source virtuelle de la lumière du bloc correspondant de l'autre côté, moins un niveau. Les sources, immuables (`VirtualLightSources`), sont publiées sur le monde (`MixinWorld`) **avant** d'être signalées au moteur, qui les lit sur son propre thread (`MixinChunkBlockLightProvider`) ; une source publiée mais pas encore appliquée (chunk qui vient de charger) est signalée de nouveau, et tout le cadre d'un portail compté pour la première fois (apparu, ou revenu avec son chunk) l'est aussi, la lumière sauvée avec le chunk pouvant venir d'une source éteinte depuis. Les deux côtés d'une paire s'éclairent l'un l'autre : sans vraie source, chaque aller-retour coûte deux niveaux et tout s'éteint. |
| `BackroomsPortalEntity` | Données canoniques synchronisées par `DataTracker` (orientations, arrivée, taille, visibilité, ouverture, lumière transmise), bloc propriétaire en NBT, boîtes de détection et de traversée en cache ; passage d'un point d'un côté à l'autre (`toDestination`, `fromDestination`). |
| `BackroomsPortalEntityRegistry` | Portails serveur chargés, tous mondes confondus, lus par index par la téléportation, le préchargement et la synchronisation ; toutes les 100 ticks, retire ceux dont le bloc propriétaire a disparu, ne les tient plus pour siens ou est dans une dimension absente (seulement si le chunk du bloc est chargé). |
| `PortalRelevance` | Un portail compte pour un joueur s'il est à portée d'activation (16 blocs, quelle que soit la direction du regard), ou plus loin mais devant lui. |
| `DestinationLoader` | Un ticket de chunk par fenêtre d'arrivée regardée, partagé entre portails ; à moins de 8 blocs d'un portail, il couvre toute la distance de vue du serveur. La fenêtre reste chargée dix secondes après que plus personne ne regarde son portail, plus longtemps que la fenêtre du client : un coup d'œil ailleurs ne décharge rien. Chargement asynchrone. Un joueur qui change de dimension laisse en plus un **ticket de départ** sur le côté quitté (distance de vue du serveur, dix secondes) : vanilla retire ses tickets aussitôt, et sans relais ce côté se déchargeait en quelques ticks, avant que la fenêtre du portail de retour ne le reprenne. |
| `TeleportExecutor` | **Joueurs** : applique les franchissements annoncés par le client (`portal_cross`) après vérification (vitesse et regard finis et bornés compris), en s'alignant sur lui sans paquet de téléportation vanilla (le joueur est déjà de l'autre côté et garde son élan), et en secours détecte lui-même le passage des pieds entre deux ticks (segment position précédente → courante ; au point de passage, le centre de l'entité doit être en face de l'ouverture et sa boîte toucher le cadre : passer à côté ressortirait dans le mur qui encadre l'arrivée) avec cinq ticks de grâce ; un changement de dimension suit la séquence vanilla sans paquet de respawn. **Objets, mobs, projectiles** : pour chaque portail, les entités de sa boîte de traversée (cadre élargi de trois blocs, la vitesse d'une flèche) dont le déplacement du tick a franchi le plan sont transposées (`Entity.teleportTo` entre dimensions, repositionnement sinon) avec le cooldown de portail vanilla. Joueurs, portails, passagers et montures sont exclus de ce chemin. |
| `PortalViewTracker` | Synchronisation serveur → client des chunks, des blocs et des entités (voir ci-dessous). |
| `PortalSight`, `PortalSightPayload` | Ce que chaque joueur voit des portails, rapporté par son client (`portal_sight`, 64 portails au plus), pour la sortie fugace. Un rapport d'un autre monde ou de plus de 40 ticks ne compte pas, et un joueur sans rapport est tenu pour tout voir ; oublié à la déconnexion et à l'arrêt du serveur. |
| `RemoteEntityView` | Par joueur et par dimension distante : envoie la pluie et l'orage bruts de cette dimension (`WorldAccessor` : `getThunderGradient` les multiplie) quand ils changent, branche le joueur comme auditeur du suivi vanilla (`EntityTracker.listeners`) de chaque entité de sa fenêtre, lui envoie les paquets d'apparition, enveloppe tout ce que le suivi émet ensuite dans `portal_remote_entity`, et débranche ce qui sort de la fenêtre (destruction envoyée). Une entité arrivée par un portail dans la fenêtre est suivie tout de suite (`PortalViewTracker.onEntityArrived`), sans attendre l'alignement périodique : sa copie part au même tick que la destruction de l'ancienne. Une entité qui quitte son monde (morte, retirée, chunk déchargé, autre dimension) est effacée de même chez ceux qui la voyaient (`onEntityUnloaded`) : vanilla leur envoie sa destruction sans enveloppe, que le client appliquerait à son monde principal. |
| `PortalCollision` | Collision à travers les portails ouverts (`MixinEntity` sur `findCollisionsForMovement`, le pas comme la marche montée) : une entité dont les pieds sont devant le plan, le centre en face de l'ouverture, et dont la boîte touche le cadre heurte aussi les blocs de l'autre côté, au-delà du plan d'arrivée, ramenés de ce côté derrière le plan. On bute sur les murs qui encadrent l'arrivée, on monte sur un sol plus haut comme sur une marche : personne ne ressort dans un bloc. Le client l'applique à son joueur (index des portails et mondes distants), le serveur aux mobs, objets et projectiles, pas aux joueurs (il refuserait des pas que le client, sans monde distant, aurait faits librement). Les blocs de ce côté derrière le plan restent : le plan d'un bloc portail passe au milieu de son bloc et une entité plus étroite qu'un bloc l'a franchi avant d'y toucher. |
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
| `portal_remote_entity` | dimension + un paquet de jeu vanilla (`EntitySpawn`, `EntityS2C`, `EntityTrackerUpdate`, équipement, passagers, effets, animations, destruction…, pluie et orage de la dimension), encodé avec le codec `PlayStateFactories.S2C` des registres de la connexion | tout ce que le suivi vanilla d'une entité de la fenêtre émet, ainsi que les sons, évènements de monde et particules de la fenêtre (`relayToViewers`) ; le client l'applique à son monde distant comme à son propre monde |
| `portal_remote_block_breaking` | dimension, position, étape (-1 efface) | fissures d'un bloc miné à travers un portail |
| `portal_teleport` | dimension, type, position, regard, vitesse | le joueur vient de passer un portail : le client bascule sur son monde distant (ou aligne sa position s'il avait déjà prédit) ; sert aussi à ramener une prédiction refusée |
| `portal_cross` (client → serveur) | UUID du portail, position, vitesse, regard | la caméra du client a franchi le plan ; le serveur vérifie la plausibilité et applique exactement cet état |
| `portal_entity_action` (client → serveur) | portail, dimension, entité, frapper ou utiliser | clic sur une entité vue à travers un portail |
| `portal_block_action` (client → serveur) | portail, dimension, `BlockHitResult`, début/abandon du minage ou utilisation | clic sur un bloc vu à travers un portail |
| `portal_sight` (client → serveur) | portails que la caméra voit (UUID, 64 au plus) | toutes les 4 ticks si la liste change, au changement de monde (après une image du nouveau), au moins chaque seconde ; rien sans rendu distant |

Toutes les 5 ticks, pour chaque joueur : portails pertinents de son monde menant **ailleurs** →
par dimension distante, un **foyer** (le portail le plus proche) et la fenêtre de chunks
(`PORTAL_PRELOAD_RADIUS_CHUNKS`, 3 par défaut = 7 x 7 chunks, +2 quand le joueur est à moins de 8 blocs du portail) autour de son arrivée, plus celles des
autres portails qui tiennent dans la carte client (`RemoteChunkWindow.fitsAround`). Les déchargements
partent avant les chargements ; les entités de la boîte englobant les chunks envoyés sont ensuite alignées.
À moins de 8 blocs d'un portail, le serveur charge l'arrivée sur toute sa distance de vue (`DestinationLoader`) : la
traversée ne déclenche pas de génération. Au passage, les chunks que le client tient déjà ne lui sont pas renvoyés par
vanilla (`MixinChunkDataSender`) : ils sont à jour et déjà construits, le voisinage immédiat ne clignote ni ne ralentit.
Ils ne valent que 100 ticks : un chunk que vanilla ne propose qu'ensuite (hors de la distance de vue) a pu être oublié
par le client, il part comme les autres. Changement de dimension vanilla ou respawn : les deux côtés repartent de zéro,
et plus rien n'est tenu.
Côté client, les récepteurs s'exécutent tels que Fabric les appelle, sur le thread de rendu et dans l'ordre des paquets : les
différer avec `client.execute` les ferait passer derrière les paquets vanilla déjà en file (`ReentrantThreadExecutor`), et
`portal_teleport` arriverait après les premiers chunks du nouveau monde.

### Client

| Classe | Rôle |
|--------|------|
| `RemoteParticles` | Particules d'un monde distant : le gestionnaire de particules vanilla est unique (atlas, fabriques), seul son contenu (monde et files) est échangé avec celui du monde distant le temps d'y ajouter, d'y faire vivre ou d'y dessiner ses particules. Tout le code vanilla sert tel quel, les mondes ne se mélangent jamais ; à la traversée, le contenu du monde d'arrivée devient celui du gestionnaire et celui de l'ancien monde le suit. |
| `RemoteWorldRegistry` | Un `RemoteWorld` par dimension annoncée, plus les **vues secondaires** (créées à la demande pour les portails qui ramènent dans le monde principal, une par monde, gardées quand leur monde devient distant et reprises quand il redevient principal) ; tout est jeté quand vanilla change le monde principal, et les tampons de dessin d'un renderer libéré (hors du tas Java, jamais rendus par vanilla) servent au suivant. Tique les entités des mondes distants et l'horloge de chaque vue à chaque tick client. |
| `PortalSightReporter` | Toutes les 4 ticks, envoie au serveur les portails que la caméra voit : celui derrière lequel elle est passée en troisième personne, et ceux dessinés à la dernière image (déjà dans le champ de vision) dont l'un des neuf points du cadre se voit sans bloc opaque devant (verre, feuilles, dalles, portes laissent voir) ; envoi quand la liste change, au changement de monde (après une image du nouveau, jamais avec la liste de l'ancien) et au moins chaque seconde ; rien sans rendu distant (stencil refusé, rendu coupé), le serveur tenant alors le joueur pour tout voir. |
| `ClientPortalIndex` | Portails chargés par monde client (principal ou distant), tenus à jour par les événements de chargement d'entités : les parcours par image (visibilité, prédiction de traversée, visée) lisent cette liste au lieu de toutes les entités. |
| `StraddlingEntityRenderer` | Entités à cheval sur un portail, joueur compris, dessinées en deux moitiés coupées au plan : dans le monde du joueur après les vues (ou dans une vue qui couvre l'écran), la partie devant la face qui les coupe et la partie sortie d'une arrivée, puis la partie encore derrière l'arrivée d'un portail de ce monde, ramenée devant son cadre ; dans la vue d'un portail, la partie passée de ce qui est engagé dans son cadre, transposée. Décisions sur la position affichée de l'image ; rotation du passage précalculée pour chaque paire d'orientations, recherches dans des boîtes en cache, sans allocation propre par image (la recherche d'entités passe par l'index d'entités vanilla). |
| `RemoteViewRenderer` | Dessin d'un `RemoteWorld` depuis la caméra transposée, avec la projection du joueur et le brouillard de sa dimension (couleur comprise, `applyFogColor` : la vue ne se fond pas dans le brouillard du monde du joueur, une bande noire sur l'horizon de l'Overworld vu du Level 0) : ciel (shader des positions), couches de blocs, entités, block entities, fissures de minage, contour du bloc visé, particules, nuages, pluie et neige (`renderWeather` vanilla sur le renderer de la vue), le tout sous le plan de découpe du portail d'arrivée (`PortalClipping`). Les sections dessinées viennent d'un parcours synchrone propre à la vue (`ViewSectionWalker`), limité au frustum et au volume visible à travers le cadre (`ViewCone`) ; entités et portails imbriqués passent le même volume. La grille de sections du renderer est ancrée sur l'arrivée du portail, pas sur la caméra, et ne se recentre que si l'ancrage s'en éloigne de plus de deux chunks ; une seule vue par renderer et par image passe par le `setupTerrain` vanilla, pour garder son graphe de culling chaud en vue d'une traversée. |
| `RemoteWorld` | `ClientWorld` + `WorldRenderer` dédié ; état du monde distant (chunks, lumière, paquets d'entités, heure du monde principal, météo de sa propre dimension reçue du serveur), le dessin étant confié à `RemoteViewRenderer`. Le ciel vanilla se rapporte à la caméra de la vue (redirections dans `renderSky` : couleur du ciel, plan sombre sous l'horizon) et se dessine, comme vanilla, avec le shader des positions posé juste avant `renderSky`. Charge les chunks comme `ClientPlayNetworkHandler` (sections, heightmaps, lumière, statut de sections), applique les paquets d'entités avec le gestionnaire de paquets temporairement rebranché sur lui, tique ses entités (interpolation, animations) et, tant qu'une vue le montre, ses particules et les effets d'ambiance de ses blocs ; ses sons sont rejoués au cadre (`FrameSoundPlayer`). Il suit l'heure et la météo du monde principal (communes au serveur). Une vue secondaire partage un monde que vanilla entretient déjà (celui du joueur, ou un monde distant qui l'a été) et ne possède que son renderer, dont les sections à reconstruire suivent celles du renderer de son monde ; elle reste liée à son monde quand il devient distant. |
| `FrameAreaRenderer` | Zone de vue d'un portail : le cadre, plat. Quand la caméra est dans l'encadrement, à moins de 20 cm du plan, une boîte mince (le cadre prolongé de 20 cm derrière son plan), découpée en logiciel contre le plan proche de la caméra et refermée par un polygone : elle couvre exactement ce que la caméra voit à travers, sans plein écran forcé ni vide. Ailleurs, une boîte donnerait au portail une épaisseur (ses flancs montraient la vue à côté du cadre, et elle basculait d'un coup de l'autre côté du plan en passant d'une face à l'autre) : vu de profil, le cadre plat n'est plus qu'un trait. Elle se dessine sans élimination des faces arrière, le temps de son seul dessin : les couches de terrain vanilla supposent cette élimination active sans la rétablir, et une vue dessinée sans elle montrerait les deux faces de la glace, du verre et de l'eau (deux fois plus opaques qu'en vrai). |
| `PortalStencilRenderer` | Au milieu de `WorldRenderer.render`, après les blocs opaques, les entités et les block entities et avant tout ce qui est translucide : l'eau, le verre, les parties translucides des entités, les particules, les nuages et la pluie du monde du joueur se fondent ensuite par-dessus la vue. Les vues ont leurs propres tampons de dessin (ceux du monde principal attendent encore ses couches translucides), la matrice de vue empilée par vanilla est neutralisée le temps des portails, et brouillard et éclairage des entités du monde du joueur sont rétablis après. La valeur du stencil est le **niveau d'imbrication** (0 le monde du joueur, 1 une vue, 2 une vue imbriquée). Les portails se dessinent un par un, du plus proche au plus lointain : la zone de vue passe du niveau courant au suivant là où elle n'est cachée par rien, sa profondeur est poussée au plan lointain, le fond prend la couleur de brouillard de la vue, la vue distante est dessinée depuis la caméra transposée (lightmap recalculée pour cette dimension), puis ses **portails imbriqués** (sous le plan de découpe de la vue qui les contient), et enfin la zone reprend sa vraie profondeur (la face la plus proche) et redescend au niveau courant. Deux portails l'un derrière l'autre ne se mélangent pas : le plus proche cache l'autre. En troisième personne, quand le recul de la caméra traverse un portail (`ThirdPersonPortal`, juste après une traversée), l'image entière est la vue de l'autre côté depuis l'image de la caméra, reculée comme vanilla contre les blocs de là-bas : tout l'écran passe au niveau 1, la vue s'y dessine sans plan de découpe ni volume limité, avec ses portails imbriqués, et le stencil refuse la suite du rendu du monde du joueur jusqu'à la fin de l'image. |
| `PortalViewTransform` | Caméra distante = image de la caméra par le passage ; matrice de vue construite comme `GameRenderer` ; **plan de découpe** du portail d'arrivée exprimé dans l'espace caméra des shaders. La projection reste celle du joueur, pour le culling comme pour le dessin : aucune projection à plan proche oblique (elle dégraderait la précision de profondeur aux angles rasants). |
| `ShaderClipInjector`, `PortalClipping` | Au chargement, chaque vertex shader qui place ses sommets par `ModelViewMat` reçoit un uniforme `backrooms_ClipPlane` et écrit `gl_ClipDistance[0]` (`MixinShaderStage`) ; pendant le dessin d'une vue, `GL_CLIP_DISTANCE0` est actif et chaque shader activé reçoit le plan courant (`MixinShaderProgram`), un shader sans l'uniforme coupant la découpe le temps de son dessin. Pile de deux plans : la vue, puis la vue imbriquée ; ou, hors des vues imbriquées, la moitié d'une entité à cheval sur un portail (`StraddlingEntityRenderer`). Dans une vue imbriquée, les entités engagées dans le cadre d'arrivée, à cheval sur son plan, se dessinent après les autres, découpe suspendue (`PortalClipping.suspend`) : il n'y a plus de plan pour leur autre moitié, et coupées, leurs modèles sans élimination des faces arrière montreraient leur intérieur. |
| `ViewCone` | Pyramide caméra → arêtes du cadre d'arrivée, fermée par le plan du cadre : rien hors de ce volume n'apparaît dans la zone de vue, sections, entités et portails imbriqués sont écartés avant d'être dessinés. Sans limite quand la caméra elle-même est passée de l'autre côté (troisième personne). |
| `PortalRenderState` | Garde de réentrance et numéro d'image, centre de grille substitué au joueur dans `setupTerrain`, caméras des vues, coupe-circuit : stencil refusé par le pilote ou exception pendant un rendu distant → rendu distant coupé pour la session, portails en cadres sombres. |
| `RemoteTargeting` | Après le raycast vanilla de chaque image : si le regard traverse un cadre avant sa cible, rayon transposé dans la vue et poursuivi sur la portée restante (blocs puis entités) ; vanilla reçoit une cible manquée. |
| `RemoteActions` | Clics quand une cible existe de l'autre côté : frappe et utilisation envoyées au serveur, minage annoncé (début, abandon) sans prédiction, délais vanilla (4 ticks entre utilisations, 5 entre blocs cassés en créatif) ; sinon vanilla garde la main (coup dans le vide, manger). |

Une entité portail n'a **qu'une face** : vue de dos, elle n'est ni dessinée ni traversable (un bloc portail à deux faces en pose deux, dos à dos). Après les portails, le monde du
joueur reprend son brouillard, son éclairage d'entités et une couleur de shader blanche : sa couche translucide, ses
particules, la main et les objets de l'interface ne prennent pas la teinte de la dimension vue.

### Traversée sans coupure

Le changement de dimension vanilla envoie un paquet de respawn : nouveau `ClientWorld`, écran « chargement
du terrain », rendu reconstruit. Ici rien de tout cela :

1. **Prédiction** (`ClientPortalCrossing`, au début de chaque `GameRenderer.renderWorld`) : si les pieds du
   joueur, à la position affichée, ont franchi le plan d'un portail depuis l'image précédente (même test que le
   serveur, les portails étant verticaux), le client transpose lui-même position, regard et
   vitesse, échange le monde principal et le monde distant déjà chargé (`RemoteWorldRegistry.swapMainWorld`),
   déplace son joueur d'un `ClientWorld` à l'autre et envoie `portal_cross` avant son prochain paquet de
   déplacement. L'image suivante est déjà celle de l'autre côté, lightmap recalculée pour sa dimension (vanilla ne la
   recalcule qu'au tick). L'ancien monde et son renderer deviennent un monde distant : le portail de retour montre
   d'où l'on vient sans rien recharger.
2. **Serveur** (`TeleportExecutor`) : la demande est acceptée si le portail est dans le monde du joueur, hors
   cooldown, et si la position annoncée est à moins de 3 blocs de celle connue du serveur et à moins de 1,5 bloc de la
   boîte du cadre.
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
   Le client diffère de dix ticks le retrait de ces copies (`EntityHandover.deferRemoval`) : le serveur les renvoie
   presque aussitôt par l'autre voie, au même identifiant, et la copie renvoyée remplace l'ancienne sans trou. Retirées
   tout de suite, elles manqueraient quelques ticks : le portail de retour, juste derrière le joueur, disparaîtrait.
   Le même identifiant suppose que le serveur n'a pas recréé l'entité entre-temps : le ticket de départ
   (`DestinationLoader.holdDeparture`) garde chargé le côté quitté. Si une copie arrive tout de même sous un autre
   identifiant, l'ancienne est retirée avant que vanilla ne l'indexe (`MixinClientWorld.addEntity`,
   `EntityHandover.replaceStaleCopy`) : l'index des identifiants uniques refuserait la nouvelle, qui resterait affichée,
   figée, sans jamais être mise à jour ni retirée.
4. **Continuité du joueur** : le joueur reste le même objet (marche, balancement de la
   vue, objet tenu continuent). Le portail le tourne souvent d'un demi-tour : tout ce qui s'interpole passe avec lui,
   valeur courante et précédente ensemble, du même angle (regard, tête, corps et balancement du bras qui tient l'objet,
   sans quoi la main rattraperait le demi-tour en quelques images), ramené au plus court pour que le lacet reste continu,
   et les positions d'interpolation du modèle vu en troisième personne. Le serveur ne renvoie pas de paquet de vitesse
   après la traversée : arrivé un tick plus tard, il écraserait celle que le client a depuis (un saut rejoué).

Les **autres entités** ne sont pas prédites : le serveur transpose seul objets, mobs et projectiles d'après leur
déplacement du tick, en les retirant d'un monde et en les recréant dans l'autre. Chez celui qui regarde, la nouvelle
copie reprend l'état visible de l'ancienne (`EntityHandover`, même identifiant unique, retrait proche d'un portail
ouvert vers une autre dimension) : âge (animations d'attente), balancement des membres et du bras, blessure, tête et
corps tournés avec le portail, cape d'un joueur, et position affichée, d'où elle rejoint celle du serveur en trois
ticks comme tout déplacement reçu ; sans cela, une copie neuve repartirait membres au repos et sauterait d'un coup à
la position du serveur. Les copies recréées autour d'un joueur qui change lui-même de monde reprennent de même leur état.

Une entité à cheval sur un portail, le joueur compris, se dessine en deux moitiés, chacune coupée au plan dans le
monde où elle se trouve (`StraddlingEntityRenderer`). Coupée par une face de son monde (engagée dans le cadre, le centre
encore devant et en face de l'ouverture, comme pour la traversée ; une entité qui longe le bord reste entière de son
côté), vanilla ne la dessine pas (`MixinWorldRenderer.renderEntity`) : sa partie devant la face se dessine
dans son monde, coupée au plan, sa partie passée dans la vue du portail, transposée, la découpe de la vue ne gardant
que ce qui est au-delà du plan d'arrivée. Juste après sa traversée, son centre est passé : le plan d'arrivée la coupe,
sa partie sortie se dessine dans son monde, sa partie encore derrière devant le cadre d'entrée, dans le monde d'où
elle vient, transposée et coupée à son plan. Chaque décision porte sur la position affichée à l'image, interpolée
entre deux ticks comme vanilla la dessine, et non sur celle du tick, qui a jusqu'à un tick d'avance : la bascule du
joueur se décide de même, sinon il disparaîtrait l'image de son passage.

Le client montre les entités avec un peu de retard (l'interpolation, environ un bloc à la marche) : quand le serveur
téléporte une entité, sa copie affichée n'a pas encore atteint le plan. Sa destruction est donc différée
(`EntityHandover.applyDestroys`, à l'écoute des destructions vanilla comme distantes) jusqu'à l'arrivée de sa copie de
l'autre côté, qui la remplace aussitôt (le serveur l'envoie au même tick, y compris vers une dimension vue par un
portail). Seule une entité vivante qui, à sa vitesse, passe le plan en face de l'ouverture, vers l'arrière d'une
face, d'ici quelques ticks est gardée : la face franchie se reconnaît ainsi, les deux faces d'un bloc portail étant au même
endroit, et ce qui meurt, est ramassé ou longe le cadre part aussitôt. La nouvelle, reprise à la position affichée de l'ancienne, est souvent encore
derrière le plan d'arrivée, et reste une arrivée (`EntityHandover.arrivalPortal`), dessinée des deux côtés, jusqu'à
être sortie du cadre. Une entité téléportée par un portail de son propre monde passe le portail telle qu'elle est
affichée (`EntityHandover.onPositionPacket`) au lieu de glisser d'un bout à l'autre en trois ticks, puis en sort
comme une arrivée, chaque partie de son côté. Vanilla n'envoie une position absolue qu'au-delà de huit blocs : en deçà,
le serveur marque le suivi de l'entité comme après une monture (`PortalViewTracker.onEntityJumped`), et son prochain
envoi est absolu.

### Entités à travers les portails

Le serveur ne réécrit pas le suivi des entités : il réutilise celui de vanilla. Pour chaque entité de la fenêtre
d'un joueur, un `RemoteEntityView` (une implémentation de `PlayerAssociatedNetworkHandler` par joueur et par
dimension) est ajouté aux auditeurs du `EntityTracker` vanilla de cette entité. Le joueur reçoit alors les mêmes
paquets que les joueurs présents dans la dimension, enveloppés avec leur dimension (`portal_remote_entity`), et
son client les applique à son monde distant avec le gestionnaire de paquets temporairement rebranché dessus :
les mêmes classes d'entités, la même interpolation, les mêmes animations. Les mondes distants sont tiqués à chaque
tick client (`ClientWorld.tickEntities`) ; leurs particules vivent dans leur propre contenu (`RemoteParticles`) et
leurs sons sont rejoués au cadre (`FrameSoundPlayer`). Le rendu reprend l'ordre de vanilla : couches opaques, entités et block
entities (dispatchers configurés sur la vue le temps du rendu), couches translucides.

### Portails imbriqués et vue secondaire

Chaque portail visible dans une vue (dans son frustum et dans le volume visible à travers son cadre) est à son tour
dessiné, **un niveau plus loin** : sa zone passe du niveau 1 au niveau 2 du stencil là où elle passe le test de profondeur contre
la vue, sous le plan de découpe de celle-ci, sa profondeur est poussée au plan lointain, et sa propre vue est rendue
depuis la caméra transposée deux fois, avant que le cadre intérieur ne reprenne sa profondeur (un autre portail
imbriqué derrière lui n'y sera pas marqué). Au-delà de ce niveau, un portail est transparent. Quand la destination
d'un portail (direct ou imbriqué) est la dimension **du joueur**, la vue est une **vue secondaire** du monde
principal : un second `WorldRenderer` sur le même `ClientWorld`, dont les sections à reconstruire suivent celles du
renderer de ce monde (`MixinWorldRenderer.scheduleChunkRender`, d'après le monde du renderer, rendu de portail compris).
Elle reste liée à son monde : quand le joueur traverse, celle du monde quitté attend son retour (ses sections suivent
toujours celles du monde, devenu distant) et celle du monde d'arrivée, s'il en a déjà eu une, resert telle quelle. Créer
puis détruire un `WorldRenderer` à chaque traversée (une grille entière de sections et leurs tampons) figeait l'image,
surtout à la troisième personne, où la caméra restée derrière le cadre voit aussitôt le portail franchi, qui montre le
monde d'arrivée. Les portails intra-dimension se
dessinent donc aussi. Un portail imbriqué dont le chunk d'arrivée n'est pas disponible dans la vue (hors fenêtre
synchronisée, hors distance de rendu) reste un cadre sombre.

Chaque vue choisit elle-même ses sections (`ViewSectionWalker`) : le culling asynchrone de vanilla
(`ChunkRenderingDataPreparer`) tient un seul graphe par renderer, le recalcule en arrière-plan quand la caméra change
de section et filtre entre-temps l'ancien graphe par le nouveau frustum ; deux vues du même monde dans une image
alterneraient des listes incomplètes et des pans de la vue clignoteraient. La grille de sections d'un renderer est ancrée
sur l'arrivée du portail et ne se recentre que si un ancrage s'en éloigne de plus de deux chunks : la vue d'un portail
ne se redécoupe pas quand le joueur marche, et le monde qu'on vient de quitter garde sa grille telle quelle.
Le parcours d'une vue remplace la liste de sections du renderer : à la traversée, le renderer promu est marqué pour que
vanilla refasse la sienne à l'image suivante (`ClientPortalCrossing.switchWorld`). Vanilla ne la refait qu'après un
changement de son graphe ou un regard tourné de deux degrés ; un joueur qui ressortait sans tourner la tête et
s'arrêtait ne voyait que ce que montrait le portail, jusqu'à son prochain mouvement.

### Interaction à travers un portail

1. **Visée** (`RemoteTargeting`, après `GameRenderer.updateCrosshairTarget`) : le rayon du regard est testé
   contre le cadre de chaque portail visible ; s'il en traverse un avant la cible vanilla, il repart du point
   transposé dans la vue (un millième de bloc devant le plan d'arrivée) sur la portée restante, contre les blocs
   (`World.raycast`) puis les entités de la vue. `MinecraftClient.crosshairTarget` devient une cible manquée :
   vanilla ne touche plus au bloc caché par le cadre.
2. **Contour** : la vue dessine le contour du bloc visé (`WorldRenderer.drawShapeOutline`, car Fabric API intercepte `drawBlockOutline` hors de `render`) quand c'est elle
   que le regard traverse, si vanilla dessine lui-même les contours dans cette image.
3. **Clics** (`RemoteActions`, en tête de `doAttack`, `handleBlockBreaking`, `doItemUse`) : frappe et utilisation
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

### Ce qui traverse un portail ouvert

Fermé (`teleportable` faux) ou invisible, un portail ne laisse rien passer, lumière comprise.

- **Son** (`MixinPlayerManager`, `FrameSoundPlayer`, `MixinClientWorld`) : les sons et
  évènements de monde (`sendToAround`) et les particules diffusées (`ServerWorld.spawnParticles`) d'une dimension
  partent aussi, enveloppés avec leur dimension, aux joueurs dont la fenêtre synchronisée contient leur chunk. Côté
  client, tout son positionné d'un monde distant (paquet du serveur, feu, lave, entité) passe par les portails du
  monde du joueur qui mènent au monde du son, ceux par lesquels il voit l'autre côté, qu'ils aient un retour ou non
  (aller simple, portail invoqué seul) : il est rejoué au point du cadre le plus proche de la source vue par ce cadre
  (son image, derrière le plan), avec le volume qui lui reste après le trajet jusqu'au cadre (un seizième par bloc,
  portée de 16 à 64 blocs, `PortalSound`), si le joueur est devant la face ; le moteur de son atténue ensuite du
  cadre à l'oreille. Un son du monde du joueur à plus de 64 blocs passe de même par un portail
  intra-dimension. Un son d'un monde distant sans chemin jusqu'au joueur n'est pas joué. Les sons qui suivent une
  entité depuis son apparition (wagonnet, abeille) ne passent pas.
- **Particules** (`RemoteParticles`, `MixinWorldRenderer`) : chaque monde distant a son propre contenu de
  particules ; celles de ses entités, de ses paquets (coups critiques, explosions) et de ses évènements (bloc cassé)
  y naissent, avec les règles de vanilla (portée de 32 blocs, réglage des particules) autour de la caméra de la
  dernière vue. Tant qu'une vue de ce monde a été dessinée depuis moins de deux secondes, ses particules vivent à
  chaque tick et les effets d'ambiance de ses blocs (flammes, fumée, gouttes, cendres) sont tirés comme vanilla
  autour du joueur ; elles se dessinent dans la vue après les couches translucides, sous le plan de découpe.
- **Regard des mobs** (`PortalPursuit`, `PortalPursuitGoal`, `MixinActiveTargetGoal`, `MixinMobEntity`, `MixinZombieAttackGoal`). Un mob qui
  cherche un joueur et n'en trouve aucun dans son monde regarde aussi par les portails ouverts à sa portée de
  poursuite : il applique à chaque joueur de l'autre côté ses propres règles de ciblage (difficulté, créatif,
  équipe, colère des mobs neutres, discrétion), puis mesure distance et vue par le cadre (deux lancers de rayon, un
  de chaque côté). S'il voit un joueur, son objectif de poursuite (ajouté aux mobs qui ciblent des joueurs ou se
  vengent) lui donne la posture d'attaque (bras levés, arc bandé), le fait fixer le joueur là où il le voit par le cadre
  et marcher d'abord en face de l'ouverture, puis deux blocs derrière le plan, en ligne droite à travers elle (de
  biais, son chemin couperait le plan à côté du cadre) ; il traverse, et de l'autre côté le joueur devient sa vraie cible : il
  n'attaque qu'une fois passé. Sa copie neuve repart aussitôt vers le joueur (`PortalPursuit.resumeAfterCrossing` :
  cible, chemin, posture), sans attendre que ses objectifs la relancent ; le client lui garde la posture de l'ancienne.
  Vanilla baisse les bras d'un zombie les cinq premiers ticks d'une attaque : celui qui a déjà la posture quand son
  attaque commence (copie neuve de l'autre côté, ou passage de la poursuite à une cible de son côté) la garde.
  Un mob frappé à travers un portail fait de même : une cible dans un autre monde n'est jamais gardée telle quelle,
  vanilla y mesurerait des distances sans aucun sens. Les mobs à cerveau (piglins, villageois, warden) n'ont pas
  d'objectifs de ciblage et ne sont pas concernés.
- **Clic molette** (`RemoteActions.pick`) : le choix de bloc vanilla s'exécute avec la cible et le monde de
  l'autre côté à la place de ceux du joueur (bloc, données de block entity en créatif avec Ctrl, œuf de créature).

---

## Configuration (`ModConfig`, propriétés `-D`)

| Propriété | Défaut | Rôle |
|-----------|--------|------|
| `petassegang.portal.enablePortalFeature` | `true` | Coupe tout (le bloc portail reste inerte, les mixins ne font rien). |
| `petassegang.portal.preloadRadiusChunks` | `3` | Rayon de la fenêtre chargée et synchronisée autour de chaque arrivée (7 x 7 chunks). |
| `petassegang.portal.renderDistanceBlocks` | `64` | Distance de rendu et de synchronisation d'un portail. |
| `petassegang.portal.activationRadiusBlocks` | `16` | En deçà, un portail compte quelle que soit la direction du regard (préchargement, synchronisation, rendu) ; au-delà, seulement s'il est devant le joueur, jusqu'à la plus grande de cette distance et de `renderDistanceBlocks` côté serveur. |
| `petassegang.portal.teleportCooldownTicks` | `0` | Délai minimal entre deux traversées d'un joueur ; zéro par défaut, on enchaîne aller et retour librement (les objets et mobs gardent un cooldown vanilla fixe de 10 ticks). |
| `petassegang.portal.debugRendering` | `false` | Une ligne de log par seconde et par portail rendu (chunks chargés, entités, état du renderer). |

`./gradlew runClient -PportalPreloadRadiusChunks=2 -PportalDebugRendering=true` passe ces propriétés au client de dev.

---

## Limites connues

- **Un niveau d'imbrication** : un portail vu à travers un portail est dessiné, un portail vu à travers celui-là est transparent.
- **Météo** : les éclaboussures et le bruit de la pluie ne se tirent qu'autour de la caméra de la vue d'un monde distant ; dans la vue d'un portail qui ramène dans le monde du joueur, la pluie tombe sans éclaboussures.
- **Troisième personne** : la vue de l'autre côté dure tant que le recul de la caméra passe par le cadre ; regard incliné, il sort par le haut d'un cadre au bout d'un à deux blocs et la caméra revient dans le monde du joueur, derrière le cadre (sans quoi le joueur, de l'autre côté, ne serait visible qu'à travers l'ouverture). Une entité à cheval sur un cadre est dessinée des deux côtés ; dans une vue vue à travers une autre vue, qui n'a qu'un plan de découpe, celle qui sort de l'arrivée de cette vue se dessine entière, dans le cadre, et celle qu'un autre portail coupe aussi. Le temps du passage, elle n'a pas d'ombre au sol, et ce qui dépasse de l'ouverture derrière le plan (une entité plus large que le cadre) ne se voit pas.
- **Collision** : les blocs de l'autre côté ne comptent que pour une entité engagée dans le cadre ; les blocs de ce côté juste derrière le plan restent (sans effet pour un bloc portail, dont le plan passe au milieu du bloc).
- **Sons** : un son qui suit une entité depuis son apparition (wagonnet, abeille) ne traverse pas ; un son relayé l'est si son chunk est dans la fenêtre synchronisée du joueur, la portée réelle se décide ensuite côté client.
- **Mobs** : seuls les mobs à objectifs (zombies, squelettes, araignées, mobs neutres en colère…) voient et poursuivent à travers un portail ; un mob à distance (squelette) traverse au lieu de tirer à travers le cadre.
- **Interaction** : pas d'écran (coffre, four, enclume…) ni de monture à travers un portail, et rien pour un spectateur ; le balayage d'épée est évalué dans le monde du joueur ; les sons et particules du coup partent dans le monde du joueur, seul le son de frappe lui est rejoué.
- **Une grille par dimension distante** : la grille de sections d'un renderer est ancrée sur l'arrivée du portail visible le plus proche. Un second portail vers la même dimension dont l'arrivée est à plus de deux chunks de là ne montre que ce que cette grille couvre (rien si elle est hors de portée), et la reprend dès qu'il devient le plus proche.
- **Sortie fugace** : le regard est celui que rapporte le client ; un client sans le mod, ou qui ne rapporte plus rien (rendu distant coupé compris), compte comme regardant, et la sortie tient tant qu'il est là. Seuls les cubes pleins et opaques cachent un cadre : un portail dessiné mais caché par une entité, une plante ou un bloc partiel, ou par un bloc entre les neuf points testés du cadre, compte comme vu. Une sortie qui vient d'être posée tient deux secondes quoi qu'on en rapporte, le temps que les clients la voient.
- **Portails qui se recouvrent** : poser un bloc portail exactement au point d'arrivée d'un autre superpose leurs portails, et le joueur peut être renvoyé d'un côté à l'autre. Décaler l'un des deux.
- **Arrivée dans l'Overworld** : un bloc posé dans le Level 0 pose son cadre d'arrivée sur la surface aux mêmes X/Z, sans vérifier qu'il y tient (relief, arbre) ; le chunk de cette surface se charge à la pose, et se génère s'il ne l'a jamais été (un à-coup).
- **Caméra contre le bord d'un cadre** : à moins de 5 cm du plan et juste à côté de l'ouverture, le plan proche de la projection coupe le cadre plat, et une mince bande de la vue manque près du bord de l'écran ; dans l'encadrement, le cadre devient une boîte qui couvre ce cas.
- **Grands cadres dans le Level 0** : faute de cellule où le cadre tient sur toute sa largeur dans le rayon de recherche, il se pose au centre de la cellule ouverte la plus proche et peut entrer dans les murs ; plus haut que quatre blocs, il entre dans le plafond.
- **Lumière** : seule la lumière des blocs passe, pas celle du ciel ; une lumière retirée s'éteint en quelques passes (deux niveaux par aller-retour entre les deux côtés).
- **Traversée** : la prédiction client n'a lieu que si le monde distant est déjà chargé ; en sprintant vers un portail qu'on vient de poser, c'est le serveur qui bascule, avec un tick de latence. À moins de 16 blocs d'un portail (distance d'activation), l'autre côté se synchronise quelle que soit la direction du regard : on peut s'y engager à reculons ou de côté. Montures et passagers ne traversent pas.
- **Mods de rendu** : les mods qui remplacent le pipeline de terrain ne sont pas supportés ; désactiver la feature avec eux.
- **Mémoire** : un `WorldRenderer` supplémentaire par dimension distante, plus une vue secondaire par dimension où un portail a ramené le joueur dans son propre monde (quelques Mo de tampons de construction chacun, gardés jusqu'à la déconnexion ou au changement de dimension vanilla).
- **Première vue secondaire** : la première fois qu'une dimension en a besoin dans une partie (portail imbriqué qui y ramène, troisième personne juste après la première traversée vers elle), son `WorldRenderer` se crée dans l'image : un à-coup unique, de l'ordre de 30 à 100 ms à 12 chunks de distance de rendu, et une image où cette vue n'a encore rien de construit (couleur de son brouillard, noir dans le Level 0). De même la première fois qu'elle sert loin de là où elle a déjà servi.

---

## Tests

| Niveau | Tests |
|--------|-------|
| Unitaires | `PortalFacingTest` (rotation, pas de miroir, entrée = sortie, sur les 36 paires ; faces arrière qui transposent comme les faces avant), `BackroomsPortalEntityTest` (dont distance signée sans allocation, boîtes de traversée et d'arrivée en cache et refaites quand la taille change, boîte à cheval sur le cadre d'entrée ou d'arrivée, décalée à la position affichée, face franchie par une trajectoire qui passe en face de l'ouverture à bref délai, centre de cadre), `BackroomsPortalBlockTest` (bloc invisible, cadre et réglages, sauvegarde et copie, info-bulle), `PortalSettingsTest` (format, bornes, piles, colonnes, centre et pied du cadre), `PortalTargetsTest` (cadre qui tient sur toute sa largeur, couloir barré, repli), `VirtualLightSourcesTest`, `PortalRelevanceTest` (dont le regard donné en angles), `TeleportExecutorTest` (traversée du plan, le centre en face de l'ouverture et la boîte touchant le cadre ; ni apparition ni saut de position ; cooldown des joueurs ; annonce à la vitesse et au regard finis et bornés), `PortalCollisionTest` (côté de sortie d'une boîte, image d'une boîte d'un portail à l'autre, entité engagée), `ThirdPersonPortalTest` (recul de la caméra à travers le cadre, au-dessus du linteau, portail le plus proche), `PortalPayloadsTest` (onze payloads, dont un paquet d'entité vanilla enveloppé, les actions à travers un portail et les portails vus, plafonnés), `PortalViewTrackerTest` (paquets relayés), `RemoteChunkWindowTest`, `RemoteEntityViewTest` (boîte de la fenêtre refaite seulement quand une borne bouge), `PortalViewTransformTest` (plan de découpe en espace caméra, caméra transposée), `ViewConeTest` (volume visible à travers le cadre, sans limite), `ShaderClipInjectorTest` (54 des 55 vertex shaders vanilla du jar, `blit_screen` exclu, un seul plan en tête de `main`), `PortalRenderStateTest` (garde de rendu, numéro d'image, coupure du rendu distant), `RemoteViewRendererTest` (ancrage de la grille), `PortalSoundTest` (portée, volume après le trajet, sortie par le cadre où le joueur voit l'autre côté), `PortalPursuitTest` (point de passage du regard dans le cadre, point d'approche en face de l'ouverture, point visé) |
| En jeu (`runGameTest`) | `PortalSyncGameTests` (fenêtre de chunks et entités vers le Nether, entité distante retirée effacée au même tick, chunks tenus valables 100 ticks après une traversée, annonce refusée sous cooldown ou à la vitesse impossible, météo du Nether envoyée avec elle puis à chaque changement seulement, entité arrivée par un portail envoyée au même tick, traversée entre dimensions sans renvoi des chunks tenus, oubliés une fois tous consommés, côté quitté gardé chargé avec les mêmes entités, annonces client, marche détectée par le serveur, changements de monde vanilla, portails orphelins, commande de debug), `PortalInteractionGameTests` (coups, tonte, clics, porte ouverte et fermée, pose et minage à travers le cadre, fissures effacées quand le minage cesse, refus dont le spectateur et le cochon sellé), `BackroomsPortalBlockGameTests` (bloc portail : copie qui garde la destination et pose ses propres portails, réglages seuls envoyés aux clients, une face sans rien derrière, aller simple sans retour, sortie fugace tenue tant que le client d'un joueur la voit (ou ne rapporte rien de récent dans ce monde) et deux secondes après sa pose, perdue pour de bon dès qu'il ne la voit plus (jamais reposée, neuve pour une copie), un joueur trop loin ne comptant pas, portail que son bloc ne tient plus pour sien retiré, pose face au joueur avec les réglages de l'item, cadre bloqué refusé, visé seulement accroupi, quatre portails dont deux faces de chaque côté, retours inverses exacts, bloc tourné ou relu avec d'autres réglages, traversée par la face arrière, retirés avec lui, destination absente, arrivée posée sur le haut réel du sol (dalle, tapis, eau), portails perdus, Level 0 prêté, portails désactivés) ; `PortalLightGameTests` (seuls les portails ouverts, visibles et réglés pour la lumière éclairent l'autre côté, la plus forte l'emporte, source pas encore appliquée signalée de nouveau ; cadre d'un portail qui apparaît revérifié, lumière fantôme éteinte ; portail double face 4 x 4 éclairé des deux côtés, éteint avec sa lumière) ; `PortalPropagationGameTests` (sons, évènements et particules relayés au joueur qui regarde ; un husk voit un joueur par le cadre, traverse, le cible et repart aussitôt vers lui, mais rien par un portail fermé, hors de portée ou masqué ; une cible dans un autre monde devient une poursuite ; chaque règle d'une poursuite vérifiée seule) ; `PortalGameTests` : traversée avec un joueur serveur (position, regard) ; un objet lancé dans le cadre ressort de l'autre côté avec sa vitesse transposée et le cooldown vanilla ; un saut de moins de huit blocs part aux clients en position absolue, un marqueur que vanilla ne suit pas traverse aussi ; un objet poussé de biais dans le cadre bute sur le mur de l'autre côté, au milieu rien ne l'arrête, ni derrière le plan, ni portail fermé, ni portail vers une dimension absente ; `RemoteChunkPayload` d'un vrai chunk octet pour octet |
| Non couvert automatiquement | le rendu (stencil par niveau, découpe par les shaders, brouillard, lightmap, entités, particules, portails imbriqués, transparence, troisième personne), la continuité des animations, le son côté client et l'interaction se valident en jeu : `./gradlew runClient`, poser un bloc portail, regarder à travers par ses deux faces (un mob de l'autre côté bouge), jeter un objet dedans, frapper ou miner à travers, traverser, revenir |
| Reproduction en jeu | `./gradlew runClient -PquickPlayWorld="Nom du monde" -PportalDebugRendering=true` charge un monde solo directement ; en cas de gel, `jstack` sur le PID `devlaunchinjector` donne la pile du thread de rendu |
