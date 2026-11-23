/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.client.render.tile;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlas;


import buildcraft.lib.client.render.WorldRenderContext;

/** A block entity render state for renderers that write their geometry while the state is extracted (as 1.12.2's
 * TESRs wrote straight into a buffer). Vertices go into {@link #cutout} or {@link #translucent} in the entity format,
 * relative to the block, on the block atlas; {@link #submit} replays them. */
public class RecordedRenderState extends BlockEntityRenderState {
    /** The pose to write vertices with (they are relative to the block; the real pose is applied when submitting). */
    public static final PoseStack.Pose IDENTITY = new PoseStack().last();

    /** A pose that turns world coordinates into coordinates relative to the given block (for renderers that
     * worked in world space in 1.12.2). */
    public static PoseStack.Pose worldPose(net.minecraft.core.BlockPos pos) {
        PoseStack stack = new PoseStack();
        stack.translate(-pos.getX(), -pos.getY(), -pos.getZ());
        return stack.last();
    }

    /** The packed block and sky light at a position (1.12.2's World.getCombinedLight, which is client-only). */
    public static int light(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        return net.minecraft.util.LightCoordsUtil.pack(
            level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, pos),
            level.getBrightness(net.minecraft.world.level.LightLayer.SKY, pos));
    }

    public WorldRenderContext.Recording cutout = new WorldRenderContext.Recording();
    public WorldRenderContext.Recording translucent = new WorldRenderContext.Recording();
    /** Fluid cuboids, drawn translucent with back faces culled ({@link WorldRenderContext#FLUID_BLOCK_ATLAS}). */
    public WorldRenderContext.Recording fluid = new WorldRenderContext.Recording();
    /** Cutout with back faces culled: for double-faced geometry (1.12.2's double-faced lasers relied on GL culling to
     * hide the reversed copy; without culling both copies z-fight). */
    public WorldRenderContext.Recording cutoutCull = new WorldRenderContext.Recording();

    /** Starts a new frame (call at the start of extraction). */
    public void reset() {
        cutout = new WorldRenderContext.Recording();
        translucent = new WorldRenderContext.Recording();
        fluid = new WorldRenderContext.Recording();
        cutoutCull = new WorldRenderContext.Recording();
    }

    /** Ends the frame (call at the end of extraction). */
    public void finish() {
        cutout.finish();
        translucent.finish();
        fluid.finish();
        cutoutCull.finish();
    }

    public void submit(PoseStack poseStack, SubmitNodeCollector collector) {
        WorldRenderContext.Recording c = cutout;
        WorldRenderContext.Recording t = translucent;
        WorldRenderContext.Recording f = fluid;
        WorldRenderContext.Recording cc = cutoutCull;
        if (!cc.isEmpty()) {
            collector.submitCustomGeometry(poseStack, RenderTypes.entityCutoutCull(TextureAtlas.LOCATION_BLOCKS),
                (pose, buffer) -> cc.replay(buffer, pose));
        }
        if (!c.isEmpty()) {
            collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS),
                (pose, buffer) -> c.replay(buffer, pose));
        }
        if (!t.isEmpty()) {
            collector.submitCustomGeometry(poseStack, WorldRenderContext.TRANSLUCENT_BLOCK_ATLAS,
                (pose, buffer) -> t.replay(buffer, pose));
        }
        if (!f.isEmpty()) {
            collector.submitCustomGeometry(poseStack, WorldRenderContext.FLUID_BLOCK_ATLAS,
                (pose, buffer) -> f.replay(buffer, pose));
        }
    }
}
