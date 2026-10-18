package com.petassegang.addons.backrooms.portal.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

import com.petassegang.addons.MinecraftTestBootstrap;
import com.petassegang.addons.backrooms.portal.entity.PortalFacing;
import com.petassegang.addons.init.ModItems;

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
        assertEquals(new PortalSettings(1, 2, true, false, true), decode("{\"double_sided\":false,\"one_way\":true}"),
                "Une face, aller simple.");
        assertTrue(PortalSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"width\":9}")).isError(), "Neuf blocs : refuse.");
        PortalSettings settings = new PortalSettings(2, 5, true, false, true);
        JsonObject json = PortalSettings.CODEC.encodeStart(JsonOps.INSTANCE, settings).getOrThrow().getAsJsonObject();
        assertEquals(settings, PortalSettings.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(), "Aller-retour du format.");
    }

    @Test
    @DisplayName("Le format reseau fait l'aller-retour, et une taille hors bornes est ramenee dans les bornes")
    void packetCodecAndClamping() {
        ByteBuf buffer = Unpooled.buffer();
        PortalSettings settings = new PortalSettings(8, 1, false, false, true);
        PortalSettings.PACKET_CODEC.encode(buffer, settings);
        assertEquals(settings, PortalSettings.PACKET_CODEC.decode(buffer), "Aller-retour reseau.");
        assertEquals(new PortalSettings(PortalSettings.MAX_SIZE, PortalSettings.MIN_SIZE, true), new PortalSettings(40, -3, true),
                "Taille ramenee dans les bornes.");
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
        assertEquals(6, PortalSettings.PRESETS.size(), "Six reglages proposes dans l'onglet creatif.");
        assertTrue(PortalSettings.DEFAULT.doubleSided() && !PortalSettings.DEFAULT.oneWay(), "Par defaut : deux faces, aller-retour.");
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
    }

    private static PortalSettings decode(String json) {
        return PortalSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
    }
}
