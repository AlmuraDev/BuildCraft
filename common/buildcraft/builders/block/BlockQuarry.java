/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.block;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import buildcraft.api.properties.BuildCraftProperties;

import buildcraft.lib.block.BlockBCTile_Neptune;
import buildcraft.lib.block.IBlockWithFacing;
import buildcraft.lib.misc.AdvancementUtil;
import buildcraft.lib.misc.CapUtil;

/** The quarry. The CONNECTED properties (sides with an inventory to push into) are real state properties now (1.12.2
 * worked them out in getActualState), kept up to date by {@link #updateShape}. The frame is removed by the tile
 * (TileQuarry.onRemove; 1.12.2 did it in breakBlock). */
public class BlockQuarry extends BlockBCTile_Neptune implements IBlockWithFacing {
    private static final Identifier ADVANCEMENT = Identifier.parse("buildcraftbuilders:shaping_the_world");

    public BlockQuarry(BlockBehaviour.Properties properties, String id) {
        super(properties.sound(SoundType.ANVIL), id);
    }

    @Override
    protected void addProperties(List<Property<?>> properties) {
        super.addProperties(properties);
        properties.addAll(BuildCraftProperties.CONNECTED_MAP.values());
    }

    private boolean isConnected(BlockGetter world, BlockPos pos, BlockState state, Direction side) {
        Direction facing = side;
        if (side.getAxis().isHorizontal()) {
            facing = Direction.from2DDataValue(
                side.get2DDataValue() + 2 + state.getValue(getFacingProperty()).get2DDataValue());
        }
        BlockEntity tile = world.getBlockEntity(pos.relative(facing));
        return tile != null && (CapUtil.getCapability(tile, CapUtil.CAP_ITEMS, facing.getOpposite()) != null);
    }

    private BlockState withConnections(BlockState state, BlockGetter world, BlockPos pos) {
        for (Direction face : Direction.values()) {
            state = state.setValue(BuildCraftProperties.CONNECTED_MAP.get(face), isConnected(world, pos, state, face));
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

    @Override
    public boolean canBeRotated(Level world, BlockPos pos, BlockState state) {
        return false;
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
        ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        if (placer instanceof Player) {
            AdvancementUtil.unlockAdvancement((Player) placer, ADVANCEMENT);
        }
    }
}
