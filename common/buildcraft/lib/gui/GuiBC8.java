/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.gui;

import java.util.function.Function;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;


import buildcraft.api.core.render.ISprite;

import buildcraft.lib.gui.json.BuildCraftJsonGui;
import buildcraft.lib.gui.json.InventorySlotHolder;
import buildcraft.lib.gui.ledger.LedgerHelp;
import buildcraft.lib.gui.ledger.LedgerOwnership;
import buildcraft.lib.gui.pos.GuiRectangle;
import buildcraft.lib.gui.pos.IGuiArea;
import buildcraft.lib.misc.GuiUtil;

/** The base of BuildCraft's container screens (future rename: "GuiContainerBuildCraft").
 * <p>
 * 26.3 notes: the size is the mutable {@link #xSize}/{@link #ySize} pair, as in 1.12.2 (vanilla's image size is final
 * and set before subclasses know theirs), and everything is drawn through {@link GuiDraw}: the background layer in
 * {@link #extractBackground}, the foreground layer after the slots in {@link #extractContents}. */
public abstract class GuiBC8<C extends ContainerBC_Neptune> extends AbstractContainerScreen<C> {
    public final BuildCraftGui mainGui;
    public final C container;

    /** The gui size, as 1.12.2's xSize/ySize (subclasses set these in their constructors). */
    public int xSize = 176, ySize = 166;
    /** 1.12.2's guiLeft/guiTop, kept equal to leftPos/topPos. */
    public int guiLeft, guiTop;
    public final Minecraft mc = Minecraft.getInstance();
    public final Font fontRenderer = Minecraft.getInstance().font;

    public GuiBC8(C container) {
        this(container, g -> new BuildCraftGui(g, BuildCraftGui.createWindowedArea(g)));
    }

    public GuiBC8(C container, Function<GuiBC8<?>, BuildCraftGui> constructor) {
        super(container, container.player.getInventory(), Component.empty());
        this.container = container;
        this.mainGui = constructor.apply(this);
        standardLedgerInit();
    }

    public GuiBC8(C container, Identifier jsonGuiDef) {
        super(container, container.player.getInventory(), Component.empty());
        this.container = container;
        BuildCraftJsonGui jsonGui = new BuildCraftJsonGui(this, BuildCraftGui.createWindowedArea(this), jsonGuiDef);
        jsonGui.properties.put("player.inventory", new InventorySlotHolder(container, container.player.getInventory()));
        this.mainGui = jsonGui;
        standardLedgerInit();
        // Force subclasses to set this themselves after calling jsonGui.load
        xSize = 10;
        ySize = 10;
    }

    private void standardLedgerInit() {
        if (container instanceof ContainerBCTile<?>) {
            mainGui.shownElements.add(new LedgerOwnership(mainGui, ((ContainerBCTile<?>) container).tile, true));
        }
        if (shouldAddHelpLedger()) {
            mainGui.shownElements.add(new LedgerHelp(mainGui, false));
        }
    }

    protected boolean shouldAddHelpLedger() {
        return true;
    }

    // Size and position

    @Override
    protected void init() {
        super.init();
        updatePosition();
    }

    private void updatePosition() {
        leftPos = computeLeftPos();
        topPos = (height - ySize) / 2;
        guiLeft = leftPos;
        guiTop = topPos;
    }

    /** Where the gui's left edge goes: centred, unless a subclass moves it (the auto workbench's recipe book). */
    protected int computeLeftPos() {
        return (width - xSize) / 2;
    }

    public int getGuiLeft() {
        return leftPos;
    }

    public int getGuiTop() {
        return topPos;
    }

    public int getXSize() {
        return xSize;
    }

    public int getYSize() {
        return ySize;
    }

    @Override
    public int getImageWidth() {
        return xSize;
    }

    @Override
    public int getImageHeight() {
        return ySize;
    }

    @Override
    protected boolean hasClickedOutside(double mx, double my, int xo, int yo) {
        return mx < xo || my < yo || mx >= xo + xSize || my >= yo + ySize;
    }

    public Font getFontRenderer() {
        return fontRenderer;
    }

