package com.petassegang.addons.backrooms.portal.block;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
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
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

import com.petassegang.addons.backrooms.portal.entity.BackroomsPortalEntity;
import com.petassegang.addons.backrooms.portal.entity.PortalFacing;
import com.petassegang.addons.core.ModConstants;
import com.petassegang.addons.init.ModBlockEntities;

/**
 * Block entity de la porte : trouve sa destination, pose ses deux portails et les retire avec elle.
 *
 * <p>Une porte posee hors du Level 0 mene au Level 0 ; posee dans le Level 0, elle ramene a la surface.
 * Le portail d'entree se tient devant la porte, le portail de retour au point d'arrivee ; chacun connait
 * la porte qui le possede et disparait de lui-meme si elle n'existe plus.
 */
public final class BackroomsDoorBlockEntity extends BlockEntity {

    /** Largeur des portails de porte, en blocs. */
    static final float PORTAL_WIDTH = 1.0f;
    /** Hauteur des portails de porte, en blocs. */
    static final float PORTAL_HEIGHT = 2.0f;
    /** Distance du plan du portail d'entree devant la face de la porte. */
    static final double PORTAL_OFFSET = 0.55;
    /** Delai avant de considerer le portail d'entree perdu, le temps que le chunk charge ses entities. */
    private static final int PORTAL_GRACE_TICKS = 40;
    /** Intervalle entre deux tentatives de resolution de la destination. */
    private static final int TARGET_RETRY_INTERVAL_TICKS = 100;
    /** Intervalle de verification du portail de retour, dans l'autre dimension. */
    private static final int RETURN_CHECK_INTERVAL_TICKS = 200;
    /** Verifications consecutives sans portail de retour avant de le reposer. */
    private static final int RETURN_MISSING_CHECKS = 2;

    /** Portails actives par le point d'entree du mod ; desactives, la porte reste decorative et ne tique pas. */
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
    private int ticksSinceLoad;
    private int returnPortalMissingChecks;

    public BackroomsDoorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BACKROOMS_DOOR, pos, state);
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
            ensureEntryPortal(serverWorld);
            spawnReturnPortal(serverWorld);
        }
    }

    /** Active ou desactive les portails des portes (point d'entree du mod, tests). */
    public static void setPortalsEnabled(boolean enabled) {
        portalsEnabled = enabled;
    }

    /** Tick serveur : resout la destination si besoin, puis veille sur les deux portails. */
    public void serverTick() {
        if (!portalsEnabled || !(this.world instanceof ServerWorld serverWorld)) {
            return;
        }
        this.ticksSinceLoad++;
        if (!isConfigured()) {
            if (this.ticksSinceLoad % TARGET_RETRY_INTERVAL_TICKS == 1) {
                resolveTarget(serverWorld);
            }
            return;
        }
        ensureEntryPortal(serverWorld);
        if (this.ticksSinceLoad % RETURN_CHECK_INTERVAL_TICKS == 0) {
            checkReturnPortal(serverWorld);
        }
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

    public @Nullable UUID attachedPortalUuid() {
        return this.attachedPortalUuid;
    }

    public @Nullable UUID returnPortalUuid() {
        return this.returnPortalUuid;
    }

    private void resolveTarget(ServerWorld world) {
        BackroomsDoorTargets.Target target = BackroomsDoorTargets.resolve(world, this.pos, doorFacing().direction);
        if (target == null) {
            ModConstants.LOGGER.debug("[Portal] Porte {} sans destination pour l'instant.", this.pos);
            return;
        }
        configureTarget(target.dimension(), target.position(), target.facing());
    }

    private void ensureEntryPortal(ServerWorld world) {
        if (this.attachedPortalUuid != null
                && (world.getEntity(this.attachedPortalUuid) != null || this.ticksSinceLoad < PORTAL_GRACE_TICKS)) {
            return;
        }
        BackroomsPortalEntity portal = BackroomsPortalEntity.builder(world)
                .position(entryPortalPos())
                .facingSource(doorFacing())
                .facingDestination(this.destinationFacing)
                .destinationDim(this.destinationDim)
                .destinationPos(this.destinationPos)
                .size(PORTAL_WIDTH, PORTAL_HEIGHT)
                .owner(world.getRegistryKey(), this.pos)
                .build();
        world.spawnEntity(portal);
        this.attachedPortalUuid = portal.getUuid();
        this.markDirty();
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
                .facingDestination(doorFacing())
                .destinationDim(world.getRegistryKey())
                .destinationPos(entryPortalPos())
                .size(PORTAL_WIDTH, PORTAL_HEIGHT)
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

    private PortalFacing doorFacing() {
        return PortalFacing.fromDirection(getCachedState().get(BackroomsDoorBlock.FACING));
    }

    /** Centre du portail d'entree : devant la face de la porte, le bas du cadre pose sur le sol trouve devant elle. */
    private Vec3d entryPortalPos() {
        Vec3d center = Vec3d.ofCenter(this.pos).add(doorFacing().normal.multiply(PORTAL_OFFSET));
        return new Vec3d(center.x, floorInFrontOfDoor() + PORTAL_HEIGHT / 2.0, center.z);
    }

    /**
     * Hauteur du sol devant la porte : dessus du premier bloc a collision, cherche du niveau de la porte jusqu'a deux
     * blocs plus bas (porte encastree dans le sol, ou posee sur une marche). Sans sol, le bas de la porte.
     */
    private double floorInFrontOfDoor() {
        BlockPos front = this.pos.offset(getCachedState().get(BackroomsDoorBlock.FACING));
        for (int y = front.getY(); y >= front.getY() - 2; y--) {
            BlockPos probe = new BlockPos(front.getX(), y, front.getZ());
            VoxelShape shape = this.world.getBlockState(probe).getCollisionShape(this.world, probe);
            if (!shape.isEmpty()) {
                return y + shape.getMax(Direction.Axis.Y);
            }
        }
        return this.pos.getY();
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
