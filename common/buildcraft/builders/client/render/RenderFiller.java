/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.client.render;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;


import buildcraft.lib.client.render.laser.LaserBoxRenderer;
import buildcraft.lib.client.render.tile.RecordedRenderState;

import buildcraft.builders.tile.TileFiller;
import buildcraft.core.client.BuildCraftLaserManager;

/** The filler's robot and, when it took its area from markers, its box (1.12.2's FastTESR). */
public class RenderFiller implements BlockEntityRenderer<TileFiller, BuilderRenderState> {
    @Override
    public BuilderRenderState createRenderState() {
        return new BuilderRenderState();
    }

    @Override
    public void extractRenderState(TileFiller tile, BuilderRenderState state, float partialTicks,
        Vec3 cameraPosition, @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);
        state.reset();
        if (tile.getBuilder() != null && tile.getLevel() != null) {
            RenderSnapshotBuilder.extract(tile.getBuilder(), tile.getLevel(), tile.getBlockPos(), partialTicks, state);
        }
        if (tile.markerBox) {
            LaserBoxRenderer.renderLaserBoxDynamic(tile.box, BuildCraftLaserManager.STRIPES_WRITE, state.translucent,
                RecordedRenderState.worldPose(tile.getBlockPos()), true);
        }
        state.finish();
    }

    @Override
    public void submit(BuilderRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
        CameraRenderState camera) {
        state.submit(poseStack, collector);
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(TileFiller tile) {
        return AABB.INFINITE;
    }
}
