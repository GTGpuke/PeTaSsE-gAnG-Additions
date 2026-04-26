package com.petassegang.addons.world.backrooms.level0.write;

import net.minecraft.block.BlockState;

/**
 * Materiaux minimaux necessaires pour ecrire une colonne.
 *
 * <p>Ce record ne porte aucun choix metier complexe : seulement le resultat
 * materialise du couple "cellule semantique + palette".
 */
public record LevelZeroColumnMaterial(
        boolean walkable,
        BlockState floor,
        BlockState interior,
        BlockState ceiling) {
}
