/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.client.render;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;


import buildcraft.lib.client.render.tile.RecordedRenderState;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import net.neoforged.neoforge.fluids.FluidStack;

import buildcraft.lib.block.BlockBCBase_Neptune;
import buildcraft.lib.client.render.OffsetVertexConsumer;
import buildcraft.lib.client.render.fluid.FluidRenderer;
import buildcraft.lib.client.render.fluid.FluidRenderer.TankSize;
import buildcraft.lib.client.render.fluid.FluidSpriteType;
import buildcraft.lib.fluid.FluidSmoother;
import buildcraft.lib.fluid.FluidSmoother.FluidStackInterp;
import buildcraft.lib.misc.VecUtil;

import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.tile.TileHeatExchange;
import buildcraft.factory.tile.TileHeatExchange.EnumProgressState;
import buildcraft.factory.tile.TileHeatExchange.ExchangeSectionEnd;
import buildcraft.factory.tile.TileHeatExchange.ExchangeSectionStart;

/** The fluids in a heat exchanger and the flow between its ends, all drawn by the start block. */
public class RenderHeatExchange implements BlockEntityRenderer<TileHeatExchange, RecordedRenderState> {
    private static final Map<Direction, TankSideData> TANK_SIDES = new EnumMap<>(Direction.class);
    private static final TankSize TANK_BOTTOM, TANK_TOP;

    static {
        double s = 1 / 64.0;
        TANK_BOTTOM = new TankSize(2, 0, 2, 14, 2, 14).shrink(s, 0, s);
        TANK_TOP = new TankSize(2, 14, 2, 14, 16, 14).shrink(s, 0, s);
        TankSize start = new TankSize(0, 4, 4, 2, 12, 12).shrink(0, s, s);
        TankSize end = new TankSize(14, 4, 4, 16, 12, 12).shrink(0, s, s);
        TankSideData sides = new TankSideData(start, end);
        Direction face = Direction.EAST;
        for (int i = 0; i < 4; i++) {
            TANK_SIDES.put(face, sides);
            face = face.getClockWise();
            sides = sides.rotateY();
        }
    }

    static class TankSideData {
        public final TankSize start, end;

        public TankSideData(TankSize start, TankSize end) {
            this.start = start;
            this.end = end;
        }

        public TankSideData rotateY() {
            return new TankSideData(start.rotateY(), end.rotateY());
        }
    }

