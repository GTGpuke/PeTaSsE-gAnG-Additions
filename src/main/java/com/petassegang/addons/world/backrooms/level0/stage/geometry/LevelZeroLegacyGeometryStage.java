package com.petassegang.addons.world.backrooms.level0.stage.geometry;

import com.petassegang.addons.config.ModConfig;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroCellConnections;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroCellTopology;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroGeometryMask;
import com.petassegang.addons.world.backrooms.level0.stage.LevelZeroCellContext;

/**
 * Etape legacy de micro-anomalies geometriques rares au-dessus de la grille 3x3.
 *
 * <p>La couche est volontairement vide pour repartir proprement sur les
 * variantes de noise. Le stage reste branche dans la pipeline afin que chaque
 * future variante puisse etre ajoutee une par une sans modifier l'architecture
 * du layout.
 *
 * <p>TODO Level 0 : reconstruire des noises geometriques plus intelligents.
 * Chaque nouvelle variante doit etre ajoutee seule, testee visuellement, eviter
 * les grandes pieces, eviter les cellules eclairees, respecter les connexions
 * de couloir et ne jamais creer de blocage ou de forme flottante au milieu du
 * passage.
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
     * @param enabled {@code true} pour autoriser les futures variantes
     */
    public LevelZeroLegacyGeometryStage(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Echantillonne les features geometriques fines d'une cellule.
     *
     * @param context contexte canonique de cellule
     * @param topology topologie fine deja derivee
     * @return masque de features geometriques
     */
    public int sample(LevelZeroCellContext context, LevelZeroCellTopology topology) {
        return sample(context, topology, LevelZeroCellConnections.none());
    }

    /**
     * Echantillonne les features geometriques fines d'une cellule avec son
     * contexte de connexions.
     *
     * <p>Pour l'instant, aucune variante n'est emise : le comportement runtime
     * reste identique a une couche desactivee, meme si le toggle est actif.
     *
     * @param context contexte canonique de cellule
     * @param topology topologie fine deja derivee
     * @param connectionMask sorties cardinales de la cellule
     * @return masque vide tant que les variantes ne sont pas reconstruites
     */
    public int sample(LevelZeroCellContext context, LevelZeroCellTopology topology, int connectionMask) {
        if (!enabled) {
            return LevelZeroGeometryMask.none();
        }
        return LevelZeroGeometryMask.none();
    }
}
