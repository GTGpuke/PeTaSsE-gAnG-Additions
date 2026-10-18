package com.gtgpuke.gtgsbackrooms.backrooms.level0.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Verrou de non-regression : l'empreinte de la generation logique d'une zone de 7x7 chunks
 * doit rester identique pour chaque seed et chaque layer.
 *
 * <p>Si ce test echoue, la generation du Level 0 a change. Soit c'est voulu
 * (nouvelle feature de generation, correction de bug) et les empreintes sont a
 * regenerer en relancant le test et en copiant les valeurs attendues ; soit c'est
 * une regression a corriger. Dans tous les cas, le changement doit etre annonce
 * dans le CHANGELOG : il modifie les mondes existants.
 */
@DisplayName("Empreintes de generation du Level 0")
class LevelZeroGoldenTest {

    static Stream<Arguments> goldenFingerprints() {
        return Stream.of(
                Arguments.of(12345L, 0, 2437600832569276195L),
                Arguments.of(12345L, 1, 5611975564148600128L),
                Arguments.of(12345L, 2, 6439155928378625376L),
                Arguments.of(12345L, 3, 6452912226331276387L),
                Arguments.of(12345L, 4, 686506886541240923L),
                Arguments.of(998877L, 0, 7072164592281014793L),
                Arguments.of(998877L, 1, 611718385357990995L),
                Arguments.of(998877L, 2, -3041910164664559086L),
                Arguments.of(998877L, 3, 9219491653957023304L),
                Arguments.of(998877L, 4, -6647712639191878404L),
                Arguments.of(13579L, 0, -5775090534720603914L),
                Arguments.of(13579L, 1, 2714368064516916110L),
                Arguments.of(13579L, 2, 3545893169376809553L),
                Arguments.of(13579L, 3, -639283899394307254L),
                Arguments.of(13579L, 4, -7817034848188341814L));
    }

    @ParameterizedTest(name = "seed {0}, layer {1}")
    @MethodSource("goldenFingerprints")
    @DisplayName("L'empreinte de generation est inchangee")
    void fingerprintIsStable(long worldSeed, int layerIndex, long expectedFingerprint) {
        assertEquals(expectedFingerprint, LevelZeroGoldenHash.hash(worldSeed, layerIndex),
                "La generation du Level 0 a change pour la seed " + worldSeed + " au layer " + layerIndex
                        + " : regression ou evolution a annoncer dans le CHANGELOG.");
    }
}
