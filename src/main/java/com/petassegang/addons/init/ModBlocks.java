package com.petassegang.addons.init;

import java.util.Optional;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.SaplingBlock;
import net.minecraft.block.SaplingGenerator;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

import com.petassegang.addons.backrooms.level0.block.LevelZeroWallpaperBlock;
import com.petassegang.addons.backrooms.portal.block.BackroomsPortalBlock;
import com.petassegang.addons.core.ModConstants;

/**
 * Registre des blocs du mod. Les {@code BlockItem} correspondants sont dans {@link ModItems}.
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
     * simple ou a sortie fugace selon ses reglages ; pose hors du Level 0 il y mene,
     * pose dedans il ramene a la surface. Les pistons ne le deplacent pas : ses portails ne le suivraient pas.
     */
    public static final BackroomsPortalBlock BACKROOMS_PORTAL = register(
            "backrooms_portal",
            new BackroomsPortalBlock(AbstractBlock.Settings.create()
                    .noCollision()
                    .nonOpaque()
                    .strength(0.5f)
                    .sounds(BlockSoundGroup.GLASS)
                    .pistonBehavior(PistonBehavior.BLOCK)));

    /** Generateur de l'Arbre Maudit, branche sur la configured feature {@code cursed_tree}. */
    public static final SaplingGenerator CURSED_TREE_GROWER = new SaplingGenerator(
            "cursed",
            Optional.empty(),
            Optional.of(RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, Identifier.of(ModConstants.MOD_ID, "cursed_tree"))),
            Optional.empty());

    /** Tronc de l'Arbre Maudit. */
    public static final PillarBlock CURSED_LOG = register(
            "cursed_log",
            new PillarBlock(AbstractBlock.Settings.copy(Blocks.OAK_LOG)));

    /** Feuilles de l'Arbre Maudit. */
    public static final LeavesBlock CURSED_LEAVES = register(
            "cursed_leaves",
            new LeavesBlock(AbstractBlock.Settings.copy(Blocks.OAK_LEAVES)));

    /** Pousse de l'Arbre Maudit. */
    public static final SaplingBlock CURSED_SAPLING = register(
            "cursed_sapling",
            new SaplingBlock(CURSED_TREE_GROWER, AbstractBlock.Settings.copy(Blocks.OAK_SAPLING)));

    /** Planches de l'Arbre Maudit. */
    public static final Block CURSED_PLANKS = register(
            "cursed_planks",
            new Block(AbstractBlock.Settings.copy(Blocks.OAK_PLANKS)));

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
