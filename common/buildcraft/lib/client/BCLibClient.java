/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.TextureAtlasStitchedEvent;
import net.neoforged.neoforge.common.NeoForge;

import buildcraft.api.tiles.IDebuggable;
import buildcraft.api.core.NbtStacks;

import buildcraft.lib.BCLib;
import buildcraft.lib.BCLibSprites;
import buildcraft.lib.client.model.ModelHolderRegistry;
import buildcraft.lib.client.render.DetachedRenderer;
import buildcraft.lib.client.render.DetachedRenderer.RenderMatrixType;
import buildcraft.lib.client.render.MarkerRenderer;
import buildcraft.lib.client.render.laser.LaserRenderer_BC8;
import buildcraft.lib.debug.DebugRenderHelper;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import buildcraft.lib.client.sprite.SpriteHolderRegistry;
import net.neoforged.neoforge.client.event.ModelEvent;
import buildcraft.lib.debug.ClientDebuggables;
import buildcraft.lib.item.ItemDebugger;
import buildcraft.lib.misc.MessageUtil;
import buildcraft.lib.net.MessageDebugRequest;
import buildcraft.lib.net.MessageDebugResponse;
import buildcraft.lib.net.MessageManager;
import buildcraft.lib.net.MessageMarker;
import buildcraft.lib.net.cache.BuildCraftObjectCaches;
import buildcraft.lib.net.cache.MessageObjectCacheResponse;

/** Lib's client-only setup and events (1.12.2's ClientProxy and the client half of BCLibEventDist). Only constructed on
 * the client, so nothing here is ever loaded on a dedicated server. */
@Mod(value = BCLib.MODID, dist = Dist.CLIENT)
public class BCLibClient {

    public BCLibClient(IEventBus modBus) {
        // Construct one module at a time (see BCModConstruction)
        synchronized (buildcraft.lib.BCModConstruction.LOCK) {
            construct(modBus);
        }
    }

