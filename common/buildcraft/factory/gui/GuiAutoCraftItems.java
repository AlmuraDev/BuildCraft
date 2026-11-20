/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.factory.gui;

import buildcraft.lib.gui.GuiDraw;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.resources.Identifier;


import buildcraft.api.core.BCLog;

import buildcraft.lib.gui.GuiBC8;
import buildcraft.lib.gui.GuiIcon;
import buildcraft.lib.gui.ledger.LedgerHelp;
import buildcraft.lib.gui.pos.GuiRectangle;
import buildcraft.lib.gui.slot.SlotBase;
import buildcraft.lib.misc.StackUtil;
import buildcraft.lib.tile.item.ItemHandlerSimple;

import buildcraft.factory.container.ContainerAutoCraftItems;

/** The item auto workbench. TODO(26.3): 1.12.2 had a recipe book here that filled the phantom recipe slots; 26.3's
 * recipe book is a different system, so it is gone for now. */
public class GuiAutoCraftItems extends GuiBC8<ContainerAutoCraftItems> {
    private static final Identifier TEXTURE_BASE =
        Identifier.parse("buildcraftfactory:textures/gui/autobench_item.png");
    private static final Identifier TEXTURE_MISC =
        Identifier.parse("buildcraftlib:textures/gui/misc_slots.png");
    private static final Identifier VANILLA_CRAFTING_TABLE =
        Identifier.parse("textures/gui/container/crafting_table.png");
    private static final int SIZE_X = 176, SIZE_Y = 197;
    private static final GuiIcon ICON_GUI = new GuiIcon(TEXTURE_BASE, 0, 0, SIZE_X, SIZE_Y);
    private static final GuiIcon ICON_FILTER_OVERLAY_SAME = new GuiIcon(TEXTURE_MISC, 54, 0, 18, 18);
    private static final GuiIcon ICON_FILTER_OVERLAY_DIFFERENT = new GuiIcon(TEXTURE_MISC, 72, 0, 18, 18);
    private static final GuiIcon ICON_FILTER_OVERLAY_SIMILAR = new GuiIcon(TEXTURE_MISC, 90, 0, 18, 18);
    private static final GuiIcon ICON_PROGRESS = new GuiIcon(TEXTURE_BASE, SIZE_X, 0, 23, 10);
    private static final GuiRectangle RECT_PROGRESS = new GuiRectangle(90, 47, 23, 10);

    public GuiAutoCraftItems(ContainerAutoCraftItems container) {
        super(container);
        xSize = SIZE_X;
        ySize = SIZE_Y;
        mainGui.shownElements.add(new LedgerHelp(mainGui, true));
    }

    @Override
    protected boolean shouldAddHelpLedger() {
        // Added on the right side in the constructor (1.12.2 kept the left free for the recipe book)
        return false;
    }

    @Override
    protected void drawBackgroundLayer(float partialTicks) {
        ICON_GUI.drawAt(mainGui.rootElement);

        double progress = container.tile.getProgress(partialTicks);

        drawProgress(RECT_PROGRESS, ICON_PROGRESS, progress, 1);

        if (hasFilters()) {
            forEachFilter((slot, filterStack) -> {
                int x = slot.x + (int) mainGui.rootElement.getX();
                int y = slot.y + (int) mainGui.rootElement.getY();
                GuiDraw.item(filterStack, x, y);
                // (item overlays are drawn by GuiDraw.item)
            });

            forEachFilter((slot, filterStack) -> {
                ItemStack real = slot.getItem();
                final GuiIcon icon;
                if (real.isEmpty() || StackUtil.canMerge(real, filterStack)) {
                    icon = ICON_FILTER_OVERLAY_SAME;
                } else {
                    icon = ICON_FILTER_OVERLAY_DIFFERENT;
                }
                int x = slot.x + (int) mainGui.rootElement.getX();
                int y = slot.y + (int) mainGui.rootElement.getY();
                icon.drawAt(x - 1, y - 1);
            });
        }
    }

    private boolean hasFilters() {
        ItemHandlerSimple filters = container.tile.invMaterialFilter;
        for (int s = 0; s < filters.getSlots(); s++) {
            ItemStack filter = filters.getStackInSlot(s);
            if (!filter.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void forEachFilter(IFilterSlotIterator iter) {
        ItemHandlerSimple filters = container.tile.invMaterialFilter;
        for (int s = 0; s < filters.getSlots(); s++) {
            ItemStack filter = filters.getStackInSlot(s);
            if (!filter.isEmpty()) {
                iter.iterate(container.materialSlots[s], filter);
            }
        }
    }

    @FunctionalInterface
    private interface IFilterSlotIterator {
        void iterate(SlotBase drawSlot, ItemStack filterStack);
    }
}
