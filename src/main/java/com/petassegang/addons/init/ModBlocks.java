package com.petassegang.addons.init;

import java.util.Optional;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.SaplingBlock;
import net.minecraft.block.SaplingGenerator;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import com.petassegang.addons.block.backrooms.LevelZeroBaseboardBlock;
import com.petassegang.addons.block.backrooms.LevelZeroWallpaperBlock;
import com.petassegang.addons.util.ModConstants;

/**
 * Registre central des blocs pour PeTaSsE_gAnG_Additions.
 *
 * <p>Tous les blocs sont enregistres directement via {@link Registry#register}.
 * Les {@link net.minecraft.item.BlockItem} correspondants sont dans {@link ModItems}.
 * Appeler {@link #register()} une seule fois depuis le point d'entree du mod.
 */
public final class ModBlocks {

    /** Papier peint jauni simple du Level 0. */
    public static final Block LEVEL_ZERO_WALLPAPER = register(
            "level_zero_wallpaper",
            new Block(AbstractBlock.Settings.copy(Blocks.END_STONE)));

    /** Variante blanche simple du papier peint du Level 0. */
    public static final Block LEVEL_ZERO_WALLPAPER_AGED = register(
            "level_zero_wallpaper_aged",
            new Block(AbstractBlock.Settings.copy(Blocks.END_STONE)));

    /** Bloc technique adapte aux transitions mixtes du papier peint du Level 0. */
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

    /** Plinthe adaptative decorant le bas des murs du Level 0. */
    public static final LevelZeroBaseboardBlock LEVEL_ZERO_BASEBOARD = register(
            "level_zero_baseboard",
            new LevelZeroBaseboardBlock(AbstractBlock.Settings.copy(Blocks.OAK_PLANKS)
                    .strength(0.3f)
                    .nonOpaque()
                    .noCollision()));

    /** Generateur d'arbre Maudit, pousse via la ConfiguredFeature personnalisee. */
    public static final SaplingGenerator CURSED_TREE_GROWER = new SaplingGenerator(
            "cursed",
            Optional.empty(),
            Optional.of(RegistryKey.of(
                    RegistryKeys.CONFIGURED_FEATURE,
                    Identifier.of(ModConstants.MOD_ID, "cursed_tree"))),
            Optional.empty()
    );

    /** Tronc de l'Arbre Maudit, s'oriente selon l'axe de placement. */
    public static final PillarBlock CURSED_LOG = register(
            "cursed_log",
            new PillarBlock(AbstractBlock.Settings.copy(Blocks.OAK_LOG)));

    /** Feuilles de l'Arbre Maudit, transparentes et persistantes autour du tronc. */
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

    /**
     * Enregistre tous les blocs dans le registre vanilla.
     * Doit etre appele exactement une fois depuis le point d'entree du mod.
     */
    public static void register() {
        // L'initialisation des champs statiques suffit — cette methode
        // force le chargement de la classe et garantit que les blocs
        // sont bien enregistres avant les items.
        ModConstants.LOGGER.debug("Blocs du mod enregistres.");
    }

    private static <T extends Block> T register(String name, T block) {
        return Registry.register(Registries.BLOCK, Identifier.of(ModConstants.MOD_ID, name), block);
    }

    private ModBlocks() {
        throw new UnsupportedOperationException("Classe de registre.");
    }
}
