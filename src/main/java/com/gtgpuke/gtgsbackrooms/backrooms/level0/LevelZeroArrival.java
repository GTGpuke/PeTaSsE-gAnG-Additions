package com.gtgpuke.gtgsbackrooms.backrooms.level0;

import java.util.HashMap;
import java.util.Map;
import java.util.function.IntBinaryOperator;

import org.jetbrains.annotations.Nullable;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;

import com.gtgpuke.gtgsbackrooms.backrooms.BackroomsConstants;
import com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.coord.LevelZeroCoords;
import com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.coord.LevelZeroLayerStackLayout;
import com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.layout.LevelZeroLayout;
import com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.noise.LevelZeroSeedResolver;
import com.gtgpuke.gtgsbackrooms.core.portal.block.PortalArea;
import com.gtgpuke.gtgsbackrooms.core.portal.block.PortalExit;
import com.gtgpuke.gtgsbackrooms.core.portal.block.PortalSettings;
import com.gtgpuke.gtgsbackrooms.core.portal.block.PortalTargets.Target;
import com.gtgpuke.gtgsbackrooms.core.portal.entity.PortalFacing;

/**
 * Arrivee d'un portail dans le Level 0 : ou poser son cadre d'arrivee, pres des X/Z de l'entree, dans le labyrinthe du
 * layer 0 reellement genere. Dans la cellule ouverte la plus proche ou le cadre tient sur toute sa largeur avec un bloc
 * libre devant et derriere chaque colonne (le portail se traverse par ses deux faces) ; couche, la ou toute son empreinte
 * est libre ; faute de mieux, au centre de la cellule ouverte la plus proche, face a un bloc libre. Le Level 0 connait sa
 * geometrie : le systeme de portails ({@link com.gtgpuke.gtgsbackrooms.core.portal.block.PortalTargets}) lui demande ou
 * arriver.
 */
public final class LevelZeroArrival {

    /** Rayon de recherche, en cellules, d'une cellule ouverte autour des X/Z du portail. */
    private static final int MAX_SEARCH_RADIUS_CELLS = 24;

    private static final PortalFacing[] HORIZONTAL_FACINGS = {
            PortalFacing.NORTH, PortalFacing.EAST, PortalFacing.SOUTH, PortalFacing.WEST,
    };

    private LevelZeroArrival() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /**
     * Arrivee dans le layer 0 de ce monde du Level 0, pres des X/Z donnes : le labyrinthe est celui de sa seed.
     *
     * @param levelZero monde du Level 0
     * @param worldX coordonnee monde X du portail
     * @param worldZ coordonnee monde Z du portail
     * @param preferredFacing orientation de sortie preferee
     * @param settings taille du cadre et sens de sortie
     * @return destination, ou {@code null} si aucune cellule ouverte dans le rayon de recherche
     */
    public static @Nullable Target find(ServerWorld levelZero, int worldX, int worldZ, PortalFacing preferredFacing, PortalSettings settings) {
        long layoutSeed = LevelZeroSeedResolver.resolveLayoutSeed(levelZero.getChunkManager().getNoiseConfig());
        return find(LevelZeroSeedResolver.resolveLayerSeed(layoutSeed, 0), worldX, worldZ, preferredFacing, settings);
    }

    /**
     * Arrivee dans le layer 0 du Level 0, pres des X/Z donnes, pour une seed de layer donnee.
     *
     * @param layerSeed seed du layer 0
     * @param worldX coordonnee monde X du portail
     * @param worldZ coordonnee monde Z du portail
     * @param preferredFacing orientation de sortie preferee
     * @param settings taille du cadre
     * @return destination, ou {@code null} si aucune cellule ouverte dans le rayon de recherche
     */
    public static @Nullable Target find(long layerSeed, int worldX, int worldZ, PortalFacing preferredFacing, PortalSettings settings) {
        return search(new LayoutCache(layerSeed), worldX, worldZ, preferredFacing, settings);
    }

