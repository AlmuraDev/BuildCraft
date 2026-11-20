/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.client.render;

import java.util.List;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import buildcraft.lib.client.render.laser.LaserBoxRenderer;
import buildcraft.lib.client.render.laser.LaserData_BC8;
import buildcraft.lib.client.render.laser.LaserRenderer_BC8;
import buildcraft.lib.client.render.tile.RecordedRenderState;
import buildcraft.lib.misc.VecUtil;

import buildcraft.builders.tile.TileBuilder;
import buildcraft.core.client.BuildCraftLaserManager;

/** The builder's box, its path and its robot (1.12.2's FastTESR). */
@OnlyIn(Dist.CLIENT)
public class RenderBuilder implements BlockEntityRenderer<TileBuilder, BuilderRenderState> {
    private static final double OFFSET = 0.1;

    @Override
    public BuilderRenderState createRenderState() {
        return new BuilderRenderState();
    }

    @Override
    public void extractRenderState(TileBuilder tile, BuilderRenderState state, float partialTicks,
        Vec3 cameraPosition, @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);
        state.reset();
        PoseStack.Pose pose = RecordedRenderState.worldPose(tile.getBlockPos());

        LaserBoxRenderer.renderLaserBoxDynamic(tile.getBox(), BuildCraftLaserManager.STRIPES_WRITE, state.translucent,
            pose, true);

        List<BlockPos> path = tile.path;
        if (path != null) {
            BlockPos last = null;
            for (BlockPos p : path) {
                if (last != null) {
                    Vec3 from = Vec3.atCenterOf(last);
                    Vec3 to = Vec3.atCenterOf(p);
                    LaserData_BC8 data = new LaserData_BC8(BuildCraftLaserManager.STRIPES_WRITE_DIRECTION,
                        offset(from, to), offset(to, from), 1 / 16.1);
                    LaserRenderer_BC8.renderLaserDynamic(data, state.translucent, pose);
                }
                last = p;
            }
        }

        if (tile.getBuilder() != null && tile.getLevel() != null) {
            RenderSnapshotBuilder.extract(tile.getBuilder(), tile.getLevel(), tile.getBlockPos(), partialTicks, state);
        }
        state.finish();
    }

    private static Vec3 offset(Vec3 from, Vec3 to) {
        Vec3 dir = to.subtract(from).normalize();
        return from.add(VecUtil.scale(dir, OFFSET));
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
    public AABB getRenderBoundingBox(TileBuilder tile) {
        return AABB.INFINITE;
    }
}
