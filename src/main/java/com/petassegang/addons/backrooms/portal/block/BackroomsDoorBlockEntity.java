package com.petassegang.addons.backrooms.portal.block;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import com.petassegang.addons.backrooms.portal.entity.BackroomsPortalEntity;
import com.petassegang.addons.backrooms.portal.entity.PortalFacing;
import com.petassegang.addons.core.ModConstants;
import com.petassegang.addons.init.ModBlockEntities;

/**
 * Block entity de la moitie basse de la porte : trouve sa destination, pose ses portails et les retire avec elle.
 *
 * <p>Une porte posee hors du Level 0 mene au Level 0 ; posee dans le Level 0, elle ramene a la surface.
 * Le portail d'entree remplit l'encadrement des que la porte connait sa destination ; fermee, la porte le cache et
 * rien ne le traverse, mais l'autre cote reste synchronise pour que l'ouverture soit immediate. Le portail de retour
 * attend au point d'arrivee et ramene dans le plan meme de la porte, au milieu de son bloc : c'est l'inverse exact du
 * portail d'entree, chaque cote montre l'autre tel qu'on le traverse. Il se ferme avec la porte ; ouverte, son battant
 * et les murs qui l'encadrent arretent de l'autre cote ceux qui s'y engageraient de biais ({@code PortalCollision}).
 * Chacun connait la porte qui le possede : le registre des portails retire celui dont la porte n'existe plus.
 */
public final class BackroomsDoorBlockEntity extends BlockEntity {

    /** Largeur des portails de porte, en blocs : l'encadrement. */
    public static final float PORTAL_WIDTH = 1.0f;
    /** Hauteur des portails de porte, en blocs : les deux moities de la porte. */
    public static final float PORTAL_HEIGHT = 2.0f;
    /** Delai avant de considerer le portail d'entree perdu, le temps que le chunk charge ses entities. */
    public static final int PORTAL_GRACE_TICKS = 40;
    /** Intervalle entre deux tentatives de resolution de la destination ; la premiere a lieu au premier tick. */
    public static final int TARGET_RETRY_INTERVAL_TICKS = 100;
    /** Intervalle de verification du portail de retour, dans l'autre dimension. */
    public static final int RETURN_CHECK_INTERVAL_TICKS = 200;
    /** Verifications consecutives sans portail de retour avant de le reposer. */
    public static final int RETURN_MISSING_CHECKS = 2;

    /** Portails actives par le point d'entree du mod ; desactives, la porte reste une porte ordinaire, sans portail. */
    private static boolean portalsEnabled;

    private static final String NBT_ATTACHED_PORTAL = "AttachedPortal";
    private static final String NBT_RETURN_PORTAL = "ReturnPortal";
    private static final String NBT_DESTINATION_DIM = "DestDim";
    private static final String NBT_DESTINATION_X = "DestX";
    private static final String NBT_DESTINATION_Y = "DestY";
    private static final String NBT_DESTINATION_Z = "DestZ";
    private static final String NBT_DESTINATION_FACING = "DestFacing";

    private @Nullable UUID attachedPortalUuid;
    private @Nullable UUID returnPortalUuid;
    private @Nullable RegistryKey<World> destinationDim;
    private @Nullable Vec3d destinationPos;
    private @Nullable PortalFacing destinationFacing;
    /** Centre du portail d'entree : le milieu de l'encadrement, sur les deux blocs de la porte. */
    private final Vec3d entryPortalPos;
    private int ticksSinceLoad;
    private int returnPortalMissingChecks;

