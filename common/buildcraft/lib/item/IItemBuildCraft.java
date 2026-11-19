/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** A BuildCraft item. Its name, registry name and creative tab come from its {@link buildcraft.lib.registry.TagManager}
 * tag (set up by {@link buildcraft.lib.registry.RegistrationHelper}); its models from its client item definition in the
 * resources (1.12.2 registered them in code). */
public interface IItemBuildCraft {
    String id();

    /** Adds the stacks this item shows in its creative tab (1.12.2's getSubItems). */
    default void addSubItems(NonNullList<ItemStack> items) {
        items.add(new ItemStack((Item) this));
    }

    /** Adds tooltip lines, as 1.12.2's addInformation did. Lines may contain formatting codes. */
    default void addInformation(ItemStack stack, @Nullable Level world, List<String> tooltip, TooltipFlag flags) {}

    /** Turns {@link #addInformation} lines into 26.3 tooltip components. */
    static void appendTooltip(IItemBuildCraft item, ItemStack stack, Item.TooltipContext context, Consumer<Component> builder,
        TooltipFlag flags) {
        List<String> lines = new ArrayList<>();
        item.addInformation(stack, context.level(), lines, flags);
        for (String line : lines) {
            builder.accept(Component.literal(line));
        }
    }
}
