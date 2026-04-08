package com.petassegang.addons;

import com.petassegang.addons.item.GangBadgeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Teste les propriétés de GangBadgeItem.
 *
 * <p><b>Note :</b> Les tests appelant {@code item.isFoil()} ou les méthodes de tooltip
 * nécessitent les classes Minecraft sur le classpath (fourni par ForgeGradle au moment
 * de la compilation des tests). Ils s'exécutent correctement via {@code ./gradlew test}.
 *
 * <p>Modèle : reproduire cette structure pour chaque nouvelle classe d'item.
 */
@DisplayName("Propriétés de GangBadgeItem")
class ItemTest {

    private GangBadgeItem item;

    @BeforeEach
    void setUp() {
        item = new GangBadgeItem(
                new Item.Properties()
                        .stacksTo(1)
                        .rarity(Rarity.EPIC)
        );
    }

    @Test
    @DisplayName("isFoil() retourne toujours true")
    void testIsFoilAlwaysTrue() {
        ItemStack stack = new ItemStack(item);
        assertTrue(item.isFoil(stack),
                "Le Gang Badge doit toujours afficher la brillance d'enchantement.");
    }

    @Test
    @DisplayName("La taille de pile est 1")
    void testStackSizeIsOne() {
        assertEquals(1, item.getDefaultMaxStackSize(),
                "Le Gang Badge ne doit pas s'empiler.");
    }

    @Test
    @DisplayName("La rareté est EPIC")
    void testRarityIsEpic() {
        ItemStack stack = new ItemStack(item);
        assertEquals(Rarity.EPIC, stack.getRarity(),
                "La rareté du Gang Badge doit être EPIC.");
    }

    @Test
    @DisplayName("GangBadgeItem n'est pas null après construction")
    void testItemConstructionNotNull() {
        assertNotNull(item, "Le constructeur de GangBadgeItem ne doit pas retourner null.");
    }

    @Test
    @DisplayName("GangBadgeItem override la méthode use()")
    void testUseMethodOverridden() throws NoSuchMethodException {
        var method = GangBadgeItem.class.getDeclaredMethod("use",
                net.minecraft.world.level.Level.class,
                net.minecraft.world.entity.player.Player.class,
                net.minecraft.world.InteractionHand.class);
        assertNotNull(method,
                "GangBadgeItem doit surcharger use() pour gérer le clic droit.");
        assertEquals(net.minecraft.world.InteractionResult.class, method.getReturnType(),
                "use() doit retourner InteractionResult.");
    }
}
