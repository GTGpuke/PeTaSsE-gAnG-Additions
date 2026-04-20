package com.petassegang.addons.world.backrooms.level0.write;

import com.petassegang.addons.config.ModConfig;
import com.petassegang.addons.world.backrooms.level0.LevelZeroSurfaceBiome;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroCellConnections;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroCellState;
import com.petassegang.addons.world.backrooms.level0.noise.StageRandom;

/**
 * Resolveur de details surfaciques connectes pour le Level 0.
 *
 * <p>La couche est actuellement conservee comme socle semantique, mais son
 * rendu normal est volontairement gele. Elle sert donc surtout de point
 * d'appui si le projet revient plus tard vers de vrais overlays connectes.
 *
 * <p>Tant que `ENABLE_LEVEL_ZERO_SURFACE_DETAILS` reste a `false`, ce resolver
 * doit se comporter comme un no-op explicite et deterministic, pas comme une
 * couche partiellement active.
 *
 * <p>Autrement dit : ce fichier documente surtout une fondation future, pas
 * une couche visuelle pleinement active aujourd'hui.
 */
public final class LevelZeroSurfaceDetailResolver {

    private static final int PATCH_SCALE = 4;

    /**
     * Resolve le profil de details surfaciques d'une colonne monde.
     *
     * @param cellState etat semantique de la cellule
     * @param worldX coordonnee monde X
     * @param worldZ coordonnee monde Z
     * @return profil de details connectes
     */
    public LevelZeroSurfaceDetailProfile resolve(LevelZeroCellState cellState, int worldX, int worldZ) {
        if (!ModConfig.ENABLE_LEVEL_ZERO_SURFACE_DETAILS) {
            // TODO Level 0: reprendre plus tard la couche d'overlays surfaciques.
            return LevelZeroSurfaceDetailProfile.none();
        }
        LevelZeroSurfaceDetail floorDetail = resolveFloorDetail(cellState, worldX, worldZ);
        LevelZeroSurfaceDetail wallDetail = resolveWallDetail(cellState, worldX, worldZ);
        LevelZeroSurfaceDetail ceilingDetail = resolveCeilingDetail(cellState, worldX, worldZ);
        return new LevelZeroSurfaceDetailProfile(
                floorDetail,
                connectionMask(worldX, worldZ, floorDetail, cellState, Layer.FLOOR),
                wallDetail,
                connectionMask(worldX, worldZ, wallDetail, cellState, Layer.WALL),
                ceilingDetail,
                connectionMask(worldX, worldZ, ceilingDetail, cellState, Layer.CEILING));
    }

    /**
     * Resolve le detail mural a partir des seules coordonnees monde.
     *
     * <p>Cette variante sert au rendu client des overlays, qui doit pouvoir
     * retrouver le detail sans dependre de la grille resolue serveur.
     *
     * @param worldX coordonnee monde X
     * @param worldZ coordonnee monde Z
     * @return detail mural attendu pour cette colonne
     */
    public static LevelZeroSurfaceDetail resolveWallDetailAtWorld(int worldX, int worldZ) {
        if (!ModConfig.ENABLE_LEVEL_ZERO_SURFACE_DETAILS) {
            // TODO Level 0: les overlays muraux sont volontairement geles pour l'instant.
            return LevelZeroSurfaceDetail.NONE;
        }
        return resolveWallDetail(LevelZeroSurfaceBiome.sampleAtWorld(worldX, worldZ), worldX, worldZ);
    }

    private LevelZeroSurfaceDetail resolveFloorDetail(LevelZeroCellState cellState, int worldX, int worldZ) {
        if (!cellState.isLocallyWalkable()) {
            return LevelZeroSurfaceDetail.NONE;
        }
        long hash = patchHash(worldX, worldZ, 0);
        return switch (cellState.surfaceBiome()) {
            case RED -> Math.floorMod(hash, 17) == 0 ? LevelZeroSurfaceDetail.FLOOR_WEAR : LevelZeroSurfaceDetail.NONE;
            default -> Math.floorMod(hash, 19) == 0 ? LevelZeroSurfaceDetail.FLOOR_STAIN : LevelZeroSurfaceDetail.NONE;
        };
    }

