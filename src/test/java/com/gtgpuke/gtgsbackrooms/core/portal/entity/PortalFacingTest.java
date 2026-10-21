package com.gtgpuke.gtgsbackrooms.core.portal.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

@DisplayName("Orientation des portails")
class PortalFacingTest {

    private static final double EPS = 1.0e-9;

    @Test
    @DisplayName("Chaque orientation a une base orthonormee directe dont la normale suit sa direction")
    void axesAreOrthonormal() {
        for (PortalFacing facing : PortalFacing.values()) {
            assertEquals(1.0, facing.axisW.length(), EPS, facing + " : axisW unitaire.");
            assertEquals(1.0, facing.axisH.length(), EPS, facing + " : axisH unitaire.");
            assertEquals(0.0, facing.axisW.dotProduct(facing.axisH), EPS, facing + " : W et H orthogonaux.");
            assertEquals(0.0, facing.axisW.crossProduct(facing.axisH).subtract(facing.normal).length(), EPS, facing + " : normale = W x H.");
            assertEquals(0.0, Vec3d.of(facing.direction.getVector()).subtract(facing.normal).length(), EPS,
                    facing + " : la normale est la direction cardinale.");
            assertEquals(facing, PortalFacing.fromDirection(facing.direction), facing + " : aller-retour direction.");
        }
    }

    static Stream<Arguments> allPairs() {
        Stream.Builder<Arguments> builder = Stream.builder();
        for (PortalFacing source : PortalFacing.values()) {
            for (PortalFacing destination : PortalFacing.values()) {
                builder.add(Arguments.of(source, destination));
            }
        }
        return builder.build();
    }

    @ParameterizedTest(name = "{0} vers {1}")
    @MethodSource("allPairs")
    @DisplayName("Le passage est une rotation : longueurs conservees, points et vecteurs coherents, regard coherent")
    void transformIsRigid(PortalFacing source, PortalFacing destination) {
        Vec3d velocity = new Vec3d(0.3, -0.5, 0.7);
        assertEquals(velocity.length(), PortalFacing.transformVector(source, destination, velocity).length(), EPS,
                "La vitesse garde sa norme.");

        Vec3d sourceCenter = new Vec3d(10.0, 64.0, 20.0);
        Vec3d destinationCenter = new Vec3d(100.0, 30.0, -50.0);
        Vec3d offset = new Vec3d(1.0, 2.0, 3.0);
        Vec3d transformedOffset = PortalFacing.transformPoint(source, sourceCenter, destination, destinationCenter,
                sourceCenter.add(offset)).subtract(destinationCenter);
        assertEquals(0.0, transformedOffset.subtract(PortalFacing.transformVector(source, destination, offset)).length(), EPS,
                "Le point transforme suit la transformation du vecteur.");

        Vec3d a = PortalFacing.transformVector(source, destination, source.axisW);
        Vec3d b = PortalFacing.transformVector(source, destination, source.axisH);
        assertEquals(0.0, a.crossProduct(b).subtract(PortalFacing.transformVector(source, destination, source.normal)).length(), EPS,
                "L'orientation de l'espace est conservee : pas de miroir.");

        for (float[] sample : new float[][] {{0f, 0f}, {90f, 0f}, {-135f, 45f}, {0f, 80f}}) {
            Vec3d expectedLook = PortalFacing.transformVector(source, destination, lookVector(sample[0], sample[1]));
            Vec2f look = PortalFacing.transformLook(source, destination, sample[0], sample[1]);
            assertEquals(0.0, expectedLook.subtract(lookVector(look.x, look.y)).length(), 1.0e-6,
                    "Le regard transforme correspond au vecteur transforme.");
        }
    }

