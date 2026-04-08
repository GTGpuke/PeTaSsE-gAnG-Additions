# CLAUDE.md — PeTaSsE_gAnG_Additions

> Lu automatiquement à chaque session. Règles OBLIGATOIRES.
> Contexte complet : `docs/prompt/NOKNOWLEDGE.md`
> Audit pré-push : `docs/prompt/BEFOREPUSH.md`

---

## Stack

Minecraft 26.1 / Forge 62.0.x / ForgeGradle 7 / Gradle 9.3.0+ / Java 25
`mod_id`: `petasse_gang_additions` — package: `com.petassegang.addons`

## Pièges FG7 critiques

- `workingDir = project.file(...)` (pas `workingDirectory()`)
- `args = [...]` (pas `programArguments()`)
- Forge déclaré dans `minecraft { version = "..." }`, pas dans `dependencies {}`
- Pas de `reobfJar`, pas de `copyIdeResources`, pas de plugin foojay
- Pas de `genVSCodeRuns` → utiliser `genEclipseRuns`
- Pas de `-Werror` dans `compilerArgs`
- `sourceSets.main.output.resourcesDir = compileJava.destinationDirectory` **OBLIGATOIRE** : le `ClasspathLocator` de FML crée un `SecureJar` uniquement depuis le répertoire contenant `mods.toml`. Sans cette config, Forge trouve le `mods.toml` dans `build/resources/main` mais ne voit pas les classes dans `build/classes/java/main` → crash "0 mods constructed".
- `Item.Properties.setId(ITEMS.key("nom"))` **OBLIGATOIRE** en MC 26.1 : le constructeur `Item(Properties)` appelle `properties.itemIdOrThrow()` ; sans `setId`, crash `NullPointerException: Item id not set` au register.
- `BlockBehaviour.Properties.setId(BLOCKS.key("nom"))` **OBLIGATOIRE** en MC 26.1 : `ofFullCopy()` ne copie PAS le `ResourceKey` du bloc source ; sans `setId`, crash `NullPointerException: Block id not set` lors du register. Même règle que pour les items.

## Lancer le jeu (dev)

Toujours via Gradle, **jamais** via les `.launch` Eclipse directement :
```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-25.0.2"; ./gradlew runClient
```
Ou via **Ctrl+Shift+B** → "runClient" (task VS Code configurée dans `.vscode/tasks.json`).

## Règles de code

- Commentaires, logs, messages d'erreur : **toujours en français**, majuscule, point final
- Contenu in-game : **toujours** `Component.translatable()` + traduction EN et FR
- Imports : `java` → `net.minecraft` → `net.minecraftforge` → `com.petassegang` — aucun wildcard
- Aucun `System.out` / `printStackTrace` → `ModConstants.LOGGER`
- DeferredRegister pour **tous** les enregistrements Forge
- Aucune allocation dans les méthodes tick/render — constantes statiques
- Assertions de test en français avec message explicite

## Audit pré-push

Lire et exécuter `docs/prompt/BEFOREPUSH.md` avant tout push ou feature déclarée terminée.
Ne jamais dire "c'est prêt" sans avoir exécuté l'audit complet.
