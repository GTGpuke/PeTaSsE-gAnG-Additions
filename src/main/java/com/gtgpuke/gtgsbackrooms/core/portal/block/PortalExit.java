package com.gtgpuke.gtgsbackrooms.core.portal.block;

import com.mojang.serialization.Codec;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.StringIdentifiable;

import com.gtgpuke.gtgsbackrooms.core.portal.entity.PortalFacing;

/**
 * Sens de sortie d'un bloc portail : la direction dans laquelle on ressort de son cadre d'arrivee. Tout droit par defaut
 * (la direction dans laquelle on est entre), un point cardinal fixe, ou le sol (on en sort vers le haut) ou le plafond
 * (on en tombe), quel que soit le sens d'entree. Le cadre d'arrivee regarde dans ce sens : sa normale pointe du cote ou
 * l'on ressort, celui par ou l'on revient.
 */
public enum PortalExit implements StringIdentifiable {
    STRAIGHT("straight", null),
    NORTH("north", PortalFacing.NORTH),
    EAST("east", PortalFacing.EAST),
    SOUTH("south", PortalFacing.SOUTH),
    WEST("west", PortalFacing.WEST),
    FLOOR("floor", PortalFacing.UP),
    CEILING("ceiling", PortalFacing.DOWN);

    /** Format du composant et de la sauvegarde : le nom en minuscules. */
    public static final Codec<PortalExit> CODEC = StringIdentifiable.createCodec(PortalExit::values);
    private static final PortalExit[] VALUES = values();

    private final String name;
    private final @Nullable PortalFacing facing;

    PortalExit(String name, @Nullable PortalFacing facing) {
        this.name = name;
        this.facing = facing;
    }

    @Override
    public String asString() {
        return this.name;
    }

    /**
     * Orientation du cadre d'arrivee pour une entree d'orientation donnee : le point cardinal choisi, ou, tout droit,
     * l'oppose de l'entree (on continue dans la direction ou l'on marchait).
     */
    public PortalFacing arrivalFacing(PortalFacing entryFacing) {
        return this.facing != null ? this.facing : entryFacing.opposite();
    }

    /** Sens de sortie d'un nom sauvegarde ; tout droit pour un nom inconnu ou absent. */
    public static PortalExit byName(String name) {
        for (PortalExit exit : VALUES) {
            if (exit.name.equals(name)) {
                return exit;
            }
        }
        return STRAIGHT;
    }
}
