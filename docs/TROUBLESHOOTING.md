# Troubleshooting — PeTaSsE_gAnG_Additions

---

## Build

### `ClassNotFoundException` sur toutes les classes de test sous Windows
**Cause :** Le chemin du projet contient un caractère accentué (`Développement`).
Avec Java 21 (JEP-400), le daemon Gradle écrit le fichier `@file` de classpath en UTF-8,
mais le launcher natif le relit avec l'encodage système (CP1252 sur Windows FR), corrompant le chemin.

**Fix déjà appliqué dans `gradle.properties` :**
```properties
org.gradle.jvmargs=-Xmx4G -XX:+UseG1GC -XX:+UseStringDeduplication -Dfile.encoding=COMPAT ...
```
`-Dfile.encoding=COMPAT` restaure le comportement pré-JEP-400 où `file.encoding` = encodage natif (CP1252).

**Si le problème réapparaît :** arrêter le daemon et relancer.
```bash
./gradlew --stop && ./gradlew build
```

### `Unsupported class file major version`
**Cause :** Version Java incorrecte. Le build nécessite Java 21.
```bash
java -version
# Doit afficher "21"
```

### `gradle-wrapper.jar not found`
**Cause :** Le binaire du wrapper Gradle est absent.
```bash
gradle wrapper --gradle-version 9.3.0
```

### Build très lent
```bash
./gradlew clean build
```
Vérifier aussi que dans `gradle.properties` :
- `org.gradle.daemon=true`
- `org.gradle.caching=true`

Ces options sont déjà activées dans le repo.

---

## Runtime

### Crash au chargement de monde : `Failed to load registries due to above errors`
**Cause probable :** Un fichier JSON de worldgen est invalide.
Consulter `run/logs/latest.log` pour les lignes `ERROR` qui précèdent le crash.

**Exemple rencontré :**
```
Failed to parse petasse_gang_additions:worldgen/configured_feature/cursed_tree.json
Caused by: No key dirt_provider in MapLike[...]
```
**Fix :** `below_trunk_provider` n'existe pas en MC 1.21.1. Utiliser `dirt_provider` :
```json
"dirt_provider": {
  "type": "minecraft:simple_state_provider",
  "state": { "Name": "minecraft:dirt" }
}
```

### Le mod n'apparaît pas dans la liste des mods
- Vérifier que le JAR est dans `mods/`.
- Vérifier que `fabric.mod.json` contient le bon `id`.
- Vérifier que Fabric Loader est installé sur le client/serveur.
- Vérifier que Java 21 est utilisé.

### Texture manquante sur un item (carré violet)
**Cause principale en MC 1.21.1 :** Le fichier `assets/<namespace>/items/<item_id>.json` manque
ou le modèle JSON pointe vers une texture inexistante.

**Exemple de fichier `items/gang_badge.json` :**
```json
{
  "model": {
    "type": "minecraft:model",
    "model": "petasse_gang_additions:item/gang_badge"
  }
}
```

Autres causes :
- Texture absente dans `textures/item/`.
- Modèle avec un chemin de texture incorrect.
- Build non régénéré (`./gradlew processResources`).

### Quelques murs du Level 0 affichent temporairement la mauvaise variante de papier peint
**Cause :** Le papier peint adaptatif repose sur une `BlockEntity` synchronisée côté client,
uniquement sur les transitions mixtes. Un mur mixte peut être rendu avec un état visuel transitoire
avant la réception du `faceMask`, surtout sur la ligne basse du mur.

**Contournements :**
- Recharger la zone.
- Tester sur de nouveaux chunks du Level 0.

**Piste si le bug réapparaît :**
- Inspecter `LevelZeroWallpaperBlockStateModel` — le mixage des `BlockStateModelPart`.

---

## Tests

### `ExceptionInInitializerError` dans les tests (Bootstrap non initialisé)
**Cause :** Les tests qui instancient des objets Minecraft (items, blocs) nécessitent que
le moteur soit initialisé avec `Bootstrap.bootstrap()`.

**Fix pour un test donné :**
```java
@BeforeAll
static void bootstrapMinecraft() {
    SharedConstants.createGameVersion();
    Bootstrap.bootstrap();
}
```

**Note :** `ignoreFailures = true` est activé dans `build.gradle` pour ne pas bloquer le build
sur ces failures attendues.

### `ClassNotFoundException` dans les tests (depuis l'IDE)
**Cause :** Les classes Minecraft ne doivent pas être lancées depuis le runner JUnit brut de l'IDE
sans le classpath Fabric Loom.

**Commande correcte :**
```bash
./gradlew test
```
