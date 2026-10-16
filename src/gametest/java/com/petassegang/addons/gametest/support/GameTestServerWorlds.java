package com.petassegang.addons.gametest.support;

import java.lang.reflect.Field;
import java.util.Map;

import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTestException;
import net.minecraft.world.World;

import com.petassegang.addons.backrooms.BackroomsConstants;

/**
 * Le serveur de test vanilla ignore les dimensions de datapack : il n'a pas de Level 0. Pour exercer ce qui le
 * cherche par sa cle (cible des portes, portails de retour), un monde existant lui est prete sous cette cle le
 * temps d'un test, par reflexion sur la table des mondes du serveur (noms Yarn de l'environnement de dev).
 */
public final class GameTestServerWorlds {

    private GameTestServerWorlds() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /** Inscrit {@code standIn} sous la cle du Level 0 ; a defaire par {@link #removeStandInLevelZero}. */
    public static void registerStandInLevelZero(MinecraftServer server, ServerWorld standIn) {
        worlds(server).put(BackroomsConstants.LEVEL_ZERO_WORLD, standIn);
    }

    /** Retire le monde prete sous la cle du Level 0. */
    public static void removeStandInLevelZero(MinecraftServer server) {
        worlds(server).remove(BackroomsConstants.LEVEL_ZERO_WORLD);
    }

    @SuppressWarnings("unchecked")
    private static Map<RegistryKey<World>, ServerWorld> worlds(MinecraftServer server) {
        try {
            Field field = MinecraftServer.class.getDeclaredField("worlds");
            field.setAccessible(true);
            return (Map<RegistryKey<World>, ServerWorld>) field.get(server);
        } catch (ReflectiveOperationException exception) {
            throw new GameTestException("Table des mondes du serveur inaccessible : " + exception.getMessage());
        }
    }
}
