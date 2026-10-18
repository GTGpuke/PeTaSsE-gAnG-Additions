# CLAUDE.md — GTG's Backrooms

> Lu automatiquement à chaque session. Règles obligatoires du projet.
> Onboarding complet : `docs/prompt/NOKNOWLEDGE.md` — Audit pré-push : `docs/prompt/BEFOREPUSH.md` — Vérification finale avant commit : `docs/prompt/FINALCHECK.md`
> Dernier audit complet : `docs/AUDIT.md`

## Stack

Minecraft **1.21.1** / Yarn `1.21.1+build.3` / Fabric Loader **0.19.5** / Fabric API **0.116.17** / Fabric Loom **1.17.21** / Gradle **9.8.1** (wrapper) / Java **21** (toolchain et JVM Gradle)
`mod_id` : `gtgs_backrooms` — package racine : `com.gtgpuke.gtgsbackrooms`

## Commandes

```bash
./gradlew test                          # JUnit 5 sous le classloader Fabric + rapport JaCoCo (build/reports/jacoco)
./gradlew runGameTest                   # Tests en jeu (serveur headless, sous agent JaCoCo) : Level 0, portails, contenu
./gradlew jacocoCoverageVerification    # Couverture fusionnee unitaire + en jeu : echoue sous 100 % des instructions ou des branches (lance par build)
./gradlew checkstyleMain checkstyleTest checkstyleGametest   # Conventions du code (config/checkstyle), lances par build
./gradlew verifyJarContents             # Le JAR ne contient que le mod : ni dependance embarquee, ni test (lance par build)
./gradlew benchmarkLevelZeroGeneration  # Benchmark déterministe layout + écriture (option -PlevelZeroPerfBudgetMsPerChunk=X)
./gradlew runClient                     # Client de dev ; toggles : -PdebugPerformanceMonitor=true -PportalFeature=false -PportalDebugRendering=true ...
./gradlew runClient -PquickPlayWorld=MonMonde    # Idem, en chargeant directement un monde solo (nom avec espace sous Git Bash : ORG_GRADLE_PROJECT_quickPlayWorld="New World" ./gradlew runClient)
./gradlew build                         # JAR dans build/libs/, apres check : Checkstyle, tests, tests en jeu, couverture, contenu du JAR
```

Un seul JDK suffit : Java 21. Si `JAVA_HOME` pointe sur un autre JDK, Gradle détecte le 21 installé (toolchain).

## Structure (voir docs/ARCHITECTURE.md)

- `init/` : registres (`Registry.register` dans des champs `static final`), onglet créatif, payloads.
- `feature/gang`, `feature/cursed` : contenu gameplay, une classe par item.
- `backrooms/level0/generation/` : `coord` → `noise` → `sector` → `layout` → `write`, orchestrés par `LevelZeroChunkGenerator`.
- `backrooms/level0/client/model/` : rendu adaptatif du mur (client uniquement).
- `backrooms/portal/` : portails (bloc portail double face et ses réglages, entity, téléportation de tout ce qui traverse, collision à travers le cadre, synchronisation des chunks et des entités, rendu stencil avec un niveau d'imbrication et la météo de l'autre côté, interaction à travers le cadre, sons, particules et regard des mobs qui le traversent), actifs par défaut (`ModConfig.ENABLE_PORTAL_FEATURE`). Lire `docs/PORTALS.md` avant d'y toucher.
- `perf/` : moniteur de performance opt-in.
- `mixin/` : commun (relais des changements de blocs, chunks tenus non renvoyés, sons et particules relayés, collision à travers le cadre, regard et poursuite des mobs, sources de lumière virtuelles des portails, accesseurs du suivi vanilla des entities et de la météo brute d'un monde) et `client/` (rendu des portails, découpe dans les shaders, monde distant, entités à cheval sur un portail, copies d'entités périmées remplacées, visée et clics à travers le cadre).
- `src/gametest/` : tests en jeu, mod séparé `gtgs_backrooms_gametest` chargé par `runGameTest` seulement (jamais dans le JAR, `verifyJarContents` le vérifie).

## Règles de code

- Commentaires, logs, messages d'erreur : **en français**, majuscule initiale, point final. Pas d'accents dans le code (sources ASCII).
- Contenu visible en jeu : `Text.translatable()` + clés dans `en_us.json` **et** `fr_fr.json` (test `ModRegistriesTest` le vérifie).
- Imports : `java` → bibliothèques tierces (`com.mojang`, `com.llamalad7` (MixinExtras), `org.*`) → `net.minecraft` → `net.fabricmc` → `com.gtgpuke`, aucun wildcard.
- Logs via `ModConstants.LOGGER` uniquement ; aucun `System.out` hors benchmark.
- Aucune allocation dans les méthodes tick/render ; pas de concaténation de chaînes dans les chemins chauds (y compris les noms de sections perf).
- Pas de couche d'abstraction sans deuxième implémentation réelle : pas d'interface/stage/builder spéculatif.
- Pas de dossier vide ni de `.gitkeep`, pas de `package-info.java` d'une ligne.
- Licence propriétaire, tous droits réservés (`LICENSE`, copiée dans les JAR) : aucun code ni aucune ressource repris
  d'un projet tiers, quelle que soit sa licence.

