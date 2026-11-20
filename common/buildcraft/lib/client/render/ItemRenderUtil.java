/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.client.render;

import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import org.joml.Quaternionf;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;


import buildcraft.lib.BCLibConfig;

/** Draws small item stacks inside blocks (travelling items in pipes, builder snapshots). 26.3 draws from extracted render
 * state, so stacks are resolved with {@link #extractItemStack} while extracting and drawn with {@link #submitItems}. */
public final class ItemRenderUtil {

    private static final float SCALE = 0.30f;
    private static final Random modelOffsetRandom = new Random(0);

    private ItemRenderUtil() {}

    /** One resolved item model at a position relative to the block being drawn. */
    public static final class PlacedItem {
        final ItemStackRenderState state;
        final float x, y, z;
        final Quaternionf rotation;

        PlacedItem(ItemStackRenderState state, double x, double y, double z, Quaternionf rotation) {
            this.state = state;
            this.x = (float) x;
            this.y = (float) y;
            this.z = (float) z;
            this.rotation = rotation;
        }
    }

    /** Resolves a stack drawn at (x, y, z) facing dir, with up to five copies for bigger stacks (as 1.12.2 did). */
    public static void extractItemStack(List<PlacedItem> into, @Nullable Level level, double x, double y, double z,
        ItemStack stack, int stackCount, @Nullable Direction dir) {
        if (stack.isEmpty()) {
            return;
        }
        if (dir == null) {
            dir = Direction.EAST;
        }
        dir = BCLibConfig.rotateTravelingItems.changeFacing(dir);
        Quaternionf rotation = new Quaternionf().rotationTo(0, 0, 1, dir.getStepX(), dir.getStepY(), dir.getStepZ());

        ItemStackRenderState state = new ItemStackRenderState();
        Minecraft.getInstance().getItemModelResolver().updateForTopItem(state, stack, ItemDisplayContext.NONE, level,
            null, 0);
        if (state.isEmpty()) {
            return;
        }

        int count = getStackModelCount(stackCount);
        if (count > 1) {
            setupModelOffsetRandom(stack);
        }
        for (int i = 0; i < count; i++) {
            double dx = 0, dy = 0, dz = 0;
            if (i > 0) {
                dx = (modelOffsetRandom.nextFloat() * 2.0F - 1.0F) * 0.08F;
                dy = (modelOffsetRandom.nextFloat() * 2.0F - 1.0F) * 0.08F;
                dz = (modelOffsetRandom.nextFloat() * 2.0F - 1.0F) * 0.08F;
            }
            into.add(new PlacedItem(state, x + dx, y + dy, z + dz, rotation));
        }
    }

    public static void submitItems(List<PlacedItem> items, PoseStack poseStack, SubmitNodeCollector collector,
        int lightCoords) {
        for (PlacedItem item : items) {
            poseStack.pushPose();
            poseStack.translate(item.x, item.y, item.z);
            poseStack.rotate(item.rotation);
            poseStack.scale(SCALE, SCALE, SCALE);
            item.state.submit(poseStack, collector, lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
    }

    private static void setupModelOffsetRandom(ItemStack stack) {
        Identifier regName = BuiltInRegistries.ITEM.getKey(stack.getItem());
        int regNameSeed = regName.getNamespace().hashCode() ^ regName.getPath().hashCode();
        modelOffsetRandom.setSeed(regNameSeed & 0x7F_FF_FF_FF);
    }

    private static int getStackModelCount(int stackCount) {
        if (stackCount > 48) return 5;
        if (stackCount > 32) return 4;
        if (stackCount > 16) return 3;
        if (stackCount > 1) return 2;
        return 1;
    }
}
