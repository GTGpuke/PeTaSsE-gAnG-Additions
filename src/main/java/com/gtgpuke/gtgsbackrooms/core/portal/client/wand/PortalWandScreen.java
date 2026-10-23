package com.gtgpuke.gtgsbackrooms.core.portal.client.wand;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.UseItemCallback;

import com.gtgpuke.gtgsbackrooms.core.ModConstants;
import com.gtgpuke.gtgsbackrooms.core.portal.block.PortalExit;
import com.gtgpuke.gtgsbackrooms.core.portal.block.PortalMark;
import com.gtgpuke.gtgsbackrooms.core.portal.block.PortalPlacement;
import com.gtgpuke.gtgsbackrooms.core.portal.block.PortalSettings;
import com.gtgpuke.gtgsbackrooms.core.portal.block.PortalWayBack;
import com.gtgpuke.gtgsbackrooms.core.portal.item.PortalWandItem;
import com.gtgpuke.gtgsbackrooms.core.portal.sync.PortalWandSettingsPayload;
import com.gtgpuke.gtgsbackrooms.core.registry.ModItems;

/**
 * Ecran de la baguette de portail : chacun des reglages d'un portail, en deux colonnes (taille, lumiere, faces, pose et
 * destination ; retour, sens de sortie, arrivee tete en bas, arrivee au hasard, plan et sortie fixee). La sortie se fixe la
 * ou se tient le joueur, et sa dimension devient la destination ; changer de destination l'oublie. Les reglages choisis
 * partent au serveur a la fermeture de l'ecran, par Terminer comme par Echap, s'ils ont change ; le serveur les pose sur la
 * baguette tenue.
 */
public final class PortalWandScreen extends Screen {

    private static final String KEY = "screen." + ModConstants.MOD_ID + ".portal_wand.";
    private static final int BUTTON_WIDTH = 150;
    private static final int BUTTON_HEIGHT = 20;
    private static final int COLUMN_GAP = 10;
    private static final int ROW_STEP = 24;
    private static final int TOP = 40;
    private static final int TITLE_Y = 20;
    private static final int TITLE_COLOR = 0xFFFFFF;
    private static final List<Integer> SIZES = IntStream.rangeClosed(PortalSettings.MIN_SIZE, PortalSettings.MAX_SIZE).boxed().toList();

    private final Hand hand;
    private final PortalSettings initial;
    private int frameWidth;
    private int frameHeight;
    private boolean transmitsLight;
    private boolean doubleSided;
    private PortalWayBack wayBack;
    private PortalExit exit;
    private int exitScatter;
    private boolean flush;
    private boolean upsideDown;
    private PortalPlacement placement;
    private Optional<RegistryKey<World>> destination;
    private Optional<PortalMark> mark;
    private @Nullable CyclingButtonWidget<Optional<RegistryKey<World>>> destinationButton;
    private @Nullable ButtonWidget markButton;

    /**
     * Ecran des reglages de la baguette tenue dans cette main.
     *
     * @param hand main qui tient la baguette
     * @param settings reglages actuels de la baguette
     */
    public PortalWandScreen(Hand hand, PortalSettings settings) {
        super(Text.translatable(KEY + "title"));
        this.hand = hand;
        this.initial = settings;
        this.frameWidth = settings.width();
        this.frameHeight = settings.height();
        this.transmitsLight = settings.transmitsLight();
        this.doubleSided = settings.doubleSided();
        this.wayBack = PortalWayBack.of(settings);
        this.exit = settings.exit();
        this.exitScatter = settings.exitScatter();
        this.flush = settings.flush();
        this.upsideDown = settings.upsideDown();
        this.placement = settings.placement();
        this.destination = settings.destination();
        this.mark = settings.mark();
    }

