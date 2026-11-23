/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.factory.container;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.recipebook.PlaceRecipeHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;

import net.minecraft.world.entity.player.Player;

import buildcraft.lib.gui.ContainerBCTile;
import buildcraft.lib.gui.slot.SlotBase;
import buildcraft.lib.gui.slot.SlotDisplay;
import buildcraft.lib.gui.slot.SlotOutput;
import buildcraft.lib.gui.slot.SlotPhantom;

import buildcraft.factory.tile.TileAutoWorkbenchItems;

public class ContainerAutoCraftItems extends ContainerBCTile<TileAutoWorkbenchItems> {

    public final SlotBase[] materialSlots;
    /** The result slot and the 3x3 phantom recipe slots, which the recipe book treats as the crafting grid. */
    public final List<Slot> recipeSlots = new ArrayList<>();

    public ContainerAutoCraftItems(MenuType<?> type, int containerId, Player player, TileAutoWorkbenchItems tile) {
        super(type, containerId, player, tile);

        recipeSlots.add(addSlot(new SlotOutput(tile.invResult, 0, 124, 35)));
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                recipeSlots.add(addSlot(new SlotPhantom(tile.invBlueprint, x + y * 3, 30 + x * 18, 17 + y * 18, false)));
            }
        }
        materialSlots = new SlotBase[9];
        for (int x = 0; x < 9; x++) {
            // hide the filter slots, but still sync them
            addSlot(new SlotPhantom(tile.invMaterialFilter, x, -1000000, -1000000));
            addSlot(materialSlots[x] = new SlotBase(tile.invMaterials, x, 8 + x * 18, 84));
        }
        addSlot(new SlotDisplay(i -> tile.resultClient, 0, 93, 27));

        addFullPlayerInventory(115);
    }

    /** A recipe picked in the recipe book: put one of each ingredient into the phantom recipe slots, as 1.12.2's
     * phantom recipe book did (the workbench crafts from its material slots, so nothing is taken from the player). */
    @Override
    public RecipeBookMenu.PostPlaceAction handlePlacement(boolean useMaxItems, boolean allowDroppingItemsToClear,
        RecipeHolder<?> recipe, ServerLevel level, Inventory inventory) {
        if (!(recipe.value() instanceof CraftingRecipe crafting)) {
            return RecipeBookMenu.PostPlaceAction.NOTHING;
        }
        ItemStack[] grid = new ItemStack[9];
        java.util.Arrays.fill(grid, ItemStack.EMPTY);
        if (crafting instanceof ShapedRecipe shaped) {
            PlaceRecipeHelper.placeRecipe(3, 3, shaped.getWidth(), shaped.getHeight(), shaped.getIngredients(),
                (Optional<Ingredient> ingredient, int index, int x, int y) -> grid[index] = firstItem(ingredient));
        } else {
            List<Ingredient> ingredients = crafting.placementInfo().ingredients();
            for (int i = 0; i < ingredients.size() && i < 9; i++) {
                grid[i] = firstItem(Optional.of(ingredients.get(i)));
            }
        }
        for (int i = 0; i < 9; i++) {
            tile.invBlueprint.setStackInSlot(i, grid[i]);
        }
        broadcastChanges();
        return RecipeBookMenu.PostPlaceAction.NOTHING;
    }

    private static ItemStack firstItem(Optional<Ingredient> ingredient) {
        return ingredient.flatMap(ing -> ing.items().findFirst()).map(item -> new ItemStack(item, 1))
            .orElse(ItemStack.EMPTY);
    }

    /** What the recipe book counts as available: the workbench's materials (the player's inventory is added by the
     * recipe book itself). */
    @Override
    public void fillCraftSlotsStackedContents(StackedItemContents stackedContents) {
        for (int i = 0; i < tile.invMaterials.getSlots(); i++) {
            stackedContents.accountSimpleStack(tile.invMaterials.getStackInSlot(i));
        }
    }
}
