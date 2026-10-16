package com.petassegang.addons.backrooms.portal.entity;

import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import com.petassegang.addons.init.ModBlocks;

/**
 * Entity persistante qui porte un portail : ou il est, ou il mene, et a quelle porte il appartient.
 *
 * <p>Les donnees utiles au client (orientations, destination, taille, visibilite) passent par le
 * {@link DataTracker} ; le reste (teleportable, porte proprietaire) ne sert qu'au serveur. Un portail
 * ne bouge jamais, ne subit aucune physique et ne peut pas etre frappe.
 */
public class BackroomsPortalEntity extends Entity {

    private static final String NBT_FACING_SOURCE = "FacingSource";
    private static final String NBT_FACING_DESTINATION = "FacingDestination";
    private static final String NBT_DESTINATION_DIM = "DestinationDim";
    private static final String NBT_DESTINATION_X = "DestinationX";
    private static final String NBT_DESTINATION_Y = "DestinationY";
    private static final String NBT_DESTINATION_Z = "DestinationZ";
    private static final String NBT_WIDTH = "Width";
    private static final String NBT_HEIGHT = "Height";
    private static final String NBT_TELEPORTABLE = "Teleportable";
    private static final String NBT_VISIBLE = "Visible";
    private static final String NBT_OWNER_DIM = "OwnerDim";
    private static final String NBT_OWNER_POS = "OwnerPos";

    /** Intervalle de verification de la porte proprietaire. */
    private static final int OWNER_CHECK_INTERVAL_TICKS = 100;
    /** Demi-epaisseur de la boite de detection autour du plan du portail. */
    private static final double DETECTION_HALF_THICKNESS = 0.25;
    /** Marge autour de la boite de detection ou chercher les entities qui ont pu traverser ce tick : la vitesse maximale des projectiles. */
    private static final double CROSSING_SEARCH_MARGIN = 3.0;
    private static final float MIN_SIZE = 0.1f;

