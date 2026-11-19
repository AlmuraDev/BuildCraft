/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.core.client.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableSet;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.phys.Vec3;

import buildcraft.lib.client.render.WorldRenderContext;
import buildcraft.lib.client.render.laser.LaserData_BC8;
import buildcraft.lib.client.render.laser.LaserData_BC8.LaserType;
import buildcraft.lib.client.render.laser.LaserRenderer_BC8;
import buildcraft.lib.misc.VecUtil;

import buildcraft.core.BCCoreConfig;
import buildcraft.core.client.BuildCraftLaserManager;
import buildcraft.core.marker.VolumeConnection;
import buildcraft.core.tile.TileMarkerVolume;

/** Draws a powered volume marker's signal lasers, along each axis that has no connection. */
public class RenderMarkerVolume implements BlockEntityRenderer<TileMarkerVolume, RenderMarkerVolume.State> {
    private static final double SCALE = 1 / 16.2; // smaller than normal lasers

    private static final LaserType LASER_TYPE = BuildCraftLaserManager.MARKER_VOLUME_SIGNAL;
    private static final Vec3 VEC_HALF = new Vec3(0.5, 0.5, 0.5);

    public static class State extends BlockEntityRenderState {
        final List<LaserData_BC8> lasers = new ArrayList<>();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return Math.max(64, BCCoreConfig.markerMaxDistance * 2);
    }

    @Override
    public void extractRenderState(TileMarkerVolume marker, State state, float partialTicks, Vec3 cameraPosition,
        @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(marker, state, partialTicks, cameraPosition, breakProgress);
        state.lasers.clear();
        if (!marker.isShowingSignals()) return;

        VolumeConnection volume = marker.getCurrentConnection();
        Set<Axis> taken = volume == null ? ImmutableSet.of() : volume.getConnectedAxis();

        Vec3 start = VecUtil.add(VEC_HALF, marker.getBlockPos());
        for (Direction face : Direction.values()) {
            if (taken.contains(face.getAxis())) {
                continue;
            }
            Vec3 end = VecUtil.offset(start, face, BCCoreConfig.markerMaxDistance);
            state.lasers.add(makeLaser(start, end, face.getAxis()));
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.lasers.isEmpty()) return;
        List<LaserData_BC8> lasers = new ArrayList<>(state.lasers);
        // The lasers are in world coordinates, and this pose is at the block
        poseStack.pushPose();
        poseStack.translate(-state.blockPos.getX(), -state.blockPos.getY(), -state.blockPos.getZ());
        collector.submitCustomGeometry(poseStack, WorldRenderContext.TRANSLUCENT_BLOCK_ATLAS, (pose, buffer) -> {
            for (LaserData_BC8 laser : lasers) {
                LaserRenderer_BC8.renderLaser(laser, buffer, pose);
            }
        });
        poseStack.popPose();
    }

    private static LaserData_BC8 makeLaser(Vec3 min, Vec3 max, Axis axis) {
        Direction faceForMin = VecUtil.getFacing(axis, true);
        Direction faceForMax = VecUtil.getFacing(axis, false);
        Vec3 one = offset(min, faceForMin);
        Vec3 two = offset(max, faceForMax);
        return new LaserData_BC8(LASER_TYPE, one, two, SCALE);
    }

    private static Vec3 offset(Vec3 vec, Direction face) {
        double by = 1 / 16.0;
        if (face == Direction.DOWN) {
            return vec.add(0, -by, 0);
        } else if (face == Direction.UP) {
            return vec.add(0, by, 0);
        } else if (face == Direction.EAST) {
            return vec.add(by, 0, 0);
        } else if (face == Direction.WEST) {
            return vec.add(-by, 0, 0);
        } else if (face == Direction.SOUTH) {
            return vec.add(0, 0, by);
        } else {// North
            return vec.add(0, 0, -by);
        }
    }
}
