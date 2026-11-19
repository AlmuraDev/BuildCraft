/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.misc;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;

import buildcraft.api.core.CapabilitiesHelper;
import buildcraft.api.core.ICapabilityProvider;
import buildcraft.api.inventory.IItemTransactor;
import buildcraft.api.transfer.IEnergyStorage;
import buildcraft.api.transfer.IFluidHandler;
import buildcraft.api.transfer.IItemHandler;
import buildcraft.api.transfer.NeoTransfer;

/** BuildCraft's own item, fluid and RF capabilities. BuildCraft tiles offer their handlers under these keys (NeoForge
 * sees them adapted, see {@link buildcraft.lib.registry.TileTypes}); asking a block that isn't a BuildCraft tile goes to
 * NeoForge's capabilities and adapts the answer. */
public class CapUtil {
    @Nonnull
    public static final BlockCapability<IItemHandler, Direction> CAP_ITEMS = CapabilitiesHelper.registerCapability(IItemHandler.class);

    @Nonnull
    public static final BlockCapability<IFluidHandler, Direction> CAP_FLUIDS = CapabilitiesHelper.registerCapability(IFluidHandler.class);

    @Nonnull
    public static final BlockCapability<IEnergyStorage, Direction> CAP_RF = CapabilitiesHelper.registerCapability(IEnergyStorage.class);

    @Nonnull
    public static final BlockCapability<IItemTransactor, Direction> CAP_ITEM_TRANSACTOR =
        CapabilitiesHelper.registerCapability(IItemTransactor.class);

    @Nullable
    public static <T> T getCapability(ICapabilityProvider provider, BlockCapability<T, Direction> capability, Direction facing) {
        if (provider == null || capability == null) {
            return null;
        }
        return provider.getCapability(capability, facing);
    }

    /** Asks the given tile for a capability: BuildCraft tiles directly, anything else through NeoForge (BuildCraft's item,
     * fluid and RF keys are adapted from NeoForge's handlers). */
    @Nullable
    public static <T> T getCapability(@Nullable BlockEntity tile, BlockCapability<T, Direction> capability, @Nullable Direction facing) {
        if (tile == null || capability == null) {
            return null;
        }
        if (tile instanceof ICapabilityProvider provider) {
            return provider.getCapability(capability, facing);
        }
        Level level = tile.getLevel();
        if (level == null) {
            return null;
        }
        return getCapability(level, tile.getBlockPos(), capability, facing);
    }

    /** Asks the block at the given position for a capability, the way {@link #getCapability(BlockEntity, BlockCapability,
     * Direction)} does. */
    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> T getCapability(Level level, BlockPos pos, BlockCapability<T, Direction> capability, @Nullable Direction facing) {
        BlockEntity tile = level.getBlockEntity(pos);
        if (tile instanceof ICapabilityProvider provider) {
            return provider.getCapability(capability, facing);
        }
        if (capability == CAP_ITEMS) {
            return (T) NeoTransfer.fromNeoItems(level.getCapability(Capabilities.Item.BLOCK, pos, facing));
        }
        if (capability == CAP_FLUIDS) {
            return (T) NeoTransfer.fromNeoFluids(level.getCapability(Capabilities.Fluid.BLOCK, pos, facing));
        }
        if (capability == CAP_RF) {
            return (T) NeoTransfer.fromNeoEnergy(level.getCapability(Capabilities.Energy.BLOCK, pos, facing));
        }
        return level.getCapability(capability, pos, facing);
    }
}
