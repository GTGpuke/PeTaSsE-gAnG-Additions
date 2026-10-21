package com.gtgpuke.gtgsbackrooms.core.registry;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import com.gtgpuke.gtgsbackrooms.core.ModConstants;

/**
 * Registre des sons du mod. Chaque son est declare dans {@code assets/gtgs_backrooms/sounds.json} sous le meme chemin,
 * avec son sous-titre traduit.
 */
public final class ModSounds {

    /** Un portail s'ouvre : la baguette vient de poser un bloc portail. */
    public static final SoundEvent PORTAL_WAND_PLACE = register("item.portal_wand.place");

    private ModSounds() {
        throw new UnsupportedOperationException("Classe de registre.");
    }

    /** Force le chargement de la classe, donc l'enregistrement des sons. */
    public static void register() {
        ModConstants.LOGGER.debug("Sons du mod enregistres.");
    }

    private static SoundEvent register(String path) {
        Identifier id = Identifier.of(ModConstants.MOD_ID, path);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }
}
