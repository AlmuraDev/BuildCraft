/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.robotics.gui;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import buildcraft.lib.gui.GuiBC8;
import buildcraft.lib.gui.GuiDraw;
import buildcraft.lib.gui.GuiIcon;
import buildcraft.lib.gui.pos.GuiRectangle;

import buildcraft.core.BCCoreItems;
import buildcraft.core.item.ItemPaintbrush_BC8;
import buildcraft.robotics.client.render.RenderZonePlanner;
import buildcraft.robotics.container.ContainerZonePlanner;
import buildcraft.robotics.zone.ZonePlan;
import buildcraft.robotics.zone.ZonePlannerMapChunk;
import buildcraft.robotics.zone.ZonePlannerMapChunk.MapColourData;
import buildcraft.robotics.zone.ZonePlannerMapChunkKey;
import buildcraft.robotics.zone.ZonePlannerMapDataClient;

/** The zone planner: a map of the area around the block, with the 16 zone layers (one per dye colour) painted on it.
 * Drag with an empty hand to move the map, scroll to zoom, and drag with a paintbrush to add (left button) or remove
 * (right button) a rectangle of its colour's layer.
 * <p>
 * 1.12.2 drew the map in 3D (display lists of every block column, seen from above in perspective); 26.3's GUI only
 * draws flat elements, so the map is a top-down view of each column's map colour. */
public class GuiZonePlanner extends GuiBC8<ContainerZonePlanner> {
    private static final Identifier TEXTURE_BASE = Identifier.parse("buildcraftrobotics:textures/gui/zone_planner.png");
    private static final int SIZE_X = 256, SIZE_Y = 228;
    private static final GuiIcon ICON_GUI = new GuiIcon(TEXTURE_BASE, 0, 0, SIZE_X, SIZE_Y);
    private static final GuiIcon ICON_PROGRESS_INPUT = new GuiIcon(TEXTURE_BASE, 9, 228, 28, 9);
    private static final GuiIcon ICON_PROGRESS_OUTPUT = new GuiIcon(TEXTURE_BASE, 0, 228, 9, 28);
    private static final GuiRectangle RECT_PROGRESS_INPUT = new GuiRectangle(44, 128, 28, 9);
    private static final GuiRectangle RECT_PROGRESS_OUTPUT = new GuiRectangle(236, 45, 9, 28);

    /** The map area, relative to the gui. */
    private static final int MAP_X = 8, MAP_Y = 9, MAP_W = 213, MAP_H = 100;
    private static final double MIN_SCALE = 1, MAX_SCALE = 8;

    private double startMouseX = 0;
    private double startMouseY = 0;
    private double startPositionX = 0;
    private double startPositionZ = 0;
    /** Pixels per block. */
    private double scale = 2;
    private double positionX;
    private double positionZ;
    private boolean canDrag = false;
    private BlockPos lastSelected = null;
    private BlockPos selectionStartXZ = null;
    private ZonePlan bufferLayer = null;

    public GuiZonePlanner(ContainerZonePlanner container) {
        super(container);
        xSize = SIZE_X;
        ySize = SIZE_Y;
        BlockPos tilePos = container.tile.getBlockPos();
        positionX = tilePos.getX() + 0.5;
        positionZ = tilePos.getZ() + 0.5;
    }

    private ItemStack getCurrentStack() {
        return container.getCarried();
    }

    @Nullable
    private ItemStack getPaintbrush() {
        ItemStack currentStack = getCurrentStack();
        if (!currentStack.isEmpty() && currentStack.getItem() instanceof ItemPaintbrush_BC8) {
            return currentStack;
        }
        return null;
    }

    @Nullable
    private ItemPaintbrush_BC8.Brush getPaintbrushBrush() {
        ItemStack paintbrush = getPaintbrush();
        if (paintbrush != null) {
            ItemPaintbrush_BC8.Brush brush = BCCoreItems.paintbrush.getBrushFromStack(paintbrush);
            if (brush.colour != null) {
                return brush;
            }
        }
        return null;
    }

    // Map geometry

    private double mapCentreX() {
        return guiLeft + MAP_X + MAP_W / 2.0;
    }

    private double mapCentreY() {
        return guiTop + MAP_Y + MAP_H / 2.0;
    }

    private boolean isInMap(double mouseX, double mouseY) {
        return mouseX >= guiLeft + MAP_X && mouseX < guiLeft + MAP_X + MAP_W && mouseY >= guiTop + MAP_Y
            && mouseY < guiTop + MAP_Y + MAP_H;
    }

    private int blockXAt(double screenX) {
        return (int) Math.floor(positionX + (screenX - mapCentreX()) / scale);
    }

    private int blockZAt(double screenY) {
        return (int) Math.floor(positionZ + (screenY - mapCentreY()) / scale);
    }

    @Nullable
    private ZonePlannerMapChunk getMapChunk(int blockX, int blockZ) {
        Level world = mc.level;
        if (world == null) {
            return null;
        }
        ZonePlannerMapChunkKey key = new ZonePlannerMapChunkKey(new ChunkPos(blockX >> 4, blockZ >> 4),
            world.dimension(), RenderZonePlanner.getMapLevel());
        return ZonePlannerMapDataClient.INSTANCE.getChunk(world, key);
    }

    @Nullable
    private BlockPos getMapBlock(int blockX, int blockZ) {
        ZonePlannerMapChunk chunk = getMapChunk(blockX, blockZ);
        MapColourData data = chunk == null ? null : chunk.getData(blockX, blockZ);
        return data == null ? null : new BlockPos(blockX, data.posY, blockZ);
    }

