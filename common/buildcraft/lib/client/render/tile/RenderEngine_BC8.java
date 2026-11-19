/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.client.render.tile;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;

import buildcraft.lib.client.model.MutableQuad;
import buildcraft.lib.engine.TileEngineBase_BC8;

/** Draws an engine from its variable model, which moves with the engine's progress (1.12.2's FastTESR). The quads are
 * computed while extracting the render state, as the model reads the tile. */
public abstract class RenderEngine_BC8<T extends TileEngineBase_BC8>
    implements BlockEntityRenderer<T, RenderEngine_BC8.EngineRenderState> {

    public static class EngineRenderState extends BlockEntityRenderState {
        public final List<MutableQuad> quads = new ArrayList<>();
    }

    @Override
    public EngineRenderState createRenderState() {
        return new EngineRenderState();
    }

    @Override
    public void extractRenderState(T engine, EngineRenderState state, float partialTicks, Vec3 cameraPosition,
        @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(engine, state, partialTicks, cameraPosition, breakProgress);
        state.quads.clear();
        int lightBlock = LightCoordsUtil.block(state.lightCoords);
        int lightSky = LightCoordsUtil.sky(state.lightCoords);
        for (MutableQuad q : getEngineModel(engine, partialTicks)) {
            MutableQuad copy = new MutableQuad(q);
            copy.maxLighti(lightBlock, lightSky);
            copy.multShade();
            state.quads.add(copy);
        }
    }

    @Override
    public void submit(EngineRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.quads.isEmpty()) {
            return;
        }
        List<MutableQuad> quads = new ArrayList<>(state.quads);
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS), (pose, buffer) -> {
            for (MutableQuad q : quads) {
                q.render(buffer, pose, OverlayTexture.NO_OVERLAY);
            }
        });
    }

    protected abstract MutableQuad[] getEngineModel(T engine, float partialTicks);
}
