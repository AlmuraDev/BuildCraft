/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.inventory.filter;

import javax.annotation.Nonnull;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

import buildcraft.api.core.IStackFilter;

/** This interface is used with several of the functions in IItemTransfer to provide a convenient means of dealing with
 * entire classes of items without having to specify each item individually. */
public enum StackFilter implements IStackFilter {

    ALL {
        @Override
        public boolean matches(@Nonnull ItemStack stack) {
            return true;
        }
    },
    FUEL {
        @Override
        public boolean matches(@Nonnull ItemStack stack) {
            // Fuels are items with the cooking fuel component on 26.3
            return stack.has(net.minecraft.core.component.DataComponents.COOKING_FUEL);
        }
    };

    @Override
    public abstract boolean matches(@Nonnull ItemStack stack);
}
