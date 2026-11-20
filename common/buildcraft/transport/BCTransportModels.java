/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterBlockStateModels;

import buildcraft.api.transport.pipe.PipeApiClient;
import buildcraft.api.transport.pluggable.IPluggableStaticBaker;

import buildcraft.lib.client.model.ModelHolderStatic;
import buildcraft.lib.client.model.ModelHolderVariable;
import buildcraft.lib.client.model.MutableQuad;
import buildcraft.lib.client.model.plug.PlugBakerSimple;
import buildcraft.lib.expression.DefaultContexts;
import buildcraft.lib.expression.FunctionContext;
import buildcraft.lib.expression.node.value.NodeVariableObject;
import buildcraft.lib.registry.TileTypes;

import buildcraft.transport.client.model.ModelPipe;
import buildcraft.transport.client.model.key.KeyPlugBlocker;
import buildcraft.transport.client.model.key.KeyPlugPowerAdaptor;
import buildcraft.transport.client.render.PipeBehaviourRendererStripes;
import buildcraft.transport.client.render.PipeFlowRendererFluids;
import buildcraft.transport.client.render.PipeFlowRendererItems;
import buildcraft.transport.client.render.PipeFlowRendererPower;
import buildcraft.transport.client.render.PipeFlowRendererRf;
import buildcraft.transport.client.render.RenderPipeHolder;
import buildcraft.transport.pipe.behaviour.PipeBehaviourStripes;
import buildcraft.transport.pipe.flow.PipeFlowFluids;
import buildcraft.transport.pipe.flow.PipeFlowItems;
import buildcraft.transport.pipe.flow.PipeFlowPower;
import buildcraft.transport.pipe.flow.PipeFlowRedstoneFlux;
import buildcraft.transport.tile.TilePipeHolder;

/** Transport's client models and renderers.
 * <p>
 * The pipe holder's block model is {@link ModelPipe}, named by its blockstate file. Pipe items are small upright pipes
 * ({@link buildcraft.transport.client.model.ModelPipeItem}, item model type "buildcrafttransport:pipe"). Plug items
 * use flat item models for now. TODO(26.3): 1.12.2 drew plug items from their plug models (ModelPluggableItem). */
public class BCTransportModels {
    public static final ModelHolderStatic BLOCKER;
    public static final ModelHolderStatic POWER_ADAPTER;

    private static final ModelHolderVariable STRIPES;
    private static final NodeVariableObject<Direction> STRIPES_DIRECTION;

    public static final IPluggableStaticBaker<KeyPlugBlocker> BAKER_PLUG_BLOCKER;
    public static final IPluggableStaticBaker<KeyPlugPowerAdaptor> BAKER_PLUG_POWER_ADAPTOR;

    static {
        BLOCKER = getStaticModel("plugs/blocker");
        POWER_ADAPTER = getStaticModel("plugs/power_adapter");

        BAKER_PLUG_BLOCKER = new PlugBakerSimple<>(BLOCKER::getCutoutQuads);
        BAKER_PLUG_POWER_ADAPTOR = new PlugBakerSimple<>(POWER_ADAPTER::getCutoutQuads);

        {
            FunctionContext fnCtx = DefaultContexts.createWithAll();
            STRIPES_DIRECTION = fnCtx.putVariableObject("side", Direction.class);
            STRIPES = getModel("pipes/stripes", fnCtx);
        }
    }

    private static ModelHolderStatic getStaticModel(String str) {
        return new ModelHolderStatic("buildcrafttransport:models/" + str + ".json");
    }

    private static ModelHolderVariable getModel(String str, FunctionContext fnCtx) {
        return new ModelHolderVariable("buildcrafttransport:models/" + str + ".json", fnCtx);
    }

    public static void fmlPreInit() {
        TilePipeHolder.modelDataFactory = ModelPipe::createModelData;
        BCTransport.MOD_BUS.addListener(BCTransportModels::registerBlockStateModels);
        BCTransport.MOD_BUS.addListener(BCTransportModels::registerRenderers);
        BCTransport.MOD_BUS.addListener(
            (net.neoforged.neoforge.client.event.RegisterItemModelsEvent event) -> event.register(
                buildcraft.transport.client.model.ModelPipeItem.TYPE, buildcraft.transport.client.model.ModelPipeItem.Unbaked.CODEC));
    }

    public static void fmlInit() {
        PipeApiClient.registry.registerBaker(KeyPlugBlocker.class, BAKER_PLUG_BLOCKER);
        PipeApiClient.registry.registerBaker(KeyPlugPowerAdaptor.class, BAKER_PLUG_POWER_ADAPTOR);

        PipeApiClient.registry.registerRenderer(PipeFlowItems.class, PipeFlowRendererItems.INSTANCE);
        PipeApiClient.registry.registerRenderer(PipeFlowFluids.class, PipeFlowRendererFluids.INSTANCE);
        PipeApiClient.registry.registerRenderer(PipeFlowPower.class, PipeFlowRendererPower.INSTANCE);
        PipeApiClient.registry.registerRenderer(PipeFlowRedstoneFlux.class, PipeFlowRendererRf.INSTANCE);

        PipeApiClient.registry.registerRenderer(PipeBehaviourStripes.class, PipeBehaviourRendererStripes.INSTANCE);
    }

    public static void fmlPostInit() {}

    private static void registerBlockStateModels(RegisterBlockStateModels event) {
        event.registerModel(ModelPipe.TYPE, ModelPipe.Unbaked.CODEC);
    }

    @SuppressWarnings("unchecked")
    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        if (BCTransportBlocks.pipeHolder == null) {
            return;
        }
        BlockEntityType<?> type = TileTypes.forState(BCTransportBlocks.pipeHolder.defaultBlockState());
        if (type != null) {
            event.registerBlockEntityRenderer((BlockEntityType<TilePipeHolder>) type, ctx -> new RenderPipeHolder());
        }
    }

    public static MutableQuad[] getStripesDynQuads(Direction side) {
        STRIPES_DIRECTION.value = side;
        return STRIPES.getCutoutQuads();
    }
}
