package com.gtgpuke.gtgsbackrooms.compat.iris;

import net.irisshaders.iris.api.v0.IrisApi;

/** Seule classe du mod qui reference l'API d'Iris : chargee seulement quand Iris l'est ({@link IrisCompat}). */
final class IrisApiBridge {

    private IrisApiBridge() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /** {@code true} si Iris rend le monde avec un pack de shaders (pas son pipeline vanilla). */
    static boolean shaderPackInUse() {
        return IrisApi.getInstance().isShaderPackInUse();
    }
}
