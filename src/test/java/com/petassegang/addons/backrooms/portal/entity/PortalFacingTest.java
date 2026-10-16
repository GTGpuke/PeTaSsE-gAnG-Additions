package com.petassegang.addons.backrooms.portal.entity;

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
        assertEquals(velocity.length(), PortalFacing.transformVelocity(source, destination, velocity).length(), EPS,
                "La vitesse garde sa norme.");

        Vec3d sourceCenter = new Vec3d(10.0, 64.0, 20.0);
        Vec3d destinationCenter = new Vec3d(100.0, 30.0, -50.0);
        Vec3d offset = new Vec3d(1.0, 2.0, 3.0);
        Vec3d transformedOffset = PortalFacing.transformPoint(source, sourceCenter, destination, destinationCenter,
                sourceCenter.add(offset)).subtract(destinationCenter);
        assertEquals(0.0, transformedOffset.subtract(PortalFacing.transformVelocity(source, destination, offset)).length(), EPS,
                "Le point transforme suit la transformation du vecteur.");

        Vec3d a = PortalFacing.transformVelocity(source, destination, source.axisW);
        Vec3d b = PortalFacing.transformVelocity(source, destination, source.axisH);
        assertEquals(0.0, a.crossProduct(b).subtract(PortalFacing.transformVelocity(source, destination, source.normal)).length(), EPS,
                "L'orientation de l'espace est conservee : pas de miroir.");

        for (float[] sample : new float[][] {{0f, 0f}, {90f, 0f}, {-135f, 45f}, {0f, 80f}}) {
            Vec3d expectedLook = PortalFacing.transformVelocity(source, destination, lookVector(sample[0], sample[1]));
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
        assertEquals(0.0, PortalFacing.transformVelocity(source, destination, entering)
                .subtract(destination.normal.multiply(0.5)).length(), EPS, "Entrer dans la source, c'est sortir de la destination.");
        assertEquals(0.0, PortalFacing.transformVelocity(source, destination, source.axisH).subtract(destination.axisH).length(), EPS,
                "Le haut reste le haut.");
        assertEquals(0.0, PortalFacing.transformVelocity(source, destination, source.axisW).add(destination.axisW).length(), EPS,
                "La largeur est retournee avec la profondeur.");
    }

    @Test
    @DisplayName("Un joueur qui entre face au nord dans une porte nord ressort face a la sortie, sa droite toujours a droite")
    void lookAndHandednessThroughADoor() {
        // Regarder vers le sud (yaw 0) dans un portail oriente nord, sortie orientee est.
        Vec2f look = PortalFacing.transformLook(PortalFacing.NORTH, PortalFacing.EAST, 0.0f, 10.0f);
        assertEquals(-90.0f, look.x, 1.0e-4f, "Le regard ressort vers l'est.");
        assertEquals(10.0f, look.y, 1.0e-4f, "Le tangage est conserve.");

        // Un point a droite du joueur (ouest quand il regarde vers le sud) reste a sa droite (sud quand il regarde vers l'est).
        Vec3d right = PortalFacing.transformVelocity(PortalFacing.NORTH, PortalFacing.EAST, new Vec3d(-1.0, 0.0, 0.0));
        assertEquals(0.0, right.subtract(new Vec3d(0.0, 0.0, 1.0)).length(), EPS, "La droite du joueur reste sa droite.");

        // Nord vers sud : demi-tour complet, le regard garde son yaw.
        Vec2f straight = PortalFacing.transformLook(PortalFacing.NORTH, PortalFacing.SOUTH, 0.0f, 0.0f);
        assertEquals(0.0f, straight.x, 1.0e-4f, "Entrer vers le sud et ressortir vers le sud.");
    }

    @Test
    @DisplayName("Un portail au sol transforme une chute en sortie horizontale")
    void upToNorth() {
        Vec3d velocity = PortalFacing.transformVelocity(PortalFacing.UP, PortalFacing.NORTH, new Vec3d(0.0, -0.5, 0.0));
        assertEquals(0.5, velocity.length(), EPS, "Norme conservee.");
        assertTrue(velocity.z < 0.0, "Tomber dans un portail au sol fait ressortir vers le nord, cote sortie.");
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
}
