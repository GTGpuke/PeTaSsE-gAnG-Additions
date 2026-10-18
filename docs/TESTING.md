# Tests — GTG's Backrooms

## Vue d'ensemble

| Niveau | Outil | Commande | Rapport |
|--------|-------|----------|---------|
| Unitaire | JUnit 5 sous le classloader Fabric (`fabric-loader-junit`) | `./gradlew test` | `build/reports/tests/test/index.html` |
| Couverture unitaire | JaCoCo | généré par `test` | `build/reports/jacoco/test/html/index.html` |
| En jeu | Fabric GameTest (serveur headless, sous agent JaCoCo) | `./gradlew runGameTest` | `build/junit.xml` + console |
| Couverture fusionnée et verrou | JaCoCo (unitaire + en jeu) : 100 % des instructions et des branches | `./gradlew jacocoCoverageVerification` (lancé par `build`) | `build/reports/jacoco/full/html/index.html` |
| Conventions du code | Checkstyle (`config/checkstyle/`) | `./gradlew checkstyleMain checkstyleTest checkstyleGametest` (lancés par `build`) | `build/reports/checkstyle/*.html` |
| Contenu du JAR | tâche `verifyJarContents` | `./gradlew verifyJarContents` (lancée par `build`) | console |
| Performance | Benchmark déterministe | `./gradlew benchmarkLevelZeroGeneration` | console |

Les échecs de tests font échouer le build, et la couverture aussi : voir Couverture.
`./gradlew build` enchaîne tout cela (`check`) : rien ne se construit sans ces vérifications.

---

## Couverture

