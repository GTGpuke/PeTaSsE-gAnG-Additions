---
name: add-entity
description: "Ajouter une entité/mob au mod PeTaSsE_gAnG_Additions. Déclenche pour 'mob', 'entité', 'monstre', 'boss', 'NPC', 'créature', 'animal', 'pet', 'entity'."
---

# Skill — Ajouter une Entité / Mob

## Quand utiliser ce skill

- "Ajoute un mob [nom]"
- "Crée une entité [description]"
- "Nouveau boss [nom]"

---

## Étapes

### 1. Définir les paramètres

| Paramètre | Valeur |
|-----------|--------|
| `ENTITY_ID` | `gang_member`, `petasse_boss` |
| `ClassName` | `GangMemberEntity`, `PetasseBossEntity` |
| Catégorie | `MobCategory.CREATURE` / `MONSTER` / `AMBIENT` / `WATER_CREATURE` |
| HP | `20.0f` (10 coeurs) |
| Taille | `EntityDimensions.scalable(0.6f, 1.95f)` (humanoid) |

---

### 2. Créer la classe entité

**Fichier :** `src/main/java/com/petassegang/addons/entity/MyEntity.java`

```java
package com.petassegang.addons.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.level.Level;

/**
 * [Nom] — [Description].
 */
public class MyEntity extends PathfinderMob {

    public MyEntity(EntityType<? extends MyEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 3.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }
}
```

---

### 3. Créer ModEntities.java (si absent)

**Fichier :** `src/main/java/com/petassegang/addons/init/ModEntities.java`

```java
package com.petassegang.addons.init;

import com.petassegang.addons.util.ModConstants;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ModConstants.MOD_ID);

    public static final RegistryObject<EntityType<MyEntity>> MY_ENTITY =
            ENTITIES.register("my_entity", () ->
                    EntityType.Builder.of(MyEntity::new, MobCategory.CREATURE)
                            .sized(0.6f, 1.95f)
                            .clientTrackingRange(10)
                            .build("my_entity")
            );

    public static void register(BusGroup modBusGroup) { ENTITIES.register(modBusGroup); }

    private ModEntities() { throw new UnsupportedOperationException("Registry class"); }
}
```

---

### 4. Enregistrer les attributs

Dans `PeTaSsEgAnGAdditionsMod.commonSetup()` (via `FMLCommonSetupEvent`), ou via un `EntityAttributeCreationEvent` :

```java
@SubscribeEvent
public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
    event.put(ModEntities.MY_ENTITY.get(), MyEntity.createAttributes().build());
}
```

---

### 5. Créer le renderer (CLIENT only)

**Fichier :** `src/main/java/com/petassegang/addons/client/renderer/MyEntityRenderer.java`

```java
package com.petassegang.addons.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class MyEntityRenderer extends MobRenderer<MyEntity, MyEntityModel<MyEntity>> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("petasse_gang_additions", "textures/entity/my_entity.png");

    public MyEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new MyEntityModel<>(context.bakeLayer(MyEntityModel.LAYER_LOCATION)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(MyEntity entity) {
        return TEXTURE;
    }
}
```

Enregistrer dans `clientSetup` :
```java
EntityRenderers.register(ModEntities.MY_ENTITY.get(), MyEntityRenderer::new);
```

---

### 6. Fichiers ressources

- `textures/entity/my_entity.png` — texture du mob
- `lang/en_us.json` : `"entity.petasse_gang_additions.my_entity": "My Entity"`
- `lang/fr_fr.json` : traduction FR

---

## Checklist finale

- [ ] `entity/MyEntity.java`
- [ ] `init/ModEntities.java` — RegistryObject ajouté
- [ ] Attributs enregistrés
- [ ] `PeTaSsEgAnGAdditionsMod` — `ModEntities.register(modBusGroup)` + renderer
- [ ] `client/renderer/MyEntityRenderer.java`
- [ ] `textures/entity/my_entity.png`
- [ ] Lang keys EN + FR
- [ ] `docs/ENTITIES.md` + `CHANGELOG.md` mis à jour
- [ ] Tests mis à jour
- [ ] `./gradlew build` + `./gradlew test` passent
