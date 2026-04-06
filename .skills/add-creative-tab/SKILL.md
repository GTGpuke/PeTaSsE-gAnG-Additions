---
name: add-creative-tab
description: "Ajouter ou modifier un onglet créatif dans PétasseGang Addons. Déclenche pour 'onglet créatif', 'creative tab', 'catégorie d'items', 'nouvel onglet', 'déplacer item dans tab', 'changer l'icône du tab'."
---

# Skill — Ajouter / Modifier un Creative Tab

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
private static void displayItems(CreativeModeTab.ItemDisplayParameters params,
                                 CreativeModeTab.Output output) {
    // Items existants
    output.accept(ModItems.GANG_BADGE.get());

    // Ajouter ici :
    output.accept(ModItems.MY_NEW_ITEM.get());
}
```

### Changer l'icône du tab

```java
.icon(() -> new ItemStack(ModItems.MY_NEW_ICON_ITEM.get()))
```

---

## Créer un nouvel onglet

**Fichier :** `src/main/java/com/petassegang/addons/creative/ModCreativeTab.java`

```java
public static final RegistryObject<CreativeModeTab> MY_NEW_TAB =
        CREATIVE_MODE_TABS.register("my_tab", () ->
                CreativeModeTab.builder()
                        .title(Component.translatable("itemGroup.petassegang_addons.my_tab"))
                        .icon(() -> new ItemStack(ModItems.MY_ICON_ITEM.get()))
                        // Positionner après le tab PétasseGang :
                        .withTabsBefore(ModCreativeTab.PETASSEGANG_TAB.getKey())
                        .displayItems((params, output) -> {
                            output.accept(ModItems.SOME_ITEM.get());
                        })
                        .build()
        );
```

---

## Ajouter des items vanilla dans le tab

```java
.displayItems((params, output) -> {
    // Items du mod
    output.accept(ModItems.GANG_BADGE.get());

    // Items vanilla (ex: pour contexte/thématique)
    output.accept(Items.DIAMOND);
    output.accept(Items.GOLD_INGOT);
})
```

---

## Traduction de l'onglet

```json
// en_us.json
"itemGroup.petassegang_addons.my_tab": "My Tab Name",

// fr_fr.json
"itemGroup.petassegang_addons.my_tab": "Nom de Mon Onglet",
```

---

## Checklist finale

- [ ] `CREATIVE_MODE_TABS.register(...)` ajouté (si nouvel onglet)
- [ ] `displayItems` mis à jour
- [ ] Lang key ajoutée EN + FR
- [ ] `docs/CHANGELOG.md` mis à jour
- [ ] `./gradlew build` passe
- [ ] Vérifié en jeu (onglet visible en mode créatif)
