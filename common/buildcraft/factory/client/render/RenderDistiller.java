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

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.state.BlockState;

import buildcraft.lib.block.BlockBCBase_Neptune;
import buildcraft.lib.client.model.MutableQuad;
import buildcraft.lib.client.render.fluid.FluidRenderer;
import buildcraft.lib.client.render.fluid.FluidRenderer.TankSize;
import buildcraft.lib.client.render.fluid.FluidSpriteType;
import buildcraft.lib.fluid.FluidSmoother;
import buildcraft.lib.fluid.FluidSmoother.FluidStackInterp;

import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.BCFactoryModels;
import buildcraft.factory.tile.TileDistiller_BC8;

/** The distiller's moving parts (its variable model) and the fluid in its three tanks. */
@OnlyIn(Dist.CLIENT)
public class RenderDistiller implements BlockEntityRenderer<TileDistiller_BC8, RecordedRenderState> {
    private static final Map<Direction, TankRenderSizes> TANK_SIZES = new EnumMap<>(Direction.class);

    static {
        Direction face = Direction.WEST;
        TankSize tankIn = new TankSize(0, 0, 4, 8, 16, 12).shrink(1 / 64.0);
        TankSize tankGasOut = new TankSize(8, 8, 0, 16, 16, 16).shrink(1 / 64.0);
        TankSize tankLiquidOut = new TankSize(8, 0, 0, 16, 8, 16).shrink(1 / 64.0);
        TankRenderSizes sizes = new TankRenderSizes(tankIn, tankGasOut, tankLiquidOut);
        for (int i = 0; i < 4; i++) {
            TANK_SIZES.put(face, sizes);
            face = face.getClockWise();
            sizes = sizes.rotateY();
        }
    }

    @Override
    public void extractRenderState(TileDistiller_BC8 tile, RecordedRenderState state, float partialTicks,
        Vec3 cameraPosition, @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);
        state.reset();
        BlockState blockState = tile.getBlockState();
        if (blockState.getBlock() == BCFactoryBlocks.distiller) {
            int combinedLight = state.lightCoords;
            Direction face = blockState.getValue(BlockBCBase_Neptune.PROP_FACING);
            TankRenderSizes sizes = TANK_SIZES.get(face);

            if (tile.clientModelData.hasNoNodes()) {
                tile.clientModelData.setNodes(BCFactoryModels.DISTILLER.createTickableNodes());
            }
            tile.setClientModelVariables(partialTicks);
            tile.clientModelData.refresh();
            MutableQuad[] quads = BCFactoryModels.DISTILLER.getCutoutQuads();
            int lightBlock = LightCoordsUtil.block(combinedLight);
            int lightSky = LightCoordsUtil.sky(combinedLight);
            for (MutableQuad q : quads) {
                MutableQuad copy = new MutableQuad(q);
                copy.maxLighti(lightBlock, lightSky);
                copy.multShade();
                copy.render(state.cutout);
            }

            renderTank(sizes.tankIn, tile.smoothedTankIn, combinedLight, partialTicks, state);
            renderTank(sizes.tankOutGas, tile.smoothedTankGasOut, combinedLight, partialTicks, state);
            renderTank(sizes.tankOutLiquid, tile.smoothedTankLiquidOut, combinedLight, partialTicks, state);
        }
        state.finish();
    }

    public static void renderTank(TankSize size, FluidSmoother tank, int combinedLight, float partialTicks,
        RecordedRenderState state) {
        FluidStackInterp fluid = tank.getFluidForRender(partialTicks);
        if (fluid == null || fluid.amount <= 0) {
            return;
        }
        int blockLight = fluid.fluid.getFluid().getFluidType().getLightLevel(fluid.fluid) & 0xF;
        FluidRenderer.vertex.lighti(Math.max(blockLight, LightCoordsUtil.block(combinedLight)),
            LightCoordsUtil.sky(combinedLight));
        FluidRenderer.renderFluid(FluidSpriteType.STILL, fluid.fluid, fluid.amount, tank.getCapacity(), size.min,
            size.max, state.translucent, null);
    }

    static class TankRenderSizes {
        final TankSize tankIn, tankOutGas, tankOutLiquid;

        public TankRenderSizes(TankSize tankIn, TankSize tankOutGas, TankSize tankOutLiquid) {
            this.tankIn = tankIn;
            this.tankOutGas = tankOutGas;
            this.tankOutLiquid = tankOutLiquid;
        }

        public TankRenderSizes rotateY() {
            return new TankRenderSizes(tankIn.rotateY(), tankOutGas.rotateY(), tankOutLiquid.rotateY());
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
