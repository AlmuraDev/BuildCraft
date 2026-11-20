/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.tile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.core.Direction;
import buildcraft.lib.tile.ITickable;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;

import buildcraft.api.core.ICapabilityProvider;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.mj.MjBattery;
import buildcraft.api.mj.MjCapabilityHelper;
import buildcraft.api.tiles.IDebuggable;

import buildcraft.lib.block.BlockBCBase_Neptune;
import buildcraft.lib.inventory.ItemTransactorHelper;
import buildcraft.lib.inventory.NoSpaceTransactor;
import buildcraft.lib.inventory.TransactorEntityItem;
import buildcraft.lib.misc.AdvancementUtil;
import buildcraft.lib.misc.BoundingBoxUtil;
import buildcraft.lib.mj.MjBatteryReceiver;
import buildcraft.lib.tile.TileBC_Neptune;
import buildcraft.lib.tile.item.ItemHandlerManager.EnumAccess;
import buildcraft.lib.tile.item.ItemHandlerSimple;

import buildcraft.factory.block.BlockChute;

public class TileChute extends TileBC_Neptune implements ITickable, IDebuggable {
    private static final Identifier ADVANCEMENT_DID_INSERT = Identifier.parse("buildcraftfactory:retired_hopper");

    private static final int PICKUP_MAX = 3;

    public final ItemHandlerSimple inv = itemManager.addInvHandler(
        "inv",
        4,
        EnumAccess.INSERT,
        EnumPipePart.VALUES
    );

    private final MjBattery battery = new MjBattery(1 * MjAPI.MJ);
    private int progress = 0;

    public TileChute(BlockPos pos, BlockState state) {
        super(pos, state);
        caps.addProvider(new MjCapabilityHelper(new MjBatteryReceiver(battery)));
    }

    public static boolean hasInventoryAtPosition(BlockGetter world, BlockPos pos, Direction side) {
        BlockEntity tile = world.getBlockEntity(pos);
        return ItemTransactorHelper.getTransactor(tile, side.getOpposite()) != NoSpaceTransactor.INSTANCE;
    }

    private void pickupItems(Direction currentSide) {
        AABB aabb = BoundingBoxUtil.extrudeFace(getPos(), currentSide, 0.25);
        int count = PICKUP_MAX;
        for (ItemEntity entity : world.getEntitiesOfClass(ItemEntity.class, aabb, EntitySelector.ENTITY_STILL_ALIVE)) {
            int moved = ItemTransactorHelper.move(new TransactorEntityItem(entity), inv, count);
            count -= moved;
            if (count <= 0) {
                return;
            }
        }
    }

    private void putInNearInventories(Direction currentSide) {
        boolean[] didWork = { false };
        List<Direction> sides = new ArrayList<>(Arrays.asList(Direction.values()));
        Collections.shuffle(sides, new Random());
        sides.removeIf(Predicate.isEqual(currentSide));
        // 1.12.2 also inserted into non-living entities with an item capability (minecarts); 26.3 entity
        // capabilities aren't sided BlockCapabilities, so only blocks are filled. TODO(26.3): entity inventories.
        sides.stream()
            .map(side -> ItemTransactorHelper.getTransactor(world.getBlockEntity(pos.relative(side)), side.getOpposite()))
            .filter(Predicate.isEqual(NoSpaceTransactor.INSTANCE).negate())
            .forEach(transactor -> {
                if (ItemTransactorHelper.move(inv, transactor, 1) > 0) {
                    didWork[0] = true;
                }
            });
        if (didWork[0]) {
            AdvancementUtil.unlockAdvancement(getOwner().id(), ADVANCEMENT_DID_INSERT);
        }
    }

    // ITickable

    @Override
    public void update() {
        if (world.isClientSide()) {
            return;
        }

        if (!(world.getBlockState(pos).getBlock() instanceof BlockChute)) {
            return;
        }

        battery.tick(getWorld(), getPos());

        Direction currentSide = world.getBlockState(pos).getValue(BlockBCBase_Neptune.BLOCK_FACING_6);

        int target = 100000;
        if (currentSide == Direction.UP) {
            progress += 1000; // can be free because of gravity
        }
        progress += battery.extractPower(0, target - progress);

        if (progress >= target) {
            progress = 0;
            pickupItems(currentSide);
        }

        putInNearInventories(currentSide);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        progress = nbt.getIntOr("progress", 0);
        battery.deserializeNBT(nbt.getCompoundOrEmpty("battery"));
    }

    @Override
    public CompoundTag writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        nbt.putInt("progress", progress);
        nbt.put("battery", battery.serializeNBT());
        return nbt;
    }

    // IDebuggable

    @Override
    public void getDebugInfo(List<String> left, List<String> right, Direction side) {
        left.add("battery = " + battery.getDebugString());
        left.add("progress = " + progress);
    }
}
