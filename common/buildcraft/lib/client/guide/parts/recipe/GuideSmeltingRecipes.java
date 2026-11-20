/* Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/ */

package buildcraft.lib.client.guide.parts.recipe;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import buildcraft.lib.client.guide.parts.GuidePartFactory;

/** Furnace recipes that use or make a stack (the furnace itself lists all of them, as on 1.12.2). */
public enum GuideSmeltingRecipes implements IStackRecipes {
    INSTANCE;

    @Override
    public List<GuidePartFactory> getUsages(@Nonnull ItemStack stack) {
        boolean all = stack.getItem() == Items.FURNACE;
        List<GuidePartFactory> list = new ArrayList<>();
        for (GuideRecipeAccess.Smelting recipe : GuideRecipeAccess.smelting()) {
            if (all || GuideRecipeAccess.anyMatches(recipe.inputs(), stack)) {
                ItemStack input = all ? recipe.inputs().get(0) : stack;
                list.add(new GuideSmeltingFactory(input, recipe.outputs().get(0)));
            }
        }
        return list;
    }

    @Override
    public List<GuidePartFactory> getRecipes(@Nonnull ItemStack stack) {
        List<GuidePartFactory> list = new ArrayList<>();
        for (GuideRecipeAccess.Smelting recipe : GuideRecipeAccess.smelting()) {
            if (GuideRecipeAccess.anyMatches(recipe.outputs(), stack)) {
                list.add(new GuideSmeltingFactory(recipe.inputs().get(0), recipe.outputs().get(0)));
            }
        }
        return list;
    }
}
