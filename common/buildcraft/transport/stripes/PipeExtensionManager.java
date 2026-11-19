/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.stripes;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import buildcraft.lib.tile.TileBC_Neptune;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mojang.authlib.GameProfile;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.NonNullList;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.LogicalSide;

import buildcraft.api.core.BCLog;
import buildcraft.api.core.BuildCraftAPI;
import buildcraft.api.transport.IStripesActivator;
import buildcraft.api.transport.IWireManager;
import buildcraft.api.transport.pipe.IItemPipe;
import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.IPipeExtensionManager;
import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.api.transport.pipe.PipeApi;
import buildcraft.api.transport.pipe.PipeBehaviour;
import buildcraft.api.transport.pipe.PipeDefinition;

import buildcraft.lib.misc.BlockUtil;
import buildcraft.lib.misc.CapUtil;
import buildcraft.lib.misc.InventoryUtil;
import buildcraft.lib.misc.SoundUtil;

import buildcraft.transport.pipe.behaviour.PipeBehaviourStripes;
import buildcraft.transport.wire.WireManager;


public enum PipeExtensionManager implements IPipeExtensionManager {
    INSTANCE;

    private final Map<ResourceKey<Level>, List<PipeExtensionRequest>> requests = new HashMap<>();
    private final Set<PipeDefinition> retractionPipeDefs = new HashSet<>();

    @Override
    public boolean requestPipeExtension(Level world, BlockPos pos, Direction dir, IStripesActivator stripes, ItemStack stack) {
        if (world.isClientSide() || stack.isEmpty() || !(stack.getItem() instanceof IItemPipe)) {
            return false;
        }

        List<PipeExtensionRequest> rList = requests.computeIfAbsent(world.dimension(), id -> new ArrayList<>());
        return rList.add(new PipeExtensionRequest(pos, dir, stripes, ((IItemPipe) stack.getItem()).getDefinition(), stack.copy()));
    }

    @Override
    public void registerRetractionPipe(PipeDefinition pipeDefinition) {
        if (pipeDefinition != null) {
            retractionPipeDefs.add(pipeDefinition);
        }
    }

