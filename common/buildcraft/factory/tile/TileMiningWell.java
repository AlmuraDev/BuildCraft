/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.tile;

import javax.annotation.Nonnull;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import buildcraft.lib.world.WorldEventListeners;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

import net.minecraft.world.level.material.Fluid;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.core.SafeTimeTracker;
import buildcraft.api.mj.IMjReceiver;

import buildcraft.lib.inventory.AutomaticProvidingTransactor;
import buildcraft.lib.misc.BlockUtil;
import buildcraft.lib.misc.CapUtil;
import buildcraft.lib.misc.InventoryUtil;
import buildcraft.lib.mj.MjBatteryReceiver;
import buildcraft.lib.world.WorldEventListenerAdapter;

import buildcraft.core.BCCoreConfig;
import buildcraft.factory.BCFactoryBlocks;

public class TileMiningWell extends TileMiner {
    private boolean shouldCheck = true;
    private final SafeTimeTracker tracker = new SafeTimeTracker(256);
    private final WorldEventListenerAdapter worldEventListener = new WorldEventListenerAdapter() {
        @Override
        public void sendBlockUpdated(@Nonnull Level world,
                                      @Nonnull BlockPos pos,
                                      @Nonnull BlockState oldState,
                                      @Nonnull BlockState newState,
                                      int flags) {
            if (pos.getX() == TileMiningWell.this.pos.getX() &&
                pos.getY() <= TileMiningWell.this.pos.getY() &&
                pos.getZ() == TileMiningWell.this.pos.getZ()) {
                shouldCheck = true;
            }
        }
    };

    public TileMiningWell(BlockPos pos, BlockState state) {
        super(pos, state);
        caps.addCapabilityInstance(CapUtil.CAP_ITEM_TRANSACTOR, AutomaticProvidingTransactor.INSTANCE, EnumPipePart.VALUES);
    }

    @Override
    protected void mine() {
        if (currentPos != null && canBreak()) {
            shouldCheck = true;
            long target = BlockUtil.computeBlockBreakPower(world, currentPos);
            progress += battery.extractPower(0, target - progress);
            if (progress >= target) {
                progress = 0;
                world.destroyBlockProgress(currentPos.hashCode(), currentPos, -1);
                BlockUtil.breakBlockAndGetDrops(
                    (ServerLevel) world,
                    currentPos,
                    new ItemStack(Items.DIAMOND_PICKAXE),
                    getOwner()
                ).ifPresent(stacks ->
                    stacks.forEach(stack -> InventoryUtil.addToBestAcceptor(world, pos, null, stack))
                );
                nextPos();
            } else {
                if (!world.isEmptyBlock(currentPos)) {
                    world.destroyBlockProgress(currentPos.hashCode(), currentPos, (int) ((progress * 9) / target));
                }
            }
        } else if (shouldCheck || tracker.markTimeIfDelay(world)) {
            nextPos();
            if (currentPos == null) {
                shouldCheck = false;
            }
        }
    }

    private boolean canBreak() {
        if (world.isEmptyBlock(currentPos) || BlockUtil.isUnbreakableBlock(world, currentPos, getOwner())) {
            return false;
        }

        Fluid fluid = BlockUtil.getFluidWithFlowing(world, currentPos);
        return fluid == null || fluid.getFluidType().getViscosity() <= 1000;
    }

    private void nextPos() {
        currentPos = pos;
        while (true) {
            currentPos = currentPos.below();
            if (world.isOutsideBuildHeight(currentPos)) {
                break;
            }
            if (pos.getY() - currentPos.getY() > BCCoreConfig.miningMaxDepth) {
                break;
            }
            if (canBreak()) {
                updateLength();
                return;
            } else if (!world.isEmptyBlock(currentPos) && world.getBlockState(currentPos).getBlock() != BCFactoryBlocks.tube) {
                break;
            }
        }
        currentPos = null;
        updateLength();
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (!world.isClientSide()) {
            WorldEventListeners.add(world, worldEventListener);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (!world.isClientSide()) {
            WorldEventListeners.remove(world, worldEventListener);
            if (currentPos != null) {
                world.destroyBlockProgress(currentPos.hashCode(), currentPos, -1);
            }
        }
    }

    @Override
    protected IMjReceiver createMjReceiver() {
        return new MjBatteryReceiver(battery);
    }
}
