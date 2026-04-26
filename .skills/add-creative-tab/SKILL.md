---
name: add-creative-tab
description: "Ajouter ou modifier un onglet créatif dans PeTaSsE_gAnG_Additions (Fabric 1.21.1). Déclenche pour 'onglet créatif', 'creative tab', 'catégorie d'items', 'nouvel onglet', 'déplacer item dans tab', 'changer l'icône du tab'."
---

# Skill — Ajouter / Modifier un Creative Tab (Fabric 1.21.1)

## Quand utiliser ce skill

- "Ajoute un nouvel onglet créatif [nom]"
- "Déplace [item] dans un autre onglet"
- "Change l'icône du tab PétasseGang"
- "Crée une catégorie séparée pour [type de contenu]"

---

## Modifier le tab existant (PétasseGang)

### Ajouter un item au tab existant

**Fichier :** `src/main/java/com/petassegang/addons/creative/ModCreativeTab.java`

```java
private static void displayItems(ItemGroup.DisplayContext context, ItemGroup.Entries entries) {
    // Items existants
    entries.add(ModItems.GANG_BADGE);

    // Ajouter ici :
    entries.add(ModItems.MY_NEW_ITEM);
}
```

**Pas de `.get()`** — les champs `ModItems.*` sont directement les instances d'`Item`.

### Changer l'icône du tab

```java
.icon(() -> new ItemStack(ModItems.MY_NEW_ICON_ITEM))
```

---

## Créer un nouvel onglet

**Fichier :** `src/main/java/com/petassegang/addons/creative/ModCreativeTab.java`

```java
package com.petassegang.addons.creative;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import com.petassegang.addons.init.ModItems;
import com.petassegang.addons.util.ModConstants;

public final class ModCreativeTab {

    public static final ItemGroup MY_NEW_TAB = Registry.register(
            Registries.ITEM_GROUP,
            Identifier.of(ModConstants.MOD_ID, "my_tab"),
            FabricItemGroup.builder()
                    .displayName(Text.translatable("itemGroup.petasse_gang_additions.my_tab"))
                    .icon(() -> new ItemStack(ModItems.MY_ICON_ITEM))
                    .entries((context, entries) -> {
                        entries.add(ModItems.SOME_ITEM);
                    })
                    .build()
    );

    public static void register() { }

    private ModCreativeTab() { throw new UnsupportedOperationException("Classe de registre."); }
}
```

Appeler `ModCreativeTab.register()` dans `PeTaSsEgAnGAdditionsMod.onInitialize()`.

---

## Ajouter des items dans un tab vanilla existant

Pour injecter des items dans un onglet vanilla (outils, combat, etc.) sans créer de nouveau tab :

```java
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEvents;
import net.minecraft.item.ItemGroups;

// Dans onInitialize() :
FabricItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(context -> {
    context.add(ModItems.MY_ITEM);
});
```

---

## Ajouter des items vanilla dans le tab du mod

```java
private static void displayItems(ItemGroup.DisplayContext context, ItemGroup.Entries entries) {
    // Items du mod
    entries.add(ModItems.GANG_BADGE);

    // Items vanilla (ex: pour contexte/thématique)
    entries.add(Items.DIAMOND);
    entries.add(Items.GOLD_INGOT);
}
```

---

## Traduction de l'onglet

```json
// en_us.json
"itemGroup.petasse_gang_additions.my_tab": "My Tab Name",

// fr_fr.json
"itemGroup.petasse_gang_additions.my_tab": "Nom de Mon Onglet",
```

---

## Checklist finale

- [ ] `Registry.register(Registries.ITEM_GROUP, ...)` ajouté (si nouvel onglet)
- [ ] `FabricItemGroup.builder()` utilisé (pas `CreativeModeTab.builder()`)
- [ ] `.displayName(Text.translatable(...))` (pas `Component.translatable`)
- [ ] Champs `ModItems.*` utilisés **sans** `.get()`
- [ ] `register()` appelé dans `onInitialize()` (si nouvel onglet)
- [ ] Lang key ajoutée EN + FR
- [ ] `docs/CHANGELOG.md` mis à jour
- [ ] `./gradlew build` passe
- [ ] Vérifié en jeu (onglet visible en mode créatif)
