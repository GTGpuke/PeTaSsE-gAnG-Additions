package com.petassegang.addons.client.model;

import java.util.Map;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.event.ModelEvent;

import com.petassegang.addons.init.ModBlocks;
import com.petassegang.addons.util.ModConstants;

/**
 * Branche le modele adaptatif du papier peint du Level 0 cote client.
 */
public final class LevelZeroWallpaperModelHandler {

    private static boolean registered;
    private static boolean adaptiveModelPrepared;

    /**
     * Enregistre les listeners de modele cote client.
     */
    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        ModelEvent.ModifyBakingResult.BUS.addListener(LevelZeroWallpaperModelHandler::onModifyBakingResult);
    }

    private static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<BlockState, net.minecraft.client.renderer.block.dispatch.BlockStateModel> models =
                event.getResults().blockStateModels();
        BlockState wallpaperState = ModBlocks.LEVEL_ZERO_WALLPAPER.get().defaultBlockState();
        BlockState alternateWallpaperState = ModBlocks.LEVEL_ZERO_WALLPAPER_AGED.get().defaultBlockState();
        net.minecraft.client.renderer.block.dispatch.BlockStateModel baseModel = models.get(wallpaperState);
        net.minecraft.client.renderer.block.dispatch.BlockStateModel alternateModel = models.get(alternateWallpaperState);

        if (baseModel == null || alternateModel == null) {
            ModConstants.LOGGER.warn("Impossible de preparer le rendu adaptatif du papier peint du Level 0.");
            return;
        }

        models.put(wallpaperState, new LevelZeroWallpaperBlockStateModel(baseModel, alternateModel));
        if (!adaptiveModelPrepared) {
            adaptiveModelPrepared = true;
            ModConstants.LOGGER.info("Le rendu adaptatif du papier peint du Level 0 est actif.");
        }
    }

    private LevelZeroWallpaperModelHandler() {
        throw new UnsupportedOperationException("Classe utilitaire client.");
    }
}
