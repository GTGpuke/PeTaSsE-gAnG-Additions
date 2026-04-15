package com.petassegang.addons.client.model;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.util.Identifier;

import com.petassegang.addons.util.ModConstants;

/**
 * Branche le modele adaptatif du papier peint du Level 0 cote client.
 *
 * <p>Le bloc possede 16 variants de block state ({@code face_mask=0} a
 * {@code face_mask=15}). {@code modifyModelAfterBake} est appele une fois
 * par variant : le modele alternatif et le wrapper sont crees a la premiere
 * invocation puis reuses pour les 15 variantes suivantes, evitant 15 re-bakes
 * et 15 messages de log en doublon.
 */
public final class LevelZeroWallpaperModelHandler {

    private static boolean registered;

    /** Instance du wrapper partagee entre tous les variants. */
    private static volatile LevelZeroWallpaperBakedModel sharedWrapper;

    /**
     * Enregistre le plugin de chargement de modele cote client.
     * Doit etre appele une seule fois depuis l'initialiseur client.
     */
    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        ModelLoadingPlugin.register(pluginContext -> {
            // Reinitialisation a chaque reload de ressources (F3+T, etc.).
            sharedWrapper = null;
            pluginContext.modifyModelAfterBake().register(LevelZeroWallpaperModelHandler::onModifyAfterBake);
        });
    }

    private static BakedModel onModifyAfterBake(BakedModel model, ModelModifier.AfterBake.Context context) {
        ModelIdentifier topId = context.topLevelId();
        if (topId == null
                || !ModConstants.MOD_ID.equals(topId.id().getNamespace())
                || !"level_zero_wallpaper_adaptive".equals(topId.id().getPath())) {
            return model;
        }

        // Tous les variants (face_mask=0..15) partagent le meme wrapper.
        // On le cree uniquement lors du premier appel du reload courant.
        LevelZeroWallpaperBakedModel wrapper = sharedWrapper;
        if (wrapper != null) {
            return wrapper;
        }

        BakedModel alternateModel = context.baker().bake(
                Identifier.of(ModConstants.MOD_ID, "block/level_zero_wallpaper_aged"),
                context.settings());
        if (model == null || alternateModel == null) {
            ModConstants.LOGGER.warn("Impossible de preparer le rendu adaptatif du papier peint du Level 0.");
            return model;
        }

        wrapper = new LevelZeroWallpaperBakedModel(model, alternateModel);
        sharedWrapper = wrapper;
        ModConstants.LOGGER.info("Rendu adaptatif du papier peint du Level 0 actif.");
        return wrapper;
    }

    private LevelZeroWallpaperModelHandler() {
        throw new UnsupportedOperationException("Classe utilitaire client.");
    }
}
