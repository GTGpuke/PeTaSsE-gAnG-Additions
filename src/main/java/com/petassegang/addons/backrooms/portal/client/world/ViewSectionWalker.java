package com.petassegang.addons.backrooms.portal.client.world;

import java.util.BitSet;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.client.render.BuiltChunkStorage;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.chunk.ChunkManager;

import com.petassegang.addons.backrooms.portal.client.view.ViewCone;
import com.petassegang.addons.mixin.client.BuiltChunkStorageAccessor;

/**
 * Choisit, de maniere synchrone, les sections a dessiner dans une vue de portail : parcours en largeur depuis la
 * section de la camera, limite aux sections qui touchent le frustum et le volume visible a travers le cadre, et dont
 * les chunks voisins sont charges (comme vanilla, une section sans voisin ne se construit pas).
 *
 * <p>Le parcours asynchrone de vanilla ({@code ChunkRenderingDataPreparer}) ne convient pas aux vues : il tient un
 * seul graphe par renderer, le recalcule en arriere-plan quand la camera change de section, et filtre entre-temps
 * l'ancien graphe par le nouveau frustum. Deux vues du meme monde dans une image (deux portes, un portail imbrique)
 * alterneraient donc des listes de sections incompletes : des pans de la vue clignoteraient. Ici chaque vue fait son
 * propre parcours, sans etat partage et sans allocation une fois les tampons dimensionnes. Les sections sortent dans
 * l'ordre du parcours, de la plus proche a la plus lointaine, celui qu'attend le rendu des couches translucides.
 */
final class ViewSectionWalker {

    private static final Direction[] DIRECTIONS = Direction.values();
    /** Demi-section, en blocs : le milieu d'une section, d'ou part le parcours hors des limites du monde, comme vanilla. */
    static final int HALF_SECTION = 8;

    private final BlockPos.Mutable scratch = new BlockPos.Mutable();
    private final BitSet visited = new BitSet();
    private int[] queue = new int[0];

    /**
     * Remplit {@code out} avec les sections a dessiner.
     *
     * @param storage grille de sections du renderer de la vue
     * @param chunkManager chunks du monde de la vue
     * @param cameraPos position en blocs de la camera de la vue
     * @param frustum frustum de la vue, deja positionne
     * @param cone volume visible a travers le cadre
     * @param out liste des sections, videe puis remplie
     */
    void discover(BuiltChunkStorage storage, ChunkManager chunkManager, BlockPos cameraPos, Frustum frustum, ViewCone cone,
                  ObjectArrayList<ChunkBuilder.BuiltChunk> out) {
        out.clear();
        ChunkBuilder.BuiltChunk[] chunks = storage.chunks;
        if (this.queue.length < chunks.length) {
            this.queue = new int[chunks.length];
        }
        this.visited.clear();
        BuiltChunkStorageAccessor accessor = (BuiltChunkStorageAccessor) storage;
        int tail = 0;
        ChunkBuilder.BuiltChunk start = accessor.backrooms$getRenderedChunk(cameraPos);
        if (start != null && contains(start, cameraPos)) {
            this.visited.set(start.index);
            this.queue[tail++] = start.index;
        } else {
            tail = enqueueBoundaryLayer(storage, accessor, cameraPos, frustum, cone, chunkManager, tail);
        }
        int head = 0;
        while (head < tail) {
            ChunkBuilder.BuiltChunk chunk = chunks[this.queue[head++]];
            out.add(chunk);
            for (Direction direction : DIRECTIONS) {
                BlockPos neighborPos = chunk.getNeighborPosition(direction);
                ChunkBuilder.BuiltChunk neighbor = accessor.backrooms$getRenderedChunk(neighborPos);
                if (neighbor == null || this.visited.get(neighbor.index)) {
                    continue;
                }
                this.visited.set(neighbor.index);
                // Une section d'une autre origine occupe cet emplacement : la grille ne couvre pas la position voulue.
                if (neighbor.getOrigin().equals(neighborPos) && accept(neighbor, frustum, cone, chunkManager)) {
                    this.queue[tail++] = neighbor.index;
                }
            }
        }
    }

    /** Camera au-dessus ou au-dessous du monde : le parcours part de la couche de sections la plus proche, comme vanilla. */
    private int enqueueBoundaryLayer(
            BuiltChunkStorage storage,
            BuiltChunkStorageAccessor accessor,
            BlockPos cameraPos,
            Frustum frustum,
            ViewCone cone,
            ChunkManager chunkManager,
            int tail) {
        HeightLimitView height = storage.getWorld();
        int y = cameraPos.getY() > height.getBottomY() ? height.getTopY() - HALF_SECTION : height.getBottomY() + HALF_SECTION;
        int centerX = ChunkSectionPos.getSectionCoord(cameraPos.getX());
        int centerZ = ChunkSectionPos.getSectionCoord(cameraPos.getZ());
        int radius = storage.getViewDistance();
        int count = tail;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                this.scratch.set(ChunkSectionPos.getOffsetPos(centerX + dx, HALF_SECTION), y, ChunkSectionPos.getOffsetPos(centerZ + dz, HALF_SECTION));
                ChunkBuilder.BuiltChunk chunk = accessor.backrooms$getRenderedChunk(this.scratch);
                if (chunk == null || this.visited.get(chunk.index)) {
                    continue;
                }
                this.visited.set(chunk.index);
                if (contains(chunk, this.scratch) && accept(chunk, frustum, cone, chunkManager)) {
                    this.queue[count++] = chunk.index;
                }
            }
        }
        return count;
    }

    private static boolean accept(ChunkBuilder.BuiltChunk chunk, Frustum frustum, ViewCone cone, ChunkManager chunkManager) {
        return cone.intersects(chunk.getBoundingBox()) && frustum.isVisible(chunk.getBoundingBox()) && neighboursLoaded(chunk, chunkManager);
    }

    /**
     * {@code true} si les quatre chunks voisins de la section sont charges, comme vanilla l'exige avant de construire une
     * section ; sans l'exemption de vanilla autour de la camera du joueur, qui n'est pas celle de la vue.
     */
    private static boolean neighboursLoaded(ChunkBuilder.BuiltChunk chunk, ChunkManager chunkManager) {
        int chunkX = ChunkSectionPos.getSectionCoord(chunk.getOrigin().getX());
        int chunkZ = ChunkSectionPos.getSectionCoord(chunk.getOrigin().getZ());
        return chunkManager.isChunkLoaded(chunkX + 1, chunkZ) && chunkManager.isChunkLoaded(chunkX - 1, chunkZ)
                && chunkManager.isChunkLoaded(chunkX, chunkZ + 1) && chunkManager.isChunkLoaded(chunkX, chunkZ - 1);
    }

    /** {@code true} si la section stockee a cet emplacement est bien celle qui contient la position. */
    private static boolean contains(ChunkBuilder.BuiltChunk chunk, BlockPos pos) {
        BlockPos origin = chunk.getOrigin();
        return (pos.getX() >> 4) == (origin.getX() >> 4) && (pos.getY() >> 4) == (origin.getY() >> 4) && (pos.getZ() >> 4) == (origin.getZ() >> 4);
    }
}
