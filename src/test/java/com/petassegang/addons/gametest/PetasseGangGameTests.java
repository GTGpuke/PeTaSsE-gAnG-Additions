package com.petassegang.addons.gametest;

import com.petassegang.addons.creative.ModCreativeTab;
import com.petassegang.addons.init.ModItems;
import com.petassegang.addons.util.ModConstants;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * In-game Forge GameTests for PétasseGang Addons.
 *
 * <p>Run with: {@code ./gradlew runGameTestServer}
 *
 * <p>Each method annotated with {@link GameTest} is a test case.
 * The test passes when {@link GameTestHelper#succeed()} is called,
 * and fails if an assertion throws or the timeout is exceeded.
 */
@GameTestHolder(ModConstants.MOD_ID)
@PrefixGameTestTemplate(false)
public class PetasseGangGameTests {

    /**
     * Verifies that the Gang Badge item has been registered in the item registry.
     */
    @GameTest(template = "petassegang_addons:empty")
    public static void gangBadgeItemIsRegistered(GameTestHelper helper) {
        helper.assertTrue(
                ModItems.GANG_BADGE.isPresent(),
                "GANG_BADGE RegistryObject must be bound after registration"
        );
        helper.assertTrue(
                ModItems.GANG_BADGE.get() != null,
                "GANG_BADGE item must not be null in the game registry"
        );
        helper.succeed();
    }

    /**
     * Verifies that the PétasseGang creative tab is registered.
     */
    @GameTest(template = "petassegang_addons:empty")
    public static void creativeTabIsRegistered(GameTestHelper helper) {
        helper.assertTrue(
                ModCreativeTab.PETASSEGANG_TAB.isPresent(),
                "PETASSEGANG_TAB must be registered"
        );
        helper.succeed();
    }
}
