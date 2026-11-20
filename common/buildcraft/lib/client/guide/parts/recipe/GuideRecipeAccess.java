/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.client.guide.parts.recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.FurnaceRecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;

/** The recipes the guide shows, as resolved stacks.
 * <p>
 * 1.12.2 read every recipe from the client's recipe registry. 26.3 clients aren't sent recipes (only what the recipe
 * book shows), so the guide reads them from the integrated server: crafting and smelting panels appear in single
 * player and on LAN hosts, and are left out on a dedicated server. TODO(26.3): sync the recipes the guide needs. */
public final class GuideRecipeAccess {
    /** A crafting recipe in a 3x3 grid (index x + y * 3), each slot a list of alternatives. */
    public record Crafting(List<List<ItemStack>> grid, List<ItemStack> outputs) {}

    public record Smelting(List<ItemStack> inputs, List<ItemStack> outputs) {}

    private static MinecraftServer cachedFor;
    private static List<Crafting> crafting = Collections.emptyList();
    private static List<Smelting> smelting = Collections.emptyList();

    private GuideRecipeAccess() {}

    /** Forgets the cached recipes (on a guide reload). */
    public static void invalidate() {
        cachedFor = null;
        crafting = Collections.emptyList();
        smelting = Collections.emptyList();
    }

    public static List<Crafting> crafting() {
        refresh();
        return crafting;
    }

    public static List<Smelting> smelting() {
        refresh();
        return smelting;
    }

    private static void refresh() {
        Minecraft mc = Minecraft.getInstance();
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == cachedFor) {
            return;
        }
        cachedFor = server;
        crafting = new ArrayList<>();
        smelting = new ArrayList<>();
        if (server == null || mc.level == null) {
            return;
        }
        ContextMap context = SlotDisplayContext.fromLevel(mc.level);
        for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
            for (RecipeDisplay display : holder.value().display()) {
                add(display, context);
            }
        }
    }

    private static void add(RecipeDisplay display, ContextMap context) {
        if (display instanceof ShapedCraftingRecipeDisplay shaped) {
            List<List<ItemStack>> grid = emptyGrid();
            int offsetX = shaped.width() == 1 ? 1 : 0;
            int offsetY = shaped.height() == 1 ? 1 : 0;
            for (int y = 0; y < shaped.height() && y < 3; y++) {
                for (int x = 0; x < shaped.width() && x < 3; x++) {
                    int i = x + y * shaped.width();
                    if (i < shaped.ingredients().size()) {
                        grid.set(x + offsetX + (y + offsetY) * 3, stacks(shaped.ingredients().get(i), context));
                    }
                }
            }
            addCrafting(grid, stacks(shaped.result(), context));
        } else if (display instanceof ShapelessCraftingRecipeDisplay shapeless) {
            List<List<ItemStack>> grid = emptyGrid();
            for (int i = 0; i < shapeless.ingredients().size() && i < 9; i++) {
                grid.set(i, stacks(shapeless.ingredients().get(i), context));
            }
            addCrafting(grid, stacks(shapeless.result(), context));
        } else if (display instanceof FurnaceRecipeDisplay furnace) {
            List<ItemStack> in = stacks(furnace.ingredient(), context);
            List<ItemStack> out = stacks(furnace.result(), context);
            if (!in.isEmpty() && !out.isEmpty()) {
                smelting.add(new Smelting(in, out));
            }
        }
    }

    private static void addCrafting(List<List<ItemStack>> grid, List<ItemStack> outputs) {
        if (!outputs.isEmpty()) {
            crafting.add(new Crafting(grid, outputs));
        }
    }

    private static List<List<ItemStack>> emptyGrid() {
        List<List<ItemStack>> grid = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) {
            grid.add(Collections.emptyList());
        }
        return grid;
    }

    private static List<ItemStack> stacks(@Nullable SlotDisplay display, ContextMap context) {
        if (display == null) {
            return Collections.emptyList();
        }
        List<ItemStack> list = new ArrayList<>();
        for (ItemStack stack : display.resolveForStacks(context)) {
            if (!stack.isEmpty()) {
                list.add(stack);
            }
        }
        return list;
    }

    /** @return True if any of the stacks is the same item as the target. */
    public static boolean anyMatches(List<ItemStack> stacks, ItemStack target) {
        for (ItemStack stack : stacks) {
            if (ItemStack.isSameItem(stack, target)) {
                return true;
            }
        }
        return false;
    }
}
