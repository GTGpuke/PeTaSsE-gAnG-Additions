package com.gtgpuke.gtgsbackrooms.gametest.support;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;

import net.minecraft.network.ClientConnection;
import net.minecraft.network.DisconnectionInfo;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.network.packet.c2s.play.AcknowledgeChunksC2SPacket;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.TestContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

/**
 * Joueurs simules des tests en jeu : connectes au serveur comme un vrai client (liste des joueurs, suivi des
 * chunks, evenements de connexion), sur un canal reseau en memoire qui garde les paquets envoyes au joueur.
 *
 * <p>Le serveur ne tique pas lui-meme le gestionnaire reseau d'un tel joueur (il n'est pas dans sa liste de
 * connexions) : {@link #tick} le fait a la demande, pour l'envoi des chunks et le tick du joueur.
 */
public final class GameTestPlayers {

    /**
     * Arene vide de 24 x 8 x 24 blocs (structure du mod) : les tests qui posent des blocs, des entities ou des joueurs
     * y tiennent sans deborder sur leurs voisins, ce que la structure vide de Fabric (un bloc) ne garantit pas.
     */
    public static final String ARENA = "gtgs_backrooms_gametest:test_arena";

    private static final Map<UUID, Connection> CONNECTIONS = new HashMap<>();
    private static final float CHUNKS_PER_TICK = 16.0f;

    /**
     * Connexion d'un joueur simule : le canal en memoire retient ce que le serveur lui envoie, et {@code unread} garde ce
     * qui en a ete sorti sans avoir encore ete demande.
     */
    private record Connection(ClientConnection connection, EmbeddedChannel channel, List<Object> unread) {
    }

    private GameTestPlayers() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /** Connecte un joueur dans le monde du test, au mode de jeu voulu, et le place a la position donnee. */
    public static ServerPlayerEntity connect(TestContext context, GameMode mode, Vec3d position, float yaw, float pitch) {
        MinecraftServer server = context.getWorld().getServer();
        ServerPlayerEntity player = new ServerPlayerEntity(server, context.getWorld(),
                new GameProfile(UUID.randomUUID(), "testeur_" + mode.getName()), SyncedClientOptions.createDefault());
        ClientConnection connection = new ClientConnection(NetworkSide.SERVERBOUND);
        EmbeddedChannel channel = new EmbeddedChannel(connection);
        server.getPlayerManager().onPlayerConnect(connection, player, ConnectedClientData.createDefault(player.getGameProfile(), false));
        CONNECTIONS.put(player.getUuid(), new Connection(connection, channel, new ArrayList<>()));
        player.changeGameMode(mode);
        player.refreshPositionAndAngles(position.x, position.y, position.z, yaw, pitch);
        // Le serveur ne simule pas la gravite d'un joueur : sans client pour le dire, il flotterait et minerait cinq fois moins vite.
        player.setOnGround(true);
        player.networkHandler.syncWithPlayerPosition();
        // Sans deplacement recu du client, le joueur ne regarderait aucun chunk et ne suivrait aucune entity.
        context.getWorld().getChunkManager().updatePosition(player);
        return player;
    }

    /**
     * Un tick du gestionnaire reseau du joueur : tick du joueur lui-meme et envoi d'un lot de chunks, que le faux
     * client acquitte aussitot (sans accuse de reception, vanilla n'envoie qu'un seul lot).
     */
    public static void tick(ServerPlayerEntity player) {
        if (!player.isDisconnected()) {
            player.networkHandler.tick();
            player.networkHandler.onAcknowledgeChunks(new AcknowledgeChunksC2SPacket(CHUNKS_PER_TICK));
        }
    }

    /** Deconnecte le joueur comme le ferait la fermeture de son canal : evenements de deconnexion compris. */
    public static void disconnect(ServerPlayerEntity player) {
        close(CONNECTIONS.remove(player.getUuid()));
    }

    /**
     * Deconnecte tous les joueurs simules encore en ligne. Un test qui echoue en cours de route ne passe pas par son
     * nettoyage : les tests sensibles aux autres joueurs repartent ainsi d'un serveur vide.
     */
    public static void disconnectAll() {
        List<Connection> open = new ArrayList<>(CONNECTIONS.values());
        CONNECTIONS.clear();
        open.forEach(GameTestPlayers::close);
    }

    private static void close(Connection connection) {
        if (connection != null && connection.connection().isOpen()) {
            connection.connection().disconnect(new DisconnectionInfo(Text.literal("Fin du test.")));
            connection.connection().handleDisconnection();
        }
    }

    /** Fait recevoir au serveur un payload comme s'il venait du client de ce joueur. */
    public static void receive(ServerPlayerEntity player, CustomPayload payload) {
        player.networkHandler.onCustomPayload(new CustomPayloadC2SPacket(payload));
    }

    /**
     * Ce que le serveur a envoye au joueur, du type demande, depuis la derniere lecture de ce type : paquets vanilla, ou
     * payloads du mod deballes de leur paquet. Les messages rendus sont oublies ; ceux des autres types restent a lire.
     *
     * <p>Pendant le tick des mondes, vanilla ecrit les paquets sans les pousser et ne vide les connexions qu'a la fin : la
     * connexion est videe ici, pour lire aussi ce que le tick en cours vient d'envoyer.
     */
    public static <T> List<T> sent(ServerPlayerEntity player, Class<T> type) {
        List<T> found = new ArrayList<>();
        Connection connection = CONNECTIONS.get(player.getUuid());
        if (connection == null) {
            return found;
        }
        connection.connection().flush();
        Object message;
        while ((message = connection.channel().readOutbound()) != null) {
            connection.unread().add(message instanceof CustomPayloadS2CPacket packet ? packet.payload() : message);
        }
        connection.unread().removeIf(content -> {
            if (!type.isInstance(content)) {
                return false;
            }
            found.add(type.cast(content));
            return true;
        });
        return found;
    }
}
