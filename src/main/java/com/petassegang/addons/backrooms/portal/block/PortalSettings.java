package com.petassegang.addons.backrooms.portal.block;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;

import net.minecraft.item.ItemStack;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.math.MathHelper;

import com.petassegang.addons.init.ModDataComponents;

/**
 * Reglages d'un portail, choisis a sa creation et gardes avec lui : taille du cadre, passage de la lumiere des blocs,
 * faces, retour et sortie fugace. L'item du bloc portail les porte dans un composant ; le bloc pose les recopie et les
 * donne a ses portails.
 *
 * @param width largeur du cadre, en blocs, dans [{@value #MIN_SIZE}, {@value #MAX_SIZE}]
 * @param height hauteur du cadre, en blocs, dans [{@value #MIN_SIZE}, {@value #MAX_SIZE}]
 * @param transmitsLight {@code true} si la lumiere des blocs passe d'un cote a l'autre
 * @param doubleSided {@code true} si le portail se voit et se traverse par ses deux faces ; sinon par sa face avant seule,
 *                    et de dos il n'y a rien : on passe au travers
 * @param oneWay {@code true} si le portail ne mene que dans un sens : rien a l'arrivee pour revenir
 * @param fleetingExit {@code true} si la sortie (le retour, a l'arrivee) ne tient que tant qu'un joueur la voit : perdue
 *                     de vue, elle disparait pour de bon ; sans effet sur un aller simple, qui n'en a pas
 */
public record PortalSettings(int width, int height, boolean transmitsLight, boolean doubleSided, boolean oneWay, boolean fleetingExit) {

    /** Plus petite largeur ou hauteur d'un cadre, en blocs. */
    public static final int MIN_SIZE = 1;
    /** Plus grande largeur ou hauteur d'un cadre, en blocs. */
    public static final int MAX_SIZE = 8;
    /** Un bloc de large, deux de haut, la lumiere passe, deux faces, aller-retour, sortie durable : le portail par defaut. */
    public static final PortalSettings DEFAULT = new PortalSettings(1, 2, true);
    /** Reglages proposes dans l'onglet creatif ; les autres se donnent par commande. */
    public static final List<PortalSettings> PRESETS = List.of(
            DEFAULT,
            new PortalSettings(2, 3, true),
            new PortalSettings(3, 4, true),
            new PortalSettings(1, 2, false),
            new PortalSettings(1, 2, true, false, false, false),
            new PortalSettings(1, 2, true, true, true, false),
            new PortalSettings(1, 2, true, true, false, true));

    /** Format du composant et de la sauvegarde ; un champ absent prend la valeur par defaut, un champ hors bornes est refuse. */
    public static final Codec<PortalSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(MIN_SIZE, MAX_SIZE).optionalFieldOf("width", DEFAULT.width()).forGetter(PortalSettings::width),
            Codec.intRange(MIN_SIZE, MAX_SIZE).optionalFieldOf("height", DEFAULT.height()).forGetter(PortalSettings::height),
            Codec.BOOL.optionalFieldOf("transmits_light", DEFAULT.transmitsLight()).forGetter(PortalSettings::transmitsLight),
            Codec.BOOL.optionalFieldOf("double_sided", DEFAULT.doubleSided()).forGetter(PortalSettings::doubleSided),
            Codec.BOOL.optionalFieldOf("one_way", DEFAULT.oneWay()).forGetter(PortalSettings::oneWay),
            Codec.BOOL.optionalFieldOf("fleeting_exit", DEFAULT.fleetingExit()).forGetter(PortalSettings::fleetingExit))
            .apply(instance, PortalSettings::new));

    /** Format reseau du composant. */
    public static final PacketCodec<ByteBuf, PortalSettings> PACKET_CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, PortalSettings::width,
            PacketCodecs.VAR_INT, PortalSettings::height,
            PacketCodecs.BOOL, PortalSettings::transmitsLight,
            PacketCodecs.BOOL, PortalSettings::doubleSided,
            PacketCodecs.BOOL, PortalSettings::oneWay,
            PacketCodecs.BOOL, PortalSettings::fleetingExit,
            PortalSettings::new);

    /** Taille ramenee dans les bornes : une sauvegarde ou un paquet ne peuvent pas creer un cadre demesure. */
    public PortalSettings {
        width = MathHelper.clamp(width, MIN_SIZE, MAX_SIZE);
        height = MathHelper.clamp(height, MIN_SIZE, MAX_SIZE);
    }

    /** Portail a deux faces, aller-retour, a la sortie durable, de cette taille et avec ce passage de la lumiere. */
    public PortalSettings(int width, int height, boolean transmitsLight) {
        this(width, height, transmitsLight, true, false, false);
    }

    /** Reglages portes par une pile, ceux par defaut si elle n'en porte pas. */
    public static PortalSettings of(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.PORTAL_SETTINGS, DEFAULT);
    }

    /** Pile de l'item du bloc portail portant ces reglages. */
    public ItemStack toStack(ItemStack base) {
        ItemStack stack = base.copy();
        stack.set(ModDataComponents.PORTAL_SETTINGS, this);
        return stack;
    }
}
