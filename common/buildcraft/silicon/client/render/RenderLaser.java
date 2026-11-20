/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.client.render;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import buildcraft.api.properties.BuildCraftProperties;

import buildcraft.lib.client.render.laser.LaserData_BC8;
import buildcraft.lib.client.render.laser.LaserRenderer_BC8;
import buildcraft.lib.client.render.tile.RecordedRenderState;

import buildcraft.core.client.BuildCraftLaserManager;
import buildcraft.core.item.ItemGoggles;
import buildcraft.silicon.BCSiliconBlocks;
import buildcraft.silicon.BCSiliconConfig;
import buildcraft.silicon.tile.TileLaser;

/** The laser beam to the table being powered (1.12.2's FastTESR), brighter for more power. */
@OnlyIn(Dist.CLIENT)
public class RenderLaser implements BlockEntityRenderer<TileLaser, RecordedRenderState> {
    private static final int MAX_POWER = BuildCraftLaserManager.POWERS.length - 1;

    @Override
    public RecordedRenderState createRenderState() {
        return new RecordedRenderState();
    }

    @Override
    public void extractRenderState(TileLaser tile, RecordedRenderState state, float partialTicks, Vec3 cameraPosition,
        @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);
        state.reset();
        if ((BCSiliconConfig.renderLaserBeams || isPlayerWearingGoggles()) && tile.laserPos != null
            && tile.getBlockState().getBlock() == BCSiliconBlocks.laser) {
            long avg = tile.getAverageClient();
            if (avg > 200_000) {
                avg += 200_000;
                Direction side = tile.getBlockState().getValue(BuildCraftProperties.BLOCK_FACING_6);
                Vec3 offset = new Vec3(0.5, 0.5, 0.5).add(Vec3.atLowerCornerOf(side.getUnitVec3i()).scale(4 / 16D));
                int index = (int) (avg * MAX_POWER / tile.getMaxPowerPerTick());
                if (index > MAX_POWER) {
                    index = MAX_POWER;
                }
                Vec3 origin = Vec3.atLowerCornerOf(tile.getBlockPos());
                LaserData_BC8 laser = new LaserData_BC8(BuildCraftLaserManager.POWERS[index], offset,
                    tile.laserPos.subtract(origin), 1 / 16D);
                LaserRenderer_BC8.renderLaserDynamic(laser, state.translucent, RecordedRenderState.IDENTITY);
            }
        }
        state.finish();
    }

    @Override
    public void submit(RecordedRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
        CameraRenderState camera) {
        state.submit(poseStack, collector);
    }

    @Override
    public AABB getRenderBoundingBox(TileLaser tile) {
        return tile.getRenderBox();
    }

    private static boolean isPlayerWearingGoggles() {
        return Minecraft.getInstance().player != null
            && Minecraft.getInstance().player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof ItemGoggles;
    }
}
