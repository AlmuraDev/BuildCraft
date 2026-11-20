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
import java.util.List;

import net.minecraft.world.level.block.state.properties.Property;

import buildcraft.api.properties.BuildCraftProperties;

import buildcraft.lib.block.BlockBCTile_Neptune;
import buildcraft.lib.block.IBlockWithFacing;

import buildcraft.builders.BCBuildersGuis;

public class BlockArchitectTable extends BlockBCTile_Neptune implements IBlockWithFacing {
    public BlockArchitectTable(BlockBehaviour.Properties properties, String id) {
        super(properties, id);
    }

    public static final Property<Boolean> PROP_VALID = BuildCraftProperties.VALID;

    @Override
    protected void addProperties(List<Property<?>> properties) {
        super.addProperties(properties);
        properties.add(PROP_VALID);
    }

    {
        registerDefaultState(defaultBlockState().setValue(PROP_VALID, Boolean.TRUE));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player,
        BlockHitResult hit) {
        if (!world.isClientSide()) {
            BCBuildersGuis.ARCHITECT.openGUI(player, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean canBeRotated(Level world, BlockPos pos, BlockState state) {
        return false;
    }
}