`./gradlew jacocoCoverageVerification` fusionne l'exécution des tests unitaires (`build/jacoco/test.exec`) et celle des
tests en jeu (`build/jacoco/gametest.exec`, l'agent JaCoCo accompagne le serveur GameTest) et **échoue si une seule
instruction ou une seule branche du code mesuré n'a été prise par aucun des deux**. `./gradlew build` l'enchaîne
après `test` et `runGameTest`. Une branche qu'aucun cas réel ne prend (garde côté client d'un code qui ne tourne que
sur le serveur, condition déjà garantie par une autre) se retire ; une décision pure enfouie dans un code d'état
(échéance d'un journal, tirage pondéré) s'extrait en fonction testable, sans changer ce qu'elle décide.

Code mesuré : tout le mod sauf ce qui exige un contexte OpenGL ou un client, hors de portée de JUnit comme du serveur
GameTest, et les mixins (tissés dans les classes vanilla). La liste exacte est `coverageExcludes` dans `build.gradle` :

| Exclu | Pourquoi |
|-------|----------|
| `PortalStencilRenderer`, `FrameAreaRenderer`, `PortalClipping`, `portal/client/world/**` (`RemoteWorld`, `RemoteViewRenderer`, `ViewSectionWalker`, `RemoteParticles`, `RemoteWorldRegistry`, `RemoteSyncReceiver`, `ClientPortalIndex`, `ClientPortalLight`, `PortalSightReporter`, `StraddlingEntityRenderer`) | appels GL, `WorldRenderer`, `MinecraftClient` |
| `portal/client/transition/**`, `portal/client/interaction/**`, `portal/client/sound/FrameSoundPlayer`, `portal/client/entity/**` | bascule de monde client, visée et clics du client, son rejoué par le moteur de son du client, renderer d'entité |
| `level0/client/**`, `feature/gang/client/**`, `perf/client/**`, `GtgsBackroomsClientMod` | modèles, hooks client, point d'entrée client |
| `mixin/**` | mesurés à travers les classes vanilla qu'ils modifient |

La logique client pure reste mesurée et testée : `PortalViewTransform` (plan de découpe, caméra transposée), `ViewCone`
(volume visible à travers le cadre), `ShaderClipInjector` (contre 54 des 55 vertex shaders vanilla du jar du jeu,
`blit_screen` exclu), `PortalRenderState`, `ThirdPersonPortal` (recul de la caméra à travers le cadre), `PortalSound`
(portée et volume du son à travers le cadre). Le rendu exclu se valide en jeu (`docs/PORTALS.md`, section Tests).

Les classes utilitaires (constructeur privé qui refuse l'appel) sont couvertes par `UtilityClassesTest`, qui les
énumère ; une nouvelle classe utilitaire s'y ajoute. Les tests en jeu tournent avec un cooldown de traversée de trois
ticks (`-Dgtgsbackrooms.portal.teleportCooldownTicks=3` dans la configuration `gameTest` de `build.gradle`) pour exercer
le refus sous cooldown, nul par défaut en jeu.

---

## Tests unitaires

### Deux familles

1. **Logique pure** (coordonnées, hash, secteurs, layout, biomes, détails muraux, portail) : aucune
   initialisation Minecraft, s'exécutent en quelques secondes.
2. **Avec registres réels** (blocs, items, palette, writer, générateur, ressources) : appeler
   `MinecraftTestBootstrap.ensure()` dans un `@BeforeAll`. Le helper démarre Minecraft une seule fois
   par JVM puis exécute le vrai `onInitialize()` du mod. Grâce à `fabric-loader-junit`, les registres ne
   sont pas gelés et les mixins de Fabric API s'appliquent.

```java
@BeforeAll
static void bootstrap() {
    MinecraftTestBootstrap.ensure();
}
```

### Ce qui est couvert

| Zone | Classes de test | Ce qui est vérifié |
|------|-----------------|--------------------|
| Coordonnées et pile verticale | `LevelZeroCoordsTest`, `LevelZeroLayerStackLayoutTest` | conversions, offsets de layer, bornes |
| Seeds | `StageRandomTest`, `LevelZeroSeedResolverTest` | formule figée, sels distincts, seeds de layer |
| Secteurs | `LevelZeroSectorGeneratorTest`, `LevelZeroSectorCacheTest` | déterminisme, salles, LRU |
| Layout | `LevelZeroLayoutTest`, `LevelZeroCellEvaluatorTest`, `LevelZeroTopologyTest`, `LevelZeroLargeRoomsTest`, `LevelZeroLightPatternTest`, `LevelZeroCorridorGapsTest`, `LevelZeroRegionWalkabilityTest`, `LevelZeroCellMicroPatternTest`, `LevelZeroCellConnectionsTest`, `LevelZeroTieBreakTest` | invariants : origine ouverte, cellules 3x3, **frontières de chunk identiques**, néons jamais adjacents, gaps jamais consécutifs |
| Biomes | `LevelZeroSurfaceBiomeTest` | régions stables, restriction par layer, densité lumineuse |
| Écriture | `LevelZeroBlockPaletteTest`, `LevelZeroBlockWriterTest`, `LevelZeroWallSamplerTest`, `LevelZeroWallPropsTest`, `LevelZeroDebugPaletteTest` | colonnes ouvertes/murs/néons, court-circuit du sampler = sonde complète, modes debug et leur palette |
| Blocs du Level 0 | `LevelZeroWallpaperBlockTest` | mur adaptatif : seize variantes de faces, papier jauni ou blanc selon le layer et la région |
| Générateur | `LevelZeroChunkGeneratorTest` | `populateNoise` sur un vrai `ProtoChunk`, `getColumnSample` identique au chunk, déterminisme |
| Non-régression | `LevelZeroGoldenTest` | empreintes de génération par seed et layer |
| Registres et ressources | `ModRegistriesTest`, `ModDatapackLoadingTest` | identifiants, items de blocs, onglet complet, blockstates/modèles/loot tables présents, EN = FR, dimension Level 0 chargée depuis le datapack du mod |
| Items | `GangBadgeItemTest`, `CursedSnackItemTest` | propriétés, composants, tooltips |
| Config et perf | `ModConfigTest`, `ModPerformanceMonitorTest` | parsing, valeurs par défaut, mesure active |
| Utilitaires et point d'entrée | `UtilityClassesTest`, `GtgsBackroomsModTest`, `ModCreativeTabTest` | classes finales non instanciables, portails activés ou non, branchements idempotents, onglets remplis |
| Mixins | `MixinTargetsTest` | chaque mixin déclaré (communs et client) s'applique à sa cible : les tests unitaires tournent côté client sous le chargeur Fabric, et charger la cible applique le mixin ; un mixin client cassé fait échouer le build au lieu de planter le client au lancement |
| Client (logique pure) | `PortalViewTransformTest`, `ViewConeTest`, `ShaderClipInjectorTest`, `PortalRenderStateTest`, `RemoteViewRendererTest`, `ThirdPersonPortalTest` | plan de découpe en espace caméra, caméra et regard transposés, volume visible à travers le cadre (et sans limite), recul de la caméra à la troisième personne à travers le cadre ou au-dessus du linteau, injection du plan dans 54 des 55 vertex shaders vanilla (`blit_screen` exclu), garde de rendu et numéro d'image, coupure du rendu distant, ancrage de la grille de sections |
| Portails | `PortalFacingTest`, `BackroomsPortalEntityTest`, `BackroomsPortalBlockTest`, `PortalSettingsTest`, `PortalTargetsTest`, `VirtualLightSourcesTest`, `PortalRelevanceTest`, `TeleportExecutorTest`, `PortalCollisionTest`, `PortalPayloadsTest`, `PortalViewTrackerTest`, `RemoteChunkWindowTest`, `RemoteEntityViewTest`, `PortalSoundTest`, `PortalPursuitTest` | rotation sans miroir sur les 36 paires, faces arrière qui transposent comme les faces avant, NBT, bloc invisible visé seulement accroupi, réglages (format, bornes, piles, colonnes, centre et pied du cadre), cadre d'arrivée qui tient sur toute sa largeur avec ses deux faces libres, sources de lumière immuables, traversée du plan (le centre en face de l'ouverture, la boîte touchant le cadre ; ni apparition ni saut de position), face franchie d'après le sens du déplacement, boîtes à cheval sur le cadre d'entrée ou d'arrivée à la position affichée, boîtes refaites quand la taille change, côté de sortie et image d'une boîte d'un portail à l'autre, entité engagée dans un cadre, onze payloads dont un paquet d'entité vanilla enveloppé et les portails vus, paquets relayés aux joueurs qui regardent une dimension, fenêtre de chunks et sa boîte d'entités ; volume du son après le trajet et sortie par le cadre où le joueur voit l'autre côté, point de passage du regard dans le cadre, point d'approche et alignement sur l'ouverture, cooldown des joueurs |

### Le test d'empreintes (`LevelZeroGoldenTest`)

Il hache, pour trois seeds et cinq layers, toute la génération logique d'une zone de 7x7 chunks
(évaluation des cellules, exposition, masques de papier peint, détails muraux). S'il échoue :

- régression → corriger ;
- évolution voulue de la génération → relancer, copier les nouvelles valeurs dans le test, et
  **l'annoncer dans le CHANGELOG** (les mondes existants changent).

### Lancer un sous-ensemble

```bash
./gradlew test --tests "*LevelZeroLayoutTest*"
./gradlew test --tests "com.gtgpuke.gtgsbackrooms.init.*"
```

---

## Tests en jeu (GameTest)

Source set dédié `src/gametest` (Loom `fabricApi.configureTests`) : un mod séparé, `gtgs_backrooms_gametest`,
avec son `fabric.mod.json` et son entrypoint `fabric-gametest`, chargé par `runGameTest` seulement. Rien de tout cela
n'entre dans le JAR du mod, ce que `verifyJarContents` vérifie à chaque `build`. Le serveur de test vanilla force le
preset plat avec l'overworld, le Nether et l'End, sans les dimensions de datapack : le Level 0 n'y existe pas, et le
Nether tient lieu de dimension distante pour les portails. Le dossier de run (`build/run/gameTest/`) n'est vidé que par
`clean` : sur un dossier neuf (build complet), le Nether se génère, et le serveur de test tique sans attendre ; les tests qui ont besoin de
l'arrivée la génèrent d'avance (`GameTestServerWorlds.pregenerateArrival`, 11x11 chunks autour d'elle ; `pregenerate` pour un rayon
donné) puis guettent ses chunks au lieu de compter les ticks. Une tâche `createAndAdd` est rejouée à chaque tick tant qu'elle
lève, jusqu'au délai du test : elle sert à attendre une condition. Une vérification définitive va dans `createAndAddReported`,
jouée une fois, qui fait échouer le test aussitôt ; son nettoyage se fait en `try/finally`, et ce qu'une attente expirée
laisserait derrière elle, dans un `@AfterBatch` du lot.

L'arborescence suit celle du code : `support/` (joueurs simulés, monde prêté), `backrooms/level0/`, `backrooms/portal/`, `feature/`.

| Classe | Ce qui est vérifié |
|--------|--------------------|
| `backrooms/level0/LevelZeroGameTests` | JSON de la dimension décodé et réencodé avec les registres du serveur, chunk d'origine généré : sol, air, plafond, bedrock sur les cinq layers ; mur adaptatif posé (masque recalculé ou déjà juste) |
| `backrooms/portal/BackroomsPortalBlockGameTests` | copie d'un bloc (clone, structure) : destination gardée, portails propres, ceux de l'original intacts ; réglages seuls envoyés aux clients ; une face : rien de dos, on passe au travers sans traverser ; aller simple : rien à l'arrivée, retours retirés à la relecture (dimension absente comprise) ; sortie fugace : jamais vue, elle attend un regard, même avec un joueur tout près ; vue, elle tient sous le regard d'un joueur, même de loin, et disparaît après une demi-seconde sans regard, même tout près (un rapport vide isolé ne la perd pas ; sauvée, jamais reposée par les vérifications des retours, neuve pour une copie du bloc ; un retour qui devient fugace à la relecture est reposé, l'entrée gardée) ; rouverte aussitôt par le prochain joueur qui passe l'entrée, à une face comme le bloc, puis de nouveau une seconde pour se retourner et le regard seul ; ni un objet, ni un spectateur, ni un portail qui n'est pas l'une des entrées du bloc ne la rouvrent, et une sortie encore là n'est pas reposée, un rapport fait dans un autre monde ignoré, un spectateur qui la regarde ou un joueur sans rapport ne la gardant pas, un rapport de plus de 40 ticks ne comptant plus ; seul le joueur qui la voit la traverse (un portail libre marqué fugace, jamais), et qui sort par elle a une seconde pour se retourner, pour elle seule et pas en repassant par elle, oubliée (avec son dernier rapport) au changement de monde vanilla et au respawn ; sans rien casser vers une dimension absente ; portail que son bloc ne tient plus pour sien retiré par le registre ; quatre portails (deux faces dos à dos de chaque côté), retours inverses exacts, même transformation par les deux faces, taille et lumière réglées ; bloc tourné ou relu avec une autre largeur, lumière, destination, dimension ou hauteur → portails reposés ; cassé → plus rien (retours compris), y compris sans block entity ; traversée par la face arrière ; pose face au joueur avec les réglages de l'item, cadre bloqué refusé, visé seulement accroupi ; cibles par dimension, sans destination ou vers une dimension absente, face perdue attendue puis reposée, retour disparu ou jamais posé, Level 0 prêté au serveur (retour suivi puis retiré avec le bloc), bloc inerte quand les portails sont désactivés ; arrivée posée sur le haut réel du sol (dalle, tapis, eau) |
| `backrooms/portal/PortalLightGameTests` | lumière des blocs passée d'un niveau de moins par un portail ouvert, visible et réglé pour elle, la plus forte de deux l'emportant, rien par un portail fermé, invisible, réglé sans lumière, sans arrivée ou vers le noir ; source publiée mais pas encore appliquée signalée de nouveau ; cadre d'un portail qui apparaît revérifié (lumière fantôme éteinte) ; portail double face 4 x 4 éclairé des deux côtés puis éteint avec sa lumière (chaque test dans son lot, loin du coin éclairé de l'arène) |
| `backrooms/portal/PortalGameTests` | traversée d'un joueur dans le même monde (position, regard) ; objet lancé ; saut de moins de huit blocs envoyé en position absolue, marqueur sans suivi ; collision à travers le cadre (un objet poussé de biais bute sur le mur de l'autre côté, au milieu il passe ; rien derrière le plan, portail fermé, dimension absente, deux cadres engagés, joueur serveur ou portail d'un autre monde ignorés) ; payload de chunk |
| `backrooms/portal/PortalSyncGameTests` | fenêtre de chunks envoyée à un joueur qui regarde un portail vers le Nether (foyer le plus proche, fenêtres partagées et fusionnées), entité distante retirée de son monde effacée chez lui au même tick, chunks tenus après une traversée valables 100 ticks, annonce de traversée refusée sous cooldown ou à la vitesse impossible, météo du Nether envoyée avec elle puis à chaque changement seulement, entités suivies, entité arrivée par un portail envoyée au même tick, blocs relayés, déchargement après demi-tour ; traversée vers le Nether sans renvoi des chunks tenus qui n'ont pas changé (empreinte de leurs blocs relevée avant le passage : un Nether neuf bouge encore) ; côté quitté gardé chargé après le départ du joueur, ses entités restant les mêmes (loin des chunks forcés de l'arène) ; annonce client vraisemblable, invraisemblable, sous cooldown, refusée (portail fermé, sans dimension, hors du cadre, en bateau) ; marche détectée par le serveur et remontée hors d'un sol irrégulier, franchissement abandonné si le portail disparaît, se ferme ou ne mène nulle part, joueur en bateau ou qui saute de position ; entités qui ne traversent pas (cooldown, bateau et passager, mort) ; changements de monde vanilla ; portails orphelins et chargement double ; commande de debug ; ticket élargi à l'approche ; portail invisible traversé sans vue ; chunk tenu qui change autour d'une traversée renvoyé par vanilla ; chunks tenus propres à chaque joueur et à leur dimension (un changement aux mêmes coordonnées d'une autre dimension n'en libère aucun) ; joueur spectateur montré par le portail aux seuls spectateurs ; texte d'un panneau écrit de l'autre côté et fissures de l'autre côté relayés ; vue d'un joueur qui vient de passer repartie au tick suivant |
| `backrooms/portal/PortalInteractionGameTests` | coups, tonte, clics, porte vanilla ouverte et fermée, cochon sellé jamais enfourché, spectateur refusé, fissures effacées quand le minage cesse ou que le mineur se déconnecte, mineur mort qui ne casse plus rien, rien de visé près du plan mais loin à côté du cadre, pose d'objets (main principale ou secondaire, bâton sans effet, tête sur un bloc musical) et minage à travers le cadre, en créatif et en survie, avec les refus (hors portée, derrière le portail, portail inconnu ou invisible, hors de la bordure du monde, épée en créatif, bloc de commande sauf opérateur, événement de casse, mode aventure, bloc disparu en cours de minage, portail à portée mais bloc hors portée, netherrack cassée sans récolte) |
| `backrooms/portal/PortalPropagationGameTests` | sons, évènements de monde et particules relayés au seul joueur qui regarde l'arrivée ; un husk voit un joueur du Nether par le cadre, prend la posture d'attaque en chemin, traverse et le cible ; sa copie neuve repart aussitôt vers le joueur, posture comprise ; en l'air, sans chemin, elle le cible sans la posture ; la poursuite finie, il la quitte, et garde une cible de son côté ; un portail fermé, hors de portée de poursuite, vers une dimension absente, ou une vue masquée d'un côté ou de l'autre ne montre rien ; une cible dans un autre monde devient une poursuite, l'objectif de poursuite fait marcher le mob, d'abord en face de l'ouverture, puis aussitôt à travers, la cible le suit à la traversée ; qui reçoit l'objectif (husk, slime, golem de fer, pas le chat) ; chaque règle d'une poursuite (portail ouvert, visible, devant le mob, vers un autre monde, à portée ; cible vivante, de l'autre côté ; délai) écarte à elle seule |
| `feature/ItemGameTests` | badge et collation utilisés par un joueur, par un joueur non serveur et par une créature |

Les joueurs simulés viennent de `GameTestPlayers` : connexion réelle au serveur sur un canal en mémoire (liste des
joueurs, suivi des chunks, événements de connexion), mode de jeu au choix, `setOnGround(true)` faute de client pour le
dire, `tick(player)` fait tourner son gestionnaire réseau en acquittant les lots de chunks (le serveur ne tique pas
lui-même ces connexions), et `receive(player, payload)` fait entrer un payload comme venu de son client. Les tests qui posent des blocs, des
entités ou des joueurs utilisent l'arène `gtgs_backrooms_gametest:test_arena` (24 x 8 x 24,
`src/gametest/resources/data/gtgs_backrooms_gametest/structure/test_arena.nbt`) :
la structure vide de Fabric fait un bloc et les tests voisins se marcheraient dessus. Dans l'arène, le sol est en
y = -1 relatif ; y = 0 est la première couche d'air.

