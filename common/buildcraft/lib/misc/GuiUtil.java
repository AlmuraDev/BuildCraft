/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.misc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import net.neoforged.neoforge.fluids.FluidStack;

import buildcraft.api.core.render.ISprite;

import buildcraft.lib.client.guide.font.IFontRenderer;
import buildcraft.lib.client.render.fluid.FluidRenderer;
import buildcraft.lib.client.render.fluid.FluidSpriteType;
import buildcraft.lib.client.sprite.SpriteNineSliced;
import buildcraft.lib.client.sprite.SubSprite;
import buildcraft.lib.expression.api.IConstantNode;
import buildcraft.lib.fluid.Tank;
import buildcraft.lib.gui.GuiDraw;
import buildcraft.lib.gui.elem.ToolTip;
import buildcraft.lib.gui.pos.GuiRectangle;
import buildcraft.lib.gui.pos.IGuiArea;
import buildcraft.lib.gui.pos.IGuiPosition;

/** GUI helpers. Everything draws through {@link GuiDraw}, so it only works inside a BuildCraft screen's render. */
public class GuiUtil {

    public static final IGuiArea AREA_WHOLE_SCREEN;

    static {
        AREA_WHOLE_SCREEN = IGuiArea.create(() -> 0, () -> 0, GuiUtil::getScreenWidth, GuiUtil::getScreenHeight);
    }

    /** @return The relative screen width (changes with both the window size and the "gui scale" setting). */
    public static int getScreenWidth() {
        return Minecraft.getInstance().getWindow().getGuiScaledWidth();
    }

    /** @return The relative screen height (changes with both the window size and the "gui scale" setting). */
    public static int getScreenHeight() {
        return Minecraft.getInstance().getWindow().getGuiScaledHeight();
    }

    public static IGuiArea moveRectangleToCentre(GuiRectangle area) {
        final double w = area.width;
        final double h = area.height;

        DoubleSupplier posX = () -> (AREA_WHOLE_SCREEN.getWidth() - w) / 2;
        DoubleSupplier posY = () -> (AREA_WHOLE_SCREEN.getHeight() - h) / 2;

        IGuiPosition position = IGuiPosition.create(posX, posY);
        return IGuiArea.create(position, area.width, area.height);
    }

    public static IGuiArea moveAreaToCentre(IGuiArea area) {
        if (area instanceof GuiRectangle || area instanceof IConstantNode) {
            return moveRectangleToCentre(area.asImmutable());
        }

        DoubleSupplier posX = () -> (AREA_WHOLE_SCREEN.getWidth() - area.getWidth()) / 2;
        DoubleSupplier posY = () -> (AREA_WHOLE_SCREEN.getHeight() - area.getHeight()) / 2;

        return IGuiArea.create(posX, posY, area::getWidth, area::getHeight);
    }

    public static ToolTip createToolTip(Supplier<ItemStack> stackRef) {
        return new ToolTip() {
            @Override
            public void refresh() {
                delegate().clear();
                ItemStack stack = stackRef.get();
                if (!stack.isEmpty()) {
                    delegate().addAll(GuiUtil.getFormattedTooltip(stack));
                }
            }
        };
    }

    /** Draws multiple elements, one after each other. */
    public static <D> void drawVerticallyAppending(IGuiPosition element, Iterable<? extends D> iterable,
        IVerticalAppendingDrawer<D> drawer) {
        double x = element.getX();
        double y = element.getY();
        for (D drawable : iterable) {
            y += drawer.draw(drawable, x, y);
        }
    }

    public static void drawItemStackAt(ItemStack stack, int x, int y) {
        GuiDraw.item(stack, x, y);
    }

    @FunctionalInterface
    public interface IVerticalAppendingDrawer<D> {
        double draw(D drawable, double x, double y);
    }

