/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.item;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import buildcraft.api.core.ActionResult;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

import buildcraft.lib.item.ItemBC_Neptune;

import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.block.BlockWaterGel;
import buildcraft.factory.block.BlockWaterGel.GelStage;

public class ItemWaterGel extends ItemBC_Neptune {

    public ItemWaterGel(String id) {
        super(id);
        this.maxStackSize = 16;
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Vec3 start = player.getPositionVector().add(0, player.getEyeHeight(), 0);
        Vec3 look = player.getLookVec();
        Vec3 end = start.add(look.scale(7));
        HitResult ray = world.rayTraceBlocks(start, end, true, false, true);

        if (ray == null || ray.getBlockPos() == null) {
            return InteractionResult.FAIL;
        }

        Block b = world.getBlockState(ray.getBlockPos()).getBlock();
        if (b != Blocks.WATER) {
            return InteractionResult.FAIL;
        }

        if (!player.isCreative()) {
            stack.setCount(stack.getCount() - 1);
        }

        // Same as ItemSnowball
        world.playSound(null, player.getX(), player.getY(), player.getZ(),//
                SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL,//
                0.5F, 0.4F / (itemRand.nextFloat() * 0.4F + 0.8F));

        if (!world.isClientSide()) {
            world.setBlockAndUpdate(ray.getBlockPos(), BCFactoryBlocks.waterGel.defaultBlockState().setValue(BlockWaterGel.PROP_STAGE, GelStage.SPREAD_0));
            world.scheduleUpdate(ray.getBlockPos(), BCFactoryBlocks.waterGel, 200);

            // TODO: Snowball stuff

            // EntitySnowball entitysnowball = new EntitySnowball(world, player);
            // entitysnowball.setHeadingFromThrower(player, player.rotationPitch, player.rotationYaw, 0.0F, 1.5F, 1.0F);
            // world.spawnEntityInWorld(entitysnowball);
        }

        // player.addStat(StatList.getObjectUseStats(this));
        return InteractionResult.SUCCESS;
    }

}
