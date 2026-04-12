package com.petassegang.addons.init;

import java.util.Set;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import com.petassegang.addons.block.entity.LevelZeroWallpaperBlockEntity;
import com.petassegang.addons.util.ModConstants;

/**
 * Registre des block entities du mod.
 */
public final class ModBlockEntities {

    /** DeferredRegister des types de block entities. */
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ModConstants.MOD_ID);

    /** Block entity du papier peint adaptatif du Level 0. */
    public static final RegistryObject<BlockEntityType<LevelZeroWallpaperBlockEntity>> LEVEL_ZERO_WALLPAPER =
            BLOCK_ENTITY_TYPES.register(
                    "level_zero_wallpaper",
                    () -> new BlockEntityType<>(
                            LevelZeroWallpaperBlockEntity::new,
                            Set.of(ModBlocks.LEVEL_ZERO_WALLPAPER.get(), ModBlocks.LEVEL_ZERO_WALLPAPER_AGED.get()))
            );

    /**
     * Enregistre le registre sur le bus d'evenements du mod.
     *
     * @param modBusGroup groupe de bus du mod
     */
    public static void register(BusGroup modBusGroup) {
        BLOCK_ENTITY_TYPES.register(modBusGroup);
    }

    private ModBlockEntities() {
        throw new UnsupportedOperationException("Classe de registre.");
    }
}
