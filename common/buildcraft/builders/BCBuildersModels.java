/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import buildcraft.lib.client.render.DetachedRenderer;
import buildcraft.lib.registry.TileTypes;

import buildcraft.builders.client.render.RenderArchitectTable;
import buildcraft.builders.client.render.RenderArchitectTables;
import buildcraft.builders.client.render.RenderBuilder;
import buildcraft.builders.client.render.RenderFiller;
import buildcraft.builders.client.render.RenderQuarry;

/** Builders' tile renderers and the architect table scan overlay. */
@OnlyIn(Dist.CLIENT)
public class BCBuildersModels {
    public static void fmlPreInit() {
        BCBuilders.MOD_BUS.addListener(BCBuildersModels::registerRenderers);
        RenderQuarry.init();
        DetachedRenderer.INSTANCE.addRenderer(DetachedRenderer.RenderMatrixType.FROM_WORLD_ORIGIN,
            RenderArchitectTables.INSTANCE);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        register(event, BCBuildersBlocks.architect, ctx -> new RenderArchitectTable());
        register(event, BCBuildersBlocks.builder, ctx -> new RenderBuilder());
        register(event, BCBuildersBlocks.filler, ctx -> new RenderFiller());
        register(event, BCBuildersBlocks.quarry, ctx -> new RenderQuarry());
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
