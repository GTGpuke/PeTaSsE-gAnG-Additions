package com.gtgpuke.gtgsbackrooms.gametest.feature;

import java.util.UUID;

import com.mojang.authlib.GameProfile;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;

import com.gtgpuke.gtgsbackrooms.feature.cursed.CursedSnackItem;
import com.gtgpuke.gtgsbackrooms.gametest.support.GameTestPlayers;
import com.gtgpuke.gtgsbackrooms.init.ModItems;

/**
 * Tests en jeu des objets du mod, utilises par un vrai joueur serveur : badge de la Gang et collation maudite,
 * y compris par un joueur qui n'est pas un joueur serveur et par une creature.
 */
public final class ItemGameTests implements FabricGameTest {

    @GameTest(templateName = GameTestPlayers.ARENA)
    public void itemsReactToTheirPlayer(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GameTestPlayers.connect(context, GameMode.SURVIVAL, context.getAbsolute(new Vec3d(1.5, 1.0, 1.5)), 0.0f, 0.0f);
        try {
            player.setStackInHand(Hand.MAIN_HAND, new ItemStack(ModItems.GANG_BADGE));
            TypedActionResult<ItemStack> result = ModItems.GANG_BADGE.use(world, player, Hand.MAIN_HAND);
            context.assertTrue(result.getResult().isAccepted(), "Le badge s'active en main.");
            context.assertTrue(result.getValue().isOf(ModItems.GANG_BADGE), "Le badge reste en main.");

            player.getHungerManager().setFoodLevel(10);
            ItemStack snack = new ItemStack(ModItems.CURSED_SNACK);
            ModItems.CURSED_SNACK.finishUsing(snack, world, player);
            context.assertTrue(player.getHungerManager().getFoodLevel() == CursedSnackItem.penalizedFoodLevel(10),
                    "La collation maudite retire de la faim au lieu d'en donner : " + player.getHungerManager().getFoodLevel() + ".");
            // Un joueur qui n'est pas un joueur serveur (faux joueur, client) active le badge sans son ni paquet.
            PlayerEntity ghost = new PlayerEntity(world, BlockPos.ORIGIN, 0.0f, new GameProfile(UUID.randomUUID(), "fantome")) {
                @Override
                public boolean isSpectator() {
                    return false;
                }

                @Override
                public boolean isCreative() {
                    return false;
                }
            };
            context.assertTrue(ModItems.GANG_BADGE.use(world, ghost, Hand.MAIN_HAND).getResult().isAccepted(), "Le badge s'active aussi pour un joueur non serveur.");
            // Mangee par une creature, la collation n'a aucune faim a penaliser.
            SheepEntity sheep = EntityType.SHEEP.create(world);
            Vec3d sheepPos = context.getAbsolute(new Vec3d(3.5, 1.0, 1.5));
            sheep.refreshPositionAndAngles(sheepPos.x, sheepPos.y, sheepPos.z, 0.0f, 0.0f);
            world.spawnEntity(sheep);
            ModItems.CURSED_SNACK.finishUsing(new ItemStack(ModItems.CURSED_SNACK), world, sheep);
            context.assertTrue(sheep.isAlive(), "La creature a mange la collation sans mal.");
            sheep.discard();
        } finally {
            GameTestPlayers.disconnect(player);
        }
        context.complete();
    }
}
