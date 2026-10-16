package com.petassegang.addons.backrooms.level0.client.model;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.util.Identifier;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;

import com.petassegang.addons.core.ModConstants;

/**
 * Remplace le modele du mur adaptatif par {@link LevelZeroWallpaperBakedModel} apres cuisson.
 *
 * <p>Le bloc possede 16 variantes de block state qui partagent toutes le meme
 * wrapper : il est construit au premier appel de chaque rechargement de ressources.
 */
public final class LevelZeroWallpaperModelHandler {

    private static final String WALLPAPER_PATH = "level_zero_wallpaper_adaptive";

    private static boolean registered;
    private static volatile LevelZeroWallpaperBakedModel sharedWrapper;

    private LevelZeroWallpaperModelHandler() {
        throw new UnsupportedOperationException("Classe utilitaire client.");
    }

    /** Enregistre le plugin de chargement de modele ; idempotent. */
    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        ModelLoadingPlugin.register(pluginContext -> {
            // Le wrapper est reconstruit a chaque rechargement de ressources (F3+T).
            sharedWrapper = null;
            pluginContext.modifyModelAfterBake().register(LevelZeroWallpaperModelHandler::onModifyAfterBake);
        });
    }

    private static BakedModel onModifyAfterBake(BakedModel model, ModelModifier.AfterBake.Context context) {
        ModelIdentifier topId = context.topLevelId();
        if (model == null
                || topId == null
                || !ModConstants.MOD_ID.equals(topId.id().getNamespace())
                || !WALLPAPER_PATH.equals(topId.id().getPath())) {
            return model;
        }
        LevelZeroWallpaperBakedModel wrapper = sharedWrapper;
        if (wrapper != null) {
            return wrapper;
        }
        BakedModel alternateModel = bake(context, "block/level_zero_wallpaper_aged");
        LevelZeroWallpaperBakedModel.DirectionalModels baseboards = bakeDirectional(context, "level_zero_baseboard");
        LevelZeroWallpaperBakedModel.DirectionalModels switches = bakeDirectional(context, "level_zero_switch");
        LevelZeroWallpaperBakedModel.DirectionalModels outlets = bakeDirectional(context, "level_zero_outlet");
        if (alternateModel == null || baseboards == null || switches == null || outlets == null) {
            ModConstants.LOGGER.warn("Impossible de preparer le rendu adaptatif du papier peint du Level 0.");
            return model;
        }
        wrapper = new LevelZeroWallpaperBakedModel(model, alternateModel, baseboards, switches, outlets);
        sharedWrapper = wrapper;
        ModConstants.LOGGER.info("Rendu adaptatif du papier peint du Level 0 actif.");
        return wrapper;
    }

    private static @Nullable LevelZeroWallpaperBakedModel.DirectionalModels bakeDirectional(
            ModelModifier.AfterBake.Context context, String basePath) {
        BakedModel north = bake(context, "block/" + basePath + "_north");
        BakedModel south = bake(context, "block/" + basePath + "_south");
        BakedModel west = bake(context, "block/" + basePath + "_west");
        BakedModel east = bake(context, "block/" + basePath + "_east");
        if (north == null || south == null || west == null || east == null) {
            return null;
        }
        return new LevelZeroWallpaperBakedModel.DirectionalModels(north, south, west, east);
    }

    private static @Nullable BakedModel bake(ModelModifier.AfterBake.Context context, String path) {
        return context.baker().bake(Identifier.of(ModConstants.MOD_ID, path), context.settings());
    }
}
