---
name: add-sound
description: "Ajouter un son au mod PeTaSsE_gAnG_Additions. Déclenche pour 'son', 'audio', 'musique', 'bruit', 'ambiance', 'sfx', 'sound', 'play sound'."
---

# Skill — Ajouter un Son

## Quand utiliser ce skill

- "Ajoute un son [nom] qui joue quand [action]"
- "Crée un son custom [description]"
- "Le [item/bloc] doit faire un bruit [description]"

---

## Étapes

### 1. Préparer le fichier audio

- Format : **OGG Vorbis** (.ogg) — seul format supporté par Minecraft
- Sample rate : 44100 Hz recommandé
- Mono ou stéréo (mono pour les sons positionnels in-world)
- Placer dans : `src/main/resources/assets/petasse_gang_additions/sounds/`

```
assets/petasse_gang_additions/sounds/
├── item/
│   └── gang_badge_equip.ogg
├── block/
│   └── gangite_break.ogg
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
  },
  "block.gangite.break": {
    "subtitle": "subtitles.petasse_gang_additions.block.gangite.break",
    "sounds": [
      { "name": "petasse_gang_additions:block/gangite_break" }
    ]
  }
}
```

La clé (ex: `"item.gang_badge.equip"`) devient la partie après `petasse_gang_additions.` dans le `SoundEvent`.

---

### 3. Créer ModSounds.java (si absent)

**Fichier :** `src/main/java/com/petassegang/addons/init/ModSounds.java`

```java
package com.petassegang.addons.init;

import com.petassegang.addons.util.ModConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ModConstants.MOD_ID);

    public static final RegistryObject<SoundEvent> GANG_BADGE_EQUIP =
            SOUND_EVENTS.register("item.gang_badge.equip",
                    () -> SoundEvent.createVariableRangeEvent(
                            new ResourceLocation(ModConstants.MOD_ID, "item.gang_badge.equip")
                    ));

    public static void register(IEventBus bus) { SOUND_EVENTS.register(bus); }

    private ModSounds() { throw new UnsupportedOperationException("Registry class"); }
}
```

Ajouter `ModSounds.register(modEventBus)` dans `PeTaSsEgAnGAdditionsMod` constructor.

---

### 4. Jouer le son en jeu

```java
// Depuis un Item.use() ou autre :
level.playSound(null, player.blockPosition(),
        ModSounds.GANG_BADGE_EQUIP.get(),
        SoundSource.PLAYERS,
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
- [ ] `init/ModSounds.java` — RegistryObject ajouté
- [ ] `PeTaSsEgAnGAdditionsMod` — `ModSounds.register(bus)` appelé
- [ ] Son appelé depuis le code approprié
- [ ] Sous-titres ajoutés en EN + FR
- [ ] `docs/CHANGELOG.md` mis à jour
- [ ] Testé en jeu (sous-titres visibles si activés)
