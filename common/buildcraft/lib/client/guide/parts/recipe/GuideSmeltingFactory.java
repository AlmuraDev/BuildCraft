/* Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/ */

package buildcraft.lib.client.guide.parts.recipe;

import java.util.Arrays;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import buildcraft.lib.client.guide.GuiGuide;
import buildcraft.lib.client.guide.parts.GuidePartFactory;
import buildcraft.lib.misc.StackUtil;

/** A furnace panel in the guide (see {@link GuideRecipeAccess} for where the recipes come from). */
public class GuideSmeltingFactory implements GuidePartFactory {
    @Nonnull
    private final ItemStack input, output;
    private final int hash;

    public GuideSmeltingFactory(ItemStack input, ItemStack output) {
        this.input = StackUtil.asNonNull(input);
        this.output = StackUtil.asNonNull(output);
        this.hash = Arrays.hashCode(new int[] { BuiltInRegistries.ITEM.getId(this.input.getItem()),
            BuiltInRegistries.ITEM.getId(this.output.getItem()), this.output.getCount() });
    }

    @Nullable
    public static GuideSmeltingFactory create(ItemStack stack) {
        for (GuideRecipeAccess.Smelting recipe : GuideRecipeAccess.smelting()) {
            if (GuideRecipeAccess.anyMatches(recipe.outputs(), stack)) {
                return new GuideSmeltingFactory(recipe.inputs().get(0), recipe.outputs().get(0));
            }
        }
        return null;
    }

    @Nullable
    public static GuideSmeltingFactory create(Item output) {
        return create(new ItemStack(output));
    }

    @Override
    public GuideSmelting createNew(GuiGuide gui) {
        return new GuideSmelting(gui, input, output);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != getClass()) return false;
        GuideSmeltingFactory other = (GuideSmeltingFactory) obj;
        if (hash != other.hash) return false;
        return ItemStack.matches(input, other.input) && ItemStack.matches(output, other.output);
    }
}
