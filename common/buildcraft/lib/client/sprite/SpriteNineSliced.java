/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.client.sprite;


import buildcraft.api.core.render.ISprite;

import buildcraft.lib.gui.GuiDraw;
import buildcraft.lib.gui.pos.IGuiArea;

/** Defines and draws a 9-sliced sprite. */
public class SpriteNineSliced {
    public final ISprite sprite;
    public final double xMin, yMin, xMax, yMax;
    public final double xScale, yScale;

    public SpriteNineSliced(ISprite sprite, int xMin, int yMin, int xMax, int yMax, int textureSize) {
        this(sprite, xMin, yMin, xMax, yMax, textureSize, textureSize);
    }

    public SpriteNineSliced(ISprite sprite, int xMin, int yMin, int xMax, int yMax, int xScale, int yScale) {
        this.sprite = sprite;
        this.xMin = xMin / (double) xScale;
        this.yMin = yMin / (double) yScale;
        this.xMax = xMax / (double) xScale;
        this.yMax = yMax / (double) yScale;
        this.xScale = xScale;
        this.yScale = yScale;
    }

    public SpriteNineSliced(ISprite sprite, double xMin, double yMin, double xMax, double yMax, double scale) {
        this(sprite, xMin, yMin, xMax, yMax, scale, scale);
    }

    public SpriteNineSliced(ISprite sprite, double xMin, double yMin, double xMax, double yMax, double xScale,
        double yScale) {
        this.sprite = sprite;
        this.xMin = xMin;
        this.yMin = yMin;
        this.xMax = xMax;
        this.yMax = yMax;
        this.xScale = xScale;
        this.yScale = yScale;
    }

    public void draw(IGuiArea element) {
        draw(element.getX(), element.getY(), element.getWidth(), element.getHeight());
    }

    public void draw(double x, double y, double width, double height) {
        sprite.bindTexture();

        double sx = xScale;
        double sy = yScale;
        double[] xa = { 0, xMin * sx, width + (xMax - 1) * sx, width };
        double[] ya = { 0, yMin * sy, height + (yMax - 1) * sy, height };

        double[] ua = { 0, xMin, xMax, 1 };
        double[] va = { 0, yMin, yMax, 1 };

        for (int xi = 0; xi < 3; xi++) {
            for (int yi = 0; yi < 3; yi++) {
                GuiDraw.quad(x + xa[xi], y + ya[yi], x + xa[xi + 1], y + ya[yi + 1], sprite.getInterpU(ua[xi]),
                    sprite.getInterpV(va[yi]), sprite.getInterpU(ua[xi + 1]), sprite.getInterpV(va[yi + 1]));
            }
        }
    }
}
