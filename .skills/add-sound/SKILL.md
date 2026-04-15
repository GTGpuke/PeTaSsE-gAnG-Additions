---
name: add-sound
description: "Ajouter un son au mod PeTaSsE_gAnG_Additions (Fabric 1.21.1). Déclenche pour 'son', 'audio', 'musique', 'bruit', 'ambiance', 'sfx', 'sound', 'play sound'."
---

# Skill — Ajouter un Son (Fabric 1.21.1)

## Quand utiliser ce skill

- "Ajoute un son [nom] qui joue quand [action]"
- "Crée un son custom [description]"
- "Le [item/bloc] doit faire un bruit [description]"

---

## Étapes

### 1. Préparer le fichier audio

- Format : **OGG Vorbis** (.ogg) — seul format supporté par Minecraft
- Sample rate : 44100 Hz recommandé
- Mono pour les sons positionnels in-world
- Placer dans : `src/main/resources/assets/petasse_gang_additions/sounds/`

```
assets/petasse_gang_additions/sounds/
├── item/
│   └── gang_badge_equip.ogg
└── ambient/
    └── gang_realm_ambient.ogg
```

---

### 2. Créer/Mettre à jour sounds.json

**Fichier :** `src/main/resources/assets/petasse_gang_additions/sounds.json`

```json
{
  "item.gang_badge.equip": {
    "subtitle": "subtitles.petasse_gang_additions.item.gang_badge.equip",
    "sounds": [
      {
        "name": "petasse_gang_additions:item/gang_badge_equip",
        "volume": 1.0,
        "pitch": 1.0,
        "weight": 1
      }
    ]
  }
}
```

La clé (`"item.gang_badge.equip"`) devient la partie après `petasse_gang_additions.` dans le `SoundEvent`.

---

### 3. Créer ModSounds.java (si absent)

**Fichier :** `src/main/java/com/petassegang/addons/init/ModSounds.java`

```java
package com.petassegang.addons.init;

import com.petassegang.addons.util.ModConstants;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class ModSounds {

    public static final SoundEvent GANG_BADGE_EQUIP = register("item.gang_badge.equip");

    private static SoundEvent register(String id) {
        Identifier identifier = Identifier.of(ModConstants.MOD_ID, id);
        return Registry.register(Registries.SOUND_EVENT, identifier,
                SoundEvent.of(identifier));
    }

    public static void initialize() {
        // Déclenche le chargement des static fields.
    }

    private ModSounds() { throw new UnsupportedOperationException("Classe utilitaire."); }
}
```

Appeler `ModSounds.initialize()` dans `PeTaSsEgAnGAdditionsMod.onInitialize()`.

---

### 4. Jouer le son en jeu

```java
// Depuis un Item.use() ou autre :
world.playSound(null, player.getBlockPos(),
        ModSounds.GANG_BADGE_EQUIP,
        SoundCategory.PLAYERS,
        1.0f,  // volume
        1.0f   // pitch
);
```

---

### 5. Traductions des sous-titres

```json
// en_us.json
"subtitles.petasse_gang_additions.item.gang_badge.equip": "Gang Badge equipped",

// fr_fr.json
"subtitles.petasse_gang_additions.item.gang_badge.equip": "Badge de Gang équipé",
```

---

## Checklist finale

- [ ] Fichier `.ogg` placé dans `assets/petasse_gang_additions/sounds/`
- [ ] `sounds.json` créé/mis à jour
- [ ] `init/ModSounds.java` — champ `static final` ajouté via `Registry.register(Registries.SOUND_EVENT, ...)`
- [ ] `ModSounds.initialize()` appelé depuis `onInitialize()`
- [ ] Son appelé depuis le code approprié avec `world.playSound()`
- [ ] Sous-titres ajoutés en EN + FR
- [ ] `docs/CHANGELOG.md` mis à jour
- [ ] Testé en jeu (sous-titres visibles si activés)
