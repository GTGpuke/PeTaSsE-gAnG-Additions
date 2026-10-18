package com.gtgpuke.gtgsbackrooms.backrooms.portal.block;

import java.util.HashMap;
import java.util.Map;
import java.util.function.IntBinaryOperator;

import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

import com.gtgpuke.gtgsbackrooms.backrooms.BackroomsConstants;
import com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.coord.LevelZeroCoords;
import com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.coord.LevelZeroLayerStackLayout;
import com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.layout.LevelZeroLayout;
import com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.noise.LevelZeroSeedResolver;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.entity.PortalFacing;

/**
 * Ciblage automatique d'un bloc portail : ou il mene, et dans quel sens on en sort.
 *
 * <p>Un portail pose hors du Level 0 mene au layer 0 du Level 0, pres des memes X/Z ; pose dans le Level 0, il ramene a
 * la surface du monde normal aux memes X/Z. Traverser ne fait pas tourner : la face de sortie regarde a l'oppose de celle
 * d'entree, et on continue dans la meme direction. Dans le Level 0, le cadre d'arrivee se pose dans la cellule ouverte la
 * plus proche ou il tient sur toute sa largeur avec un bloc libre devant et derriere chaque colonne (le portail se
 * traverse par ses deux faces) ; faute de mieux, au centre de la cellule ouverte la plus proche, face a un bloc libre.
 */
public final class PortalTargets {

    /** Rayon de recherche, en cellules, d'une cellule ouverte autour des X/Z du portail. */
    private static final int MAX_SEARCH_RADIUS_CELLS = 24;

    private static final PortalFacing[] HORIZONTAL_FACINGS = {
            PortalFacing.NORTH, PortalFacing.EAST, PortalFacing.SOUTH, PortalFacing.WEST,
    };

    /**
     * Destination d'un portail.
     *
     * @param dimension dimension d'arrivee
     * @param position centre du cadre d'arrivee
     * @param facing orientation du cadre d'arrivee, normale vers la sortie de sa face avant
     */
    public record Target(RegistryKey<World> dimension, Vec3d position, PortalFacing facing) {
    }

    private PortalTargets() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /**
     * Resout la destination d'un bloc portail pose dans un monde serveur.
     *
     * @param world monde du bloc
     * @param anchor position du bloc
     * @param entryFacing orientation de sa face avant, normale vers le joueur qui l'a pose
     * @param settings taille du cadre
     * @return destination, ou {@code null} si la dimension cible est absente ou sans cellule ouverte
     */
    public static @Nullable Target resolve(ServerWorld world, BlockPos anchor, PortalFacing entryFacing, PortalSettings settings) {
        return resolve(world.getServer(), world.getRegistryKey(), anchor, entryFacing, settings);
    }

    /**
     * Meme resolution, la dimension du bloc donnee par sa cle : un portail du Level 0 mene a la surface de l'overworld,
     * tout autre portail mene au Level 0.
     *
     * @param server serveur dont on lit les mondes
     * @param dimension dimension du bloc
     * @param anchor position du bloc
     * @param entryFacing orientation de sa face avant
     * @param settings taille du cadre
     * @return destination, ou {@code null} si la dimension cible est absente ou sans cellule ouverte
     */
    public static @Nullable Target resolve(
            MinecraftServer server,
            RegistryKey<World> dimension,
            BlockPos anchor,
            PortalFacing entryFacing,
            PortalSettings settings) {
        PortalFacing exitFacing = entryFacing.opposite();
        if (dimension.equals(BackroomsConstants.LEVEL_ZERO_WORLD)) {
            ServerWorld overworld = server.getOverworld();
            // World.getTopY renvoie le fond du monde si le chunk n'est pas charge : on le charge d'abord (une fois, a la pose).
            overworld.getChunk(ChunkSectionPos.getSectionCoord(anchor.getX()), ChunkSectionPos.getSectionCoord(anchor.getZ()));
            int surfaceY = overworld.getTopY(Heightmap.Type.MOTION_BLOCKING, anchor.getX(), anchor.getZ());
            Vec3d floor = new Vec3d(anchor.getX() + 0.5, floorHeight(overworld, anchor.getX(), surfaceY, anchor.getZ()), anchor.getZ() + 0.5);
            return new Target(World.OVERWORLD, PortalArea.center(floor, exitFacing, settings), exitFacing);
        }
        ServerWorld levelZero = server.getWorld(BackroomsConstants.LEVEL_ZERO_WORLD);
        if (levelZero == null) {
            return null;
        }
        long layoutSeed = LevelZeroSeedResolver.resolveLayoutSeed(levelZero.getChunkManager().getNoiseConfig());
        return levelZeroTarget(LevelZeroSeedResolver.resolveLayerSeed(layoutSeed, 0), anchor.getX(), anchor.getZ(), exitFacing, settings);
    }

