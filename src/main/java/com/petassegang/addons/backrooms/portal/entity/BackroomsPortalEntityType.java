package com.petassegang.addons.backrooms.portal.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import com.petassegang.addons.core.ModConstants;

/**
 * Type de l'entity portail, toujours enregistre pour que les mondes restent lisibles.
 */
public final class BackroomsPortalEntityType {

    public static final Identifier ID = Identifier.of(ModConstants.MOD_ID, "backrooms_portal");

    /** Petite boite, suivie de pres et rafraichie rarement : un portail ne bouge jamais. */
    public static final EntityType<BackroomsPortalEntity> INSTANCE = EntityType.Builder
            .create(BackroomsPortalEntity::new, SpawnGroup.MISC)
            .dimensions(0.1f, 0.1f)
            .maxTrackingRange(8)
            .trackingTickInterval(20)
            .build(null);

    private static boolean registered;

    private BackroomsPortalEntityType() {
        throw new UnsupportedOperationException("Classe de registre.");
    }

    /** Enregistre le type ; idempotent. */
    public static void register() {
        if (!registered) {
            Registry.register(Registries.ENTITY_TYPE, ID, INSTANCE);
            registered = true;
        }
    }
}
