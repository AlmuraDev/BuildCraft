/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.energy.client.render;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;


import buildcraft.lib.client.model.MutableQuad;
import buildcraft.lib.client.render.tile.RenderEngine_BC8.EngineRenderState;

import buildcraft.energy.BCEnergyModels;
import buildcraft.energy.tile.TileDynamoMJ;

/** Draws the MJ dynamo from its variable model (1.12.2's FastTESR), the same way engines are drawn. */
public class RenderDynamoMJ implements BlockEntityRenderer<TileDynamoMJ, EngineRenderState> {

    @Override
    public EngineRenderState createRenderState() {
        return new EngineRenderState();
    }

    @Override
    public void extractRenderState(TileDynamoMJ dynamo, EngineRenderState state, float partialTicks,
        Vec3 cameraPosition, @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(dynamo, state, partialTicks, cameraPosition, breakProgress);
        state.quads.clear();
        int lightBlock = LightCoordsUtil.block(state.lightCoords);
        int lightSky = LightCoordsUtil.sky(state.lightCoords);
        for (MutableQuad q : BCEnergyModels.getMjDynamoQuads(dynamo, partialTicks)) {
            MutableQuad copy = new MutableQuad(q);
            copy.maxLighti(lightBlock, lightSky);
            copy.multShade();
            state.quads.add(copy);
        }
    }

    @Override
    public void submit(EngineRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
        CameraRenderState camera) {
        if (state.quads.isEmpty()) {
            return;
        }
        List<MutableQuad> quads = new ArrayList<>(state.quads);
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS),
            (pose, buffer) -> {
                for (MutableQuad q : quads) {
                    q.render(buffer, pose, OverlayTexture.NO_OVERLAY);
                }
            });
    }
}
