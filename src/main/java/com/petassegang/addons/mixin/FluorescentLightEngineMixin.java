package com.petassegang.addons.mixin;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.BlockLightEngine;
import net.minecraft.world.level.lighting.BlockLightSectionStorage;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.level.chunk.LightChunkGetter;

import com.petassegang.addons.block.LevelZeroFluorescentLightBlock;

/**
 * Propagation lumineuse carrée pour les néons fluorescents du Level 0.
 *
 * <h3>Forme du faisceau (DOWN fixe)</h3>
 * <ul>
 *   <li>Colonne centrale 7×7 (|p1|≤3, |p2|≤3), profondeur d=1..4 : niveau 13.</li>
 *   <li>Halo Chebyshev au-delà du cœur :
 *       {@code level = 13 - 2*chebExtra - 2*depthExtra}, décroissance de 2 par
 *       anneau ou par tranche de profondeur supplémentaire.</li>
 *   <li>Portée totale : d ∈ [1,10], |p1|,|p2| ∈ [0, 9-depthExtra].</li>
 * </ul>
 *
 * <h3>Stratégie de nettoyage (pré-effacement)</h3>
 * <p>La contrainte habituelle (niveau-2 entre voisins face-adjacents) n'est pas
 * applicable ici : un cube plat à niveau 13 crée des saddle-points que vanilla
 * {@code propagateDecrease} ne peut pas traverser.
 *
 * <p>Solution : {@link #petassegang$preCleanBeam} s'injecte en tête de
 * {@code propagateDecrease}. Dès qu'un néon connu est décrémenté, TOUS ses blocs
 * de faisceau sont immédiatement mis à 0 <em>avant</em> que vanilla ne touche quoi
 * que ce soit. Vanilla voit alors des niveaux 0 partout → skip → aucun re-fill →
 * aucune sphère vanilla.
 *
 * <p>Les positions de néons actifs sont tracées par instance (champ
 * {@code petassegang$activeNeons}), ce qui fonctionne correctement dans les
 * environnements multi-dimensions (une instance de {@code BlockLightEngine}
 * par dimension).
 */
@Mixin(value = BlockLightEngine.class, remap = false)
@SuppressWarnings({"rawtypes", "unchecked"})
abstract class FluorescentLightEngineMixin extends LightEngine {

    @Shadow @Final
    private BlockPos.MutableBlockPos mutablePos;

    /** Positions des néons actifs pour cette instance de moteur lumière. */
    @Unique
    private final LongOpenHashSet petassegang$activeNeons = new LongOpenHashSet();

    protected FluorescentLightEngineMixin(LightChunkGetter chunkSource, BlockLightSectionStorage storage) {
        super(chunkSource, storage);
    }

    // -------------------------------------------------------------------------
    // Propagation increase
    // -------------------------------------------------------------------------

