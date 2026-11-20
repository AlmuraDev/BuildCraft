/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.gui;

import java.util.function.Function;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import buildcraft.lib.gui.json.BuildCraftJsonGui;
import buildcraft.lib.gui.ledger.LedgerHelp;
import buildcraft.lib.gui.pos.IGuiArea;
import buildcraft.lib.misc.GuiUtil;

/** Reference implementation for a gui that delegates to a {@link BuildCraftGui} for most of its functionality. */
@OnlyIn(Dist.CLIENT)
public class GuiScreenBuildCraft extends Screen {

    public final BuildCraftGui mainGui;

    /** Creates a new {@link GuiScreenBuildCraft} that will occupy the entire screen. */
    public GuiScreenBuildCraft() {
        this(g -> new BuildCraftGui(g));
    }

    /** Creates a new {@link GuiScreenBuildCraft} that will occupy the given {@link IGuiArea} Call
     * {@link GuiUtil#moveAreaToCentre(IGuiArea)} if you want a centred gui. (Ignoring ledgers, which will display off
     * to the side) */
    public GuiScreenBuildCraft(IGuiArea area) {
        this(g -> new BuildCraftGui(g, area));
    }

    public GuiScreenBuildCraft(Function<GuiScreenBuildCraft, BuildCraftGui> constructor) {
        super(Component.empty());
        this.mainGui = constructor.apply(this);
        standardLedgerInit();
    }

    /** Creates a new gui that will load its elements from the given json resource. */
    public GuiScreenBuildCraft(Identifier jsonGuiDef) {
        super(Component.empty());
        this.mainGui = new BuildCraftJsonGui(this, jsonGuiDef);
        standardLedgerInit();
    }

    /** Creates a new gui that will load its elements from the given json resource. Like
     * {@link #GuiScreenBuildCraft(IGuiArea)} this will occupy only the given {@link IGuiArea} */
    public GuiScreenBuildCraft(Identifier jsonGuiDef, IGuiArea area) {
        super(Component.empty());
        this.mainGui = new BuildCraftJsonGui(this, area, jsonGuiDef);
        standardLedgerInit();
    }

    private void standardLedgerInit() {
        if (shouldAddHelpLedger()) {
            mainGui.shownElements.add(new LedgerHelp(mainGui, false));
        }
    }

    protected boolean shouldAddHelpLedger() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        mainGui.tick();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        GuiDraw.begin(graphics);
        try {
            mainGui.drawBackgroundLayer(partialTicks, mouseX, mouseY, () -> {});
            mainGui.drawElementBackgrounds();
            super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
            graphics.nextStratum();
            mainGui.drawElementForegrounds(() -> extractTransparentBackground(graphics));
        } finally {
            GuiDraw.end();
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (mainGui.onMouseClicked((int) event.x(), (int) event.y(), event.button())) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        boolean handled = super.mouseReleased(event);
        mainGui.onMouseReleased((int) event.x(), (int) event.y(), event.button());
        return handled;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        boolean handled = super.mouseDragged(event, dx, dy);
        mainGui.onMouseDragged((int) event.x(), (int) event.y(), event.button(), 0);
        return handled;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (mainGui.onKeyTyped((char) 0, event.key())) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (Character.isBmpCodePoint(event.codepoint()) && mainGui.onKeyTyped((char) event.codepoint(), 0)) {
            return true;
        }
        return super.charTyped(event);
    }
}
