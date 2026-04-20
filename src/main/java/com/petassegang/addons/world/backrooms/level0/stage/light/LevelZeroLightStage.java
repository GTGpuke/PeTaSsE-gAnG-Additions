package com.petassegang.addons.world.backrooms.level0.stage.light;

import com.petassegang.addons.world.backrooms.level0.LevelZeroSurfaceBiome;
import com.petassegang.addons.world.backrooms.level0.layout.sector.LevelZeroSectorRoomKind;
import com.petassegang.addons.world.backrooms.level0.noise.StageRandom;
import com.petassegang.addons.world.backrooms.level0.stage.LevelZeroCellContext;
import com.petassegang.addons.world.backrooms.level0.stage.LevelZeroCellStage;

/**
 * Etape historique de decision lumineuse du Level 0.
 *
 * <p>Cette classe dit si une cellule est candidate a porter un neon selon son
 * biome, son statut de grande piece et son type de salle. L'arbitrage final de
 * proximite entre voisins reste ensuite du ressort de la pipeline complete.
 */
public final class LevelZeroLightStage implements LevelZeroCellStage<Boolean> {

    private static final int LARGE_ROOM_GROUP_SIZE = 8;
    private static final int LARGE_ROOM_DEFAULT_GRID = 2;
    private static final int LARGE_ROOM_PILLAR_GRID = 3;
    private static final int LARGE_ROOM_BLACKOUT_MODULO = 14;

