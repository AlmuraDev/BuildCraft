/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.client.render;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

import buildcraft.lib.debug.IDetachedRenderer;

/** Dispatches "detached renderer elements" - rendering that does not require a specific tile or entity in the world
 * (perhaps held item HUD elements). The renderers run while the level render state is extracted ({@link #extract}),
 * drawing through {@link WorldRenderContext}; what they drew is submitted with the rest of the frame
 * ({@link #submit}). */
@OnlyIn(Dist.CLIENT)
public enum DetachedRenderer {
    INSTANCE;

    public enum RenderMatrixType {
        /** Positions relative to the camera. */
        FROM_PLAYER,
        /** Positions in world coordinates. */
        FROM_WORLD_ORIGIN;
    }

    private final Map<RenderMatrixType, List<IDetachedRenderer>> renders = new EnumMap<>(RenderMatrixType.class);

    /** The last frame's recording, and the camera position it was made for. */
    private WorldRenderContext.Recording lastRecording;
    private Vec3 lastCamera = Vec3.ZERO;

    DetachedRenderer() {
        for (RenderMatrixType type : RenderMatrixType.values()) {
            renders.put(type, new ArrayList<>());
        }
    }

    public void addRenderer(RenderMatrixType type, IDetachedRenderer renderer) {
        renders.get(type).add(renderer);
    }

    /** Runs every renderer, recording their quads in world coordinates. */
    public void extract(Player player, Vec3 camera, float partialTicks) {
        WorldRenderContext.Recording recording = new WorldRenderContext.Recording();
        PoseStack poseStack = new PoseStack();
        for (RenderMatrixType type : RenderMatrixType.values()) {
            List<IDetachedRenderer> rendersForType = this.renders.get(type);
            if (rendersForType.isEmpty()) continue;
            poseStack.pushPose();
            if (type == RenderMatrixType.FROM_PLAYER) {
                poseStack.translate(camera.x, camera.y, camera.z);
            }
            WorldRenderContext.begin(poseStack, recording);
            try {
                for (IDetachedRenderer render : rendersForType) {
                    render.render(player, partialTicks);
                }
            } finally {
                WorldRenderContext.end();
                poseStack.popPose();
            }
        }
        recording.finish();
        lastRecording = recording.isEmpty() ? null : recording;
        lastCamera = camera;
    }

    /** Submits what {@link #extract} recorded. */
    public void submit(SubmitCustomGeometryEvent event) {
        WorldRenderContext.Recording recording = lastRecording;
        if (recording == null) {
            return;
        }
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-lastCamera.x, -lastCamera.y, -lastCamera.z);
        event.getSubmitNodeCollector().submitCustomGeometry(poseStack, WorldRenderContext.TRANSLUCENT_BLOCK_ATLAS,
            (pose, buffer) -> recording.replay(buffer, pose));
        poseStack.popPose();
    }
}