## Génération du Level 0 : règle d'or

La génération est **déterministe et verrouillée** par `LevelZeroGoldenTest` (empreintes par seed et layer).
Toute modification qui change ces empreintes modifie les mondes existants : elle doit être voulue,
annoncée dans `docs/CHANGELOG.md`, et les empreintes doivent être régénérées (relancer le test, copier les valeurs).
Les sels de `StageRandom.Stage` et l'algorithme de `LevelZeroSectorGenerator` ne se modifient jamais à la légère.

## Pièges Fabric / Yarn 1.21.1

- `Identifier.of(ns, path)` ; `Item.Settings` ; `.maxCount()` ; `hasGlint()` ; `finishUsing()` ; `TypedActionResult` ; `World`/`ServerWorld`/`PlayerEntity`.
- `dirt_provider` obligatoire dans un `minecraft:tree` configured_feature.
- `-Dfile.encoding=COMPAT` dans `org.gradle.jvmargs` reste nécessaire sur Windows avec un chemin accentué.
- Les `assets/<ns>/items/*.json` n'existent qu'à partir de 1.21.4 : en 1.21.1 seuls `models/item/*.json` comptent.
- Tests : `MinecraftTestBootstrap.ensure()` dans `@BeforeAll` pour tout ce qui touche blocs, items ou registres ; inutile pour la logique pure.
- `DataTracker` n'a pas de handler `double` : une coordonnée précise se stocke en `LONG` (`Double.doubleToRawLongBits`), un `FLOAT` perd le centimètre loin de l'origine (le serveur GameTest tourne vers x = -1.4e7).
- `World.getTopY` renvoie `getBottomY()` si le chunk n'est pas chargé : charger le chunk avant de lire une hauteur de surface.
- `ChunkPos` n'a pas de `PACKET_CODEC` en 1.21.1 : `PacketCodecs.VAR_LONG.xmap(ChunkPos::new, ChunkPos::toLong)`.
- `Frustum` exige une projection dont la sous-matrice 3x3 est orthogonale : `coverBoxAroundSetPosition` recule le frustum le long de la troisième ligne et boucle sans fin avec un plan proche oblique (gel du client). Les vues de portail n'utilisent aucune projection oblique : projection du joueur partout, le plan du portail d'arrivée se coupe par `gl_ClipDistance` dans les shaders (`ShaderClipInjector`, `PortalClipping`).
- Le culling vanilla (`ChunkRenderingDataPreparer`) est asynchrone et tient un seul graphe par `WorldRenderer` : deux caméras sur un même renderer dans une image alternent des listes de sections incomplètes (flicker). Les vues de portail choisissent leurs sections elles-mêmes (`ViewSectionWalker`) et n'appellent `setupTerrain` qu'une fois par renderer et par image.
- `Entity.setPos` prévient le gestionnaire d'entities : déplacer temporairement un joueur vers une zone non chargée de son monde le sort de sa section et de son suivi. Couper l'écouteur (`setChangeListener(EntityChangeListener.NONE)`) le temps de l'appel, puis le rétablir après avoir remis la position.
- `ClientWorld.getEntities()` du monde principal est la seule liste que vanilla tique : un `ClientWorld` secondaire doit appeler lui-même `tickEntities()` ; ses sections ne tiquent qu'après `loadChunkFromPacket` (`resetChunkColor` → `startTicking`).
- Les récepteurs `ClientPlayNetworking` de Fabric s'exécutent déjà sur le thread de rendu, dans l'ordre des paquets. Ne pas les envelopper dans `client.execute(...)` : appelé depuis une tâche en cours, `ReentrantThreadExecutor` le remet en fin de file, derrière les paquets vanilla déjà reçus (un chunk du nouveau monde appliqué à l'ancien → `IndexOutOfBoundsException`, déconnexion).
- Tests en jeu : le serveur GameTest n'a ni Level 0 ni client ; le Nether sert de dimension distante, les joueurs sont connectés
  par `GameTestPlayers` (vrai `onPlayerConnect` sur canal en mémoire, `setOnGround(true)` sinon le minage est cinq fois plus lent),
  les payloads client s'injectent par `onCustomPayload`, et les tests qui posent quoi que ce soit utilisent l'arène
  `gtgs_backrooms_gametest:test_arena` (24 blocs) : la structure vide de Fabric fait un bloc et les voisins se marchent dessus.
  Le dossier de run `build/run/gameTest` n'est vidé que par `clean` (un build complet repart d'un monde neuf) et le serveur de test tique sans attendre : générer
  l'arrivée d'avance (`GameTestServerWorlds.pregenerate`) et guetter les chunks du Nether (`PortalViewTracker.watchedChunkCount() > 0`) plutôt que compter les ticks. Un bateau embarque de lui-même les créatures qui le touchent.
  Un joueur simulé ne suit une entité vanilla qu'une fois ses chunks reçus (`GameTestPlayers.tick`) et sa position
  redonnée (`getChunkManager().updatePosition`, ce que fait un vrai client en bougeant) ; l'apparition arrive groupée
  dans un `BundleS2CPacket`.
