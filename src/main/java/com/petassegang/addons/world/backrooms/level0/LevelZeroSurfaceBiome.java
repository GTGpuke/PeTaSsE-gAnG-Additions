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
 *
 * <p>Depuis la bascule multi-layer, un biome peut aussi dependre du layer :
 * un meme X/Z peut donc garder la meme topologie tout en changeant
 * d'ambiance visuelle et lumineuse d'un etage a l'autre.
 *
 * <p>Ce fichier est donc le catalogue visuel du Level 0, pas une couche de
 * generation structurelle.
 */
public enum LevelZeroSurfaceBiome {

    /** Palette classique du Level 0. */
    BASE(0, LevelZeroLayout.SURFACE_VARIANT_BASE, LevelZeroLayout.SURFACE_VARIANT_BASE, 3, 7, 0, 0, false, -1),
    /** Palette alternative avec murs blancs et tapis rouges. */
    RED(1, LevelZeroLayout.SURFACE_VARIANT_ALTERNATE, LevelZeroLayout.SURFACE_VARIANT_ALTERNATE, 4, 5, 1, 1, true, layerMask(1, 3));

    /** Taille d'une region cosmetique en cellules logiques. */
    private static final int REGION_SIZE_CELLS = 48;
    /** Rarete du biome secondaire. */
    private static final int RED_REGION_MODULO = 18;
    /** Rarete des regions entierement sombres dans les biomes qui l'autorisent. */
    private static final int FULL_DARK_REGION_MODULO = 4;
    private final int id;
    private final int floorVariant;
    private final int wallpaperVariant;
    private final int lightGridSpacing;
    private final int lightDropoutModulo;
    private final int lightGridPhaseX;
    private final int lightGridPhaseZ;
    private final boolean supportsFullDarkRegions;
    private final int allowedLayerMask;

    LevelZeroSurfaceBiome(int id,
                         int floorVariant,
                         int wallpaperVariant,
                         int lightGridSpacing,
                         int lightDropoutModulo,
                         int lightGridPhaseX,
                         int lightGridPhaseZ,
                         boolean supportsFullDarkRegions,
                         int allowedLayerMask) {
        this.id = id;
        this.floorVariant = floorVariant;
        this.wallpaperVariant = wallpaperVariant;
        this.lightGridSpacing = lightGridSpacing;
        this.lightDropoutModulo = lightDropoutModulo;
        this.lightGridPhaseX = lightGridPhaseX;
        this.lightGridPhaseZ = lightGridPhaseZ;
        this.supportsFullDarkRegions = supportsFullDarkRegions;
        this.allowedLayerMask = allowedLayerMask;
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
     * Retourne l'espacement cible de la trame lumineuse de ce biome, en cellules.
     *
     * @return espacement de la grille lumineuse
     */
    public int lightGridSpacing() {
        return lightGridSpacing;
    }

    /**
     * Retourne le modulo de suppression legere applique sur les points de grille.
     *
     * @return modulo de dropout, superieur ou egal a 2
     */
    public int lightDropoutModulo() {
        return lightDropoutModulo;
    }

    /**
     * Retourne la phase X fixe de la grille lumineuse de ce biome.
     *
     * @return decalage X stable de la trame
     */
    public int lightGridPhaseX() {
        return lightGridPhaseX;
    }

    /**
     * Retourne la phase Z fixe de la grille lumineuse de ce biome.
     *
     * @return decalage Z stable de la trame
     */
    public int lightGridPhaseZ() {
        return lightGridPhaseZ;
    }

    /**
     * Indique si ce biome peut produire des regions entierement sombres.
     *
     * @return {@code true} si ce biome autorise des blackouts regionaux
     */
    public boolean supportsFullDarkRegions() {
        return supportsFullDarkRegions;
    }

    public boolean supportsLayer(int layerIndex) {
        return allowedLayerMask < 0 || ((allowedLayerMask >>> layerIndex) & 1) != 0;
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
        return sampleAtWorld(worldX, worldZ, 0);
    }

    public static LevelZeroSurfaceBiome sampleAtWorld(int worldX, int worldZ, int layerIndex) {
        int cellX = LevelZeroCoords.worldToCellX(worldX);
        int cellZ = LevelZeroCoords.worldToCellZ(worldZ);
        return sampleAtCell(cellX, cellZ, layerIndex);
    }

    /**
     * Echantillonne le biome cosmetique a partir d'une position logique.
     *
     * @param cellX coordonnee X en cellules logiques
     * @param cellZ coordonnee Z en cellules logiques
     * @return biome cosmetique de surface
     */
    public static LevelZeroSurfaceBiome sampleAtCell(int cellX, int cellZ) {
        return sampleAtCell(cellX, cellZ, 0);
    }

    public static LevelZeroSurfaceBiome sampleAtCell(int cellX, int cellZ, int layerIndex) {
        int regionX = Math.floorDiv(cellX, REGION_SIZE_CELLS);
        int regionZ = Math.floorDiv(cellZ, REGION_SIZE_CELLS);
        // Le biome est choisi a l'echelle d'une grande region logique pour
        // eviter un bruit trop local. Le layer fait partie du hash pour
        // permettre des ambiances verticales distinctes sans changer le layout.
        long hash = StageRandom.mixLegacy(
                0L,
                StageRandom.Stage.SURFACE_BIOME,
                regionX,
                ((((long) regionZ) & 0xffffffffL) << 8) ^ layerIndex);
        if (RED.supportsLayer(layerIndex) && Math.floorMod(hash, RED_REGION_MODULO) == 0) {
            return RED;
        }
        return BASE;
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

    /**
     * Deduit le biome de surface a partir de l'identifiant de variante de sol.
     *
     * @param floorVariant variante logique de moquette
     * @return biome cosmetique correspondant
     */
    public static LevelZeroSurfaceBiome fromFloorVariant(int floorVariant) {
        return floorVariant == LevelZeroLayout.SURFACE_VARIANT_ALTERNATE ? RED : BASE;
    }

    /**
     * Indique si la cellule appartient a une region de biome completement sombre.
     *
     * @param cellX coordonnee X en cellules
     * @param cellZ coordonnee Z en cellules
     * @param layoutSeed seed de layout
     * @return {@code true} si cette region doit rester sans neon
     */
    public boolean isFullDarkRegion(int cellX, int cellZ, long layoutSeed) {
        if (!supportsFullDarkRegions) {
            return false;
        }
        // Les blackouts regionaux restent derives de la seed de layout pour
        // etre partages par toute une zone coherente, et non cellule par cellule.
        int regionX = Math.floorDiv(cellX, REGION_SIZE_CELLS);
        int regionZ = Math.floorDiv(cellZ, REGION_SIZE_CELLS);
        long hash = StageRandom.mixLegacy(layoutSeed, StageRandom.Stage.BIOME_LIGHTING, regionX, regionZ);
        return Math.floorMod(hash, FULL_DARK_REGION_MODULO) == 0;
    }

    private static int layerMask(int... layers) {
        int mask = 0;
        for (int layer : layers) {
            mask |= 1 << layer;
        }
        return mask;
    }
}
