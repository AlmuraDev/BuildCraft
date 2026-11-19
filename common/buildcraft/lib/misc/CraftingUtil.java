/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.misc;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public final class CraftingUtil {

    /**
     * Deactivate constructor
     */
    private CraftingUtil() {
    }

    /** @return The crafting recipe the grid matches, or null (recipes are only known on the server on 26.3). */
    @Nullable
    public static RecipeHolder<CraftingRecipe> findMatchingRecipe(CraftingContainer inventory, Level world) {
        if (!(world instanceof ServerLevel server)) {
            return null;
        }
        return server.recipeAccess().getRecipeFor(RecipeType.CRAFTING, inventory.asCraftInput(), server).orElse(null);
    }
}
