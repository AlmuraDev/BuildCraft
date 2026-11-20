/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.list;

import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;


import buildcraft.api.lists.ListMatchHandler;

/** Matches items by their tags, as 1.12.2 matched them by ore dictionary names. Tags like "c:ingots/iron" give a type
 * ("c:ingots", 1.12.2's "ingot" prefix) and a material ("iron", the "Iron" suffix):
 * <ul>
 * <li>CLASS: the items share a tag.</li>
 * <li>TYPE: the items share a type ("any ingot").</li>
 * <li>MATERIAL: the items share a material ("anything iron").</li>
 * </ul> */
public class ListMatchHandlerOreDictionary extends ListMatchHandler {

    /** @return The tags that describe a kind of item ("c:ingots/iron"), rather than a behaviour. */
    private static Set<TagKey<Item>> namedTags(ItemStack stack) {
        return stack.typeHolder().tags().filter(tag -> tag.location().getPath().indexOf('/') > 0).collect(Collectors.toSet());
    }

    @Nullable
    static String getType(TagKey<Item> tag) {
        String path = tag.location().getPath();
        int slash = path.lastIndexOf('/');
        return slash > 0 ? tag.location().getNamespace() + ":" + path.substring(0, slash) : null;
    }

    @Nullable
    static String getMaterial(TagKey<Item> tag) {
        String path = tag.location().getPath();
        int slash = path.lastIndexOf('/');
        return slash > 0 ? path.substring(slash + 1) : null;
    }

    private static Set<String> keys(Set<TagKey<Item>> tags, Type type) {
        return tags.stream().map(tag -> type == Type.MATERIAL ? getMaterial(tag) : getType(tag)).filter(s -> s != null)
            .collect(Collectors.toSet());
    }

    @Override
    public boolean matches(Type type, @Nonnull ItemStack stack, @Nonnull ItemStack target, boolean precise) {
        Set<TagKey<Item>> tags = namedTags(stack);
        if (tags.isEmpty()) {
            // Unfortunately we cannot compare the items.
            return false;
        }
        Set<TagKey<Item>> targetTags = namedTags(target);

        if (type == Type.CLASS) {
            for (TagKey<Item> tag : tags) {
                if (targetTags.contains(tag)) {
                    return true;
                }
            }
            return false;
        }
        Set<String> wanted = keys(tags, type);
        for (String key : keys(targetTags, type)) {
            if (wanted.contains(key)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isValidSource(Type type, @Nonnull ItemStack stack) {
        return !namedTags(stack).isEmpty();
    }

    @Override
    public NonNullList<ItemStack> getClientExamples(Type type, @Nonnull ItemStack stack) {
        NonNullList<ItemStack> stacks = NonNullList.create();
        Set<TagKey<Item>> tags = namedTags(stack);
        if (tags.isEmpty()) {
            return stacks;
        }
        Set<String> wanted = type == Type.CLASS ? null : keys(tags, type);
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack example = new ItemStack(item);
            Set<TagKey<Item>> exampleTags = namedTags(example);
            boolean matches;
            if (wanted == null) {
                matches = exampleTags.stream().anyMatch(tags::contains);
            } else {
                matches = keys(exampleTags, type).stream().anyMatch(wanted::contains);
            }
            if (matches) {
                stacks.add(example);
            }
        }
        return stacks;
    }
}
