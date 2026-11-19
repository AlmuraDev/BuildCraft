/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.block;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import buildcraft.api.blocks.ICustomRotationHandler;
import buildcraft.api.properties.BuildCraftProperties;

/** A marker: a small post attached to the side of a block. Its "active" look is a real state property on 26.3 (1.12.2
 * worked it out from the tile when rendering), set by the tile through {@link #setActive}. */
public abstract class BlockMarkerBase extends BlockBCTile_Neptune implements ICustomRotationHandler {
    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        double halfWidth = 0.1;
        double h = 0.65;
        // Little variables to make reading a *bit* more sane
        final double nw = 0.5 - halfWidth;
        final double pw = 0.5 + halfWidth;
        final double ih = 1 - h;
        SHAPES.put(Direction.DOWN, Shapes.box(nw, ih, nw, pw, 1, pw));
        SHAPES.put(Direction.UP, Shapes.box(nw, 0, nw, pw, h, pw));
        SHAPES.put(Direction.SOUTH, Shapes.box(nw, nw, 0, pw, pw, h));
        SHAPES.put(Direction.NORTH, Shapes.box(nw, nw, ih, pw, pw, 1));
        SHAPES.put(Direction.EAST, Shapes.box(0, nw, nw, h, pw, pw));
        SHAPES.put(Direction.WEST, Shapes.box(ih, nw, nw, 1, pw, pw));
    }

    /** The properties markers want on top of BuildCraft's defaults. */
    public static BlockBehaviour.Properties markerProperties(BlockBehaviour.Properties properties) {
        return properties.strength(0.25F).noCollision().noOcclusion();
    }

    public BlockMarkerBase(BlockBehaviour.Properties properties, String id) {
        super(markerProperties(properties), id);

        BlockState defaultState = defaultBlockState();
        defaultState = defaultState.setValue(BuildCraftProperties.BLOCK_FACING_6, Direction.UP);
        defaultState = defaultState.setValue(BuildCraftProperties.ACTIVE, false);
        registerDefaultState(defaultState);
    }

    @Override
    protected void addProperties(List<Property<?>> properties) {
        super.addProperties(properties);
        properties.add(BuildCraftProperties.BLOCK_FACING_6);
        properties.add(BuildCraftProperties.ACTIVE);
    }

    /** Shows the marker as active or not (called by its tile, on either side, whenever that changes). */
    public static void setActive(Level world, BlockPos pos, boolean active) {
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof BlockMarkerBase && state.getValue(BuildCraftProperties.ACTIVE) != active) {
            world.setBlock(pos, state.setValue(BuildCraftProperties.ACTIVE, active), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(BuildCraftProperties.BLOCK_FACING_6));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(BuildCraftProperties.BLOCK_FACING_6, context.getClickedFace());
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    /** Markers need a solid side to stand on (1.12.2's canPlaceBlockOnSide). */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        Direction sideOn = state.getValue(BuildCraftProperties.BLOCK_FACING_6);
        BlockPos onPos = pos.relative(sideOn.getOpposite());
        return world.getBlockState(onPos).isFaceSturdy(world, onPos, sideOn);
    }

    /** Breaks the marker when the block it stands on goes (1.12.2's neighborChanged check). */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess ticks, BlockPos pos,
        Direction direction, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (!canSurvive(state, world, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, world, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    @Override
    public InteractionResult attemptRotation(Level world, BlockPos pos, BlockState state, Direction sideWrenched) {
        if (state.getBlock() instanceof BlockMarkerBase) {// Just check to make sure we have the right block...
            return VanillaRotationHandlers.rotateOnce(world, pos, state, BuildCraftProperties.BLOCK_FACING_6,
                VanillaRotationHandlers.ROTATE_FACING);
        } else {
            return InteractionResult.PASS;
        }
    }
}
