/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.item;

import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import buildcraft.lib.item.ItemBC_Neptune;
import buildcraft.lib.misc.ColourUtil;

import buildcraft.transport.BCTransportItems;

/** Gate wire. The colour is the "buildcrafttransport:colour" component (1.12.2 used the damage value). */
public class ItemWire extends ItemBC_Neptune {
    public ItemWire(Item.Properties properties, String id) {
        super(properties, id);
    }

    /** @return The wire's colour (white if the stack has none). */
    public static DyeColor getColour(ItemStack stack) {
        return stack.getOrDefault(BCTransportItems.COLOUR, DyeColor.WHITE);
    }

    public ItemStack createStack(DyeColor colour, int count) {
        ItemStack stack = new ItemStack(this, count);
        stack.set(BCTransportItems.COLOUR, colour);
        return stack;
    }

    @Override
    public void addSubItems(NonNullList<ItemStack> subItems) {
        for (DyeColor colour : DyeColor.values()) {
            subItems.add(createStack(colour, 1));
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal(ColourUtil.getTextFullTooltip(getColour(stack)) + " ").append(super.getName(stack));
    }
}
