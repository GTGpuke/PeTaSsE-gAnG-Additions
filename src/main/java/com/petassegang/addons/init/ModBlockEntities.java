package com.petassegang.addons.init;

import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import com.petassegang.addons.backrooms.portal.block.BackroomsDoorBlockEntity;
import com.petassegang.addons.core.ModConstants;

/**
 * Registre des block entities du mod.
 */
public final class ModBlockEntities {

    /** Block entity de la porte Backrooms. */
    public static final BlockEntityType<BackroomsDoorBlockEntity> BACKROOMS_DOOR = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(ModConstants.MOD_ID, "backrooms_door"),
            BlockEntityType.Builder.create(BackroomsDoorBlockEntity::new, ModBlocks.BACKROOMS_DOOR).build(null));

    private ModBlockEntities() {
        throw new UnsupportedOperationException("Classe de registre.");
    }

    /** Force le chargement de la classe, donc l'enregistrement des block entities. */
    public static void register() {
        ModConstants.LOGGER.debug("Block entities du mod enregistrees.");
    }
}