    private static final TrackedData<Integer> TRACKED_FACING_SOURCE =
            DataTracker.registerData(BackroomsPortalEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> TRACKED_FACING_DESTINATION =
            DataTracker.registerData(BackroomsPortalEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<String> TRACKED_DESTINATION_DIM =
            DataTracker.registerData(BackroomsPortalEntity.class, TrackedDataHandlerRegistry.STRING);
    // Coordonnees d'arrivee en double exact (bits dans un long) : un float perd le centimetre loin de l'origine.
    private static final TrackedData<Long> TRACKED_DESTINATION_X =
            DataTracker.registerData(BackroomsPortalEntity.class, TrackedDataHandlerRegistry.LONG);
    private static final TrackedData<Long> TRACKED_DESTINATION_Y =
            DataTracker.registerData(BackroomsPortalEntity.class, TrackedDataHandlerRegistry.LONG);
    private static final TrackedData<Long> TRACKED_DESTINATION_Z =
            DataTracker.registerData(BackroomsPortalEntity.class, TrackedDataHandlerRegistry.LONG);
    private static final TrackedData<Float> TRACKED_WIDTH =
            DataTracker.registerData(BackroomsPortalEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> TRACKED_HEIGHT =
            DataTracker.registerData(BackroomsPortalEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Boolean> TRACKED_VISIBLE =
            DataTracker.registerData(BackroomsPortalEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    private boolean teleportable = true;
    private @Nullable RegistryKey<World> ownerDimension;
    private @Nullable BlockPos ownerPos;
    /** Boite de detection calculee a la demande ; invalidee quand la position change. */
    private @Nullable Box detectionBox;
    /** Centre d'arrivee lu dans le tracker, en cache : les transformations de vue le lisent a chaque image. */
    private @Nullable Vec3d destinationPosCache;
    private @Nullable Box crossingBox;
    private @Nullable Vec3d detectionBoxPos;

    public BackroomsPortalEntity(EntityType<? extends BackroomsPortalEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.intersectionChecked = false;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(TRACKED_FACING_SOURCE, PortalFacing.NORTH.ordinal());
        builder.add(TRACKED_FACING_DESTINATION, PortalFacing.SOUTH.ordinal());
        builder.add(TRACKED_DESTINATION_DIM, World.OVERWORLD.getValue().toString());
        builder.add(TRACKED_DESTINATION_X, Double.doubleToRawLongBits(0.0));
        builder.add(TRACKED_DESTINATION_Y, Double.doubleToRawLongBits(0.0));
        builder.add(TRACKED_DESTINATION_Z, Double.doubleToRawLongBits(0.0));
        builder.add(TRACKED_WIDTH, 1.0f);
        builder.add(TRACKED_HEIGHT, 2.0f);
        builder.add(TRACKED_VISIBLE, true);
    }

    @Override
    public void tick() {
        super.tick();
        // Position et dimension de la porte sont toujours fixees ensemble.
        if (this.ownerPos != null
                && this.age % OWNER_CHECK_INTERVAL_TICKS == 0
                && this.getWorld() instanceof ServerWorld serverWorld
                && isOrphan(serverWorld.getServer(), this.ownerDimension, this.ownerPos)) {
            discard();
        }
    }

    /** Vrai si la porte proprietaire a disparu alors que son chunk est charge ; un chunk decharge ne tranche pas. */
    private static boolean isOrphan(MinecraftServer server, RegistryKey<World> ownerDimension, BlockPos ownerPos) {
        ServerWorld ownerWorld = server.getWorld(ownerDimension);
        if (ownerWorld == null) {
            return true;
        }
        if (!ownerWorld.isChunkLoaded(ChunkPos.toLong(ownerPos))) {
            return false;
        }
        return !ownerWorld.getBlockState(ownerPos).isOf(ModBlocks.BACKROOMS_DOOR);
    }

    /** Orientation du portail dans son monde, normale vers le cote d'entree. */
    public PortalFacing facingSource() {
        return safeFacing(this.dataTracker.get(TRACKED_FACING_SOURCE));
    }

    /** Orientation du portail d'arrivee, normale vers le cote de sortie. */
    public PortalFacing facingDestination() {
        return safeFacing(this.dataTracker.get(TRACKED_FACING_DESTINATION));
    }

    /** Dimension d'arrivee ; l'overworld si la valeur synchronisee est illisible. */
    public RegistryKey<World> destinationDim() {
        Identifier id = Identifier.tryParse(this.dataTracker.get(TRACKED_DESTINATION_DIM));
        return id == null ? World.OVERWORLD : RegistryKey.of(RegistryKeys.WORLD, id);
    }

    /** Centre du portail d'arrivee. */
    public Vec3d destinationPos() {
        Vec3d cached = this.destinationPosCache;
        if (cached == null) {
            cached = new Vec3d(
                    Double.longBitsToDouble(this.dataTracker.get(TRACKED_DESTINATION_X)),
                    Double.longBitsToDouble(this.dataTracker.get(TRACKED_DESTINATION_Y)),
                    Double.longBitsToDouble(this.dataTracker.get(TRACKED_DESTINATION_Z)));
            this.destinationPosCache = cached;
        }
        return cached;
    }

    @Override
    public void onTrackedDataSet(TrackedData<?> data) {
        super.onTrackedDataSet(data);
        if (TRACKED_DESTINATION_X.equals(data) || TRACKED_DESTINATION_Y.equals(data) || TRACKED_DESTINATION_Z.equals(data)) {
            this.destinationPosCache = null;
        }
    }

    public float width() {
        return this.dataTracker.get(TRACKED_WIDTH);
    }

    public float height() {
        return this.dataTracker.get(TRACKED_HEIGHT);
    }

    public boolean teleportable() {
        return this.teleportable;
    }

    public void setTeleportable(boolean teleportable) {
        this.teleportable = teleportable;
    }

    public boolean visible() {
        return this.dataTracker.get(TRACKED_VISIBLE);
    }

    /** Dimension de la porte proprietaire, ou {@code null} pour un portail libre. */
    public @Nullable RegistryKey<World> ownerDimension() {
        return this.ownerDimension;
    }

    /** Position de la porte proprietaire, ou {@code null} pour un portail libre. */
    public @Nullable BlockPos ownerPos() {
        return this.ownerPos;
    }

    /** Boite autour du plan du portail (largeur, hauteur, un demi-bloc d'epaisseur), en coordonnees monde. */
    public Box detectionBox() {
        Vec3d center = getPos();
        if (this.detectionBox == null || this.detectionBoxPos != center) {
            PortalFacing facing = facingSource();
            Vec3d w = facing.axisW.multiply(width() / 2.0);
            Vec3d h = facing.axisH.multiply(height() / 2.0);
            Vec3d n = facing.normal.multiply(DETECTION_HALF_THICKNESS);
            this.detectionBox = new Box(center.subtract(w).subtract(h).subtract(n), center.add(w).add(h).add(n));
            this.crossingBox = this.detectionBox.expand(CROSSING_SEARCH_MARGIN);
            this.detectionBoxPos = center;
        }
        return this.detectionBox;
    }

    /** Boite de detection elargie de trois blocs : les entities qui s'y trouvent peuvent avoir traverse ce tick. */
    public Box crossingBox() {
        detectionBox();
        return this.crossingBox;
    }

    private void setDestinationPos(Vec3d destination) {
        this.dataTracker.set(TRACKED_DESTINATION_X, Double.doubleToRawLongBits(destination.x));
        this.dataTracker.set(TRACKED_DESTINATION_Y, Double.doubleToRawLongBits(destination.y));
        this.dataTracker.set(TRACKED_DESTINATION_Z, Double.doubleToRawLongBits(destination.z));
    }

    /**
     * {@code true} si un point du plan du portail tombe dans le cadre, elargi de {@code margin} de chaque cote.
     * Teste dans le repere du portail (largeur, hauteur) : le point est sur le plan par construction.
     */
    public boolean frameContains(double x, double y, double z, double margin) {
        PortalFacing facing = facingSource();
        double dx = x - getX();
        double dy = y - getY();
        double dz = z - getZ();
        double w = dx * facing.axisW.x + dy * facing.axisW.y + dz * facing.axisW.z;
        double h = dx * facing.axisH.x + dy * facing.axisH.y + dz * facing.axisH.z;
        return Math.abs(w) <= width() / 2.0 + margin && Math.abs(h) <= height() / 2.0 + margin;
    }

    /** Distance signee d'un point au plan du portail : positive du cote d'entree. */
    public double signedDistance(Vec3d point) {
        return signedDistance(point.x, point.y, point.z);
    }

    /** Variante sans allocation de {@link #signedDistance(Vec3d)}. */
    public double signedDistance(double x, double y, double z) {
        Vec3d normal = facingSource().normal;
        return (x - getX()) * normal.x + (y - getY()) * normal.y + (z - getZ()) * normal.z;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        String destinationDim = nbt.getString(NBT_DESTINATION_DIM);
        if (destinationDim.isBlank()) {
            destinationDim = World.OVERWORLD.getValue().toString();
        }
        this.dataTracker.set(TRACKED_FACING_SOURCE, readFacing(nbt, NBT_FACING_SOURCE, PortalFacing.NORTH).ordinal());
        this.dataTracker.set(TRACKED_FACING_DESTINATION, readFacing(nbt, NBT_FACING_DESTINATION, PortalFacing.SOUTH).ordinal());
        this.dataTracker.set(TRACKED_DESTINATION_DIM, destinationDim);
        setDestinationPos(new Vec3d(nbt.getDouble(NBT_DESTINATION_X), nbt.getDouble(NBT_DESTINATION_Y), nbt.getDouble(NBT_DESTINATION_Z)));
        this.dataTracker.set(TRACKED_WIDTH, Math.max(MIN_SIZE, nbt.getFloat(NBT_WIDTH)));
        this.dataTracker.set(TRACKED_HEIGHT, Math.max(MIN_SIZE, nbt.getFloat(NBT_HEIGHT)));
        this.dataTracker.set(TRACKED_VISIBLE, !nbt.contains(NBT_VISIBLE) || nbt.getBoolean(NBT_VISIBLE));
        this.teleportable = !nbt.contains(NBT_TELEPORTABLE) || nbt.getBoolean(NBT_TELEPORTABLE);

        // Identifier.tryParse("") renvoie "minecraft:" et non null : on teste la presence des cles.
        Identifier ownerDimId = nbt.contains(NBT_OWNER_DIM, NbtElement.STRING_TYPE)
                ? Identifier.tryParse(nbt.getString(NBT_OWNER_DIM))
                : null;
        if (ownerDimId != null && nbt.contains(NBT_OWNER_POS, NbtElement.LONG_TYPE)) {
            this.ownerDimension = RegistryKey.of(RegistryKeys.WORLD, ownerDimId);
            this.ownerPos = BlockPos.fromLong(nbt.getLong(NBT_OWNER_POS));
        } else {
            this.ownerDimension = null;
            this.ownerPos = null;
        }
        this.detectionBox = null;
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putString(NBT_FACING_SOURCE, facingSource().name());
        nbt.putString(NBT_FACING_DESTINATION, facingDestination().name());
        nbt.putString(NBT_DESTINATION_DIM, this.dataTracker.get(TRACKED_DESTINATION_DIM));
        Vec3d destination = destinationPos();
        nbt.putDouble(NBT_DESTINATION_X, destination.x);
        nbt.putDouble(NBT_DESTINATION_Y, destination.y);
        nbt.putDouble(NBT_DESTINATION_Z, destination.z);
        nbt.putFloat(NBT_WIDTH, width());
        nbt.putFloat(NBT_HEIGHT, height());
        nbt.putBoolean(NBT_TELEPORTABLE, this.teleportable);
        nbt.putBoolean(NBT_VISIBLE, visible());
        if (this.ownerDimension != null && this.ownerPos != null) {
            nbt.putString(NBT_OWNER_DIM, this.ownerDimension.getValue().toString());
            nbt.putLong(NBT_OWNER_POS, this.ownerPos.asLong());
        }
    }

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket(EntityTrackerEntry entityTrackerEntry) {
        return new EntitySpawnS2CPacket(this, entityTrackerEntry);
    }

    @Override
    public boolean canHit() {
        return false;
    }

    @Override
    public boolean shouldRender(double cameraX, double cameraY, double cameraZ) {
        return visible();
    }

    public static Builder builder(World world) {
        return new Builder(world);
    }

    private static PortalFacing safeFacing(int ordinal) {
        PortalFacing[] values = PortalFacing.values();
        return ordinal < 0 || ordinal >= values.length ? PortalFacing.NORTH : values[ordinal];
    }

    private static PortalFacing readFacing(NbtCompound nbt, String key, PortalFacing fallback) {
        try {
            return PortalFacing.valueOf(nbt.getString(key));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    /** Construction d'un portail complet ; refuse une configuration incomplete. */
    public static final class Builder {
        private final World world;
        private Vec3d position;
        private PortalFacing facingSource;
        private PortalFacing facingDestination;
        private RegistryKey<World> destinationDim;
        private Vec3d destinationPos;
        private float width = 1.0f;
        private float height = 2.0f;
        private boolean teleportable = true;
        private boolean visible = true;
        private RegistryKey<World> ownerDimension;
        private BlockPos ownerPos;

        private Builder(World world) {
            this.world = world;
        }

        public Builder position(Vec3d position) {
            this.position = position;
            return this;
        }

        public Builder facingSource(PortalFacing facingSource) {
            this.facingSource = facingSource;
            return this;
        }

        public Builder facingDestination(PortalFacing facingDestination) {
            this.facingDestination = facingDestination;
            return this;
        }

        public Builder destinationDim(RegistryKey<World> destinationDim) {
            this.destinationDim = destinationDim;
            return this;
        }

        public Builder destinationPos(Vec3d destinationPos) {
            this.destinationPos = destinationPos;
            return this;
        }

        public Builder size(float width, float height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder teleportable(boolean teleportable) {
            this.teleportable = teleportable;
            return this;
        }

        public Builder visible(boolean visible) {
            this.visible = visible;
            return this;
        }

        /** Porte proprietaire : le portail disparait avec elle. */
        public Builder owner(RegistryKey<World> ownerDimension, BlockPos ownerPos) {
            this.ownerDimension = ownerDimension;
            this.ownerPos = ownerPos;
            return this;
        }

        public BackroomsPortalEntity build() {
            if (this.position == null) {
                throw new IllegalStateException("Position du portail manquante.");
            }
            if (this.facingSource == null) {
                throw new IllegalStateException("Orientation source du portail manquante.");
            }
            if (this.facingDestination == null) {
                throw new IllegalStateException("Orientation destination du portail manquante.");
            }
            if (this.destinationDim == null) {
                throw new IllegalStateException("Dimension destination du portail manquante.");
            }
            if (this.destinationPos == null) {
                throw new IllegalStateException("Position destination du portail manquante.");
            }
            if (this.width <= 0.0f || this.height <= 0.0f) {
                throw new IllegalStateException("Taille du portail invalide : " + this.width + "x" + this.height + ".");
            }

            BackroomsPortalEntity portal = new BackroomsPortalEntity(BackroomsPortalEntityType.INSTANCE, this.world);
            portal.setPosition(this.position);
            portal.dataTracker.set(TRACKED_FACING_SOURCE, this.facingSource.ordinal());
            portal.dataTracker.set(TRACKED_FACING_DESTINATION, this.facingDestination.ordinal());
            portal.dataTracker.set(TRACKED_DESTINATION_DIM, this.destinationDim.getValue().toString());
            portal.setDestinationPos(this.destinationPos);
            portal.dataTracker.set(TRACKED_WIDTH, this.width);
            portal.dataTracker.set(TRACKED_HEIGHT, this.height);
            portal.dataTracker.set(TRACKED_VISIBLE, this.visible);
            portal.teleportable = this.teleportable;
            portal.ownerDimension = this.ownerDimension;
            portal.ownerPos = this.ownerPos;
            return portal;
        }
    }
}
