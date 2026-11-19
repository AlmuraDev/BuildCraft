/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.registry;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import buildcraft.api.transfer.IEnergyStorage;
import buildcraft.api.transfer.IFluidHandler;
import buildcraft.api.transfer.IItemHandler;
import buildcraft.api.transfer.NeoTransfer;

import buildcraft.lib.misc.CapUtil;
import buildcraft.lib.tile.TileBC_Neptune;

/** Knows which {@link BlockEntityType} each BuildCraft tile block creates, and exposes BuildCraft tiles' capabilities to
 * NeoForge. 1.12.2 tiles had no-argument constructors and were registered by class; on 26.3 a tile needs its type when it's
 * constructed, which {@link TileBC_Neptune} gets from here by its block. */
public final class TileTypes {
    private TileTypes() {}

    private static final Map<Block, BlockEntityType<?>> BY_BLOCK = new IdentityHashMap<>();
    private static final List<BlockEntityType<? extends TileBC_Neptune>> TYPES = new ArrayList<>();
    private static final Set<BlockCapability<?, Direction>> CAPABILITIES = new LinkedHashSet<>();

    /** Records that the given blocks create tiles of the given type. Called by {@link RegistrationHelper}. */
    public static void bind(BlockEntityType<? extends TileBC_Neptune> type, Block... blocks) {
        TYPES.add(type);
        for (Block block : blocks) {
            BlockEntityType<?> old = BY_BLOCK.put(block, type);
            if (old != null && old != type) {
                throw new IllegalStateException("The block " + block + " already creates " + old + ", not " + type);
            }
        }
    }

    /** A block that creates different tile types depending on its state (like engines, by engine type). */
    public interface IStateTileTypes {
        BlockEntityType<?> getTileType(BlockState state);
    }

    /** Records a tile type whose blocks pick it themselves (see {@link IStateTileTypes}), so its capabilities are still
     * exposed. */
    public static void addType(BlockEntityType<? extends TileBC_Neptune> type) {
        TYPES.add(type);
    }

    public static BlockEntityType<?> forState(BlockState state) {
        if (state.getBlock() instanceof IStateTileTypes stateTypes) {
            BlockEntityType<?> type = stateTypes.getTileType(state);
            if (type == null) {
                throw new IllegalStateException("No BuildCraft tile type for " + state + "!");
            }
            return type;
        }
        BlockEntityType<?> type = BY_BLOCK.get(state.getBlock());
        if (type == null) {
            throw new IllegalStateException("No BuildCraft tile type for " + state + "!");
        }
        return type;
    }

    /** Adds a BuildCraft capability that every BuildCraft tile offers to other mods through NeoForge, when it has it. */
    public static void exposeCapability(BlockCapability<?, Direction> capability) {
        CAPABILITIES.add(capability);
    }

    /** Must be on the mod event bus (of buildcraftlib). */
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        for (BlockEntityType<? extends TileBC_Neptune> type : TYPES) {
            register(event, type);
        }
    }

    private static <BE extends TileBC_Neptune> void register(RegisterCapabilitiesEvent event, BlockEntityType<BE> type) {
        for (BlockCapability<?, Direction> cap : CAPABILITIES) {
            registerOne(event, type, cap);
        }
        // NeoForge's own item, fluid and energy handlers, adapted from BuildCraft's
        event.registerBlockEntity(Capabilities.Item.BLOCK, type,
            (tile, side) -> NeoTransfer.toNeo((IItemHandler) tile.getCapability(CapUtil.CAP_ITEMS, side)));
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, type,
            (tile, side) -> NeoTransfer.toNeo((IFluidHandler) tile.getCapability(CapUtil.CAP_FLUIDS, side)));
        event.registerBlockEntity(Capabilities.Energy.BLOCK, type, (tile, side) -> {
            Object energy = tile.getCapability(Capabilities.Energy.BLOCK, side);
            if (energy != null) {
                return (net.neoforged.neoforge.transfer.energy.EnergyHandler) energy;
            }
            return NeoTransfer.toNeo((IEnergyStorage) tile.getCapability(CapUtil.CAP_RF, side));
        });
    }

    private static <BE extends TileBC_Neptune, T> void registerOne(RegisterCapabilitiesEvent event, BlockEntityType<BE> type,
        BlockCapability<T, Direction> cap) {
        event.registerBlockEntity(cap, type, (tile, side) -> tile.getCapability(cap, side));
    }
}
