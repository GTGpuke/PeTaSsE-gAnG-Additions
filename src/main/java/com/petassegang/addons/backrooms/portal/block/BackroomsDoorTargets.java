package com.petassegang.addons.backrooms.portal.block;

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
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

import com.petassegang.addons.backrooms.BackroomsConstants;
import com.petassegang.addons.backrooms.level0.generation.coord.LevelZeroCoords;
import com.petassegang.addons.backrooms.level0.generation.coord.LevelZeroLayerStackLayout;
import com.petassegang.addons.backrooms.level0.generation.layout.LevelZeroLayout;
import com.petassegang.addons.backrooms.level0.generation.noise.LevelZeroSeedResolver;
import com.petassegang.addons.backrooms.portal.entity.PortalFacing;

/**
 * Ciblage automatique d'une porte Backrooms : ou elle mene, et dans quel sens on en sort.
 *
 * <p>Une porte posee hors du Level 0 mene au layer 0 du Level 0, dans la cellule ouverte la plus
 * proche des memes X/Z ; une porte posee dans le Level 0 ramene a la surface du monde normal aux
 * memes X/Z. Le joueur arrive toujours sur un bloc reellement libre du labyrinthe genere et sort
 * vers une cellule ouverte, jamais face a un mur.
 */
public final class BackroomsDoorTargets {

    /** Rayon de recherche, en cellules, d'une cellule ouverte autour des X/Z de la porte. */
    public static final int MAX_SEARCH_RADIUS_CELLS = 24;
    /** Hauteur du centre d'un portail de porte au-dessus du sol : moitie de sa hauteur (2 blocs). */
    static final double PORTAL_CENTER_ABOVE_FLOOR = 1.0;

    private static final PortalFacing[] HORIZONTAL_FACINGS = {
            PortalFacing.NORTH, PortalFacing.EAST, PortalFacing.SOUTH, PortalFacing.WEST,
    };

    /**
     * Destination d'une porte.
     *
     * @param dimension dimension d'arrivee
     * @param position centre du portail d'arrivee
     * @param facing orientation du portail d'arrivee, normale vers la sortie
     */
    public record Target(RegistryKey<World> dimension, Vec3d position, PortalFacing facing) {
    }

    private BackroomsDoorTargets() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /**
     * Resout la destination d'une porte posee dans un monde serveur.
     *
     * @param world monde ou la porte est posee
     * @param doorPos position de la porte
     * @param doorFacing orientation de la porte
     * @return destination, ou {@code null} si la dimension cible est absente ou sans cellule ouverte
     */
    public static @Nullable Target resolve(ServerWorld world, BlockPos doorPos, Direction doorFacing) {
        return resolve(world.getServer(), world.getRegistryKey(), doorPos, doorFacing);
    }

    /**
     * Meme resolution, la dimension de la porte donnee par sa cle : une porte du Level 0 mene a la surface de l'overworld,
     * toute autre porte mene au Level 0.
     *
     * @param server serveur dont on lit les mondes
     * @param doorDimension dimension de la porte
     * @param doorPos position de la porte
     * @param doorFacing orientation de la porte
     * @return destination, ou {@code null} si la dimension cible est absente ou sans cellule ouverte
     */
    public static @Nullable Target resolve(MinecraftServer server, RegistryKey<World> doorDimension, BlockPos doorPos, Direction doorFacing) {
        PortalFacing facing = PortalFacing.fromDirection(doorFacing);
        if (doorDimension.equals(BackroomsConstants.LEVEL_ZERO_WORLD)) {
            ServerWorld overworld = server.getOverworld();
            // World.getTopY renvoie le fond du monde si le chunk n'est pas charge : on le charge d'abord (une fois, a la pose).
            overworld.getChunk(ChunkSectionPos.getSectionCoord(doorPos.getX()), ChunkSectionPos.getSectionCoord(doorPos.getZ()));
            int surfaceY = overworld.getTopY(Heightmap.Type.MOTION_BLOCKING, doorPos.getX(), doorPos.getZ());
            Vec3d center = new Vec3d(doorPos.getX() + 0.5, surfaceY + PORTAL_CENTER_ABOVE_FLOOR, doorPos.getZ() + 0.5);
            return new Target(World.OVERWORLD, center, facing);
        }
        ServerWorld levelZero = server.getWorld(BackroomsConstants.LEVEL_ZERO_WORLD);
        if (levelZero == null) {
            return null;
        }
        long layoutSeed = LevelZeroSeedResolver.resolveLayoutSeed(levelZero.getChunkManager().getNoiseConfig());
        return levelZeroTarget(LevelZeroSeedResolver.resolveLayerSeed(layoutSeed, 0), doorPos.getX(), doorPos.getZ(), facing);
    }

