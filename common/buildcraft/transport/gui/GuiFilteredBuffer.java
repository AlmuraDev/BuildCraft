/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.transport.gui;

import buildcraft.lib.gui.GuiDraw;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;

import buildcraft.lib.gui.GuiBC8;
import buildcraft.lib.gui.GuiIcon;

import buildcraft.transport.BCTransportSprites;
import buildcraft.transport.container.ContainerFilteredBuffer_BC8;

public class GuiFilteredBuffer extends GuiBC8<ContainerFilteredBuffer_BC8> {
    private static final Identifier TEXTURE_BASE = Identifier.parse("buildcrafttransport:textures/gui/filtered_buffer.png");
    private static final int SIZE_X = 176, SIZE_Y = 169;
    private static final GuiIcon ICON_GUI = new GuiIcon(TEXTURE_BASE, 0, 0, SIZE_X, SIZE_Y);

    public GuiFilteredBuffer(ContainerFilteredBuffer_BC8 container) {
        super(container);
        xSize = SIZE_X;
        ySize = SIZE_Y;
    }

    @Override
    protected void drawBackgroundLayer(float partialTicks) {
        ICON_GUI.drawAt(mainGui.rootElement);
        for (int i = 0; i < 9; i++) {
            ItemStack stack = container.tile.invFilter.getStackInSlot(i);
            double currentX = mainGui.rootElement.getX() + 8 + i * 18;
            double currentY = mainGui.rootElement.getY() + 61;
            // GL11.glEnable(GL11.GL_BLEND);
            // GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            // GuiDraw.colour((float) (1), (float) ( 1), (float) ( 1), (float) ( 0.5F));
            if (!stack.isEmpty()) {
                GuiDraw.item(stack, (int) currentX, (int) currentY);
            } else {
                GuiIcon.drawAt(BCTransportSprites.NOTHING_FILTERED_BUFFER_SLOT, currentX, currentY, 16, 16);
            }
            // GuiDraw.colour((float) (1), (float) ( 1), (float) ( 1), (float) ( 1));
            // GL11.glDisable(GL11.GL_BLEND);
        }
        // GuiDraw.pushPose();
        // GL11.glTranslatef(0, 0, 100);
        GuiDraw.colour((float) (1), (float) ( 1), (float) ( 1), (float) ( 0.7f));
        ICON_GUI.drawAt(mainGui.rootElement);

        GuiDraw.colour((float) (1), (float) ( 1), (float) ( 1), (float) ( 1));
        // GuiDraw.popPose();
    }

    @Override
    protected void drawForegroundLayer() {
        int x = guiLeft;
        int y = guiTop;
        String title = I18n.get("tile.filteredBufferBlock.name");
        int xPos = (xSize - fontRenderer.width(title)) / 2;
        GuiDraw.text(fontRenderer, title, x + xPos, y + 10, 0x404040, false);
    }
}