```bash
./gradlew runGameTest
```

La tâche échoue si un test échoue. Dossier de run : `build/run/gameTest/` (logs dans `logs/latest.log`), rapport
par test dans `build/junit.xml`.

---

## Conventions du code (Checkstyle)

`./gradlew checkstyleMain checkstyleTest checkstyleGametest` (dans `check`, donc dans `build`) applique
`config/checkstyle/checkstyle.xml` aux trois source sets : sources ASCII, imports ordonnés par groupes
(`java` → tiers → `net.minecraft` → `net.fabricmc` → `com.gtgpuke`) sans wildcard ni doublon, journal via
`ModConstants.LOGGER` uniquement, Javadoc sur chaque type public, classes utilitaires finales au constructeur privé,
mise en forme (accolades, espaces, une instruction par ligne, 180 colonnes). Zéro avertissement toléré (`maxWarnings = 0`).
Les deux exceptions assumées sont dans `config/checkstyle/suppressions.xml` : le logger est créé dans `ModConstants`,
le benchmark écrit ses chiffres sur la console. Rapports : `build/reports/checkstyle/{main,test,gametest}.html`.

---

## Contenu du JAR

`./gradlew verifyJarContents` (dans `check`) ouvre le JAR remappé et échoue s'il contient autre chose que le mod :
`fabric.mod.json`, `pack.mcmeta`, le fichier de mixins, le manifeste, la licence (`LICENSE_gtgs_backrooms`),
`assets/` et `data/` du mod, les tags `data/minecraft/tags/` et les classes `com/gtgpuke/gtgsbackrooms/` hors
`gametest/`. Une dépendance embarquée (classe tierce ou clé `jars` du `fabric.mod.json`) ou un test qui se glisserait
dans le JAR fait échouer le build.

