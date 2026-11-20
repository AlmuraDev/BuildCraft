/*
 * Copyright (c) 2016 SpaceToad and the BuildCraft team
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.lib.tile;

import java.io.IOException;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.Sets;
import com.mojang.authlib.GameProfile;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.NonNullList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

import net.neoforged.neoforge.capabilities.BlockCapability;
import buildcraft.lib.net.IMessage;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.fml.LogicalSide;
import buildcraft.api.transfer.IItemHandlerModifiable;

import buildcraft.api.core.BCDebugging;
import buildcraft.api.core.BCLog;
import buildcraft.api.core.ICapabilityProvider;
import buildcraft.api.core.IPlayerOwned;

import buildcraft.lib.cache.CachedChunk;
import buildcraft.lib.cache.IChunkCache;
import buildcraft.lib.cache.ITileCache;
import buildcraft.lib.cache.TileCacheRet;
import buildcraft.lib.cache.TileCacheType;
import buildcraft.lib.block.BlockBCTile_Neptune;
import buildcraft.lib.cap.CapabilityHelper;
import buildcraft.lib.debug.IDetachedRenderer;
import buildcraft.lib.debug.BCAdvDebugging;
import buildcraft.lib.debug.IAdvDebugTarget;
import buildcraft.lib.delta.DeltaManager;
import buildcraft.lib.delta.DeltaManager.EnumDeltaMessage;
import buildcraft.lib.fluid.TankManager;
import buildcraft.lib.migrate.BCVersion;
import buildcraft.lib.misc.BlockUtil;
import buildcraft.lib.misc.ChunkUtil;
import buildcraft.lib.misc.FakePlayerProvider;
import buildcraft.lib.misc.InventoryUtil;
import buildcraft.lib.misc.MessageUtil;
import buildcraft.lib.misc.NBTUtilBC;
import buildcraft.lib.misc.PermissionUtil;
import buildcraft.lib.misc.PermissionUtil.PermissionBlock;
import buildcraft.lib.misc.StringUtilBC;
import buildcraft.lib.misc.data.IdAllocator;
import buildcraft.lib.net.IPayloadReceiver;
import buildcraft.lib.net.IPayloadWriter;
import buildcraft.lib.net.MessageManager;
import buildcraft.lib.net.MessageUpdateTile;
import buildcraft.lib.net.PacketBufferBC;
import buildcraft.lib.registry.TileTypes;
import buildcraft.lib.tile.item.ItemHandlerManager;

public abstract class TileBC_Neptune extends BlockEntity
    implements IPayloadReceiver, IAdvDebugTarget, IPlayerOwned, ICapabilityProvider {
    public static final boolean DEBUG = BCDebugging.shouldDebugLog("lib.tile");

    protected static final IdAllocator IDS = new IdAllocator("tile");

    /** Used for sending all data used for rendering the tile on a client. This does not include items, power, stages,
     * etc (Unless some are shown in the world) */
    public static final int NET_RENDER_DATA = IDS.allocId("RENDER_DATA");
    /** Used for sending all data in the GUI. Basically what has been omitted from {@link #NET_RENDER_DATA} that is
     * shown in the GUI. */
    public static final int NET_GUI_DATA = IDS.allocId("GUI_DATA");
    /** Used for sending the data that would normally be sent with {@link AbstractContainerMenu#detectAndSendChanges()}. Note that
     * if no bytes are written then the update message won't be sent. You should detect if any changes have been made to
     * the gui since the last tick, so you don't resend duplicate information if nothing has changed by the next
     * tick. */
    public static final int NET_GUI_TICK = IDS.allocId("GUI_TICK");

    public static final int NET_REN_DELTA_SINGLE = IDS.allocId("REN_DELTA_SINGLE");
    public static final int NET_REN_DELTA_CLEAR = IDS.allocId("REN_DELTA_CLEAR");
    public static final int NET_GUI_DELTA_SINGLE = IDS.allocId("GUI_DELTA_SINGLE");
    public static final int NET_GUI_DELTA_CLEAR = IDS.allocId("GUI_DELTA_CLEAR");

    /** Used for detailed debugging for inspecting every part of the current tile. For example, tanks use this to
     * display which other tanks makeup the whole structure. */
    public static final int NET_ADV_DEBUG = IDS.allocId("DEBUG_DATA");
    public static final int NET_ADV_DEBUG_DISABLE = IDS.allocId("DEBUG_DISABLE");

    /** Used to tell the client to redraw the block. */
    public static final int NET_REDRAW = IDS.allocId("REDRAW");

    protected final CapabilityHelper caps = new CapabilityHelper();
    protected final ItemHandlerManager itemManager = new ItemHandlerManager(this::onSlotChange);
    protected final TankManager tankManager = new TankManager();

    /** Handles all of the players that are currently using this tile (have a GUI open) */
    private final Set<Player> usingPlayers = Sets.newIdentityHashSet();
    private GameProfile owner;

    private final IChunkCache chunkCache = new CachedChunk(this);
    private final ITileCache tileCache = TileCacheType.NEIGHBOUR_CACHE.create(this);

    protected final DeltaManager deltaManager = new DeltaManager((gui, type, writer) -> {
        final int id;
        if (type == EnumDeltaMessage.ADD_SINGLE) {
            id = gui ? NET_GUI_DELTA_SINGLE : NET_REN_DELTA_SINGLE;
        } else if (type == EnumDeltaMessage.SET_VALUE) {
            id = gui ? NET_GUI_DELTA_CLEAR : NET_REN_DELTA_CLEAR;
        } else {
            throw new IllegalArgumentException("Unknown delta message type " + type);
        }
        if (gui) {
            createAndSendGuiMessage(id, writer);
        } else {
            createAndSendMessage(id, writer);
        }
    });

    /** The world this tile is in (an alias of {@link BlockEntity}'s level, kept so 1.12.2 code reads the same). */
    protected Level world;

    /** This tile's position (an alias of {@link BlockEntity}'s position). */
    protected final BlockPos pos;

    /** Every BuildCraft tile is created for a {@link BlockBCTile_Neptune}, which knows its {@link BlockEntityType}. */
    public TileBC_Neptune(BlockPos pos, BlockState state) {
        super(TileTypes.forState(state), pos, state);
        // The chunk loader passes one MutableBlockPos for every tile in the chunk: keep our own copy
        this.pos = pos.immutable();
        caps.addProvider(itemManager);
    }

    /** Saved data read before this tile had a level (see {@link #loadAdditional(ValueInput)}). */
    @Nullable
    private CompoundTag pendingNbt;

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        this.world = level;
        if (pendingNbt != null && level != null) {
            CompoundTag nbt = pendingNbt;
            pendingNbt = null;
            readFromNBT(nbt);
        }
    }

    public final Level getWorld() {
        return level;
    }

    public final BlockPos getPos() {
        return worldPosition;
    }

    public final boolean hasWorld() {
        return level != null;
    }

    // ##################################################
    //
    // Local blockstate + tile entity getters
    //
    // Some of these (may) use a cached version
    // at some point in the future, or are already
    // based on a cache.
    //
    // ##################################################

    public final BlockState getCurrentState() {
        return BlockUtil.getBlockState(world, pos);
    }

    @Nullable
    public final BlockState getCurrentStateForBlock(Block expectedBlock) {
        BlockState state = getCurrentState();
        if (state.getBlock() == expectedBlock) {
            return state;
        }
        return null;
    }

    public final BlockState getNeighbourState(Direction offset) {
        // In the future it is plausible that we might cache block states here.
        // However, until that is implemented, just call the world directly.
        return getOffsetState(offset.getUnitVec3i());
    }

    /** @param offset The position of the {@link BlockState}, <i>relative</i> to this {@link BlockEntity#getPos()}. */
    public final BlockState getOffsetState(Vec3i offset) {
        return getLocalState(pos.offset(offset));
    }

    /** @param pos The <i>absolute</i> position of the {@link BlockState} . */
    public final BlockState getLocalState(BlockPos pos) {
        if (DEBUG && !world.isLoaded(pos)) {
            BCLog.logger.warn(
                "[lib.tile] Ghost-loading block at " + StringUtilBC.blockPosToString(pos) + " (from " + StringUtilBC
                    .blockPosToString(getPos()) + ")"
            );
        }
        return BlockUtil.getBlockState(world, pos, true);
    }

    public final BlockEntity getNeighbourTile(Direction offset) {
        TileCacheRet cached = tileCache.getTile(offset);
        if (cached != null) {
            return cached.tile;
        }
        if (DEBUG && !world.isLoaded(pos)) {
            BCLog.logger.warn(
                "[lib.tile] Ghost-loading tile at " + StringUtilBC.blockPosToString(pos) + " (from " + StringUtilBC
                    .blockPosToString(getPos()) + ")"
            );
        }
        return BlockUtil.getBlockEntity(getWorld(), getPos().relative(offset), true);
    }

    /** @param offset The position of the {@link BlockEntity} to retrieve, <i>relative</i> to this
     *            {@link BlockEntity#getPos()} . */
    public final BlockEntity getOffsetTile(Vec3i offset) {
        return getLocalTile(pos.offset(offset));
    }

    /** @param pos The <i>absolute</i> position of the {@link BlockEntity} . */
    public final BlockEntity getLocalTile(BlockPos pos) {
        TileCacheRet cached = tileCache.getTile(pos);
        if (cached != null) {
            return cached.tile;
        }
        if (DEBUG && !world.isLoaded(pos)) {
            BCLog.logger.warn(
                "[lib.tile] Ghost-loading tile at " + StringUtilBC.blockPosToString(pos) + " (from " + StringUtilBC
                    .blockPosToString(getPos()) + ")"
            );
        }
        return BlockUtil.getBlockEntity(world, pos, true);
    }

    public final LevelChunk getContainingChunk() {
        return chunkCache.getChunk(getPos());
    }

    public final LevelChunk getChunk(BlockPos pos) {
        LevelChunk chunk = chunkCache.getChunk(pos);
        if (chunk == null) {
            return ChunkUtil.getChunk(getWorld(), pos, true);
        }
        return chunk;
    }

    // ##################
    //
    // Misc overridables
    //
    // ##################

    /** @return The {@link IdAllocator} that allocates all ID's for this class, and its parent classes. All subclasses
     *         should override this if they allocate their own ids after calling
     *         {@link IdAllocator#makeChild(String)} */
    public IdAllocator getIdAllocator() {
        return IDS;
    }

    /** Checks to see if this tile can update. The base implementation only checks to see if it has a world. */
    public boolean cannotUpdate() {
        return !hasWorld();
    }

    /** Called whenever the block holding this tile is exploded. Called by
     * {@link Block#onBlockExploded(Level, BlockPos, Explosion)} */
    public void onExplode(Explosion explosion) {

    }

    /** Called whenever the block is removed. Called by {@link #onExplode(Explosion)}, and
     * {@link Block#breakBlock(Level, BlockPos, BlockState)} */
    public void onRemove() {
        NonNullList<ItemStack> toDrop = NonNullList.create();
        addDrops(toDrop, 0);
        InventoryUtil.dropAll(world, pos, toDrop);
    }

    /** Called by 26.3 when the block holding this tile is removed (1.12.2's breakBlock): calls {@link #onRemove()}. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        onRemove();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        chunkCache.invalidate();
        tileCache.invalidate();
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        chunkCache.invalidate();
        tileCache.invalidate();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        chunkCache.invalidate();
        tileCache.invalidate();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        chunkCache.invalidate();
        tileCache.invalidate();
    }

    /** Called whenever {@link #onRemove()} is called (by default). */
    public void addDrops(NonNullList<ItemStack> toDrop, int fortune) {
        itemManager.addDrops(toDrop);
        tankManager.addDrops(toDrop);
    }

    public void onPlacedBy(LivingEntity placer, ItemStack stack) {
        if (!placer.level().isClientSide()) {
            if (placer instanceof Player) {
                Player player = (Player) placer;
                owner = player.getGameProfile();
                if (owner.id() == null) {
                    // Basically everything relies on the UUID
                    throw new IllegalArgumentException("No UUID for owner! ( " + placer.getClass() + " " + placer + " -> " + owner + " )");
                }
            } else {
                throw new IllegalArgumentException("Not an Player! (placer = " + placer + ")");
            }
        }
    }

    public void onPlayerOpen(Player player) {
        if (owner == null || owner == FakePlayerProvider.NULL_PROFILE) {
            owner = player.getGameProfile();
            if (owner.id() == null) {
                // Basically everything relies on the UUID
                throw new IllegalArgumentException("No UUID for owner! ( " + player.getClass() + " " + player + " -> " + owner + " )");
            }
        }
        sendNetworkUpdate(NET_GUI_DATA, player);
        usingPlayers.add(player);
    }

    public void onPlayerClose(Player player) {
        usingPlayers.remove(player);
    }

    public boolean onActivated(Player player, InteractionHand hand, Direction facing, float hitX, float hitY,
        float hitZ) {
        return tankManager.onActivated(player, getPos(), hand);
    }

    public void onNeighbourBlockChanged(Block block, BlockPos nehighbour) {
        tileCache.invalidate();
    }

    @Override
    public final boolean hasCapability(@Nonnull BlockCapability<?, Direction> capability, @Nullable Direction facing) {
        return getCapability(capability, facing) != null;
    }

    /** Answers both BuildCraft's own lookups and (through {@link TileTypes}) NeoForge's capability system. Tiles add
     * their capabilities to {@link #caps}; if what a tile offers changes, it must call {@link #invalidateCapabilities()}. */
    @Override
    @Nullable
    public <T> T getCapability(@Nonnull BlockCapability<T, Direction> capability, @Nullable Direction facing) {
        return caps.getCapability(capability, facing);
    }

    // Item caps
    protected void onSlotChange(IItemHandlerModifiable handler, int slot, @Nonnull ItemStack before,
        @Nonnull ItemStack after) {
        if (world.isLoaded(pos)) {
            if (getCurrentState().hasAnalogOutputSignal()) {
                setChanged();
            } else {
                markChunkDirty();
            }
        }
    }

    /** Cheaper version of {@link #markDirty()} that doesn't update nearby comparators, so all it will do is ensure that
     * the current chunk is saved after the last tick. */
    public void markChunkDirty() {
        if (world != null) {
            world.blockEntityChanged(this.pos);
        }
    }

    // ##################
    //
    // Permission related
    //
    // ##################

    protected boolean hasOwner() {
        return owner != null;
    }

    @Override
    public GameProfile getOwner() {
        if (owner == null) {
            String msg = "[lib.tile] Unknown owner for " + getClass() + " at ";
            BCLog.logger.warn(msg + StringUtilBC.blockPosToString(getPos()));
            owner = FakePlayerProvider.NULL_PROFILE;
        }
        return owner;
    }

    public PermissionUtil.PermissionBlock getPermBlock() {
        return new PermissionBlock(this, pos);
    }

    public boolean canEditOther(BlockPos other) {
        return PermissionUtil.hasPermission(
            PermissionUtil.PERM_EDIT, getPermBlock(), PermissionUtil.createFrom(world, other)
        );
    }

    public boolean canPlayerEdit(Player player) {
        return PermissionUtil.hasPermission(PermissionUtil.PERM_EDIT, player, getPermBlock());
    }

    public boolean canInteractWith(Player player) {
        if (world.getBlockEntity(pos) != this) {
            return false;
        }
        if (player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > 64.0D) {
            return false;
        }
        // edit rather than view because you can normally change the contents from gui interaction
        return canPlayerEdit(player);
    }

    // ##################
    //
    // Network helpers
    //
    // ##################

    /** Tells MC to redraw this block. Note that this sends the NET_REDRAW message. */
    public final void redrawBlock() {
        if (hasWorld()) {
            if (world.isClientSide()) {
                BlockState state = world.getBlockState(pos);
                requestModelDataUpdate();
                world.sendBlockUpdated(pos, state, state, Block.UPDATE_IMMEDIATE);

                if (DEBUG) {
                    double x = pos.getX() + 0.5;
                    double y = pos.getY() + 0.5;
                    double z = pos.getZ() + 0.5;
                    world.addParticle(ParticleTypes.HEART, x, y, z, 0, 0, 0);
                }
            } else {
                sendNetworkUpdate(NET_REDRAW);
            }
        }
    }

    /** Sends a network update update of the specified ID. */
    public final void sendNetworkUpdate(int id) {
        if (hasWorld()) {
            MessageUpdateTile message = createNetworkUpdate(id);
            if (world.isClientSide()) {
                MessageManager.sendToServer(message);
            } else {
                MessageUtil.sendToAllWatching(world, pos, message);
            }
        }
    }

    public final void sendNetworkGuiTick(Player player) {
        if (hasWorld() && !world.isClientSide()) {
            MessageUpdateTile message = createNetworkUpdate(NET_GUI_TICK);
            if (message.getPayloadSize() <= Short.BYTES) {
                return;
            }
            MessageManager.sendTo(message, (ServerPlayer) player);
        }
    }

    public final void sendNetworkGuiUpdate(int id) {
        if (hasWorld()) {
            for (Player player : usingPlayers) {
                sendNetworkUpdate(id, player);
            }
        }
    }

    public final void sendNetworkUpdate(int id, Player target) {
        if (hasWorld() && target instanceof ServerPlayer) {
            MessageUpdateTile message = createNetworkUpdate(id);
            MessageManager.sendTo(message, (ServerPlayer) target);
        }
    }

    public final MessageUpdateTile createNetworkUpdate(final int id) {
        if (hasWorld()) {
            final LogicalSide side = world.isClientSide() ? LogicalSide.CLIENT : LogicalSide.SERVER;
            return createMessage(id, (buffer) -> writePayload(id, buffer, side));
        } else {
            BCLog.logger.warn("Did not have a world at " + pos + "!");
        }
        return null;
    }

    public final void createAndSendMessage(int id, IPayloadWriter writer) {
        if (hasWorld()) {
            IMessage message = createMessage(id, writer);
            if (world.isClientSide()) {
                MessageManager.sendToServer(message);
            } else {
                MessageUtil.sendToAllWatching(world, pos, message);
            }
        }
    }

    public final void createAndSendGuiMessage(int id, IPayloadWriter writer) {
        if (hasWorld()) {
            IMessage message = createMessage(id, writer);
            if (world.isClientSide()) {
                MessageManager.sendToServer(message);
            } else {
                MessageUtil.sendToPlayers(usingPlayers, message);
            }
        }
    }

    public final void createAndSendMessage(int id, ServerPlayer player, IPayloadWriter writer) {
        if (hasWorld()) {
            IMessage message = createMessage(id, writer);
            MessageManager.sendTo(message, player);
        }
    }

    public final void createAndSendGuiMessage(int id, ServerPlayer player, IPayloadWriter writer) {
        if (usingPlayers.contains(player)) {
            createAndSendMessage(id, player, writer);
        }
    }

    public final MessageUpdateTile createMessage(int id, IPayloadWriter writer) {
        PacketBufferBC buffer = new PacketBufferBC(Unpooled.buffer());
        buffer.writeShort(id);
        writer.write(buffer);
        return new MessageUpdateTile(pos, buffer);
    }

    @Override
    public void onDataPacket(Connection net, ValueInput input) {
        handleUpdateTag(input);
    }

    @Override
    @Nullable
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** The chunk/update packet data: just this tile's render payload, as bytes under "d" (not its saved NBT). */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        ByteBuf buf = Unpooled.buffer();
        buf.writeShort(NET_RENDER_DATA);
        writePayload(NET_RENDER_DATA, new PacketBufferBC(buf), world.isClientSide() ? LogicalSide.CLIENT : LogicalSide.SERVER);
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);

        CompoundTag nbt = new CompoundTag();
        nbt.putByteArray("d", bytes);
        return nbt;
    }

    @Override
    public void handleUpdateTag(ValueInput input) {
        handleUpdateTag(input.read(NBTUtilBC.WHOLE_COMPOUND).orElseGet(CompoundTag::new));
    }

    public void handleUpdateTag(CompoundTag tag) {
        // Explicitly don't read the (server) data from NBT
        if (!tag.contains("d")) {
            // A bit odd, but ok - this was probably sent by something else
            return;
        }
        byte[] bytes = tag.getByteArray("d").orElse(new byte[0]);
        if (bytes.length < 2) {
            // less than 2 bytes won't even be enough to read the ID, so we'll treat it as no data.
            BCLog.logger.warn("[lib.tile] Received an update tag that didn't have any data!\n\t(" + tag + ")");
            return;
        }
        ByteBuf buf = Unpooled.copiedBuffer(bytes);

        try {
            int id = buf.readUnsignedShort();
            PacketBufferBC buffer = new PacketBufferBC(buf);
            readPayload(id, buffer, world.isClientSide() ? LogicalSide.CLIENT : LogicalSide.SERVER, null);
            // Make sure that we actually read the entire message rather than just discarding it
            MessageUtil.ensureEmpty(buffer, world.isClientSide(), getClass() + ", id = " + getIdAllocator().getNameFor(id));
            spawnReceiveParticles(id);
        } catch (IOException e) {
            throw new RuntimeException("Received an update tag that failed to read correctly!", e);
        }
    }

    private void spawnReceiveParticles(int id) {
        if (DEBUG) {
            String name = getIdAllocator().getNameFor(id);

            if (world != null) {
                double x = pos.getX() + 0.5;
                double y = pos.getY() + 0.5;
                double z = pos.getZ() + 0.5;
                double r = 0.01 + (id & 3) / 4.0;
                double g = 0.01 + ((id / 4) & 3) / 4.0;
                double b = 0.01 + ((id / 16) & 3) / 4.0;
                int rgb = ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
                world.addParticle(new DustParticleOptions(rgb, 1), x, y, z, 0, 0, 0);
            }
        }
    }

    @Override
    public final IMessage receivePayload(IPayloadContext ctx, PacketBufferBC buffer) throws IOException {
        int id = buffer.readUnsignedShort();
        LogicalSide side = MessageUtil.getSide(ctx);
        readPayload(id, buffer, side, ctx);

        // Make sure that we actually read the entire message rather than just discarding it
        MessageUtil.ensureEmpty(buffer, world.isClientSide(), getClass() + ", id = " + getIdAllocator().getNameFor(id));

        if (side == LogicalSide.CLIENT) {
            spawnReceiveParticles(id);
        }
        return null;
    }

    // ######################
    //
    // Network overridables
    //
    // ######################

    public void writePayload(int id, PacketBufferBC buffer, LogicalSide side) {
        // write render data with gui data
        if (id == NET_GUI_DATA) {

            writePayload(NET_RENDER_DATA, buffer, side);

            if (side == LogicalSide.SERVER) {
                MessageUtil.writeGameProfile(buffer, owner);
            }
        }
        if (side == LogicalSide.SERVER) {
            if (id == NET_RENDER_DATA) {
                deltaManager.writeDeltaState(false, buffer);
            } else if (id == NET_GUI_DATA) {
                deltaManager.writeDeltaState(true, buffer);
            }
        }
    }

    /** @param ctx The context. Will be null if this is a generic update payload
     * @throws IOException if something went wrong */
    public void readPayload(int id, PacketBufferBC buffer, LogicalSide side, IPayloadContext ctx) throws IOException {
        // read render data with gui data
        if (id == NET_GUI_DATA) {
            readPayload(NET_RENDER_DATA, buffer, side, ctx);

            if (side == LogicalSide.CLIENT) {
                owner = MessageUtil.readGameProfile(buffer);
            }
        }
        if (side == LogicalSide.CLIENT) {
            if (id == NET_RENDER_DATA) deltaManager.receiveDeltaData(false, EnumDeltaMessage.CURRENT_STATE, buffer);
            else if (id == NET_GUI_DATA) deltaManager.receiveDeltaData(true, EnumDeltaMessage.CURRENT_STATE, buffer);
            else if (id == NET_REN_DELTA_SINGLE) deltaManager.receiveDeltaData(
                false, EnumDeltaMessage.ADD_SINGLE, buffer
            );
            else if (id == NET_GUI_DELTA_SINGLE) deltaManager.receiveDeltaData(
                true, EnumDeltaMessage.ADD_SINGLE, buffer
            );
            else if (id == NET_REN_DELTA_CLEAR) deltaManager.receiveDeltaData(
                false, EnumDeltaMessage.SET_VALUE, buffer
            );
            else if (id == NET_GUI_DELTA_CLEAR) deltaManager.receiveDeltaData(true, EnumDeltaMessage.SET_VALUE, buffer);
            else if (id == NET_REDRAW) redrawBlock();
            else if (id == NET_ADV_DEBUG) {
                BCAdvDebugging.setClientDebugTarget(this);
            }
        }
    }

    // ######################
    //
    // NBT handling
    //
    // ######################

    /** Loads from the NBT this tile saved: 26.3's value input is read as one compound, laid out as on 1.12.2. */
    @Override
    protected final void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        CompoundTag nbt = input.read(NBTUtilBC.WHOLE_COMPOUND).orElseGet(CompoundTag::new);
        if (world == null) {
            // 26.3 reads a chunk's tiles before giving them their level (1.12.2 set the world first), and a lot of
            // readFromNBT code uses the world (pipe flows read the game time). Read it once the level is set.
            pendingNbt = nbt;
        } else {
            pendingNbt = null;
            readFromNBT(nbt);
        }
    }

    @Override
    protected final void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        // Never read (the tile was saved before it joined a level): keep the data as it was
        output.store(NBTUtilBC.WHOLE_COMPOUND, pendingNbt != null ? pendingNbt.copy() : writeToNBT(new CompoundTag()));
    }

    public void readFromNBT(CompoundTag nbt) {
        migrateOldNBT(nbt.getIntOr("data-version", 0), nbt);
        deltaManager.readFromNBT(nbt.getCompoundOrEmpty("deltas"));
        if (nbt.contains("owner")) {
            owner = NBTUtilBC.readGameProfile(nbt.getCompoundOrEmpty("owner"));
        }
        if (nbt.contains("items")) {
            itemManager.deserializeNBT(nbt.getCompoundOrEmpty("items"));
        }
        if (nbt.contains("tanks")) {
            tankManager.deserializeNBT(nbt.getCompoundOrEmpty("tanks"));
        }
    }

    protected void migrateOldNBT(int version, CompoundTag nbt) {
        // 7.99.0 -> 7.99.4
        // Most tiles with a single tank saved it under "tank"
        CompoundTag tankComp = nbt.getCompoundOrEmpty("tank");
        if (!tankComp.isEmpty()) {
            CompoundTag tanks = new CompoundTag();
            tanks.put("tank", tankComp);
            nbt.put("tanks", tanks);
        }
    }

    public CompoundTag writeToNBT(CompoundTag nbt) {
        nbt.putInt("data-version", BCVersion.CURRENT.dataVersion);
        nbt.put("deltas", deltaManager.writeToNBT());
        if (owner != null && owner.id() != null && !owner.name().isEmpty() && owner != FakePlayerProvider.NULL_PROFILE) {
            nbt.put("owner", NBTUtilBC.writeGameProfile(owner));
        }
        CompoundTag items = itemManager.serializeNBT();
        if (!items.isEmpty()) {
            nbt.put("items", items);
        }
        CompoundTag tanks = tankManager.serializeNBT();
        if (!tanks.isEmpty()) {
            nbt.put("tanks", tanks);
        }
        return nbt;
    }

    // ##################
    //
    // Advanced debugging
    //
    // ##################

    public boolean isBeingDebugged() {
        return BCAdvDebugging.isBeingDebugged(this);
    }

    public void enableDebugging() {
        if (world.isClientSide()) {
            return;
        }
        BCAdvDebugging.setCurrentDebugTarget(this);
    }

    @Override
    public void disableDebugging() {
        sendNetworkUpdate(NET_ADV_DEBUG_DISABLE);
    }

    @Override
    public boolean doesExistInWorld() {
        return hasWorld() && world.getBlockEntity(pos) == this;
    }

    @Override
    public void sendDebugState() {
        sendNetworkUpdate(NET_ADV_DEBUG);
    }

    @Override
    public IDetachedRenderer getDebugRenderer() {
        return null;
    }
}
