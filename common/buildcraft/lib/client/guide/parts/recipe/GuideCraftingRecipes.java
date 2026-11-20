/* Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/ */

package buildcraft.lib.client.guide.parts.recipe;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.world.item.ItemStack;

import buildcraft.lib.client.guide.parts.GuidePartFactory;

/** Crafting recipes that use or make a stack. */
public enum GuideCraftingRecipes implements IStackRecipes {
    INSTANCE;

    @Override
    public List<GuidePartFactory> getUsages(@Nonnull ItemStack target) {
        List<GuidePartFactory> list = new ArrayList<>();
        for (GuideRecipeAccess.Crafting recipe : GuideRecipeAccess.crafting()) {
            for (List<ItemStack> slot : recipe.grid()) {
                if (GuideRecipeAccess.anyMatches(slot, target)) {
                    list.add(new GuideCraftingFactory(recipe));
                    break;
                }
            }
        }
        return list;
    }

    @Override
    public List<GuidePartFactory> getRecipes(@Nonnull ItemStack target) {
        List<GuidePartFactory> list = new ArrayList<>();
        for (GuideRecipeAccess.Crafting recipe : GuideRecipeAccess.crafting()) {
            if (GuideRecipeAccess.anyMatches(recipe.outputs(), target)) {
                list.add(new GuideCraftingFactory(recipe));
            }
        }
        return list;
    }

    /** Called when the guide reloads: the recipes are read again the next time they're needed. */
    public void generateIndices() {
        GuideRecipeAccess.invalidate();
    }
}