    /** Ouvre l'ecran quand le joueur utilise la baguette dans le vide ; visant un bloc, la baguette pose un portail. */
    public static void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (!world.isClient || !stack.isOf(ModItems.PORTAL_WAND)) {
                return TypedActionResult.pass(stack);
            }
            MinecraftClient.getInstance().setScreen(new PortalWandScreen(hand, PortalSettings.of(stack)));
            return TypedActionResult.success(stack);
        });
    }

    @Override
    protected void init() {
        int left = this.width / 2 - BUTTON_WIDTH - COLUMN_GAP / 2;
        int right = this.width / 2 + COLUMN_GAP / 2;
        addDrawableChild(CyclingButtonWidget.<Integer>builder(value -> Text.literal(Integer.toString(value))).values(SIZES).initially(this.frameWidth)
                .build(left, TOP, BUTTON_WIDTH, BUTTON_HEIGHT, Text.translatable(KEY + "width"), (button, value) -> this.frameWidth = value));
        addDrawableChild(CyclingButtonWidget.<Integer>builder(value -> Text.literal(Integer.toString(value))).values(SIZES).initially(this.frameHeight)
                .build(left, TOP + ROW_STEP, BUTTON_WIDTH, BUTTON_HEIGHT, Text.translatable(KEY + "height"), (button, value) -> this.frameHeight = value));
        addDrawableChild(CyclingButtonWidget.onOffBuilder(Text.translatable(KEY + "light.on"), Text.translatable(KEY + "light.off"))
                .initially(this.transmitsLight)
                .build(left, TOP + 2 * ROW_STEP, BUTTON_WIDTH, BUTTON_HEIGHT, Text.translatable(KEY + "light"), (button, value) -> this.transmitsLight = value));
        addDrawableChild(CyclingButtonWidget.onOffBuilder(Text.translatable(KEY + "faces.double"), Text.translatable(KEY + "faces.single"))
                .initially(this.doubleSided)
                .build(left, TOP + 3 * ROW_STEP, BUTTON_WIDTH, BUTTON_HEIGHT, Text.translatable(KEY + "faces"), (button, value) -> this.doubleSided = value));
        addDrawableChild(CyclingButtonWidget.<PortalPlacement>builder(value -> Text.translatable(KEY + "placement." + value.asString()))
                .values(PortalPlacement.values())
                .initially(this.placement)
                .build(left, TOP + 4 * ROW_STEP, BUTTON_WIDTH, BUTTON_HEIGHT, Text.translatable(KEY + "placement"), (button, value) -> this.placement = value));
        this.destinationButton = addDrawableChild(CyclingButtonWidget.<Optional<RegistryKey<World>>>builder(PortalWandScreen::destinationName)
                .values(destinations())
                .initially(this.destination)
                .build(left, TOP + 5 * ROW_STEP, BUTTON_WIDTH, BUTTON_HEIGHT, Text.translatable(KEY + "destination"), (button, value) -> {
                    this.destination = value;
                    // La sortie fixee est dans sa propre dimension : une autre destination l'oublie.
                    this.mark = Optional.empty();
                    updateMarkButton();
                }));
        addDrawableChild(CyclingButtonWidget.<PortalWayBack>builder(value -> Text.translatable(KEY + "return." + value.key()))
                .values(PortalWayBack.values())
                .initially(this.wayBack)
                .build(right, TOP, BUTTON_WIDTH, BUTTON_HEIGHT, Text.translatable(KEY + "return"), (button, value) -> this.wayBack = value));
        addDrawableChild(CyclingButtonWidget.<PortalExit>builder(value -> Text.translatable(KEY + "exit." + value.asString())).values(PortalExit.values())
                .initially(this.exit)
                .build(right, TOP + ROW_STEP, BUTTON_WIDTH, BUTTON_HEIGHT, Text.translatable(KEY + "exit"), (button, value) -> this.exit = value));
        addDrawableChild(CyclingButtonWidget.onOffBuilder(Text.translatable(KEY + "arrival.upside_down"), Text.translatable(KEY + "arrival.upright"))
                .initially(this.upsideDown)
                .build(right, TOP + 2 * ROW_STEP, BUTTON_WIDTH, BUTTON_HEIGHT, Text.translatable(KEY + "arrival"), (button, value) -> this.upsideDown = value));
        addDrawableChild(new ScatterSlider(right, TOP + 3 * ROW_STEP));
        addDrawableChild(CyclingButtonWidget.onOffBuilder(Text.translatable(KEY + "plane.edge"), Text.translatable(KEY + "plane.middle"))
                .initially(this.flush)
                .build(right, TOP + 4 * ROW_STEP, BUTTON_WIDTH, BUTTON_HEIGHT, Text.translatable(KEY + "plane"), (button, value) -> this.flush = value));
        this.markButton = addDrawableChild(ButtonWidget.builder(Text.empty(), button -> toggleMark())
                .dimensions(right, TOP + 5 * ROW_STEP, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        updateMarkButton();
        addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> close())
                .dimensions(this.width / 2 - BUTTON_WIDTH / 2, TOP + 6 * ROW_STEP + ROW_STEP / 2, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    /**
     * Destinations proposees : automatique, puis chaque dimension du serveur (le monde normal, le Nether et l'End d'abord),
     * et celle des reglages si le serveur ne l'annonce pas.
     */
    private List<Optional<RegistryKey<World>>> destinations() {
        List<RegistryKey<World>> worlds = new ArrayList<>();
        if (this.client != null && this.client.getNetworkHandler() != null) {
            worlds.addAll(this.client.getNetworkHandler().getWorldKeys());
        }
        this.destination.filter(key -> !worlds.contains(key)).ifPresent(worlds::add);
        List<RegistryKey<World>> vanilla = List.of(World.OVERWORLD, World.NETHER, World.END);
        worlds.sort(Comparator.<RegistryKey<World>>comparingInt(key -> vanilla.contains(key) ? vanilla.indexOf(key) : vanilla.size())
                .thenComparing(key -> key.getValue().toString()));
        List<Optional<RegistryKey<World>>> values = new ArrayList<>();
        values.add(Optional.empty());
        worlds.forEach(key -> values.add(Optional.of(key)));
        return values;
    }

    private static Text destinationName(Optional<RegistryKey<World>> destination) {
        return destination.map(PortalWandItem::dimensionName).orElseGet(() -> Text.translatable(KEY + "destination.auto"));
    }

    /** Fixe la sortie la ou se tient le joueur (sa dimension devient la destination), ou l'oublie si elle l'est deja. */
    private void toggleMark() {
        if (this.mark.isPresent() || this.client == null || this.client.player == null) {
            this.mark = Optional.empty();
        } else {
            PortalMark here = PortalMark.at(this.client.player);
            this.mark = Optional.of(here);
            this.destination = Optional.of(here.dimension());
            if (this.destinationButton != null) {
                this.destinationButton.setValue(this.destination);
            }
        }
        updateMarkButton();
    }

    private void updateMarkButton() {
        if (this.markButton != null) {
            this.markButton.setMessage(this.mark.map(fixed -> (Text) Text.translatable(KEY + "mark.set",
                    fixed.pos().getX(), fixed.pos().getY(), fixed.pos().getZ())).orElseGet(() -> Text.translatable(KEY + "mark")));
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, TITLE_Y, TITLE_COLOR);
    }

    /** Fermeture, par Terminer ou Echap : les reglages partent au serveur s'ils ont change. */
    @Override
    public void removed() {
        PortalSettings chosen = new PortalSettings(this.frameWidth, this.frameHeight, this.transmitsLight, this.doubleSided,
                this.wayBack.oneWay(), this.wayBack.fleetingExit(), this.exit, this.exitScatter, this.flush, this.upsideDown, this.placement,
                this.destination, this.mark);
        if (!chosen.equals(this.initial)) {
            ClientPlayNetworking.send(new PortalWandSettingsPayload(this.hand, chosen));
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    /** Rayon de l'arrivee tiree au sort, de 0 (aux memes X/Z) a {@link PortalSettings#MAX_SCATTER} blocs. */
    private final class ScatterSlider extends SliderWidget {

        ScatterSlider(int x, int y) {
            super(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, Text.empty(), exitScatter / (double) PortalSettings.MAX_SCATTER);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(exitScatter == 0 ? Text.translatable(KEY + "scatter.none") : Text.translatable(KEY + "scatter", exitScatter));
        }

        @Override
        protected void applyValue() {
            exitScatter = (int) Math.round(this.value * PortalSettings.MAX_SCATTER);
        }
    }
}
