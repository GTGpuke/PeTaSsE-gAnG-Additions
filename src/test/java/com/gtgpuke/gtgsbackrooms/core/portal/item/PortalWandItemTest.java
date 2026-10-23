package com.gtgpuke.gtgsbackrooms.core.portal.item;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import com.gtgpuke.gtgsbackrooms.MinecraftTestBootstrap;
import com.gtgpuke.gtgsbackrooms.backrooms.BackroomsConstants;
import com.gtgpuke.gtgsbackrooms.core.portal.block.PortalExit;
import com.gtgpuke.gtgsbackrooms.core.portal.block.PortalMark;
import com.gtgpuke.gtgsbackrooms.core.portal.block.PortalPlacement;
import com.gtgpuke.gtgsbackrooms.core.portal.block.PortalSettings;
import com.gtgpuke.gtgsbackrooms.core.portal.block.PortalWayBack;
import com.gtgpuke.gtgsbackrooms.core.registry.ModDataComponents;
import com.gtgpuke.gtgsbackrooms.core.registry.ModItems;

@DisplayName("Baguette de portail")
class PortalWandItemTest {

    /** Lignes d'usage, apres celles des reglages. */
    private static final List<String> USE = List.of("place", "configure", "sneak");

    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.ensure();
    }

    @Test
    @DisplayName("L'info-bulle donne la taille, la lumiere, les faces et le retour, puis ce qui n'est pas par defaut, puis l'usage")
    void tooltipDescribesTheSettingsAndTheUse() {
        assertEquals(withUse("size", "light", "double_sided", "round_trip"), tooltipKeys(new ItemStack(ModItems.PORTAL_WAND)),
                "Reglages par defaut.");
        assertEquals(withUse("size", "no_light", "single_sided", "one_way"), tooltipKeys(new PortalSettings(3, 4, false, false, true, true)),
                "Lumiere bloquee, une face, aller simple : sans sortie, rien de fugace.");
        assertEquals(withUse("size", "light", "double_sided", "fleeting_exit"), tooltipKeys(new PortalSettings(1, 2, true, true, false, true)),
                "Sortie fugace.");
        assertEquals(withUse("size", "light", "double_sided", "round_trip", "exit", "exit_scatter"),
                tooltipKeys(new PortalSettings(1, 2, true, true, false, false, PortalExit.WEST, 20)), "Sortie vers l'ouest, arrivee tiree au sort.");
        assertEquals(withUse("size", "no_light", "double_sided", "fleeting_exit", "exit", "exit_scatter", "flush", "upside_down"),
                tooltipKeys(new PortalSettings(2, 2, false, true, false, true, PortalExit.FLOOR, 12, true, true)), "Tout ce qui s'ecarte du defaut.");
        assertEquals(withUse("size", "light", "double_sided", "round_trip", "placement", "destination"),
                tooltipKeys(new PortalSettings(1, 2, true, true, false, false, PortalExit.STRAIGHT, 0, false, false, PortalPlacement.FLOOR,
                        Optional.of(World.NETHER), Optional.empty())), "Pose au sol, vers le Nether.");
        assertEquals(withUse("size", "light", "double_sided", "round_trip", "mark"),
                tooltipKeys(new PortalSettings(1, 2, true, true, false, false, PortalExit.STRAIGHT, 0, false, false, PortalPlacement.LOOK,
                        Optional.of(World.END), Optional.of(new PortalMark(World.END, new BlockPos(3, 70, 9), Direction.SOUTH)))),
                "Sortie fixee : elle dit la destination.");
        assertEquals("gtgs_backrooms:inconnue", PortalWandItem.dimensionName(RegistryKey.of(RegistryKeys.WORLD, Identifier.of("gtgs_backrooms", "inconnue")))
                .getString(), "Une dimension sans nom traduit : son identifiant.");
        assertEquals(1, ModItems.PORTAL_WAND.getMaxCount(), "Une baguette par pile.");
    }

    @Test
    @DisplayName("Chaque cle de l'info-bulle, de l'ecran et du message de copie est traduite en anglais et en francais")
    void everyKeyIsTranslated() throws IOException {
        Set<String> keys = new HashSet<>();
        for (PortalExit exit : PortalExit.values()) {
            for (PortalWayBack way : PortalWayBack.values()) {
                collectKeys(wand(new PortalSettings(2, 3, false, false, way.oneWay(), way.fleetingExit(), exit, 5, true, true)), keys);
                collectKeys(wand(new PortalSettings(1, 2, true, true, way.oneWay(), way.fleetingExit(), exit, 0, false)), keys);
            }
        }
        // Chaque pose, et chaque dimension nommee, en destination comme en sortie fixee.
        for (PortalPlacement placement : PortalPlacement.values()) {
            for (RegistryKey<World> dimension : List.of(World.OVERWORLD, World.NETHER, World.END, BackroomsConstants.LEVEL_ZERO_WORLD)) {
                collectKeys(wand(new PortalSettings(1, 2, true, true, false, false, PortalExit.STRAIGHT, 0, false, false, placement,
                        Optional.of(dimension), Optional.empty())), keys);
                collectKeys(wand(new PortalSettings(1, 2, true, true, false, false, PortalExit.STRAIGHT, 0, false, false, placement,
                        Optional.of(dimension), Optional.of(new PortalMark(dimension, BlockPos.ORIGIN, Direction.NORTH)))), keys);
            }
        }
        // L'ecran est du code client : ses cles se composent comme lui, sur les memes valeurs.
        String screen = "screen.gtgs_backrooms.portal_wand.";
        for (String suffix : List.of("title", "width", "height", "light", "light.on", "light.off", "faces", "faces.double", "faces.single",
                "return", "exit", "scatter", "scatter.none", "plane", "plane.middle", "plane.edge", "arrival", "arrival.upright", "arrival.upside_down",
                "placement", "destination", "destination.auto", "mark", "mark.set")) {
            keys.add(screen + suffix);
        }
        for (PortalWayBack way : PortalWayBack.values()) {
            keys.add(screen + "return." + way.key());
        }
        for (PortalExit exit : PortalExit.values()) {
            keys.add(screen + "exit." + exit.asString());
        }
        for (PortalPlacement placement : PortalPlacement.values()) {
            keys.add(screen + "placement." + placement.asString());
        }
        keys.add("message.gtgs_backrooms.portal_wand.copied");
        for (String language : List.of("en_us", "fr_fr")) {
            JsonObject translations = readLanguage(language);
            for (String key : keys) {
                assertTrue(translations.has(key), key + " est traduite en " + language + ".");
            }
        }
    }

    @Test
    @DisplayName("Le recepteur des reglages ne s'enregistre qu'une fois")
    void initIsIdempotent() {
        assertDoesNotThrow(PortalWandItem::init, "Deja enregistre au demarrage du mod : un second appel ne fait rien.");
    }

    private static List<String> withUse(String... settings) {
        List<String> keys = new ArrayList<>(List.of(settings));
        keys.addAll(USE);
        return keys;
    }

    private static List<String> tooltipKeys(PortalSettings settings) {
        return tooltipKeys(wand(settings));
    }

    private static List<String> tooltipKeys(ItemStack stack) {
        List<Text> tooltip = new ArrayList<>();
        ModItems.PORTAL_WAND.appendTooltip(stack, Item.TooltipContext.DEFAULT, tooltip, TooltipType.BASIC);
        List<String> keys = new ArrayList<>();
        for (Text line : tooltip) {
            String key = ((TranslatableTextContent) line.getContent()).getKey();
            keys.add(key.substring(key.lastIndexOf('.') + 1));
        }
        return keys;
    }

    /** Cles de toutes les lignes de l'info-bulle de cette pile, arguments traduits compris. */
    private static void collectKeys(ItemStack stack, Set<String> keys) {
        List<Text> tooltip = new ArrayList<>();
        ModItems.PORTAL_WAND.appendTooltip(stack, Item.TooltipContext.DEFAULT, tooltip, TooltipType.BASIC);
        for (Text line : tooltip) {
            collectKeys(line, keys);
        }
    }

    private static void collectKeys(Text text, Set<String> keys) {
        if (text.getContent() instanceof TranslatableTextContent translatable) {
            keys.add(translatable.getKey());
            for (Object argument : translatable.getArgs()) {
                if (argument instanceof Text inner) {
                    collectKeys(inner, keys);
                }
            }
        }
    }

    private static JsonObject readLanguage(String language) throws IOException {
        try (InputStream stream = PortalWandItemTest.class.getResourceAsStream("/assets/gtgs_backrooms/lang/" + language + ".json")) {
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    /** Baguette de portail portant ces reglages. */
    private static ItemStack wand(PortalSettings settings) {
        ItemStack wand = new ItemStack(ModItems.PORTAL_WAND);
        wand.set(ModDataComponents.PORTAL_SETTINGS, settings);
        return wand;
    }
}
