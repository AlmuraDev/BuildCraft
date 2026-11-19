/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.list;

import java.util.Set;

import javax.annotation.Nonnull;

import net.minecraft.world.item.ItemStack;

import buildcraft.api.lists.ListMatchHandler;

public class ListMatchHandlerTools extends ListMatchHandler {
    /** 1.12.2's tool classes, which are item tags on 26.3. */
    private static final java.util.Map<String, net.minecraft.tags.TagKey<net.minecraft.world.item.Item>> TOOL_TAGS = java.util.Map.of(
        "pickaxe", net.minecraft.tags.ItemTags.PICKAXES,
        "axe", net.minecraft.tags.ItemTags.AXES,
        "shovel", net.minecraft.tags.ItemTags.SHOVELS,
        "hoe", net.minecraft.tags.ItemTags.HOES,
        "sword", net.minecraft.tags.ItemTags.SWORDS);

    private static Set<String> getToolClasses(ItemStack stack) {
        Set<String> classes = new java.util.HashSet<>();
        for (java.util.Map.Entry<String, net.minecraft.tags.TagKey<net.minecraft.world.item.Item>> entry : TOOL_TAGS.entrySet()) {
            if (stack.is(entry.getValue())) {
                classes.add(entry.getKey());
            }
        }
        return classes;
    }

    @Override
    public boolean matches(Type type, @Nonnull ItemStack stack, @Nonnull ItemStack target, boolean precise) {
        if (type == Type.TYPE) {
            Set<String> toolClassesSource = getToolClasses(stack);
            Set<String> toolClassesTarget = getToolClasses(target);
            if (toolClassesSource.size() > 0 && toolClassesTarget.size() > 0) {
                if (precise) {
                    if (toolClassesSource.size() != toolClassesTarget.size()) {
                        return false;
                    }
                }
                for (String s : toolClassesSource) {
                    if (!toolClassesTarget.contains(s)) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isValidSource(Type type, @Nonnull ItemStack stack) {
        return getToolClasses(stack).size() > 0;
    }
}
