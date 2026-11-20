/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.item;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import buildcraft.lib.item.ItemBC_Neptune;

import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.block.BlockWaterGel;
import buildcraft.factory.block.BlockWaterGel.GelStage;

/** Thrown at still water, starts a water gel spreading through it. */
public class ItemWaterGel extends ItemBC_Neptune {
    public ItemWaterGel(Item.Properties properties, String id) {
        super(properties.stacksTo(16), id);
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult ray = getPlayerPOVHitResult(world, player, ClipContext.Fluid.SOURCE_ONLY);
        if (ray.getType() != HitResult.Type.BLOCK) {
            return InteractionResult.FAIL;
        }
        BlockPos pos = ray.getBlockPos();
        if (world.getBlockState(pos).getBlock() != Blocks.WATER) {
            return InteractionResult.FAIL;
        }

        if (!player.isCreative()) {
            stack.shrink(1);
        }

        // Same as a snowball
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW,
            SoundSource.NEUTRAL, 0.5F, 0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F));

        if (!world.isClientSide() && BCFactoryBlocks.waterGel != null) {
            world.setBlockAndUpdate(pos,
                BCFactoryBlocks.waterGel.defaultBlockState().setValue(BlockWaterGel.PROP_STAGE, GelStage.SPREAD_0));
            world.scheduleTick(pos, BCFactoryBlocks.waterGel, 200);
        }
        return InteractionResult.SUCCESS;
    }
}
