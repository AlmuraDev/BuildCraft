/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.item;

import java.util.function.Function;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;

import buildcraft.lib.block.BlockBCBase_Neptune;

/** A block item whose stacks are variants of their block (set by the block state component on 26.3; 1.12.2 used the item
 * damage), each with its own name: "tile.&lt;block&gt;.&lt;variant&gt;.name". */
public class ItemBlockBCMulti extends ItemBlockBC_Neptune {
    protected final Function<ItemStack, String> nameFunction;

    public ItemBlockBCMulti(BlockBCBase_Neptune block, Item.Properties properties, Function<ItemStack, String> nameFunction) {
        super(block, properties);
        this.nameFunction = nameFunction;
    }

    /** @return The translation key of the given stack's variant. */
    public String getDescriptionId(ItemStack stack) {
        String base = getDescriptionId();
        if (base.endsWith(".name")) {
            base = base.substring(0, base.length() - ".name".length());
        }
        return base + "." + this.nameFunction.apply(stack) + ".name";
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(getDescriptionId(stack));
    }

    /** @return The value of the given property in the state a stack of this block places. */
    public static <T extends Comparable<T>> T getStateValue(ItemStack stack, Block block, Property<T> property) {
        BlockItemStateProperties props = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY);
        return props.apply(block.defaultBlockState()).getValue(property);
    }

    /** @return A stack of the given block that places it with the given property value. */
    public static <T extends Comparable<T>> ItemStack withStateValue(Block block, Property<T> property, T value) {
        ItemStack stack = new ItemStack(block);
        stack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(property, value));
        return stack;
    }
}
