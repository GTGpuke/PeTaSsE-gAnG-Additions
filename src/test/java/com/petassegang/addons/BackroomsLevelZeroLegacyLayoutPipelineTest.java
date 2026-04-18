package com.petassegang.addons;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.petassegang.addons.world.backrooms.level0.LevelZeroSurfaceBiome;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroCellTopology;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroCellMicroPattern;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroGeometryFeature;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroGeometryMask;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroRegionWalkability;
import com.petassegang.addons.world.backrooms.level0.stage.LevelZeroCellContext;
import com.petassegang.addons.world.backrooms.level0.stage.LevelZeroCellEvaluation;
import com.petassegang.addons.world.backrooms.level0.stage.LevelZeroLegacyRegionWalkabilityStage;
import com.petassegang.addons.world.backrooms.level0.stage.LevelZeroLegacyLayoutPipeline;
import com.petassegang.addons.world.backrooms.level0.stage.LevelZeroRegionContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifie la pipeline explicite legacy du layout Level 0.
 */
@DisplayName("Pipeline legacy du layout Level 0")
class BackroomsLevelZeroLegacyLayoutPipelineTest {

    @Test
    @DisplayName("La pipeline legacy reste deterministe pour une cellule donnee")
    void testLegacyPipelineIsDeterministic() {
        LevelZeroLegacyLayoutPipeline pipeline = new LevelZeroLegacyLayoutPipeline(7);
        LevelZeroCellContext context = new LevelZeroCellContext(12, -5, 556677L);

        assertEquals(pipeline.sampleLight(context), pipeline.sampleLight(context),
                "L'etape lumiere doit rester deterministe.");
        assertEquals(pipeline.sampleSurfaceBiome(context), pipeline.sampleSurfaceBiome(context),
                "L'etape biome de surface doit rester deterministe.");
        assertEquals(pipeline.sampleLargeRoom(context), pipeline.sampleLargeRoom(context),
                "L'etape grande piece doit rester deterministe.");
        assertEquals(pipeline.sampleSectorCacheKey(3, -2, context.layoutSeed()),
                pipeline.sampleSectorCacheKey(3, -2, context.layoutSeed()),
                "La cle de cache secteur doit rester deterministe.");
    }

    @Test
    @DisplayName("Le biome de surface legacy reste aligne avec l'echantillonnage historique")
    void testLegacyPipelineMatchesSurfaceBiomeSampling() {
        LevelZeroLegacyLayoutPipeline pipeline = new LevelZeroLegacyLayoutPipeline(7);
        LevelZeroCellContext context = new LevelZeroCellContext(32, 48, 13579L);

        assertEquals(LevelZeroSurfaceBiome.sampleAtCell(context.cellX(), context.cellZ()),
                pipeline.sampleSurfaceBiome(context),
                "La pipeline legacy doit conserver le biome cosmetique historique.");
    }

    @Test
    @DisplayName("L'evaluation agregee de cellule reste coherente avec les etapes legacy")
    void testLegacyPipelineAggregatesCellStages() {
        LevelZeroLegacyLayoutPipeline pipeline = new LevelZeroLegacyLayoutPipeline(7);
        LevelZeroCellContext context = new LevelZeroCellContext(5, 9, 24680L);
        LevelZeroRegionWalkability walkability = new LevelZeroLegacyRegionWalkabilityStage((x, z) -> x == 5 && z >= 8 && z <= 10)
                .sample(new LevelZeroRegionContext(new com.petassegang.addons.world.backrooms.level0.layout.LevelZeroChunkCellWindow(0, 0, 4, 8, 6, 10), 24680L));
        LevelZeroCellEvaluation evaluation = pipeline.evaluateCell(context, walkability);

        assertEquals(context, evaluation.context(),
                "L'evaluation agregee doit conserver le contexte de cellule.");
        assertEquals(LevelZeroCellTopology.CORRIDOR, evaluation.topology(),
                "La topologie derivee doit reconnaitre un couloir droit simple.");
        assertEquals(pipeline.sampleSurfaceBiome(context), evaluation.surfaceBiome(),
                "L'evaluation agregee doit conserver le biome historique.");
        assertEquals(pipeline.sampleLargeRoom(context), evaluation.largeRoom(),
                "L'evaluation agregee doit conserver le marquage de grande piece historique.");
        assertEquals(pipeline.sampleLight(context), evaluation.lighted(),
                "L'evaluation agregee doit conserver le placement historique des neons.");
        assertEquals(false, LevelZeroGeometryMask.has(evaluation.geometryMask(), LevelZeroGeometryFeature.NONE),
                "Le masque geometrique ne doit jamais contenir la pseudo-feature NONE.");
        assertEquals(LevelZeroCellMicroPattern.FULL_OPEN, evaluation.microPattern(),
                "Un couloir sans anomalie geometrique doit garder un motif 3x3 pleinement ouvert.");
    }

    @Test
    @DisplayName("La pipeline peut desactiver la micro-geometrie pour comparer avant/apres")
    void testLegacyPipelineCanDisableNoiseGeometry() {
        LevelZeroLegacyLayoutPipeline disabledPipeline = new LevelZeroLegacyLayoutPipeline(7, false);
        LevelZeroLegacyLayoutPipeline enabledPipeline = new LevelZeroLegacyLayoutPipeline(7, true);
        LevelZeroRegionWalkability walkability = new LevelZeroLegacyRegionWalkabilityStage((x, z) -> x == 17 && z >= -4 && z <= -2)
                .sample(new LevelZeroRegionContext(new com.petassegang.addons.world.backrooms.level0.layout.LevelZeroChunkCellWindow(0, 0, 16, -4, 18, -2), 778899L));

        LevelZeroCellEvaluation disabledEvaluation = disabledPipeline.evaluateCell(
                new LevelZeroCellContext(17, -3, 778899L),
                walkability);
        LevelZeroCellEvaluation enabledEvaluation = enabledPipeline.evaluateCell(
                new LevelZeroCellContext(17, -3, 778899L),
                walkability);

        assertEquals(LevelZeroGeometryMask.none(), disabledEvaluation.geometryMask(),
                "La comparaison debug doit pouvoir couper totalement la micro-geometrie.");
        assertEquals(LevelZeroCellMicroPattern.FULL_OPEN, disabledEvaluation.microPattern(),
                "Sans micro-geometrie, une cellule de couloir doit redevenir pleinement ouverte.");
        assertTrue(enabledEvaluation.geometryMask() == LevelZeroGeometryMask.none()
                        || enabledEvaluation.microPattern() != LevelZeroCellMicroPattern.FULL_OPEN
                        || enabledEvaluation.geometryMask() != disabledEvaluation.geometryMask(),
                "La pipeline active doit rester libre de produire une variation geometrique sur la meme cellule.");
    }
}