    /** A copy of Forge 1.12.2's GuiUtils.drawHoveringText, except that it returns the height of the box that was
     * drawn. Positions the box relative to the mouse like vanilla does, and wraps text that doesn't fit on screen.
     *
     * @param maxTextWidth the maximum width of the text in the tooltip box, or a negative number for no maximum.
     * @return the height of the box drawn, plus 5 (0 if nothing was drawn) */
    public static int drawHoveringText(List<String> textLines, final int mouseX, final int mouseY,
        final int screenWidth, final int screenHeight, final int maxTextWidth, Font font) {
        if (textLines.isEmpty()) {
            return 0;
        }
        int tooltipTextWidth = 0;
        for (String textLine : textLines) {
            tooltipTextWidth = Math.max(tooltipTextWidth, font.width(textLine));
        }

        boolean needsWrap = false;
        int titleLinesCount = 1;
        int tooltipX = mouseX + 12;
        if (tooltipX + tooltipTextWidth + 4 > screenWidth) {
            tooltipX = mouseX - 16 - tooltipTextWidth;
            if (tooltipX < 4) {
                if (mouseX > screenWidth / 2) {
                    tooltipTextWidth = mouseX - 12 - 8;
                } else {
                    tooltipTextWidth = screenWidth - 16 - mouseX;
                }
                needsWrap = true;
            }
        }

        if (maxTextWidth > 0 && tooltipTextWidth > maxTextWidth) {
            tooltipTextWidth = maxTextWidth;
            needsWrap = true;
        }

        if (needsWrap) {
            int wrappedTooltipWidth = 0;
            List<String> wrappedTextLines = new ArrayList<>();
            for (int i = 0; i < textLines.size(); i++) {
                List<String> wrappedLine = wrap(font, textLines.get(i), tooltipTextWidth);
                if (i == 0) {
                    titleLinesCount = wrappedLine.size();
                }
                for (String line : wrappedLine) {
                    wrappedTooltipWidth = Math.max(wrappedTooltipWidth, font.width(line));
                    wrappedTextLines.add(line);
                }
            }
            tooltipTextWidth = wrappedTooltipWidth;
            textLines = wrappedTextLines;

            if (mouseX > screenWidth / 2) {
                tooltipX = mouseX - 16 - tooltipTextWidth;
            } else {
                tooltipX = mouseX + 12;
            }
        }

        int tooltipY = mouseY - 12;
        int tooltipHeight = 8;

        if (textLines.size() > 1) {
            tooltipHeight += (textLines.size() - 1) * 10;
            if (textLines.size() > titleLinesCount) {
                tooltipHeight += 2; // gap between title lines and next lines
            }
        }

        if (tooltipY + tooltipHeight + 6 > screenHeight) {
            tooltipY = screenHeight - tooltipHeight - 6;
        }

        final int backgroundColor = 0xF0100010;
        int x0 = tooltipX - 3, x1 = tooltipX + tooltipTextWidth + 3;
        int y0 = tooltipY - 3, y1 = tooltipY + tooltipHeight + 3;
        GuiDraw.gradient(x0, y0 - 1, x1, y0, backgroundColor, backgroundColor);
        GuiDraw.gradient(x0, y1, x1, y1 + 1, backgroundColor, backgroundColor);
        GuiDraw.gradient(x0, y0, x1, y1, backgroundColor, backgroundColor);
        GuiDraw.gradient(x0 - 1, y0, x0, y1, backgroundColor, backgroundColor);
        GuiDraw.gradient(x1, y0, x1 + 1, y1, backgroundColor, backgroundColor);
        final int borderColorStart = 0x505000FF;
        final int borderColorEnd = (borderColorStart & 0xFEFEFE) >> 1 | borderColorStart & 0xFF000000;
        GuiDraw.gradient(x0, y0 + 1, x0 + 1, y1 - 1, borderColorStart, borderColorEnd);
        GuiDraw.gradient(x1 - 1, y0 + 1, x1, y1 - 1, borderColorStart, borderColorEnd);
        GuiDraw.gradient(x0, y0, x1, y0 + 1, borderColorStart, borderColorStart);
        GuiDraw.gradient(x0, y1 - 1, x1, y1, borderColorEnd, borderColorEnd);

        for (int lineNumber = 0; lineNumber < textLines.size(); ++lineNumber) {
            GuiDraw.text(font, textLines.get(lineNumber), tooltipX, tooltipY, -1, true);
            if (lineNumber + 1 == titleLinesCount) {
                tooltipY += 2;
            }
            tooltipY += 10;
        }
        return tooltipHeight + 5;
    }