    /**
     * Meme recherche sur un labyrinthe donne : {@code (x, z) -> 1} si la colonne de blocs est libre.
     *
     * @return destination, ou {@code null} si aucune cellule ouverte dans le rayon de recherche
     */
    static @Nullable Target search(
            IntBinaryOperator walkable,
            int worldX,
            int worldZ,
            PortalFacing preferredFacing,
            PortalSettings settings) {
        int cellX = LevelZeroCoords.worldToCell(worldX);
        int cellZ = LevelZeroCoords.worldToCell(worldZ);
        // Un sens de sortie impose (point cardinal, sol, plafond) ne se tourne pas : le cadre se pose la ou il tient tel quel.
        boolean imposed = settings.exit() != PortalExit.STRAIGHT;
        long fallback = Long.MIN_VALUE;
        for (int radius = 0; radius <= MAX_SEARCH_RADIUS_CELLS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }
                    int x = LevelZeroCoords.cellCenterWorld(cellX + dx);
                    int z = LevelZeroCoords.cellCenterWorld(cellZ + dz);
                    if (walkable.applyAsInt(x, z) == 0) {
                        continue;
                    }
                    PortalFacing facing;
                    if (preferredFacing.direction.getAxis().isVertical()) {
                        facing = fitsFlat(walkable, x, z, preferredFacing, settings) ? preferredFacing : null;
                    } else if (imposed) {
                        facing = fits(walkable, x, z, preferredFacing, settings.width()) ? preferredFacing : null;
                    } else {
                        facing = fittingFacing(walkable, x, z, preferredFacing, settings.width());
                    }
                    if (facing != null) {
                        return target(x, z, facing, settings);
                    }
                    if (fallback == Long.MIN_VALUE) {
                        fallback = ((long) x << 32) | (z & 0xFFFFFFFFL);
                    }
                }
            }
        }
        if (fallback == Long.MIN_VALUE) {
            return null;
        }
        int x = (int) (fallback >> 32);
        int z = (int) fallback;
        return target(x, z, imposed ? preferredFacing : exitFacing(walkable, x, z, preferredFacing), settings);
    }

    /** Cadre debout ou au sol : ancre au sol du layer 0 ; cadre au plafond : ancre dans la derniere rangee d'air, sous le plafond. */
    private static Target target(int x, int z, PortalFacing facing, PortalSettings settings) {
        int anchorY = facing == PortalFacing.DOWN ? LevelZeroLayerStackLayout.airMaxY(0) : LevelZeroLayerStackLayout.airMinY(0);
        Vec3d floor = new Vec3d(x + 0.5, anchorY, z + 0.5);
        return new Target(BackroomsConstants.LEVEL_ZERO_WORLD, PortalArea.center(floor, facing, settings), facing);
    }

    /** {@code true} si chaque bloc de l'empreinte d'un cadre couche (largeur le long de X, hauteur le long de Z) est libre. */
    static boolean fitsFlat(IntBinaryOperator walkable, int x, int z, PortalFacing facing, PortalSettings settings) {
        for (int column = PortalArea.firstColumn(settings.width()); column <= PortalArea.lastColumn(settings.width()); column++) {
            for (int row = 0; row < settings.height(); row++) {
                int cellX = x + (int) (facing.axisW.x * column + facing.axisH.x * row);
                int cellZ = z + (int) (facing.axisW.z * column + facing.axisH.z * row);
                if (walkable.applyAsInt(cellX, cellZ) == 0) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Orientation ou le cadre tient, la preferee d'abord ; {@code null} si aucune. */
    static @Nullable PortalFacing fittingFacing(IntBinaryOperator walkable, int x, int z, PortalFacing preferred, int width) {
        if (fits(walkable, x, z, preferred, width)) {
            return preferred;
        }
        for (PortalFacing facing : HORIZONTAL_FACINGS) {
            if (facing != preferred && fits(walkable, x, z, facing, width)) {
                return facing;
            }
        }
        return null;
    }

    /** {@code true} si chaque colonne du cadre est libre, ainsi que le bloc devant et le bloc derriere elle. */
    static boolean fits(IntBinaryOperator walkable, int x, int z, PortalFacing facing, int width) {
        int stepX = (int) facing.axisW.x;
        int stepZ = (int) facing.axisW.z;
        int normalX = facing.direction.getOffsetX();
        int normalZ = facing.direction.getOffsetZ();
        for (int column = PortalArea.firstColumn(width); column <= PortalArea.lastColumn(width); column++) {
            int columnX = x + stepX * column;
            int columnZ = z + stepZ * column;
            if (walkable.applyAsInt(columnX, columnZ) == 0
                    || walkable.applyAsInt(columnX + normalX, columnZ + normalZ) == 0
                    || walkable.applyAsInt(columnX - normalX, columnZ - normalZ) == 0) {
                return false;
            }
        }
        return true;
    }

    /** Repli : la preferee si le bloc devant elle est libre, sinon la premiere orientation horizontale qui l'est, sinon la preferee. */
    static PortalFacing exitFacing(IntBinaryOperator walkable, int x, int z, PortalFacing preferred) {
        if (isOpenTowards(walkable, x, z, preferred)) {
            return preferred;
        }
        for (PortalFacing facing : HORIZONTAL_FACINGS) {
            if (isOpenTowards(walkable, x, z, facing)) {
                return facing;
            }
        }
        return preferred;
    }

    private static boolean isOpenTowards(IntBinaryOperator walkable, int x, int z, PortalFacing facing) {
        return walkable.applyAsInt(x + facing.direction.getOffsetX(), z + facing.direction.getOffsetZ()) != 0;
    }

    /**
     * Layouts de chunks du layer 0 generes a la demande pendant une recherche : la verite du monde (zone sure de
     * l'origine, micro-geometrie des couloirs) plutot que la traversabilite brute des cellules.
     */
    static final class LayoutCache implements IntBinaryOperator {
        private final long layerSeed;
        private final Map<Long, LevelZeroLayout> layouts = new HashMap<>();

        LayoutCache(long layerSeed) {
            this.layerSeed = layerSeed;
        }

        @Override
        public int applyAsInt(int worldX, int worldZ) {
            return isWalkable(worldX, worldZ) ? 1 : 0;
        }

        /** {@code true} si la colonne de blocs est libre dans le labyrinthe genere. */
        boolean isWalkable(int worldX, int worldZ) {
            int chunkX = ChunkSectionPos.getSectionCoord(worldX);
            int chunkZ = ChunkSectionPos.getSectionCoord(worldZ);
            LevelZeroLayout layout = this.layouts.computeIfAbsent(ChunkPos.toLong(chunkX, chunkZ),
                    key -> LevelZeroLayout.generate(chunkX, chunkZ, this.layerSeed, 0));
            return layout.isLocallyWalkable(LevelZeroCoords.worldToLocal(worldX), LevelZeroCoords.worldToLocal(worldZ));
        }
    }
}
