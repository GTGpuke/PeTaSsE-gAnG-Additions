package com.petassegang.addons.backrooms.portal.block;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;

import com.petassegang.addons.backrooms.portal.entity.PortalFacing;

/**
 * Cadre d'un portail pose par le bloc portail : les blocs qu'il occupe et son centre.
 *
 * <p>Le plan passe au milieu des blocs. Le cadre monte de la hauteur reglee depuis le pied du bloc ancre, et s'etend de
 * part et d'autre de sa colonne sur la largeur reglee : une largeur paire prend une colonne de plus du cote de l'axe de
 * largeur, et son centre tombe entre deux blocs. Le cadre d'arrivee suit la meme regle autour de son point de sol.
 */
public final class PortalArea {

    private PortalArea() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /** Premiere colonne du cadre, relative a l'ancre, le long de l'axe de largeur. */
    static int firstColumn(int width) {
        return -((width - 1) / 2);
    }

    /** Derniere colonne du cadre, relative a l'ancre, le long de l'axe de largeur. */
    static int lastColumn(int width) {
        return width / 2;
    }

    /** Decalage du centre du cadre le long de l'axe de largeur : un demi-bloc pour une largeur paire, rien sinon. */
    static double widthOffset(int width) {
        return (firstColumn(width) + lastColumn(width)) / 2.0;
    }

    /**
     * Centre d'un cadre pose sur un point de sol.
     *
     * @param floor milieu du bloc ancre, a ses pieds
     * @param facing orientation du cadre
     * @param settings taille du cadre
     * @return centre du cadre
     */
    public static Vec3d center(Vec3d floor, PortalFacing facing, PortalSettings settings) {
        return floor.add(facing.axisW.multiply(widthOffset(settings.width()))).add(0.0, settings.height() / 2.0, 0.0);
    }

    /** Pied du cadre dont le centre est donne : l'inverse de {@link #center(Vec3d, PortalFacing, PortalSettings)}. */
    public static Vec3d floor(Vec3d center, PortalFacing facing, PortalSettings settings) {
        return center.subtract(facing.axisW.multiply(widthOffset(settings.width()))).subtract(0.0, settings.height() / 2.0, 0.0);
    }

    /** Centre du cadre d'un bloc portail ancre en {@code anchor}. */
    public static Vec3d center(BlockPos anchor, PortalFacing facing, PortalSettings settings) {
        return center(Vec3d.ofBottomCenter(anchor), facing, settings);
    }

    /**
     * {@code true} si chaque bloc du cadre est libre ou remplacable (air, herbe, neige) : le plan ne traverse aucun bloc.
     *
     * @param world monde lu
     * @param anchor bloc ancre, en bas du cadre
     * @param facing orientation du cadre, horizontale
     * @param settings taille du cadre
     * @return {@code true} si le cadre tient
     */
    public static boolean isFree(BlockView world, BlockPos anchor, PortalFacing facing, PortalSettings settings) {
        BlockPos.Mutable cell = new BlockPos.Mutable();
        int stepX = (int) facing.axisW.x;
        int stepZ = (int) facing.axisW.z;
        for (int column = firstColumn(settings.width()); column <= lastColumn(settings.width()); column++) {
            for (int row = 0; row < settings.height(); row++) {
                cell.set(anchor.getX() + stepX * column, anchor.getY() + row, anchor.getZ() + stepZ * column);
                if (!world.getBlockState(cell).isReplaceable()) {
                    return false;
                }
            }
        }
        return true;
    }
}
