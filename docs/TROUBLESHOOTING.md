# Troubleshooting - PeTaSsE_gAnG_Additions

---

## Build

### `Could not resolve net.minecraftforge:forge`
**Cause :** Maven Forge inaccessible ou mauvaise version.
```bash
./gradlew build --refresh-dependencies
# Si cela persiste, verifier forge_version dans gradle.properties
```

### `Unsupported class file major version`
**Cause :** Version Java incorrecte. Le build necessite Java 25.
```bash
java -version
# Doit afficher "25"
```

### `Task 'genEclipseRuns' not found`
**Cause :** ForgeGradle pas encore telecharge ou mauvaise version.
```bash
./gradlew dependencies
./gradlew genEclipseRuns
```

### `gradle-wrapper.jar not found`
**Cause :** Le binaire du wrapper Gradle est absent.
```bash
gradle wrapper --gradle-version 9.3.0
```

---

## Runtime

### `The Mod File build/resources/main has mods that were not found`
**Cause :** FML ne trouve pas les classes compilees si `mods.toml` et les `.class` ne sortent pas dans le meme repertoire.

**Fix deja applique dans `build.gradle` :**
```groovy
sourceSets {
    main {
        output.resourcesDir = compileJava.destinationDirectory
    }
}
```

### `NullPointerException: Item id not set` au register
**Cause :** En MC 26.1, `Item.Properties` doit recevoir un `setId(...)`.

**Fix type :**
```java
new Item.Properties().setId(ITEMS.key("gang_badge"))
```

### Le mod n'apparait pas dans la liste des mods
- Verifier que le JAR est dans `mods/`.
- Verifier que `mods.toml` contient le bon `modId`.
- Verifier que Java 25 est utilise.

### `Registry object not present` au demarrage
**Cause :** Un `RegistryObject.get()` est appele trop tot.

**Regle :**
- Ne pas appeler `.get()` dans un initialiseur statique dependant d'un autre registre.
- Utiliser `.get()` seulement apres la phase d'enregistrement Forge.

### Texture manquante sur un item
**Cause principale en MC 26.1 :** Le fichier `assets/<namespace>/items/<item_id>.json` manque.

**Exemple :**
```json
{
  "model": {
    "type": "minecraft:model",
    "model": "petasse_gang_additions:item/gang_badge"
  }
}
```

Autres causes possibles :
- Texture absente dans `textures/item/`.
- Modele avec un chemin de texture incorrect.
- Build non regenere.

### Quelques murs du Level 0 affichent temporairement la mauvaise variante de papier peint
**Cause :** Le papier peint adaptatif du Level 0 repose sur une `BlockEntity` synchronisee cote client, mais uniquement sur les transitions mixtes. Selon le timing de chargement du chunk, un mur mixte peut etre rendu avec un etat visuel transitoire avant la reception complete du `faceMask`.

**Etat actuel :**
- Le `faceMask` est calcule a la generation.
- Il est stocke uniquement pour les colonnes mixtes dans la `BlockEntity`.
- Les murs 100 % jaunes et 100 % blancs sont maintenant de simples blocs sans pipeline adaptatif.
- Le client force maintenant un refresh de `ModelData` au chargement, a la reception du tag de chunk et a la reception du packet reseau.

**Impact :**
- Le probleme semble fortement reduit.
- Le pipeline reste plus sensible qu'un bloc vanilla sans rendu adaptatif.

**Contournements utiles :**
- Recharger la zone.
- Tester sur de nouveaux chunks du Level 0.
- Comparer en priorite les murs generes naturellement avant les murs poses a la main.

**Piste restante si le bug reapparait :**
- Le prochain suspect sera le mixage des `BlockStateModelPart` dans `LevelZeroWallpaperBlockStateModel`, plus que la generation ou les biomes.

### Crash bootstrap `Field fluid is not private and an instance field`
**Cause probable :** Conflit Forge ou environnement client incoherent, generalement avant le vrai chargement du mod.

**Constat actuel :**
- Le repo ne contient pas d'`AccessTransformer`.
- Le crash apparait pendant le bootstrap Forge, avant la logique metier du Level 0.

**Verification conseillee :**
- Tester avec une instance Forge propre.
- Ne laisser que le bon JAR du mod dans `mods/`.
- Verifier que le JAR partage est bien celui de la version attendue.

---

## Tests

### `ClassNotFoundException` dans les tests
**Cause :** Les classes Minecraft et Forge ne doivent pas etre lancees depuis le runner JUnit brut de l'IDE.

**Commande correcte :**
```bash
./gradlew test
```

### `ClassNotFoundException` sur toutes les classes de test sous Windows
**Cause :** Le chemin du projet contient un caractere accentue, ce qui peut corrompre le classpath du worker Gradle.

**Contournement :**
- Deplacer le projet vers un chemin sans accent.
- Ou utiliser temporairement :
```bash
./gradlew build -x test
```

---

## Performance

### Build tres lent
```bash
./gradlew clean build
```

Verifier aussi que :
- `org.gradle.daemon=true`
- `org.gradle.caching=true`

Ces options sont deja activees dans `gradle.properties`.
