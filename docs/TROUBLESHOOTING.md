# Troubleshooting — GTG's Backrooms

## Build

### `ClassNotFoundException` sur toutes les classes de test (Windows)
**Cause** : chemin du projet accentué (`Développement`) + Java 21 : le fichier `@argfile` du classpath est
écrit en UTF-8 et relu en CP1252.
**Fix** : `-Dfile.encoding=COMPAT` dans `org.gradle.jvmargs` (déjà dans `gradle.properties`). Si le
problème réapparaît : `./gradlew --stop && ./gradlew test`.

### `Dependency requires at least JVM runtime version X`
**Cause** : la version de Loom demande une JVM plus récente pour Gradle lui-même. Loom 1.17 tourne sur Java 21 ;
Loom ≥ 1.18 exige Java 25 pour Gradle. Ne monter Loom qu'avec le JDK correspondant.

### `Plugin fabric-loom requires at least Gradle X`
**Cause** : wrapper trop ancien. Mettre à jour `distributionUrl` dans `gradle/wrapper/gradle-wrapper.properties`.

### Warnings javac inattendus
Le build compile en `-Xlint:all` sauf `processing`, `serial` et `classfile` (annotations ErrorProne absentes
des jars Guava/Minecraft). Tout autre warning est à corriger.

---

## Runtime

### Crash au chargement de monde : `Failed to load registries`
Un JSON de worldgen est invalide : lire les `ERROR` qui précèdent dans `run/logs/latest.log`.
Cas connu : `below_trunk_provider` n'existe pas en 1.21.1, utiliser `dirt_provider`.

### Le mod n'apparaît pas
JAR dans `mods/`, Fabric Loader ≥ 0.16.9 et Fabric API présents, Java 21.

### Texture violette sur un item
Modèle `assets/gtgs_backrooms/models/item/<id>.json` manquant ou chemin de texture incorrect.
En 1.21.1 le dossier `assets/<ns>/items/` **n'est pas lu** (format 1.21.4+). `ModRegistriesTest` échoue si un modèle manque.

### Les blocs `level_zero_baseboard` de mon monde ont disparu
Le bloc plinthe autonome a été retiré en 0.8.0 (il était invisible et sans collision). Les plinthes des murs
sont rendues en overlay et ne sont pas concernées.

### Le bloc portail ne fait rien
La feature est coupée par `-Dgtgsbackrooms.portal.enablePortalFeature=false` (ou `-PportalFeature=false` en dev) :
le bloc reste inerte. Sinon, il n'a pas encore trouvé de destination : hors du Level 0 il faut que la dimension
`backrooms_level_0` existe (datapack du mod chargé) ; il réessaie toutes les 5 secondes. Le bloc ne se pose pas : son
cadre, à la taille réglée, traverserait un bloc. Il ne se casse pas : il ne se vise qu'accroupi.

### La lumière ne passe pas par le portail
Le portail est réglé sans lumière (`transmits_light:false`), fermé ou invisible, ou la lumière vient du ciel : seule
celle des blocs passe, d'un niveau de moins que de l'autre côté.

### Le portail est un cadre noir
Le monde distant n'est pas encore arrivé (chunks envoyés 8 par 8, toutes les 5 ticks) ou le chunk d'arrivée est
hors de la fenêtre synchronisée (portail imbriqué lointain). Si c'est permanent, chercher `[Portal]` dans le log : stencil refusé
par le pilote ou exception pendant le rendu distant (rendu coupé pour la session). `-PportalDebugRendering=true`
logge l'état du renderer distant chaque seconde. Les mods qui remplacent le pipeline de rendu du terrain ne sont pas supportés.

### Crash, chargement infini ou écran noir au lancement, avec `java.lang.Error: Unresolved compilation problems`

Ce n'est pas le mod : l'extension Java de VSCode (Eclipse JDT) a compilé une classe avec des erreurs et l'a écrite dans
`build/classes/java/main`, par-dessus la sortie de Gradle (son modèle de projet est périmé : les « cannot be resolved »
du panneau Problems). `.vscode/settings.json` coupe cet autobuild (`java.autobuild.enabled: false`) ; pour retrouver un
modèle sain, « Java: Clean Java Language Server Workspace » puis réimport Gradle avec l'extension sur un JDK 21.
En attendant, `./gradlew runClient` recompile avec javac ; si une classe douteuse traîne : `./gradlew compileJava --rerun-tasks`.

