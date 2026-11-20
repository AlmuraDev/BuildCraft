/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;


/** 1.12.2's GuiTextField API on top of 26.3's {@link EditBox}, so BuildCraft's screens can keep driving their text
 * fields by hand (typed keys, clicks and drawing in the foreground layer). */
public class GuiTextFieldBC extends EditBox {
    public GuiTextFieldBC(int id, Font font, int x, int y, int width, int height) {
        super(font, x, y, width, height, Component.empty());
    }

    public void setMaxStringLength(int length) {
        setMaxLength(length);
    }

    public void setText(String text) {
        setValue(text);
    }

    public String getText() {
        return getValue();
    }

    /** @param typedChar The typed character, or 0 for a key press.
     * @param keyCode The (GLFW) key, for key presses.
     * @return True if the field used the key. */
    public boolean textboxKeyTyped(char typedChar, int keyCode) {
        if (!isFocused()) {
            return false;
        }
        if (typedChar != 0) {
            return charTyped(new CharacterEvent(typedChar));
        }
        return keyPressed(new KeyEvent(keyCode, 0, 0));
    }

    /** Focuses the field if the click was inside it (and unfocuses it otherwise). */
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        boolean inside = isMouseOver(mouseX, mouseY);
        setFocused(inside);
        return inside;
    }

    /** 1.12.2 blinked the cursor from the screen's tick; 26.3's edit box times it itself. */
    public void updateCursorCounter() {}

    public void drawTextBox() {
        GuiGraphicsExtractor graphics = GuiDraw.graphics();
        if (graphics != null) {
            Minecraft mc = Minecraft.getInstance();
            double mx = mc.mouseHandler.getScaledXPos(mc.getWindow());
            double my = mc.mouseHandler.getScaledYPos(mc.getWindow());
            extractRenderState(graphics, (int) mx, (int) my, 0);
        }
    }
}
