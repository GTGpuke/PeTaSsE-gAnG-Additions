package com.petassegang.addons.world.backrooms.level0.write;

import net.minecraft.block.BlockState;

/**
 * Representation intermediaire minimale des blocs d'une colonne du Level 0.
 */
public record LevelZeroColumnMaterial(
        boolean walkable,
        BlockState floor,
        BlockState interior,
        BlockState ceiling) {
}
