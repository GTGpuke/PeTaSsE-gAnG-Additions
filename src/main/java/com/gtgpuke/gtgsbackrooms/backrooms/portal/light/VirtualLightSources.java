package com.gtgpuke.gtgsbackrooms.backrooms.portal.light;

import java.util.Arrays;

import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;

/**
 * Sources de lumiere virtuelles d'un monde, a un instant donne : des blocs qui eclairent sans etre lumineux. Immuables
 * une fois publiees ({@link VirtualLightView}) : le moteur de lumiere du serveur les lit sur son propre thread pendant que
 * le thread principal prepare les suivantes.
 */
public final class VirtualLightSources {

    /** Aucune source. */
    public static final VirtualLightSources EMPTY = new VirtualLightSources(new long[0], new byte[0], 0);

    private final long[] positions;
    private final byte[] luminances;
    private final Long2ByteOpenHashMap index;

    /**
     * Copie les {@code size} premieres sources donnees.
     *
     * @param positions positions des blocs ({@code BlockPos.asLong})
     * @param luminances luminosite de chaque bloc, de 1 a 15
     * @param size nombre de sources
     */
    public VirtualLightSources(long[] positions, byte[] luminances, int size) {
        this.positions = Arrays.copyOf(positions, size);
        this.luminances = Arrays.copyOf(luminances, size);
        this.index = new Long2ByteOpenHashMap(size);
        for (int i = 0; i < size; i++) {
            this.index.put(this.positions[i], this.luminances[i]);
        }
    }

    /** Luminosite virtuelle d'un bloc ({@code BlockPos.asLong}), zero s'il n'est pas une source. */
    public int luminance(long position) {
        return this.index.get(position);
    }

    /** Nombre de sources. */
    public int size() {
        return this.positions.length;
    }

    /** Position de la source d'indice donne. */
    public long position(int index) {
        return this.positions[index];
    }

    /** Luminosite de la source d'indice donne. */
    public int luminanceAt(int index) {
        return this.luminances[index];
    }
}
