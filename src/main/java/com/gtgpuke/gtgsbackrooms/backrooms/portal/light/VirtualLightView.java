package com.gtgpuke.gtgsbackrooms.backrooms.portal.light;

/**
 * Sources de lumiere virtuelles portees par un monde ({@code MixinWorld}) : serveur, client ou monde distant du client.
 * Le moteur de lumiere de ce monde les ajoute aux blocs lumineux ({@code MixinChunkBlockLightProvider}).
 */
public interface VirtualLightView {

    /** Sources publiees, jamais {@code null}. */
    VirtualLightSources backrooms$virtualLight();

    /** Publie de nouvelles sources ; le moteur de lumiere les lit des son prochain calcul. */
    void backrooms$setVirtualLight(VirtualLightSources sources);
}
