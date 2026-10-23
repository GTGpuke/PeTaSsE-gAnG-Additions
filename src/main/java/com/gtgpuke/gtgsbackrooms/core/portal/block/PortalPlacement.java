package com.gtgpuke.gtgsbackrooms.core.portal.block;

import com.mojang.serialization.Codec;
import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.Direction;

/**
 * Pose d'un bloc portail par la baguette : selon le regard du joueur (debout face a lui, couche au sol ou au plafond s'il
 * regarde franchement vers le bas ou le haut), ou imposee : debout, au sol, au plafond, quel que soit le regard.
 */
public enum PortalPlacement implements StringIdentifiable {
    LOOK("look", null),
    STANDING("standing", null),
    FLOOR("floor", Direction.UP),
    CEILING("ceiling", Direction.DOWN);

    /** Format du composant et de la sauvegarde : le nom en minuscules. */
    public static final Codec<PortalPlacement> CODEC = StringIdentifiable.createCodec(PortalPlacement::values);
    /** Regard plus incline que cela vers un sol ou un plafond : la pose selon le regard couche le cadre dessus. */
    static final float LYING_PITCH = 60.0f;
    private static final PortalPlacement[] VALUES = values();

    private final String name;
    private final @Nullable Direction lying;

    PortalPlacement(String name, @Nullable Direction lying) {
        this.name = name;
        this.lying = lying;
    }

    @Override
    public String asString() {
        return this.name;
    }

    /**
     * Face avant du cadre couche que cette pose donne au bloc que ce joueur pose : le haut (cadre au sol, on y tombe), le bas
     * (au plafond, on y monte), ou {@code null} pour un cadre debout. Selon le regard, sans joueur (distributeur, commande),
     * le cadre est debout.
     *
     * @param player joueur qui pose le bloc, ou {@code null}
     */
    public @Nullable Direction lyingFacing(@Nullable PlayerEntity player) {
        if (this != LOOK || player == null) {
            return this.lying;
        }
        if (player.getPitch() > LYING_PITCH) {
            return Direction.UP;
        }
        return player.getPitch() < -LYING_PITCH ? Direction.DOWN : null;
    }

    /** Pose d'un nom sauvegarde ; selon le regard pour un nom inconnu ou absent. */
    public static PortalPlacement byName(String name) {
        for (PortalPlacement placement : VALUES) {
            if (placement.name.equals(name)) {
                return placement;
            }
        }
        return LOOK;
    }
}
