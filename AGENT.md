# CLAUDE.md — PeTaSsE_gAnG_Additions

> Lu automatiquement à chaque session. Règles OBLIGATOIRES.
> Contexte complet : `docs/prompt/NOKNOWLEDGE.md`
> Audit pré-push : `docs/prompt/BEFOREPUSH.md`

---

## Stack

Minecraft **1.21.1** / Fabric Loader **0.16.9** / Fabric Loom **1.9** / Gradle **9.3.0+** / Java **21**
`mod_id`: `petasse_gang_additions` — package: `com.petassegang.addons`

## Pièges Fabric critiques

- **Pas de `DeferredRegister`** → enregistrement direct via `Registry.register(Registries.XXX, Identifier.of(MOD_ID, "name"), object)` dans des champs `static final`
- **`Identifier.of(ns, path)`** — pas `Identifier.fromNamespaceAndPath()`
- **`Item.Settings`** pas `Item.Properties` ; pas de `.setId()` ; `.maxCount()` pas `.stacksTo()`
- **`net.minecraft.item.Item`** (Yarn) — pas `net.minecraft.world.item.Item`
- **`net.minecraft.block.Block`** (Yarn) — pas `net.minecraft.world.level.block.Block`
- **`@Environment(EnvType.CLIENT)`** pas `@OnlyIn(Dist.CLIENT)`
- **`FabricLoader.getInstance().isModLoaded("id")`** pas `ModList.get().isLoaded()`
- **`dirt_provider`** OBLIGATOIRE dans `minecraft:tree` configured_feature — `below_trunk_provider` n'existe pas en 1.21.1 → crash au chargement de monde
- **`-Dfile.encoding=COMPAT`** OBLIGATOIRE dans `org.gradle.jvmargs` sur Windows avec Java 21 si le chemin du projet contient un caractère accentué (`Développement`) — sans ça, le worker de test Gradle ne trouve pas les classes
- **Imports ordre** : `java` → `net.minecraft` → `net.fabricmc` → `com.petassegang` — aucun wildcard
- **`FabricItemGroupEvents.modifyEntriesEvent()`** pour ajouter des items dans un onglet créatif
- **`TypedActionResult<ItemStack>`** pas `InteractionResultHolder`, **`ActionResult`** pas `InteractionResult`
- **`World`** pas `Level`, **`ServerWorld`** pas `ServerLevel`, **`PlayerEntity`** pas `Player`
- **`Registries.BLOCK.getId(block)`** pour obtenir l'ID d'un objet enregistré (pas `.get().getId()`)
- **`hasGlint(ItemStack)`** pas `isFoil(ItemStack)`
- **`getMaxCount()`** pas `getDefaultMaxStackSize()`
- **`finishUsing(ItemStack, World, LivingEntity)`** pas `finishUsingItem(...)`

## Lancer le jeu (dev)

Toujours via Gradle :
```bash
./gradlew runClient
```
Ou via **Ctrl+Shift+B** → "runClient" (task VS Code configurée dans `.vscode/tasks.json`).

## Règles de code

- Commentaires, logs, messages d'erreur : **toujours en français**, majuscule, point final
- Contenu in-game : **toujours** `Text.translatable()` + traduction EN et FR
- Imports : `java` → `net.minecraft` → `net.fabricmc` → `com.petassegang` — aucun wildcard
- Aucun `System.out` / `printStackTrace` → `ModConstants.LOGGER`
- `Registry.register()` pour **tous** les enregistrements Fabric
- Aucune allocation dans les méthodes tick/render — constantes statiques
- Assertions de test en français avec message explicite

## Audit pré-push

Lire et exécuter `docs/prompt/BEFOREPUSH.md` avant tout push ou feature déclarée terminée.
Ne jamais dire "c'est prêt" sans avoir exécuté l'audit complet.