    @Override
    public void extractRenderState(TileHeatExchange tile, RecordedRenderState state, float partialTicks,
        Vec3 cameraPosition, @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);
        state.reset();
        BlockState blockState = tile.getBlockState();
        if (tile.isStart() && blockState.getBlock() == BCFactoryBlocks.heatExchange) {
            render(tile, blockState, state.lightCoords, partialTicks, state.translucent);
        }
        state.finish();
    }

    private static void render(TileHeatExchange tile, BlockState blockState, int combinedLight, float partialTicks,
        VertexConsumer out) {
        ExchangeSectionStart section = (ExchangeSectionStart) tile.getSection();
        ExchangeSectionEnd sectionEnd = section.getEndSection();

        Direction face = blockState.getValue(BlockBCBase_Neptune.PROP_FACING).getCounterClockWise();
        TankSideData sideTank = TANK_SIDES.get(face);
        renderTank(TANK_BOTTOM, section.smoothedTankInput, combinedLight, partialTicks, out);
        renderTank(sideTank.start, section.smoothedTankOutput, combinedLight, partialTicks, out);
        int middles = section.middleCount;
        if (sectionEnd != null) {
            BlockPos diff = sectionEnd.getTile().getBlockPos().subtract(tile.getBlockPos());
            VertexConsumer atEnd = new OffsetVertexConsumer(out, diff.getX(), diff.getY(), diff.getZ());
            renderTank(TANK_TOP, sectionEnd.smoothedTankOutput, combinedLight, partialTicks, atEnd);
            renderTank(sideTank.end, sectionEnd.smoothedTankInput, combinedLight, partialTicks, atEnd);
        }

        if (middles > 0 && sectionEnd != null) {
            EnumProgressState progressState = section.getProgressState();
            double progress = section.getProgress(partialTicks);
            if (progress > 0) {
                double length = middles + 2 - 4 / 16.0 - 0.02;
                double p0 = 2 / 16.0 + 0.01;
                double p1 = p0 + length - 0.01;
                double progressStart = p0;
                double progressEnd = p0 + length * progress;
                boolean flip = progressState == EnumProgressState.PREPARING;
                flip ^= face.getAxisDirection() == AxisDirection.NEGATIVE;
                if (flip) {
                    progressStart = p1 - length * progress;
                    progressEnd = p1;
                }
                BlockPos diff = BlockPos.ZERO;
                if (face.getAxisDirection() == AxisDirection.NEGATIVE) {
                    diff = diff.relative(face, middles + 1);
                }
                double otherStart = flip ? p0 : p1 - length * progress;
                double otherEnd = flip ? p0 + length * progress : p1;
                Vec3 vDiff = Vec3.atLowerCornerOf(diff);
                renderFlow(vDiff, face, out, progressStart + 0.01, progressEnd - 0.01,
                    sectionEnd.smoothedTankInput.getFluidForRender(), 4, partialTicks);
                renderFlow(vDiff, face.getOpposite(), out, otherStart, otherEnd,
                    section.smoothedTankInput.getFluidForRender(), 2, partialTicks);
            }
        }
    }

    private static void renderTank(TankSize size, FluidSmoother tank, int combinedLight, float partialTicks,
        VertexConsumer out) {
        FluidStackInterp fluid = tank.getFluidForRender(partialTicks);
        if (fluid == null || fluid.amount <= 0) {
            return;
        }
        int blockLight = fluid.fluid.getFluid().getFluidType().getLightLevel(fluid.fluid) & 0xF;
        FluidRenderer.vertex.lighti(Math.max(blockLight, LightCoordsUtil.block(combinedLight)),
            LightCoordsUtil.sky(combinedLight));
        FluidRenderer.renderFluid(FluidSpriteType.STILL, fluid.fluid, fluid.amount, tank.getCapacity(), size.min,
            size.max, out, null);
    }

    private static void renderFlow(Vec3 diff, Direction face, VertexConsumer out, double s, double e,
        FluidStack fluid, int point, float partialTicks) {
        double tickTime = Minecraft.getInstance().level.getGameTime();
        double offset = (tickTime + partialTicks) % 31 / 31.0;
        if (face.getAxisDirection() == AxisDirection.NEGATIVE) {
            offset = -offset;
            face = face.getOpposite();
        }
        Vec3 dirVec = Vec3.atLowerCornerOf(face.getUnitVec3i());
        double ds = (point + 0.1) / 16.0;
        Vec3 vs = new Vec3(ds, ds, ds);
        Vec3 ve = new Vec3(1 - ds, 1 - ds, 1 - ds);
        diff = diff.subtract(VecUtil.scale(dirVec, offset));
        s += offset;
        e += offset;
        if (s < 0) {
            s++;
            e++;
            diff = diff.subtract(dirVec);
        }
        for (int i = 0; i <= e; i++) {
            Vec3 d = diff;
            diff = diff.add(dirVec);
            if (i < s - 1) {
                continue;
            }
            double s1 = s < i ? 0 : (s % 1);
            double e1 = e > i + 1 ? 1 : (e % 1);
            vs = VecUtil.replaceValue(vs, face.getAxis(), s1);
            ve = VecUtil.replaceValue(ve, face.getAxis(), e1);
            boolean[] sides = new boolean[6];
            Arrays.fill(sides, true);
            if (s < i) {
                sides[face.getOpposite().ordinal()] = false;
            }
            if (e > i + 1) {
                sides[face.ordinal()] = false;
            }
            FluidRenderer.renderFluid(FluidSpriteType.FROZEN, fluid, 1, 1, vs, ve,
                new OffsetVertexConsumer(out, d.x, d.y, d.z), sides);
        }
    }

    @Override
    public AABB getRenderBoundingBox(TileHeatExchange tile) {
        // The start block draws the whole exchanger (up to 7 blocks long)
        return tile.isStart() ? new AABB(tile.getBlockPos()).inflate(8) : new AABB(tile.getBlockPos());
    }

    @Override
    public RecordedRenderState createRenderState() {
        return new RecordedRenderState();
    }

    @Override
    public void submit(RecordedRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
        CameraRenderState camera) {
        state.submit(poseStack, collector);
    }
}