- Tests en jeu : les arènes ont un plafond de barrières que la heightmap voit (`getTopY` y renvoie le plafond), un chunk forcé
  ne suit ses entités qu'après quelques ticks (attendre `shouldTickEntity` avant d'y faire arriver une entité), et le dossier de
  run n'est vidé que par `clean` : une entité sauvée par un lancement précédent peut s'y trouver. Une entité qui change de
  monde garde son UUID : la retrouver par `getEntity(uuid)` plutôt que par une recherche dans une boîte. Les messages des
  échecs sont dans `build/junit.xml`, pas dans le journal. `TimedTaskRunner.createAndAdd` rejoue sa tâche à chaque tick
  tant qu'elle lève (une attente) ; une vérification définitive va dans `createAndAddReported` (jouée une fois, échec
  immédiat), nettoyage en `try/finally`, et ce qu'une attente expirée laisserait dans un `@AfterBatch`. Un échec de
  `createAndAddReported` n'arrête pas la suite : les tâches suivantes s'enchaînent (une attente réussie passe à la
  suivante dans le même tick) et le message gardé dans `build/junit.xml` est celui de la dernière vérification ratée,
  pas de la première.
- Mixins : `@Shadow` ne voit que les membres déclarés dans la classe cible, pas ceux hérités (`ClientPlayNetworkHandler.client`
  est déclaré dans `ClientCommonNetworkHandler`) ; l'erreur ne sort qu'au chargement de la cible, donc au lancement du client
  pour un mixin client. `MixinTargetsTest` charge la cible de chaque mixin déclaré et fait échouer le build à la place. Aucune
  classe de test dans le paquet `mixin` : Mixin refuse de charger les classes de son paquet.
- Rendu au milieu de `WorldRenderer.render` (les portails passent avant la couche translucide) : les couches de terrain et
  d'entités vanilla supposent l'élimination des faces arrière active, `RenderPhase.ENABLE_CULLING` ne l'active pas ; un
  `disableCull` laissé actif fait dessiner les deux faces de l'eau et du verre (opacité doublée). Ne couper le culling
  que le temps d'un dessin. Les tampons d'entités du monde principal tiennent encore ses couches translucides à ce
  moment-là : une vue dessine dans les siens (`VertexConsumerProvider.immediate`), avec la matrice de vue empilée par
  vanilla neutralisée, puis rend brouillard et éclairage d'entités du monde du joueur.
- Changement de dimension d'un joueur : le serveur retire chez lui les copies d'entités qu'il ne suit plus de la même
  façon et les renvoie aussitôt par l'autre voie ; un retrait immédiat côté client fait disparaître quelques ticks le
  portail de retour juste derrière le joueur (`EntityHandover.deferRemoval` diffère le retrait).
- Un joueur qui change de dimension perd aussitôt ses tickets de chunk : sans autre ticket, le côté quitté se décharge en
  quelques ticks et ses entités reviennent sous de nouveaux identifiants (`DestinationLoader.holdDeparture` le garde
  chargé). Côté client, `EntityIndex` refuse une copie dont l'UUID est déjà présent (`Duplicate entity UUID`) : elle
  reste affichée sans jamais être mise à jour (`EntityHandover.replaceStaleCopy` retire l'ancienne d'abord). Les bancs
  en `forceload` ne voient pas ce déchargement.
