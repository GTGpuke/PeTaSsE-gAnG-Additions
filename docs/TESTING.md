# Tests — PeTaSsE_gAnG_Additions

## Vue d'ensemble

| Niveau | Outil | Commande | Rapport |
|--------|-------|----------|---------|
| Unitaire | JUnit 5 sous le classloader Fabric (`fabric-loader-junit`) | `./gradlew test` | `build/reports/tests/test/index.html` |
| Couverture unitaire | JaCoCo | généré par `test` | `build/reports/jacoco/test/html/index.html` |
| En jeu | Fabric GameTest (serveur headless, sous agent JaCoCo) | `./gradlew runGameTest` | `build/junit.xml` + console |
| Couverture fusionnée et verrou | JaCoCo (unitaire + en jeu) : 100 % des instructions, 96 % des branches | `./gradlew jacocoCoverageVerification` (lancé par `build`) | `build/reports/jacoco/full/html/index.html` |
| Conventions du code | Checkstyle (`config/checkstyle/`) | `./gradlew checkstyleMain checkstyleTest checkstyleGametest` (lancés par `build`) | `build/reports/checkstyle/*.html` |
| Contenu du JAR | tâche `verifyJarContents` | `./gradlew verifyJarContents` (lancée par `build`) | console |
| Performance | Benchmark déterministe | `./gradlew benchmarkLevelZeroGeneration` | console |

