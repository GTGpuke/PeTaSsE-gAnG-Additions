package com.petassegang.addons.block;

import net.minecraft.util.StringRepresentable;

/**
 * Palette minimale des couleurs de neons.
 */
public enum NeonColor implements StringRepresentable {
    WARM_YELLOW("warm_yellow"),
    COOL_WHITE("cool_white"),
    RED("red"),
    BLUE("blue"),
    GREEN("green");

    private final String serializedName;

    NeonColor(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
