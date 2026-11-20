/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.gui;

import java.io.IOException;

import buildcraft.lib.gui.GuiTextFieldBC;
import net.minecraft.resources.Identifier;

import buildcraft.lib.gui.GuiBC8;
import buildcraft.lib.gui.GuiIcon;

import buildcraft.builders.BCBuildersItems;
import buildcraft.builders.container.ContainerReplacer;
import buildcraft.builders.snapshot.ClientSnapshots;

public class GuiReplacer extends GuiBC8<ContainerReplacer> {
    private static final Identifier TEXTURE_BASE = Identifier.parse("buildcraftbuilders:textures/gui/replacer.png");
    private static final int SIZE_X = 176, SIZE_Y = 241;
    private static final GuiIcon ICON_GUI = new GuiIcon(TEXTURE_BASE, 0, 0, SIZE_X, SIZE_Y);

    private GuiTextFieldBC nameField;

    public GuiReplacer(ContainerReplacer container) {
        super(container);
        xSize = SIZE_X;
        ySize = SIZE_Y;
    }

    @Override
    protected void init() {
        super.init();
        nameField = new GuiTextFieldBC(0, fontRenderer, guiLeft + 30, guiTop + 117, 138, 12);
    }

    @Override
    protected void drawBackgroundLayer(float partialTicks) {
        ICON_GUI.drawAt(mainGui.rootElement);
        // TODO(26.3): 1.12.2 drew a preview of the snapshot here (ClientSnapshots.renderSnapshot, a fake world)
    }

    @Override
    protected void drawForegroundLayer() {
        nameField.drawTextBox();
    }

    @Override
    protected void containerTick() {
        nameField.updateCursorCounter();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        boolean typed = false;
        if (nameField.isFocused()) {
            typed = nameField.textboxKeyTyped(typedChar, keyCode);
            // container.sendNameToServer(nameField.getText().trim());
        }
        if (!typed) {
            super.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        nameField.mouseClicked(mouseX, mouseY, mouseButton);
    }
}
