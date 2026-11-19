/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core.item;

import net.minecraft.core.NonNullList;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import buildcraft.api.core.IEngineType;

import buildcraft.lib.engine.BlockEngineBase_BC8;
import buildcraft.lib.item.ItemBlockBCMulti;

/** The item of an engine block: its stacks carry the engine type in their block state. */
public class ItemEngine_BC8<E extends Enum<E> & IEngineType & StringRepresentable> extends ItemBlockBCMulti {
    private final BlockEngineBase_BC8<E> engineBlock;

    public ItemEngine_BC8(BlockEngineBase_BC8<E> block, Item.Properties properties) {
        super(block, properties, stack -> block.getUnlocalizedName(block.getType(stack)));
        engineBlock = block;
    }

    @Override
    public String getDescriptionId(ItemStack stack) {
        return "tile." + engineBlock.getUnlocalizedName(engineBlock.getType(stack)) + ".name";
    }

    @Override
    public void addSubItems(NonNullList<ItemStack> items) {
        engineBlock.addSubStacks(items);
    }
}