    /**
     * @author PétasseGang
     * @reason Remplace la propagation vanilla par un faisceau carré 7×7 à niveau 13
     *         pour les néons. Pré-enregistre la position du néon dans
     *         {@code activeNeons} pour le nettoyage futur.
     */
    @Overwrite
    protected void propagateIncrease(final long fromNode, final long increaseData, final int fromLevel) {
        BlockLightSectionStorageAccessor storage = (BlockLightSectionStorageAccessor) this.storage;

        // Check anti-périmé : le nœud a été modifié depuis l'enqueue.
        // Le decrease a déjà remis le niveau à 0 → abandon immédiat.
        if (storage.petassegang$getStoredLevel(fromNode) != fromLevel) {
            return;
        }

        BlockState fromState = this.getState(this.mutablePos.set(fromNode));

        if (isFluorescentEmitter(fromState)) {
            petassegang$activeNeons.add(fromNode);
            petassegang$propagateBeam(fromNode, storage);
            return;
        }

        if (petassegang$isInNeonBeam(fromNode)) {
            return;
        }

        // Propagation vanilla intacte pour tous les autres blocs.
        BlockState fromStateForOcclusion = LightEngine.QueueEntry.isFromEmptyShape(increaseData)
                ? Blocks.AIR.defaultBlockState()
                : fromState;

        for (Direction dir : PROPAGATION_DIRECTIONS) {
            if (!LightEngine.QueueEntry.shouldPropagateInDirection(increaseData, dir)) {
                continue;
            }
            long toNode = BlockPos.offset(fromNode, dir);
            if (!storage.petassegang$storingLightForSection(SectionPos.blockToSection(toNode))) {
                continue;
            }
            int toLevel = storage.petassegang$getStoredLevel(toNode);
            if (fromLevel - 1 <= toLevel) {
                continue;
            }
            this.mutablePos.set(toNode);
            BlockState toState = this.getState(this.mutablePos);
            int newToLevel = fromLevel - this.getOpacity(toState);
            if (newToLevel <= toLevel) {
                continue;
            }
            if (this.shapeOccludes(fromStateForOcclusion, toState, dir)) {
                continue;
            }
            storage.petassegang$setStoredLevel(toNode, newToLevel);
            if (newToLevel > 1) {
                this.enqueueIncrease(toNode, LightEngine.QueueEntry.increaseSkipOneDirection(
                        newToLevel, isEmptyShape(toState), dir.getOpposite()));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Pré-effacement injecté dans propagateDecrease
    // -------------------------------------------------------------------------

    /**
     * Injecté en tête de {@code propagateDecrease}.
     *
     * <p>Si le nœud est un néon connu, pré-efface tous les blocs du faisceau
     * à 0 AVANT que vanilla ne procède au decrease. Vanilla trouve alors des
     * niveaux 0 partout, skip tout → aucun re-fill → pas de sphère vanilla.
     */
    @Inject(method = "propagateDecrease", at = @At("HEAD"))
    private void petassegang$preCleanBeam(long fromNode, long decreaseData, CallbackInfo ci) {
        if (!petassegang$activeNeons.remove(fromNode)) {
            return;
        }
        BlockLightSectionStorageAccessor storage = (BlockLightSectionStorageAccessor) this.storage;
        petassegang$clearBeam(fromNode, storage);
    }

    // -------------------------------------------------------------------------
    // Helpers privés
    // -------------------------------------------------------------------------

    /**
     * Applique le faisceau lumineux vers le bas depuis {@code neonNode}.
     *
     * <p>Formule : {@code level = 13 - 2*chebExtra - 2*depthExtra}, avec
     * {@code chebExtra = max(0, max(|p1|,|p2|) - 3)} et
     * {@code depthExtra = max(0, d - 4)}.
     * Les bornes de boucle garantissent {@code level ∈ [1, 13]} sans skip interne.
     */
    @Unique
    private void petassegang$propagateBeam(long neonNode, BlockLightSectionStorageAccessor storage) {
        int neonX = BlockPos.getX(neonNode);
        int neonY = BlockPos.getY(neonNode);
        int neonZ = BlockPos.getZ(neonNode);

        for (int d = 1; d <= 10; d++) {
            int depthExtra = Math.max(0, d - 4);
            int maxP = 9 - depthExtra;
            if (maxP < 0) break;

            for (int p1 = -maxP; p1 <= maxP; p1++) {
                for (int p2 = -maxP; p2 <= maxP; p2++) {
                    int chebExtra = Math.max(0, Math.max(Math.abs(p1), Math.abs(p2)) - 3);
                    int targetLevel = 13 - 2 * chebExtra - 2 * depthExtra;

                    long targetNode = BlockPos.asLong(neonX + p1, neonY - d, neonZ + p2);

                    if (!storage.petassegang$storingLightForSection(SectionPos.blockToSection(targetNode))) {
                        continue;
                    }

                    this.mutablePos.set(targetNode);
                    int effectiveLevel = targetLevel - this.getOpacity(this.getState(this.mutablePos));
                    if (effectiveLevel <= 0) {
                        continue;
                    }

                    if (effectiveLevel > storage.petassegang$getStoredLevel(targetNode)) {
                        storage.petassegang$setStoredLevel(targetNode, effectiveLevel);
                    }
                }
            }
        }
    }

    /**
     * Remet à 0 tous les blocs du faisceau du néon en {@code neonNode}.
     * Itère exactement les mêmes positions que {@link #petassegang$propagateBeam}.
     */
    @Unique
    private void petassegang$clearBeam(long neonNode, BlockLightSectionStorageAccessor storage) {
        int neonX = BlockPos.getX(neonNode);
        int neonY = BlockPos.getY(neonNode);
        int neonZ = BlockPos.getZ(neonNode);

        for (int d = 1; d <= 10; d++) {
            int depthExtra = Math.max(0, d - 4);
            int maxP = 9 - depthExtra;
            if (maxP < 0) break;

            for (int p1 = -maxP; p1 <= maxP; p1++) {
                for (int p2 = -maxP; p2 <= maxP; p2++) {
                    long targetNode = BlockPos.asLong(neonX + p1, neonY - d, neonZ + p2);
                    if (storage.petassegang$storingLightForSection(SectionPos.blockToSection(targetNode))) {
                        storage.petassegang$setStoredLevel(targetNode, 0);
                    }
                }
            }
        }
    }

    /**
     * Retourne {@code true} si le nœud se trouve directement sous un néon actif
     * (axe central, distance ≤ 10 blocs vers le haut).
     */
    @Unique
    private boolean petassegang$isInNeonBeam(long blockNode) {
        long checkNode = blockNode;
        for (int i = 1; i <= 10; i++) {
            checkNode = BlockPos.offset(checkNode, Direction.UP);
            this.mutablePos.set(checkNode);
            BlockState state = this.getState(this.mutablePos);
            if (isFluorescentEmitter(state)) {
                return true;
            }
            if (!state.isAir()) {
                break;
            }
        }
        return false;
    }

    private static boolean isFluorescentEmitter(BlockState state) {
        return state.getBlock() instanceof LevelZeroFluorescentLightBlock
                && state.getValue(LevelZeroFluorescentLightBlock.LIT)
                && !state.getValue(LevelZeroFluorescentLightBlock.BROKEN);
    }
}
