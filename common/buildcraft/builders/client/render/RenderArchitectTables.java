/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.client.render;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;


import buildcraft.lib.client.model.ModelUtil;
import buildcraft.lib.client.render.WorldRenderContext;
import buildcraft.lib.client.sprite.SpriteHolderRegistry;
import buildcraft.lib.client.sprite.SpriteHolderRegistry.SpriteHolder;
import buildcraft.lib.debug.IDetachedRenderer;

import buildcraft.builders.client.ClientArchitectTables;

/** The fading "scanned" overlay on blocks an architect table has just read. 1.12.2 clipped it to the scanned box
 * with the stencil buffer; 26.3 has no stencil access here, so the overlay is only limited to blocks inside a box. */
public enum RenderArchitectTables implements IDetachedRenderer {
    INSTANCE;

    private static final SpriteHolder SCAN = SpriteHolderRegistry.getHolder("buildcraftbuilders:blocks/scan");

    @Override
    public void render(Player player, float partialTicks) {
        PoseStack stack = WorldRenderContext.poseStack();
        VertexConsumer buffer = WorldRenderContext.buffer(WorldRenderContext.TRANSLUCENT_BLOCK_ATLAS);
        if (stack == null || buffer == null || ClientArchitectTables.SCANNED_BLOCKS.isEmpty()) {
            return;
        }
        PoseStack.Pose pose = stack.last();
        List<AABB> boxes = new ArrayList<>(ClientArchitectTables.BOXES.keySet());
        List<BlockPos> poses = new ArrayList<>(ClientArchitectTables.SCANNED_BLOCKS.keySet());
        Vec3 eye = player.position();
        poses.sort(Comparator.<BlockPos>comparingDouble(pos -> Vec3.atLowerCornerOf(pos).distanceTo(eye)).reversed());

        ModelUtil.UvFaceData uvs = new ModelUtil.UvFaceData(SCAN.getInterpU(0), SCAN.getInterpV(0), SCAN.getInterpU(1),
            SCAN.getInterpV(1));
        for (BlockPos pos : poses) {
            AABB block = new AABB(pos);
            boolean inBox = false;
            for (AABB box : boxes) {
                if (box.intersects(block)) {
                    inBox = true;
                    break;
                }
            }
            if (!inBox) {
                continue;
            }
            int alpha = ClientArchitectTables.SCANNED_BLOCKS.get(pos) * 50
                / ClientArchitectTables.START_SCANNED_BLOCK_VALUE;
            for (Direction face : Direction.values()) {
                ModelUtil.createFace(face, new Vector3f(pos.getX() + 0.5F, pos.getY() + 0.5F, pos.getZ() + 0.5F),
                    new Vector3f(0.505F, 0.505F, 0.505F), uvs)
                    .lighti(15, 15)
                    .colouri(255, 255, 255, alpha)
                    .render(buffer, pose, OverlayTexture.NO_OVERLAY);
            }
        }
    }
}