    /** Wraps a line to the given width. Formatting codes only carry over within the first piece. */
    public static List<String> wrap(Font font, String text, int width) {
        List<String> out = new ArrayList<>();
        for (FormattedText part : font.getSplitter().splitLines(text, width, Style.EMPTY)) {
            out.add(part.getString());
        }
        if (out.isEmpty()) {
            out.add("");
        }
        return out;
    }

    public static void drawHorizontalLine(int startX, int endX, int y, int color) {
        if (endX < startX) {
            int i = startX;
            startX = endX;
            endX = i;
        }
        GuiDraw.fill(startX, y, endX + 1, y + 1, color);
    }

    public static void drawVerticalLine(int x, int startY, int endY, int color) {
        if (endY < startY) {
            int i = startY;
            startY = endY;
            endY = i;
        }
        GuiDraw.fill(x, startY + 1, x + 1, endY, color);
    }

    public static void drawRect(IGuiArea area, int colour) {
        int xMin = (int) area.getX();
        int yMin = (int) area.getY();
        int xMax = (int) area.getEndX();
        int yMax = (int) area.getEndY();
        GuiDraw.fill(xMin, yMin, xMax, yMax, colour);
    }

    /** 1.12.2's Gui.drawTexturedModalRect, on the bound texture (256x256). */
    public static void drawTexturedModalRect(double posX, double posY, double textureX, double textureY, double width,
        double height) {
        GuiDraw.texturedRect(Math.floor(posX), Math.floor(posY), Math.floor(textureX), Math.floor(textureY),
            Math.floor(width), Math.floor(height));
    }

    public static void drawFluid(IGuiArea position, Tank tank) {
        drawFluid(position, tank.getFluidForRender(), tank.getCapacity());
    }

    public static void drawFluid(IGuiArea position, FluidStack fluid, int capacity) {
        if (fluid == null || fluid.getAmount() <= 0) return;
        drawFluid(position, fluid, fluid.getAmount(), capacity);
    }

    public static void drawFluid(IGuiArea position, FluidStack fluid, int amount, int capacity) {
        if (fluid == null || fluid.isEmpty() || amount <= 0) return;

        double height = amount * position.getHeight() / capacity;

        double startX = position.getX();
        double startY;
        double endX = startX + position.getWidth();
        double endY;

        if (fluid.getFluidType().isLighterThanAir()) {
            startY = position.getY() + height;
            endY = position.getY();
        } else {
            startY = position.getEndY();
            endY = startY - height;
        }

        drawFluidForGui(fluid, startX, startY, endX, endY);
    }

    /** Fills the area with the fluid's still texture, tiled every 16 pixels from (startX, startY) (1.12.2's
     * FluidRenderer.drawFluidForGui). */
    public static void drawFluidForGui(FluidStack fluid, double startX, double startY, double endX, double endY) {
        TextureAtlasSprite sprite = FluidRenderer.getFluidSprite(FluidSpriteType.STILL, fluid);
        if (sprite == null) {
            sprite = SpriteUtil.missingSprite();
        }
        int colour = 0xFF_00_00_00 | (FluidRenderer.getFluidTint(fluid) & 0xFF_FF_FF);
        double diffX = endX - startX;
        double diffY = endY - startY;
        double signX = Math.signum(diffX);
        double signY = Math.signum(diffY);
        for (double dx = 0; dx < Math.abs(diffX); dx += 16) {
            double w = Math.min(16, Math.abs(diffX) - dx);
            double x0 = startX + signX * dx;
            double x1 = x0 + signX * w;
            for (double dy = 0; dy < Math.abs(diffY); dy += 16) {
                double h = Math.min(16, Math.abs(diffY) - dy);
                double y0 = startY + signY * dy;
                double y1 = y0 + signY * h;
                GuiDraw.quad(TextureAtlas.LOCATION_BLOCKS, x0, y0, x1, y1, sprite.getU0(), sprite.getV0(),
                    sprite.getU((float) (w / 16)), sprite.getV((float) (h / 16)), colour);
            }
        }
    }

    public static AutoGlScissor scissor(double x, double y, double width, double height) {
        return scissor(new GuiRectangle(x, y, width, height));
    }

