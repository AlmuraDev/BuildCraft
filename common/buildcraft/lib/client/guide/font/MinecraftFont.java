/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.client.guide.font;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;

import buildcraft.lib.gui.GuiDraw;
import buildcraft.lib.misc.GuiUtil;

/** Implements a font that delegates to Minecraft's own {@link Font} */
public enum MinecraftFont implements IFontRenderer {
    INSTANCE;

    private static Font getFontRenderer() {
        return Minecraft.getInstance().font;
    }

    @Override
    public int getStringWidth(String text) {
        return getFontRenderer().width(text);
    }

    @Override
    public int getFontHeight(String text) {
        return getMaxFontHeight();
    }

    @Override
    public int getMaxFontHeight() {
        return getFontRenderer().lineHeight;
    }

    @Override
    public int drawString(String text, int x, int y, int colour, boolean shadow, boolean centered, float scale) {
        boolean _scale = scale != 1;
        if (_scale) {
            GuiDraw.pushPose();
            GuiDraw.scale(scale, scale);
            x = (int) (x / scale);
            y = (int) (y / scale);
        }
        if (centered) {
            x -= getStringWidth(text) / 2;
        }
        GuiDraw.text(getFontRenderer(), text, x, y, colour, shadow);
        int v = getStringWidth(text);
        if (_scale) {
            GuiDraw.popPose();
            v = (int) (v * scale);
        }
        return v;
    }

    @Override
    public List<String> wrapString(String text, int maxWidth, boolean shadow, float scale) {
        return GuiUtil.wrap(getFontRenderer(), text, (int) (maxWidth / scale));
    }
}
