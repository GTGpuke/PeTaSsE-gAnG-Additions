package com.gtgpuke.gtgsbackrooms.backrooms.portal.block;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;

import com.gtgpuke.gtgsbackrooms.backrooms.portal.entity.PortalFacing;

/**
 * Cadre d'un portail pose par le bloc portail : les blocs qu'il occupe et son centre.
 *
 * <p>Le plan passe au milieu des blocs. Le cadre part du bloc ancre le long de son axe de hauteur sur la hauteur reglee
 * (vers le haut pour un cadre debout ; couche au sol ou au plafond, son axe de hauteur est horizontal), et s'etend de part
 * et d'autre de sa colonne sur la largeur reglee : une largeur paire prend une colonne de plus du cote de l'axe de
 * largeur, et son centre tombe entre deux blocs. Le cadre d'arrivee suit la meme regle autour de son point de sol. Un
 * cadre couche demande aussi {@value #CLEARANCE_BEHIND} blocs libres derriere lui : le corps qui le franchit les occupe
 * avant la bascule.
 */
public final class PortalArea {

    /** Blocs libres exiges derriere un cadre couche, en plus du cadre : la tete ou les pieds d'un joueur qui le franchit. */
    public static final int CLEARANCE_BEHIND = 2;

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

    /** Decalage du centre du cadre le long de l'axe de hauteur : les rangees partent de l'ancre, le centre est au milieu d'elles. */
    static double rowOffset(int height) {
        return (height - 1) / 2.0;
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
        return floor.add(0.0, 0.5, 0.0)
                .add(facing.axisW.multiply(widthOffset(settings.width())))
                .add(facing.axisH.multiply(rowOffset(settings.height())));
    }

    /** Pied du cadre dont le centre est donne : l'inverse de {@link #center(Vec3d, PortalFacing, PortalSettings)}. */
    public static Vec3d floor(Vec3d center, PortalFacing facing, PortalSettings settings) {
        return center.subtract(0.0, 0.5, 0.0)
                .subtract(facing.axisW.multiply(widthOffset(settings.width())))
                .subtract(facing.axisH.multiply(rowOffset(settings.height())));
    }

    /** Centre du cadre d'un bloc portail ancre en {@code anchor}. */
    public static Vec3d center(BlockPos anchor, PortalFacing facing, PortalSettings settings) {
        return center(Vec3d.ofBottomCenter(anchor), facing, settings);
    }

    /**
     * {@code true} si chaque bloc du cadre est libre ou remplacable (air, herbe, neige) : le plan ne traverse aucun bloc.
     * Un cadre couche (au sol, au plafond) demande de plus {@value #CLEARANCE_BEHIND} blocs libres derriere chacun des
     * siens : la bascule se decide aux yeux pour un cadre au sol, aux pieds pour un cadre au plafond, et le reste du corps
     * a deja passe le plan.
     *
     * @param world monde lu
     * @param anchor bloc ancre, premier bloc du cadre le long de son axe de hauteur
     * @param facing orientation du cadre
     * @param settings taille du cadre
     * @return {@code true} si le cadre tient
     */
    public static boolean isFree(BlockView world, BlockPos anchor, PortalFacing facing, PortalSettings settings) {
        BlockPos.Mutable cell = new BlockPos.Mutable();
        int depth = facing.direction.getAxis().isVertical() ? CLEARANCE_BEHIND : 0;
        for (int column = firstColumn(settings.width()); column <= lastColumn(settings.width()); column++) {
            for (int row = 0; row < settings.height(); row++) {
                for (int behind = 0; behind <= depth; behind++) {
                    cell.set(anchor.getX() + (int) (facing.axisW.x * column + facing.axisH.x * row - facing.normal.x * behind),
                            anchor.getY() + (int) (facing.axisW.y * column + facing.axisH.y * row - facing.normal.y * behind),
                            anchor.getZ() + (int) (facing.axisW.z * column + facing.axisH.z * row - facing.normal.z * behind));
                    if (!world.getBlockState(cell).isReplaceable()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
