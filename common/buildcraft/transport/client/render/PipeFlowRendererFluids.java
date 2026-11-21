/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.util.LightCoordsUtil;

import buildcraft.lib.misc.MathUtil;
import buildcraft.api.transport.pipe.IPipeRenderOutput;

import buildcraft.lib.client.render.OffsetVertexConsumer;
import java.util.Arrays;


import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.fml.LogicalSide;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.transport.pipe.IPipeFlowRenderer;

import buildcraft.lib.client.render.fluid.FluidRenderer;
import buildcraft.lib.client.render.fluid.FluidSpriteType;
import buildcraft.lib.misc.VecUtil;

import buildcraft.transport.pipe.Pipe;
import buildcraft.transport.pipe.flow.PipeFlowFluids;

public enum PipeFlowRendererFluids implements IPipeFlowRenderer<PipeFlowFluids> {
    INSTANCE;

    @Override
    public void render(PipeFlowFluids flow, float partialTicks, IPipeRenderOutput out) {
        FluidStack forRender = flow.getFluidStackForRender();
        if (forRender == null) {
            return;
        }

        boolean[] sides = new boolean[6];
        Arrays.fill(sides, true);

        double[] amounts = flow.getAmountsForRender(partialTicks);
        // 1.12.2 slid the fluid's texture along with its flow offset. On 26.3 the fluid is the animated still sprite
        // in the shared block atlas, and a shifted cuboid maps part of its faces outside that sprite (other textures
        // flash through), so the texture stays put; the sprite's own animation shows the movement.
        Vec3[] offsets = new Vec3[EnumPipePart.VALUES.length];

        int blocklight = forRender.getFluid().getFluidType().getLightLevel(forRender);
        int light = out.light();
        int combinedLight = LightCoordsUtil.pack(Math.max(blocklight, LightCoordsUtil.block(light)), LightCoordsUtil.sky(light));

        FluidRenderer.vertex.lighti(combinedLight);
        VertexConsumer translucent = out.translucent();

        boolean gas = forRender.getFluid().getFluidType().isLighterThanAir();
        boolean horizontal = false;
        boolean vertical = flow.pipe.isConnected(gas ? Direction.DOWN : Direction.UP);

        for (Direction face : Direction.values()) {
            double size = ((Pipe) flow.pipe).getConnectedDist(face);
            double amount = MathUtil.clamp(amounts[face.get3DDataValue()], 0, flow.capacity);
            if (face.getAxis() != Axis.Y) {
                horizontal |= flow.pipe.isConnected(face) && amount > 0;
            }

            Vec3 center = VecUtil.offset(new Vec3(0.5, 0.5, 0.5), face, 0.245 + size / 2);
            Vec3 radius = new Vec3(0.24, 0.24, 0.24);
            radius = VecUtil.replaceValue(radius, face.getAxis(), 0.005 + size / 2);

            if (face.getAxis() == Axis.Y) {
                double perc = amount / flow.capacity;
                perc = Math.sqrt(perc);
                radius = new Vec3(perc * 0.24, radius.y, perc * 0.24);
            }

            Vec3 offset = offsets[face.get3DDataValue()];
            if (offset == null) offset = Vec3.ZERO;
            center = center.add(offset);
            // The fluid moves with its offset, its texture doesn't (1.12.2 shifted the buffer back)
            VertexConsumer fluidBuffer = new OffsetVertexConsumer(translucent, -offset.x, -offset.y, -offset.z);

            Vec3 min = center.subtract(radius);
            Vec3 max = center.add(radius);

            if (face.getAxis() == Axis.Y) {
                FluidRenderer.renderFluid(FluidSpriteType.FROZEN, forRender, 1, 1, min, max, fluidBuffer, sides);
            } else {
                FluidRenderer.renderFluid(FluidSpriteType.FROZEN, forRender, amount, flow.capacity, min, max,
                    fluidBuffer, sides);
            }
        }

        double amount = MathUtil.clamp(amounts[EnumPipePart.CENTER.getIndex()], 0, flow.capacity);

        double horizPos = 0.26;

        Vec3 offset = offsets[EnumPipePart.CENTER.getIndex()];
        if (offset == null) offset = Vec3.ZERO;
        VertexConsumer fluidBuffer = new OffsetVertexConsumer(translucent, -offset.x, -offset.y, -offset.z);

        if (horizontal | !vertical) {
            Vec3 min = new Vec3(0.26, 0.26, 0.26);
            Vec3 max = new Vec3(0.74, 0.74, 0.74);

            min = min.add(offset);
            max = max.add(offset);

            FluidRenderer.renderFluid(FluidSpriteType.FROZEN, forRender, amount, flow.capacity, min, max,
                fluidBuffer, sides);
            horizPos += (max.y - min.y) * amount / flow.capacity;
        }

        if (vertical && horizPos < 0.74) {
            double perc = amount / flow.capacity;
            perc = Math.sqrt(perc);
            double minXZ = 0.5 - 0.24 * perc;
            double maxXZ = 0.5 + 0.24 * perc;

            double yMin = gas ? 0.26 : horizPos;
            double yMax = gas ? 1 - horizPos : 0.74;

            Vec3 min = new Vec3(minXZ, yMin, minXZ);
            Vec3 max = new Vec3(maxXZ, yMax, maxXZ);
            min = min.add(offset);
            max = max.add(offset);

            FluidRenderer.renderFluid(FluidSpriteType.FROZEN, forRender, 1, 1, min, max, fluidBuffer, sides);
        }

        FluidRenderer.vertex.lighti(0xF, 0xF);
    }
}
