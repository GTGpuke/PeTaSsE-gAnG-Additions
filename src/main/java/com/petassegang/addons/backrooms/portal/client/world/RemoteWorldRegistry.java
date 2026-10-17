package com.petassegang.addons.backrooms.portal.client.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

import com.petassegang.addons.core.ModConstants;
import com.petassegang.addons.mixin.client.ClientWorldAccessor;
import com.petassegang.addons.mixin.client.MinecraftClientAccessor;

/**
 * Mondes distants du client, un par dimension annoncee par le serveur, plus la vue secondaire du monde principal.
 *
 * <p>Ils sont tous oublies quand le monde principal change (changement de dimension, reconnexion) :
 * le serveur repart lui aussi de zero a ce moment-la, les deux cotes restent donc d'accord.
 */
public final class RemoteWorldRegistry {

    private static final Map<RegistryKey<World>, RemoteWorld> WORLDS = new HashMap<>();
    /** Memes mondes que {@link #WORLDS}, pour les parcourir a chaque tick sans iterateur. */
    private static final List<RemoteWorld> WORLD_LIST = new ArrayList<>();
    private static @Nullable ClientWorld mainWorld;
    /** Renderer cree par vanilla, a rendre a {@code MinecraftClient} quand les mondes distants sont liberes. */
    private static @Nullable WorldRenderer vanillaRenderer;
    /** Vue secondaire du monde principal, creee a la demande pour les portails qui y ramenent. */
    private static @Nullable RemoteWorld mirror;

    private RemoteWorldRegistry() {
        throw new UnsupportedOperationException("Classe de registre.");
    }

    /** Monde distant d'une dimension, ou {@code null} si le serveur ne l'a pas annoncee. */
    public static @Nullable RemoteWorld get(RegistryKey<World> dimension) {
        return WORLDS.get(dimension);
    }

