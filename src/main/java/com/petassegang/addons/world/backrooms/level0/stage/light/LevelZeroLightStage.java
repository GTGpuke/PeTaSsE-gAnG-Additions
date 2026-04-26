package com.petassegang.addons.world.backrooms.level0.stage.light;

import com.petassegang.addons.world.backrooms.level0.LevelZeroSurfaceBiome;
import com.petassegang.addons.world.backrooms.level0.layout.sector.LevelZeroSectorRoomKind;
import com.petassegang.addons.world.backrooms.level0.noise.StageRandom;
import com.petassegang.addons.world.backrooms.level0.stage.LevelZeroCellContext;
import com.petassegang.addons.world.backrooms.level0.stage.LevelZeroCellStage;

/**
 * Etape historique de decision lumineuse du Level 0.
 *
 * <p>Cette classe part d'une trame globale volontairement simple a l'echelle
 * de tout le niveau, puis laisse le biome filtrer cette trame selon sa densite
 * cible et ses eventuels blackouts. L'arbitrage final de proximite entre
 * voisins reste ensuite du ressort de la pipeline complete.
 */
public final class LevelZeroLightStage implements LevelZeroCellStage<Boolean> {

    /**
     * Construit l'etape historique de placement des neons.
     *
     * @param lightInterval ancien modulo historique, conserve uniquement pour
     *                      stabiliser le contrat public legacy
     */
    public LevelZeroLightStage(int lightInterval) {
        // Le modulo historique reste accepte pour conserver le contrat de la
        // pipeline legacy, meme s'il n'influence plus le motif actif.
    }

    @Override
    public Boolean sample(LevelZeroCellContext context) {
        return sample(context, LevelZeroSurfaceBiome.BASE, false, LevelZeroSectorRoomKind.NONE);
    }

    /**
     * Echantillonne la lumiere d'une cellule a partir du biome et du statut de grande piece.
     *
     * @param context contexte canonique de cellule
     * @param surfaceBiome biome cosmetique de la cellule
     * @param largeRoom {@code true} si la cellule appartient a une grande piece
     * @return {@code true} si un neon doit etre place
     */
    public boolean sample(LevelZeroCellContext context,
                          LevelZeroSurfaceBiome surfaceBiome,
                          boolean largeRoom) {
        return sample(context, surfaceBiome, largeRoom, LevelZeroSectorRoomKind.NONE);
    }

    /**
     * Echantillonne la presence d'un neon a partir du biome, du statut
     * de grande piece et du type de salle legacy.
     *
     * @param context contexte canonique de cellule
     * @param surfaceBiome biome cosmetique de surface
     * @param largeRoom {@code true} si la cellule appartient a une grande piece
     * @param roomKind type de salle legacy conserve pour compatibilite de
     *                 signature, sans impact sur le motif actif
     * @return {@code true} si un neon doit etre place
     */
    public boolean sample(LevelZeroCellContext context,
                          LevelZeroSurfaceBiome surfaceBiome,
                          boolean largeRoom,
                          LevelZeroSectorRoomKind roomKind) {
        return sampleCandidate(context, surfaceBiome, largeRoom, roomKind);
    }

    /**
     * Echantillonne uniquement la candidature lumineuse locale d'une cellule,
     * sans appliquer la regle finale de proximite entre voisins.
     *
     * <p>Cette methode sert a la pipeline complete pour arbitrer les conflits
     * de voisinage avec le vrai contexte des cellules adjacentes.
     *
     * @param context contexte canonique de cellule
     * @param surfaceBiome biome cosmetique de surface
     * @param largeRoom {@code true} si la cellule suit la logique de grande piece
     * @param roomKind type de salle legacy conserve pour compatibilite de
     *                 signature, sans impact sur le motif actif
     * @return {@code true} si la cellule est candidate a porter un neon
     */
    public boolean sampleCandidate(LevelZeroCellContext context,
                                   LevelZeroSurfaceBiome surfaceBiome,
                                   boolean largeRoom,
                                   LevelZeroSectorRoomKind roomKind) {
        if (!sampleGlobalStripedPattern(context)) {
            return false;
        }
        if (surfaceBiome.isFullDarkRegion(context.cellX(), context.cellZ(), context.layoutSeed())) {
            return false;
        }
        if (largeRoom && surfaceBiome.isLargeRoomBlackout(context.cellX(), context.cellZ(), context.layoutSeed())) {
            return false;
        }
        return surfaceBiome.keepsStripedLight(context.cellX(), context.cellZ(), context.layoutSeed());
    }

    /**
     * Retourne un score deterministe de priorite lumineuse pour arbitrer les
     * conflits de proximite entre cellules candidates.
     *
     * @param context contexte canonique de cellule
     * @return score stable de priorite
     */
    public long lightScore(LevelZeroCellContext context) {
        return StageRandom.mixLegacy(
                context.layoutSeed(),
                StageRandom.Stage.LIGHTS,
                context.cellX(),
                context.cellZ());
    }

    private boolean sampleGlobalStripedPattern(LevelZeroCellContext context) {
        int rowPhase = Math.floorMod(context.cellZ(), 4);
        if (rowPhase == 1) {
            return false;
        }
        if (rowPhase == 0) {
            return Math.floorMod(context.cellX(), 2) == 0;
        }
        if (rowPhase == 2) {
            return Math.floorMod(context.cellX(), 2) != 0;
        }
        return false;
    }
}
