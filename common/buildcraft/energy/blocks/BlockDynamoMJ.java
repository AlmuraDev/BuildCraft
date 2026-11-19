/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.energy.blocks;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;

import buildcraft.api.blocks.ICustomRotationHandler;

import buildcraft.lib.block.BlockBCTile_Neptune;

import buildcraft.energy.tile.TileDynamoMJ;

/** The MJ to RF dynamo. Like the engines it is drawn entirely by its tile renderer, which animates it. */
public class BlockDynamoMJ extends BlockBCTile_Neptune implements ICustomRotationHandler {

    public BlockDynamoMJ(BlockBehaviour.Properties properties, String id) {
        super(properties.noOcclusion(), id);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block block,
        @Nullable Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, world, pos, block, orientation, movedByPiston);
        if (world.isClientSide()) return;
        BlockEntity tile = world.getBlockEntity(pos);
        if (tile instanceof TileDynamoMJ) {
            ((TileDynamoMJ) tile).rotateIfInvalid();
        }
    }

    // ICustomRotationHandler

    @Override
    public InteractionResult attemptRotation(Level world, BlockPos pos, BlockState state, Direction sideWrenched) {
        BlockEntity tile = world.getBlockEntity(pos);
        if (tile instanceof TileDynamoMJ) {
            return ((TileDynamoMJ) tile).attemptRotation();
        }
        return InteractionResult.FAIL;
    }
}