    /**
     * Construit l'etape historique de placement des neons.
     *
     * @param lightInterval modulo historique des neons
     */
    public LevelZeroLightStage(int lightInterval) {
        // Le modulo historique reste accepte pour conserver le contrat de la
        // pipeline legacy, meme si la logique actuelle s'appuie surtout sur les
        // trames lumineuses derivees des biomes.
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
     * @param roomKind type de salle legacy eventuellement remonte par le secteur
     * @return {@code true} si un neon doit etre place
     */
    public boolean sample(LevelZeroCellContext context,
                          LevelZeroSurfaceBiome surfaceBiome,
                          boolean largeRoom,
                          LevelZeroSectorRoomKind roomKind) {
        if (!sampleCandidate(context, surfaceBiome, largeRoom, roomKind)) {
            return false;
        }
        if (largeRoom) {
            return true;
        }
        if (hasWinningNeighbor(context, surfaceBiome)) {
            return false;
        }
        return true;
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
     * @param roomKind type de salle legacy eventuellement remonte par le secteur
     * @return {@code true} si la cellule est candidate a porter un neon
     */
    public boolean sampleCandidate(LevelZeroCellContext context,
                                   LevelZeroSurfaceBiome surfaceBiome,
                                   boolean largeRoom,
                                   LevelZeroSectorRoomKind roomKind) {
        if (largeRoom) {
            return sampleLargeRoomLight(context, roomKind);
        }
        return sampleBiomeLight(context, surfaceBiome);
    }

    private boolean hasWinningNeighbor(LevelZeroCellContext context,
                                       LevelZeroSurfaceBiome surfaceBiome) {
        return neighborWins(context, 1, 0)
                || neighborWins(context, -1, 0)
                || neighborWins(context, 0, 1)
                || neighborWins(context, 0, -1)
                || neighborWins(context, 1, 1)
                || neighborWins(context, 1, -1)
                || neighborWins(context, -1, 1)
                || neighborWins(context, -1, -1);
    }

    private boolean neighborWins(LevelZeroCellContext context,
                                 int offsetX,
                                 int offsetZ) {
        LevelZeroCellContext neighbor = new LevelZeroCellContext(
                context.cellX() + offsetX,
                context.cellZ() + offsetZ,
                context.layoutSeed(),
                context.layerIndex());
        LevelZeroSurfaceBiome neighborBiome = LevelZeroSurfaceBiome.sampleAtCell(
                neighbor.cellX(),
                neighbor.cellZ(),
                neighbor.layerIndex());
        if (!sampleBiomeLight(neighbor, neighborBiome)) {
            return false;
        }
        long selfScore = lightScore(context);
        long neighborScore = lightScore(neighbor);
        if (neighborScore != selfScore) {
            return neighborScore > selfScore;
        }
        if (neighbor.cellX() != context.cellX()) {
            return neighbor.cellX() > context.cellX();
        }
        return neighbor.cellZ() > context.cellZ();
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

    private boolean sampleBiomeLight(LevelZeroCellContext context, LevelZeroSurfaceBiome surfaceBiome) {
        if (surfaceBiome.isFullDarkRegion(context.cellX(), context.cellZ(), context.layoutSeed())) {
            return false;
        }
        int spacing = surfaceBiome.lightGridSpacing();
        // La lumiere normale n'est plus un simple roll local : elle doit tomber
        // sur une trame fixe par biome, puis subir un leger dropout pour casser
        // la regularite parfaite sans perdre l'alignement 3x3.
        if (Math.floorMod(context.cellX() - surfaceBiome.lightGridPhaseX(), spacing) != 0
                || Math.floorMod(context.cellZ() - surfaceBiome.lightGridPhaseZ(), spacing) != 0) {
            return false;
        }

        long dropoutHash = StageRandom.mixLegacy(
                context.layoutSeed(),
                StageRandom.Stage.LIGHTS,
                context.cellX(),
                context.cellZ());
        return Math.floorMod(dropoutHash, surfaceBiome.lightDropoutModulo()) != 0;
    }

    private boolean sampleLargeRoomLight(LevelZeroCellContext context,
                                         LevelZeroSectorRoomKind roomKind) {
        int groupX = Math.floorDiv(context.cellX(), LARGE_ROOM_GROUP_SIZE);
        int groupZ = Math.floorDiv(context.cellZ(), LARGE_ROOM_GROUP_SIZE);
        int localX = Math.floorMod(context.cellX(), LARGE_ROOM_GROUP_SIZE);
        int localZ = Math.floorMod(context.cellZ(), LARGE_ROOM_GROUP_SIZE);
        long groupHash = StageRandom.mixLegacy(
                context.layoutSeed(),
                StageRandom.Stage.LARGE_ROOM_LIGHTING,
                groupX,
                groupZ);

        if (Math.floorMod(groupHash, LARGE_ROOM_BLACKOUT_MODULO) == 0) {
            return false;
        }
        if (localX == 0 || localZ == 0) {
            return false;
        }

        // Les grandes pieces suivent un seul motif lisible par groupe :
        // une ligne ou une diagonale unique, jamais une repetition qui ferait
        // se coller deux cellules eclairees d'une meme piece.
        int samplingStep = switch (roomKind) {
            case PILLAR_ROOM -> LARGE_ROOM_PILLAR_GRID;
            case RECT_ROOM, CUSTOM_ROOM, NONE -> LARGE_ROOM_DEFAULT_GRID;
        };
        int offsetX = Math.floorMod(groupHash, LARGE_ROOM_GROUP_SIZE);
        int offsetZ = Math.floorMod(Long.rotateLeft(groupHash, 11), LARGE_ROOM_GROUP_SIZE);
        LargeRoomLightPattern pattern = LargeRoomLightPattern.fromHash(groupHash);
        return pattern.isLit(localX, localZ, samplingStep, offsetX, offsetZ);
    }

    private enum LargeRoomLightPattern {
        LINE_X {
            @Override
            boolean isLit(int localX, int localZ, int samplingStep, int offsetX, int offsetZ) {
                return localX == offsetX
                        && Math.floorMod(localZ - offsetZ, samplingStep) == 0;
            }
        },
        LINE_Z {
            @Override
            boolean isLit(int localX, int localZ, int samplingStep, int offsetX, int offsetZ) {
                return localZ == offsetZ
                        && Math.floorMod(localX - offsetX, samplingStep) == 0;
            }
        },
        DIAGONAL_MAIN {
            @Override
            boolean isLit(int localX, int localZ, int samplingStep, int offsetX, int offsetZ) {
                return localX - localZ == offsetX - offsetZ
                        && Math.floorMod(localX + localZ - offsetX - offsetZ, samplingStep) == 0;
            }
        },
        DIAGONAL_ANTI {
            @Override
            boolean isLit(int localX, int localZ, int samplingStep, int offsetX, int offsetZ) {
                return localX + localZ == offsetX + offsetZ
                        && Math.floorMod((localX - localZ) - (offsetX - offsetZ), samplingStep) == 0;
            }
        };

        abstract boolean isLit(int localX, int localZ, int samplingStep, int offsetX, int offsetZ);

        static LargeRoomLightPattern fromHash(long groupHash) {
            return switch (Math.floorMod(groupHash, 4)) {
                case 0 -> LINE_X;
                case 1 -> LINE_Z;
                case 2 -> DIAGONAL_MAIN;
                default -> DIAGONAL_ANTI;
            };
        }
    }
}