    private static void construct(IEventBus modBus) {
        DetachedRenderer.INSTANCE.addRenderer(RenderMatrixType.FROM_WORLD_ORIGIN, MarkerRenderer.INSTANCE);
        DetachedRenderer.INSTANCE.addRenderer(RenderMatrixType.FROM_WORLD_ORIGIN, DebugRenderHelper.INSTANCE);
        // The guide's page entries come from resource packs (assets/*/compat/buildcraft/guide)
        buildcraft.lib.script.ReloadableRegistryManager resourceManager =
            buildcraft.lib.script.ReloadableRegistryManager.RESOURCE_PACKS;
        buildcraft.api.registry.BuildCraftRegistryManager.managerResourcePacks = resourceManager;
        resourceManager.registerRegistry(buildcraft.lib.client.guide.GuidePageRegistry.INSTANCE);
        // TODO(26.3): the config change listener (lib/client/reload) and the config screen.
        BCLibSprites.fmlPreInitClient();

        // Stacks in network messages are encoded with the client's registries
        NbtStacks.registries = () -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getSingleplayerServer() != null) {
                return mc.getSingleplayerServer().registryAccess();
            }
            return mc.getConnection() != null ? mc.getConnection().registryAccess() : null;
        };

        modBus.addListener(BCLibClient::clientSetup);
        modBus.addListener(BCLibClient::addReloadListeners);
        modBus.addListener(BCLibClient::textureStitchPost);
        modBus.addListener(BCLibClient::modelBake);
        modBus.addListener(BCLibClient::registerFluidModels);
        NeoForge.EVENT_BUS.register(BCLibClient.class);
    }

    private static void clientSetup(FMLClientSetupEvent event) {
        MessageManager.setHandler(MessageMarker.class, MessageMarker.HANDLER, LogicalSide.CLIENT);
        MessageManager.setHandler(MessageObjectCacheResponse.class, MessageObjectCacheResponse.HANDLER, LogicalSide.CLIENT);
        MessageManager.setHandler(MessageDebugResponse.class, MessageDebugResponse.HANDLER, LogicalSide.CLIENT);
        MessageManager.setHandler(buildcraft.lib.net.MessageGuideRecipes.Response.class,
            buildcraft.lib.client.guide.parts.recipe.GuideRecipeAccess.RESPONSE_HANDLER, LogicalSide.CLIENT);
        buildcraft.lib.gui.config.GuiConfigManager.loadFromConfigFile();
    }

    private static void addReloadListeners(net.neoforged.neoforge.client.event.AddClientReloadListenersEvent event) {
        if (buildcraft.lib.BCLibItems.isGuideEnabled()) {
            event.addListener(net.minecraft.resources.Identifier.fromNamespaceAndPath(BCLib.MODID, "guide"),
                buildcraft.lib.client.guide.GuideManager.INSTANCE);
        }
    }

    private static void modelBake(ModelEvent.BakingCompleted event) {
        ModelHolderRegistry.onModelBake();
        LaserRenderer_BC8.clearModels();
    }

    private static void textureStitchPost(TextureAtlasStitchedEvent event) {
        SpriteHolderRegistry.onResourcesReloaded();
    }

    @SubscribeEvent
    public static void onConnectToServer(ClientPlayerNetworkEvent.LoggingIn event) {
        BuildCraftObjectCaches.onClientJoinServer();
        // The guide's crafting/smelting panels need the server's recipes (dedicated servers only; single player reads
        // the integrated server directly)
        buildcraft.lib.client.guide.parts.recipe.GuideRecipeAccess.onDisconnect();
        if (buildcraft.lib.BCLibItems.isGuideEnabled()) {
            buildcraft.lib.client.guide.parts.recipe.GuideRecipeAccess.requestFromServer();
        }
    }

    @SubscribeEvent
    public static void onDisconnectFromServer(ClientPlayerNetworkEvent.LoggingOut event) {
        buildcraft.lib.client.guide.parts.recipe.GuideRecipeAccess.onDisconnect();
    }

    @SubscribeEvent
    public static void extractWorld(ExtractLevelRenderStateEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        float partialTicks = event.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        DetachedRenderer.INSTANCE.extract(player, event.getCamera().position(), partialTicks);
    }

    @SubscribeEvent
    public static void submitWorld(SubmitCustomGeometryEvent event) {
        DetachedRenderer.INSTANCE.submit(event);
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post event) {
        BuildCraftObjectCaches.onClientTick();
        MessageUtil.postClientTick();
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player != null && ItemDebugger.isShowDebugInfo(player)) {
            HitResult mouseOver = mc.hitResult;
            if (mouseOver instanceof BlockHitResult blockHit) {
                IDebuggable debuggable = ClientDebuggables.getDebuggableObject(mouseOver);
                if (debuggable instanceof BlockEntity) {
                    BlockEntity tile = (BlockEntity) debuggable;
                    MessageManager.sendToServer(new MessageDebugRequest(tile.getBlockPos(), blockHit.getDirection()));
                } else if (debuggable instanceof Entity) {
                    // TODO: Support entities!
                }
            }
        }
    }

    /** Every BuildCraft fluid draws its own still and flowing textures (on the block atlas), untinted. */
    private static void registerFluidModels(net.neoforged.neoforge.client.event.RegisterFluidModelsEvent event) {
        for (buildcraft.lib.fluid.BCFluid fluid : buildcraft.lib.fluid.FluidManager.getAll()) {
            net.minecraft.client.renderer.block.FluidModel.Unbaked model =
                new net.minecraft.client.renderer.block.FluidModel.Unbaked(
                    new net.minecraft.client.resources.model.sprite.Material(fluid.getStill()),
                    new net.minecraft.client.resources.model.sprite.Material(fluid.getFlowing()), null,
                    (net.neoforged.neoforge.client.fluid.FluidTintSource) null);
            event.register(model, fluid.getSource(), fluid.getFlowingFluid());
        }
    }
}
