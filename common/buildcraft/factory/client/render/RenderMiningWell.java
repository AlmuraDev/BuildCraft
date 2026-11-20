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

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import buildcraft.lib.client.render.tile.RecordedRenderState;

import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import buildcraft.api.properties.BuildCraftProperties;

import buildcraft.lib.client.render.laser.LaserData_BC8.LaserRow;
import buildcraft.lib.client.render.laser.LaserData_BC8.LaserType;
import buildcraft.lib.client.render.tile.RenderPartCube;
import buildcraft.lib.client.sprite.SpriteHolderRegistry;
import buildcraft.lib.client.sprite.SpriteHolderRegistry.SpriteHolder;

import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.tile.TileMiningWell;

/** The mining well's power and status LEDs and its tube (1.12.2's FastTESR). */
@OnlyIn(Dist.CLIENT)
public class RenderMiningWell implements BlockEntityRenderer<TileMiningWell, RecordedRenderState> {
    private static final int[] COLOUR_POWER = new int[16];
    private static final int COLOUR_STATUS_ON = 0xFF_77_DD_77; // a light green
    private static final int COLOUR_STATUS_OFF = 0xFF_1f_10_1b; // black-ish

    private static final int BLOCK_LIGHT_STATUS_ON = 0xF;
    private static final int BLOCK_LIGHT_STATUS_OFF = 0x0;

    private static final double POWER = 2.5 / 16.0;
    private static final double STATUS = 4.5 / 16.0;
    private static final double Y = 5.5 / 16.0;

    private static final RenderPartCube LED_POWER, LED_STATUS;
    private static final LaserType TUBE_LASER;

    static {
        for (int i = 0; i < COLOUR_POWER.length; i++) {
            int c = ((i * 0x40) / COLOUR_POWER.length) & 0xFF;
            int r = (((i * 0xB0) / COLOUR_POWER.length) & 0xFF) + 0x4F;
            int colour = (0xFF << 24) | (c << 16) | (c << 8) | r;
            COLOUR_POWER[i] = colour;
        }
        LED_POWER = new RenderPartCube();
        LED_STATUS = new RenderPartCube();

        SpriteHolder spriteTubeMiddle = SpriteHolderRegistry.getHolder("buildcraftfactory:blocks/mining_well/tube");
        LaserRow cap = new LaserRow(spriteTubeMiddle, 0, 8, 8, 16);
        LaserRow middle = new LaserRow(spriteTubeMiddle, 0, 0, 16, 8);
        LaserRow[] middles = { middle };
        TUBE_LASER = new LaserType(cap, middle, middles, null, cap);
    }

    @Override
    public void extractRenderState(TileMiningWell tile, RecordedRenderState state, float partialTicks,
        Vec3 cameraPosition, @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);
        state.reset();

        Direction facing = Direction.NORTH;
        BlockState blockState = tile.getBlockState();
        if (blockState.getBlock() == BCFactoryBlocks.miningWell) {
            facing = blockState.getValue(BuildCraftProperties.BLOCK_FACING);
        }

        final int dX, dZ;
        final double ledX, ledZ;
        if (facing.getAxis() == Axis.X) {
            dX = 0;
            dZ = facing.getAxisDirection().getStep();
            ledZ = 0.5;
            ledX = facing == Direction.EAST ? 15.8 / 16.0 : 0.2 / 16.0;
        } else {
            dX = -facing.getAxisDirection().getStep();
            dZ = 0;
            ledX = 0.5;
            ledZ = facing == Direction.SOUTH ? 15.8 / 16.0 : 0.2 / 16.0;
        }

        net.minecraft.core.BlockPos lightPos = tile.getBlockPos().relative(facing);
        int combinedLight = net.minecraft.util.LightCoordsUtil.pack(
            tile.getLevel().getBrightness(net.minecraft.world.level.LightLayer.BLOCK, lightPos),
            tile.getLevel().getBrightness(net.minecraft.world.level.LightLayer.SKY, lightPos));
        LED_POWER.setWhiteTex();
        LED_STATUS.setWhiteTex();
        LED_POWER.center.lighti(combinedLight);
        LED_STATUS.center.lighti(combinedLight);

        LED_POWER.center.positiond(ledX + dX * POWER, Y, ledZ + dZ * POWER);
        float percentFilled = tile.getPercentFilledForRender();
        int colourIndex = (int) (percentFilled * (COLOUR_POWER.length - 1));
        LED_POWER.center.colouri(COLOUR_POWER[colourIndex]);
        LED_POWER.center.maxLighti(percentFilled > 0.01 ? BLOCK_LIGHT_STATUS_ON : BLOCK_LIGHT_STATUS_OFF, 0);
        LED_POWER.render(state.cutout);

        LED_STATUS.center.positiond(ledX + dX * STATUS, Y, ledZ + dZ * STATUS);
        boolean complete = tile.isComplete();
        LED_STATUS.center.colouri(complete ? COLOUR_STATUS_OFF : COLOUR_STATUS_ON);
        LED_STATUS.center.maxLighti(complete ? BLOCK_LIGHT_STATUS_OFF : BLOCK_LIGHT_STATUS_ON, 0);
        LED_STATUS.render(state.cutout);

        RenderTube.renderTube(tile, TUBE_LASER, partialTicks, state.cutout);
        state.finish();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(TileMiningWell tile) {
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
