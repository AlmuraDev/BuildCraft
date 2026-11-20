/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.energy;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import buildcraft.api.enums.EnumEngineType;
import buildcraft.api.enums.EnumPowerStage;

import buildcraft.lib.client.model.ModelHolderVariable;
import buildcraft.lib.client.model.MutableQuad;
import buildcraft.lib.engine.TileEngineBase_BC8;
import buildcraft.lib.expression.DefaultContexts;
import buildcraft.lib.expression.FunctionContext;
import buildcraft.lib.expression.node.value.NodeVariableDouble;
import buildcraft.lib.expression.node.value.NodeVariableObject;
import buildcraft.lib.misc.ExpressionCompat;
import buildcraft.lib.registry.TileTypes;

import buildcraft.core.BCCoreBlocks;
import buildcraft.api.properties.BuildCraftProperties;
import buildcraft.energy.client.render.RenderDynamoMJ;
import buildcraft.energy.client.render.RenderEngineIron;
import buildcraft.energy.client.render.RenderEngineRF;
import buildcraft.energy.client.render.RenderEngineStone;
import buildcraft.energy.tile.TileDynamoMJ;
import buildcraft.energy.tile.TileEngineIron_BC8;
import buildcraft.energy.tile.TileEngineRF;
import buildcraft.energy.tile.TileEngineStone_BC8;

/** Energy's engine and dynamo models (variable models, drawn by their tile renderers). Fluid models are registered for
 * every BuildCraft fluid by BCLibClient.
 * <p>
 * TODO(26.3): engine and dynamo items. 1.12.2 baked the variable model into the item model; 26.3 needs a special item
 * model renderer for that, so the items show plain fallback models for now (as core's engines do). */
public class BCEnergyModels {
    private static final NodeVariableDouble ENGINE_PROGRESS;
    private static final NodeVariableObject<EnumPowerStage> ENGINE_STAGE;
    private static final NodeVariableObject<Direction> ENGINE_FACING;

    private static final ModelHolderVariable ENGINE_STONE;
    private static final ModelHolderVariable ENGINE_IRON;
    private static final ModelHolderVariable ENGINE_RF;
    private static final ModelHolderVariable MJ_DYNAMO;

    static {
        FunctionContext fnCtx = new FunctionContext(ExpressionCompat.ENUM_POWER_STAGE, DefaultContexts.createWithAll());
        ENGINE_PROGRESS = fnCtx.putVariableDouble("progress");
        ENGINE_STAGE = fnCtx.putVariableObject("stage", EnumPowerStage.class);
        ENGINE_FACING = fnCtx.putVariableObject("direction", Direction.class);
        ENGINE_STONE = new ModelHolderVariable("buildcraftenergy:models/block/engine_stone.json", fnCtx);
        ENGINE_IRON = new ModelHolderVariable("buildcraftenergy:models/block/engine_iron.json", fnCtx);
        ENGINE_RF = new ModelHolderVariable("buildcraftenergy:models/block/engine_rf.json", fnCtx);
        MJ_DYNAMO = new ModelHolderVariable("buildcraftenergy:models/block/mj_dynamo.json", fnCtx);
    }

    public static void fmlPreInit() {
        BCEnergy.MOD_BUS.addListener(BCEnergyModels::registerRenderers);
        buildcraft.core.BCCoreModels.ENGINE_ITEM_QUADS.put(EnumEngineType.STONE, () -> getItemQuads(ENGINE_STONE));
        buildcraft.core.BCCoreModels.ENGINE_ITEM_QUADS.put(EnumEngineType.IRON, () -> getItemQuads(ENGINE_IRON));
        buildcraft.core.BCCoreModels.ENGINE_ITEM_QUADS.put(EnumEngineType.RF, () -> getItemQuads(ENGINE_RF));
        BCEnergy.MOD_BUS.addListener(BCEnergyModels::registerItemModels);
    }

