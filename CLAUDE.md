# CLAUDE.md — PétasseGang Addons

> Lu automatiquement à chaque session. Règles OBLIGATOIRES.
> Contexte complet : `docs/prompt/NOKNOWLEDGE.md`
> Audit pré-push : `docs/prompt/BEFOREPUSH.md`

---

## Stack

Minecraft 26.1 / Forge 62.0.x / ForgeGradle 7 / Gradle 9.3.0+ / Java 25
`mod_id`: `petassegang_addons` — package: `com.petassegang.addons`

## Pièges FG7 critiques

- `workingDir = project.file(...)` (pas `workingDirectory()`)
- `args = [...]` (pas `programArguments()`)
- Forge déclaré dans `minecraft { version = "..." }`, pas dans `dependencies {}`
- Pas de `reobfJar`, pas de `copyIdeResources`, pas de plugin foojay
- Pas de `genVSCodeRuns` → utiliser `genEclipseRuns`
- Pas de `-Werror` dans `compilerArgs`

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
