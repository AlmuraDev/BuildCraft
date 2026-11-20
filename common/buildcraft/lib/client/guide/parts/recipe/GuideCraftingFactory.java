/* Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/ */

package buildcraft.lib.client.guide.parts.recipe;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import buildcraft.lib.client.guide.GuiGuide;
import buildcraft.lib.client.guide.parts.GuidePartFactory;
import buildcraft.lib.recipe.ChangingItemStack;

/** A crafting grid panel in the guide (see {@link GuideRecipeAccess} for where the recipes come from). */
public class GuideCraftingFactory implements GuidePartFactory {

    private final GuideRecipeAccess.Crafting recipe;
    private final int hash;

    public GuideCraftingFactory(GuideRecipeAccess.Crafting recipe) {
        this.recipe = recipe;
        int h = hashStacks(recipe.outputs());
        for (List<ItemStack> slot : recipe.grid()) {
            h = h * 31 + hashStacks(slot);
        }
        this.hash = h;
    }

    private static int hashStacks(List<ItemStack> stacks) {
        int h = 1;
        for (ItemStack stack : stacks) {
            h = h * 31 + BuiltInRegistries.ITEM.getId(stack.getItem());
            h = h * 31 + stack.getCount();
        }
        return h;
    }

    @Nullable
    public static GuidePartFactory create(@Nonnull ItemStack stack) {
        for (GuideRecipeAccess.Crafting recipe : GuideRecipeAccess.crafting()) {
            if (GuideRecipeAccess.anyMatches(recipe.outputs(), stack)) {
                return new GuideCraftingFactory(recipe);
            }
        }
        return null;
    }

    @Nullable
    public static GuidePartFactory create(Item output) {
        return create(new ItemStack(output));
    }

    private static ChangingItemStack changing(List<ItemStack> stacks) {
        NonNullList<ItemStack> list = NonNullList.create();
        list.addAll(stacks);
        return new ChangingItemStack(list);
    }

    @Override
    public GuideCrafting createNew(GuiGuide gui) {
        ChangingItemStack[][] input = new ChangingItemStack[3][3];
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                input[x][y] = changing(recipe.grid().get(x + y * 3));
            }
        }
        return new GuideCrafting(gui, input, changing(recipe.outputs()));
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != getClass()) return false;
        GuideCraftingFactory other = (GuideCraftingFactory) obj;
        if (hash != other.hash) return false;
        return sameStacks(recipe.outputs(), other.recipe.outputs()) && sameGrid(other);
    }

    private boolean sameGrid(GuideCraftingFactory other) {
        for (int i = 0; i < 9; i++) {
            if (!sameStacks(recipe.grid().get(i), other.recipe.grid().get(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean sameStacks(List<ItemStack> a, List<ItemStack> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            if (!ItemStack.matches(a.get(i), b.get(i))) return false;
        }
        return true;
    }
}
