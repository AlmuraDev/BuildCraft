/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.gui;

import java.util.ArrayDeque;
import java.util.Deque;

import javax.annotation.Nullable;

import org.joml.Matrix3x2fStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;


/** The drawing context for BuildCraft's GUI code. 1.12.2's GUIs drew straight to OpenGL (bind a texture, set a colour,
 * push a matrix, draw a quad); 26.3 screens draw into a {@link GuiGraphicsExtractor} that is only handed to the
 * screen's render methods. Screens call {@link #begin(GuiGraphicsExtractor)} / {@link #end()} around their drawing,
 * and everything below (icons, sprites, elements, ledgers) draws through the static methods here, which keep the
 * 1.12.2 "current texture" and "current colour" state. Drawing outside a screen's render pass does nothing. */
public final class GuiDraw {
    /** UV resolution for quads with arbitrary (non-pixel) texture coordinates. */
    private static final int UV_SCALE = 1 << 16;

    @Nullable
    private static GuiGraphicsExtractor graphics;
    private static Identifier texture = TextureAtlas.LOCATION_BLOCKS;
    private static int colour = -1;
    private static final Deque<GuiGraphicsExtractor> stack = new ArrayDeque<>();

    private GuiDraw() {}

    public static void begin(GuiGraphicsExtractor g) {
        if (graphics != null) {
            stack.push(graphics);
        }
        graphics = g;
        colour = -1;
    }

    public static void end() {
        graphics = stack.isEmpty() ? null : stack.pop();
        colour = -1;
    }

    @Nullable
    public static GuiGraphicsExtractor graphics() {
        return graphics;
    }

    public static boolean isActive() {
        return graphics != null;
    }

    public static Font font() {
        return Minecraft.getInstance().font;
    }

    // Texture and colour state

    /** 1.12.2's bindTexture: the texture following {@link #quad} calls use. */
    public static void bind(Identifier location) {
        texture = location;
    }

    public static void bindBlockAtlas() {
        texture = TextureAtlas.LOCATION_BLOCKS;
    }

    public static Identifier boundTexture() {
        return texture;
    }

    /** 1.12.2's GlStateManager.color: tints following textured quads (ARGB). */
    public static void colour(int argb) {
        colour = argb;
    }

    public static void colour(float r, float g, float b, float a) {
        colour = ARGB.color(Math.round(a * 255), Math.round(r * 255), Math.round(g * 255), Math.round(b * 255));
    }

    public static void resetColour() {
        colour = -1;
    }

    public static int currentColour() {
        return colour;
    }

    // Matrix

    public static void pushPose() {
        if (graphics != null) graphics.pose().pushMatrix();
    }

    public static void popPose() {
        if (graphics != null) graphics.pose().popMatrix();
    }

    public static void translate(double x, double y) {
        if (graphics != null) graphics.pose().translate((float) x, (float) y);
    }

    public static void scale(double x, double y) {
        if (graphics != null) graphics.pose().scale((float) x, (float) y);
    }

    // Drawing

    /** Draws the bound texture over (x0, y0) to (x1, y1) with UVs (0-1) u0,v0 to u1,v1, tinted by the current colour. */
    public static void quad(double x0, double y0, double x1, double y1, double u0, double v0, double u1, double v1) {
        quad(texture, x0, y0, x1, y1, u0, v0, u1, v1, colour);
    }

    public static void quad(Identifier tex, double x0, double y0, double x1, double y1, double u0, double v0, double u1,
        double v1, int argb) {
        GuiGraphicsExtractor g = graphics;
        if (g == null || x1 == x0 || y1 == y0) {
            return;
        }
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate((float) x0, (float) y0);
        pose.scale((float) (x1 - x0), (float) (y1 - y0));
        int uw = (int) Math.round((u1 - u0) * UV_SCALE);
        int vh = (int) Math.round((v1 - v0) * UV_SCALE);
        g.blit(RenderPipelines.GUI_TEXTURED, tex, 0, 0, (float) (u0 * UV_SCALE), (float) (v0 * UV_SCALE), 1, 1, uw, vh,
            UV_SCALE, UV_SCALE, argb);
        pose.popMatrix();
    }

    /** 1.12.2's Gui.drawTexturedModalRect: pixel UVs on a 256x256 texture. */
    public static void texturedRect(double x, double y, double u, double v, double width, double height) {
        quad(x, y, x + width, y + height, u / 256, v / 256, (u + width) / 256, (v + height) / 256);
    }

    /** 1.12.2's Gui.drawRect (ARGB; alpha 0 draws nothing, as before). */
    public static void fill(double x0, double y0, double x1, double y1, int argb) {
        GuiGraphicsExtractor g = graphics;
        if (g == null) return;
        if (isWhole(x0) && isWhole(y0) && isWhole(x1) && isWhole(y1)) {
            g.fill((int) x0, (int) y0, (int) x1, (int) y1, argb);
            return;
        }
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate((float) x0, (float) y0);
        pose.scale((float) (x1 - x0), (float) (y1 - y0));
        g.fill(0, 0, 1, 1, argb);
        pose.popMatrix();
    }

    public static void gradient(int x0, int y0, int x1, int y1, int argbTop, int argbBottom) {
        if (graphics != null) graphics.fillGradient(x0, y0, x1, y1, argbTop, argbBottom);
    }

    /** Draws text with 1.12.2's colour rule: a colour without alpha is opaque. Formatting codes work. */
    public static void text(String str, double x, double y, int rgb, boolean shadow) {
        text(font(), str, x, y, rgb, shadow);
    }

    public static void text(Font font, String str, double x, double y, int rgb, boolean shadow) {
        GuiGraphicsExtractor g = graphics;
        if (g == null || str == null) return;
        int argb = ARGB.alpha(rgb) == 0 ? rgb | 0xFF_00_00_00 : rgb;
        if (isWhole(x) && isWhole(y)) {
            g.text(font, str, (int) x, (int) y, argb, shadow);
        } else {
            Matrix3x2fStack pose = g.pose();
            pose.pushMatrix();
            pose.translate((float) x, (float) y);
            g.text(font, str, 0, 0, argb, shadow);
            pose.popMatrix();
        }
    }

    public static void item(ItemStack stack, int x, int y) {
        GuiGraphicsExtractor g = graphics;
        if (g == null || stack.isEmpty()) return;
        g.item(stack, x, y);
        g.itemDecorations(font(), stack, x, y);
    }

    public static void fakeItem(ItemStack stack, int x, int y) {
        GuiGraphicsExtractor g = graphics;
        if (g == null || stack.isEmpty()) return;
        g.fakeItem(stack, x, y);
    }

    public static void enableScissor(int x0, int y0, int x1, int y1) {
        if (graphics != null) graphics.enableScissor(x0, y0, x1, y1);
    }

    public static void disableScissor() {
        if (graphics != null) graphics.disableScissor();
    }

    /** Draws everything after this on top of what came before (tooltips, menus). */
    public static void nextLayer() {
        if (graphics != null) graphics.nextStratum();
    }

    private static boolean isWhole(double d) {
        return d == Math.rint(d);
    }
}