    // Drawing

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        updatePosition();
        GuiDraw.begin(graphics);
        try {
            mainGui.drawBackgroundLayer(partialTicks, mouseX, mouseY,
                () -> super.extractBackground(graphics, mouseX, mouseY, partialTicks));
            drawBackgroundLayer(partialTicks);
            mainGui.drawElementBackgrounds();
        } finally {
            GuiDraw.end();
        }
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        super.extractContents(graphics, mouseX, mouseY, partialTicks);
        GuiDraw.begin(graphics);
        try {
            graphics.nextStratum();
            graphics.pose().pushMatrix();
            graphics.pose().translate(leftPos, topPos);
            mainGui.preDrawForeground();
            drawForegroundLayer();
            mainGui.drawElementForegrounds(() -> GuiDraw.gradient(0, 0, width, height, 0xC0_10_10_10, 0xD0_10_10_10));
            drawForegroundLayerAboveElements();
            mainGui.postDrawForeground();
            graphics.pose().popMatrix();
        } finally {
            GuiDraw.end();
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        // BuildCraft's guis draw their own titles in drawForegroundLayer
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (mainGui.currentMenu == null || !mainGui.currentMenu.shouldFullyOverride()) {
            super.extractTooltip(graphics, mouseX, mouseY);
        }
    }

    // Protected -> Public

    public void drawGradientRect(IGuiArea area, int startColor, int endColor) {
        drawGradientRect((int) area.getX(), (int) area.getY(), (int) area.getEndX(), (int) area.getEndY(), startColor,
            endColor);
    }

    public void drawGradientRect(int left, int top, int right, int bottom, int startColor, int endColor) {
        GuiDraw.gradient(left, top, right, bottom, startColor, endColor);
    }

    // Gui -- double -> int

    public void drawTexturedModalRect(double posX, double posY, double textureX, double textureY, double width,
        double height) {
        GuiDraw.texturedRect(posX, posY, textureX, textureY, width, height);
    }

    public void drawString(Font fontRenderer, String text, double x, double y, int colour) {
        drawString(fontRenderer, text, x, y, colour, true);
    }

    public void drawString(Font fontRenderer, String text, double x, double y, int colour, boolean shadow) {
        GuiDraw.text(fontRenderer, text, x, y, colour, shadow);
    }

    /** @deprecated Use {@link GuiUtil#drawItemStackAt(ItemStack,int,int)} instead */
    @Deprecated
    public static void drawItemStackAt(ItemStack stack, int x, int y) {
        GuiUtil.drawItemStackAt(stack, x, y);
    }

    public void drawProgress(GuiRectangle rect, GuiIcon icon, double widthPercent, double heightPercent) {
        double nWidth = rect.width * Math.abs(widthPercent);
        double nHeight = rect.height * Math.abs(heightPercent);
        ISprite sprite = GuiUtil.subRelative(icon.sprite, 0, 0, widthPercent, heightPercent);
        double x = rect.x + mainGui.rootElement.getX();
        double y = rect.y + mainGui.rootElement.getY();
        GuiIcon.draw(sprite, x, y, x + nWidth, y + nHeight);
    }

    // Input

    @Override
    protected void containerTick() {
        super.containerTick();
        mainGui.tick();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        boolean handled = super.mouseClicked(event, doubleClick);
        mouseClicked((int) event.x(), (int) event.y(), event.button());
        return handled;
    }

    /** 1.12.2's click hook, called after vanilla's slot handling. Subclasses override it and call super. */
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        mainGui.onMouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        boolean handled = super.mouseDragged(event, dx, dy);
        mouseClickMove((int) event.x(), (int) event.y(), event.button(), 0);
        return handled;
    }

    /** 1.12.2's drag hook. Subclasses override it and call super. */
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        mainGui.onMouseDragged(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        boolean handled = super.mouseReleased(event);
        mouseReleased((int) event.x(), (int) event.y(), event.button());
        return handled;
    }

    /** 1.12.2's release hook. Subclasses override it and call super. */
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        mainGui.onMouseReleased(mouseX, mouseY, state);
    }

    /** Set by {@link #keyTyped(char, int)} when nothing used the key, so vanilla's handling (closing, hotbar keys) runs. */
    private boolean passKeyToVanilla;

    @Override
    public boolean keyPressed(KeyEvent event) {
        passKeyToVanilla = false;
        keyTyped((char) 0, event.key());
        return !passKeyToVanilla || super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (!Character.isBmpCodePoint(event.codepoint())) {
            return super.charTyped(event);
        }
        passKeyToVanilla = false;
        keyTyped((char) event.codepoint(), 0);
        return !passKeyToVanilla || super.charTyped(event);
    }

    /** 1.12.2's key hook: a typed character (keyCode 0) or a pressed key (typedChar 0, a GLFW key). Subclasses
     * override it and call super for keys they don't use. */
    protected void keyTyped(char typedChar, int keyCode) {
        if (!mainGui.onKeyTyped(typedChar, keyCode)) {
            passKeyToVanilla = true;
        }
    }

    protected void drawBackgroundLayer(float partialTicks) {}

    protected void drawForegroundLayer() {}

    /** Like {@link #drawForegroundLayer()}, but is called after all {@link IGuiElement}'s have been drawn. */
    protected void drawForegroundLayerAboveElements() {}
}