    /**
     * Hauteur exacte du sol d'une colonne : le haut de la collision du bloc qu'on y foule. Une dalle, un chemin ou une
     * couche de neige ne font pas un bloc plein : le cadre d'arrivee se pose dessus, au demi-bloc pres, au lieu de
     * flotter au-dessus, et celui qui le traverse depuis un portail pose sur un bloc plein garde les pieds au sol.
     *
     * @param world monde lu
     * @param x coordonnee monde X de la colonne
     * @param surfaceY premier Y libre au-dessus des blocs qui bloquent le mouvement (heightmap {@code MOTION_BLOCKING})
     * @param z coordonnee monde Z de la colonne
     * @return hauteur du sol, en blocs
     */
    public static double floorHeight(BlockView world, int x, int surfaceY, int z) {
        BlockPos.Mutable pos = new BlockPos.Mutable(x, surfaceY, z);
        // Un tapis ou une couche de neige peuvent ne pas compter dans la heightmap, mais on marche dessus.
        VoxelShape above = world.getBlockState(pos).getCollisionShape(world, pos);
        if (!above.isEmpty()) {
            return surfaceY + above.getMax(Direction.Axis.Y);
        }
        pos.setY(surfaceY - 1);
        VoxelShape ground = world.getBlockState(pos).getCollisionShape(world, pos);
        return ground.isEmpty() ? surfaceY : surfaceY - 1 + ground.getMax(Direction.Axis.Y);
    }

    /**
     * Destination dans le layer 0 du Level 0, pres des X/Z donnes.
     *
     * @param layerSeed seed du layer 0
     * @param worldX coordonnee monde X du portail
     * @param worldZ coordonnee monde Z du portail
     * @param preferredFacing orientation de sortie preferee
     * @param settings taille du cadre
     * @return destination, ou {@code null} si aucune cellule ouverte dans le rayon de recherche
     */
    public static @Nullable Target levelZeroTarget(long layerSeed, int worldX, int worldZ, PortalFacing preferredFacing, PortalSettings settings) {
        return levelZeroTarget(new LayoutCache(layerSeed), worldX, worldZ, preferredFacing, settings);
    }

    /**
     * Meme recherche sur un labyrinthe donne : {@code (x, z) -> 1} si la colonne de blocs est libre.
     *
     * @return destination, ou {@code null} si aucune cellule ouverte dans le rayon de recherche
     */
    static @Nullable Target levelZeroTarget(
            IntBinaryOperator walkable,
            int worldX,
            int worldZ,
            PortalFacing preferredFacing,
            PortalSettings settings) {
        int cellX = LevelZeroCoords.worldToCell(worldX);
        int cellZ = LevelZeroCoords.worldToCell(worldZ);
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
                    PortalFacing facing = fittingFacing(walkable, x, z, preferredFacing, settings.width());
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
        return target(x, z, exitFacing(walkable, x, z, preferredFacing), settings);
    }

    private static Target target(int x, int z, PortalFacing facing, PortalSettings settings) {
        Vec3d floor = new Vec3d(x + 0.5, LevelZeroLayerStackLayout.airMinY(0), z + 0.5);
        return new Target(BackroomsConstants.LEVEL_ZERO_WORLD, PortalArea.center(floor, facing, settings), facing);
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
