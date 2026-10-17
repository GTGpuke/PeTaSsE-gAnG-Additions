package com.petassegang.addons.init;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import com.petassegang.addons.backrooms.portal.entity.BackroomsPortalEntity;
import com.petassegang.addons.core.ModConstants;

/**
 * Registre des entities du mod.
 */
public final class ModEntities {

    /**
     * Portail des Backrooms, toujours enregistre (meme portails desactives) pour que les mondes qui en contiennent
     * restent lisibles. Petite boite, suivie de pres et rafraichie rarement : un portail ne bouge jamais.
     */
    public static final EntityType<BackroomsPortalEntity> BACKROOMS_PORTAL = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(ModConstants.MOD_ID, "backrooms_portal"),
            EntityType.Builder.create(BackroomsPortalEntity::new, SpawnGroup.MISC)
                    .dimensions(0.1f, 0.1f)
                    .maxTrackingRange(8)
                    .trackingTickInterval(20)
                    .build(null));

    private ModEntities() {
        throw new UnsupportedOperationException("Classe de registre.");
    }

    /** Force le chargement de la classe, donc l'enregistrement des entities. */
    public static void register() {
        ModConstants.LOGGER.debug("Entities du mod enregistrees.");
    }
}