    @SubscribeEvent
    public void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel world)) {
            return;
        }
        List<PipeExtensionRequest> rList = requests.get(world.dimension());
        if (rList == null) {
            return;
        }
        for (PipeExtensionRequest r : rList) {
            if (retractionPipeDefs.contains(r.pipeDef)) {
                retract(world, r);
            } else {
                extend(world, r);
            }
        }
        rList.clear();
    }

    private void retract(Level w, PipeExtensionRequest r) {
        Direction retractDir = r.dir.getOpposite();
        if (!isValidRetractionPath(w, r, retractDir)) {

            // check other directions
            List<Direction> possible = new ArrayList<>();
            for (Direction facing : Direction.values()) {
                if (facing.getAxis() != r.dir.getAxis()) {
                    if (isValidRetractionPath(w, r, facing)) {
                        possible.add(facing);
                    }
                }
            }

            if (possible.isEmpty()) {
                r.stripes.sendItem(r.stack.copy(), r.dir);
                return;
            }
            retractDir = possible.get(Mth.nextInt(w.getRandom(), 0, possible.size() - 1));
        }
        BlockPos p = r.pos.relative(retractDir);

        NonNullList<ItemStack> stacksToSendBack = NonNullList.create();
        // Always send back catalyst pipe
        stacksToSendBack.add(r.stack);

        // Step 1: Copy over existing stripes pipe
        BlockSnapshot blockSnapshot1 = BlockSnapshot.create(w.dimension(), w, r.pos);
        BlockState stripesStateOld = w.getBlockState(r.pos);
        BlockEntity stripesTileOld = w.getBlockEntity(r.pos);
        final GameProfile owner;
        // Fetch owner
        {
            IPipeHolder holder = CapUtil.getCapability(stripesTileOld, PipeApi.CAP_PIPE_HOLDER, null);
            if (stripesTileOld == null || holder == null) {
                BCLog.logger
                    .warn("Found an invalid request at " + r.pos + " as " + stripesTileOld + " was not a pipe tile!");
                return;
            }
            owner = holder.getOwner();
            PipeBehaviour behaviour = holder.getPipe().getBehaviour();
            if (behaviour instanceof PipeBehaviourStripes) {
                ((PipeBehaviourStripes) behaviour).direction = retractDir.getOpposite();
            }
        }

        CompoundTag stripesNBTOld = ((TileBC_Neptune) stripesTileOld).writeToNBT(new CompoundTag());

        // Step 2: Remove previous pipe
        BlockSnapshot blockSnapshot2 = BlockSnapshot.create(w.dimension(), w, p);
        NonNullList<ItemStack> list = NonNullList.create();
        boolean canceled = !BlockUtil.breakBlock((ServerLevel) w, p, list, r.pos, owner);
        if (canceled) {
            blockSnapshot2.restore();
            BlockEntity tile = w.getBlockEntity(p);
            if (tile != null) {
                tile.onLoad();
            }
        }

        // Step 3: Place stripes pipe back and remove old one
        if (!canceled) {
            // - Correct NBT coordinates
            stripesNBTOld.putInt("x", p.getX());
            stripesNBTOld.putInt("y", p.getY());
            stripesNBTOld.putInt("z", p.getZ());

            // - Create block and tile
            FakePlayer player = BuildCraftAPI.fakePlayerProvider.getFakePlayer((ServerLevel) w, owner, p);
            player.getInventory().clearContent();
            w.setBlock(p, stripesStateOld, 3);
            if (canceled = EventHooks.onBlockPlace(player, blockSnapshot2, r.dir)) {
                blockSnapshot2.restore();
                BlockEntity tile = w.getBlockEntity(r.pos);
                if (tile != null) {
                    tile.onLoad();
                }
            } else {
                SoundUtil.playBlockBreak(w, p, blockSnapshot2.getState());

                canceled = !BlockUtil.breakBlock((ServerLevel) w, r.pos, NonNullList.create(), r.pos, owner);
                if (canceled) {
                    blockSnapshot1.restore();
                    BlockEntity tile1 = w.getBlockEntity(r.pos);
                    if (tile1 != null) {
                        tile1.onLoad();
                    }

                    blockSnapshot2.restore();
                    BlockEntity tile2 = w.getBlockEntity(p);
                    if (tile2 != null) {
                        tile2.onLoad();
                    }
                } else {
                    stacksToSendBack.addAll(list);
                    for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                        ItemStack stack = player.getInventory().removeItemNoUpdate(i);
                        if (!stack.isEmpty()) {
                            stacksToSendBack.add(stack);
                        }
                    }
                }
            }
        }

        // Step 4: Hope for the best, clean up.
        cleanup(w, r, p, stacksToSendBack, canceled, stripesNBTOld);
    }

    private void extend(Level w, PipeExtensionRequest r) {
        BlockPos p = r.pos.relative(r.dir);
        if (!w.isEmptyBlock(p) && !w.getBlockState(p).canBeReplaced()) {
            r.stripes.sendItem(r.stack.copy(), r.dir);
            return;
        }

        NonNullList<ItemStack> stacksToSendBack = NonNullList.create();

        // Step 1: Copy over and remove existing stripes pipe
        BlockState stripesStateOld = w.getBlockState(r.pos);
        CompoundTag stripesNBTOld = new CompoundTag();
        BlockEntity stripesTileOld = w.getBlockEntity(r.pos);
        final GameProfile owner;
        // Fetch owner
        {
            IPipeHolder holder = CapUtil.getCapability(stripesTileOld, PipeApi.CAP_PIPE_HOLDER, null);
            if (stripesTileOld == null || holder == null) {
                BCLog.logger.warn("Found an invalid request at " + r.pos + " as " + stripesTileOld + " was not a pipe tile!");
                return;
            }
            owner = holder.getOwner();
        }

        stripesNBTOld = ((TileBC_Neptune) stripesTileOld).writeToNBT(new CompoundTag());
        BlockSnapshot blockSnapshot1 = BlockSnapshot.create(w.dimension(), w, r.pos);
        boolean canceled = !BlockUtil.breakBlock((ServerLevel) w, r.pos, NonNullList.create(), r.pos, owner);
        if (canceled) {
            stacksToSendBack.add(r.stack);

            blockSnapshot1.restore();
            BlockEntity tile = w.getBlockEntity(r.pos);
            if (tile != null) {
                tile.onLoad();
            }
        }

        NonNullList<ItemStack> list = NonNullList.create();

        // Step 2: Add new pipe
        if (!canceled) {
            FakePlayer player = BuildCraftAPI.fakePlayerProvider.getFakePlayer((ServerLevel) w, owner, r.pos);
            player.getInventory().clearContent();
            player.getInventory().setItem(player.getInventory().getSelectedSlot(), r.stack);
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(r.pos), r.dir.getOpposite(), r.pos, false);
            InteractionResult result = CommonHooks.onPlaceItemIntoWorld(
                new UseOnContext(w, player, InteractionHand.MAIN_HAND, r.stack, hit));
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().removeItemNoUpdate(i);
                if (!stack.isEmpty()) {
                    list.add(stack);
                }
            }
            if (canceled = !result.consumesAction()) {
                blockSnapshot1.restore();
                BlockEntity tile = w.getBlockEntity(r.pos);
                if (tile != null) {
                    tile.onLoad();
                }
            }
        }

        // Step 3: Place stripes pipe back
        if (!canceled) {
            // - Correct NBT coordinates
            stripesNBTOld.putInt("x", p.getX());
            stripesNBTOld.putInt("y", p.getY());
            stripesNBTOld.putInt("z", p.getZ());

            // - Create block and tile
            FakePlayer player = BuildCraftAPI.fakePlayerProvider.getFakePlayer((ServerLevel) w, owner, p);
            player.getInventory().clearContent();
            BlockSnapshot blockSnapshot2 = BlockSnapshot.create(w.dimension(), w, p);
            w.setBlock(p, stripesStateOld, 3);
            if (canceled = EventHooks.onBlockPlace(player, blockSnapshot2, r.dir.getOpposite())) {
                stacksToSendBack.add(r.stack);

                blockSnapshot1.restore();
                BlockEntity tile = w.getBlockEntity(r.pos);
                if (tile != null) {
                    tile.onLoad();
                }

                blockSnapshot2.restore();
            } else {
                stacksToSendBack.addAll(list);
            }
        } else {
            stacksToSendBack.addAll(list);
        }

        // Step 4: Hope for the best, clean up.
        cleanup(w, r, p, stacksToSendBack, canceled, stripesNBTOld);
    }

    private void cleanup(Level w, PipeExtensionRequest r, BlockPos p, NonNullList<ItemStack> stacksToSendBack, boolean canceled, CompoundTag stripesNBTOld) {
        BlockEntity stripesTileNew = w.getBlockEntity(canceled ? r.pos : p);
        if (stripesTileNew == null) {
            // Odd.
            // Maybe it would be better to crash?
            InventoryUtil.dropAll(w, p, stacksToSendBack);
            return;
        }
        if (!canceled) {
            if (stripesTileNew instanceof TileBC_Neptune tileNew) {
                tileNew.readFromNBT(stripesNBTOld);
            }
            stripesTileNew.onLoad();
        }

        IPipeHolder stripesPipeHolderNew = CapUtil.getCapability(stripesTileNew, PipeApi.CAP_PIPE_HOLDER, null);
        if (stripesPipeHolderNew != null) {
            if (!canceled) {
                IWireManager wireManager = stripesPipeHolderNew.getWireManager();
                if (wireManager instanceof WireManager) {
                    ((WireManager) wireManager).getWireSystems().rebuildWireSystemsAround(stripesPipeHolderNew);
                }
            }

            PipeBehaviour behaviour = stripesPipeHolderNew.getPipe().getBehaviour();
            if (behaviour instanceof IStripesActivator) {
                IStripesActivator stripesNew = (IStripesActivator) behaviour;
                for (ItemStack s : stacksToSendBack) {
                    s = s.copy();
                    if (!stripesNew.sendItem(s, r.dir)) {
                        stripesNew.dropItem(s, r.dir);
                    }
                }
            } else {
                InventoryUtil.dropAll(w, p, stacksToSendBack);
            }
        } else {
            InventoryUtil.dropAll(w, p, stacksToSendBack);
        }
    }

    private boolean isValidRetractionPath(Level w, PipeExtensionRequest r, Direction retractDir) {
        BlockEntity tile = w.getBlockEntity(r.pos.relative(retractDir));
        IPipe pipe = CapUtil.getCapability(tile, PipeApi.CAP_PIPE, null);
        if (pipe != null) {
            boolean connected = false;
            for (Direction facing : Direction.values()) {
                if (pipe.getConnectedType(facing) == IPipe.ConnectedType.TILE) {
                    return false;
                }
                if (facing == retractDir.getOpposite() && pipe.getConnectedType(facing) != IPipe.ConnectedType.PIPE) {
                    return false;
                }
                if (facing != retractDir.getOpposite() && connected && pipe.getConnectedType(facing) != null) {
                    return false;
                }
                if (facing != retractDir.getOpposite() && !connected && pipe.getConnectedType(facing) != null) {
                    connected = true;
                }

            }
            return true;
        }
        return false;
    }

    private class PipeExtensionRequest {
        public final BlockPos pos;
        public final Direction dir;
        public final IStripesActivator stripes;
        public final PipeDefinition pipeDef;
        public final ItemStack stack;

        private PipeExtensionRequest(BlockPos pos, Direction dir, IStripesActivator stripes, PipeDefinition pipeDef, ItemStack stack) {
            this.pos = pos;
            this.dir = dir;
            this.stripes = stripes;
            this.pipeDef = pipeDef;
            this.stack = stack;
        }
    }
}
