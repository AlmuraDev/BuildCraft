/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.block;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.resources.Identifier;

import buildcraft.api.blocks.ICustomPaintHandler;
import buildcraft.api.core.EnumPipePart;
import buildcraft.api.transport.EnumWirePart;
import buildcraft.api.transport.IItemPluggable;
import buildcraft.api.transport.WireNode;
import buildcraft.api.transport.pipe.IItemPipe;
import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.api.transport.pipe.PipeApi;
import buildcraft.api.transport.pipe.PipeDefinition;
import buildcraft.api.transport.pluggable.PipePluggable;

import buildcraft.lib.block.BlockBCTile_Neptune;
import buildcraft.lib.misc.AdvancementUtil;
import buildcraft.lib.misc.BoundingBoxUtil;
import buildcraft.lib.misc.InventoryUtil;
import buildcraft.lib.misc.VecUtil;
import buildcraft.lib.net.PacketBufferBC;

import buildcraft.transport.BCTransportItems;
import buildcraft.transport.BCTransportProxy;
import buildcraft.transport.item.ItemPipeHolder;
import buildcraft.transport.item.ItemWire;
import buildcraft.transport.pipe.Pipe;
import buildcraft.transport.tile.TilePipeHolder;
import buildcraft.transport.wire.EnumWireBetween;

/** The block every pipe lives in. Its shape is the union of the pipe, its connections, pluggables and wires; which of
 * those a player clicked or broke is found by tracing their look against each part ({@link PipeHitResult}). */
public class BlockPipeHolder extends BlockBCTile_Neptune implements ICustomPaintHandler {
    static final AABB FULL_BLOCK_AABB = new AABB(0, 0, 0, 1, 1, 1);
    private static final AABB BOX_CENTER = new AABB(0.25, 0.25, 0.25, 0.75, 0.75, 0.75);
    private static final AABB BOX_DOWN = new AABB(0.25, 0, 0.25, 0.75, 0.25, 0.75);
    private static final AABB BOX_UP = new AABB(0.25, 0.75, 0.25, 0.75, 1, 0.75);
    private static final AABB BOX_NORTH = new AABB(0.25, 0.25, 0, 0.75, 0.75, 0.25);
    private static final AABB BOX_SOUTH = new AABB(0.25, 0.25, 0.75, 0.75, 0.75, 1);
    private static final AABB BOX_WEST = new AABB(0, 0.25, 0.25, 0.25, 0.75, 0.75);
    private static final AABB BOX_EAST = new AABB(0.75, 0.25, 0.25, 1, 0.75, 0.75);
    private static final AABB[] BOX_FACES = { BOX_DOWN, BOX_UP, BOX_NORTH, BOX_SOUTH, BOX_WEST, BOX_EAST };

    private static final Identifier ADVANCEMENT_LOGIC_TRANSPORTATION
        = Identifier.parse("buildcrafttransport:logic_transportation");

    public BlockPipeHolder(BlockBehaviour.Properties properties, String id) {
        super(properties.destroyTime(0.25f).explosionResistance(3.0f).noOcclusion().dynamicShape(), id);
    }

    // Shapes

    /** @return The box of the pipe's connection on the given face, sized to how far the connection reaches. */
    private static AABB getConnectionBox(Pipe pipe, Direction face) {
        float conSize = pipe.getConnectedDist(face);
        AABB aabb = BOX_FACES[face.ordinal()];
        if (conSize > 0 && conSize != 0.25f) {
            Vec3 center = VecUtil.offset(new Vec3(0.5, 0.5, 0.5), face, 0.25 + (conSize / 2));
            Vec3 radius = new Vec3(0.25, 0.25, 0.25);
            radius = VecUtil.replaceValue(radius, face.getAxis(), conSize / 2);
            aabb = BoundingBoxUtil.makeFrom(center.subtract(radius), center.add(radius));
        }
        return aabb;
    }

    /** @return Every part box of the holder, or an empty list if it has none. */
    private static List<AABB> getPartBoxes(TilePipeHolder tile) {
        List<AABB> boxes = new ArrayList<>();
        Pipe pipe = tile.getPipe();
        if (pipe != null) {
            boxes.add(BOX_CENTER);
            for (Direction face : Direction.values()) {
                if (pipe.getConnectedDist(face) > 0) {
                    boxes.add(getConnectionBox(pipe, face));
                }
            }
        }
        for (Direction face : Direction.values()) {
            PipePluggable pluggable = tile.getPluggable(face);
            if (pluggable != null && pluggable.getBoundingBox() != null) {
                boxes.add(pluggable.getBoundingBox());
            }
        }
        for (EnumWirePart part : tile.getWireManager().parts.keySet()) {
            boxes.add(part.boundingBox);
        }
        for (EnumWireBetween between : tile.getWireManager().betweens.keySet()) {
            boxes.add(between.boundingBox);
        }
        return boxes;
    }

