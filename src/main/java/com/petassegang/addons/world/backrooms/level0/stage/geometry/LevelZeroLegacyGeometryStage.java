package com.petassegang.addons.world.backrooms.level0.stage.geometry;

import com.petassegang.addons.config.ModConfig;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroCellTopology;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroGeometryFeature;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroGeometryMask;
import com.petassegang.addons.world.backrooms.level0.noise.StageRandom;
import com.petassegang.addons.world.backrooms.level0.stage.LevelZeroCellContext;

/**
 * Etape legacy de micro-anomalies geometriques rares au-dessus de la grille 3x3.
 */
public final class LevelZeroLegacyGeometryStage {

    private final boolean enabled;

    /**
     * Construit l'etape geometry legacy a partir de la config globale.
     */
    public LevelZeroLegacyGeometryStage() {
        this(ModConfig.ENABLE_LEVEL_ZERO_NOISE_GEOMETRY);
    }

    /**
     * Construit l'etape geometry legacy avec un toggle explicite.
     *
     * @param enabled {@code true} pour activer les anomalies geometriques
     */
    public LevelZeroLegacyGeometryStage(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Echantillonne les features geometriques fines d'une cellule.
     *
     * <p>Cette premiere version reste volontairement conservative :
     * elle n'affecte que rarement des cellules traversables, en priorite sur
     * des couloirs droits. Les jonctions restent stables pour ne pas ajouter
     * artificiellement de surplus topologique ou de faux cul-de-sac.
     * Les etranglements 1-wide restent exceptionnellement rares.
     *
     * @param context contexte canonique de cellule
     * @param topology topologie fine deja derivee
     * @return masque de features geometriques
     */
    public int sample(LevelZeroCellContext context, LevelZeroCellTopology topology) {
        if (!enabled) {
            return LevelZeroGeometryMask.none();
        }
        if (topology == LevelZeroCellTopology.WALL || topology == LevelZeroCellTopology.ROOM_LARGE) {
            // Les murs pleins et les grandes pieces restent hors de cette
            // couche pour ne pas superposer de bruit geometrique a des zones
            // qui doivent rester soit stables, soit traitees a part.
            return LevelZeroGeometryMask.none();
        }

        long hash = StageRandom.mixLegacy(
                context.layoutSeed(),
                StageRandom.Stage.NOISE_GEOMETRY,
                context.cellX(),
                context.cellZ());
        int roll = Math.floorMod(hash, 10_000);
        int mask = LevelZeroGeometryMask.none();

        if (topology == LevelZeroCellTopology.CORRIDOR) {
            // Le couloir droit est la cible principale de cette couche : il
            // accepte quelques variations tres rares sans casser la lecture du
            // labyrinthe.
            if (roll < 90) {
                mask = LevelZeroGeometryMask.with(mask, LevelZeroGeometryFeature.OFFSET_WALL);
            } else if (roll < 150) {
                mask = LevelZeroGeometryMask.with(mask, LevelZeroGeometryFeature.RECESS);
            } else if (roll < 178) {
                mask = LevelZeroGeometryMask.with(mask, LevelZeroGeometryFeature.HALF_WALL);
            } else if (roll < 184) {
                mask = LevelZeroGeometryMask.with(mask, LevelZeroGeometryFeature.PINCH_1WIDE);
            }
            return mask;
        }

        if (topology == LevelZeroCellTopology.ANGLE) {
            if (roll < 50) {
                mask = LevelZeroGeometryMask.with(mask, LevelZeroGeometryFeature.OFFSET_WALL);
            } else if (roll < 90) {
                mask = LevelZeroGeometryMask.with(mask, LevelZeroGeometryFeature.RECESS);
            }
            return mask;
        }

        if (topology == LevelZeroCellTopology.T_JUNCTION
                || topology == LevelZeroCellTopology.CROSSROAD
                || topology == LevelZeroCellTopology.JUNCTION) {
            // Les jonctions restent neutres pour eviter de creer de faux surplus
            // topologiques ou des intersections visuellement brouillees.
            return LevelZeroGeometryMask.none();
        }

        if (topology == LevelZeroCellTopology.DEAD_END) {
            if (roll < 120) {
                mask = LevelZeroGeometryMask.with(mask, LevelZeroGeometryFeature.ALCOVE);
            } else if (roll < 145) {
                mask = LevelZeroGeometryMask.with(mask, LevelZeroGeometryFeature.RECESS);
            }
        }
        return mask;
    }
}
