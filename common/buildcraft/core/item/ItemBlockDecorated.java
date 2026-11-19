/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core.item;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import buildcraft.api.enums.EnumDecoratedBlock;

import buildcraft.lib.item.ItemBlockBCMulti;

import buildcraft.core.block.BlockDecoration;

/** The decorated blocks' item: one item, its stacks carrying the decoration type in their block state. */
public class ItemBlockDecorated extends ItemBlockBCMulti {
    private final BlockDecoration decoration;

    public ItemBlockDecorated(BlockDecoration block, Item.Properties properties) {
        super(block, properties, stack -> getStateValue(stack, block, BlockDecoration.DECORATED_TYPE).getName());
        this.decoration = block;
    }

    @Override
    public void addSubItems(NonNullList<ItemStack> items) {
        for (EnumDecoratedBlock type : EnumDecoratedBlock.VALUES) {
            items.add(withStateValue(decoration, BlockDecoration.DECORATED_TYPE, type));
        }
    }
}
