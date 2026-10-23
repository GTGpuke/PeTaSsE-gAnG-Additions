package com.gtgpuke.gtgsbackrooms.core.portal.block;

import java.util.function.Predicate;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;

import com.gtgpuke.gtgsbackrooms.core.portal.entity.PortalFacing;

/**
 * Cadre d'un portail pose par le bloc portail : les blocs qu'il occupe et son centre.
 *
 * <p>Le plan passe au milieu des blocs. Le cadre part du bloc ancre le long de son axe de hauteur sur la hauteur reglee
 * (vers le haut pour un cadre debout ; couche au sol ou au plafond, son axe de hauteur est horizontal), et s'etend de part
 * et d'autre de sa colonne sur la largeur reglee : une largeur paire prend une colonne de plus du cote de l'axe de
 * largeur, et son centre tombe entre deux blocs. Le cadre d'arrivee suit la meme regle autour de son point de sol.
 *
 * <p>Avec le plan au bord du bloc ({@link PortalSettings#flush()}), le plan passe sur la face arriere des blocs du cadre,
 * un demi-bloc en retrait. Derriere le plan, rien n'est exige : un bloc y est cache a qui s'engage dans le cadre
 * ({@link com.gtgpuke.gtgsbackrooms.core.portal.transition.PortalCollision#tunnel}), un cadre se pose contre un mur, un
 * plafond ou un sol plein.
 */
public final class PortalArea {

    /** Pas vers l'avant du cadre qui ramene dans le bloc ancre un pied lu sur sa face arriere (plan au bord du bloc). */
    private static final double INTO_ANCHOR = 0.25;

    private PortalArea() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /** Premiere colonne du cadre, relative a l'ancre, le long de l'axe de largeur. */
    public static int firstColumn(int width) {
        return -((width - 1) / 2);
    }

    /** Derniere colonne du cadre, relative a l'ancre, le long de l'axe de largeur. */
    public static int lastColumn(int width) {
        return width / 2;
    }

    /** Decalage du centre du cadre le long de l'axe de largeur : un demi-bloc pour une largeur paire, rien sinon. */
    static double widthOffset(int width) {
        return (firstColumn(width) + lastColumn(width)) / 2.0;
    }

    /** Recul du plan derriere le milieu des blocs du cadre, le long de la normale : un demi-bloc au bord du bloc, rien sinon. */
    static double planeOffset(PortalSettings settings) {
        return settings.flush() ? 0.5 : 0.0;
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
     * @param settings taille du cadre et place du plan
     * @return centre du cadre
     */
    public static Vec3d center(Vec3d floor, PortalFacing facing, PortalSettings settings) {
        return floor.add(0.0, 0.5, 0.0)
                .add(facing.axisW.multiply(widthOffset(settings.width())))
                .add(facing.axisH.multiply(rowOffset(settings.height())))
                .subtract(facing.normal.multiply(planeOffset(settings)));
    }

    /** Pied du cadre dont le centre est donne : l'inverse de {@link #center(Vec3d, PortalFacing, PortalSettings)}. */
    public static Vec3d floor(Vec3d center, PortalFacing facing, PortalSettings settings) {
        return center.subtract(0.0, 0.5, 0.0)
                .subtract(facing.axisW.multiply(widthOffset(settings.width())))
                .subtract(facing.axisH.multiply(rowOffset(settings.height())))
                .add(facing.normal.multiply(planeOffset(settings)));
    }

    /**
     * Bloc ancre d'un cadre dont on ne connait que le centre, l'orientation et la taille, que son plan passe au milieu des
     * blocs ou au bord : ramene a la colonne et a la rangee de l'ancre, le centre tombe au milieu du bloc ancre ou sur sa
     * face arriere, et un quart de bloc vers l'avant le met dans le bloc dans les deux cas.
     *
     * @param center centre du cadre
     * @param facing orientation du cadre
     * @param width largeur du cadre, en blocs
     * @param height hauteur du cadre, en blocs
     * @return bloc ancre
     */
    public static BlockPos anchor(Vec3d center, PortalFacing facing, int width, int height) {
        Vec3d floor = center.subtract(facing.axisW.multiply(widthOffset(width))).subtract(facing.axisH.multiply(rowOffset(height)));
        return BlockPos.ofFloored(floor.x + facing.normal.x * INTO_ANCHOR, floor.y + facing.normal.y * INTO_ANCHOR,
                floor.z + facing.normal.z * INTO_ANCHOR);
    }

    /** Centre du cadre d'un bloc portail ancre en {@code anchor}. */
    public static Vec3d center(BlockPos anchor, PortalFacing facing, PortalSettings settings) {
        return center(Vec3d.ofBottomCenter(anchor), facing, settings);
    }

    /**
     * {@code true} si chaque bloc du cadre est libre ou remplacable (air, herbe, neige). Ce qui est derriere le plan ne
     * compte pas : un bloc y est cache a qui s'engage dans le cadre.
     *
     * @param world monde lu
     * @param anchor bloc ancre, premier bloc du cadre le long de son axe de hauteur
     * @param facing orientation du cadre
     * @param settings taille du cadre
     * @return {@code true} si le cadre tient
     */
    public static boolean isFree(BlockView world, BlockPos anchor, PortalFacing facing, PortalSettings settings) {
        return allCells(anchor, facing, settings, cell -> world.getBlockState(cell).isReplaceable());
    }

    /**
     * {@code true} si chaque bloc du cadre est libre ou remplacable et sans fluide : on peut y arriver, ni dans un bloc ni
     * dans la lave.
     */
    public static boolean isClear(BlockView world, BlockPos anchor, PortalFacing facing, PortalSettings settings) {
        return allCells(anchor, facing, settings, cell -> {
            BlockState state = world.getBlockState(cell);
            return state.isReplaceable() && state.getFluidState().isEmpty();
        });
    }

    /** Plus petite boite de blocs qui contient tous ceux du cadre. */
    public static BlockBox cells(BlockPos anchor, PortalFacing facing, PortalSettings settings) {
        return BlockBox.create(cell(anchor, facing, firstColumn(settings.width()), 0),
                cell(anchor, facing, lastColumn(settings.width()), settings.height() - 1));
    }

    private static boolean allCells(BlockPos anchor, PortalFacing facing, PortalSettings settings, Predicate<BlockPos> test) {
        for (int column = firstColumn(settings.width()); column <= lastColumn(settings.width()); column++) {
            for (int row = 0; row < settings.height(); row++) {
                if (!test.test(cell(anchor, facing, column, row))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static BlockPos cell(BlockPos anchor, PortalFacing facing, int column, int row) {
        return anchor.add((int) (facing.axisW.x * column + facing.axisH.x * row), (int) (facing.axisW.y * column + facing.axisH.y * row),
                (int) (facing.axisW.z * column + facing.axisH.z * row));
    }
}
