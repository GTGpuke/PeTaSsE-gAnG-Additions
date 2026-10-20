package com.gtgpuke.gtgsbackrooms.backrooms.portal.block;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;

import net.minecraft.item.ItemStack;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.math.MathHelper;

import com.gtgpuke.gtgsbackrooms.init.ModDataComponents;

/**
 * Reglages d'un portail, choisis a sa creation et gardes avec lui : taille du cadre, passage de la lumiere des blocs,
 * faces, retour, sortie fugace, sens de sortie et tirage au sort de l'arrivee. L'item du bloc portail les porte dans un
 * composant ; le bloc pose les recopie et les donne a ses portails.
 *
 * @param width largeur du cadre, en blocs, dans [{@value #MIN_SIZE}, {@value #MAX_SIZE}]
 * @param height hauteur du cadre, en blocs, dans [{@value #MIN_SIZE}, {@value #MAX_SIZE}]
 * @param transmitsLight {@code true} si la lumiere des blocs passe d'un cote a l'autre
 * @param doubleSided {@code true} si le portail se voit et se traverse par ses deux faces ; sinon par sa face avant seule,
 *                    et de dos il n'y a rien : on passe au travers
 * @param oneWay {@code true} si le portail ne mene que dans un sens : rien a l'arrivee pour revenir
 * @param fleetingExit {@code true} si la sortie (le retour, a l'arrivee) ne tient que tant qu'un joueur la voit : perdue
 *                     de vue, elle disparait jusqu'au prochain joueur qui passe l'entree ; sans effet sur un aller simple,
 *                     qui n'en a pas
 * @param exit sens dans lequel on ressort du cadre d'arrivee : tout droit (la direction d'entree), un point cardinal, ou
 *             par le sol ou le plafond (cadre d'arrivee couche)
 * @param exitScatter rayon, en blocs, du tirage au sort des X/Z de l'arrivee autour de ceux du bloc, dans
 *                    [0, {@value #MAX_SCATTER}] ; zero pour une arrivee aux memes X/Z
 */
