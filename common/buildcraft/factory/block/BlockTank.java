/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.block;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import buildcraft.api.properties.BuildCraftProperties;
import buildcraft.api.transport.pipe.ICustomPipeConnection;

import buildcraft.lib.block.BlockBCTile_Neptune;

import buildcraft.factory.tile.TileTank;

/** Tanks join the tank below them: JOINED_BELOW is a real state property now (1.12.2 worked it out in getActualState),
 * kept up to date by {@link #updateShape}. */
public class BlockTank extends BlockBCTile_Neptune implements ICustomPipeConnection, ITankBlockConnector {
    private static final Property<Boolean> JOINED_BELOW = BuildCraftProperties.JOINED_BELOW;
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 16, 14);

    public BlockTank(BlockBehaviour.Properties properties, String id) {
        super(properties.noOcclusion(), id);
    }

    @Override
    protected void addProperties(List<Property<?>> properties) {
        super.addProperties(properties);
        properties.add(JOINED_BELOW);
    }

    private static boolean isTankBelow(BlockGetter world, BlockPos pos) {
        return world.getBlockState(pos.below()).getBlock() instanceof ITankBlockConnector;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null
            : state.setValue(JOINED_BELOW, isTankBelow(context.getLevel(), context.getClickedPos()));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess ticks, BlockPos pos,
        Direction direction, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        state = super.updateShape(state, world, ticks, pos, direction, neighbourPos, neighbourState, random);
        if (direction == Direction.DOWN) {
            state = state.setValue(JOINED_BELOW, neighbourState.getBlock() instanceof ITankBlockConnector);
        }
        return state;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState neighbourState, Direction side) {
        return side.getAxis() == Axis.Y && neighbourState.getBlock() instanceof ITankBlockConnector;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos, Direction direction) {
        BlockEntity tile = world.getBlockEntity(pos);
        if (tile instanceof TileTank) {
            return ((TileTank) tile).getComparatorLevel();
        }
        return 0;
    }

    @Override
    public float getExtension(Level world, BlockPos pos, Direction face, BlockState state) {
        return face.getAxis() == Axis.Y ? 0 : 2 / 16f;
    }
}
