/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.client.render;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;


import buildcraft.api.transport.pipe.IPipeBehaviourRenderer;
import buildcraft.api.transport.pipe.IPipeFlowRenderer;
import buildcraft.api.transport.pipe.IPipeRenderOutput;
import buildcraft.api.transport.pipe.PipeBehaviour;
import buildcraft.api.transport.pipe.PipeFlow;
import buildcraft.api.transport.pluggable.IPlugDynamicRenderer;
import buildcraft.api.transport.pluggable.PipePluggable;

import buildcraft.lib.client.render.ItemRenderUtil;
import buildcraft.lib.client.render.WorldRenderContext;

import buildcraft.transport.client.PipeRegistryClient;
import buildcraft.transport.pipe.Pipe;
import buildcraft.transport.tile.TilePipeHolder;

/** Draws a pipe's dynamic parts: wires, dynamic pluggables, and the flow and behaviour contents (1.12.2's FastTESR).
 * The renderers write while the render state is extracted; the recorded geometry is replayed when submitting. */
public class RenderPipeHolder implements BlockEntityRenderer<TilePipeHolder, RenderPipeHolder.PipeRenderState> {

    private static final double MAX_DISTANCE_SQ = 64;

    public static class PipeRenderState extends BlockEntityRenderState implements IPipeRenderOutput {
        WorldRenderContext.Recording cutout = new WorldRenderContext.Recording();
        WorldRenderContext.Recording translucent = new WorldRenderContext.Recording();
        final List<ItemRenderUtil.PlacedItem> items = new ArrayList<>();
        @Nullable
        TilePipeHolder pipe;

        void reset() {
            cutout = new WorldRenderContext.Recording();
            translucent = new WorldRenderContext.Recording();
            items.clear();
        }

        @Override
        public VertexConsumer cutout() {
            return cutout;
        }

        @Override
        public VertexConsumer translucent() {
            return translucent;
        }

        @Override
        public int light() {
            return lightCoords;
        }

        @Override
        public void addItem(ItemStack stack, Vec3 position, @Nullable Direction facing, int count) {
            ItemRenderUtil.extractItemStack(items, pipe == null ? null : pipe.getLevel(), position.x, position.y,
                position.z, stack, count, facing);
        }
    }

    @Override
    public PipeRenderState createRenderState() {
        return new PipeRenderState();
    }

    @Override
    public void extractRenderState(TilePipeHolder pipe, PipeRenderState state, float partialTicks,
        Vec3 cameraPosition, @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(pipe, state, partialTicks, cameraPosition, breakProgress);
        state.reset();
        if (Vec3.atCenterOf(pipe.getPipePos()).distanceToSqr(cameraPosition) > MAX_DISTANCE_SQ) {
            return;
        }
        state.pipe = pipe;
        try {
            PipeWireRenderer.renderWires(pipe, state.cutout, state.lightCoords);
            for (Direction face : Direction.values()) {
                PipePluggable plug = pipe.getPluggable(face);
                if (plug != null) {
                    renderPlug(plug, partialTicks, state);
                }
            }
            Pipe p = pipe.getPipe();
            if (p != null) {
                if (p.flow != null) {
                    renderFlow(p.flow, partialTicks, state);
                }
                if (p.behaviour != null) {
                    renderBehaviour(p.behaviour, partialTicks, state);
                }
            }
        } finally {
            state.pipe = null;
            state.cutout.finish();
            state.translucent.finish();
        }
    }

    @Override
    public void submit(PipeRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
        CameraRenderState camera) {
        WorldRenderContext.Recording cutout = state.cutout;
        WorldRenderContext.Recording translucent = state.translucent;
        if (!cutout.isEmpty()) {
            collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS),
                (pose, buffer) -> cutout.replay(buffer, pose));
        }
        if (!translucent.isEmpty()) {
            collector.submitCustomGeometry(poseStack, WorldRenderContext.FLUID_BLOCK_ATLAS,
                (pose, buffer) -> translucent.replay(buffer, pose));
        }
        if (!state.items.isEmpty()) {
            ItemRenderUtil.submitItems(state.items, poseStack, collector, state.lightCoords);
        }
    }

    private static <P extends PipePluggable> void renderPlug(P plug, float partialTicks, IPipeRenderOutput out) {
        IPlugDynamicRenderer<P> renderer = PipeRegistryClient.getPlugRenderer(plug);
        if (renderer != null) {
            renderer.render(plug, partialTicks, out);
        }
    }

    private static <F extends PipeFlow> void renderFlow(F flow, float partialTicks, IPipeRenderOutput out) {
        IPipeFlowRenderer<F> renderer = PipeRegistryClient.getFlowRenderer(flow);
        if (renderer != null) {
            renderer.render(flow, partialTicks, out);
        }
    }

    private static <B extends PipeBehaviour> void renderBehaviour(B behaviour, float partialTicks,
        IPipeRenderOutput out) {
        IPipeBehaviourRenderer<B> renderer = PipeRegistryClient.getBehaviourRenderer(behaviour);
        if (renderer != null) {
            renderer.render(behaviour, partialTicks, out);
        }
    }
}
