package com.petassegang.addons.backrooms.level0.generation.write;

import com.petassegang.addons.backrooms.level0.biome.LevelZeroSurfaceBiome;
import com.petassegang.addons.backrooms.level0.block.LevelZeroWallpaperBlock;
import com.petassegang.addons.backrooms.level0.generation.coord.LevelZeroCoords;
import com.petassegang.addons.backrooms.level0.generation.layout.LevelZeroLayout;
import com.petassegang.addons.backrooms.level0.generation.layout.LevelZeroRegionWalkability;

/**
 * Lit, pour une colonne de mur, quelles faces sont visibles et quelle variante de
 * papier peint chacune doit afficher.
 *
 * <p>Les lectures de traversabilite passent par la fenetre du layout quand
 * elle couvre la cellule, sinon par le cache de secteurs. Le choix de variante
 * depend du biome de la premiere colonne d'air en face du mur ; comme les
 * biomes sont tires par regions de {@value LevelZeroSurfaceBiome#REGION_SIZE_CELLS}
 * cellules, la sonde est court-circuitee quand aucune frontiere de region n'est
 * a portee, ce qui est le cas de la quasi-totalite des murs.
 */
public final class LevelZeroWallSampler {

    /** Portee de la sonde en cellules de part et d'autre du mur. */
    private static final int PROBE_REACH_CELLS =
            LevelZeroWallpaperBlock.MAX_SURFACE_PROBE_DISTANCE / LevelZeroCoords.CELL_SCALE;

    private final long layoutSeed;
    private final int layerIndex;
    private final LevelZeroRegionWalkability region;

    /**
     * Construit un sampler pour un layout de chunk.
     *
     * @param layout layout dont on reutilise la fenetre de traversabilite
     */
    public LevelZeroWallSampler(LevelZeroLayout layout) {
        this(layout.layoutSeed(), layout.layerIndex(), layout.walkability());
    }

    /**
     * Construit un sampler explicite.
     *
     * @param layoutSeed seed du layer
     * @param layerIndex index du layer
     * @param region fenetre de traversabilite deja calculee, ou {@code null} pour tout lire dans le cache
     */
    public LevelZeroWallSampler(long layoutSeed, int layerIndex, LevelZeroRegionWalkability region) {
        this.layoutSeed = layoutSeed;
        this.layerIndex = layerIndex;
        this.region = region;
    }

    /**
     * Masque des faces horizontales d'une colonne donnant sur une cellule ouverte.
     *
     * @param worldX coordonnee monde X
     * @param worldZ coordonnee monde Z
     * @return bits de {@link LevelZeroWallpaperBlock} ; 0 si la colonne est enfouie
     */
    public int exposedFaceMask(int worldX, int worldZ) {
        int mask = 0;
        if (isWalkableAt(worldX, worldZ - 1)) {
            mask |= LevelZeroWallpaperBlock.NORTH_MASK;
        }
        if (isWalkableAt(worldX, worldZ + 1)) {
            mask |= LevelZeroWallpaperBlock.SOUTH_MASK;
        }
        if (isWalkableAt(worldX - 1, worldZ)) {
            mask |= LevelZeroWallpaperBlock.WEST_MASK;
        }
        if (isWalkableAt(worldX + 1, worldZ)) {
            mask |= LevelZeroWallpaperBlock.EAST_MASK;
        }
        return mask;
    }

    /**
     * Masque des faces qui doivent afficher la variante blanche du papier peint.
     *
     * @param worldX coordonnee monde X
     * @param worldZ coordonnee monde Z
     * @return bits de {@link LevelZeroWallpaperBlock}
     */
    public int wallpaperFaceMask(int worldX, int worldZ) {
        int cellX = LevelZeroCoords.worldToCell(worldX);
        int cellZ = LevelZeroCoords.worldToCell(worldZ);
        boolean uniformAlongX = sameBiomeRegion(cellX - PROBE_REACH_CELLS, cellX + PROBE_REACH_CELLS);
        boolean uniformAlongZ = sameBiomeRegion(cellZ - PROBE_REACH_CELLS, cellZ + PROBE_REACH_CELLS);
        int ownBits = LevelZeroSurfaceBiome.sampleAtCell(cellX, cellZ, layerIndex) == LevelZeroSurfaceBiome.RED
                ? LevelZeroWallpaperBlock.FULL_MASK
                : 0;
        if (uniformAlongX && uniformAlongZ) {
            return ownBits;
        }
        int mask = 0;
        if (uniformAlongZ) {
            mask |= ownBits & (LevelZeroWallpaperBlock.NORTH_MASK | LevelZeroWallpaperBlock.SOUTH_MASK);
        } else {
            mask |= probeFace(worldX, worldZ, 0, -1, LevelZeroWallpaperBlock.NORTH_MASK);
            mask |= probeFace(worldX, worldZ, 0, 1, LevelZeroWallpaperBlock.SOUTH_MASK);
        }
        if (uniformAlongX) {
            mask |= ownBits & (LevelZeroWallpaperBlock.WEST_MASK | LevelZeroWallpaperBlock.EAST_MASK);
        } else {
            mask |= probeFace(worldX, worldZ, -1, 0, LevelZeroWallpaperBlock.WEST_MASK);
            mask |= probeFace(worldX, worldZ, 1, 0, LevelZeroWallpaperBlock.EAST_MASK);
        }
        return mask;
    }

    private int probeFace(int worldX, int worldZ, int stepX, int stepZ, int maskBit) {
        for (int distance = 1; distance <= LevelZeroWallpaperBlock.MAX_SURFACE_PROBE_DISTANCE; distance++) {
            int sampleX = worldX + stepX * distance;
            int sampleZ = worldZ + stepZ * distance;
            if (isWalkableAt(sampleX, sampleZ)) {
                return isRedAt(sampleX, sampleZ) ? maskBit : 0;
            }
        }
        int fallbackX = worldX + stepX * LevelZeroWallpaperBlock.FALLBACK_BIOME_SAMPLE_DISTANCE;
        int fallbackZ = worldZ + stepZ * LevelZeroWallpaperBlock.FALLBACK_BIOME_SAMPLE_DISTANCE;
        return isRedAt(fallbackX, fallbackZ) ? maskBit : 0;
    }

    private boolean isRedAt(int worldX, int worldZ) {
        return LevelZeroSurfaceBiome.sampleAtWorld(worldX, worldZ, layerIndex) == LevelZeroSurfaceBiome.RED;
    }

    private boolean isWalkableAt(int worldX, int worldZ) {
        int cellX = LevelZeroCoords.worldToCell(worldX);
        int cellZ = LevelZeroCoords.worldToCell(worldZ);
        if (region != null && region.contains(cellX, cellZ)) {
            return region.isWalkable(cellX, cellZ);
        }
        return LevelZeroLayout.isWalkableCell(layoutSeed, cellX, cellZ);
    }

    private static boolean sameBiomeRegion(int cellA, int cellB) {
        return Math.floorDiv(cellA, LevelZeroSurfaceBiome.REGION_SIZE_CELLS)
                == Math.floorDiv(cellB, LevelZeroSurfaceBiome.REGION_SIZE_CELLS);
    }
}
