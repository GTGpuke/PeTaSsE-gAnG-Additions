# CLAUDE.md — PeTaSsE_gAnG_Additions

> Lu automatiquement à chaque session. Règles obligatoires du projet.
> Onboarding complet : `docs/prompt/NOKNOWLEDGE.md` — Audit pré-push : `docs/prompt/BEFOREPUSH.md`
> Dernier audit complet : `docs/AUDIT.md`

## Stack

Minecraft **1.21.1** / Yarn `1.21.1+build.3` / Fabric Loader **0.19.5** / Fabric API **0.116.17** / Fabric Loom **1.17.21** / Gradle **9.8.1** (wrapper) / Java **21** (toolchain et JVM Gradle)
`mod_id` : `petasse_gang_additions` — package racine : `com.petassegang.addons`

## Commandes

```bash
./gradlew test                          # JUnit 5 sous le classloader Fabric + rapport JaCoCo (build/reports/jacoco)
./gradlew runGameTest                   # Tests en jeu (serveur headless, sous agent JaCoCo) : Level 0, portails, contenu
./gradlew jacocoCoverageVerification    # Couverture fusionnee unitaire + en jeu : echoue sous 100 % des instructions ou 96 % des branches (lance par build)
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
- `backrooms/portal/` : portails (porte, entity, téléportation de tout ce qui traverse, synchronisation des chunks et des entités, rendu stencil avec un niveau d'imbrication, interaction à travers le cadre), actifs par défaut (`ModConfig.ENABLE_PORTAL_FEATURE`). Lire `docs/PORTALS.md` avant d'y toucher.
- `perf/` : moniteur de performance opt-in.
- `mixin/` : commun (`MixinServerChunkManager` relais des changements de blocs, accesseurs du suivi vanilla des entities) et `client/` (rendu des portails, visée et clics à travers le cadre).
- `src/gametest/` : tests en jeu, mod séparé `petasse_gang_additions_gametest` chargé par `runGameTest` seulement (jamais dans le JAR, `verifyJarContents` le vérifie).

## Règles de code

- Commentaires, logs, messages d'erreur : **en français**, majuscule initiale, point final. Pas d'accents dans le code (sources ASCII).
- Contenu visible en jeu : `Text.translatable()` + clés dans `en_us.json` **et** `fr_fr.json` (test `ModRegistriesTest` le vérifie).
- Imports : `java` → bibliothèques tierces (`com.mojang`, `org.*`) → `net.minecraft` → `net.fabricmc` → `com.petassegang`, aucun wildcard.
- Logs via `ModConstants.LOGGER` uniquement ; aucun `System.out` hors benchmark.
- Aucune allocation dans les méthodes tick/render ; pas de concaténation de chaînes dans les chemins chauds (y compris les noms de sections perf).
- Pas de couche d'abstraction sans deuxième implémentation réelle : pas d'interface/stage/builder spéculatif.
- Pas de dossier vide ni de `.gitkeep`, pas de `package-info.java` d'une ligne.

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
- `Frustum` exige une projection dont la sous-matrice 3x3 est orthogonale : `coverBoxAroundSetPosition` recule le frustum le long de la troisième ligne et boucle sans fin avec un plan proche oblique (gel du client). Le culling se fait avec la projection normale, l'oblique ne sert qu'au dessin.
- `Entity.setPos` prévient le gestionnaire d'entities : déplacer temporairement un joueur vers une zone non chargée de son monde le sort de sa section et de son suivi. Couper l'écouteur (`setChangeListener(EntityChangeListener.NONE)`) le temps de l'appel, puis le rétablir après avoir remis la position.
- `ClientWorld.getEntities()` du monde principal est la seule liste que vanilla tique : un `ClientWorld` secondaire doit appeler lui-même `tickEntities()` ; ses sections ne tiquent qu'après `loadChunkFromPacket` (`resetChunkColor` → `startTicking`).
- Les récepteurs `ClientPlayNetworking` de Fabric s'exécutent déjà sur le thread de rendu, dans l'ordre des paquets. Ne pas les envelopper dans `client.execute(...)` : appelé depuis une tâche en cours, `ReentrantThreadExecutor` le remet en fin de file, derrière les paquets vanilla déjà reçus (un chunk du nouveau monde appliqué à l'ancien → `IndexOutOfBoundsException`, déconnexion).
- Tests en jeu : le serveur GameTest n'a ni Level 0 ni client ; le Nether sert de dimension distante, les joueurs sont connectés
  par `GameTestPlayers` (vrai `onPlayerConnect` sur canal en mémoire, `setOnGround(true)` sinon le minage est cinq fois plus lent),
  les payloads client s'injectent par `onCustomPayload`, et les tests qui posent quoi que ce soit utilisent l'arène
  `petasse_gang_additions_gametest:test_arena` (24 blocs) : la structure vide de Fabric fait un bloc et les voisins se marchent dessus.
  Le dossier de run `build/run/gameTest` est recréé à chaque lancement et le serveur de test tique sans attendre : générer
  l'arrivée d'avance (`pregenerateArrival`) et guetter les chunks du Nether (`PortalViewTracker.watchedChunkCount() > 0`) plutôt que compter les ticks. Un bateau embarque de lui-même les créatures qui le touchent.
- L'extension Java de VSCode compile avec JDT dans `build/classes/java/main` (même dossier que Gradle) ; avec un modèle de projet périmé, elle y écrit des classes `Unresolved compilation problems` qui font planter le client au chargement. L'autobuild est coupé dans `.vscode/settings.json` ; ne jamais le réactiver sans un import Gradle sain (JDK 21).

## Avant de push

Suivre `docs/prompt/BEFOREPUSH.md` : `./gradlew clean build --warning-mode all` sans warning (il enchaîne Checkstyle,
tests unitaires, tests en jeu, verrou de couverture à 100 % des instructions et 96 % des branches, contrôle du contenu
du JAR), CHANGELOG et docs à jour. Toute instruction nouvelle hors colle OpenGL
client doit être couverte par un test unitaire ou un test en jeu, sinon le build échoue (`docs/TESTING.md`, section Couverture).