Les échecs de tests font échouer le build (`ignoreFailures` n'existe plus), et la couverture aussi : voir Couverture.
`./gradlew build` enchaîne tout cela (`check`) : rien ne se construit sans ces vérifications.

---

## Couverture

`./gradlew jacocoCoverageVerification` fusionne l'exécution des tests unitaires (`build/jacoco/test.exec`) et celle des
tests en jeu (`build/jacoco/gametest.exec`, l'agent JaCoCo accompagne le serveur GameTest) et **échoue si une seule
instruction du code mesuré n'a été exécutée par aucun des deux, ou si moins de 96 % de ses branches l'ont été**.
`./gradlew build` l'enchaîne après `test` et `runGameTest`. Les branches restantes sont des gardes que ni JUnit ni le
serveur de test ne peuvent prendre (`world.isClient()` côté serveur, environnement hors développement, `instanceof`
défensifs) : on ne les couvre pas par des tests artificiels, et on ne retire pas une garde utile pour un pourcentage.

Code mesuré : tout le mod sauf ce qui exige un contexte OpenGL ou un client, hors de portée de JUnit comme du serveur
GameTest, et les mixins (tissés dans les classes vanilla). La liste exacte est `coverageExcludes` dans `build.gradle` :

| Exclu | Pourquoi |
|-------|----------|
| `PortalStencilRenderer`, `ViewAreaRenderer`, `portal/client/world/**` (`RemoteWorld`, `RemoteViewRenderer`, `RemoteWorldRegistry`, `RemoteChunkReceiver`, `ClientPortalIndex`) | appels GL, `WorldRenderer`, `MinecraftClient` |
| `portal/client/transition/**`, `portal/client/interaction/**`, `portal/client/entity/**` | bascule de monde client, visée et clics du client, renderer d'entité |
| `level0/client/**`, `feature/gang/client/**`, `perf/client/**`, `PeTaSsEgAnGAdditionsClientMod` | modèles, hooks client, point d'entrée client |
| `mixin/**` | mesurés à travers les classes vanilla qu'ils modifient |

La logique client pure reste mesurée et testée : `PortalViewTransform` (projection oblique, caméra transposée),
`PortalRenderState`. Le rendu exclu se valide en jeu (`docs/PORTALS.md`, section Tests).

Les classes utilitaires (constructeur privé qui refuse l'appel) sont couvertes par `UtilityClassesTest`, qui les
énumère ; une nouvelle classe utilitaire s'y ajoute. Les tests en jeu tournent avec un cooldown de traversée de trois
ticks (`-Dpetassegang.portal.teleportCooldownTicks=3` dans la configuration `gameTest` de `build.gradle`) pour exercer
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
| Layout | `LevelZeroLayoutTest`, `LevelZeroCellEvaluatorTest`, `LevelZeroTopologyTest`, `LevelZeroLargeRoomsTest`, `LevelZeroLightPatternTest`, `LevelZeroCorridorGapsTest`, `LevelZeroRegionWalkabilityTest`, `LevelZeroCellMicroPatternTest` | invariants : origine ouverte, cellules 3x3, **frontières de chunk identiques**, néons jamais adjacents, gaps jamais consécutifs |
| Biomes | `LevelZeroSurfaceBiomeTest` | régions stables, restriction par layer, densité lumineuse |
| Écriture | `LevelZeroBlockPaletteTest`, `LevelZeroBlockWriterTest`, `LevelZeroWallSamplerTest`, `LevelZeroWallPropsTest` | colonnes ouvertes/murs/néons, court-circuit du sampler = sonde complète, modes debug |
| Générateur | `LevelZeroChunkGeneratorTest` | `populateNoise` sur un vrai `ProtoChunk`, `getColumnSample` identique au chunk, déterminisme |
| Non-régression | `LevelZeroGoldenTest` | empreintes de génération par seed et layer |
| Registres et ressources | `ModRegistriesTest` | identifiants, items de blocs, onglet complet, blockstates/modèles/loot tables présents, EN = FR |
| Items | `GangBadgeItemTest`, `CursedSnackItemTest` | propriétés, composants, tooltips |
| Config et perf | `ModConfigTest`, `ModPerformanceMonitorTest` | parsing, valeurs par défaut, mesure active |
| Utilitaires et point d'entrée | `UtilityClassesTest`, `PeTaSsEgAnGAdditionsModTest`, `ModCreativeTabTest` | classes finales non instanciables, portails activés ou non, branchements idempotents, onglets remplis |
| Client (logique pure) | `PortalViewTransformTest`, `PortalRenderStateTest` | projection oblique, caméra et regard transposés, garde de rendu, coupure du rendu distant |
| Portails | `PortalFacingTest`, `BackroomsPortalEntityTest`, `BackroomsDoorBlockEntityTest`, `BackroomsDoorTargetsTest`, `PortalRelevanceTest`, `TeleportExecutorTest`, `TpCooldownTrackerTest`, `PortalPayloadsTest`, `RemoteChunkWindowTest`, `PortalViewTransformTest` | rotation sans miroir sur les 36 paires, NBT, cellule d'arrivée réellement libre, traversée du plan (avec et sans allocation), dix payloads dont un paquet d'entité vanilla enveloppé, fenêtre de chunks, plan proche oblique |

### Le test d'empreintes (`LevelZeroGoldenTest`)

Il hache, pour trois seeds et cinq layers, toute la génération logique d'une zone de 7x7 chunks
(évaluation des cellules, exposition, masques de papier peint, détails muraux). S'il échoue :

- régression → corriger ;
- évolution voulue de la génération → relancer, copier les nouvelles valeurs dans le test, et
  **l'annoncer dans le CHANGELOG** (les mondes existants changent).

### Lancer un sous-ensemble

```bash
./gradlew test --tests "*LevelZeroLayoutTest*"
./gradlew test --tests "com.petassegang.addons.init.*"
```

---

## Tests en jeu (GameTest)

Source set dédié `src/gametest` (Loom `fabricApi.configureTests`) : un mod séparé, `petasse_gang_additions_gametest`,
avec son `fabric.mod.json` et son entrypoint `fabric-gametest`, chargé par `runGameTest` seulement. Rien de tout cela
n'entre dans le JAR du mod, ce que `verifyJarContents` vérifie à chaque `build`. Le serveur de test vanilla force le
preset plat avec l'overworld, le Nether et l'End, sans les dimensions de datapack : le Level 0 n'y existe pas, et le
Nether tient lieu de dimension distante pour les portails. Le dossier de run (`build/run/gameTest/`) est recréé à chaque
lancement : le Nether se génère à chaque fois, et le serveur de test tique sans attendre ; les tests qui ont besoin de
l'arrivée la génèrent d'avance (`pregenerateArrival`, 9x9 chunks) puis guettent ses chunks au lieu de compter les ticks.

L'arborescence suit celle du code : `support/` (joueurs simulés, monde prêté), `backrooms/level0/`, `backrooms/portal/`, `feature/`.

| Classe | Ce qui est vérifié |
|--------|--------------------|
| `backrooms/level0/LevelZeroGameTests` | JSON de la dimension décodé et réencodé avec les registres du serveur, chunk d'origine généré : sol, air, plafond, bedrock sur les cinq layers ; mur adaptatif posé (masque recalculé ou déjà juste) |
| `backrooms/portal/BackroomsDoorGameTests` | porte → deux portails liés et propriétaires → cassée → plus rien ; porte posée face au joueur, cibles par dimension, porte sans destination ou vers une dimension absente, sol irrégulier devant la porte, portail d'entrée perdu, porte tournée, portail de retour disparu ou jamais posé, Level 0 prêté au serveur |
| `backrooms/portal/PortalGameTests` | traversée d'un joueur dans le même monde (position, regard, cooldown) ; objet lancé ; payload de chunk |
| `backrooms/portal/PortalSyncGameTests` | fenêtre de chunks envoyée à un joueur qui regarde un portail vers le Nether (foyer le plus proche, fenêtres partagées et fusionnées), entités suivies, blocs relayés, déchargement après demi-tour ; traversée vers le Nether sans renvoi des chunks tenus ; annonce client vraisemblable, invraisemblable, sous cooldown, refusée (portail fermé, sans dimension, hors du cadre, en bateau) ; marche détectée par le serveur et remontée hors d'un sol irrégulier, franchissement abandonné si le portail disparaît, se ferme ou ne mène nulle part, joueur en bateau ou qui saute de position ; entités qui ne traversent pas (cooldown, bateau et passager, mort) ; changements de monde vanilla ; portails orphelins et chargement double ; commande de debug ; ticket élargi à l'approche ; portail invisible traversé sans vue |
| `backrooms/portal/PortalInteractionGameTests` | coups, tonte, clics, pose d'objets (main principale ou secondaire, bâton sans effet, tête sur un bloc musical) et minage à travers le cadre, en créatif et en survie, avec les refus (hors portée, derrière le portail, portail inconnu ou invisible, hors de la bordure du monde, épée en créatif, bloc de commande sauf opérateur, événement de casse, mode aventure, bloc disparu en cours de minage, portail à portée mais bloc hors portée, netherrack cassée sans récolte) |
| `feature/ItemGameTests` | badge et collation utilisés par un joueur, par un joueur non serveur et par une créature |

Les joueurs simulés viennent de `GameTestPlayers` : connexion réelle au serveur sur un canal en mémoire (liste des
joueurs, suivi des chunks, événements de connexion), mode de jeu au choix, `setOnGround(true)` faute de client pour le
dire, `tick(player)` fait tourner son gestionnaire réseau en acquittant les lots de chunks (le serveur ne tique pas
lui-même ces connexions), et `receive(player, payload)` fait entrer un payload comme venu de son client. Les tests qui posent des blocs, des
entités ou des joueurs utilisent l'arène `petasse_gang_additions_gametest:test_arena` (24 x 8 x 24,
`src/gametest/resources/data/petasse_gang_additions_gametest/structure/test_arena.nbt`) :
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
(`java` → tiers → `net.minecraft` → `net.fabricmc` → `com.petassegang`) sans wildcard ni doublon, journal via
`ModConstants.LOGGER` uniquement, Javadoc sur chaque type public, classes utilitaires finales au constructeur privé,
mise en forme (accolades, espaces, une instruction par ligne, 180 colonnes). Zéro avertissement toléré (`maxWarnings = 0`).
Les deux exceptions assumées sont dans `config/checkstyle/suppressions.xml` : le logger est créé dans `ModConstants`,
le benchmark écrit ses chiffres sur la console. Rapports : `build/reports/checkstyle/{main,test,gametest}.html`.

---

## Contenu du JAR

`./gradlew verifyJarContents` (dans `check`) ouvre le JAR remappé et échoue s'il contient autre chose que le mod :
`fabric.mod.json`, `pack.mcmeta`, le fichier de mixins, le manifeste, `assets/` et `data/` du mod, les tags
`data/minecraft/tags/` et les classes `com/petassegang/addons/` hors `gametest/`. Une dépendance embarquée (classe
tierce ou clé `jars` du `fabric.mod.json`) ou un test qui se glisserait dans le JAR fait échouer le build.

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
- Chaque instruction nouvelle hors colle client doit être exécutée par un test, et les branches rester couvertes à
  96 % au moins, sinon `build` échoue : chercher la ligne rouge ou jaune dans `build/reports/jacoco/full/html/index.html`
  et écrire le test qui la justifie, plutôt que d'exclure.
- Les tests de génération doivent rester déterministes : seeds fixes, zones fixes.
- Si une classe touche aux blocs ou aux registres : `MinecraftTestBootstrap.ensure()`.
