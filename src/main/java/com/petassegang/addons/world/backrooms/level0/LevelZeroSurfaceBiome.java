package com.petassegang.addons.world.backrooms.level0;

import net.minecraft.block.BlockState;

import com.petassegang.addons.init.ModBlocks;
import com.petassegang.addons.world.backrooms.level0.coord.LevelZeroCoords;
import com.petassegang.addons.world.backrooms.level0.noise.StageRandom;

/**
 * Biomes cosmetiques internes du Level 0.
 *
 * <p>Ils ne changent jamais la topologie du labyrinthe. Ils servent
 * uniquement a selectionner de grandes palettes de surfaces.
 */
public enum LevelZeroSurfaceBiome {

    /** Palette classique du Level 0. */
    BASE(0, LevelZeroLayout.SURFACE_VARIANT_BASE, LevelZeroLayout.SURFACE_VARIANT_BASE),
    /** Palette alternative avec murs blancs et tapis rouges. */
    RED(1, LevelZeroLayout.SURFACE_VARIANT_ALTERNATE, LevelZeroLayout.SURFACE_VARIANT_ALTERNATE);

    /** Taille d'une region cosmetique en cellules logiques. */
    private static final int REGION_SIZE_CELLS = 48;
    /** Rarete du biome secondaire. */
    private static final int RED_REGION_MODULO = 18;
    private final int id;
    private final int floorVariant;
    private final int wallpaperVariant;

    LevelZeroSurfaceBiome(int id, int floorVariant, int wallpaperVariant) {
        this.id = id;
        this.floorVariant = floorVariant;
        this.wallpaperVariant = wallpaperVariant;
    }

    /**
     * Retourne l'identifiant compact du biome.
     *
     * @return identifiant stable du biome
     */
    public int id() {
        return id;
    }

    /**
     * Retourne la variante de sol associee a ce biome.
     *
     * @return identifiant de variante de moquette
     */
    public int floorVariant() {
        return floorVariant;
    }

    /**
     * Retourne la variante de papier peint associee a ce biome.
     *
     * @return identifiant de variante de papier peint
     */
    public int wallpaperVariant() {
        return wallpaperVariant;
    }

    /**
     * Convertit un identifiant compact en biome.
     *
     * @param id identifiant stocke dans le layout
     * @return biome cosmetique correspondant
     */
    public static LevelZeroSurfaceBiome fromId(int id) {
        return id == RED.id ? RED : BASE;
    }

    /**
     * Echantillonne le biome cosmetique a partir d'une position monde.
     *
     * <p>Le calcul est purement deterministe et ne depend pas de l'etat du monde,
     * ce qui permet au client et au serveur de retrouver exactement la meme
     * palette sans synchronisation supplementaire.
     *
     * @param worldX coordonnee X monde en blocs
     * @param worldZ coordonnee Z monde en blocs
     * @return biome cosmetique de surface
     */
    public static LevelZeroSurfaceBiome sampleAtWorld(int worldX, int worldZ) {
        int cellX = LevelZeroCoords.worldToCellX(worldX);
        int cellZ = LevelZeroCoords.worldToCellZ(worldZ);
        return sampleAtCell(cellX, cellZ);
    }

    /**
     * Echantillonne le biome cosmetique a partir d'une position logique.
     *
     * @param cellX coordonnee X en cellules logiques
     * @param cellZ coordonnee Z en cellules logiques
     * @return biome cosmetique de surface
     */
    public static LevelZeroSurfaceBiome sampleAtCell(int cellX, int cellZ) {
        int regionX = Math.floorDiv(cellX, REGION_SIZE_CELLS);
        int regionZ = Math.floorDiv(cellZ, REGION_SIZE_CELLS);
        long hash = StageRandom.mixLegacy(0L, StageRandom.Stage.SURFACE_BIOME, regionX, regionZ);
        return Math.floorMod(hash, RED_REGION_MODULO) == 0 ? RED : BASE;
    }

    /**
     * Deduit le biome de surface a partir du bloc de sol pose dans le monde.
     *
     * @param floorState etat du bloc de sol observe
     * @return biome cosmetique correspondant
     */
    public static LevelZeroSurfaceBiome fromFloorState(BlockState floorState) {
        return floorState.isOf(ModBlocks.LEVEL_ZERO_DAMP_CARPET_AGED) ? RED : BASE;
    }
}
