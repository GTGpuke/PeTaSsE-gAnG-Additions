package com.gtgpuke.gtgsbackrooms.core.registry;

import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import com.gtgpuke.gtgsbackrooms.core.ModConstants;
import com.gtgpuke.gtgsbackrooms.core.portal.block.PortalSettings;

/**
 * Registre des composants d'item du mod.
 */
public final class ModDataComponents {

    /** Reglages du portail que pose la baguette : taille du cadre, passage de la lumiere, faces, retour, sortie, plan. */
    public static final ComponentType<PortalSettings> PORTAL_SETTINGS = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(ModConstants.MOD_ID, "portal_settings"),
            ComponentType.<PortalSettings>builder().codec(PortalSettings.CODEC).packetCodec(PortalSettings.PACKET_CODEC).build());

    private ModDataComponents() {
        throw new UnsupportedOperationException("Classe de registre.");
    }

    /** Force le chargement de la classe, donc l'enregistrement des composants. */
    public static void register() {
        ModConstants.LOGGER.debug("Composants d'item du mod enregistres.");
    }
}
