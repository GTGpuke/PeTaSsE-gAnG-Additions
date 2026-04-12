package com.petassegang.addons.init;

import java.util.Optional;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.TintedParticleLeavesBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.petassegang.addons.block.LevelZeroWallpaperBlock;
import com.petassegang.addons.util.ModConstants;

/**
 * Registre central des blocs pour PeTaSsE_gAnG_Additions.
 *
 * <p>Tous les blocs sont declares ici via {@link DeferredRegister}.
 * Les {@link net.minecraft.world.item.BlockItem} correspondants sont dans {@link ModItems}.
 * Appeler {@link #register(BusGroup)} une seule fois depuis le constructeur du mod principal.
 */
public final class ModBlocks {

    /** DeferredRegister central pour tous les blocs du mod. */
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, ModConstants.MOD_ID);

    /** Papier peint jauni simple du Level 0. */
    public static final RegistryObject<Block> LEVEL_ZERO_WALLPAPER = BLOCKS.register(
            "level_zero_wallpaper",
            () -> new Block(BlockBehaviour.Properties
                    .ofFullCopy(Blocks.END_STONE)
                    .setId(BLOCKS.key("level_zero_wallpaper")))
    );

    /** Variante blanche simple du papier peint du Level 0. */
    public static final RegistryObject<Block> LEVEL_ZERO_WALLPAPER_AGED = BLOCKS.register(
            "level_zero_wallpaper_aged",
            () -> new Block(BlockBehaviour.Properties
                    .ofFullCopy(Blocks.END_STONE)
                    .setId(BLOCKS.key("level_zero_wallpaper_aged")))
    );

    /** Bloc technique adapte aux transitions mixtes du papier peint du Level 0. */
    public static final RegistryObject<Block> LEVEL_ZERO_WALLPAPER_ADAPTIVE = BLOCKS.register(
            "level_zero_wallpaper_adaptive",
            () -> new LevelZeroWallpaperBlock(BlockBehaviour.Properties
                    .ofFullCopy(Blocks.END_STONE)
                    .setId(BLOCKS.key("level_zero_wallpaper_adaptive")))
    );

    /** Bloc interne d'isolant pour remplir le coeur des murs du Level 0. */
    public static final RegistryObject<Block> LEVEL_ZERO_WALL_INSULATION = BLOCKS.register(
            "level_zero_wall_insulation",
            () -> new Block(BlockBehaviour.Properties
                    .ofFullCopy(Blocks.END_STONE)
                    .setId(BLOCKS.key("level_zero_wall_insulation")))
    );

    /** Moquette humide du Level 0. */
    public static final RegistryObject<Block> LEVEL_ZERO_DAMP_CARPET = BLOCKS.register(
            "level_zero_damp_carpet",
            () -> new Block(BlockBehaviour.Properties
                    .ofFullCopy(Blocks.YELLOW_WOOL)
                    .setId(BLOCKS.key("level_zero_damp_carpet")))
    );

    /** Moquette rouge du second biome du Level 0. */
    public static final RegistryObject<Block> LEVEL_ZERO_DAMP_CARPET_AGED = BLOCKS.register(
            "level_zero_damp_carpet_aged",
            () -> new Block(BlockBehaviour.Properties
                    .ofFullCopy(Blocks.YELLOW_WOOL)
                    .setId(BLOCKS.key("level_zero_damp_carpet_aged")))
    );

    /** Dalle de plafond du Level 0. */
    public static final RegistryObject<Block> LEVEL_ZERO_CEILING_TILE = BLOCKS.register(
            "level_zero_ceiling_tile",
            () -> new Block(BlockBehaviour.Properties
                    .ofFullCopy(Blocks.CALCITE)
                    .setId(BLOCKS.key("level_zero_ceiling_tile")))
    );

    /** Neon fluorescent du Level 0. */
    public static final RegistryObject<Block> LEVEL_ZERO_FLUORESCENT_LIGHT = BLOCKS.register(
            "level_zero_fluorescent_light",
            () -> new Block(BlockBehaviour.Properties
                    .ofFullCopy(Blocks.SEA_LANTERN)
                    .lightLevel(state -> 15)
                    .setId(BLOCKS.key("level_zero_fluorescent_light")))
    );

    /** Generateur d'arbre Maudit, pousse en utilisant la ConfiguredFeature personnalisee. */
    public static final TreeGrower CURSED_TREE_GROWER = new TreeGrower(
            "cursed",
            Optional.empty(),
            Optional.of(ResourceKey.create(
                    Registries.CONFIGURED_FEATURE,
                    Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "cursed_tree"))),
            Optional.empty()
    );

    /** Tronc de l'Arbre Maudit, s'oriente selon l'axe de placement. */
    public static final RegistryObject<RotatedPillarBlock> CURSED_LOG = BLOCKS.register(
            "cursed_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties
                    .ofFullCopy(Blocks.OAK_LOG)
                    .setId(BLOCKS.key("cursed_log")))
    );

    /** Feuilles de l'Arbre Maudit, transparentes et persistantes autour du tronc. */
    public static final RegistryObject<TintedParticleLeavesBlock> CURSED_LEAVES = BLOCKS.register(
            "cursed_leaves",
            () -> new TintedParticleLeavesBlock(0.02f, BlockBehaviour.Properties
                    .ofFullCopy(Blocks.OAK_LEAVES)
                    .setId(BLOCKS.key("cursed_leaves")))
    );

    /** Pousse de l'Arbre Maudit. */
    public static final RegistryObject<SaplingBlock> CURSED_SAPLING = BLOCKS.register(
            "cursed_sapling",
            () -> new SaplingBlock(CURSED_TREE_GROWER, BlockBehaviour.Properties
                    .ofFullCopy(Blocks.OAK_SAPLING)
                    .setId(BLOCKS.key("cursed_sapling")))
    );

    /** Planches de l'Arbre Maudit. */
    public static final RegistryObject<Block> CURSED_PLANKS = BLOCKS.register(
            "cursed_planks",
            () -> new Block(BlockBehaviour.Properties
                    .ofFullCopy(Blocks.OAK_PLANKS)
                    .setId(BLOCKS.key("cursed_planks")))
    );

    /**
     * Enregistre le DeferredRegister sur le bus d'evenements du mod.
     *
     * @param modBusGroup le groupe de bus d'evenements du mod
     */
    public static void register(BusGroup modBusGroup) {
        BLOCKS.register(modBusGroup);
    }

    private ModBlocks() {
        throw new UnsupportedOperationException("Classe de registre.");
    }
}
