/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import buildcraft.lib.block.BlockBCTile_Neptune;
import buildcraft.lib.block.IBlockWithFacing;

import buildcraft.builders.BCBuildersGuis;

public class BlockReplacer extends BlockBCTile_Neptune implements IBlockWithFacing {
    public BlockReplacer(BlockBehaviour.Properties properties, String id) {
        super(properties, id);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player,
        BlockHitResult hit) {
        if (!world.isClientSide()) {
            BCBuildersGuis.REPLACER.openGUI(player, pos);
        }
        return InteractionResult.SUCCESS;
    }
}
