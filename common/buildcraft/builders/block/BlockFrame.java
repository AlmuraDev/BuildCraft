/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.block;

import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import buildcraft.api.properties.BuildCraftProperties;

import buildcraft.lib.block.BlockBCBase_Neptune;

/** The quarry frame. It drops nothing (no loot table). The CONNECTED properties are real state properties now (1.12.2
 * worked them out in getActualState), kept up to date by {@link #updateShape}. */
public class BlockFrame extends BlockBCBase_Neptune {
    public static final Map<Direction, Property<Boolean>> CONNECTED_MAP = BuildCraftProperties.CONNECTED_MAP;

    private static final VoxelShape BASE = Block.box(4, 4, 4, 12, 12, 12);
    private static final Map<Direction, VoxelShape> CONNECTIONS = Map.of(
        Direction.DOWN, Block.box(4, 0, 4, 12, 4, 12),
        Direction.UP, Block.box(4, 12, 4, 12, 16, 12),
        Direction.NORTH, Block.box(4, 4, 0, 12, 12, 4),
        Direction.SOUTH, Block.box(4, 4, 12, 12, 12, 16),
        Direction.WEST, Block.box(0, 4, 4, 4, 12, 12),
        Direction.EAST, Block.box(12, 4, 4, 16, 12, 12));

    public BlockFrame(BlockBehaviour.Properties properties, String id) {
        super(properties.noOcclusion().noLootTable(), id);
    }

    @Override
    protected void addProperties(List<Property<?>> properties) {
        super.addProperties(properties);
        properties.addAll(CONNECTED_MAP.values());
    }

    private static boolean connectsTo(BlockState neighbour) {
        Block block = neighbour.getBlock();
        return block instanceof BlockFrame || block instanceof BlockQuarry;
    }

    /** The state a frame placed at pos should have, connected to the frames (and quarry) already around it. The
     * quarry places frames itself, so it can't rely on getStateForPlacement. */
    public BlockState getConnectedState(BlockGetter world, BlockPos pos) {
        return withConnections(defaultBlockState(), world, pos);
    }

    private BlockState withConnections(BlockState state, BlockGetter world, BlockPos pos) {
        for (Direction side : Direction.values()) {
            state = state.setValue(CONNECTED_MAP.get(side), connectsTo(world.getBlockState(pos.relative(side))));
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
        return state.setValue(CONNECTED_MAP.get(direction), connectsTo(neighbourState));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        VoxelShape shape = BASE;
        for (Direction side : Direction.values()) {
            if (state.getValue(CONNECTED_MAP.get(side))) {
                shape = Shapes.or(shape, CONNECTIONS.get(side));
            }
        }
        return shape;
    }

    /** Straight runs of frame hide the faces that meet the next frame (1.12.2's shouldSideBeRendered). */
    @Override
    protected boolean skipRendering(BlockState state, BlockState neighbour, Direction side) {
        return connectsTo(neighbour) && state.getValue(CONNECTED_MAP.get(side));
    }
}
