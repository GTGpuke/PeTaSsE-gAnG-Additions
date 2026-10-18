---
name: add-entity
description: "Ajouter une entité/mob au mod GTG's Backrooms (Fabric 1.21.1). Déclenche pour 'mob', 'entité', 'monstre', 'boss', 'NPC', 'créature', 'animal', 'pet', 'entity'."
---

# Skill — Ajouter une Entité / Mob (Fabric 1.21.1)

## Quand utiliser ce skill

- "Ajoute un mob [nom]"
- "Crée une entité [description]"
- "Nouveau boss [nom]"

---

## Étapes

### 1. Définir les paramètres

| Paramètre | Valeur |
|-----------|--------|
| `ENTITY_ID` | `gang_member`, `backrooms_boss` |
| `ClassName` | `GangMemberEntity`, `BackroomsBossEntity` |
| Catégorie | `SpawnGroup.CREATURE` / `MONSTER` / `AMBIENT` / `WATER_CREATURE` |
| HP | `20.0f` (10 cœurs) |
| Taille | `EntityDimensions.changing(0.6f, 1.95f)` (humanoïde) |

---

### 2. Créer la classe entité

**Fichier :** `src/main/java/com/gtgpuke/gtgsbackrooms/entity/MyEntity.java`

```java
package com.gtgpuke.gtgsbackrooms.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.world.World;

/**
 * [Nom] — [Description].
 */
public class MyEntity extends PathAwareEntity {

    public MyEntity(EntityType<? extends MyEntity> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(1, new WanderAroundFarGoal(this, 1.0));
        goalSelector.add(2, new LookAroundGoal(this));
    }
}
```

---

### 3. Créer ModEntities.java (si absent)

**Fichier :** `src/main/java/com/gtgpuke/gtgsbackrooms/init/ModEntities.java`

```java
package com.gtgpuke.gtgsbackrooms.init;

import com.gtgpuke.gtgsbackrooms.entity.MyEntity;
import com.gtgpuke.gtgsbackrooms.core.ModConstants;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModEntities {

    public static final EntityType<MyEntity> MY_ENTITY = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(ModConstants.MOD_ID, "my_entity"),
            EntityType.Builder.create(MyEntity::new, SpawnGroup.CREATURE)
                    .dimensions(0.6f, 1.95f)
                    .build()
    );

    public static void initialize() { }

    private ModEntities() { throw new UnsupportedOperationException("Classe utilitaire."); }
}
```

Appeler `ModEntities.initialize()` dans `onInitialize()`.

---

### 4. Enregistrer les attributs

Dans `GtgsBackroomsMod.onInitialize()`, via l'event Fabric :

```java
// Dans onInitialize() :
FabricDefaultAttributeRegistry.register(ModEntities.MY_ENTITY, MyEntity.createAttributes());
```

---

### 5. Créer le renderer (CLIENT uniquement)

**Fichier :** `src/main/java/com/gtgpuke/gtgsbackrooms/client/renderer/MyEntityRenderer.java`

```java
package com.gtgpuke.gtgsbackrooms.client.renderer;

import com.gtgpuke.gtgsbackrooms.entity.MyEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class MyEntityRenderer extends MobEntityRenderer<MyEntity, MyEntityModel<MyEntity>> {

    private static final Identifier TEXTURE =
            Identifier.of("gtgs_backrooms", "textures/entity/my_entity.png");

    public MyEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new MyEntityModel<>(context.getPart(MyEntityModel.MODEL_LAYER)), 0.5f);
    }

    @Override
    public Identifier getTexture(MyEntity entity) {
        return TEXTURE;
    }
}
```

Enregistrer dans `GtgsBackroomsClientMod.onInitializeClient()` :
```java
EntityRendererRegistry.register(ModEntities.MY_ENTITY, MyEntityRenderer::new);
```

---

### 6. Fichiers ressources

- `textures/entity/my_entity.png` — texture du mob (64×32 ou 64×64)
- `lang/en_us.json` : `"entity.gtgs_backrooms.my_entity": "My Entity"`
- `lang/fr_fr.json` : traduction FR

---

## Checklist finale

- [ ] `entity/MyEntity.java`
- [ ] `init/ModEntities.java` — champ `static final` via `Registry.register(Registries.ENTITY_TYPE, ...)`
- [ ] `ModEntities.initialize()` appelé dans `onInitialize()`
- [ ] `FabricDefaultAttributeRegistry.register()` dans `onInitialize()`
- [ ] `client/renderer/MyEntityRenderer.java` avec `@Environment(EnvType.CLIENT)`
- [ ] Renderer enregistré dans `onInitializeClient()`
- [ ] `textures/entity/my_entity.png`
- [ ] Lang keys EN + FR
- [ ] `docs/ENTITIES.md` + `CHANGELOG.md` mis à jour
- [ ] Tests mis à jour
- [ ] `./gradlew build` + `./gradlew test` passent