---

## Benchmark

```bash
./gradlew benchmarkLevelZeroGeneration
./gradlew benchmarkLevelZeroGeneration -PlevelZeroPerfBudgetMsPerChunk=2.0
```

Mesure, sur 3 seeds et 169 chunks par passe : le layout à secteurs froids (génération incluse), le
layout à secteurs chauds, et le coût complet layout + écriture des 256 colonnes x 5 layers. Le budget
optionnel porte sur ce dernier chiffre. Les chiffres de référence sont dans `docs/AUDIT.md`.

---

## Écrire un nouveau test

- Un comportement par méthode, un message d'assertion en français qui dit ce qui devrait être vrai.
- Pas de test par réflexion « le constructeur existe » pour une classe normale : tester ce que le code fait. Les
  classes utilitaires, elles, s'ajoutent à `UtilityClassesTest`.
- Chaque instruction et chaque branche nouvelles hors colle client doivent être prises par un test, sinon `build`
  échoue : chercher la ligne rouge ou jaune dans `build/reports/jacoco/full/html/index.html`
  et écrire le test qui la justifie, plutôt que d'exclure.
- Les tests de génération doivent rester déterministes : seeds fixes, zones fixes.
- Si une classe touche aux blocs ou aux registres : `MinecraftTestBootstrap.ensure()`.
