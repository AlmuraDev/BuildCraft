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

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import buildcraft.lib.client.render.tile.RecordedRenderState;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.neoforge.fluids.FluidStack;

import buildcraft.lib.client.render.fluid.FluidRenderer;
import buildcraft.lib.client.render.fluid.FluidSpriteType;
import buildcraft.lib.fluid.FluidSmoother.FluidStackInterp;
import buildcraft.lib.misc.FluidUtilBC;

import buildcraft.factory.tile.TileTank;

/** The fluid in a tank, joined to the tanks above and below when they are full of the same fluid. */
@OnlyIn(Dist.CLIENT)
public class RenderTank implements BlockEntityRenderer<TileTank, RecordedRenderState> {
    private static final Vec3 MIN = new Vec3(0.13, 0.01, 0.13);
    private static final Vec3 MAX = new Vec3(0.86, 0.99, 0.86);
    private static final Vec3 MIN_CONNECTED = new Vec3(0.13, 0, 0.13);
    private static final Vec3 MAX_CONNECTED = new Vec3(0.86, 1 - 1e-5, 0.86);

    @Override
    public void extractRenderState(TileTank tile, RecordedRenderState state, float partialTicks, Vec3 cameraPosition,
        @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);
        state.reset();
        FluidStackInterp forRender = tile.getFluidForRender(partialTicks);
        if (forRender != null) {
            boolean[] sideRender = { true, true, true, true, true, true };
            boolean connectedUp = isFullyConnected(tile, Direction.UP, partialTicks);
            boolean connectedDown = isFullyConnected(tile, Direction.DOWN, partialTicks);
            sideRender[Direction.DOWN.ordinal()] = !connectedDown;
            sideRender[Direction.UP.ordinal()] = !connectedUp;

            Vec3 min = connectedDown ? MIN_CONNECTED : MIN;
            Vec3 max = connectedUp ? MAX_CONNECTED : MAX;
            FluidStack fluid = forRender.fluid;
            int blocklight = fluid.getFluid().getFluidType().getLightLevel(fluid);
            int light = state.lightCoords;
            FluidRenderer.vertex.lighti(Math.max(blocklight, LightCoordsUtil.block(light)), LightCoordsUtil.sky(light));
            FluidRenderer.renderFluid(FluidSpriteType.STILL, fluid, forRender.amount, tile.tank.getCapacity(), min, max,
                state.translucent, sideRender);
        }
        state.finish();
    }

    private static boolean isFullyConnected(TileTank thisTank, Direction face, float partialTicks) {
        BlockPos pos = thisTank.getBlockPos().relative(face);
        BlockEntity oTile = thisTank.getLevel().getBlockEntity(pos);
        if (oTile instanceof TileTank) {
            TileTank oTank = (TileTank) oTile;
            if (!TileTank.canTanksConnect(thisTank, oTank, face)) {
                return false;
            }
            FluidStackInterp forRender = oTank.getFluidForRender(partialTicks);
            if (forRender == null) {
                return false;
            }
            FluidStack fluid = forRender.fluid;
            if (fluid == null || forRender.amount <= 0) {
                return false;
            } else if (thisTank.getFluidForRender(partialTicks) == null
                || !FluidUtilBC.isFluidEqual(fluid, thisTank.getFluidForRender(partialTicks).fluid)) {
                return false;
            }
            if (fluid.getFluid().getFluidType().isLighterThanAir()) {
                face = face.getOpposite();
            }
            return forRender.amount >= oTank.tank.getCapacity() || face == Direction.UP;
        } else {
            return false;
        }
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
