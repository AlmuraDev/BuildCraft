/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.block;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;

import buildcraft.lib.registry.TileTypes;
import buildcraft.lib.tile.ITickable;
import buildcraft.lib.tile.TileBC_Neptune;

/** A BuildCraft block with a tile. The tile's type comes from {@link TileTypes} (registered with
 * {@link buildcraft.lib.registry.RegistrationHelper#registerTile}); tiles that implement {@link ITickable} are ticked on
 * both sides. */
public abstract class BlockBCTile_Neptune extends BlockBCBase_Neptune implements EntityBlock {
    public BlockBCTile_Neptune(BlockBehaviour.Properties properties, String id) {
        super(properties, id);
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return TileTypes.forState(state).create(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return (world, pos, blockState, tile) -> {
            if (tile instanceof ITickable tickable) {
                tickable.update();
            }
        };
    }

    @Override
    public void onBlockExploded(BlockState state, ServerLevel world, BlockPos pos, Explosion explosion) {
        BlockEntity tile = world.getBlockEntity(pos);
        if (tile instanceof TileBC_Neptune) {
            TileBC_Neptune tileBC = (TileBC_Neptune) tile;
            tileBC.onExplode(explosion);
        }
        super.onBlockExploded(state, world, pos, explosion);
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        BlockEntity tile = world.getBlockEntity(pos);
        if (tile instanceof TileBC_Neptune && placer != null) {
            TileBC_Neptune tileBC = (TileBC_Neptune) tile;
            tileBC.onPlacedBy(placer, stack);
        }
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player,
        InteractionHand hand, BlockHitResult hit) {
        BlockEntity tile = world.getBlockEntity(pos);
        if (tile instanceof TileBC_Neptune) {
            TileBC_Neptune tileBC = (TileBC_Neptune) tile;
            BlockPos hitPos = hit.getBlockPos();
            float hitX = (float) (hit.getLocation().x - hitPos.getX());
            float hitY = (float) (hit.getLocation().y - hitPos.getY());
            float hitZ = (float) (hit.getLocation().z - hitPos.getZ());
            if (tileBC.onActivated(player, hand, hit.getDirection(), hitX, hitY, hitZ)) {
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player,
        BlockHitResult hit) {
        InteractionResult result = useItemOn(ItemStack.EMPTY, state, world, pos, player, InteractionHand.MAIN_HAND, hit);
        return result == InteractionResult.TRY_WITH_EMPTY_HAND ? InteractionResult.PASS : result;
    }

    /** 26.3 doesn't say which neighbour changed, so the tile is told with a null position. */
    @Override
    protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, @Nullable Orientation orientation,
        boolean movedByPiston) {
        super.neighborChanged(state, world, pos, block, orientation, movedByPiston);
        BlockEntity tile = world.getBlockEntity(pos);
        if (tile instanceof TileBC_Neptune) {
            TileBC_Neptune tileBC = (TileBC_Neptune) tile;
            tileBC.onNeighbourBlockChanged(block, null);
        }
    }

    /** Called (by NeoForge) when a neighbouring tile changes, which does say which neighbour it was. */
    @Override
    public void onNeighborChange(BlockState state, LevelReader world, BlockPos pos, BlockPos neighbor) {
        BlockEntity tile = world.getBlockEntity(pos);
        if (tile instanceof TileBC_Neptune) {
            TileBC_Neptune tileBC = (TileBC_Neptune) tile;
            tileBC.onNeighbourBlockChanged(world.getBlockState(neighbor).getBlock(), neighbor);
        }
    }
}
