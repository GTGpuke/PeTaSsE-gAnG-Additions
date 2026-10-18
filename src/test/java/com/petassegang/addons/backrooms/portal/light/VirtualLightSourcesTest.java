package com.petassegang.addons.backrooms.portal.light;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.util.math.BlockPos;

@DisplayName("Sources de lumiere virtuelles")
class VirtualLightSourcesTest {

    @Test
    @DisplayName("Les sources copient les tampons donnes : on les lit par position et par indice, rien ailleurs")
    void sourcesAreAnImmutableCopy() {
        long first = BlockPos.asLong(1, 2, 3);
        long second = BlockPos.asLong(-4, 70, 9);
        long[] positions = {first, second, 0L};
        byte[] luminances = {12, 7, 15};
        VirtualLightSources sources = new VirtualLightSources(positions, luminances, 2);
        positions[0] = 0L;
        luminances[0] = 1;
        assertEquals(2, sources.size(), "Seules les deux premieres entrees comptent.");
        assertEquals(first, sources.position(0), "Les tampons modifies ensuite ne changent rien.");
        assertEquals(12, sources.luminanceAt(0), "Luminosite par indice.");
        assertEquals(7, sources.luminance(second), "Luminosite par position.");
        assertEquals(0, sources.luminance(BlockPos.asLong(0, 0, 0)), "Un bloc qui n'est pas une source.");
        assertEquals(0, VirtualLightSources.EMPTY.size(), "Aucune source.");
        assertEquals(0, VirtualLightSources.EMPTY.luminance(first), "Rien n'eclaire.");
    }
}
