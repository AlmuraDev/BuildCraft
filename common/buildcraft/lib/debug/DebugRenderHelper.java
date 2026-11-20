/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.debug;

import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;

import net.neoforged.fml.LogicalSide;

import buildcraft.lib.client.model.ModelUtil;
import buildcraft.lib.client.render.WorldRenderContext;
import buildcraft.lib.client.sprite.SpriteHolderRegistry;
import buildcraft.lib.client.model.MutableQuad;
import buildcraft.lib.debug.IDetachedRenderer;

public enum DebugRenderHelper implements IDetachedRenderer {
    INSTANCE;

    private static final MutableQuad[] smallCuboid;

    static {
        smallCuboid = new MutableQuad[6];
        Vector3f center = new Vector3f(0.5f, 0.5f, 0.5f);
        Vector3f radius = new Vector3f(0.25f, 0.25f, 0.25f);

        for (Direction face : Direction.values()) {
            MutableQuad quad = ModelUtil.createFace(face, center, radius, null);
            quad.lightf(1, 1);
            smallCuboid[face.ordinal()] = quad;
        }
    }

    @Override
    public void render(Player player, float partialTicks) {
        IAdvDebugTarget target = BCAdvDebugging.INSTANCE.targetClient;
        if (target == null) {
            return;
        } else if (!target.doesExistInWorld()) {
            // targetClient = null;
            // return;
        }
        IDetachedRenderer renderer = target.getDebugRenderer();
        if (renderer != null) {
            renderer.render(player, partialTicks);
        }
    }

    /** Draws a box in world coordinates into the current detached render pass. */
    public static void renderAABB(AABB aabb, int colour) {
        PoseStack stack = WorldRenderContext.poseStack();
        VertexConsumer bb = WorldRenderContext.buffer(WorldRenderContext.TRANSLUCENT_BLOCK_ATLAS);
        if (stack == null || bb == null) return;
        for (Direction face : Direction.values()) {
            MutableQuad quad = ModelUtil.createFace(
                face,
                new Vector3f(
                    (float) aabb.getCenter().x,
                    (float) aabb.getCenter().y,
                    (float) aabb.getCenter().z
                ),
                new Vector3f(
                    (float) (aabb.maxX - aabb.minX) / 2,
                    (float) (aabb.maxY - aabb.minY) / 2,
                    (float) (aabb.maxZ - aabb.minZ) / 2
                ),
                null
            );
            quad.lightf(1, 1);
            quad.texFromSprite(SpriteHolderRegistry.whiteSprite());
            quad.colouri(colour);
            quad.render(bb, stack.last(), OverlayTexture.NO_OVERLAY);
        }
    }

    /** Draws a small cube in the middle of the block into the current detached render pass. */
    public static void renderSmallCuboid(BlockPos pos, int colour) {
        PoseStack stack = WorldRenderContext.poseStack();
        VertexConsumer bb = WorldRenderContext.buffer(WorldRenderContext.TRANSLUCENT_BLOCK_ATLAS);
        if (stack == null || bb == null) return;
        stack.pushPose();
        stack.translate(pos.getX(), pos.getY(), pos.getZ());
        for (MutableQuad q : smallCuboid) {
            q.texFromSprite(SpriteHolderRegistry.whiteSprite());
            q.colouri(colour);
            q.render(bb, stack.last(), OverlayTexture.NO_OVERLAY);
        }
        stack.popPose();
    }
}
