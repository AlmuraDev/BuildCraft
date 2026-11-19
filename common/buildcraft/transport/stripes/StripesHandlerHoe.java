/**
 * Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 * <p/>
 * The BuildCraft API is distributed under the terms of the MIT License. Please check the contents of the license, which
 * should be located as "LICENSE.API" in the BuildCraft source code distribution.
 */
package buildcraft.transport.stripes;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import buildcraft.api.transport.IStripesActivator;
import buildcraft.api.transport.IStripesHandlerItem;

public enum StripesHandlerHoe implements IStripesHandlerItem {
    INSTANCE;

    @Override
    public boolean handle(Level world,
                          BlockPos pos,
                          Direction direction,
                          ItemStack stack,
                          Player player,
                          IStripesActivator activator) {

        if (!stack.is(ItemTags.HOES)) {
            return false;
        }

        pos = pos.relative(direction);
        if (useOn(player, stack, pos, Direction.UP, 0, 0, 0) != InteractionResult.PASS) {
            return true;
        }

        if (direction != Direction.UP && useOn(player, stack, pos.below(), Direction.UP, 0, 0, 0) != InteractionResult.PASS) {
            return true;
        }

        return false;
    }

    /** Uses the stack (held in the stripes pipe's fake player's main hand) on the given face of the block. */
    static InteractionResult useOn(Player player, ItemStack stack, BlockPos pos, Direction face, double hitX, double hitY,
        double hitZ) {
        BlockHitResult hit = new BlockHitResult(new Vec3(pos.getX() + hitX, pos.getY() + hitY, pos.getZ() + hitZ), face,
            pos, false);
        return stack.useOn(new UseOnContext(player.level(), player, InteractionHand.MAIN_HAND, stack, hit));
    }
}
