/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.stripes;

import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.DispenserBlock;

import buildcraft.api.core.BCLog;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import buildcraft.api.transport.IStripesActivator;
import buildcraft.api.transport.IStripesHandlerItem;

public enum StripesHandlerDispenser implements IStripesHandlerItem {
    INSTANCE;

    public static final List<Item> ITEMS = new ArrayList<>();
    public static final List<Class<? extends Item>> ITEM_CLASSES = new ArrayList<>();

    /** A dispenser facing the stripes pipe's direction, at the pipe. There is no dispenser block entity behind it, so
     * behaviours that put things back into the dispenser can't run (1.12.2 gave them the pipe's tile instead). */
    public static BlockSource createSource(ServerLevel world, BlockPos pos, Direction side) {
        return new BlockSource(world, pos, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, side), null);
    }

    private static boolean shouldHandle(ItemStack stack) {
        if (ITEMS.contains(stack.getItem())) {
            return true;
        }

        Class<?> c = stack.getItem().getClass();
        while (c != Item.class) {
            if (ITEMS.contains(c)) {
                return true;
            }
            c = c.getSuperclass();
        }
        return false;
    }

    @Override
    public boolean handle(Level world,
                          BlockPos pos,
                          Direction direction,
                          ItemStack stack,
                          Player player,
                          IStripesActivator activator) {
        DispenseItemBehavior behaviour = DispenserBlock.DISPENSER_REGISTRY.get(stack.getItem());
        if (behaviour == null || !(world instanceof ServerLevel server)) {
            return false;
        }

        BlockSource source = createSource(server, pos, direction);
        ItemStack output;
        try {
            output = behaviour.dispense(source, stack.copy());
        } catch (RuntimeException e) {
            // Mostly behaviours that wanted the dispenser block entity
            BCLog.logger.debug("[transport.stripes] Couldn't dispense " + stack + " from a stripes pipe", e);
            return false;
        }
        player.getInventory().setItem(player.getInventory().getSelectedSlot(), output);
        return true;
    }
}
