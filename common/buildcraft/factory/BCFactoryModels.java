/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterBlockStateModels;

import buildcraft.lib.client.model.ModelHolderVariable;
import buildcraft.lib.registry.TileTypes;

import buildcraft.factory.client.model.ModelHeatExchange;
import buildcraft.factory.client.render.RenderDistiller;
import buildcraft.factory.client.render.RenderHeatExchange;
import buildcraft.factory.client.render.RenderMiningWell;
import buildcraft.factory.client.render.RenderPump;
import buildcraft.factory.client.render.RenderTank;
import buildcraft.factory.tile.TileDistiller_BC8;

/** Factory's client models and tile renderers. The heat exchanger's block model is {@link ModelHeatExchange}, named by
 * its blockstate file. TODO(26.3): the heat exchanger item used its baked variable model; it shows a plain fallback
 * model until it has a special item model renderer. */
@OnlyIn(Dist.CLIENT)
public class BCFactoryModels {
    public static final ModelHolderVariable DISTILLER = new ModelHolderVariable(
        "buildcraftfactory:models/tiles/distiller.json",
        TileDistiller_BC8.MODEL_FUNC_CTX
    );
    public static final ModelHolderVariable HEAT_EXCHANGE_STATIC = new ModelHolderVariable(
        "buildcraftfactory:models/tiles/heat_exchange_static.json",
        ModelHeatExchange.FUNCTION_CONTEXT
    );

    public static void fmlPreInit() {
        BCFactory.MOD_BUS.addListener(BCFactoryModels::registerBlockStateModels);
        BCFactory.MOD_BUS.addListener(BCFactoryModels::registerRenderers);
        BCFactory.MOD_BUS.addListener((ModelEvent.BakingCompleted event) -> ModelHeatExchange.clearCache());
    }

    private static void registerBlockStateModels(RegisterBlockStateModels event) {
        event.registerModel(ModelHeatExchange.TYPE, ModelHeatExchange.Unbaked.CODEC);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        register(event, BCFactoryBlocks.miningWell, ctx -> new RenderMiningWell());
        register(event, BCFactoryBlocks.pump, ctx -> new RenderPump());
        register(event, BCFactoryBlocks.tank, ctx -> new RenderTank());
        register(event, BCFactoryBlocks.distiller, ctx -> new RenderDistiller());
        register(event, BCFactoryBlocks.heatExchange, ctx -> new RenderHeatExchange());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void register(EntityRenderersEvent.RegisterRenderers event, Block block,
        BlockEntityRendererProvider provider) {
        if (block == null) {
            return;
        }
        BlockEntityType type = TileTypes.forState(block.defaultBlockState());
        event.registerBlockEntityRenderer(type, (BlockEntityRendererProvider) provider);
    }
}
