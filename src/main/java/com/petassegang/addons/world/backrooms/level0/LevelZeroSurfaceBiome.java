package com.petassegang.addons.world.backrooms.level0;

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
}
