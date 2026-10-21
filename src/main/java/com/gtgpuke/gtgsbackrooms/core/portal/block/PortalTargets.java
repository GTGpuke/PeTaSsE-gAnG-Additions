package com.gtgpuke.gtgsbackrooms.core.portal.block;


import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

import com.gtgpuke.gtgsbackrooms.backrooms.BackroomsConstants;
import com.gtgpuke.gtgsbackrooms.backrooms.level0.LevelZeroArrival;
import com.gtgpuke.gtgsbackrooms.core.portal.entity.PortalFacing;

/**
 * Ciblage automatique d'un bloc portail : ou il mene, et dans quel sens on en sort.
 *
 * <p>Un portail pose hors du Level 0 mene au layer 0 du Level 0, pres des memes X/Z ; pose dans le Level 0, il ramene a
 * la surface du monde normal aux memes X/Z. Par defaut, traverser ne fait pas tourner : la face de sortie regarde a
 * l'oppose de celle d'entree, et on continue dans la meme direction ; les reglages peuvent imposer un point cardinal de
 * sortie, le sol ou le plafond ({@link PortalExit}) et tirer au sort les X/Z de l'arrivee dans un rayon autour de ceux du
 * bloc. A la surface, le cadre d'arrivee se pose sur le haut reel du sol ; dans le Level 0, c'est le Level 0 qui sait ou
 * il tient ({@link LevelZeroArrival}).
 */
public final class PortalTargets {

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
        return resolve(world.getServer(), world.getRegistryKey(), anchor, entryFacing, settings, world.getRandom());
    }

    /**
     * Meme resolution, la dimension du bloc donnee par sa cle : un portail du Level 0 mene a la surface de l'overworld,
     * tout autre portail mene au Level 0.
     *
     * @param server serveur dont on lit les mondes
     * @param dimension dimension du bloc
     * @param anchor position du bloc
     * @param entryFacing orientation de sa face avant
     * @param settings taille du cadre, sens de sortie et rayon de tirage au sort de l'arrivee
     * @param random tirage des X/Z de l'arrivee, quand les reglages en veulent un
     * @return destination, ou {@code null} si la dimension cible est absente ou sans cellule ouverte
     */
    public static @Nullable Target resolve(
            MinecraftServer server,
            RegistryKey<World> dimension,
            BlockPos anchor,
            PortalFacing entryFacing,
            PortalSettings settings,
            Random random) {
        PortalFacing exitFacing = settings.exit().arrivalFacing(entryFacing);
        int x = anchor.getX() + scatter(random, settings.exitScatter());
        int z = anchor.getZ() + scatter(random, settings.exitScatter());
        if (dimension.equals(BackroomsConstants.LEVEL_ZERO_WORLD)) {
            ServerWorld overworld = server.getOverworld();
            // World.getTopY renvoie le fond du monde si le chunk n'est pas charge : on le charge d'abord (une fois, a la pose).
            overworld.getChunk(ChunkSectionPos.getSectionCoord(x), ChunkSectionPos.getSectionCoord(z));
            int surfaceY = overworld.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z);
            Vec3d floor = new Vec3d(x + 0.5, floorHeight(overworld, x, surfaceY, z) + ceilingLift(exitFacing), z + 0.5);
            return new Target(World.OVERWORLD, PortalArea.center(floor, exitFacing, settings), exitFacing);
        }
        ServerWorld levelZero = server.getWorld(BackroomsConstants.LEVEL_ZERO_WORLD);
        return levelZero == null ? null : LevelZeroArrival.find(levelZero, x, z, exitFacing, settings);
    }

    /**
     * Un cadre au plafond pose a la surface est releve de deux blocs : qui en sort pend sous son plan, les pieds encore
     * au-dessus du sol. Rien pour les autres orientations.
     */
    static int ceilingLift(PortalFacing facing) {
        return facing == PortalFacing.DOWN ? PortalArea.CLEARANCE_BEHIND : 0;
    }

    /** Ecart tire au sort dans [-rayon, rayon], en blocs ; nul sans rayon. */
    static int scatter(Random random, int radius) {
        return radius <= 0 ? 0 : random.nextBetween(-radius, radius);
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
}
