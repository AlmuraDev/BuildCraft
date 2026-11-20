/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.misc;

import net.minecraft.client.Minecraft;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import buildcraft.lib.client.model.ModelUtil;
import buildcraft.lib.gui.GuiDraw;

/** The parts of 1.12.2's RenderUtil the GUIs still use. The tessellator pool and Forge's batch buffer are gone: 26.3
 * draws GUIs through {@link GuiDraw} and the world through render states. */
@OnlyIn(Dist.CLIENT)
public class RenderUtil {
    /** Takes _RGB (alpha is set to 1): the colour following GUI quads are tinted with. */
    public static void setGLColorFromInt(int color) {
        GuiDraw.colour(0xFF_00_00_00 | (color & 0xFF_FF_FF));
    }

    /** Takes ARGB */
    public static void setGLColorFromIntPlusAlpha(int color) {
        GuiDraw.colour(color);
    }

    public static int swapARGBforABGR(int argb) {
        return ModelUtil.swapARGBforABGR(argb);
    }

    /** @return true if this thread is the main minecraft thread. */
    public static boolean isMainRenderThread() {
        return Minecraft.getInstance().isSameThread();
    }
}