### Déconnexion « Network Protocol Error » en traversant (chunk illisible)

Un chunk du nouveau monde a été appliqué à l'ancien `ClientWorld` : un récepteur de payload a été différé derrière les
paquets vanilla. Les récepteurs client des portails doivent rester synchrones (pas de `client.execute`, voir
`RemoteSyncReceiver.init`).

### Objets de la barre ou de la main teintés (blanc bleuté, noir) quand un portail est à l'écran

Le brouillard d'une vue est resté actif après le rendu des portails : les objets de l'interface, dessinés en
profondeur, en prennent la couleur. `PortalStencilRenderer` garde le brouillard du monde du joueur avant les vues
(`saveMainFog`) et le rend à la suite du rendu vanilla sur tous les chemins, exception comprise (`restoreMainFog`).

### Le ciel est noir à travers un portail (depuis une autre dimension)

Le dôme du ciel (`lightSkyBuffer`, positions seules) se dessine avec le shader courant : vanilla pose le shader des
positions juste avant `renderSky`. Si le dernier shader posé est celui des positions et couleurs (fond du cadre), son
attribut couleur, absent du tampon, vaut noir. `RemoteViewRenderer.render` doit poser `GameRenderer::getPositionProgram`
avant `renderSky`. L'heure et la météo ne sont pas en cause : le monde distant les copie du monde principal à chaque
tick, et son type de dimension (donc son heure fixe éventuelle) est celui annoncé par le serveur.

### Saccade ou recul à l'arrivée, élan perdu

Le serveur a renvoyé une téléportation vanilla après une prédiction client (`requestTeleport` ramène le joueur un tick
en arrière et le client remet sa vitesse à zéro). Les franchissements prédits doivent passer par `placePlayer(..., true)`
dans `TeleportExecutor` ; seul le secours serveur téléporte à la vanilla.

### Le joueur est renvoyé d'un côté à l'autre d'un portail

Deux blocs portail se recouvrent : l'un est posé exactement au point d'arrivée de l'autre, leurs portails se superposent
et chaque arrivée retombe dans le cadre de l'autre. Décaler ou casser l'un des deux (voir `docs/PORTALS.md`, limites).

### Les entités de l'autre côté manquent ou restent figées

Elles n'arrivent qu'avec la fenêtre de chunks synchronisée (7 x 7 chunks autour de l'arrivée) : hors fenêtre, rien.
`-PportalDebugRendering=true` affiche par portail le nombre d'entités du monde distant. Si elles apparaissent mais
restent figées, le tick des mondes distants (`RemoteWorldRegistry.tickRemoteWorlds`) n'est plus appelé depuis
`GtgsBackroomsClientMod`.

### Des portails orphelins traînent dans le Level 0
Ils se retirent seuls dès que le chunk de leur bloc est chargé et que le bloc n'y est plus (les portails d'une
ancienne porte des Backrooms aussi). En dev :
`/backrooms portal list` puis `/backrooms portal clear`.

### Le jeu gèle sans message
Prendre une pile d'exécution du client avant de le tuer : `jps -l` donne le PID de la ligne `devlaunchinjector`,
puis `jstack <PID> > dump.txt`. Le thread `Render thread` dit où ça boucle. Pendant le développement de 0.9.0 un
tel gel venait de `Frustum.coverBoxAroundSetPosition` nourri avec une projection oblique ; les vues de portail
n'utilisent plus que la projection du joueur (voir `docs/PORTALS.md`).

---

## Tests

### `Registry is already frozen`
Un test enregistre du contenu après le bootstrap sans passer par `MinecraftTestBootstrap.ensure()`, ou le
test est lancé hors de Gradle (sans `fabric-loader-junit`). Lancer via `./gradlew test`.

### `ExceptionInInitializerError` dans `DataComponentTypes` ou `Blocks`
Le test touche à Minecraft sans bootstrap : ajouter `MinecraftTestBootstrap.ensure()` dans un `@BeforeAll`.

### `LevelZeroGoldenTest` échoue
La génération a changé. Voir [TESTING.md](TESTING.md#le-test-dempreintes-levelzerogoldentest).

### `runGameTest` échoue ou ne se termine pas
Logs dans `build/run/gameTest/logs/latest.log` (dossier recréé à chaque lancement). Le serveur de test quitte seul
à la fin ; chaque échec est rapporté dans `build/junit.xml` avec son message.
