/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.block;

import java.util.Locale;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import buildcraft.api.transport.pipe.ICustomPipeConnection;

import buildcraft.lib.block.BlockBCTile_Neptune;
import buildcraft.lib.block.IBlockWithFacing;

import buildcraft.factory.tile.TileHeatExchange;

/** The heat exchanger. Its only state property is the facing: which part of an exchanger a block is (start, middle,
 * end) comes from its tile, and which neighbours it joins from their states, both read by its block model
 * (ModelHeatExchange; 1.12.2 put them in getActualState). */
public class BlockHeatExchange extends BlockBCTile_Neptune implements ICustomPipeConnection, IBlockWithFacing {
    public enum EnumExchangePart implements StringRepresentable {
        START,
        MIDDLE,
        END;

        private final String lowerCaseName = name().toLowerCase(Locale.ROOT);

        @Override
        public String getSerializedName() {
            return getName();
        }

        public String getName() {
            return lowerCaseName;
        }
    }

    public BlockHeatExchange(BlockBehaviour.Properties properties, String id) {
        super(properties.noOcclusion(), id);
    }

    @Override
    public boolean rotateBlock(Level world, BlockPos pos, Direction axis) {
        BlockEntity tile = world.getBlockEntity(pos);
        if (tile instanceof TileHeatExchange) {
            return ((TileHeatExchange) tile).rotate();
        }
        return false;
    }

    @Override
    public InteractionResult attemptRotation(Level world, BlockPos pos, BlockState state, Direction sideWrenched) {
        BlockEntity tile = world.getBlockEntity(pos);
        if (tile instanceof TileHeatExchange) {
            return ((TileHeatExchange) tile).rotate() ? InteractionResult.PASS : InteractionResult.FAIL;
        }
        return InteractionResult.FAIL;
    }

    @Override
    public float getExtension(Level world, BlockPos pos, Direction face, BlockState state) {
        return 0;
    }
}
