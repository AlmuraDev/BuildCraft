/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.item;

import java.util.function.Consumer;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import buildcraft.lib.registry.TagManager;

public class ItemBC_Neptune extends Item implements IItemBuildCraft {
    /** The tag used to identify this in the {@link TagManager} */
    public final String id;

    /** @param properties The item's properties, already carrying its id (see
     *            {@link buildcraft.lib.registry.RegistrationHelper}). */
    public ItemBC_Neptune(Item.Properties properties, String id) {
        super(properties);
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder,
        TooltipFlag flags) {
        super.appendHoverText(stack, context, display, builder, flags);
        IItemBuildCraft.appendTooltip(this, stack, context, builder, flags);
    }
}
