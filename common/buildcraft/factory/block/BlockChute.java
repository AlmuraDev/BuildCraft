/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.block;

import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;

import buildcraft.api.properties.BuildCraftProperties;

import buildcraft.lib.block.BlockBCTile_Neptune;
import buildcraft.lib.block.IBlockWithFacing;

import buildcraft.factory.BCFactoryGuis;
import buildcraft.factory.tile.TileChute;

/** The chute shows which sides have an inventory: the CONNECTED properties are real state properties now (1.12.2
 * worked them out in getActualState), kept up to date by {@link #updateShape}. */
public class BlockChute extends BlockBCTile_Neptune implements IBlockWithFacing {
    public static final Map<Direction, Property<Boolean>> CONNECTED_MAP = BuildCraftProperties.CONNECTED_MAP;

    public BlockChute(BlockBehaviour.Properties properties, String id) {
        super(properties.noOcclusion(), id);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player,
        BlockHitResult hit) {
        if (!world.isClientSide()) {
            BCFactoryGuis.CHUTE.openGUI(player, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void addProperties(List<Property<?>> properties) {
        super.addProperties(properties);
        properties.addAll(CONNECTED_MAP.values());
    }

    private BlockState withConnections(BlockState state, BlockGetter world, BlockPos pos) {
        for (Direction side : Direction.values()) {
            state = state.setValue(CONNECTED_MAP.get(side), side != state.getValue(getFacingProperty())
                && TileChute.hasInventoryAtPosition(world, pos.relative(side), side));
        }
        return state;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : withConnections(state, context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess ticks, BlockPos pos,
        Direction direction, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        state = super.updateShape(state, world, ticks, pos, direction, neighbourPos, neighbourState, random);
        return withConnections(state, world, pos);
    }

    // IBlockWithFacing

    @Override
    public boolean canFaceVertically() {
        return true;
    }
}