- Entités : avant son premier tick, une entité qui vient d'apparaître a `prevX/prevY/prevZ` à l'origine du monde (un
  trajet de là coupe n'importe quel plan : `TeleportExecutor.crossedThisTick` ignore les sauts de plus de 8 blocs).
  Côté client, une entité s'affiche environ un bloc derrière le serveur (interpolation) : téléportée par un portail,
  sa copie n'a pas encore atteint le plan, et la nouvelle, reprise à cette position, reste derrière le plan d'arrivée
  (`EntityHandover` garde l'ancienne jusqu'à l'arrivée de la nouvelle, dessinée ensuite des deux côtés par
  `StraddlingEntityRenderer`). Une décision de rendu se prend sur la position affichée (`lerp(tickDelta, lastRenderX,
  getX())`), jamais sur celle du tick, qui a jusqu'à un tick d'avance.
- Moteur de lumière (sources virtuelles, celles de la lumière transmise par les portails) : `ChunkBlockLightProvider.getLightSourceLuminance` est l'entrée des sources
  des mises à jour (`checkBlock`) ; l'éclairage initial d'un chunk lit `getLuminance()` sans elle, et la lumière sauvée
  avec un chunk revient telle quelle (re-signaler ce qui manque ou ce qui a pu s'éteindre). Le moteur serveur
  tourne sur son propre thread (lectures thread-safe obligatoires), et `checkBlock` sur les cellules changées suffit à
  propager ou éteindre une source virtuelle, serveur comme client.
- Météo : `World.getThunderGradient` rend l'orage déjà multiplié par la pluie, alors que le paquet vanilla
  (`THUNDER_GRADIENT_CHANGED`) porte la valeur brute (`WorldAccessor`). Vanilla ne tique que le renderer du joueur :
  celui d'une vue doit tiquer lui-même (`WorldRenderer.tick`), sinon sa pluie reste figée, et `client.worldRenderer`
  est déjà celui de la vue pendant qu'elle se dessine.
- Renderers : `WorldRenderer.close()` ne libère que ses post-effets ; les tampons de son `BufferBuilderStorage` (une
  trentaine de Mio hors du tas Java) ne sont jamais rendus. Un renderer de vue jeté passe par `RemoteWorld.release`, qui
  garde ses tampons pour le suivant.
- Spectateur : `ServerPlayerEntity.attack` d'un spectateur appelle `setCameraEntity`, qui le téléporte vers la cible,
  dans son monde ; toute action d'un spectateur à travers un portail est refusée. Vanilla ne suit un joueur spectateur
  que pour les spectateurs (`canBeSpectated`) : une vue de portail fait de même, et un spectateur ne compte pas pour une
  sortie fugace. La traversée d'un portail ne passe pas
  par `teleportTo` : l'événement Fabric `AFTER_PLAYER_CHANGE_WORLD` ne signale que les changements de monde vanilla
  (`AFTER_RESPAWN` une réapparition), et `ENTITY_UNLOAD` part juste avant que vanilla ne retire le suivi d'une entité.
- Envoi des chunks : tant qu'un chunk attend son tour dans le `ChunkDataSender` d'un joueur, vanilla ne lui envoie aucun
  changement de bloc (`isTracked` exclut `isInNextBatch`), comptant l'envoyer entier : un chunk qu'on lui épargne (tenu
  après une traversée, `MixinChunkDataSender`) et qui change doit partir quand même.
- `ClientPlayNetworkHandler.onBlockEntityUpdate` applique les données à `client.world`, pas au monde du gestionnaire :
  rebrancher le gestionnaire sur un monde distant ne suffit pas, le monde distant les applique lui-même.
- Tests en jeu : `GameTestPlayers.sent` consomme ce qu'il rend. Une attente puis une vérification qui lisent le même
  type doivent accumuler ce qu'elles lisent, sinon la vérification ne voit plus ce que l'attente a déjà lu (et un envoi
  fautif passe inaperçu).
- Tests en jeu : `PlayerManager.respawnPlayer` appelé directement ne met pas à jour `networkHandler.player` (vanilla le
  fait à la demande de respawn) ; sans cela, la déconnexion retire l'ancien joueur et le nouveau reste dans le monde,
  compté parmi les joueurs des tests suivants.
- L'extension Java de VSCode compile avec JDT dans `build/classes/java/main` (même dossier que Gradle) ; avec un modèle de projet périmé, elle y écrit des classes `Unresolved compilation problems` qui font planter le client au chargement. L'autobuild est coupé dans `.vscode/settings.json` ; ne jamais le réactiver sans un import Gradle sain (JDK 21).

## Avant de push

Suivre `docs/prompt/BEFOREPUSH.md` : `./gradlew clean build --warning-mode all` sans warning (il enchaîne Checkstyle,
tests unitaires, tests en jeu, verrou de couverture à 100 % des instructions et des branches, contrôle du contenu
du JAR), CHANGELOG et docs à jour. Toute instruction nouvelle hors colle OpenGL
client doit être couverte par un test unitaire ou un test en jeu, sinon le build échoue (`docs/TESTING.md`, section Couverture).