    /**
     * Vue d'une dimension depuis un portail : le monde distant, ou la vue secondaire du monde principal
     * quand le portail ramene dans la dimension courante (portail intra-dimension ou imbrique).
     *
     * @return vue de la dimension, ou {@code null} si le serveur ne l'a pas annoncee
     */
    public static @Nullable RemoteWorld viewOf(RegistryKey<World> dimension) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld current = client.world;
        if (current != null && dimension.equals(current.getRegistryKey())) {
            if (mirror == null) {
                mirror = RemoteWorld.mirror(client, current);
                ModConstants.LOGGER.debug("[Portal] Vue secondaire creee pour le monde principal {}.", dimension.getValue());
            }
            return mirror;
        }
        return WORLDS.get(dimension);
    }

    /**
     * Cree le monde distant d'une dimension annoncee, ou renvoie celui qui existe deja.
     *
     * @param dimension dimension distante
     * @param dimensionTypeId type de dimension, resolu dans les registres de la connexion
     * @return monde distant, ou {@code null} hors connexion ou si le type est inconnu
     */
    public static @Nullable RemoteWorld announce(RegistryKey<World> dimension, Identifier dimensionTypeId) {
        RemoteWorld existing = WORLDS.get(dimension);
        if (existing != null) {
            return existing;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
        if (networkHandler == null || client.world == null) {
            return null;
        }
        Optional<RegistryEntry.Reference<DimensionType>> dimensionType =
                networkHandler.getRegistryManager().get(RegistryKeys.DIMENSION_TYPE).getEntry(dimensionTypeId);
        if (dimensionType.isEmpty()) {
            ModConstants.LOGGER.warn("[Portal] Type de dimension inconnu pour le monde distant {} : {}.", dimension.getValue(), dimensionTypeId);
            return null;
        }
        RemoteWorld remote = RemoteWorld.create(client, networkHandler, dimension, dimensionType.get());
        remote.syncEnvironment(client.world);
        WORLDS.put(dimension, remote);
        WORLD_LIST.add(remote);
        // Creer un WorldRenderer rebranche le dispatcher d'entities partage : on le rend au monde principal.
        client.getEntityRenderDispatcher().setWorld(client.world);
        ModConstants.LOGGER.debug("[Portal] Monde distant cree pour {}.", dimension.getValue());
        return remote;
    }

    /** Oublie tous les mondes distants si le monde principal n'est plus celui de la derniere operation. */
    public static void syncWithMainWorld(@Nullable ClientWorld currentMainWorld) {
        if (currentMainWorld != mainWorld) {
            // Vanilla a remplace le monde principal (respawn, changement de dimension) : l'ancien n'existe plus pour personne.
            ClientPortalIndex.forget(mainWorld);
            dispose();
            mainWorld = currentMainWorld;
        }
    }

    /** Monde distant dessine par ce renderer, ou {@code null} : renderer principal ou vue secondaire. */
    public static @Nullable RemoteWorld ownerOf(WorldRenderer renderer) {
        for (int i = 0; i < WORLD_LIST.size(); i++) {
            RemoteWorld remote = WORLD_LIST.get(i);
            if (remote.renderer() == renderer) {
                return remote;
            }
        }
        return null;
    }

    /**
     * Traversee d'un portail : le monde distant {@code newMainWorld} devient le monde principal et
     * l'ancien monde principal reste disponible comme monde distant, avec son renderer.
     *
     * @param oldWorld monde principal quitte
     * @param oldRenderer renderer de ce monde (celui de {@code MinecraftClient} au moment du passage)
     * @param promoted monde distant qui devient principal
     * @param playerChunkX chunk du joueur dans l'ancien monde, centre de sa carte de chunks
     * @param playerChunkZ chunk du joueur dans l'ancien monde, centre de sa carte de chunks
     */
    public static void swapMainWorld(ClientWorld oldWorld, WorldRenderer oldRenderer, RemoteWorld promoted, int playerChunkX, int playerChunkZ) {
        if (vanillaRenderer == null) {
            vanillaRenderer = oldRenderer;
        }
        WORLDS.remove(promoted.world().getRegistryKey());
        WORLD_LIST.remove(promoted);
        // Les particules du monde d'arrivee deviennent celles du gestionnaire, celles de l'ancien monde le suivent : rien ne s'efface.
        RemoteParticles oldParticles = promoted.particles();
        oldParticles.exchange(MinecraftClient.getInstance().particleManager);
        RemoteWorld adopted = RemoteWorld.adopt(oldWorld, oldRenderer, playerChunkX, playerChunkZ, oldParticles);
        WORLDS.put(oldWorld.getRegistryKey(), adopted);
        WORLD_LIST.add(adopted);
        // La vue secondaire regardait l'ancien monde principal : elle sera recreee a la demande pour le nouveau.
        disposeMirror();
        mainWorld = promoted.world();
    }

    /**
     * Vanilla va creer un nouveau monde principal (connexion, respawn dans une autre dimension) et le lier au renderer
     * de {@code MinecraftClient} : le renderer vanilla, le seul que vanilla recharge avec les ressources, reprend donc sa
     * place d'abord. Celui qu'une traversee avait promu est libere avec l'ancien monde.
     */
    public static void restoreVanillaRenderer() {
        MinecraftClient client = MinecraftClient.getInstance();
        WorldRenderer current = client.worldRenderer;
        if (vanillaRenderer == null || current == vanillaRenderer) {
            return;
        }
        ((MinecraftClientAccessor) client).backrooms$setWorldRenderer(vanillaRenderer);
        current.setWorld(null);
        current.close();
    }

    /** Tick client : heure et meteo suivent le monde principal, les entities s'interpolent et s'animent comme les siennes. */
    public static void tickRemoteWorlds(MinecraftClient client) {
        if (client.world == null || client.isPaused() || WORLD_LIST.isEmpty()) {
            return;
        }
        syncWithMainWorld(client.world);
        for (int i = 0; i < WORLD_LIST.size(); i++) {
            RemoteWorld remote = WORLD_LIST.get(i);
            remote.syncEnvironment(client.world);
            remote.tick(client);
        }
    }

    /** Le renderer principal programme la reconstruction d'une section : la vue secondaire la reprend. */
    public static void onMainChunkRenderScheduled(int x, int y, int z, boolean important) {
        if (mirror != null) {
            mirror.scheduleChunkRender(x, y, z, important);
        }
    }

    /**
     * Libere tous les mondes distants (deconnexion, changement de monde principal par vanilla) et remet le renderer
     * principal en accord avec le monde principal.
     *
     * <p>Un {@code ClientWorld} envoie ses mises a jour de blocs au renderer avec lequel vanilla l'a cree : celui qui
     * etait {@code MinecraftClient.worldRenderer} a ce moment-la. {@link #restoreVanillaRenderer()} rend sa place au
     * renderer vanilla avant que vanilla ne cree un monde ; si un monde a tout de meme ete lie a un autre renderer, c'est
     * celui-la qui reste principal, sans quoi ses blocs changes ne se redessineraient plus. Sans monde (fin de partie),
     * le renderer vanilla reprend sa place pour la prochaine.
     */
    public static void dispose() {
        MinecraftClient client = MinecraftClient.getInstance();
        for (int i = 0; i < WORLD_LIST.size(); i++) {
            RemoteWorld remote = WORLD_LIST.get(i);
            ClientPortalIndex.forget(remote.world());
            if (remote.renderer() != vanillaRenderer) {
                remote.dispose();
            }
        }
        WORLDS.clear();
        WORLD_LIST.clear();
        disposeMirror();
        if (vanillaRenderer != null) {
            WorldRenderer current = client.worldRenderer;
            WorldRenderer expected = client.world == null ? vanillaRenderer : ((ClientWorldAccessor) client.world).backrooms$getWorldRenderer();
            if (current != expected) {
                ((MinecraftClientAccessor) client).backrooms$setWorldRenderer(expected);
                if (expected == vanillaRenderer) {
                    vanillaRenderer.setWorld(client.world);
                }
                if (current != vanillaRenderer) {
                    current.setWorld(null);
                    current.close();
                }
            }
            if (expected != vanillaRenderer) {
                vanillaRenderer.setWorld(null);
            } else {
                // Le renderer vanilla est de nouveau le principal : la prochaine traversee le retiendra a nouveau.
                vanillaRenderer = null;
            }
        }
        if (client.world != null) {
            client.getEntityRenderDispatcher().setWorld(client.world);
        }
    }

    private static void disposeMirror() {
        if (mirror != null) {
            mirror.dispose();
            mirror = null;
        }
    }
}
