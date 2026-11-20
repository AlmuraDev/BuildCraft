/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.core;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import buildcraft.api.enums.EnumEngineType;
import buildcraft.api.enums.EnumPowerStage;
import buildcraft.api.properties.BuildCraftProperties;

import buildcraft.lib.client.model.ModelHolderVariable;
import buildcraft.lib.client.model.MutableQuad;
import buildcraft.lib.engine.TileEngineBase_BC8;
import buildcraft.lib.expression.DefaultContexts;
import buildcraft.lib.expression.FunctionContext;
import buildcraft.lib.expression.node.value.NodeVariableDouble;
import buildcraft.lib.expression.node.value.NodeVariableObject;
import buildcraft.lib.misc.ExpressionCompat;

import buildcraft.core.client.render.RenderEngineCreative;
import buildcraft.core.client.render.RenderEngineWood;
import buildcraft.core.client.render.RenderMarkerVolume;
import buildcraft.core.tile.TileMarkerVolume;
import buildcraft.core.tile.TileEngineCreative;
import buildcraft.core.tile.TileEngineRedstone_BC8;

/** Core's client models: the engines' variable models and their block entity renderers. */
public class BCCoreModels {
    private static final NodeVariableDouble ENGINE_PROGRESS;
    private static final NodeVariableObject<EnumPowerStage> ENGINE_STAGE;
    private static final NodeVariableObject<net.minecraft.core.Direction> ENGINE_FACING;

    private static final ModelHolderVariable ENGINE_REDSTONE;
    private static final ModelHolderVariable ENGINE_CREATIVE;

    static {
        FunctionContext fnCtx = new FunctionContext(ExpressionCompat.ENUM_POWER_STAGE, DefaultContexts.createWithAll());
        ENGINE_PROGRESS = fnCtx.putVariableDouble("progress");
        ENGINE_STAGE = fnCtx.putVariableObject("stage", EnumPowerStage.class);
        ENGINE_FACING = fnCtx.putVariableObject("direction", net.minecraft.core.Direction.class);

        ENGINE_REDSTONE = new ModelHolderVariable("buildcraftcore:models/block/engine_redstone.json", fnCtx);
        ENGINE_CREATIVE = new ModelHolderVariable("buildcraftcore:models/block/engine_creative.json", fnCtx);
    }

    /** Touches the class so the model holders exist before the first resource reload. */
    /** The quads each engine type's item shows (1.12.2 baked these into the engine item models). Core adds wood and
     * creative; energy adds its own engines. Called on the render thread. */
    public static final java.util.Map<EnumEngineType, java.util.function.Supplier<MutableQuad[]>> ENGINE_ITEM_QUADS =
        new java.util.concurrent.ConcurrentHashMap<>();

    public static void fmlPreInit() {
        ENGINE_ITEM_QUADS.put(EnumEngineType.WOOD, () -> getItemEngineQuads(ENGINE_REDSTONE, EnumPowerStage.BLUE));
        ENGINE_ITEM_QUADS.put(EnumEngineType.CREATIVE, () -> getItemEngineQuads(ENGINE_CREATIVE, EnumPowerStage.BLACK));
        BCCore.MOD_BUS.addListener(BCCoreModels::registerItemModels);
    }

    /** Engine items: the engine's own model, pointing up, part way through its stroke (as 1.12.2 baked them). */
    private static void registerItemModels(net.neoforged.neoforge.client.event.RegisterItemModelsEvent event) {
        event.register(net.minecraft.resources.Identifier.fromNamespaceAndPath(BCCore.MODID, "engine"),
            buildcraft.lib.client.model.ItemModelParts.codec(
                new buildcraft.lib.client.model.ItemModelParts.Source<EnumEngineType>() {
                    @Override
                    public EnumEngineType key(net.minecraft.world.item.ItemStack stack) {
                        return BCCoreBlocks.engine == null ? EnumEngineType.WOOD : BCCoreBlocks.engine.getType(stack);
                    }

                    @Override
                    public void addQuads(EnumEngineType type, buildcraft.lib.client.model.ItemModelParts.QuadSink sink) {
                        java.util.function.Supplier<MutableQuad[]> quads = ENGINE_ITEM_QUADS.get(type);
                        if (quads != null) {
                            sink.add(quads.get(), false);
                        }
                    }
                }, buildcraft.lib.client.model.ItemModelParts.TRANSFORM_BLOCK, true));
    }

    private static MutableQuad[] getItemEngineQuads(ModelHolderVariable model, EnumPowerStage stage) {
        ENGINE_PROGRESS.value = 0.2;
        ENGINE_STAGE.value = stage;
        ENGINE_FACING.value = net.minecraft.core.Direction.UP;
        buildcraft.lib.misc.data.ModelVariableData varData = new buildcraft.lib.misc.data.ModelVariableData();
        varData.setNodes(model.createTickableNodes());
        varData.tick();
        varData.refresh();
        return model.getCutoutQuads();
    }

    @SuppressWarnings("unchecked")
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        registerMarkerRenderer(event);
        if (BCCoreBlocks.engine == null) {
            return;
        }
        BlockEntityType<?> wood = BCCoreBlocks.engine.getTileType(
            BCCoreBlocks.engine.defaultBlockState().setValue(BuildCraftProperties.ENGINE_TYPE, EnumEngineType.WOOD));
        BlockEntityType<?> creative = BCCoreBlocks.engine.getTileType(
            BCCoreBlocks.engine.defaultBlockState().setValue(BuildCraftProperties.ENGINE_TYPE, EnumEngineType.CREATIVE));
        if (wood != null) {
            event.registerBlockEntityRenderer((BlockEntityType<TileEngineRedstone_BC8>) wood, ctx -> new RenderEngineWood());
        }
        if (creative != null) {
            event.registerBlockEntityRenderer((BlockEntityType<TileEngineCreative>) creative,
                ctx -> new RenderEngineCreative());
        }
    }

    @SuppressWarnings("unchecked")
    private static void registerMarkerRenderer(EntityRenderersEvent.RegisterRenderers event) {
        BlockEntityType<?> markerVolume = BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(
            net.minecraft.resources.Identifier.fromNamespaceAndPath(BCCore.MODID, "marker.volume"));
        if (markerVolume != null && BCCoreBlocks.markerVolume != null) {
            event.registerBlockEntityRenderer((BlockEntityType<TileMarkerVolume>) markerVolume,
                ctx -> new RenderMarkerVolume());
        }
    }

    private static MutableQuad[] getEngineQuads(ModelHolderVariable model, TileEngineBase_BC8 tile, float partialTicks) {
        ENGINE_PROGRESS.value = tile.getProgressClient(partialTicks);
        ENGINE_STAGE.value = tile.getPowerStage();
        ENGINE_FACING.value = tile.getCurrentFacing();
        if (tile.clientModelData.hasNoNodes()) {
            tile.clientModelData.setNodes(model.createTickableNodes());
        }
        tile.clientModelData.refresh();
        return model.getCutoutQuads();
    }

    public static MutableQuad[] getRedstoneEngineQuads(TileEngineRedstone_BC8 tile, float partialTicks) {
        return getEngineQuads(ENGINE_REDSTONE, tile, partialTicks);
    }

    public static MutableQuad[] getCreativeEngineQuads(TileEngineCreative tile, float partialTicks) {
        return getEngineQuads(ENGINE_CREATIVE, tile, partialTicks);
    }
}
