/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.misc;

import java.util.List;

import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;

import net.minecraftforge.fml.common.registry.GameRegistry;

public final class CraftingUtil {

    /**
     * Deactivate constructor
     */
    private CraftingUtil() {
    }

    public static Recipe findMatchingRecipe(CraftingContainer par1InventoryCrafting, Level par2World) {
            List<Recipe> recipes = GameRegistry.findRegistry(Recipe.class).getValues();
            for (Recipe recipe : recipes) {
                if (recipe.matches(par1InventoryCrafting, par2World)) {
                    return recipe;
                }
            }
            return null;

    }
}
