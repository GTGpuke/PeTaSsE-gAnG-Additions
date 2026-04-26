package com.petassegang.addons.client.model.backrooms;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.util.Identifier;

import com.petassegang.addons.util.ModConstants;

/**
 * Branche le modele adaptatif des plinthes du Level 0 cote client.
 */
public final class LevelZeroBaseboardModelHandler {

    private static boolean registered;
    private static volatile LevelZeroBaseboardBakedModel sharedWrapper;

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        ModelLoadingPlugin.register(pluginContext -> {
            sharedWrapper = null;
            pluginContext.modifyModelAfterBake().register(LevelZeroBaseboardModelHandler::onModifyAfterBake);
        });
    }

    private static BakedModel onModifyAfterBake(BakedModel model, ModelModifier.AfterBake.Context context) {
        ModelIdentifier topId = context.topLevelId();
        if (topId == null
                || !ModConstants.MOD_ID.equals(topId.id().getNamespace())
                || !"level_zero_baseboard".equals(topId.id().getPath())) {
            return model;
        }

        LevelZeroBaseboardBakedModel wrapper = sharedWrapper;
        if (wrapper != null) {
            return wrapper;
        }

        BakedModel northModel = context.baker().bake(
                Identifier.of(ModConstants.MOD_ID, "block/level_zero_baseboard_north"),
                context.settings());
        BakedModel southModel = context.baker().bake(
                Identifier.of(ModConstants.MOD_ID, "block/level_zero_baseboard_south"),
                context.settings());
        BakedModel westModel = context.baker().bake(
                Identifier.of(ModConstants.MOD_ID, "block/level_zero_baseboard_west"),
                context.settings());
        BakedModel eastModel = context.baker().bake(
                Identifier.of(ModConstants.MOD_ID, "block/level_zero_baseboard_east"),
                context.settings());
        if (model == null || northModel == null || southModel == null || westModel == null || eastModel == null) {
            ModConstants.LOGGER.warn("Impossible de preparer le rendu adaptatif des plinthes du Level 0.");
            return model;
        }

        wrapper = new LevelZeroBaseboardBakedModel(model, northModel, southModel, westModel, eastModel);
        sharedWrapper = wrapper;
        ModConstants.LOGGER.info("Rendu adaptatif des plinthes du Level 0 actif.");
        return wrapper;
    }

    private LevelZeroBaseboardModelHandler() {
        throw new UnsupportedOperationException("Classe utilitaire client.");
    }
}
