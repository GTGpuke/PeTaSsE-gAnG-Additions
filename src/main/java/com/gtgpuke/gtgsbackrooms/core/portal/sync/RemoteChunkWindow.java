package com.gtgpuke.gtgsbackrooms.core.portal.sync;

import it.unimi.dsi.fastutil.longs.LongSet;

import net.minecraft.util.math.ChunkPos;

/**
 * Geometrie partagee entre le serveur et le client pour les chunks distants.
 *
 * <p>Le client range les chunks d'une dimension distante dans une carte carree centree sur le premier
 * chunk recu (regle vanilla : rayon = max(2, distance) + 3). Le serveur ne doit donc envoyer que des
 * chunks qui tiennent dans cette carte, quelle que soit la position de ce premier chunk dans la fenetre
 * du foyer : d'ou la marge de deux rayons de fenetre dans {@link #fitsAround}.
 */
public final class RemoteChunkWindow {

    /** Distance de chargement, en chunks, donnee au monde client distant. */
    public static final int LOAD_DISTANCE = 32;
    /** Rayon de la carte de chunks du client distant, en chunks. */
    public static final int MAP_RADIUS = Math.max(2, LOAD_DISTANCE) + 3;

    private RemoteChunkWindow() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /**
     * Ajoute a {@code out} les chunks de la fenetre carree, du centre vers l'exterieur ; un chunk deja present n'est pas
     * repete.
     *
     * @param centerX chunk central X
     * @param centerZ chunk central Z
     * @param radius rayon de la fenetre en chunks (0 = le seul chunk central)
     * @param out cles {@link ChunkPos#toLong(int, int)} a completer ; un ensemble ordonne garde l'ordre d'ajout
     */
    public static void collect(int centerX, int centerZ, int radius, LongSet out) {
        for (int ring = 0; ring <= radius; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
                        continue;
                    }
                    out.add(ChunkPos.toLong(centerX + dx, centerZ + dz));
                }
            }
        }
    }

    /**
     * {@code true} si une fenetre de rayon {@code radius} centree en ({@code x}, {@code z}) tient dans
     * la carte client, celle-ci etant centree quelque part dans la fenetre du foyer.
     *
     * @param anchorX chunk central du foyer X
     * @param anchorZ chunk central du foyer Z
     * @param x chunk central de la fenetre candidate X
     * @param z chunk central de la fenetre candidate Z
     * @param radius rayon des fenetres en chunks
     * @return {@code true} si tous les chunks de la fenetre candidate seront acceptes par le client
     */
    public static boolean fitsAround(int anchorX, int anchorZ, int x, int z, int radius) {
        int distance = Math.max(Math.abs(x - anchorX), Math.abs(z - anchorZ));
        return distance + 2 * radius <= MAP_RADIUS;
    }
}
