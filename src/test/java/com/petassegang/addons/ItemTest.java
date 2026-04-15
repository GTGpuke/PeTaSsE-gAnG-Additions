package com.petassegang.addons;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Rarity;
import net.minecraft.util.TypedActionResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.petassegang.addons.item.GangBadgeItem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Teste les propriétés de GangBadgeItem.
 *
 * <p><b>Note :</b> Les tests appelant {@code item.hasGlint()} ou les méthodes de tooltip
 * nécessitent les classes Minecraft sur le classpath (fourni par Fabric Loom au moment
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
                new Item.Settings()
                        .maxCount(1)
                        .rarity(Rarity.EPIC)
        );
    }

    @Test
    @DisplayName("hasGlint() retourne toujours true")
    void testHasGlintAlwaysTrue() {
        ItemStack stack = new ItemStack(item);
        assertTrue(item.hasGlint(stack),
                "Le Gang Badge doit toujours afficher la brillance d'enchantement.");
    }

    @Test
    @DisplayName("La taille de pile est 1")
    void testStackSizeIsOne() {
        assertEquals(1, item.getMaxCount(),
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
                net.minecraft.world.World.class,
                net.minecraft.entity.player.PlayerEntity.class,
                net.minecraft.util.Hand.class);
        assertNotNull(method,
                "GangBadgeItem doit surcharger use() pour gérer le clic droit.");
        assertEquals(TypedActionResult.class, method.getReturnType(),
                "use() doit retourner TypedActionResult.");
    }
}