    /** The MJ dynamo item: its model pointing up, part way through its stroke (like the engine items). */
    private static void registerItemModels(net.neoforged.neoforge.client.event.RegisterItemModelsEvent event) {
        event.register(net.minecraft.resources.Identifier.fromNamespaceAndPath("buildcraftenergy", "mj_dynamo"),
            buildcraft.lib.client.model.ItemModelParts.codec(
                new buildcraft.lib.client.model.ItemModelParts.Source<Boolean>() {
                    @Override
                    public Boolean key(net.minecraft.world.item.ItemStack stack) {
                        return Boolean.TRUE;
                    }

                    @Override
                    public void addQuads(Boolean key, buildcraft.lib.client.model.ItemModelParts.QuadSink sink) {
                        sink.add(getItemQuads(MJ_DYNAMO), false);
                    }
                }, buildcraft.lib.client.model.ItemModelParts.TRANSFORM_BLOCK, true));
    }

    /** An engine model as 1.12.2 baked it for the item: facing up, blue, 20% through its stroke. */
    private static MutableQuad[] getItemQuads(ModelHolderVariable model) {
        ENGINE_PROGRESS.value = 0.2;
        ENGINE_STAGE.value = EnumPowerStage.BLUE;
        ENGINE_FACING.value = Direction.UP;
        buildcraft.lib.misc.data.ModelVariableData varData = new buildcraft.lib.misc.data.ModelVariableData();
        varData.setNodes(model.createTickableNodes());
        varData.tick();
        varData.refresh();
        return model.getCutoutQuads();
    }

    @SuppressWarnings("unchecked")
    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        if (BCCoreBlocks.engine != null) {
            BlockEntityType<?> stone = engineType(EnumEngineType.STONE);
            BlockEntityType<?> iron = engineType(EnumEngineType.IRON);
            BlockEntityType<?> rf = engineType(EnumEngineType.RF);
            if (stone != null) {
                event.registerBlockEntityRenderer((BlockEntityType<TileEngineStone_BC8>) stone,
                    ctx -> new RenderEngineStone());
            }
            if (iron != null) {
                event.registerBlockEntityRenderer((BlockEntityType<TileEngineIron_BC8>) iron,
                    ctx -> new RenderEngineIron());
            }
            if (rf != null) {
                event.registerBlockEntityRenderer((BlockEntityType<TileEngineRF>) rf, ctx -> new RenderEngineRF());
            }
        }
        if (BCEnergyBlocks.mjDynamo != null) {
            BlockEntityType<?> dynamo = TileTypes.forState(BCEnergyBlocks.mjDynamo.defaultBlockState());
            event.registerBlockEntityRenderer((BlockEntityType<TileDynamoMJ>) dynamo, ctx -> new RenderDynamoMJ());
        }
    }

    private static BlockEntityType<?> engineType(EnumEngineType type) {
        return BCCoreBlocks.engine.getTileType(
            BCCoreBlocks.engine.defaultBlockState().setValue(BuildCraftProperties.ENGINE_TYPE, type));
    }

    private static MutableQuad[] getEngineQuads(ModelHolderVariable model, TileEngineBase_BC8 tile,
        float partialTicks) {
        ENGINE_PROGRESS.value = tile.getProgressClient(partialTicks);
        ENGINE_STAGE.value = tile.getPowerStage();
        ENGINE_FACING.value = tile.getCurrentFacing();
        if (tile.clientModelData.hasNoNodes()) {
            tile.clientModelData.setNodes(model.createTickableNodes());
        }
        tile.clientModelData.refresh();
        return model.getCutoutQuads();
    }

    public static MutableQuad[] getStoneEngineQuads(TileEngineStone_BC8 tile, float partialTicks) {
        return getEngineQuads(ENGINE_STONE, tile, partialTicks);
    }

    public static MutableQuad[] getIronEngineQuads(TileEngineIron_BC8 tile, float partialTicks) {
        return getEngineQuads(ENGINE_IRON, tile, partialTicks);
    }

    public static MutableQuad[] getRfEngineQuads(TileEngineRF tile, float partialTicks) {
        return getEngineQuads(ENGINE_RF, tile, partialTicks);
    }

    public static MutableQuad[] getMjDynamoQuads(TileDynamoMJ tile, float partialTicks) {
        ENGINE_PROGRESS.value = tile.getProgressClient(partialTicks);
        ENGINE_STAGE.value = tile.getPowerStage();
        ENGINE_FACING.value = tile.getCurrentDirection();
        if (tile.clientModelData.hasNoNodes()) {
            tile.clientModelData.setNodes(MJ_DYNAMO.createTickableNodes());
        }
        tile.clientModelData.refresh();
        return MJ_DYNAMO.getCutoutQuads();
    }
}