    // Input

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (isInMap(x, y) && scrollY != 0) {
            scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale * Math.pow(1.2, scrollY)));
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        canDrag = false;
        if (!isInMap(mouseX, mouseY)) {
            return;
        }
        if (getPaintbrush() != null) {
            if (lastSelected != null) {
                selectionStartXZ = lastSelected;
            }
        } else if (getCurrentStack().isEmpty()) {
            startPositionX = positionX;
            startPositionZ = positionZ;
            startMouseX = mouseX;
            startMouseY = mouseY;
            canDrag = true;
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        if (!canDrag) {
            ItemPaintbrush_BC8.Brush brush = getPaintbrushBrush();
            if (brush != null && selectionStartXZ != null && isInMap(mouseX, mouseY)) {
                BlockPos current = new BlockPos(blockXAt(mouseX), 0, blockZAt(mouseY));
                lastSelected = current;
                bufferLayer = new ZonePlan(container.tile.layers[brush.colour.getId()]);
                BlockPos tilePos = container.tile.getBlockPos();
                for (int x = Math.min(selectionStartXZ.getX(), current.getX());
                     x <= Math.max(selectionStartXZ.getX(), current.getX()); x++) {
                    for (int z = Math.min(selectionStartXZ.getZ(), current.getZ());
                         z <= Math.max(selectionStartXZ.getZ(), current.getZ()); z++) {
                        if (clickedMouseButton == 0) {
                            bufferLayer.set(x - tilePos.getX(), z - tilePos.getZ(), true);
                        } else if (clickedMouseButton == 1) {
                            bufferLayer.set(x - tilePos.getX(), z - tilePos.getZ(), false);
                        }
                    }
                }
            }
            return;
        }
        positionX = startPositionX - (mouseX - startMouseX) / scale;
        positionZ = startPositionZ - (mouseY - startMouseY) / scale;
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        selectionStartXZ = null;
        ItemPaintbrush_BC8.Brush brush = getPaintbrushBrush();
        if (brush != null && bufferLayer != null) {
            container.tile.layers[brush.colour.getId()] = bufferLayer;
            container.tile.sendLayerToServer(brush.colour.getId());
        }
        bufferLayer = null;
    }

    // Drawing

    @Override
    protected void drawBackgroundLayer(float partialTicks) {
        ICON_GUI.drawAt(mainGui.rootElement);

        drawProgress(RECT_PROGRESS_INPUT, ICON_PROGRESS_INPUT, container.tile.deltaProgressInput.getDynamic(partialTicks),
            1);
        drawProgress(RECT_PROGRESS_OUTPUT, ICON_PROGRESS_OUTPUT, 1,
            container.tile.deltaProgressOutput.getDynamic(partialTicks));
    }

    @Override
    protected void drawForegroundLayer() {
        int mx = (int) mainGui.mouse.getX();
        int my = (int) mainGui.mouse.getY();
        lastSelected = null;
        if (isInMap(mx, my)) {
            lastSelected = getMapBlock(blockXAt(mx), blockZAt(my));
        }

        int left = guiLeft + MAP_X, top = guiTop + MAP_Y;
        GuiDraw.enableScissor(left, top, left + MAP_W, top + MAP_H);
        int minX = blockXAt(left), maxX = blockXAt(left + MAP_W);
        int minZ = blockZAt(top), maxZ = blockZAt(top + MAP_H);
        ItemPaintbrush_BC8.Brush brush = getPaintbrushBrush();
        BlockPos tilePos = container.tile.getBlockPos();

        for (int bx = minX; bx <= maxX; bx++) {
            for (int bz = minZ; bz <= maxZ; bz++) {
                ZonePlannerMapChunk chunk = getMapChunk(bx, bz);
                if (chunk == null) {
                    continue;
                }
                MapColourData data = chunk.getData(bx, bz);
                if (data == null) {
                    continue;
                }
                double x0 = mapCentreX() + (bx - positionX) * scale;
                double y0 = mapCentreY() + (bz - positionZ) * scale;
                GuiDraw.fill(x0, y0, x0 + scale, y0 + scale, 0xFF_00_00_00 | data.colour);

                for (int i = 0; i < container.tile.layers.length; i++) {
                    if (brush != null && brush.colour.getId() != i) {
                        continue;
                    }
                    ZonePlan layer = container.tile.layers[i];
                    if (brush != null && bufferLayer != null) {
                        layer = bufferLayer;
                    }
                    if (layer.get(bx - tilePos.getX(), bz - tilePos.getZ())) {
                        int colour = DyeColor.byId(i).getTextureDiffuseColor() & 0xFF_FF_FF;
                        GuiDraw.fill(x0, y0, x0 + scale, y0 + scale, 0x88_00_00_00 | colour);
                    }
                }
            }
        }

        // The zone planner itself, and the hovered block
        double tx = mapCentreX() + (tilePos.getX() - positionX) * scale;
        double ty = mapCentreY() + (tilePos.getZ() - positionZ) * scale;
        GuiDraw.fill(tx, ty, tx + scale, ty + scale, 0xFF_FF_FF_FF);
        if (lastSelected != null) {
            double sx = mapCentreX() + (lastSelected.getX() - positionX) * scale;
            double sy = mapCentreY() + (lastSelected.getZ() - positionZ) * scale;
            GuiDraw.fill(sx, sy, sx + scale, sy + scale, 0x77_00_00_00);
        }
        GuiDraw.disableScissor();

        if (lastSelected != null) {
            String text = "X: " + lastSelected.getX() + " Y: " + lastSelected.getY() + " Z: " + lastSelected.getZ();
            GuiDraw.text(fontRenderer, text, guiLeft + 130, guiTop + 130, 0x404040, false);
        }
    }
}
