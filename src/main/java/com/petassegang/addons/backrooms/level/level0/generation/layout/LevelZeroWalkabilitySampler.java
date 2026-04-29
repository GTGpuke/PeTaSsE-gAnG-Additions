package com.petassegang.addons.backrooms.level.level0.generation.layout;

import com.petassegang.addons.backrooms.level.level0.generation.layout.sector.LevelZeroSectorRoomKind;

/**
 * Contrat minimal de lecture de la base structurelle d'une cellule logique.
 *
 * <p>Cette interface abstrait la source la plus brute de la pipeline :
 * walkability et, optionnellement, type de salle historique. Les couches
 * superieures peuvent ainsi raisonner sans savoir si cette base vient d'un
 * cache secteur, d'une region ou d'un faux sampler de test.
 */
public interface LevelZeroWalkabilitySampler {

    /**
     * Echantillonne la traversabilite d'une cellule logique.
     *
     * @param cellX coordonnee cellule X
     * @param cellZ coordonnee cellule Z
     * @return {@code true} si la cellule est ouverte
     */
    boolean sampleWalkableCell(int cellX, int cellZ);

    /**
     * Echantillonne le type de salle legacy d'une cellule logique.
     *
     * @param cellX coordonnee cellule X
     * @param cellZ coordonnee cellule Z
     * @return type de salle derive
     */
    default LevelZeroSectorRoomKind sampleRoomKindCell(int cellX, int cellZ) {
        return LevelZeroSectorRoomKind.NONE;
    }
}