    @ParameterizedTest(name = "{0} vers {1}")
    @MethodSource("allPairs")
    @DisplayName("Ce qui est devant la source est derriere la destination, et entrer par l'une fait sortir par l'autre")
    void frontBecomesBackAndEnteringExits(PortalFacing source, PortalFacing destination) {
        Vec3d sourceCenter = new Vec3d(0.0, 64.0, 0.0);
        Vec3d destinationCenter = new Vec3d(50.0, 10.0, 50.0);
        Vec3d inFront = sourceCenter.add(source.normal.multiply(2.0));
        Vec3d mapped = PortalFacing.transformPoint(source, sourceCenter, destination, destinationCenter, inFront);
        assertEquals(0.0, mapped.subtract(destinationCenter.subtract(destination.normal.multiply(2.0))).length(), EPS,
                "Deux blocs devant la source = deux blocs derriere la destination.");

        Vec3d entering = source.normal.multiply(-0.5);
        assertEquals(0.0, PortalFacing.transformVector(source, destination, entering)
                .subtract(destination.normal.multiply(0.5)).length(), EPS, "Entrer dans la source, c'est sortir de la destination.");
        assertEquals(0.0, PortalFacing.transformVector(source, destination, source.axisH).subtract(destination.axisH).length(), EPS,
                "Le haut reste le haut.");
        assertEquals(0.0, PortalFacing.transformVector(source, destination, source.axisW).add(destination.axisW).length(), EPS,
                "La largeur est retournee avec la profondeur.");
    }

    @Test
    @DisplayName("Un joueur qui entre par le nord dans un portail nord ressort face a la sortie, sa droite toujours a droite")
    void lookAndHandednessThroughAPortal() {
        // Regarder vers le sud (yaw 0) dans un portail oriente nord, sortie orientee est.
        Vec2f look = PortalFacing.transformLook(PortalFacing.NORTH, PortalFacing.EAST, 0.0f, 10.0f);
        assertEquals(-90.0f, look.x, 1.0e-4f, "Le regard ressort vers l'est.");
        assertEquals(10.0f, look.y, 1.0e-4f, "Le tangage est conserve.");

        // Un point a droite du joueur (ouest quand il regarde vers le sud) reste a sa droite (sud quand il regarde vers l'est).
        Vec3d right = PortalFacing.transformVector(PortalFacing.NORTH, PortalFacing.EAST, new Vec3d(-1.0, 0.0, 0.0));
        assertEquals(0.0, right.subtract(new Vec3d(0.0, 0.0, 1.0)).length(), EPS, "La droite du joueur reste sa droite.");

        // Nord vers sud : demi-tour complet, le regard garde son yaw.
        Vec2f straight = PortalFacing.transformLook(PortalFacing.NORTH, PortalFacing.SOUTH, 0.0f, 0.0f);
        assertEquals(0.0f, straight.x, 1.0e-4f, "Entrer vers le sud et ressortir vers le sud.");
    }

    @Test
    @DisplayName("Un portail au sol transforme une chute en sortie horizontale")
    void upToNorth() {
        Vec3d velocity = PortalFacing.transformVector(PortalFacing.UP, PortalFacing.NORTH, new Vec3d(0.0, -0.5, 0.0));
        assertEquals(0.5, velocity.length(), EPS, "Norme conservee.");
        assertTrue(velocity.z < 0.0, "Tomber dans un portail au sol fait ressortir vers le nord, cote sortie.");
        // Du sol au plafond, tout droit : une translation, ni demi-tour ni miroir.
        Vec3d falling = new Vec3d(0.3, -0.5, -0.2);
        assertEquals(0.0, PortalFacing.transformVector(PortalFacing.UP, PortalFacing.DOWN, falling).subtract(falling).length(), EPS,
                "Tomber dans un cadre au sol et ressortir d'un plafond garde vitesse et cap.");
        assertEquals(0.0, PortalFacing.transformVector(PortalFacing.DOWN, PortalFacing.UP, falling.multiply(-1.0)).add(falling).length(), EPS,
                "Monter dans un cadre au plafond et ressortir d'un sol aussi.");
        Vec2f look = PortalFacing.transformLook(PortalFacing.UP, PortalFacing.DOWN, 180.0f, 30.0f);
        assertEquals(0.0f, MathHelper.wrapDegrees(look.x - 180.0f), 1.0e-3f, "Le regard garde son cap.");
        assertEquals(30.0f, look.y, 1.0e-3f, "Et son inclinaison.");
    }

