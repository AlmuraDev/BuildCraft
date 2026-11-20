/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.recipe;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import buildcraft.api.facades.FacadeAPI;

import buildcraft.lib.misc.StackUtil;
import buildcraft.lib.recipe.ChangingItemStack;
import buildcraft.lib.recipe.IRecipeViewable;

import buildcraft.silicon.BCSiliconItems;
import buildcraft.silicon.item.ItemPluggableFacade;
import buildcraft.silicon.plug.FacadeBlockStateInfo;
import buildcraft.silicon.plug.FacadeInstance;
import buildcraft.silicon.plug.FacadeStateManager;

/** A facade on its own in a crafting grid swaps between solid and hollow. 26.3 recipes come from data, so this is the
 * recipe type "buildcraftsilicon:facade_swap" (data/buildcraftsilicon/recipe/facade_swap.json); 1.12.2 registered the
 * recipe object in code. */
public class FacadeSwapRecipe extends CustomRecipe implements IRecipeViewable.IViewableGrid {
    public static final FacadeSwapRecipe INSTANCE = new FacadeSwapRecipe();
    public static final MapCodec<FacadeSwapRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, FacadeSwapRecipe> STREAM_CODEC =
        StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<FacadeSwapRecipe> SERIALIZER =
        new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private static final int TIME_GAP = 500;

    private static final ChangingItemStack[] INPUTS = { null };
    private static ChangingItemStack OUTPUTS;

    private FacadeSwapRecipe() {}

    public static void genRecipes() {
        if (FacadeAPI.facadeItem == null) {
            throw new IllegalStateException("Don't call FacadeSwapRecipe if the facade item doesn't exist!");
        }
        NonNullList<ItemStack> list1 = NonNullList.create();
        NonNullList<ItemStack> list2 = NonNullList.create();
        for (FacadeBlockStateInfo info : FacadeStateManager.validFacadeStates.values()) {
            if (info.isVisible) {
                ItemStack stack = createFacade(info, false);
                ItemStack stackHollow = createFacade(info, true);
                list1.add(stack);
                list1.add(stackHollow);
                list2.add(stackHollow);
                list2.add(stack);
            }
        }
        if (!list1.isEmpty()) {
            INPUTS[0] = new ChangingItemStack(list1);
            INPUTS[0].setTimeGap(TIME_GAP);
            OUTPUTS = new ChangingItemStack(list2);
            OUTPUTS.setTimeGap(TIME_GAP);
        }
    }

    @Override
    public boolean matches(CraftingInput input, Level world) {
        return !assemble(input).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack stackIn = StackUtil.EMPTY;
        for (int s = 0; s < input.size(); s++) {
            ItemStack stack = input.getItem(s);
            if (!stack.isEmpty()) {
                if (stackIn.isEmpty()) {
                    stackIn = stack;
                } else {
                    return StackUtil.EMPTY;
                }
            }
        }
        if (BCSiliconItems.plugFacade == null || stackIn.getItem() != BCSiliconItems.plugFacade) {
            return StackUtil.EMPTY;
        }
        FacadeInstance states = ItemPluggableFacade.getStates(stackIn);
        states = states.withSwappedIsHollow();
        return BCSiliconItems.plugFacade.createItemStack(states);
    }

    @Override
    public RecipeSerializer<FacadeSwapRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public ChangingItemStack[] getRecipeInputs() {
        if (INPUTS[0] == null) {
            genRecipes();
        }
        return INPUTS;
    }

    @Override
    public ChangingItemStack getRecipeOutputs() {
        if (OUTPUTS == null) {
            genRecipes();
        }
        return OUTPUTS;
    }

    private static ItemStack createFacade(FacadeBlockStateInfo info, boolean isHollow) {
        FacadeInstance state = FacadeInstance.createSingle(info, isHollow);
        return BCSiliconItems.plugFacade.createItemStack(state);
    }

    @Override
    public int getRecipeWidth() {
        return 1;
    }

    @Override
    public int getRecipeHeight() {
        return 1;
    }
}