    /** Clips drawing to the area until the returned scissor is closed (scissors nest). */
    public static AutoGlScissor scissor(IGuiArea area) {
        GuiRectangle rect = area.asImmutable();
        GuiDraw.enableScissor((int) Math.floor(rect.x), (int) Math.floor(rect.y), (int) Math.ceil(rect.getEndX()),
            (int) Math.ceil(rect.getEndY()));
        return GuiDraw::disableScissor;
    }

    public static ISprite subRelative(ISprite sprite, double u, double v, double width, double height, double size) {
        return GuiUtil.subRelative(sprite, u / size, v / size, width / size, height / size);
    }

    public static ISprite subAbsolute(ISprite sprite, double uMin, double vMin, double uMax, double vMax,
        double spriteSize) {
        double size = spriteSize;
        return GuiUtil.subAbsolute(sprite, uMin / size, vMin / size, uMax / size, vMax / size);
    }

    public static ISprite subRelative(ISprite sprite, double u, double v, double width, double height) {
        return GuiUtil.subAbsolute(sprite, u, v, u + width, v + height);
    }

    public static ISprite subAbsolute(ISprite sprite, double uMin, double vMin, double uMax, double vMax) {
        if (uMin == 0 && vMin == 0 && uMax == 1 && vMax == 1) {
            return sprite;
        }
        return new SubSprite(sprite, uMin, vMin, uMax, vMax);
    }

    public static SpriteNineSliced slice(ISprite sprite, int uMin, int vMin, int uMax, int vMax, int textureSize) {
        return new SpriteNineSliced(sprite, uMin, vMin, uMax, vMax, textureSize);
    }

    public static SpriteNineSliced slice(ISprite sprite, double uMin, double vMin, double uMax, double vMax,
        double scale) {
        return new SpriteNineSliced(sprite, uMin, vMin, uMax, vMax, scale);
    }

    /** An {@link AutoCloseable} that pops the current scissor region. */
    public interface AutoGlScissor extends AutoCloseable {
        @Override
        void close();
    }

    public static List<String> getFormattedTooltip(ItemStack stack) {
        List<String> list = getUnFormattedTooltip(stack);

        if (!list.isEmpty()) {
            ChatFormatting colour = stack.getRarity().color();
            list.set(0, colour + list.get(0));
        }

        for (int i = 1; i < list.size(); ++i) {
            list.set(i, ChatFormatting.GRAY + list.get(i));
        }

        return list;
    }

    public static List<String> getUnFormattedTooltip(ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        List<String> list = new ArrayList<>();
        for (Component line : Screen.getTooltipFromItem(mc, stack)) {
            list.add(line.getString());
        }
        if (list.isEmpty()) {
            return Collections.singletonList(getStackDisplayName(stack));
        }
        return list;
    }

    public static String getStackDisplayName(ItemStack stack) {
        return stack.getHoverName().getString();
    }

    public static TooltipFlag getTooltipFlags() {
        boolean adv = Minecraft.getInstance().options.advancedItemTooltips;
        return adv ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL;
    }

    public static WrappedTextData getWrappedTextData(String text, IFontRenderer fontRenderer, int maxWidth,
        boolean shadow, float scale) {
        List<String> lines = fontRenderer.wrapString(text, maxWidth, shadow, scale);
        return new WrappedTextData(fontRenderer, lines.toArray(new String[0]), shadow, scale, maxWidth,
            (int) (lines.size() * fontRenderer.getFontHeight("Ly") * scale));
    }

    public static class WrappedTextData {
        public final IFontRenderer renderer;
        public final String[] lines;
        public final float scale;
        public final boolean shadow;
        public final int width, height;

        public WrappedTextData(IFontRenderer renderer, String[] lines, boolean shadow, float scale, int width,
            int height) {
            this.renderer = renderer;
            this.lines = lines;
            this.shadow = shadow;
            this.scale = scale;
            this.width = width;
            this.height = height;
        }

        public void drawAt(int x, int y, int colour, boolean centered) {
            for (String line : lines) {
                renderer.drawString(line, x, y, colour, shadow, centered, scale);
                y += renderer.getFontHeight(line) * scale;
            }
        }
    }
}
