package com.gtgpuke.gtgsbackrooms.backrooms.portal.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import com.gtgpuke.gtgsbackrooms.MinecraftTestBootstrap;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.entity.PortalFacing;
import com.gtgpuke.gtgsbackrooms.init.ModItems;

@DisplayName("Reglages et cadre des portails")
class PortalSettingsTest {

    private static final double EPSILON = 1.0e-9;

    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.ensure();
    }

    @Test
    @DisplayName("Un champ absent prend la valeur par defaut, un champ hors bornes est refuse, et le format est stable")
    void codecReadsWritesAndRejects() {
        assertEquals(PortalSettings.DEFAULT, decode("{}"), "Rien de regle : les reglages par defaut.");
        assertEquals(new PortalSettings(3, 4, false), decode("{\"width\":3,\"height\":4,\"transmits_light\":false}"), "Taille et lumiere.");
        assertEquals(new PortalSettings(1, 2, true, false, true, false), decode("{\"double_sided\":false,\"one_way\":true}"),
                "Une face, aller simple.");
        assertEquals(new PortalSettings(1, 2, true, true, false, true), decode("{\"fleeting_exit\":true}"), "Sortie fugace.");
        assertEquals(new PortalSettings(1, 2, true, true, false, false, PortalExit.EAST, 12), decode("{\"exit\":\"east\",\"exit_scatter\":12}"),
                "Sortie vers l'est, arrivee tiree au sort a douze blocs pres.");
        assertTrue(PortalSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"width\":9}")).isError(), "Neuf blocs : refuse.");
        assertTrue(PortalSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"exit_scatter\":65}")).isError(),
                "Soixante-cinq blocs de tirage : refuse.");
        assertTrue(PortalSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"exit\":\"diagonale\"}")).isError(),
                "Un sens de sortie inconnu : refuse.");
        PortalSettings settings = new PortalSettings(2, 5, true, false, true, true, PortalExit.WEST, 5);
        JsonObject json = PortalSettings.CODEC.encodeStart(JsonOps.INSTANCE, settings).getOrThrow().getAsJsonObject();
        assertEquals(settings, PortalSettings.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(), "Aller-retour du format.");
    }

    @Test
    @DisplayName("Le format reseau fait l'aller-retour, et une taille hors bornes est ramenee dans les bornes")
    void packetCodecAndClamping() {
        ByteBuf buffer = Unpooled.buffer();
        PortalSettings settings = new PortalSettings(8, 1, false, false, true, true, PortalExit.SOUTH, 64);
        PortalSettings.PACKET_CODEC.encode(buffer, settings);
        assertEquals(settings, PortalSettings.PACKET_CODEC.decode(buffer), "Aller-retour reseau.");
        assertEquals(new PortalSettings(PortalSettings.MAX_SIZE, PortalSettings.MIN_SIZE, true), new PortalSettings(40, -3, true),
                "Taille ramenee dans les bornes.");
        assertEquals(0, new PortalSettings(1, 2, true, true, false, false, PortalExit.STRAIGHT, -3).exitScatter(), "Rayon negatif : nul.");
        assertEquals(PortalSettings.MAX_SCATTER, new PortalSettings(1, 2, true, true, false, false, PortalExit.STRAIGHT, 1000).exitScatter(),
                "Rayon demesure : ramene a la borne.");
    }

    @Test
    @DisplayName("Une pile sans reglages donne ceux par defaut ; une pile reglee donne les siens")
    void stackSettings() {
        ItemStack plain = new ItemStack(ModItems.BACKROOMS_PORTAL);
        assertEquals(PortalSettings.DEFAULT, PortalSettings.of(plain), "L'item porte les reglages par defaut.");
        assertEquals(PortalSettings.DEFAULT, PortalSettings.of(ItemStack.EMPTY), "Une pile vide aussi.");
        PortalSettings wide = new PortalSettings(3, 4, true);
        ItemStack stack = wide.toStack(plain);
        assertEquals(wide, PortalSettings.of(stack), "La pile reglee porte ses reglages.");
        assertEquals(PortalSettings.DEFAULT, PortalSettings.of(plain), "La pile d'origine n'est pas modifiee.");
        assertTrue(PortalSettings.DEFAULT.doubleSided() && !PortalSettings.DEFAULT.oneWay() && !PortalSettings.DEFAULT.fleetingExit(),
                "Par defaut : deux faces, aller-retour, sortie durable.");
    }

    @Test
    @DisplayName("L'onglet propose chaque taille utile, chaque sens de sortie, chaque rayon de tirage au sort et chaque comportement")
    void presetsCoverEveryWayToMakeAPortal() {
        List<PortalSettings> presets = PortalSettings.PRESETS;
        assertEquals(PortalSettings.DEFAULT, presets.get(0), "Le portail par defaut en tete.");
        assertEquals(presets.size(), new HashSet<>(presets).size(), "Tous differents : l'onglet refuse deux piles identiques.");
        assertEquals(37, presets.size(), "Trente-sept reglages proposes dans l'onglet creatif.");
        assertTrue(presets.contains(new PortalSettings(1, 1, true)) && presets.contains(new PortalSettings(3, 3, true))
                && presets.contains(new PortalSettings(PortalSettings.MAX_SIZE, PortalSettings.MAX_SIZE, true)), "Du plus petit cadre au plus grand, carres compris.");
        for (PortalExit exit : PortalExit.values()) {
            for (int width = 1; width <= 2; width++) {
                assertTrue(presets.contains(new PortalSettings(width, 2, true, true, false, false, exit, 0)), "Sortie " + exit + ", " + width + " de large.");
            }
            assertTrue(presets.contains(new PortalSettings(1, 2, true, true, false, false, exit, PortalSettings.PRESET_SCATTER)),
                    "Sortie " + exit + ", arrivee tiree au sort.");
        }
        for (int scatter : new int[] {8, PortalSettings.PRESET_SCATTER, PortalSettings.MAX_SCATTER}) {
            assertTrue(presets.contains(new PortalSettings(1, 2, true, true, false, false, PortalExit.STRAIGHT, scatter)), "Tirage au sort a " + scatter + " blocs.");
        }
        assertTrue(presets.contains(new PortalSettings(1, 2, false)) && presets.contains(new PortalSettings(1, 2, true, false, false, false))
                && presets.contains(new PortalSettings(1, 2, true, true, true, false)) && presets.contains(new PortalSettings(1, 2, true, true, false, true)),
                "Chaque comportement seul : sans lumiere, une face, aller simple, sortie fugace.");
        assertTrue(presets.contains(new PortalSettings(2, 2, true, true, true, false)), "Trappe : carree, aller simple.");
    }

    @Test
    @DisplayName("Le cadre s'etend autour de la colonne de l'ancre ; une largeur paire prend une colonne de plus et decale son centre")
    void frameColumnsAndCenter() {
        assertEquals(0, PortalArea.firstColumn(1), "Un bloc : premiere colonne, celle de l'ancre.");
        assertEquals(0, PortalArea.lastColumn(1), "Un bloc : derniere colonne, celle de l'ancre.");
        assertEquals(0, PortalArea.firstColumn(2), "Deux blocs : premiere colonne, celle de l'ancre.");
        assertEquals(1, PortalArea.lastColumn(2), "Deux blocs : derniere colonne, sa voisine le long de la largeur.");
        assertEquals(-1, PortalArea.firstColumn(3), "Trois blocs centres sur l'ancre : premiere colonne avant elle.");
        assertEquals(1, PortalArea.lastColumn(3), "Trois blocs centres sur l'ancre : derniere colonne apres elle.");
        assertEquals(0.0, PortalArea.widthOffset(3), EPSILON, "Largeur impaire : centre sur l'ancre.");
        assertEquals(0.5, PortalArea.widthOffset(4), EPSILON, "Largeur paire : un demi-bloc.");
        Vec3d center = PortalArea.center(new BlockPos(10, 64, -5), PortalFacing.NORTH, new PortalSettings(2, 3, true));
        assertEquals(10.5 + PortalFacing.NORTH.axisW.x * 0.5, center.x, EPSILON, "Decale le long de la largeur.");
        assertEquals(65.5, center.y, EPSILON, "A mi-hauteur du cadre, depuis le pied de l'ancre.");
        assertEquals(-4.5, center.z, EPSILON, "Plan au milieu du bloc.");
        assertEquals(new Vec3d(10.5, 64.0, -4.5), PortalArea.floor(center, PortalFacing.NORTH, new PortalSettings(2, 3, true)),
                "Le pied du cadre se retrouve depuis son centre : au pied de l'ancre, au milieu du bloc.");
        assertEquals(1.0, PortalArea.rowOffset(3), EPSILON, "Trois rangees depuis l'ancre : le centre est a la deuxieme.");
        assertEquals(0.5, PortalArea.rowOffset(2), EPSILON, "Deux rangees : entre les deux.");
        Vec3d lying = PortalArea.center(new BlockPos(10, 64, -5), PortalFacing.UP, new PortalSettings(2, 3, true));
        assertEquals(new Vec3d(11.0, 64.5, -5.5), lying,
                "Couche au sol : plan au milieu du bloc, largeur paire decalee vers l'est, rangees vers le nord depuis l'ancre.");
        assertEquals(new Vec3d(10.5, 64.0, -4.5), PortalArea.floor(lying, PortalFacing.UP, new PortalSettings(2, 3, true)), "Et son pied se retrouve.");
        assertEquals(new Vec3d(10.0, 64.5, -5.5), PortalArea.center(new BlockPos(10, 64, -5), PortalFacing.DOWN, new PortalSettings(2, 3, true)),
                "Au plafond : rangees vers le nord aussi, largeur paire decalee vers l'ouest.");
    }

    @Test
    @DisplayName("Tout droit, le cadre d'arrivee regarde a l'oppose de l'entree ; un point cardinal s'impose quelle que soit l'entree")
    void exitDirections() {
        assertEquals(PortalFacing.NORTH, PortalExit.STRAIGHT.arrivalFacing(PortalFacing.SOUTH), "Entre par le sud, on ressort vers le nord.");
        assertEquals(PortalFacing.WEST, PortalExit.STRAIGHT.arrivalFacing(PortalFacing.EAST), "Entre par l'est, on ressort vers l'ouest.");
        for (PortalExit exit : PortalExit.values()) {
            if (exit != PortalExit.STRAIGHT) {
                assertEquals(exit.arrivalFacing(PortalFacing.SOUTH), exit.arrivalFacing(PortalFacing.NORTH), exit + " s'impose quelle que soit l'entree.");
            }
            assertEquals(exit, PortalExit.byName(exit.asString()), "Chaque sens se relit par son nom.");
        }
        assertEquals(PortalFacing.EAST, PortalExit.EAST.arrivalFacing(PortalFacing.SOUTH), "Vers l'est : le cadre d'arrivee regarde l'est.");
        assertEquals(PortalFacing.UP, PortalExit.FLOOR.arrivalFacing(PortalFacing.SOUTH), "Par le sol : le cadre d'arrivee est couche au sol, on en sort vers le haut.");
        assertEquals(PortalFacing.DOWN, PortalExit.CEILING.arrivalFacing(PortalFacing.UP), "Par le plafond : le cadre d'arrivee est au plafond, on en tombe.");
        assertEquals(PortalFacing.DOWN, PortalExit.STRAIGHT.arrivalFacing(PortalFacing.UP), "Tout droit depuis un cadre au sol : on ressort d'un plafond.");
        assertEquals("east", PortalExit.EAST.asString(), "Nom en minuscules.");
        assertEquals(PortalExit.STRAIGHT, PortalExit.byName(""), "Sans nom : tout droit.");
        assertEquals(PortalExit.STRAIGHT, PortalExit.byName("diagonale"), "Nom inconnu : tout droit.");
    }

    private static PortalSettings decode(String json) {
        return PortalSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
    }
}
