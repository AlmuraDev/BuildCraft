/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.silicon.gui;

import buildcraft.lib.gui.GuiDraw;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.resources.language.I18n;
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
import buildcraft.lib.registry.TagManager;

import buildcraft.silicon.container.ContainerAdvancedCraftingTable;

/** TODO(26.3): 1.12.2 had a recipe book here that filled the phantom recipe slots; 26.3's
 * recipe book is a different system, so it is gone for now. */
public class GuiAdvancedCraftingTable extends GuiBC8<ContainerAdvancedCraftingTable> {
    private static final Identifier TEXTURE_BASE = Identifier.parse("buildcraftsilicon:textures/gui/advanced_crafting_table.png");
    private static final Identifier VANILLA_CRAFTING_TABLE = Identifier.parse("textures/gui/container/crafting_table.png");
    private static final int SIZE_X = 176, SIZE_Y = 241;
    private static final GuiIcon ICON_GUI = new GuiIcon(TEXTURE_BASE, 0, 0, SIZE_X, SIZE_Y);
    private static final GuiIcon ICON_PROGRESS = new GuiIcon(TEXTURE_BASE, SIZE_X, 0, 4, 70);
    private static final GuiRectangle RECT_PROGRESS = new GuiRectangle(164, 7, 4, 70);

    public GuiAdvancedCraftingTable(ContainerAdvancedCraftingTable container) {
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

        long target = container.tile.getTarget();
        if (target != 0) {
            double v = (double) container.tile.power / target;
            ICON_PROGRESS.drawCutInside(
                    new GuiRectangle(
                            RECT_PROGRESS.x,
                            (int) (RECT_PROGRESS.y + RECT_PROGRESS.height * Math.max(1 - v, 0)),
                            RECT_PROGRESS.width,
                            (int) Math.ceil(RECT_PROGRESS.height * Math.min(v, 1))
                    ).offset(mainGui.rootElement)
            );
        }
    }

    @Override
    protected void drawForegroundLayer() {
        String title = I18n.get("tile." + TagManager.getTag("block.advanced_crafting_table", TagManager.EnumTagType.UNLOCALIZED_NAME) + ".name");
        GuiDraw.text(fontRenderer, title, guiLeft + (xSize - fontRenderer.width(title)) / 2, guiTop + 5, 0x404040, false);
    }
}
