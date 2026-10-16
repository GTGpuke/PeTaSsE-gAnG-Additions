# Changelog

Toutes les évolutions notables de PeTaSsE_gAnG_Additions sont documentées ici.
Format : [Keep a Changelog](https://keepachangelog.com/fr/1.0.0/). Versions : [SemVer](https://semver.org/lang/fr/).

---

## [Unreleased]

---

## [0.9.0] - 2026-10-16

Système de portails complet : porte des Backrooms, téléportation sans coupure (joueurs, objets, mobs, projectiles),
synchronisation des chunks et des entités de l'autre dimension, rendu du monde distant à travers le cadre avec un
niveau de portails imbriqués, et interaction à travers le cadre. Conception, protocole et limites dans [PORTALS.md](PORTALS.md).

### Added
- **Porte des Backrooms** (`backrooms_door`) en item, dans un nouvel onglet créatif *PetasseGang - Portails*.
  Posée hors du Level 0, elle y mène (cellule réellement libre la plus proche des mêmes X/Z, sortie vers une
  cellule ouverte) ; posée dans le Level 0, elle ramène à la surface. Portail d'entrée devant la porte, portail
  de retour à l'arrivée ; les deux disparaissent avec la porte et se retirent seuls s'ils deviennent orphelins.
- **Synchronisation serveur → client** des chunks de l'autre dimension (payloads `portal_remote_dimension`,
  `portal_remote_chunk`, `portal_remote_chunk_unload`, `portal_remote_block_update`) : fenêtre de 7x7 chunks
  autour de chaque arrivée regardée, 8 chunks par joueur toutes les 5 ticks, changements de blocs relayés.
  Fonctionne en multijoueur (le prototype ne marchait qu'en solo).
- **Rendu du monde distant** à travers le cadre : stencil par portail, caméra transposée, lightmap recalculée
  pour la dimension distante, projection à plan proche oblique (rien n'est dessiné derrière le portail
  d'arrivée), profondeur du cadre restaurée après le rendu. Portails à sens unique (invisibles de dos).
- Coupe-circuit : stencil refusé par le pilote ou exception pendant un rendu distant → rendu distant coupé pour
  la session et portails en cadres sombres ; vérification que la substitution de caméra du mixin est active.
- **Traversée sans coupure** : le client prédit le passage dès que la caméra franchit le plan, bascule sur le
  monde distant déjà chargé (pas de paquet de respawn, pas d'écran de chargement, pas de rendu reconstruit) et
  garde l'ancien monde pour voir derrière soi ; le serveur applique l'état exact annoncé par le client après
  vérification (`portal_cross` → `portal_teleport`) ou le ramène en arrière.
- **Entités visibles à travers les portails** : le joueur est branché sur le suivi vanilla (`EntityTracker`) des entités
  de sa fenêtre distante ; leurs paquets lui parviennent enveloppés (`portal_remote_entity`) et son client les applique
  à son monde distant, tiqué à chaque tick (interpolation, animations). Entités, block entities et fissures de minage
  sont rendues dans la vue, dans l'ordre de vanilla.
- **Objets, mobs et projectiles traversent** les portails : transposition serveur d'après le déplacement du tick
  (`Entity.teleportTo` entre dimensions), vitesse et regard transposés, cooldown de portail vanilla.
- **Portails imbriqués** (un niveau) : les portails visibles dans une vue sont dessinés à leur tour (bit haut du
  stencil, projection de la vue qui les contient). Les portails qui ramènent dans la dimension du joueur (imbriqués
  ou intra-dimension) passent par une **vue secondaire** du monde principal : second `WorldRenderer` sur le même
  monde, sections à reconstruire répercutées depuis le renderer principal.
- **Interaction à travers le cadre** : visée transposée après le raycast vanilla (contour du bloc visé dans la vue),
  frappe, utilisation et minage envoyés au serveur (`portal_entity_action`, `portal_block_action`), qui valide portée
  et portail puis exécute l'action vanilla avec le joueur transposé de l'autre côté le temps de l'appel ; minage
  compté par le serveur avec fissures (`portal_remote_block_breaking`). Pas d'écran ni de monture à travers un portail.
- Vue distante complète : brouillard de la dimension depuis la caméra transposée, ciel (soleil, lune, étoiles), nuages,
  heure et météo alignées sur le monde principal (communes au serveur) ; le fond du cadre prend la couleur du brouillard.
- Zone de vue en boîte mince découpée contre le plan proche (plus de cadre plein écran forcé ni de vide au bord du
  portail) ; brouillard remis à zéro après le rendu des portails (plus de main ni d'objets teintés) ; ciel de la vue
  dessiné avec la projection normale.
- Tests : `BackroomsDoorGameTests` (porte → deux portails liés et propriétaires → cassée → aucun portail), `PortalGameTests`
  (traversée par un joueur serveur avec position, regard et cooldown ; objet lancé dans le cadre ressorti de l'autre côté ;
  payload d'un vrai chunk octet pour octet),
  `TeleportExecutorTest`, `BackroomsDoorTargetsTest`, `PortalRelevanceTest`, `PortalPayloadsTest`,
  `RemoteChunkWindowTest`, `PortalViewTransformTest` ; `PortalFacingTest` réécrit sur les 36 paires d'orientations.
- Textures 32x32 de la porte (`backrooms_door`, `backrooms_door_top`).

### Changed
- `ModConfig.ENABLE_PORTAL_FEATURE` vaut `true` par défaut (`-Dpetassegang.portal.enablePortalFeature=false` coupe tout).
- `ModConfig.PORTAL_PRELOAD_RADIUS_CHUNKS` vaut `3` par défaut (fenêtre de 7x7 chunks : les arrivées voisines tiennent dans la même vue).
- Arrivée sans saccade : après une prédiction client, le serveur s'aligne sans paquet de téléportation vanilla (plus de
  retour en arrière d'un tick ni de perte d'élan), et le client ignore une confirmation identique à sa prédiction.
- Plus de délai entre deux traversées (`teleportCooldownTicks` vaut 0 par défaut) ; objets et mobs gardent un cooldown fixe.
- Changement de dimension plus fluide : à moins de 8 blocs d'un portail, l'arrivée est chargée sur la distance de vue du
  serveur et la fenêtre synchronisée s'élargit ; au passage, les chunks que le client tient déjà ne sont pas renvoyés.
- Le ciel d'une vue se rapporte à sa caméra (plus de plan sombre tiré depuis la hauteur du joueur dans le Level 0).
- Couverture de tests verrouillée à 100 % des instructions et 96 % des branches du code mesuré : l'agent JaCoCo accompagne
  le serveur GameTest, `jacocoFullReport` fusionne tests unitaires et tests en jeu, `jacocoCoverageVerification` échoue
  sous ces seuils et `build` l'enchaîne. Hors mesure, listé dans `build.gradle` et `docs/TESTING.md` : la colle OpenGL du
  client et les mixins. Branches mortes retirées au passage (gardes sur des invariants : fenêtre d'arrivée toujours
  enregistrée, portails du registre toujours serveur, vue de joueur toujours inscrite, `switch` sans cas inatteignable).
- Tests en jeu déplacés dans un source set dédié `src/gametest` (Loom `fabricApi.configureTests`) : mod séparé
  `petasse_gang_additions_gametest` chargé par `runGameTest` seulement, dossier de run `build/run/gameTest`, arborescence par
  domaine (`support/`, `backrooms/level0/`, `backrooms/portal/`, `feature/`). Plus aucun
  test dans le JAR, ce que la tâche `verifyJarContents` vérifie à chaque `build` (JAR = le mod seul, sans dépendance embarquée).
- Conventions du code vérifiées par Checkstyle (`config/checkstyle/`) dans `check` : sources ASCII, ordre des imports sans
  wildcard, logger unique, Javadoc des types publics, classes utilitaires finales, mise en forme. Zéro avertissement toléré.
- CI : Checkstyle, tests unitaires, tests en jeu, couverture, `build` avec contrôle du JAR, benchmark, rapports publiés ;
  release : `clean build` complet avant publication ; Dependabot pour les actions GitHub.
- Tests en jeu étendus : synchronisation et traversée vers une autre dimension (le Nether sur le serveur de test),
  annonces client, marche détectée par le serveur, changements de monde vanilla, portails orphelins, commande de debug,
  interaction et minage à travers le cadre, objets, mur adaptatif, cas limites de la porte ; joueurs simulés connectés
  (`GameTestPlayers`), arène de test de 24 blocs. Cooldown de traversée de 3 ticks sur le serveur de test. Cas ajoutés
  ensuite : refus d'annonce (portail fermé, sans dimension, hors du cadre, en bateau), entités qui ne traversent pas,
  fenêtres partagées et fusionnées, bordure du monde, opérateur sur bloc de commande, netherrack sans récolte, porte
  tournée, portail de retour reposé.
- Tests unitaires étendus : classes utilitaires, état de rendu, transformation de vue, onglets, point d'entrée, NBT et
  tracker du portail, départage, connexions de cellules, biomes, détails muraux, writer en debug, secteur à remplissage total.
- Plus d'allocation par image ni par tick dans les portails : prédiction de traversée en primitifs, frustums réutilisés
  (`FrustumAccessor`), centre d'arrivée du portail en cache, boîte d'entités reconstruite seulement si la fenêtre change ;
  `ClientPortalIndex` remplace les parcours de toutes les entités par image ; le rendu des vues sort de `RemoteWorld`
  (`RemoteViewRenderer`).
- Code mort retiré : arbitrage entre gaps de couloir voisins (impossible par construction du mélange de hash, génération
  inchangée, empreintes identiques), gardes jamais atteintes dans les biomes, les détails muraux et la porte ;
  départage des grandes pièces partagé (`LevelZeroTieBreak`) ; `ModPerformanceMonitor` sans branche de concurrence ;
  `BackroomsDoorTargets.resolve` par clé de dimension.
- Documentation : plus aucune mention d'un mod tiers dans la doc du projet (`DEPENDENCIES.md`, `AUDIT.md`).
- Le dôme du ciel d'une vue se dessine avec le shader des positions, posé avant `renderSky` comme le fait vanilla : le
  ciel de l'overworld vu depuis une autre dimension n'est plus noir (il prenait le shader position-couleur laissé par le
  fond du cadre, dont l'attribut couleur absent vaut noir).
- La porte n'a plus de collision (le joueur qui en ressort n'est plus poussé par le bloc) et le bas du cadre se pose sur
  le sol trouvé devant elle (porte encastrée ou sur une marche comprises).
- **Géométrie du passage** (`PortalFacing`) : rotation de 180° autour de l'axe vertical du portail. Ce qui
  est devant la source est derrière la destination, entrer dans l'un fait sortir de l'autre, la droite reste la
  droite. Le prototype faisait ressortir le joueur face au portail de sortie.
- Téléportation : décidée uniquement sur une vraie traversée du plan depuis le côté d'entrée (plus de
  déclenchement par proximité), joueurs itérés sans copie, boîte de détection en cache.
- Préchargement : un ticket par fenêtre d'arrivée, partagé entre portails, chargement asynchrone (plus de
  chargement synchrone de 25 chunks sur le thread serveur).
- Entity portail : porte propriétaire en NBT ; `BackroomsPortalEntityRegistry` devient une liste itérée par index.
- Noms alignés sur Yarn : `ClientWorldRegistry` → `RemoteWorldRegistry`, `RemoteLevelRenderer` → `RemoteWorld`,
  `MixinLevelRenderer` → `MixinWorldRenderer`, `PortalDepthTracker` → `PortalRenderState`.
- Commande de dev réduite à `/backrooms portal list` et `/backrooms portal clear` (la porte remplace
  `spawn_test` et `spawn_door`).

### Removed
- Déplacement du joueur pendant la frame pour centrer le renderer distant (remplacé par une substitution de
  position dans `WorldRenderer.setupTerrain` par mixin), miroir des chunks du serveur intégré, `RemoteWorldFactory`,
  `RemoteWorldRenderInfo`, `GameRendererAccessor`, propriété `portalDebugThrowDuringRemoteRender`,
  `PortalFacing.transformYaw` (déprécié).

---


## [0.8.0] - 2026-10-15

Refonte complète du projet : structure, génération, tests, outillage et documentation.
Détail et mesures dans [AUDIT.md](AUDIT.md).

### Changed
- **Outillage** : Gradle 9.8.1, Fabric Loom 1.17.21, Fabric Loader 0.19.5, Fabric API 0.116.17+1.21.1,
  JUnit 5.11.4, `fabric-loader-junit`, JaCoCo, run `gametest`. Un seul JDK (21) pour tout.
- **Structure** : `backrooms/level/level0` → `backrooms/level0` ; `creative/`, `network/` fusionnés dans `init/` ;
  `perf/section` → `perf` ; features aplaties (`feature/gang/GangBadgeItem`, `feature/cursed/CursedSnackItem`).
- **Génération Level 0** : le pipeline en stages/façades/builders est remplacé par cinq classes lisibles
  (`LevelZeroLayout`, `LevelZeroCellEvaluator`, `LevelZeroTopology`, `LevelZeroLightPattern`,
  `LevelZeroCorridorGaps`) et un writer unique. **Sortie identique** : vérifiée par empreintes sur
  3 seeds x 5 layers x 49 chunks (`LevelZeroGoldenTest`).
- **Performance de génération** : cache de secteurs LRU borné (plus de plafond qui désactivait le cache),
  secteurs 2,5x plus compacts, une lecture de secteur par chunk au lieu d'une par cellule, court-circuit du
  calcul des faces de papier peint hors frontière de région, suppression de la concaténation de chaînes et
  des allocations par colonne, suppression de l'arbitrage lumineux entre voisins (code mort démontré).
- **Rendu client** : `LevelZeroWallpaperBakedModel` lit la pile verticale canonique ; plus de constantes Y héritées (60-67).
- **Portails** : logger commun, feature-flag appliqué au mixin de framebuffer et au rendu stencil
  (plus aucun coût par frame quand désactivé), texture d'entity inexistante remplacée.
- **Config** : `-Dpetassegang.levelZero.noiseGeometry` est maintenant réellement appliqué (il était ignoré).
- **Documentation** : README, CLAUDE.md, docs techniques et skills réécrits sur l'état réel du code.

### Fixed
- **Frontières de chunk** : la fenêtre de traversabilité déborde de deux cellules autour du chunk.
  Topologie, néons et gaps de couloir sont désormais identiques de part et d'autre d'une frontière
  (auparavant les cellules de bord voyaient un mur fictif hors du chunk ; les gaps sur les couloirs
  traversant une frontière peuvent donc apparaître là où ils étaient absents).
- **Mur adaptatif posé à la main** : le recalcul du masque de faces échantillonnait Y=63 (ancienne géographie),
  il utilise la hauteur du bloc et le layer réel.
- **Version dans les logs** : le log de démarrage affichait le mod_id à la place de la version.
- **Compilation** : zéro warning javac.

### Removed
- Plinthe en bloc (`level_zero_baseboard`, item, modèle de bloc et de plinthe autonome) : rendue invisible et
  sans collision depuis le passage au rendu en overlay. **Les blocs déjà posés disparaissent au chargement.**
  Le rendu des plinthes sur les murs est inchangé.
- Couches sémantiques jamais rendues en jeu : `surface details` (gelée, flag toujours false) et
  `structures` rares (debug uniquement), avec leurs flags `surfaceDetails` et `debugStructures`.
- `ModConfig.ENABLE_GANG_BADGE` (jamais lu), stubs dépréciés, 122 `.gitkeep` de dossiers vides,
  `package-info.java` d'une ligne, `META-INF/mods.toml` (Forge), `atlases/blocks.json` (sans effet),
  `assets/.../items/*.json` (format 1.21.4, ignoré en 1.21.1), docs d'audit de l'ère Forge, spec dupliquée.

---

## [0.7.0] - 2026-04-21

### Changed
- **Backrooms Level 0** : génération structurée autour d'une pile verticale multi-layer déterministe
  (`LevelZeroLayerStackLayout`, `LevelZeroVerticalSlice`), slices seedées par layer.
- **Lumière Level 0** : trame globale en quinconce filtrée par biome ; grandes pièces parfois sombres.
- **Micro-géométrie Level 0** : gaps de couloir alignés sur les directions sémantiques.
- **Performance debug** : moniteur de performance opt-in client et serveur.
- **Tests** : extension de la couverture JUnit du Level 0.

---

## [0.6.0] - 2026-04-15

### Changed
- **Migration Forge → Fabric** : réécriture complète de Forge 62.0.x / MC 26.1 / Java 25 vers
  Fabric Loader 0.16.9 / Minecraft 1.21.1 / Java 21 (`fabric.mod.json`, `Registry.register`,
  `Item.Settings`, `Text.translatable`, réseau `ServerPlayNetworking`, mappings Yarn).
- **Encodage Windows** : `-Dfile.encoding=COMPAT` dans `org.gradle.jvmargs`.
- **`cursed_tree.json`** : `below_trunk_provider` remplacé par `dirt_provider`.

---

## [0.5.0] et antérieurs

Prototype Forge : Gang Badge, Casse-croûte Maudit, Arbre Maudit, première dimension Level 0.
