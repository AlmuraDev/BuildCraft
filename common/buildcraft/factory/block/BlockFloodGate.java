/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.block;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;

import buildcraft.api.properties.BuildCraftProperties;
import buildcraft.api.tools.IToolWrench;

import buildcraft.lib.block.BlockBCTile_Neptune;
import buildcraft.lib.tile.TileBC_Neptune;

import buildcraft.factory.tile.TileFloodGate;

/** The flood gate shows its open sides: the CONNECTED properties are real state properties now (1.12.2 read them from
 * the tile in getActualState), set whenever a wrench toggles a side. */
public class BlockFloodGate extends BlockBCTile_Neptune {
    public static final Map<Direction, Property<Boolean>> CONNECTED_MAP;

    static {
        CONNECTED_MAP = new HashMap<>(BuildCraftProperties.CONNECTED_MAP);
        CONNECTED_MAP.remove(Direction.UP);
    }

    public BlockFloodGate(BlockBehaviour.Properties properties, String id) {
        super(properties, id);
    }

    @Override
    protected void addProperties(List<Property<?>> properties) {
        super.addProperties(properties);
        properties.addAll(CONNECTED_MAP.values());
    }

    /** New flood gates have every side open (TileFloodGate.openSides). */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state != null) {
            for (Property<Boolean> prop : CONNECTED_MAP.values()) {
                state = state.setValue(prop, true);
            }
        }
        return state;
    }

    /** @return The state showing the flood gate's open sides. */
    public static BlockState withOpenSides(BlockState state, TileFloodGate tile) {
        for (Map.Entry<Direction, Property<Boolean>> entry : CONNECTED_MAP.entrySet()) {
            state = state.setValue(entry.getValue(), tile.openSides.contains(entry.getKey()));
        }
        return state;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack heldItem, BlockState state, Level world, BlockPos pos,
        Player player, InteractionHand hand, BlockHitResult hit) {
        if (heldItem.getItem() instanceof IToolWrench) {
            Direction side = hit.getDirection();
            if (!world.isClientSide() && side != Direction.UP) {
                BlockEntity tile = world.getBlockEntity(pos);
                if (tile instanceof TileFloodGate floodGate && CONNECTED_MAP.containsKey(side)) {
                    if (!floodGate.openSides.remove(side)) {
                        floodGate.openSides.add(side);
                    }
                    floodGate.queue.clear();
                    floodGate.sendNetworkUpdate(TileBC_Neptune.NET_RENDER_DATA);
                    world.setBlock(pos, withOpenSides(state, floodGate), Block.UPDATE_ALL);
                    floodGate.setChanged();
                    return InteractionResult.SUCCESS;
                }
            }
            return InteractionResult.PASS;
        }
        return super.useItemOn(heldItem, state, world, pos, player, hand, hit);
    }
}
