/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.gui;

import net.minecraft.resources.Identifier;


import buildcraft.api.core.render.ISprite;

import buildcraft.lib.client.sprite.SpriteRaw;
import buildcraft.lib.gui.pos.GuiRectangle;
import buildcraft.lib.gui.pos.IGuiArea;
import buildcraft.lib.gui.pos.IGuiPosition;

public class GuiIcon implements ISimpleDrawable {
    public final ISprite sprite;
    public final int textureSize;
    public final int width, height;

    public GuiIcon(ISprite sprite, int textureSize) {
        this.sprite = sprite;
        this.textureSize = textureSize;
        this.width = (int) (Math.abs(sprite.getInterpU(1) - sprite.getInterpU(0)) * textureSize);
        this.height = (int) (Math.abs(sprite.getInterpV(1) - sprite.getInterpV(0)) * textureSize);
    }

    public GuiIcon(Identifier texture, double u, double v, double width, double height, int texSize) {
        this(new SpriteRaw(texture, u, v, width, height, texSize), texSize);
    }

    public GuiIcon(Identifier texture, double u, double v, double width, double height) {
        this(texture, u, v, width, height, 256);
    }

    public GuiIcon offset(double u, double v) {
        SpriteRaw raw = (SpriteRaw) sprite;
        double uMin = raw.uMin + u / textureSize;
        double vMin = raw.vMin + v / textureSize;
        return new GuiIcon(new SpriteRaw(raw.location, uMin, vMin, raw.width, raw.height), textureSize);
    }

    public boolean containsGuiPos(double x, double y, IGuiPosition pos) {
        return new GuiRectangle(x, y, width, height).contains(pos);
    }

    @Override
    public void drawAt(double x, double y) {
        this.drawScaledInside(x, y, this.width, this.height);
    }

    public void drawScaledInside(IGuiArea element) {
        drawScaledInside(element.getX(), element.getY(), element.getWidth(), element.getHeight());
    }

    public void drawScaledInside(double x, double y, double drawnWidth, double drawnHeight) {
        draw(sprite, x, y, x + drawnWidth, y + drawnHeight);
    }

    /** 1.12.2 drew an arbitrary (perspective-correct) quadrilateral; 26.3's gui pipeline has no projective texture
     * co-ordinates, so this draws the sprite over the quad's bounding box. */
    public void drawCustomQuad(double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4) {
        double minX = Math.min(Math.min(x1, x2), Math.min(x3, x4));
        double maxX = Math.max(Math.max(x1, x2), Math.max(x3, x4));
        double minY = Math.min(Math.min(y1, y2), Math.min(y3, y4));
        double maxY = Math.max(Math.max(y1, y2), Math.max(y3, y4));
        draw(sprite, minX, minY, maxX, maxY);
    }

    public void drawCutInside(IGuiArea element) {
        drawCutInside(element.getX(), element.getY(), element.getWidth(), element.getHeight());
    }

    public void drawCutInside(double x, double y, double displayWidth, double displayHeight) {
        sprite.bindTexture();

        displayWidth = Math.min(this.width, displayWidth);
        displayHeight = Math.min(this.height, displayHeight);

        double xMin = x;
        double yMin = y;

        double xMax = x + displayWidth;
        double yMax = y + displayHeight;

        double uMin = sprite.getInterpU(0);
        double vMin = sprite.getInterpV(0);

        double uMax = sprite.getInterpU(displayWidth / width);
        double vMax = sprite.getInterpV(displayHeight / height);

        GuiDraw.quad(xMin, yMin, xMax, yMax, uMin, vMin, uMax, vMax);
    }

    public void drawCustomScaledAt(IGuiArea area, double texU0, double texV0, double texU1, double texV1) {
        double xMin = area.getX();
        double yMin = area.getY();
        double xMax = xMin + area.getWidth();
        double yMax = yMin + area.getHeight();
        drawCustomScaledAt(xMin, yMin, xMax, yMax, texU0, texV0, texU1, texV1);
    }

    /** @param xMin Absolute horizontal position to start rendering at
     * @param yMin Absolute vertical position to start rendering at
     * @param xMax Absolute horizontal width to end rendering at
     * @param yMax Absolute vertical width to end rendering at
     * @param texU0 Minimum texture U co-ordinate, always between 0 and 1.
     * @param texV0 Minimum texture V co-ordinate, always between 0 and 1.
     * @param texU1 Maximum texture U co-ordinate, always between 0 and 1.
     * @param texV1 Maximum texture V co-ordinate, always between 0 and 1. */
    public void drawCustomScaledAt(double xMin, double yMin, double xMax, double yMax, double texU0, double texV0, double texU1, double texV1) {
        drawCustomScaledAt(sprite, xMin, yMin, xMax, yMax, texU0, texV0, texU1, texV1);
    }

    /** @param xMin Absolute horizontal position to start rendering at
     * @param yMin Absolute vertical position to start rendering at
     * @param xMax Absolute horizontal width to end rendering at
     * @param yMax Absolute vertical width to end rendering at
     * @param texU0 Minimum texture U co-ordinate, always between 0 and 1.
     * @param texV0 Minimum texture V co-ordinate, always between 0 and 1.
     * @param texU1 Maximum texture U co-ordinate, always between 0 and 1.
     * @param texV1 Maximum texture V co-ordinate, always between 0 and 1. */
    public static void drawCustomScaledAt(ISprite sprite, double xMin, double yMin, double xMax, double yMax, double texU0, double texV0, double texU1, double texV1) {
        sprite.bindTexture();

        double uMin = sprite.getInterpU(texU0);
        double vMin = sprite.getInterpV(texV0);

        double uMax = sprite.getInterpU(texU1);
        double vMax = sprite.getInterpV(texV1);

        GuiDraw.quad(xMin, yMin, xMax, yMax, uMin, vMin, uMax, vMax);
    }

    public static void drawAt(ISprite sprite, double x, double y, double size) {
        drawAt(sprite, x, y, size, size);
    }

    public static void drawAt(ISprite sprite, double x, double y, double width, double height) {
        draw(sprite, x, y, x + width, y + height);
    }

    public static void draw(ISprite sprite, double xMin, double yMin, double xMax, double yMax) {
        sprite.bindTexture();

        double uMin = sprite.getInterpU(0);
        double vMin = sprite.getInterpV(0);

        double uMax = sprite.getInterpU(1);
        double vMax = sprite.getInterpV(1);

        GuiDraw.quad(xMin, yMin, xMax, yMax, uMin, vMin, uMax, vMax);
    }

}
