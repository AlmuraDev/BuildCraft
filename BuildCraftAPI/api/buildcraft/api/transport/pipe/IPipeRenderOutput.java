/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */
package buildcraft.api.transport.pipe;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** Where a pipe's dynamic renderers draw (26.3 draws from extracted render state, so they run while it is extracted).
 * Vertices are in the entity vertex format (position, colour, uv, overlay, light, normal), relative to the pipe's block,
 * on the block atlas. */
@OnlyIn(Dist.CLIENT)
public interface IPipeRenderOutput {
    VertexConsumer cutout();

    VertexConsumer translucent();

    /** @return The pipe's packed light. */
    int light();

    /** Draws an item stack (a travelling item) at the given position relative to the pipe's block. */
    void addItem(ItemStack stack, Vec3 position, @Nullable Direction facing, int count);
}
