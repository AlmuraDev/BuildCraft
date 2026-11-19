/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core.item;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import buildcraft.api.enums.EnumSpring;

import buildcraft.lib.item.ItemBlockBCMulti;

import buildcraft.core.block.BlockSpring;

/** The springs' item: its stacks carry the spring type (water, oil) in their block state. */
public class ItemBlockSpring extends ItemBlockBCMulti {
    private static final String[] NAMES = { "water", "oil" };

    private final BlockSpring spring;

    public ItemBlockSpring(BlockSpring block, Item.Properties properties) {
        super(block, properties, stack -> NAMES[getStateValue(stack, block, BlockSpring.SPRING_TYPE).ordinal()]);
        this.spring = block;
    }

    @Override
    public void addSubItems(NonNullList<ItemStack> items) {
        for (EnumSpring type : EnumSpring.VALUES) {
            items.add(withStateValue(spring, BlockSpring.SPRING_TYPE, type));
        }
    }
}
