---
name: project-conventions
description: "Rappelle les conventions du projet PeTaSsE_gAnG_Additions (Fabric 1.21.1) : mod id, structure des packages, nommage, patterns Registry.register(), imports, séparation client/serveur, règles de génération et de qualité. Déclenche pour 'conventions', 'structure', 'règles', 'comment ajouter', 'architecture' ou 'comment organiser'."
---

# Conventions du projet PeTaSsE_gAnG_Additions (Fabric 1.21.1)

Source de vérité : `CLAUDE.md` (racine) et `docs/ARCHITECTURE.md`.

## Identifiants clés

| Constante | Valeur |
|-----------|--------|
| MOD_ID | `petasse_gang_additions` |
| Package racine | `com.petassegang.addons` |
| Minecraft / Yarn | `1.21.1` / `1.21.1+build.3` |
| Fabric Loader / API | `0.19.5` / `0.116.17+1.21.1` |
| Fabric Loom / Gradle | `1.17.21` / `9.8.1` |
| Java | `21` (toolchain et JVM Gradle) |
| Tests | JUnit `5.11.4` + `fabric-loader-junit`, JaCoCo, GameTest |

## Structure des packages

```text
com.petassegang.addons/
|- PeTaSsEgAnGAdditionsMod.java        <- ModInitializer (registerContent + hooks)
|- PeTaSsEgAnGAdditionsClientMod.java  <- ClientModInitializer
|- core/ModConstants.java
|- config/ModConfig.java
|- init/                                <- ModBlocks, ModItems, ModBlockEntities, ModChunkGenerators, ModCreativeTab, ModNetworking
|- feature/gang/, feature/cursed/       <- une classe par item, sous-package client/ si besoin
|- backrooms/
|  |- BackroomsConstants.java
|  |- level0/
|  |  |- biome/  block/  client/model/
|  |  `- generation/  (LevelZeroChunkGenerator ; coord/ noise/ sector/ layout/ write/)
|  `- portal/                           <- portails (entity, block, transition, interaction, sync, client), voir docs/PORTALS.md
|- perf/                                <- moniteur opt-in (+ client/)
|- gametest/                            <- tests en jeu
`- mixin/                               <- MixinServerChunkManager + client/
```

Pas de dossier vide, pas de `.gitkeep`, pas de `package-info.java`. Un package n'existe que s'il
contient du code utilisé.

## Nommage

| Type | Convention | Exemple |
|------|-----------|---------|
| Classe | PascalCase | `LevelZeroBlockWriter` |
| Méthode / variable | camelCase | `wallpaperFaceMask` |
| Constante | UPPER_SNAKE_CASE | `REGION_MARGIN_CELLS` |
| Identifiant registre | snake_case | `level_zero_wallpaper_adaptive` |
| Clé de langue | `block.<mod_id>.<id>` / `item.<mod_id>.<id>` / `itemGroup.<mod_id>.<id>` | `block.petasse_gang_additions.cursed_log` |

## Enregistrement Fabric

```java
public static final Item GANG_BADGE = Registry.register(
        Registries.ITEM,
        Identifier.of(ModConstants.MOD_ID, "gang_badge"),
        new GangBadgeItem(new Item.Settings().maxCount(1).rarity(Rarity.EPIC)));
```

Le champ contient l'objet (pas de `.get()`). Tout nouvel item va aussi dans `ModCreativeTab.ENTRIES`.

## Imports

`java` → tiers (`com.mojang`, `org.*`) → `net.minecraft` → `net.fabricmc` → `com.petassegang`, aucun wildcard.

## Règles de qualité

- Commentaires, logs (`ModConstants.LOGGER`) et messages d'erreur en français, majuscule, point final, ASCII.
- Texte visible en jeu via `Text.translatable`, traduit en EN **et** FR (vérifié par `ModRegistriesTest`).
- Code client uniquement sous `client/` ou `mixin/client/`.
- Aucune allocation ni concaténation de chaîne dans les méthodes tick/render.
- Pas d'abstraction spéculative : interface, stage ou builder seulement avec un second usage réel.
- Génération du Level 0 déterministe et verrouillée par `LevelZeroGoldenTest` ; sels de `StageRandom` figés ;
  toute évolution voulue régénère les empreintes **et** passe par `docs/CHANGELOG.md`.
- Toute modification touchant `backrooms/level0/` relit d'abord `docs/ARCHITECTURE.md` (section pipeline)
  et, pour une évolution de spec, `docs/backrooms/backrooms-level0-pipeline-v6.md`.
- Textures des blocs du Level 0 en 32x32 ; loot tables dans `data/<modid>/loot_table/blocks/`.
- Les murs du Level 0 sont fixes ; les colonnes enfouies restent en `minecraft:bedrock`.
- `dirt_provider` (pas `below_trunk_provider`) dans un `minecraft:tree`.

## Après chaque ajout

- `lang/en_us.json` et `lang/fr_fr.json`
- `docs/CHANGELOG.md` et la doc métier (`BLOCKS.md`, `ITEMS.md`, `DIMENSIONS.md`, `ENTITIES.md`)
- un test de comportement dans `src/test/` (pas de test par réflexion)
- `./gradlew build --warning-mode all` vert : Checkstyle, tests unitaires, tests en jeu, couverture (100 % des instructions, 96 % des branches), contenu du JAR
