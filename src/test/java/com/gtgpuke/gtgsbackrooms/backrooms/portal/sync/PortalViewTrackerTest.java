package com.gtgpuke.gtgsbackrooms.backrooms.portal.sync;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundFromEntityS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldEventS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldEvents;

import com.gtgpuke.gtgsbackrooms.MinecraftTestBootstrap;

@DisplayName("Paquets relayes aux joueurs qui regardent une dimension")
class PortalViewTrackerTest {

    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.ensure();
    }

    @Test
    @DisplayName("Sons, sons d'entity, evenements de monde, particules et fissures passent ; le reste concerne les joueurs presents")
    void relayedPackets() {
        assertTrue(PortalViewTracker.isRelayed(new PlaySoundS2CPacket(SoundEvents.BLOCK_NOTE_BLOCK_BELL, SoundCategory.BLOCKS,
                0.0, 64.0, 0.0, 1.0f, 1.0f, 0L)), "Un son a une position.");
        assertTrue(PortalViewTracker.isRelayed(new PlaySoundFromEntityS2CPacket(SoundEvents.BLOCK_NOTE_BLOCK_BELL, SoundCategory.BLOCKS,
                new ItemEntity(EntityType.ITEM, null), 1.0f, 1.0f, 0L)), "Un son porte par une entity.");
        assertTrue(PortalViewTracker.isRelayed(new WorldEventS2CPacket(WorldEvents.BLOCK_BROKEN, BlockPos.ORIGIN,
                Block.getRawIdFromState(Blocks.STONE.getDefaultState()), false)), "Un evenement de monde.");
        assertTrue(PortalViewTracker.isRelayed(new ParticleS2CPacket(ParticleTypes.FLAME, false, 0.0, 64.0, 0.0, 0.0f, 0.0f, 0.0f, 0.0f, 1)),
                "Des particules.");
        assertTrue(PortalViewTracker.isRelayed(new BlockBreakingProgressS2CPacket(42, BlockPos.ORIGIN, 3)), "Les fissures d'un bloc mine.");
        assertFalse(PortalViewTracker.isRelayed(new GameMessageS2CPacket(Text.literal("Bonjour."), false)), "Un message de chat.");
    }
}
