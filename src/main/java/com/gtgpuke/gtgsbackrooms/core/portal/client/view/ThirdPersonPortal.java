package com.gtgpuke.gtgsbackrooms.core.portal.client.view;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.util.math.Vec3d;

import com.gtgpuke.gtgsbackrooms.core.portal.entity.BackroomsPortalEntity;

/**
 * Camera a la troisieme personne passee de l'autre cote d'un portail.
 *
 * <p>La camera suit le joueur a quatre blocs. Juste apres une traversee, le joueur est de l'autre cote et sa camera
 * est encore derriere le portail d'arrivee : vanilla montrerait le monde d'arrivee vu de derriere ce cadre, et l'image
 * sauterait d'une dimension a l'autre. Tant que le segment de l'oeil a la camera traverse un portail, l'image entiere
 * est donc rendue depuis l'image de la camera, dans le monde ou mene ce portail : celui qu'on vient de quitter, d'ou
 * la camera voyait deja la scene une image plus tot. Quand l'angle de la camera fait passer ce segment hors du cadre,
 * la camera est de nouveau dans le monde du joueur.
 */
public final class ThirdPersonPortal {

    private ThirdPersonPortal() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /**
     * Portail visible dont le plan est traverse de face par le segment {@code eye -> camera}, dans son cadre ; le plus
     * proche de l'oeil s'il y en a plusieurs.
     *
     * @param portals portails du monde du joueur
     * @param eye oeil du joueur
     * @param camera position voulue de la camera, a sa pleine distance
     * @return le portail traverse, ou {@code null}
     */
    public static @Nullable BackroomsPortalEntity crossedBy(List<BackroomsPortalEntity> portals, Vec3d eye, Vec3d camera) {
        BackroomsPortalEntity nearest = null;
        double nearestFraction = Double.MAX_VALUE;
        for (int i = 0; i < portals.size(); i++) {
            BackroomsPortalEntity portal = portals.get(i);
            double fraction = crossingFraction(portal, eye, camera);
            if (portal.visible() && fraction < nearestFraction && portal.frameContains(
                    eye.x + (camera.x - eye.x) * fraction, eye.y + (camera.y - eye.y) * fraction, eye.z + (camera.z - eye.z) * fraction)) {
                nearest = portal;
                nearestFraction = fraction;
            }
        }
        return nearest;
    }

    /**
     * Fraction du segment {@code from -> to} ou il passe le plan du portail de l'avant vers l'arriere.
     *
     * @return fraction entre 0 et 1, ou {@link Double#MAX_VALUE} si le segment ne passe pas le plan dans ce sens
     */
    public static double crossingFraction(BackroomsPortalEntity portal, Vec3d from, Vec3d to) {
        double before = portal.signedDistance(from);
        double after = portal.signedDistance(to);
        return before > 0.0 && after < 0.0 ? before / (before - after) : Double.MAX_VALUE;
    }
}
