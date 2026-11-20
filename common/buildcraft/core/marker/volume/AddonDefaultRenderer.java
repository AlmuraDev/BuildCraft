/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core.marker.volume;

import java.util.function.Supplier;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import buildcraft.lib.client.sprite.SpriteHolderRegistry;

/** Draws an addon as a full-bright textured cube (white when no sprite is given). */
@OnlyIn(Dist.CLIENT)
public class AddonDefaultRenderer<T extends Addon> implements IFastAddonRenderer<T> {
    private static final int FULL_BRIGHT = LightCoordsUtil.pack(15, 0);

    private final Supplier<TextureAtlasSprite> sprite;

    public AddonDefaultRenderer() {
        this(SpriteHolderRegistry::whiteSprite);
    }

    public AddonDefaultRenderer(Supplier<TextureAtlasSprite> sprite) {
        this.sprite = sprite;
    }

    @Override
    public void renderAddonFast(T addon, Player player, float partialTicks, VertexConsumer bb, PoseStack.Pose pose) {
        renderCube(bb, pose, addon.getBoundingBox(), sprite.get(), 255);
    }

    /** Draws a cube with 1.12.2's fixed per-face shading (top brightest, bottom darkest). */
    public static void renderCube(VertexConsumer b, PoseStack.Pose pose, AABB bb, TextureAtlasSprite s, int alpha) {
        float u0 = s.getU0(), u1 = s.getU1(), v0 = s.getV0(), v1 = s.getV1();
        // North (-Z)
        quad(b, pose, 204, alpha, 0, 0, -1, u0, v0, u1, v1, //
            bb.minX, bb.maxY, bb.minZ, bb.maxX, bb.maxY, bb.minZ, bb.maxX, bb.minY, bb.minZ, bb.minX, bb.minY, bb.minZ);
        // South (+Z)
        quad(b, pose, 204, alpha, 0, 0, 1, u0, v0, u1, v1, //
            bb.minX, bb.minY, bb.maxZ, bb.maxX, bb.minY, bb.maxZ, bb.maxX, bb.maxY, bb.maxZ, bb.minX, bb.maxY, bb.maxZ);
        // Down
        quad(b, pose, 127, alpha, 0, -1, 0, u0, v0, u1, v1, //
            bb.minX, bb.minY, bb.minZ, bb.maxX, bb.minY, bb.minZ, bb.maxX, bb.minY, bb.maxZ, bb.minX, bb.minY, bb.maxZ);
        // Up
        quad(b, pose, 255, alpha, 0, 1, 0, u0, v0, u1, v1, //
            bb.minX, bb.maxY, bb.maxZ, bb.maxX, bb.maxY, bb.maxZ, bb.maxX, bb.maxY, bb.minZ, bb.minX, bb.maxY, bb.minZ);
        // West (-X)
        quad(b, pose, 153, alpha, -1, 0, 0, u0, v0, u1, v1, //
            bb.minX, bb.minY, bb.maxZ, bb.minX, bb.maxY, bb.maxZ, bb.minX, bb.maxY, bb.minZ, bb.minX, bb.minY, bb.minZ);
        // East (+X)
        quad(b, pose, 153, alpha, 1, 0, 0, u0, v0, u1, v1, //
            bb.maxX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ, bb.maxX, bb.minY, bb.maxZ);
    }

    private static void quad(VertexConsumer b, PoseStack.Pose pose, int grey, int alpha, float nx, float ny, float nz,
        float u0, float v0, float u1, float v1, double x0, double y0, double z0, double x1, double y1, double z1,
        double x2, double y2, double z2, double x3, double y3, double z3) {
        vertex(b, pose, grey, alpha, nx, ny, nz, u0, v0, x0, y0, z0);
        vertex(b, pose, grey, alpha, nx, ny, nz, u0, v1, x1, y1, z1);
        vertex(b, pose, grey, alpha, nx, ny, nz, u1, v1, x2, y2, z2);
        vertex(b, pose, grey, alpha, nx, ny, nz, u1, v0, x3, y3, z3);
    }

    private static void vertex(VertexConsumer b, PoseStack.Pose pose, int grey, int alpha, float nx, float ny,
        float nz, float u, float v, double x, double y, double z) {
        b.addVertex(pose, (float) x, (float) y, (float) z)
            .setColor(grey, grey, grey, alpha)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(FULL_BRIGHT)
            .setNormal(pose, nx, ny, nz);
    }
}