    private static VoxelShape toShape(List<AABB> boxes) {
        VoxelShape shape = Shapes.empty();
        for (AABB box : boxes) {
            shape = Shapes.or(shape, Shapes.create(box));
        }
        return shape;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        TilePipeHolder tile = getPipe(world, pos, false);
        if (tile == null) {
            return Shapes.block();
        }
        List<AABB> boxes = getPartBoxes(tile);
        return boxes.isEmpty() ? Shapes.block() : toShape(boxes);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return getShape(state, world, pos, context);
    }

    /** Solid faces: the faces whose pluggable says so (facades), as 1.12.2's isSideSolid did. */
    @Override
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter world, BlockPos pos) {
        TilePipeHolder tile = getPipe(world, pos, false);
        if (tile == null) {
            return Shapes.empty();
        }
        VoxelShape shape = Shapes.empty();
        for (Direction face : Direction.values()) {
            PipePluggable pluggable = tile.getPluggable(face);
            if (pluggable != null && pluggable.isSideSolid()) {
                shape = Shapes.or(shape, Shapes.create(BOX_FACES[face.ordinal()].inflate(0.25)
                    .intersect(FULL_BLOCK_AABB).move(-0, -0, -0)));
            }
        }
        return shape;
    }

    // Ray tracing

    @Nullable
    public PipeHitResult rayTrace(Level world, BlockPos pos, Player player) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().normalize().scale(player.blockInteractionRange()));
        return rayTrace(world, pos, start, end);
    }

    @Nullable
    public PipeHitResult rayTrace(BlockGetter world, BlockPos pos, Vec3 start, Vec3 end) {
        TilePipeHolder tile = getPipe(world, pos, false);
        if (tile == null) {
            return computeTrace(null, pos, start, end, FULL_BLOCK_AABB, 400);
        }
        PipeHitResult best = null;
        Pipe pipe = tile.getPipe();
        boolean computed = false;
        if (pipe != null) {
            computed = true;
            best = computeTrace(best, pos, start, end, BOX_CENTER, 0);
            for (Direction face : Direction.values()) {
                if (pipe.getConnectedDist(face) > 0) {
                    best = computeTrace(best, pos, start, end, getConnectionBox(pipe, face), face.ordinal() + 1);
                }
            }
        }
        for (Direction face : Direction.values()) {
            PipePluggable pluggable = tile.getPluggable(face);
            if (pluggable != null && pluggable.getBoundingBox() != null) {
                best = computeTrace(best, pos, start, end, pluggable.getBoundingBox(), face.ordinal() + 1 + 6);
                computed = true;
            }
        }
        for (EnumWirePart part : tile.getWireManager().parts.keySet()) {
            best = computeTrace(best, pos, start, end, part.boundingBox, part.ordinal() + 1 + 6 + 6);
            computed = true;
        }
        for (EnumWireBetween between : tile.getWireManager().betweens.keySet()) {
            best = computeTrace(best, pos, start, end, between.boundingBox, between.ordinal() + 1 + 6 + 6 + 8);
            computed = true;
        }
        if (!computed) {
            return computeTrace(null, pos, start, end, FULL_BLOCK_AABB, 400);
        }
        return best;
    }

    @Nullable
    public static EnumWirePart rayTraceWire(BlockPos pos, Vec3 start, Vec3 end) {
        Vec3 realStart = start.subtract(pos.getX(), pos.getY(), pos.getZ());
        Vec3 realEnd = end.subtract(pos.getX(), pos.getY(), pos.getZ());
        EnumWirePart best = null;
        double dist = 1000;
        for (EnumWirePart part : EnumWirePart.VALUES) {
            java.util.Optional<Vec3> trace = part.boundingBoxPossible.clip(realStart, realEnd);
            if (trace.isPresent()) {
                double nextDist = trace.get().distanceToSqr(realStart);
                if (best == null || dist > nextDist) {
                    best = part;
                    dist = nextDist;
                }
            }
        }
        return best;
    }

    @Nullable
    private static PipeHitResult computeTrace(@Nullable PipeHitResult lastBest, BlockPos pos, Vec3 start, Vec3 end,
        AABB aabb, int part) {
        BlockHitResult hit = AABB.clip(List.of(aabb), start, end, pos);
        if (hit == null) {
            return lastBest;
        }
        PipeHitResult next = new PipeHitResult(hit.getLocation(), hit.getDirection(), pos, part);
        if (lastBest == null) {
            return next;
        }
        double distLast = lastBest.getLocation().distanceToSqr(start);
        double distNext = next.getLocation().distanceToSqr(start);
        return distLast > distNext ? next : lastBest;
    }

    @Nullable
    public static Direction getPartSideHit(PipeHitResult trace) {
        if (trace.subHit <= 0) {
            return trace.getDirection();
        }
        if (trace.subHit <= 6) {
            return Direction.values()[trace.subHit - 1];
        }
        if (trace.subHit <= 6 + 6) {
            return Direction.values()[trace.subHit - 1 - 6];
        }
        return null;
    }

    @Nullable
    public static EnumWirePart getWirePartHit(PipeHitResult trace) {
        if (trace.subHit <= 6 + 6) {
            return null;
        } else if (trace.subHit <= 6 + 6 + 8) {
            return EnumWirePart.VALUES[trace.subHit - 1 - 6 - 6];
        } else {
            return null;
        }
    }

    @Nullable
    public static EnumWireBetween getWireBetweenHit(PipeHitResult trace) {
        if (trace.subHit <= 6 + 6 + 8) {
            return null;
        } else if (trace.subHit <= 6 + 6 + 8 + EnumWireBetween.VALUES.length) {
            return EnumWireBetween.VALUES[trace.subHit - 1 - 6 - 6 - 8];
        } else {
            return null;
        }
    }

    /** @return The box of the part with the given index (as in {@link PipeHitResult#subHit}), in block coordinates. */
    @Nullable
    public static AABB getPartBox(TilePipeHolder tile, int part) {
        if (part == 0) {
            return BOX_CENTER;
        } else if (part < 1 + 6) {
            Pipe pipe = tile.getPipe();
            Direction face = Direction.values()[part - 1];
            return pipe == null ? BOX_FACES[part - 1] : getConnectionBox(pipe, face);
        } else if (part < 1 + 6 + 6) {
            PipePluggable pluggable = tile.getPluggable(Direction.values()[part - 1 - 6]);
            return pluggable == null ? null : pluggable.getBoundingBox();
        } else if (part < 1 + 6 + 6 + 8) {
            return EnumWirePart.VALUES[part - 1 - 6 - 6].boundingBox;
        } else if (part < 1 + 6 + 6 + 8 + EnumWireBetween.VALUES.length) {
            return EnumWireBetween.VALUES[part - 1 - 6 - 6 - 8].boundingBox;
        }
        return null;
    }

    // Picking

    @Override
    public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state, boolean includeData,
        Player player) {
        TilePipeHolder tile = getPipe(world, pos, false);
        PipeHitResult target = player instanceof Player && world instanceof Level level ? rayTrace(level, pos, player) : null;
        if (tile == null || target == null) {
            return ItemStack.EMPTY;
        }
        if (target.subHit <= 6) {
            Pipe pipe = tile.getPipe();
            if (pipe != null) {
                PipeDefinition def = pipe.getDefinition();
                IItemPipe item = PipeApi.pipeRegistry.getItemForPipe(def);
                if (item instanceof ItemPipeHolder pipeItem) {
                    return pipeItem.createStack(pipe.getColour());
                }
            }
        } else if (target.subHit <= 12) {
            PipePluggable plug = tile.getPluggable(Direction.values()[target.subHit - 7]);
            if (plug != null) {
                return plug.getPickStack();
            }
        } else {
            EnumWirePart part = getWirePartHit(target);
            EnumWireBetween between = getWireBetweenHit(target);
            if (part != null && tile.wireManager.getColorOfPart(part) != null) {
                return wireStack(tile.wireManager.getColorOfPart(part), 1);
            } else if (between != null && tile.wireManager.getColorOfPart(between.parts[0]) != null) {
                return wireStack(tile.wireManager.getColorOfPart(between.parts[0]), 1);
            }
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack wireStack(DyeColor colour, int count) {
        return BCTransportItems.wire == null ? ItemStack.EMPTY : BCTransportItems.wire.createStack(colour, count);
    }

    // Interaction

    @Override
    protected InteractionResult useItemOn(ItemStack held, BlockState state, Level world, BlockPos pos, Player player,
        InteractionHand hand, BlockHitResult hit) {
        InteractionResult result = activate(world, pos, player, hand, held, hit);
        return result == InteractionResult.PASS ? InteractionResult.TRY_WITH_EMPTY_HAND : result;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player,
        BlockHitResult hit) {
        return activate(world, pos, player, InteractionHand.MAIN_HAND, ItemStack.EMPTY, hit);
    }

    private InteractionResult activate(Level world, BlockPos pos, Player player, InteractionHand hand, ItemStack held,
        BlockHitResult hit) {
        TilePipeHolder tile = getPipe(world, pos, false);
        if (tile == null) {
            return InteractionResult.PASS;
        }
        PipeHitResult trace = rayTrace(world, pos, player);
        if (trace == null) {
            return InteractionResult.PASS;
        }
        Vec3 hitVec = trace.getLocation();
        float hitX = (float) (hitVec.x - pos.getX());
        float hitY = (float) (hitVec.y - pos.getY());
        float hitZ = (float) (hitVec.z - pos.getZ());
        Direction realSide = getPartSideHit(trace);
        if (realSide == null) {
            realSide = hit.getDirection();
        }
        if (trace.subHit > 6 && trace.subHit <= 12) {
            PipePluggable existing = tile.getPluggable(realSide);
            if (existing != null) {
                return existing.onPluggableActivate(player, trace, hitX, hitY, hitZ) ? InteractionResult.SUCCESS
                    : InteractionResult.PASS;
            }
        }

        EnumPipePart part = trace.subHit == 0 ? EnumPipePart.CENTER : EnumPipePart.fromFacing(realSide);

        Item item = held.isEmpty() ? null : held.getItem();
        PipePluggable existing = tile.getPluggable(realSide);
        if (item instanceof IItemPluggable && existing == null) {
            IItemPluggable itemPlug = (IItemPluggable) item;
            PipePluggable plug = itemPlug.onPlace(held, tile, realSide, player, hand);
            if (plug == null) {
                return InteractionResult.PASS;
            } else {
                tile.replacePluggable(realSide, plug);
                plug.onPlacedBy(player);
                if (!player.isCreative()) {
                    held.shrink(1);
                }
                return InteractionResult.SUCCESS;
            }
        }
        if (item instanceof ItemWire) {
            EnumWirePart wirePartHit = getWirePartHit(trace);
            EnumWirePart wirePart;
            TilePipeHolder attachTile = tile;
            if (wirePartHit != null) {
                WireNode node = new WireNode(pos, wirePartHit);
                node = node.offset(trace.getDirection());
                wirePart = node.part;
                if (!node.pos.equals(pos)) {
                    attachTile = getPipe(world, node.pos, false);
                }
            } else {
                wirePart = EnumWirePart.get(
                    (hitVec.x % 1 + 1) % 1 > 0.5, (hitVec.y % 1 + 1) % 1 > 0.5, (hitVec.z % 1 + 1) % 1 > 0.5
                );
            }
            if (wirePart != null && attachTile != null) {
                DyeColor colour = ItemWire.getColour(held);
                boolean attached = attachTile.getWireManager().addPart(wirePart, colour);
                attachTile.scheduleNetworkUpdate(IPipeHolder.PipeMessageReceiver.WIRES);
                if (attached) {
                    WireNode from = new WireNode(attachTile.getPipePos(), wirePart);

                    boolean isNowConnected = false;
                    for (Direction dir : Direction.values()) {
                        WireNode to = from.offset(dir);
                        if (to.pos.equals(attachTile.getPipePos())) {
                            if (attachTile.getWireManager().getColorOfPart(to.part) == colour) {
                                isNowConnected = true;
                                break;
                            }
                        } else {
                            BlockEntity localTile = attachTile.getLocalTile(to.pos);
                            if (localTile instanceof TilePipeHolder) {
                                if (((TilePipeHolder) localTile).getWireManager().getColorOfPart(to.part) == colour) {
                                    isNowConnected = true;
                                    break;
                                }
                            }
                        }
                    }
                    if (isNowConnected) {
                        AdvancementUtil.unlockAdvancement(player, ADVANCEMENT_LOGIC_TRANSPORTATION);
                    }

                    if (!player.isCreative()) {
                        held.shrink(1);
                    }
                    return InteractionResult.SUCCESS;
                }
            }
        }
        Pipe pipe = tile.getPipe();
        if (pipe == null) {
            return InteractionResult.PASS;
        }
        if (pipe.behaviour.onPipeActivate(player, trace, hitX, hitY, hitZ, part)) {
            return InteractionResult.SUCCESS;
        }
        if (pipe.flow.onFlowActivate(player, trace, hitX, hitY, hitZ, part)) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    // Breaking

    /** Breaking a pluggable or wire only removes that part; breaking the pipe drops the pipe and everything on it.
     * Drops are given here, so {@link #playerDestroy} doesn't drop the block's loot again. */
    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level world, BlockPos pos, Player player, ItemStack toolStack,
        boolean willHarvest, FluidState fluid) {
        if (world.isClientSide()) {
            return false;
        }

        TilePipeHolder tile = getPipe(world, pos, false);
        if (tile == null) {
            return super.onDestroyedByPlayer(state, world, pos, player, toolStack, willHarvest, fluid);
        }

        NonNullList<ItemStack> toDrop = NonNullList.create();
        PipeHitResult trace = rayTrace(world, pos, player);
        Direction side = null;
        EnumWirePart part = null;
        EnumWireBetween between = null;

        if (trace != null && trace.subHit > 6) {
            side = getPartSideHit(trace);
            part = getWirePartHit(trace);
            between = getWireBetweenHit(trace);
        }

        if (side != null) {
            removePluggable(side, tile, toDrop);
            if (!player.isCreative()) {
                InventoryUtil.dropAll(world, pos, toDrop);
            }
            return false;
        } else if (part != null) {
            toDrop.add(wireStack(tile.wireManager.getColorOfPart(part), 1));
            tile.wireManager.removePart(part);
            if (!player.isCreative()) {
                InventoryUtil.dropAll(world, pos, toDrop);
            }
            tile.scheduleNetworkUpdate(IPipeHolder.PipeMessageReceiver.WIRES);
            return false;
        } else if (between != null) {
            toDrop.add(wireStack(tile.wireManager.getColorOfPart(between.parts[0]), between.to == null ? 2 : 1));
            if (between.to == null) {
                tile.wireManager.removeParts(Arrays.asList(between.parts));
            } else {
                tile.wireManager.removePart(between.parts[0]);
            }
            if (!player.isCreative()) {
                InventoryUtil.dropAll(world, pos, toDrop);
            }
            tile.scheduleNetworkUpdate(IPipeHolder.PipeMessageReceiver.WIRES);
            return false;
        } else {
            addDrops(tile, toDrop);
            for (Direction face : Direction.values()) {
                removePluggable(face, tile, NonNullList.create());
            }
        }
        if (!player.isCreative()) {
            InventoryUtil.dropAll(world, pos, toDrop);
        }
        return super.onDestroyedByPlayer(state, world, pos, player, toolStack, willHarvest, fluid);
    }

    @Override
    public void playerDestroy(ServerLevel world, ServerPlayer player, BlockPos pos, BlockState state,
        @Nullable BlockEntity te, ItemStack stack) {
        // The drops were given in onDestroyedByPlayer
        player.awardStat(net.minecraft.stats.Stats.BLOCK_MINED.get(this));
        player.causeFoodExhaustion(0.005F);
    }

    /** Drops when broken any other way (explosions, other mods): the pipe, pluggables and wires. */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity te = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        NonNullList<ItemStack> toDrop = NonNullList.create();
        if (te instanceof TilePipeHolder tile) {
            addDrops(tile, toDrop);
        }
        return toDrop;
    }

    private static void addDrops(TilePipeHolder tile, NonNullList<ItemStack> toDrop) {
        for (Direction face : Direction.values()) {
            PipePluggable pluggable = tile.getPluggable(face);
            if (pluggable != null) {
                pluggable.addDrops(toDrop, 0);
            }
        }
        for (DyeColor color : tile.wireManager.parts.values()) {
            toDrop.add(wireStack(color, 1));
        }
        Pipe pipe = tile.getPipe();
        if (pipe != null) {
            pipe.addDrops(toDrop, 0);
        }
    }

    @Override
    public float getExplosionResistance(BlockState state, BlockGetter world, BlockPos pos, Explosion explosion) {
        Vec3 subtract = explosion.center().subtract(Vec3.atCenterOf(pos)).normalize();
        Direction side = Arrays.stream(Direction.values())
            .min(Comparator.comparing(facing -> new Vec3(facing.getUnitVec3i()).distanceTo(subtract)))
            .orElseThrow(IllegalArgumentException::new);
        TilePipeHolder tile = getPipe(world, pos, true);
        if (tile != null) {
            PipePluggable pluggable = tile.getPluggable(side);
            if (pluggable != null) {
                float explosionResistance = pluggable.getExplosionResistance(explosion.getDirectSourceEntity(), explosion);
                if (explosionResistance > 0) {
                    return explosionResistance;
                }
            }
        }
        return super.getExplosionResistance(state, world, pos, explosion);
    }

    @Override
    protected void entityInside(BlockState state, Level world, BlockPos pos, Entity entity,
        InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        TilePipeHolder tile = getPipe(world, pos, false);
        if (tile == null) {
            return;
        }
        Pipe pipe = tile.getPipe();
        if (pipe != null) {
            pipe.getBehaviour().onEntityCollide(entity);
        }
    }

    private static void removePluggable(Direction side, TilePipeHolder tile, NonNullList<ItemStack> toDrop) {
        PipePluggable removed = tile.replacePluggable(side, null);
        if (removed != null) {
            removed.onRemove();
            removed.addDrops(toDrop, 0);
        }
    }

    public static TilePipeHolder getPipe(BlockGetter access, BlockPos pos, boolean requireServer) {
        if (access instanceof Level) {
            return getPipe((Level) access, pos, requireServer);
        }
        if (requireServer) {
            return null;
        }
        BlockEntity tile = access.getBlockEntity(pos);
        if (tile instanceof TilePipeHolder) {
            return (TilePipeHolder) tile;
        }
        return null;
    }

    public static TilePipeHolder getPipe(Level world, BlockPos pos, boolean requireServer) {
        if (requireServer && world.isClientSide()) {
            return null;
        }
        BlockEntity tile = world.getBlockEntity(pos);
        if (tile instanceof TilePipeHolder) {
            return (TilePipeHolder) tile;
        }
        return null;
    }

    // Particles

    @Override
    public boolean addLandingEffects(BlockState state, ServerLevel world, BlockPos pos, BlockState iblockstate,
        LivingEntity entity, int numberOfParticles) {
        BlockEntity te = world.getBlockEntity(pos);
        if (te instanceof TilePipeHolder pipeHolder) {
            pipeHolder.createAndSendMessage(TilePipeHolder.NET_CREATE_LANDING_PARTICLE, (PacketBufferBC buffer) -> {
                buffer.writeDouble(entity.getX());
                buffer.writeDouble(entity.getY());
                buffer.writeDouble(entity.getZ());
                buffer.writeInt(numberOfParticles);
            });
            return true;
        }
        return super.addLandingEffects(state, world, pos, iblockstate, entity, numberOfParticles);
    }

    @Override
    public boolean addRunningEffects(BlockState state, Level world, BlockPos pos, Entity entity) {
        if (world.isClientSide() && world.getBlockEntity(pos) instanceof TilePipeHolder pipeHolder) {
            return BCTransportProxy.getProxy().addRunningEffects(pipeHolder, entity);
        }
        return super.addRunningEffects(state, world, pos, entity);
    }

    // TODO(26.3 render batch): hit and destroy particles with the hit part's sprite (IClientBlockExtensions).

    // paint

    @Override
    public InteractionResult attemptPaint(Level world, BlockPos pos, BlockState state, Vec3 hitPos, Direction hitSide,
        DyeColor paintColour) {
        TilePipeHolder tile = getPipe(world, pos, true);
        if (tile == null) {
            return InteractionResult.PASS;
        }

        Pipe pipe = tile.getPipe();
        if (pipe == null) {
            return InteractionResult.FAIL;
        }
        if (pipe.getColour() == paintColour || !pipe.definition.canBeColoured) {
            return InteractionResult.FAIL;
        } else {
            pipe.setColour(paintColour);
            return InteractionResult.SUCCESS;
        }
    }

    // Redstone

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getDirectSignal(BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction side) {
        if (side == null) {
            return 0;
        }
        TilePipeHolder tile = getPipe(blockAccess, pos, false);
        if (tile != null) {
            return tile.getRedstoneOutput(side.getOpposite());
        }
        return 0;
    }

    @Override
    protected int getSignal(BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction side) {
        return getDirectSignal(blockState, blockAccess, pos, side);
    }
}
