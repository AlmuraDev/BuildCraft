/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import buildcraft.api.blocks.CustomRotationHelper;

import buildcraft.lib.misc.collect.OrderedEnumMap;

/** Wrench rotation for vanilla blocks. 1.12.2 had a handler per vanilla block class; 26.3's blocks all describe their
 * orientation with the same few state properties, so one handler (per property) covers them, rotating through
 * neighbouring sides only and skipping orientations the block can't survive in. */
public class VanillaRotationHandlers {
    /* Player friendly rotations- these only rotate through sides that are touching (only 90 degree changes, in any
     * axis), rather than jumping around. */
    public static final OrderedEnumMap<Direction> ROTATE_HORIZONTAL, ROTATE_FACING, ROTATE_HOPPER;

    static {
        Direction e = Direction.EAST, w = Direction.WEST;
        Direction u = Direction.UP, d = Direction.DOWN;
        Direction n = Direction.NORTH, s = Direction.SOUTH;
        ROTATE_HORIZONTAL = new OrderedEnumMap<>(Direction.class, e, s, w, n);
        ROTATE_FACING = new OrderedEnumMap<>(Direction.class, e, s, d, w, n, u);
        ROTATE_HOPPER = new OrderedEnumMap<>(Direction.class, e, s, w, n, d);
    }

    public static void fmlInit() {
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!"minecraft".equals(BuiltInRegistries.BLOCK.getKey(block).getNamespace())) {
                continue;
            }
            BlockState state = block.defaultBlockState();
            if (block instanceof DoorBlock) {
                CustomRotationHelper.INSTANCE.registerHandler(block, VanillaRotationHandlers::rotateDoor);
            } else if (block instanceof ChestBlock) {
                CustomRotationHelper.INSTANCE.registerHandler(block, VanillaRotationHandlers::rotateChest);
            } else if (state.hasProperty(BlockStateProperties.ATTACH_FACE)
                && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                CustomRotationHelper.INSTANCE.registerHandler(block, VanillaRotationHandlers::rotateAttached);
            } else if (state.hasProperty(BlockStateProperties.FACING)) {
                CustomRotationHelper.INSTANCE.registerHandler(block,
                    (w, p, s, side) -> rotateOnce(w, p, s, BlockStateProperties.FACING, ROTATE_FACING));
            } else if (state.hasProperty(BlockStateProperties.FACING_HOPPER)) {
                CustomRotationHelper.INSTANCE.registerHandler(block,
                    (w, p, s, side) -> rotateOnce(w, p, s, BlockStateProperties.FACING_HOPPER, ROTATE_HOPPER));
            } else if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                CustomRotationHelper.INSTANCE.registerHandler(block,
                    (w, p, s, side) -> rotateOnce(w, p, s, BlockStateProperties.HORIZONTAL_FACING, ROTATE_HORIZONTAL));
            } else if (state.hasProperty(BlockStateProperties.ROTATION_16)) {
                CustomRotationHelper.INSTANCE.registerHandler(block, VanillaRotationHandlers::rotateSixteen);
            } else if (state.hasProperty(BlockStateProperties.AXIS)) {
                CustomRotationHelper.INSTANCE.registerHandler(block, VanillaRotationHandlers::rotateAxis);
            }
        }
    }

    public static <T> int getOrdinal(T side, T[] array) {
        for (int i = 0; i < array.length; i++) {
            if (side == array[i]) return i;
        }
        return 0;
    }

    /** Rotates the block's facing to the next one (in the given order) that it can survive in. */
    public static InteractionResult rotateOnce(Level world, BlockPos pos, BlockState state, EnumProperty<Direction> prop,
        OrderedEnumMap<Direction> order) {
        Direction current = state.getValue(prop);
        Direction next = current;
        for (int i = 0; i < order.getOrderLength(); i++) {
            next = order.next(next);
            if (!prop.getPossibleValues().contains(next)) {
                continue;
            }
            BlockState rotated = state.setValue(prop, next);
            if (rotated.canSurvive(world, pos)) {
                world.setBlockAndUpdate(pos, rotated);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.FAIL;
    }

    /** Doors turn both halves; after a full turn their hinge swaps sides, as on 1.12.2. */
    private static InteractionResult rotateDoor(Level world, BlockPos pos, BlockState state, Direction sideWrenched) {
        BlockPos lowerPos = state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
        BlockPos upperPos = lowerPos.above();
        BlockState lower = world.getBlockState(lowerPos);
        BlockState upper = world.getBlockState(upperPos);
        if (!(lower.getBlock() instanceof DoorBlock) || !(upper.getBlock() instanceof DoorBlock)) {
            return InteractionResult.PASS;
        }
        Direction next = ROTATE_HORIZONTAL.next(lower.getValue(DoorBlock.FACING));
        lower = lower.setValue(DoorBlock.FACING, next);
        upper = upper.setValue(DoorBlock.FACING, next);
        if (next == ROTATE_HORIZONTAL.get(0)) {
            lower = lower.cycle(DoorBlock.HINGE);
            upper = upper.cycle(DoorBlock.HINGE);
        }
        world.setBlock(upperPos, upper, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        world.setBlock(lowerPos, lower, Block.UPDATE_ALL);
        return InteractionResult.SUCCESS;
    }

    /** Single chests turn; double chests don't (turning one half would break them apart). */
    private static InteractionResult rotateChest(Level world, BlockPos pos, BlockState state, Direction sideWrenched) {
        if (state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            return InteractionResult.FAIL;
        }
        return rotateOnce(world, pos, state, ChestBlock.FACING, ROTATE_HORIZONTAL);
    }

    /** Buttons, levers and grindstones: wall sides in turn, then the floor and ceiling. */
    private static InteractionResult rotateAttached(Level world, BlockPos pos, BlockState state, Direction sideWrenched) {
        AttachFace face = state.getValue(BlockStateProperties.ATTACH_FACE);
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        for (int i = 0; i < 6; i++) {
            if (face == AttachFace.WALL) {
                if (facing == ROTATE_HORIZONTAL.get(ROTATE_HORIZONTAL.getOrderLength() - 1)) {
                    face = AttachFace.FLOOR;
                }
                facing = ROTATE_HORIZONTAL.next(facing);
            } else if (face == AttachFace.FLOOR) {
                face = AttachFace.CEILING;
            } else {
                face = AttachFace.WALL;
                facing = ROTATE_HORIZONTAL.get(0);
            }
            BlockState rotated = state.setValue(BlockStateProperties.ATTACH_FACE, face)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
            if (rotated.canSurvive(world, pos)) {
                world.setBlockAndUpdate(pos, rotated);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.FAIL;
    }

    /** Signs, banners and skulls standing on the ground: a quarter turn. */
    private static InteractionResult rotateSixteen(Level world, BlockPos pos, BlockState state, Direction sideWrenched) {
        int rotation = state.getValue(BlockStateProperties.ROTATION_16);
        world.setBlockAndUpdate(pos, state.setValue(BlockStateProperties.ROTATION_16, (rotation + 4) & 15));
        return InteractionResult.SUCCESS;
    }

    /** Logs, pillars and chains: the next axis. */
    private static InteractionResult rotateAxis(Level world, BlockPos pos, BlockState state, Direction sideWrenched) {
        world.setBlockAndUpdate(pos, state.cycle(BlockStateProperties.AXIS));
        return InteractionResult.SUCCESS;
    }
}
