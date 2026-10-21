package com.gtgpuke.gtgsbackrooms.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.block.BlockState;
import net.minecraft.server.world.ChunkLevelType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import com.gtgpuke.gtgsbackrooms.core.portal.light.VirtualLightSources;
import com.gtgpuke.gtgsbackrooms.core.portal.light.VirtualLightView;
import com.gtgpuke.gtgsbackrooms.core.portal.sync.PortalViewTracker;

/**
 * Chaque monde (serveur, client, monde distant du client) porte ses sources de lumiere virtuelles : elles disparaissent
 * avec lui, sans registre a vider. Un monde serveur relaie aux vues de portail les changements de blocs que vanilla
 * tait dans un chunk pas encore actif.
 */
@Mixin(World.class)
public abstract class MixinWorld implements VirtualLightView {

    /** Publiees par le thread principal, lues par le moteur de lumiere sur le sien. */
    @Unique
    private volatile VirtualLightSources backrooms$virtualLight = VirtualLightSources.EMPTY;

    @Override
    public VirtualLightSources backrooms$virtualLight() {
        return this.backrooms$virtualLight;
    }

    @Override
    public void backrooms$setVirtualLight(VirtualLightSources sources) {
        this.backrooms$virtualLight = sources;
    }

    /**
     * Vanilla ne previent ses ecouteurs d'un changement de bloc que dans un chunk actif (ses joueurs ne recoivent que
     * ceux-la) ; une vue de portail recoit aussi les chunks seulement charges de sa fenetre (bordure du ticket, ou le
     * temps que les voisins se chargent). Le changement y est relaye quand meme ; dans un chunk actif, le relais passe
     * par les ecouteurs ({@code MixinServerChunkManager}).
     */
    @WrapOperation(method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;II)Z", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/chunk/WorldChunk;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Z)Lnet/minecraft/block/BlockState;"))
    private @Nullable BlockState backrooms$relayOutsideActiveChunks(
            WorldChunk chunk, BlockPos pos, BlockState state, boolean moved, Operation<BlockState> original) {
        BlockState previous = original.call(chunk, pos, state, moved);
        if (previous != null && (Object) this instanceof ServerWorld world && !chunk.getLevelType().isAfter(ChunkLevelType.BLOCK_TICKING)) {
            PortalViewTracker.onBlockChanged(world, pos);
        }
        return previous;
    }
}
