# Troubleshooting — PeTaSsE_gAnG_Additions

---

## Build

### `Could not resolve net.minecraftforge:forge`
**Cause :** Maven Forge inaccessible ou mauvaise version.
```bash
./gradlew build --refresh-dependencies
# Si ça persiste, vérifier forge_version dans gradle.properties
```

### `Unsupported class file major version`
**Cause :** Java version incorrecte. Le build nécessite Java 25.
```bash
java -version  # doit afficher "25"
# Configurer JAVA_HOME vers JDK 25
export JAVA_HOME=/path/to/jdk-25
```

### `Task 'genEclipseRuns' not found`
**Cause :** ForgeGradle pas encore téléchargé ou mauvaise version.
```bash
./gradlew dependencies  # force le téléchargement
./gradlew genEclipseRuns
```

### `gradle-wrapper.jar not found`
**Cause :** Le JAR binaire du wrapper n'est pas dans le repo.
```bash
# Si Gradle est installé globalement :
gradle wrapper --gradle-version 9.3.0
```

---

## Runtime (in-game)

### `The Mod File build/resources/main has mods that were not found`
**Cause :** Le `ClasspathLocator` de FML crée un `SecureJar` uniquement depuis le répertoire contenant `META-INF/mods.toml` (`build/resources/main`). Sans configuration spéciale, les classes compilées sont dans `build/classes/java/main` — répertoire séparé que Forge ne voit pas.

**Fix dans `build.gradle`** (déjà appliqué) :
```groovy
sourceSets {
    main {
        output.resourcesDir = compileJava.destinationDirectory
    }
}
```
Cela force `processResources` à écrire `mods.toml` dans le même répertoire que les classes compilées.

**Symptôme sans le fix :** l'écran rouge Minecraft avec "1 error has occurred during loading".

---

### `NullPointerException: Item id not set` au register
**Cause :** En MC 26.1, le constructeur `Item(Properties)` appelle `Properties.itemIdOrThrow()`. Sans `setId()`, NPE au moment de l'enregistrement.

**Fix dans `ModItems.java`** (déjà appliqué) :
```java
ITEMS.register("gang_badge", () -> new GangBadgeItem(
    new Item.Properties()
        .setId(ITEMS.key("gang_badge"))  // OBLIGATOIRE en MC 26.1
        .stacksTo(1)
        .rarity(Rarity.EPIC)
));
```

---

### Le mod n'apparaît pas dans la liste des mods
- Vérifie que `mods.toml` a les bonnes valeurs (`modId`, `loaderVersion`)
- Vérifie que le JAR est dans le dossier `mods/`
- Vérifie la version Java : MC 26.1 requiert Java 25

### `Registry object not present` au démarrage
**Cause :** Un `RegistryObject.get()` appelé avant que la registration soit terminée.
- Ne jamais appeler `.get()` dans un initialiseur statique d'un autre registre
- Utiliser `.get()` seulement dans des méthodes appelées après `FMLCommonSetupEvent`

### L'onglet créatif PétasseGang n'apparaît pas
- Vérifie que `ModCreativeTab.register(modEventBus)` est appelé dans le constructeur `@Mod`
- Vérifie que `ModCreativeTab.PETASSEGANG_TAB` est non-null (test `RegistryTest`)

### Texture `gang_badge` manquante (carré violet)
- Vérifie que `gang_badge.png` est dans `assets/petasse_gang_additions/textures/item/`
- Vérifie que `gang_badge.json` (modèle) référence le bon chemin de texture
- Rebuild : `./gradlew build`

---

## Tests

### `ClassNotFoundException` dans les tests
**Cause :** MC classes pas sur le classpath de test.
- Les tests JUnit qui instancient des classes MC nécessitent le classpath ForgeGradle.
- Utilise `./gradlew test` et non le runner JUnit de l'IDE directement.

### Tests qui passent localement mais échouent en CI
- Vérifier que le cache Gradle CI est à jour (Actions → Caches → supprimer si corrompu)
- S'assurer que le wrapper JAR est généré correctement

---

## Performance

### Build très lent
```bash
# Activer le daemon et le cache (déjà dans gradle.properties)
org.gradle.daemon=true
org.gradle.caching=true

# Forcer le rebuild propre si le cache semble corrompu
./gradlew clean build
```
