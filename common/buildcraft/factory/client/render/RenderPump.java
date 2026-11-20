/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.client.render;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;


import buildcraft.lib.client.render.tile.RecordedRenderState;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;

import buildcraft.lib.client.render.laser.LaserData_BC8.LaserRow;
import buildcraft.lib.client.render.laser.LaserData_BC8.LaserType;
import buildcraft.lib.client.render.tile.RenderPartCube;
import buildcraft.lib.client.sprite.SpriteHolderRegistry;
import buildcraft.lib.client.sprite.SpriteHolderRegistry.SpriteHolder;

import buildcraft.factory.tile.TilePump;

/** The pump's power and status LEDs and its tube (1.12.2's FastTESR). */
public class RenderPump implements BlockEntityRenderer<TilePump, RecordedRenderState> {
    private static final int[] COLOUR_POWER = new int[16];
    private static final int COLOUR_STATUS_ON = 0xFF_77_DD_77; // a light green
    private static final int COLOUR_STATUS_OFF = 0xFF_1f_10_1b; // black-ish

    private static final int BLOCK_LIGHT_STATUS_ON = 0xF;
    private static final int BLOCK_LIGHT_STATUS_OFF = 0x0;

    private static final double POWER = 1.5 / 16.0;
    private static final double STATUS = 3.5 / 16.0;
    private static final double Y = 13.5 / 16.0;

    private static final RenderPartCube[] LED_POWER;
    private static final RenderPartCube[] LED_STATUS;
    private static final LaserType TUBE_LASER;

    static {
        for (int i = 0; i < COLOUR_POWER.length; i++) {
            int c = (i * 0x40) / COLOUR_POWER.length;
            int r = (i * 0xE0) / COLOUR_POWER.length + 0x1F;
            int colour = (0xFF << 24) + (c << 16) + (c << 8) + r;
            COLOUR_POWER[i] = colour;
        }
        LED_POWER = new RenderPartCube[4];
        LED_STATUS = new RenderPartCube[4];
        for (int i = 0; i < 4; i++) {
            Direction facing = Direction.from2DDataValue(i);
            final int dX, dZ;
            final double ledX, ledZ;
            if (facing.getAxis() == Axis.X) {
                dX = 0;
                dZ = facing.getAxisDirection().getStep();
                ledZ = 0.5;
                ledX = facing == Direction.EAST ? 15.6 / 16.0 : 0.4 / 16.0;
            } else {
                dX = -facing.getAxisDirection().getStep();
                dZ = 0;
                ledX = 0.5;
                ledZ = facing == Direction.SOUTH ? 15.6 / 16.0 : 0.4 / 16.0;
            }
            LED_POWER[i] = new RenderPartCube();
            LED_POWER[i].center.positiond(ledX + dX * POWER, Y, ledZ + dZ * POWER);
            LED_STATUS[i] = new RenderPartCube();
            LED_STATUS[i].center.positiond(ledX + dX * STATUS, Y, ledZ + dZ * STATUS);
        }

        SpriteHolder spriteTubeMiddle = SpriteHolderRegistry.getHolder("buildcraftfactory:blocks/pump/tube");
        LaserRow cap = new LaserRow(spriteTubeMiddle, 0, 8, 8, 16);
        LaserRow middle = new LaserRow(spriteTubeMiddle, 0, 0, 16, 8);
        LaserRow[] middles = { middle };
        TUBE_LASER = new LaserType(cap, middle, middles, null, cap);
    }

    @Override
    public void extractRenderState(TilePump tile, RecordedRenderState state, float partialTicks, Vec3 cameraPosition,
        @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);
        state.reset();

        float percentFilled = tile.getPercentFilledForRender();
        int powerColour = COLOUR_POWER[(int) (percentFilled * (COLOUR_POWER.length - 1))];

        boolean complete = tile.isComplete();
        int statusColour = complete ? COLOUR_STATUS_OFF : COLOUR_STATUS_ON;
        int statusLight = complete ? BLOCK_LIGHT_STATUS_OFF : BLOCK_LIGHT_STATUS_ON;

        for (int i = 0; i < 4; i++) {
            // Get the light level of a direction
            Direction dir = Direction.from2DDataValue(i);
            BlockPos pos = tile.getBlockPos().relative(dir);
            int block = tile.getLevel().getBrightness(LightLayer.BLOCK, pos);
            int sky = tile.getLevel().getBrightness(LightLayer.SKY, pos);

            LED_POWER[i].setWhiteTex();
            LED_STATUS[i].setWhiteTex();
            LED_POWER[i].center.colouri(powerColour);
            LED_STATUS[i].center.colouri(statusColour);
            LED_POWER[i].center.lighti(block, sky);
            LED_STATUS[i].center.lighti(Math.max(statusLight, block), sky);
            LED_POWER[i].render(state.cutout);
            LED_STATUS[i].render(state.cutout);
        }

        RenderTube.renderTube(tile, TUBE_LASER, partialTicks, state.cutout);
        state.finish();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(TilePump tile) {
        return AABB.INFINITE;
    }

    @Override
    public RecordedRenderState createRenderState() {
        return new RecordedRenderState();
    }

    @Override
    public void submit(RecordedRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
        CameraRenderState camera) {
        state.submit(poseStack, collector);
    }
}