public record PortalSettings(
        int width, int height, boolean transmitsLight, boolean doubleSided, boolean oneWay, boolean fleetingExit, PortalExit exit, int exitScatter) {

    /** Plus petite largeur ou hauteur d'un cadre, en blocs. */
    public static final int MIN_SIZE = 1;
    /** Plus grande largeur ou hauteur d'un cadre, en blocs. */
    public static final int MAX_SIZE = 8;
    /** Plus grand rayon de tirage au sort de l'arrivee, en blocs. */
    public static final int MAX_SCATTER = 64;
    /** Un bloc de large, deux de haut, la lumiere passe, deux faces, aller-retour, sortie durable : le portail par defaut. */
    public static final PortalSettings DEFAULT = new PortalSettings(1, 2, true);
    /** Rayon du tirage au sort de l'arrivee propose avec chaque sens de sortie, en blocs. */
    static final int PRESET_SCATTER = 32;
    /**
     * Reglages proposes dans l'onglet creatif, le portail par defaut en tete ; les autres se donnent par commande. Le
     * cadre se couche au sol ou au plafond selon le regard a la pose : chacun se pose debout comme couche.
     */
    public static final List<PortalSettings> PRESETS = presets();

    /** Format du composant et de la sauvegarde ; un champ absent prend la valeur par defaut, un champ hors bornes est refuse. */
    public static final Codec<PortalSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(MIN_SIZE, MAX_SIZE).optionalFieldOf("width", DEFAULT.width()).forGetter(PortalSettings::width),
            Codec.intRange(MIN_SIZE, MAX_SIZE).optionalFieldOf("height", DEFAULT.height()).forGetter(PortalSettings::height),
            Codec.BOOL.optionalFieldOf("transmits_light", DEFAULT.transmitsLight()).forGetter(PortalSettings::transmitsLight),
            Codec.BOOL.optionalFieldOf("double_sided", DEFAULT.doubleSided()).forGetter(PortalSettings::doubleSided),
            Codec.BOOL.optionalFieldOf("one_way", DEFAULT.oneWay()).forGetter(PortalSettings::oneWay),
            Codec.BOOL.optionalFieldOf("fleeting_exit", DEFAULT.fleetingExit()).forGetter(PortalSettings::fleetingExit),
            PortalExit.CODEC.optionalFieldOf("exit", DEFAULT.exit()).forGetter(PortalSettings::exit),
            Codec.intRange(0, MAX_SCATTER).optionalFieldOf("exit_scatter", DEFAULT.exitScatter()).forGetter(PortalSettings::exitScatter))
            .apply(instance, PortalSettings::new));

    /** Format reseau du composant : le format de sauvegarde, en NBT (un tuple ne tient que six champs). */
    public static final PacketCodec<ByteBuf, PortalSettings> PACKET_CODEC = PacketCodecs.codec(CODEC);

    /** Taille et rayon ramenes dans les bornes : une sauvegarde ne peut pas creer un cadre demesure ni une arrivee perdue. */
    public PortalSettings {
        width = MathHelper.clamp(width, MIN_SIZE, MAX_SIZE);
        height = MathHelper.clamp(height, MIN_SIZE, MAX_SIZE);
        exitScatter = MathHelper.clamp(exitScatter, 0, MAX_SCATTER);
    }

    /** Reglages sans sens de sortie impose ni tirage au sort : on ressort tout droit, aux memes X/Z. */
    public PortalSettings(int width, int height, boolean transmitsLight, boolean doubleSided, boolean oneWay, boolean fleetingExit) {
        this(width, height, transmitsLight, doubleSided, oneWay, fleetingExit, PortalExit.STRAIGHT, 0);
    }

    /** Portail a deux faces, aller-retour, a la sortie durable, de cette taille et avec ce passage de la lumiere. */
    public PortalSettings(int width, int height, boolean transmitsLight) {
        this(width, height, transmitsLight, true, false, false);
    }

    /**
     * Le portail par defaut, les autres tailles, chaque sens de sortie (pour un cadre d'un bloc de large et pour un cadre
     * carre, qui va bien au sol), l'arrivee tiree au sort a chaque rayon puis avec chaque sens de sortie, et enfin les
     * comportements, seuls puis combines. Tous differents : l'onglet refuse deux piles identiques.
     */
    private static List<PortalSettings> presets() {
        List<PortalExit> imposed = new ArrayList<>(List.of(PortalExit.values()));
        imposed.remove(PortalExit.STRAIGHT);
        List<PortalSettings> presets = new ArrayList<>();
        presets.add(DEFAULT);
        int[][] sizes = {{1, 1}, {2, 2}, {2, 3}, {3, 3}, {3, 4}, {4, 4}, {MAX_SIZE, MAX_SIZE}};
        for (int[] size : sizes) {
            presets.add(new PortalSettings(size[0], size[1], true));
        }
        // Debout ou couche a l'entree, on ressort par un mur, par le sol ou par le plafond : changement de gravite compris.
        for (int width = 1; width <= 2; width++) {
            for (PortalExit exit : imposed) {
                presets.add(new PortalSettings(width, 2, true, true, false, false, exit, 0));
            }
        }
        for (int scatter : new int[] {8, PRESET_SCATTER, MAX_SCATTER}) {
            presets.add(new PortalSettings(1, 2, true, true, false, false, PortalExit.STRAIGHT, scatter));
        }
        for (PortalExit exit : imposed) {
            presets.add(new PortalSettings(1, 2, true, true, false, false, exit, PRESET_SCATTER));
        }
        presets.add(new PortalSettings(1, 2, false));
        presets.add(new PortalSettings(1, 2, true, false, false, false));
        presets.add(new PortalSettings(1, 2, true, true, true, false));
        presets.add(new PortalSettings(1, 2, true, true, false, true));
        // Trappe : posee au sol, on tombe du plafond du Level 0, sans retour.
        presets.add(new PortalSettings(2, 2, true, true, true, false));
        presets.add(new PortalSettings(1, 2, true, true, true, false, PortalExit.STRAIGHT, MAX_SCATTER));
        presets.add(new PortalSettings(1, 2, true, true, false, true, PortalExit.STRAIGHT, PRESET_SCATTER));
        presets.add(new PortalSettings(1, 2, false, false, true, false));
        return List.copyOf(presets);
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
