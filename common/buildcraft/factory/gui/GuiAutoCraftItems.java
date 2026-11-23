/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.factory.gui;

import buildcraft.lib.gui.GuiDraw;

import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.resources.Identifier;

import buildcraft.lib.gui.GuiBC8;
import buildcraft.lib.gui.GuiIcon;
import buildcraft.lib.gui.ledger.LedgerHelp;
import buildcraft.lib.gui.pos.GuiRectangle;
import buildcraft.lib.gui.slot.SlotBase;
import buildcraft.lib.misc.StackUtil;
import buildcraft.lib.tile.item.ItemHandlerSimple;

import buildcraft.factory.container.ContainerAutoCraftItems;

/** The item auto workbench, with vanilla's recipe book on the left (as in 1.12.2): picking a recipe fills the phantom
 * recipe slots. The recipe book wiring follows vanilla's AbstractRecipeBookScreen, which GuiBC8 can't extend. */
public class GuiAutoCraftItems extends GuiBC8<ContainerAutoCraftItems> implements RecipeUpdateListener {
    private static final Identifier TEXTURE_BASE =
        Identifier.parse("buildcraftfactory:textures/gui/autobench_item.png");
    private static final Identifier TEXTURE_MISC =
        Identifier.parse("buildcraftlib:textures/gui/misc_slots.png");
    private static final int SIZE_X = 176, SIZE_Y = 197;
    private static final GuiIcon ICON_GUI = new GuiIcon(TEXTURE_BASE, 0, 0, SIZE_X, SIZE_Y);
    private static final GuiIcon ICON_FILTER_OVERLAY_SAME = new GuiIcon(TEXTURE_MISC, 54, 0, 18, 18);
    private static final GuiIcon ICON_FILTER_OVERLAY_DIFFERENT = new GuiIcon(TEXTURE_MISC, 72, 0, 18, 18);
    private static final GuiIcon ICON_FILTER_OVERLAY_SIMILAR = new GuiIcon(TEXTURE_MISC, 90, 0, 18, 18);
    private static final GuiIcon ICON_PROGRESS = new GuiIcon(TEXTURE_BASE, SIZE_X, 0, 23, 10);
    private static final GuiRectangle RECT_PROGRESS = new GuiRectangle(90, 47, 23, 10);

    private final AutoCraftRecipeBook recipeBook;
    private boolean widthTooNarrow;
    /** The left edge the recipe book wants (it moves the gui right while open), or -1 before init. */
    private int bookLeftPos = -1;

    public GuiAutoCraftItems(ContainerAutoCraftItems container) {
        super(container);
        xSize = SIZE_X;
        ySize = SIZE_Y;
        mainGui.shownElements.add(new LedgerHelp(mainGui, true));
        recipeBook = new AutoCraftRecipeBook(container);
    }

    // Recipe book

    @Override
    protected void init() {
        super.init();
        widthTooNarrow = width < 379;
        recipeBook.init(width, height, minecraft, widthTooNarrow);
        bookLeftPos = recipeBook.updateScreenPosition(width, xSize);
        leftPos = guiLeft = bookLeftPos;
        addRenderableWidget(new ImageButton(leftPos + 5, topPos + 32, 20, 18, RecipeBookComponent.RECIPE_BUTTON_SPRITES,
            button -> {
                recipeBook.toggleVisibility();
                bookLeftPos = recipeBook.updateScreenPosition(width, xSize);
                leftPos = guiLeft = bookLeftPos;
                button.setPosition(leftPos + 5, topPos + 32);
            }));
        addWidget(recipeBook);
    }

    @Override
    protected int computeLeftPos() {
        return bookLeftPos >= 0 ? bookLeftPos : super.computeLeftPos();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        if (recipeBook.isVisible() && widthTooNarrow) {
            extractBackground(graphics, mouseX, mouseY, a);
        } else {
            extractContents(graphics, mouseX, mouseY, a);
        }
        graphics.nextStratum();
        recipeBook.extractRenderState(graphics, mouseX, mouseY, a);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(
            new net.neoforged.neoforge.client.event.ScreenEvent.Render.Foreground(this, graphics, mouseX, mouseY, a));
        graphics.nextStratum();
        extractCarriedItem(graphics, mouseX, mouseY);
        extractTooltip(graphics, mouseX, mouseY);
        recipeBook.extractTooltip(graphics, mouseX, mouseY, hoveredSlot);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return recipeBook.charTyped(event) || super.charTyped(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return recipeBook.keyPressed(event) || super.keyPressed(event);
    }

    @Override
    public boolean isInputCaptured() {
        return super.isInputCaptured() || recipeBook.capturesInput();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (recipeBook.mouseClicked(event, doubleClick)) {
            setFocused(recipeBook);
            return true;
        }
        return widthTooNarrow && recipeBook.isVisible() || super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        return recipeBook.mouseDragged(event, dx, dy) || super.mouseDragged(event, dx, dy);
    }

    @Override
    protected boolean isHovering(int left, int top, int w, int h, double xm, double ym) {
        return (!widthTooNarrow || !recipeBook.isVisible()) && super.isHovering(left, top, w, h, xm, ym);
    }

    @Override
    protected boolean hasClickedOutside(double mx, double my, int xo, int yo) {
        return super.hasClickedOutside(mx, my, xo, yo)
            && recipeBook.hasClickedOutside(mx, my, leftPos, topPos, xSize, ySize);
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int buttonNum, ContainerInput containerInput) {
        super.slotClicked(slot, slotId, buttonNum, containerInput);
        recipeBook.slotClicked(slot);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        recipeBook.tick();
    }

    @Override
    public void recipesUpdated() {
        recipeBook.recipesUpdated();
    }

    @Override
    public void fillGhostRecipe(RecipeDisplay display) {
        recipeBook.fillGhostRecipe(display);
    }

    @Override
    protected boolean shouldAddHelpLedger() {
        // Added on the right side in the constructor: the left is the recipe book's
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