    /** Block entity de la moitie basse, creee par le bloc a la pose ou au chargement. */
    public BackroomsDoorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BACKROOMS_DOOR, pos, state);
        this.entryPortalPos = Vec3d.ofCenter(pos).add(0.0, PORTAL_HEIGHT / 2.0 - 0.5, 0.0);
    }

    /**
     * Fixe la destination de la porte et, dans un monde serveur, pose aussitot ses deux portails.
     *
     * @param destinationDim dimension d'arrivee
     * @param destinationPos centre du portail d'arrivee
     * @param destinationFacing orientation du portail d'arrivee, normale vers la sortie
     */
    public void configureTarget(RegistryKey<World> destinationDim, Vec3d destinationPos, PortalFacing destinationFacing) {
        this.destinationDim = destinationDim;
        this.destinationPos = destinationPos;
        this.destinationFacing = destinationFacing;
        this.markDirty();
        if (this.world instanceof ServerWorld serverWorld) {
            syncEntryPortal(serverWorld);
            spawnReturnPortal(serverWorld);
        }
    }

    /** Active ou desactive les portails des portes (point d'entree du mod, tests). */
    public static void setPortalsEnabled(boolean enabled) {
        portalsEnabled = enabled;
    }

    /**
     * Tick serveur : au premier tick, complete une porte sauvegardee en un seul bloc ; puis, portails actives, resout la
     * destination si besoin, aligne le portail d'entree sur la porte et veille sur le retour.
     */
    public void serverTick() {
        if (!(this.world instanceof ServerWorld serverWorld)) {
            return;
        }
        this.ticksSinceLoad++;
        if (this.ticksSinceLoad == 1) {
            repairUpperHalf(serverWorld);
        }
        if (!portalsEnabled) {
            return;
        }
        if (!isConfigured()) {
            if (this.ticksSinceLoad % TARGET_RETRY_INTERVAL_TICKS == 1) {
                resolveTarget(serverWorld);
            }
            return;
        }
        syncEntryPortal(serverWorld);
        syncReturnPortal(serverWorld);
        if (this.ticksSinceLoad % RETURN_CHECK_INTERVAL_TICKS == 0) {
            checkReturnPortal(serverWorld);
        }
    }

    /**
     * Moitie haute absente au chargement : la porte a ete sauvegardee quand elle tenait en un seul bloc, orientee vers son
     * cote d'entree. Elle est retournee pour garder ce cote d'entree (celui d'une porte a deux moities est l'oppose de son
     * orientation), et sa moitie haute est reposee, fermee comme la basse, si la place est libre ou remplacable (herbe,
     * neige). Une porte complete garde ses deux moities ensemble : il suffit de verifier au premier tick.
     */
    private void repairUpperHalf(ServerWorld world) {
        BlockPos above = this.pos.up();
        BlockState aboveState = world.getBlockState(above);
        BlockState lower = getCachedState();
        if (aboveState.isOf(lower.getBlock())) {
            return;
        }
        BlockState turned = lower.with(DoorBlock.FACING, lower.get(DoorBlock.FACING).getOpposite());
        // Sans mise a jour de voisinage : seule, la moitie basse se retirerait.
        world.setBlockState(this.pos, turned, Block.NOTIFY_LISTENERS);
        if (aboveState.isReplaceable()) {
            world.setBlockState(above, turned.with(DoorBlock.HALF, DoubleBlockHalf.UPPER), Block.NOTIFY_ALL);
        }
    }

    /** {@code true} si la porte est ouverte : les deux portails ne se traversent qu'alors. */
    public boolean isOpen() {
        return getCachedState().get(DoorBlock.OPEN);
    }

    /** Retire les deux portails ; un portail dont le chunk n'est pas charge se retirera seul plus tard. */
    public void discardPortals() {
        if (!(this.world instanceof ServerWorld serverWorld)) {
            return;
        }
        discard(serverWorld, this.attachedPortalUuid);
        if (this.destinationDim != null) {
            ServerWorld destination = serverWorld.getServer().getWorld(this.destinationDim);
            if (destination != null) {
                discard(destination, this.returnPortalUuid);
            }
        }
        this.attachedPortalUuid = null;
        this.returnPortalUuid = null;
        this.markDirty();
    }

    /** {@code true} des que la porte connait sa destination. */
    public boolean isConfigured() {
        return this.destinationDim != null && this.destinationPos != null && this.destinationFacing != null;
    }

    /** Portail d'entree pose dans l'encadrement, ou {@code null} tant que la porte n'a pas de destination. */
    public @Nullable UUID attachedPortalUuid() {
        return this.attachedPortalUuid;
    }

    /** Portail de retour pose au point d'arrivee, ou {@code null} s'il n'a pas pu l'etre. */
    public @Nullable UUID returnPortalUuid() {
        return this.returnPortalUuid;
    }

    private void resolveTarget(ServerWorld world) {
        BackroomsDoorTargets.Target target = BackroomsDoorTargets.resolve(world, this.pos, entrySide());
        if (target == null) {
            ModConstants.LOGGER.debug("[Portal] Porte {} sans destination pour l'instant.", this.pos);
            return;
        }
        configureTarget(target.dimension(), target.position(), target.facing());
    }

    /**
     * Le portail d'entree existe tant que la porte connait sa destination ; la porte le ferme et le cache, elle ne le
     * retire pas. Un portail qui ne remplit plus l'encadrement (porte tournee, sauvegarde d'un ancien placement) est
     * remplace.
     */
    private void syncEntryPortal(ServerWorld world) {
        Entity entity = this.attachedPortalUuid == null ? null : world.getEntity(this.attachedPortalUuid);
        if (entity instanceof BackroomsPortalEntity portal) {
            if (portal.getPos().equals(this.entryPortalPos) && portal.facingSource() == entryFacing()) {
                boolean open = isOpen();
                if (portal.teleportable() != open) {
                    portal.setTeleportable(open);
                }
                return;
            }
            portal.discard();
        } else if (this.attachedPortalUuid != null && this.ticksSinceLoad < PORTAL_GRACE_TICKS) {
            // Le chunk vient de charger : ses entities arrivent, on laisse au portail le temps d'apparaitre.
            return;
        }
        BackroomsPortalEntity portal = BackroomsPortalEntity.builder(world)
                .position(this.entryPortalPos)
                .facingSource(entryFacing())
                .facingDestination(this.destinationFacing)
                .destinationDim(this.destinationDim)
                .destinationPos(this.destinationPos)
                .size(PORTAL_WIDTH, PORTAL_HEIGHT)
                .teleportable(isOpen())
                .owner(world.getRegistryKey(), this.pos)
                .build();
        world.spawnEntity(portal);
        this.attachedPortalUuid = portal.getUuid();
        this.markDirty();
    }

    /**
     * Le portail de retour se ferme avec la porte : fermee, elle ne se traverse dans aucun sens. Il mene toujours au plan
     * de la porte, dans son orientation : un retour qui ressortirait ailleurs (sauvegarde d'un ancien placement) y est
     * ramene, celui d'une porte tournee depuis est repose.
     */
    private void syncReturnPortal(ServerWorld world) {
        ServerWorld destination = world.getServer().getWorld(this.destinationDim);
        Entity entity = destination == null || this.returnPortalUuid == null ? null : destination.getEntity(this.returnPortalUuid);
        if (!(entity instanceof BackroomsPortalEntity portal)) {
            return;
        }
        if (portal.facingDestination() != entryFacing()) {
            portal.discard();
            spawnReturnPortal(world);
            return;
        }
        boolean open = isOpen();
        if (portal.teleportable() != open) {
            portal.setTeleportable(open);
        }
        if (!this.entryPortalPos.equals(portal.destinationPos())) {
            portal.setDestinationPos(this.entryPortalPos);
        }
    }

    private void spawnReturnPortal(ServerWorld world) {
        ServerWorld destination = world.getServer().getWorld(this.destinationDim);
        if (destination == null) {
            return;
        }
        // Charge le chunk d'arrivee une fois : le retour doit exister avant la premiere traversee.
        destination.getChunk(ChunkSectionPos.getSectionCoord(this.destinationPos.x), ChunkSectionPos.getSectionCoord(this.destinationPos.z));
        BackroomsPortalEntity portal = BackroomsPortalEntity.builder(destination)
                .position(this.destinationPos)
                .facingSource(this.destinationFacing)
                .facingDestination(entryFacing())
                .destinationDim(world.getRegistryKey())
                .destinationPos(this.entryPortalPos)
                .size(PORTAL_WIDTH, PORTAL_HEIGHT)
                .teleportable(isOpen())
                .owner(world.getRegistryKey(), this.pos)
                .build();
        destination.spawnEntity(portal);
        this.returnPortalUuid = portal.getUuid();
        this.returnPortalMissingChecks = 0;
        this.markDirty();
    }

    private void checkReturnPortal(ServerWorld world) {
        ServerWorld destination = world.getServer().getWorld(this.destinationDim);
        if (destination == null) {
            return;
        }
        if (this.returnPortalUuid != null && destination.getEntity(this.returnPortalUuid) != null) {
            this.returnPortalMissingChecks = 0;
            return;
        }
        // Sans chunk d'arrivee actif, impossible de savoir si le portail manque vraiment.
        if (!destination.shouldTickEntity(BlockPos.ofFloored(this.destinationPos))) {
            return;
        }
        if (++this.returnPortalMissingChecks >= RETURN_MISSING_CHECKS) {
            spawnReturnPortal(world);
        }
    }

    private Direction entrySide() {
        return BackroomsDoorBlock.entrySide(getCachedState());
    }

    private PortalFacing entryFacing() {
        return PortalFacing.fromDirection(entrySide());
    }

    private static void discard(ServerWorld world, @Nullable UUID uuid) {
        if (uuid != null) {
            Entity portal = world.getEntity(uuid);
            if (portal != null) {
                portal.discard();
            }
        }
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        this.attachedPortalUuid = nbt.containsUuid(NBT_ATTACHED_PORTAL) ? nbt.getUuid(NBT_ATTACHED_PORTAL) : null;
        this.returnPortalUuid = nbt.containsUuid(NBT_RETURN_PORTAL) ? nbt.getUuid(NBT_RETURN_PORTAL) : null;
        // Identifier.tryParse("") renvoie "minecraft:" et non null : il faut tester la presence de la cle.
        Identifier destinationDimId = nbt.contains(NBT_DESTINATION_DIM, NbtElement.STRING_TYPE)
                ? Identifier.tryParse(nbt.getString(NBT_DESTINATION_DIM))
                : null;
        this.destinationDim = destinationDimId == null ? null : RegistryKey.of(RegistryKeys.WORLD, destinationDimId);
        this.destinationPos = nbt.contains(NBT_DESTINATION_X)
                ? new Vec3d(nbt.getDouble(NBT_DESTINATION_X), nbt.getDouble(NBT_DESTINATION_Y), nbt.getDouble(NBT_DESTINATION_Z))
                : null;
        try {
            this.destinationFacing = nbt.contains(NBT_DESTINATION_FACING)
                    ? PortalFacing.valueOf(nbt.getString(NBT_DESTINATION_FACING))
                    : null;
        } catch (IllegalArgumentException ignored) {
            this.destinationFacing = null;
        }
        this.ticksSinceLoad = 0;
        this.returnPortalMissingChecks = 0;
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        if (this.attachedPortalUuid != null) {
            nbt.putUuid(NBT_ATTACHED_PORTAL, this.attachedPortalUuid);
        }
        if (this.returnPortalUuid != null) {
            nbt.putUuid(NBT_RETURN_PORTAL, this.returnPortalUuid);
        }
        if (this.destinationDim != null) {
            nbt.putString(NBT_DESTINATION_DIM, this.destinationDim.getValue().toString());
        }
        if (this.destinationPos != null) {
            nbt.putDouble(NBT_DESTINATION_X, this.destinationPos.x);
            nbt.putDouble(NBT_DESTINATION_Y, this.destinationPos.y);
            nbt.putDouble(NBT_DESTINATION_Z, this.destinationPos.z);
        }
        if (this.destinationFacing != null) {
            nbt.putString(NBT_DESTINATION_FACING, this.destinationFacing.name());
        }
    }
}
