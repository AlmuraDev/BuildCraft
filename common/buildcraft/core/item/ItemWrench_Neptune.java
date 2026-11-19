/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.core.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Level;

import buildcraft.api.blocks.CustomRotationHelper;
import buildcraft.api.tools.IToolWrench;

import buildcraft.lib.item.ItemBC_Neptune;
import buildcraft.lib.misc.AdvancementUtil;
import buildcraft.lib.misc.SoundUtil;

public class ItemWrench_Neptune extends ItemBC_Neptune implements IToolWrench {
    private static final Identifier ADVANCEMENT = Identifier.parse("buildcraftcore:wrenched");

    public ItemWrench_Neptune(Item.Properties properties, String id) {
        super(properties.stacksTo(1), id);
    }

    @Override
    public boolean canWrench(Player player, InteractionHand hand, ItemStack wrench, HitResult rayTrace) {
        return true;
    }

    @Override
    public void wrenchUsed(Player player, InteractionHand hand, ItemStack wrench, HitResult rayTrace) {
        AdvancementUtil.unlockAdvancement(player, ADVANCEMENT);
        player.swing(hand, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
    }

    @Override
    public boolean doesSneakBypassUse(ItemStack stack, LevelReader world, BlockPos pos, Player player) {
        return false;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        InteractionHand hand = context.getHand();
        Direction side = context.getClickedFace();
        // FIXME: Disabled world check as it doesn't allow us to swing the player's arm!
        // if (world.isClientSide()) {
        // return InteractionResult.PASS;
        // }
        BlockState state = world.getBlockState(pos);
        InteractionResult result = CustomRotationHelper.INSTANCE.attemptRotateBlock(world, pos, state, side);
        if (result == InteractionResult.SUCCESS) {
            wrenchUsed(player, hand, player.getItemInHand(hand), new BlockHitResult(context.getClickLocation(), side, pos, false));
        }
        SoundUtil.playSlideSound(world, pos, state, result);
        return result;
    }
}
