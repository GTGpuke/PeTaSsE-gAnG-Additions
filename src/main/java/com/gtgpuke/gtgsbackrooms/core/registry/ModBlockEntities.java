package com.gtgpuke.gtgsbackrooms.core.registry;

import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import com.gtgpuke.gtgsbackrooms.core.ModConstants;
import com.gtgpuke.gtgsbackrooms.core.portal.block.BackroomsPortalBlockEntity;

/**
 * Registre des block entities du mod.
 */
public final class ModBlockEntities {

    /** Block entity du bloc portail : reglages, destination et portails. */
    public static final BlockEntityType<BackroomsPortalBlockEntity> BACKROOMS_PORTAL = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(ModConstants.MOD_ID, "backrooms_portal"),
            BlockEntityType.Builder.create(BackroomsPortalBlockEntity::new, ModBlocks.BACKROOMS_PORTAL).build(null));

    private ModBlockEntities() {
        throw new UnsupportedOperationException("Classe de registre.");
    }

    /** Force le chargement de la classe, donc l'enregistrement des block entities. */
    public static void register() {
        ModConstants.LOGGER.debug("Block entities du mod enregistrees.");
    }
}