    /**
     * Destination dans le layer 0 du Level 0 : centre de la cellule ouverte la plus proche des X/Z donnes.
     *
     * @param layerSeed seed du layer 0
     * @param worldX coordonnee monde X de la porte
     * @param worldZ coordonnee monde Z de la porte
     * @param preferredFacing orientation de sortie preferee (celle de la porte)
     * @return destination, ou {@code null} si aucune cellule ouverte dans le rayon de recherche
     */
    public static @Nullable Target levelZeroTarget(long layerSeed, int worldX, int worldZ, PortalFacing preferredFacing) {
        return levelZeroTarget(new LayoutCache(layerSeed), worldX, worldZ, preferredFacing);
    }

    /**
     * Meme recherche sur une ouverture de cellules donnee : {@code (cellX, cellZ) -> 1} si le centre de la cellule est libre.
     *
     * @return destination, ou {@code null} si aucune cellule ouverte dans le rayon de recherche
     */
    static @Nullable Target levelZeroTarget(IntBinaryOperator open, int worldX, int worldZ, PortalFacing preferredFacing) {
        long cell = nearestOpenCell(open, LevelZeroCoords.worldToCell(worldX), LevelZeroCoords.worldToCell(worldZ));
        if (cell == Long.MIN_VALUE) {
            return null;
        }
        int cellX = (int) (cell >> 32);
        int cellZ = (int) cell;
        Vec3d center = new Vec3d(
                LevelZeroCoords.cellCenterWorld(cellX) + 0.5,
                LevelZeroLayerStackLayout.airMinY(0) + PORTAL_CENTER_ABOVE_FLOOR,
                LevelZeroCoords.cellCenterWorld(cellZ) + 0.5);
        return new Target(BackroomsConstants.LEVEL_ZERO_WORLD, center, exitFacing(open, cellX, cellZ, preferredFacing));
    }

    /**
     * Cellule dont le bloc central est libre, la plus proche en distance de Chebyshev.
     *
     * @return cellule encodee ({@code x << 32 | z}), ou {@link Long#MIN_VALUE} si aucune dans le rayon
     */
    static long nearestOpenCell(IntBinaryOperator open, int cellX, int cellZ) {
        for (int radius = 0; radius <= MAX_SEARCH_RADIUS_CELLS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }
                    if (open.applyAsInt(cellX + dx, cellZ + dz) != 0) {
                        return ((long) (cellX + dx) << 32) | ((cellZ + dz) & 0xFFFFFFFFL);
                    }
                }
            }
        }
        return Long.MIN_VALUE;
    }

    /**
     * Orientation de sortie : la preferee si la cellule voisine est ouverte, sinon la premiere
     * orientation horizontale ouverte, sinon la preferee.
     */
    static PortalFacing exitFacing(IntBinaryOperator open, int cellX, int cellZ, PortalFacing preferred) {
        if (isOpenTowards(open, cellX, cellZ, preferred)) {
            return preferred;
        }
        for (PortalFacing facing : HORIZONTAL_FACINGS) {
            if (isOpenTowards(open, cellX, cellZ, facing)) {
                return facing;
            }
        }
        return preferred;
    }

    private static boolean isOpenTowards(IntBinaryOperator open, int cellX, int cellZ, PortalFacing facing) {
        Direction direction = facing.direction;
        return direction.getAxis().isHorizontal()
                && open.applyAsInt(cellX + direction.getOffsetX(), cellZ + direction.getOffsetZ()) != 0;
    }

    /**
     * Layouts de chunks du layer 0 generes a la demande pendant une recherche : la verite du monde
     * (zone sure de l'origine, micro-geometrie des couloirs) plutot que la traversabilite brute.
     */
    static final class LayoutCache implements IntBinaryOperator {
        private final long layerSeed;
        private final Map<Long, LevelZeroLayout> layouts = new HashMap<>();

        LayoutCache(long layerSeed) {
            this.layerSeed = layerSeed;
        }

        @Override
        public int applyAsInt(int cellX, int cellZ) {
            return isCellCenterOpen(cellX, cellZ) ? 1 : 0;
        }

        /** {@code true} si le bloc central de la cellule est libre dans le labyrinthe genere. */
        boolean isCellCenterOpen(int cellX, int cellZ) {
            int worldX = LevelZeroCoords.cellCenterWorld(cellX);
            int worldZ = LevelZeroCoords.cellCenterWorld(cellZ);
            int chunkX = ChunkSectionPos.getSectionCoord(worldX);
            int chunkZ = ChunkSectionPos.getSectionCoord(worldZ);
            LevelZeroLayout layout = this.layouts.computeIfAbsent(ChunkPos.toLong(chunkX, chunkZ),
                    key -> LevelZeroLayout.generate(chunkX, chunkZ, this.layerSeed, 0));
            return layout.isLocallyWalkable(LevelZeroCoords.worldToLocal(worldX), LevelZeroCoords.worldToLocal(worldZ));
        }
    }
}
