/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.inventory.filter;

import java.util.Arrays;
import java.util.Collection;

import javax.annotation.Nonnull;

import net.minecraft.world.item.ItemStack;
import net.minecraft.core.NonNullList;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import buildcraft.lib.misc.OreDictTags;

import buildcraft.api.core.IStackFilter;
import buildcraft.api.recipes.StackDefinition;

import buildcraft.lib.misc.StackUtil;

/** Returns true if the stack matches any one one of the filter stacks. */
public class OreStackFilter implements IStackFilter {

    private final String[] ores;

    public OreStackFilter(String... iOres) {
        ores = iOres;
    }

    @Override
    public boolean matches(@Nonnull ItemStack stack) {
        for (String ore : ores) {
            if (OreDictTags.matches(stack, ore)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public NonNullList<ItemStack> getExamples() {
        NonNullList<ItemStack> examples = NonNullList.create();
        for (String ore : ores) {
            TagKey<Item> tag = OreDictTags.toTag(ore);
            if (tag != null) {
                BuiltInRegistries.ITEM.getTagOrEmpty(tag).forEach(holder -> examples.add(new ItemStack(holder)));
            }
        }
        return examples;
    }

    public static StackDefinition definition(int count, String... ores) {
        return new StackDefinition(new OreStackFilter(ores), count);
    }

    public static StackDefinition definition( String... ores) {
        return definition(1, ores);
    }
}
