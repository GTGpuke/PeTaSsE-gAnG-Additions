package com.gtgpuke.gtgsbackrooms.core.registry;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

import com.gtgpuke.gtgsbackrooms.backrooms.level0.block.LevelZeroWallpaperBlock;
import com.gtgpuke.gtgsbackrooms.core.ModConstants;
import com.gtgpuke.gtgsbackrooms.core.portal.block.BackroomsPortalBlock;

/**
 * Registre des blocs du mod. Les {@code BlockItem} correspondants sont dans {@link ModItems}, sauf pour le bloc portail,
 * que pose la baguette.
 */
public final class ModBlocks {

    /** Papier peint jauni du Level 0 (bloc decoratif simple). */
    public static final Block LEVEL_ZERO_WALLPAPER = register(
            "level_zero_wallpaper",
            new Block(AbstractBlock.Settings.copy(Blocks.END_STONE)));

    /** Papier peint blanc du Level 0 (bloc decoratif simple). */
    public static final Block LEVEL_ZERO_WALLPAPER_AGED = register(
            "level_zero_wallpaper_aged",
            new Block(AbstractBlock.Settings.copy(Blocks.END_STONE)));

    /** Mur adaptatif pose par le generateur sur toutes les colonnes visibles du Level 0. */
    public static final LevelZeroWallpaperBlock LEVEL_ZERO_WALLPAPER_ADAPTIVE = register(
            "level_zero_wallpaper_adaptive",
            new LevelZeroWallpaperBlock(AbstractBlock.Settings.copy(Blocks.END_STONE)));

    /** Moquette humide du Level 0. */
    public static final Block LEVEL_ZERO_DAMP_CARPET = register(
            "level_zero_damp_carpet",
            new Block(AbstractBlock.Settings.copy(Blocks.YELLOW_WOOL)));

    /** Moquette rouge du second biome du Level 0. */
    public static final Block LEVEL_ZERO_DAMP_CARPET_AGED = register(
            "level_zero_damp_carpet_aged",
            new Block(AbstractBlock.Settings.copy(Blocks.YELLOW_WOOL)));

    /** Dalle de plafond du Level 0. */
    public static final Block LEVEL_ZERO_CEILING_TILE = register(
            "level_zero_ceiling_tile",
            new Block(AbstractBlock.Settings.copy(Blocks.CALCITE)));

    /** Neon fluorescent du Level 0. */
    public static final Block LEVEL_ZERO_FLUORESCENT_LIGHT = register(
            "level_zero_fluorescent_light",
            new Block(AbstractBlock.Settings.copy(Blocks.SEA_LANTERN).luminance(state -> 15)));

    /**
     * Bloc portail : invisible et traversable, il pose un portail plein cadre, a une ou deux faces, aller-retour, aller
     * simple ou a sortie fugace selon ses reglages ; pose hors du Level 0 il y mene, pose dedans il ramene a la surface.
     * Seule la baguette le pose, et casse il ne rend rien. Les pistons ne le deplacent pas : ses portails ne le suivraient pas.
     */
    public static final BackroomsPortalBlock BACKROOMS_PORTAL = register(
            "backrooms_portal",
            new BackroomsPortalBlock(AbstractBlock.Settings.create()
                    .noCollision()
                    .nonOpaque()
                    .strength(0.5f)
                    .sounds(BlockSoundGroup.GLASS)
                    .dropsNothing()
                    .pistonBehavior(PistonBehavior.BLOCK)));

    private ModBlocks() {
        throw new UnsupportedOperationException("Classe de registre.");
    }

    /** Force le chargement de la classe, donc l'enregistrement des blocs. */
    public static void register() {
        ModConstants.LOGGER.debug("Blocs du mod enregistres.");
    }

    private static <T extends Block> T register(String name, T block) {
        return Registry.register(Registries.BLOCK, Identifier.of(ModConstants.MOD_ID, name), block);
    }
}