    @Test
    @DisplayName("Un regard qui devient vertical garde le cap de qui baissera ou levera les yeux")
    void verticalLookKeepsAMeaningfulHeading() {
        // Tombe dans un sol en regardant l'horizon au nord, sorti d'un mur au nord (on file vers le nord) : on regarde le
        // ciel, et en baissant les yeux on regarde le nord, la ou l'on va.
        Vec2f up = PortalFacing.transformLook(PortalFacing.UP, PortalFacing.NORTH, 180.0f, 0.0f);
        assertEquals(-90.0f, up.y, 1.0e-3f, "Le regard monte au ciel.");
        assertEquals(0.0f, MathHelper.wrapDegrees(up.x - 180.0f), 1.0e-3f, "Le cap est le nord.");
        // Monte dans un plafond en regardant l'horizon au nord, sorti d'un mur a l'est : le nord du plafond devient le ciel.
        // Le haut de l'ecran reste vers l'est, ou l'on file : l'image ne tourne pas a la traversee (cap a l'ouest, qui
        // regarde le ciel a le haut de l'ecran derriere lui).
        Vec2f rising = PortalFacing.transformLook(PortalFacing.DOWN, PortalFacing.EAST, 180.0f, 0.0f);
        assertEquals(-90.0f, rising.y, 1.0e-3f, "Le regard monte au ciel.");
        assertEquals(0.0f, MathHelper.wrapDegrees(rising.x - 90.0f), 1.0e-3f, "Cap a l'ouest : le haut de l'ecran vers l'est.");
        // Tombe dans un sol en regardant le sol, la tete vers le nord, sorti d'un sol au sud : on regarde le ciel.
        Vec2f looking = PortalFacing.transformLook(PortalFacing.UP, PortalFacing.UP, 180.0f, 90.0f);
        assertEquals(-90.0f, looking.y, 1.0e-3f, "Le regard monte au ciel.");
    }

    @Test
    @DisplayName("Une direction inconnue est refusee")
    void fromDirectionRejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> PortalFacing.fromDirection(null), "Null est refuse.");
        assertEquals(PortalFacing.UP, PortalFacing.fromDirection(Direction.UP), "UP est reconnu.");
    }

    private static Vec3d lookVector(float yaw, float pitch) {
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        double cosPitch = Math.cos(pitchRad);
        return new Vec3d(-Math.sin(yawRad) * cosPitch, -Math.sin(pitchRad), Math.cos(yawRad) * cosPitch);
    }

    @Test
    @DisplayName("La face arriere d'une orientation a la normale opposee, et l'opposee de l'opposee est l'orientation")
    void oppositeFlipsTheNormal() {
        for (PortalFacing facing : PortalFacing.values()) {
            PortalFacing back = facing.opposite();
            assertEquals(0.0, back.normal.add(facing.normal).length(), EPS, facing + " : normales opposees.");
            assertEquals(facing, back.opposite(), facing + " : aller-retour.");
        }
    }

    @Test
    @DisplayName("Horizontalement, deux portails dos a dos vers les faces correspondantes font la meme transformation : un trou")
    void backPairMatchesTheFrontPair() {
        PortalFacing[] horizontal = {PortalFacing.NORTH, PortalFacing.EAST, PortalFacing.SOUTH, PortalFacing.WEST};
        Vec3d[] vectors = {new Vec3d(0.3, -1.2, 2.5), new Vec3d(-4.0, 0.5, 0.25), new Vec3d(1.0, 1.0, -1.0)};
        for (PortalFacing source : horizontal) {
            for (PortalFacing destination : horizontal) {
                for (Vec3d vector : vectors) {
                    Vec3d front = PortalFacing.transformVector(source, destination, vector);
                    Vec3d back = PortalFacing.transformVector(source.opposite(), destination.opposite(), vector);
                    assertEquals(0.0, front.subtract(back).length(), EPS, source + " -> " + destination + " : meme image par les deux faces.");
                }
            }
        }
    }
}
