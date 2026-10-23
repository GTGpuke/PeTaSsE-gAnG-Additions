package com.gtgpuke.gtgsbackrooms.core.portal.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.entity.Entity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import com.gtgpuke.gtgsbackrooms.core.portal.entity.PortalFacing;

/**
 * Sortie fixee d'un portail, marquee avec la baguette la ou se tenait le joueur : dimension, bloc de ses pieds et direction
 * de son regard. Le portail y mene, et l'on en ressort en marchant dans cette direction.
 *
 * @param dimension dimension de la sortie
 * @param pos bloc des pieds du joueur qui l'a marquee
 * @param facing direction horizontale de son regard ; une direction verticale devient le nord
 */
public record PortalMark(RegistryKey<World> dimension, BlockPos pos, Direction facing) {

    /** Format du composant et de la sauvegarde. */
    public static final Codec<PortalMark> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            World.CODEC.fieldOf("dimension").forGetter(PortalMark::dimension),
            BlockPos.CODEC.fieldOf("pos").forGetter(PortalMark::pos),
            Direction.CODEC.fieldOf("facing").forGetter(PortalMark::facing))
            .apply(instance, PortalMark::new));

    /** On ressort d'une sortie fixee en marchant : jamais vers le haut ni vers le bas. */
    public PortalMark {
        if (facing.getAxis().isVertical()) {
            facing = Direction.NORTH;
        }
    }

    /** Sortie marquee la ou se tient cette entity : son monde, le bloc de ses pieds, la direction de son regard. */
    public static PortalMark at(Entity entity) {
        return new PortalMark(entity.getWorld().getRegistryKey(), entity.getBlockPos(), entity.getHorizontalFacing());
    }

    /** Orientation du cadre d'arrivee : sa normale suit la direction du regard, celle dans laquelle on ressort. */
    public PortalFacing arrivalFacing() {
        return PortalFacing.fromDirection(this.facing);
    }
}
