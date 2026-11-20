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

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import buildcraft.lib.client.render.WorldRenderContext;

/** A block entity render state for renderers that write their geometry while the state is extracted (as 1.12.2's
 * TESRs wrote straight into a buffer). Vertices go into {@link #cutout} or {@link #translucent} in the entity format,
 * relative to the block, on the block atlas; {@link #submit} replays them. */
@OnlyIn(Dist.CLIENT)
public class RecordedRenderState extends BlockEntityRenderState {
    /** The pose to write vertices with (they are relative to the block; the real pose is applied when submitting). */
    public static final PoseStack.Pose IDENTITY = new PoseStack().last();

    public WorldRenderContext.Recording cutout = new WorldRenderContext.Recording();
    public WorldRenderContext.Recording translucent = new WorldRenderContext.Recording();

    /** Starts a new frame (call at the start of extraction). */
    public void reset() {
        cutout = new WorldRenderContext.Recording();
        translucent = new WorldRenderContext.Recording();
    }

    /** Ends the frame (call at the end of extraction). */
    public void finish() {
        cutout.finish();
        translucent.finish();
    }

    public void submit(PoseStack poseStack, SubmitNodeCollector collector) {
        WorldRenderContext.Recording c = cutout;
        WorldRenderContext.Recording t = translucent;
        if (!c.isEmpty()) {
            collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS),
                (pose, buffer) -> c.replay(buffer, pose));
        }
        if (!t.isEmpty()) {
            collector.submitCustomGeometry(poseStack, WorldRenderContext.TRANSLUCENT_BLOCK_ATLAS,
                (pose, buffer) -> t.replay(buffer, pose));
        }
    }
}
