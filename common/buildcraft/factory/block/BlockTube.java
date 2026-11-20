/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import buildcraft.lib.block.BlockBCBase_Neptune;

import buildcraft.factory.tile.TileMiner;

/** The mining tube under a mining well or pump. Unbreakable while its miner is above it. */
public class BlockTube extends BlockBCBase_Neptune {
    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 16, 12);

    public BlockTube(BlockBehaviour.Properties properties, String id) {
        super(properties.strength(-1.0F, 3600000.0F).noOcclusion(), id);
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level world, BlockPos pos, Player player, ItemStack toolStack,
        boolean willHarvest, FluidState fluid) {
        BlockPos currentPos = pos;
        // noinspection StatementWithEmptyBody
        while (world.getBlockState(currentPos = currentPos.above()).getBlock() == this) {
        }
        if (!(world.getBlockEntity(currentPos) instanceof TileMiner)) {
            return super.onDestroyedByPlayer(state, world, pos, player, toolStack, willHarvest, fluid);
        } else {
            return false;
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