    private LevelZeroSurfaceDetail resolveWallDetail(LevelZeroCellState cellState, int worldX, int worldZ) {
        if (cellState.isLocallyWalkable()) {
            return LevelZeroSurfaceDetail.NONE;
        }
        return resolveWallDetail(cellState.surfaceBiome(), worldX, worldZ);
    }

    private LevelZeroSurfaceDetail resolveCeilingDetail(LevelZeroCellState cellState, int worldX, int worldZ) {
        if (!cellState.walkable()) {
            return LevelZeroSurfaceDetail.NONE;
        }
        long hash = patchHash(worldX, worldZ, 2);
        return Math.floorMod(hash, cellState.surfaceBiome() == LevelZeroSurfaceBiome.RED ? 23 : 29) == 0
                ? LevelZeroSurfaceDetail.CEILING_STAIN
                : LevelZeroSurfaceDetail.NONE;
    }

    private int connectionMask(int worldX,
                               int worldZ,
                               LevelZeroSurfaceDetail detail,
                               LevelZeroCellState cellState,
                               Layer layer) {
        if (detail == LevelZeroSurfaceDetail.NONE) {
            return 0;
        }
        int mask = 0;
        if (detail == sampleLayerDetail(worldX, worldZ - 1, cellState, layer)) {
            mask |= LevelZeroCellConnections.NORTH;
        }
        if (detail == sampleLayerDetail(worldX + 1, worldZ, cellState, layer)) {
            mask |= LevelZeroCellConnections.EAST;
        }
        if (detail == sampleLayerDetail(worldX, worldZ + 1, cellState, layer)) {
            mask |= LevelZeroCellConnections.SOUTH;
        }
        if (detail == sampleLayerDetail(worldX - 1, worldZ, cellState, layer)) {
            mask |= LevelZeroCellConnections.WEST;
        }
        return mask;
    }

    private LevelZeroSurfaceDetail sampleLayerDetail(int worldX,
                                                     int worldZ,
                                                     LevelZeroCellState cellState,
                                                     Layer layer) {
        return switch (layer) {
            case FLOOR -> resolveFloorDetail(cellState, worldX, worldZ);
            case WALL -> resolveWallDetail(cellState, worldX, worldZ);
            case CEILING -> resolveCeilingDetail(cellState, worldX, worldZ);
        };
    }

    private static long patchHash(int worldX, int worldZ, int saltOffset) {
        // Les patches restent calcules a une echelle plus large que le bloc
        // seul pour permettre, plus tard, de vrais motifs connectes et des
        // taches multi-blocs sans changer le contrat de cette couche.
        return StageRandom.mixLegacy(
                0L,
                StageRandom.Stage.SURFACE_DETAILS,
                Math.floorDiv(worldX, PATCH_SCALE) + saltOffset * 131L,
                Math.floorDiv(worldZ, PATCH_SCALE) - saltOffset * 197L);
    }

    private static LevelZeroSurfaceDetail resolveWallDetail(LevelZeroSurfaceBiome biome, int worldX, int worldZ) {
        long hash = patchHash(worldX, worldZ, 1);
        return switch (biome) {
            case RED -> Math.floorMod(hash, 15) == 0 ? LevelZeroSurfaceDetail.WALL_DIRT : LevelZeroSurfaceDetail.NONE;
            default -> Math.floorMod(hash, 13) == 0 ? LevelZeroSurfaceDetail.WALL_DAMP : LevelZeroSurfaceDetail.NONE;
        };
    }

    private enum Layer {
        FLOOR,
        WALL,
        CEILING
    }
}
