---
name: project-conventions
description: "Rappelle les conventions du projet PétasseGang Addons : Mod ID, structure des packages, nommage, patterns DeferredRegister, imports standards, séparation client/serveur, règles qualité. Déclenche pour 'conventions', 'structure', 'règles', 'comment ajouter', 'architecture', 'comment organiser'."
---

# Conventions du projet PétasseGang Addons

## Identifiants clés

| Constante | Valeur |
|-----------|--------|
| MOD_ID | `petassegang_addons` |
| Package racine | `com.petassegang.addons` |
| Version MC | `26.1` |
| Version Forge | `62.0.x` |
| Java | `25` |

## Structure des packages

```
com.petassegang.addons/
├── PetasseGangAddonsMod.java   ← @Mod, lifecycle, registration wiring
├── config/ModConfig.java        ← ForgeConfigSpec
├── creative/ModCreativeTab.java ← creative tab DeferredRegister
├── init/ModItems.java           ← DeferredRegister<Item>
│   (init/ModBlocks, ModEntities, ModSounds…)
├── item/                        ← classes d'items custom
├── block/                       ← classes de blocs custom
├── entity/                      ← classes d'entités custom
├── world/                       ← génération monde / dimensions
├── network/                     ← packets
├── client/                      ← CLIENT-only (renderers, GUI)
└── util/ModConstants.java       ← MOD_ID, MOD_NAME, LOGGER
```

## Conventions de nommage

| Type | Convention | Exemple |
|------|-----------|---------|
| Classe Java | PascalCase | `GangBadgeItem` |
| Méthode | camelCase | `appendHoverText` |
| Constante | UPPER_SNAKE | `GANG_BADGE`, `MOD_ID` |
| Resource ID | lowercase_snake | `gang_badge` |
| Lang key item | `item.<mod_id>.<id>` | `item.petassegang_addons.gang_badge` |
| Lang key block | `block.<mod_id>.<id>` | |
| Lang key tab | `itemGroup.<mod_id>.<id>` | |

## Pattern DeferredRegister (à toujours respecter)

```java
// Dans init/ModFoos.java
public static final DeferredRegister<Foo> FOOS =
    DeferredRegister.create(ForgeRegistries.FOOS, ModConstants.MOD_ID);

public static final RegistryObject<Foo> MY_FOO = FOOS.register(
    "my_foo", () -> new MyFoo(/* properties */));

public static void register(IEventBus bus) { FOOS.register(bus); }
```

## Imports standards

```java
import com.petassegang.addons.util.ModConstants;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
```

## Règles qualité

- Zéro import wildcard (`import java.util.*` interdit)
- Zéro allocation dans hot-paths (tick, render) — utiliser `static final`
- Commentaires en anglais
- Logger uniquement via `ModConstants.LOGGER`
- Code CLIENT-only → `client/` package + guard `FMLEnvironment.dist`

## Séparation client/serveur

```java
// Dans @Mod constructor :
if (FMLEnvironment.dist == Dist.CLIENT) {
    modEventBus.addListener(this::clientSetup);
}
// Dans clientSetup : registrations client (renderers, screens)
// Tout ce qui est CLIENT-only doit être dans client/ package
```

## Mise à jour obligatoire après chaque ajout

- `lang/en_us.json` + `lang/fr_fr.json`
- `docs/ITEMS.md` (ou BLOCKS.md, etc.)
- `docs/CHANGELOG.md`
- Test correspondant dans `src/test/`
