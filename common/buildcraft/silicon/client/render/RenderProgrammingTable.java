/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.client.render;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;


import buildcraft.lib.client.render.tile.RecordedRenderState;
import buildcraft.lib.client.sprite.SpriteHolderRegistry;

import buildcraft.silicon.tile.TileProgrammingTable_Neptune;

/** The glass pane over the (dev only) programming table. */
public class RenderProgrammingTable implements BlockEntityRenderer<TileProgrammingTable_Neptune, RecordedRenderState> {
    private static final Identifier GLASS = Identifier.withDefaultNamespace("block/white_stained_glass");

    @Override
    public RecordedRenderState createRenderState() {
        return new RecordedRenderState();
    }

    @Override
    public void extractRenderState(TileProgrammingTable_Neptune tile, RecordedRenderState state, float partialTicks,
        Vec3 cameraPosition, @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);
        state.reset();
        TextureAtlasSprite sprite = SpriteHolderRegistry.lookupSprite(GLASS);
        int light = state.lightCoords;
        VertexConsumer out = state.translucent;
        vertex(out, 4, 4, sprite, light);
        vertex(out, 12, 4, sprite, light);
        vertex(out, 12, 12, sprite, light);
        vertex(out, 4, 12, sprite, light);
        state.finish();
    }

    private static void vertex(VertexConsumer out, int x, int z, TextureAtlasSprite sprite, int light) {
        out.addVertex(x / 16f, 9 / 16f, z / 16f).setColor(0xFFFFFFFF).setUv(sprite.getU(x / 16f), sprite.getV(z / 16f))
            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 1, 0);
    }

    @Override
    public void submit(RecordedRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
        CameraRenderState camera) {
        state.submit(poseStack, collector);
    }
}
