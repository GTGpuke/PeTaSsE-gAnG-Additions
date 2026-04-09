package com.petassegang.addons.init;

import java.util.Optional;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.TintedParticleLeavesBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.petassegang.addons.util.ModConstants;

/**
 * Registre central des blocs pour PeTaSsE_gAnG_Additions.
 *
 * <p>Tous les blocs sont déclarés ici via {@link DeferredRegister}.
 * Les {@link net.minecraft.world.item.BlockItem} correspondants sont dans {@link ModItems}.
 * Appeler {@link #register(BusGroup)} une seule fois depuis le constructeur du mod principal.
 */
public final class ModBlocks {

    /** DeferredRegister central pour tous les blocs du mod. */
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, ModConstants.MOD_ID);

    // ── Arbre Maudit ─────────────────────────────────────────────────────────

    /** Générateur d'arbre Maudit — pousse en utilisant la ConfiguredFeature personnalisée. */
    public static final TreeGrower CURSED_TREE_GROWER = new TreeGrower(
            "cursed",
            Optional.empty(),
            Optional.of(ResourceKey.create(
                    Registries.CONFIGURED_FEATURE,
                    Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "cursed_tree"))),
            Optional.empty()
    );

    /** Tronc de l'Arbre Maudit — s'oriente selon l'axe de placement (X/Y/Z). */
    public static final RegistryObject<RotatedPillarBlock> CURSED_LOG = BLOCKS.register(
            "cursed_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties
                    .ofFullCopy(net.minecraft.world.level.block.Blocks.OAK_LOG)
                    .setId(BLOCKS.key("cursed_log")))
    );

    /** Feuilles de l'Arbre Maudit — transparentes, disparaissent sans tronc adjacent. */
    public static final RegistryObject<TintedParticleLeavesBlock> CURSED_LEAVES = BLOCKS.register(
            "cursed_leaves",
            () -> new TintedParticleLeavesBlock(0.02f, BlockBehaviour.Properties
                    .ofFullCopy(net.minecraft.world.level.block.Blocks.OAK_LEAVES)
                    .setId(BLOCKS.key("cursed_leaves")))
    );

    /** Pousse de l'Arbre Maudit — pousse en utilisant la structure du chêne. */
    public static final RegistryObject<SaplingBlock> CURSED_SAPLING = BLOCKS.register(
            "cursed_sapling",
            () -> new SaplingBlock(CURSED_TREE_GROWER, BlockBehaviour.Properties
                    .ofFullCopy(net.minecraft.world.level.block.Blocks.OAK_SAPLING)
                    .setId(BLOCKS.key("cursed_sapling")))
    );

    /** Planches de l'Arbre Maudit. */
    public static final RegistryObject<Block> CURSED_PLANKS = BLOCKS.register(
            "cursed_planks",
            () -> new Block(BlockBehaviour.Properties
                    .ofFullCopy(net.minecraft.world.level.block.Blocks.OAK_PLANKS)
                    .setId(BLOCKS.key("cursed_planks")))
    );

    // ── Méthode d'enregistrement ──────────────────────────────────────────────

    /**
     * Enregistre le DeferredRegister sur le bus d'événements du mod.
     * Doit être appelé exactement une fois, depuis le constructeur de {@code PeTaSsEgAnGAdditionsMod}.
     *
     * @param modBusGroup le groupe de bus d'événements du mod
     */
    public static void register(BusGroup modBusGroup) {
        BLOCKS.register(modBusGroup);
    }

    private ModBlocks() {
        throw new UnsupportedOperationException("Classe de registre.");
    }
}
