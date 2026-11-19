/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
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
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.TextureAtlasStitchedEvent;
import net.neoforged.neoforge.common.NeoForge;

import buildcraft.api.core.IDebuggable;
import buildcraft.api.core.NbtStacks;
import buildcraft.api.registry.BuildCraftRegistryManager;

import buildcraft.lib.BCLib;
import buildcraft.lib.BCLibConfig;
import buildcraft.lib.BCLibItems;
import buildcraft.lib.BCLibSprites;
import buildcraft.lib.client.guide.GuideManager;
import buildcraft.lib.client.guide.GuidePageRegistry;
import buildcraft.lib.client.model.ModelHolderRegistry;
import buildcraft.lib.client.render.DetachedRenderer;
import buildcraft.lib.client.render.DetachedRenderer.RenderMatrixType;
import buildcraft.lib.client.render.MarkerRenderer;
import buildcraft.lib.client.render.fluid.FluidRenderer;
import buildcraft.lib.client.render.laser.LaserRenderer_BC8;
import buildcraft.lib.client.sprite.SpriteHolderRegistry;
import buildcraft.lib.debug.ClientDebuggables;
import buildcraft.lib.debug.DebugRenderHelper;
import buildcraft.lib.expression.model.ModelVariableData;
import buildcraft.lib.gui.config.GuiConfigManager;
import buildcraft.lib.item.ItemDebugger;
import buildcraft.lib.misc.MessageUtil;
import buildcraft.lib.net.MessageDebugRequest;
import buildcraft.lib.net.MessageDebugResponse;
import buildcraft.lib.net.MessageManager;
import buildcraft.lib.net.MessageMarker;
import buildcraft.lib.net.cache.BuildCraftObjectCaches;
import buildcraft.lib.net.cache.MessageObjectCacheResponse;
import buildcraft.lib.client.reload.LibConfigChangeListener;
import buildcraft.lib.script.ReloadableRegistryManager;

/** Lib's client-only setup and events (1.12.2's ClientProxy and the client half of BCLibEventDist). Only constructed on
 * the client, so nothing here is ever loaded on a dedicated server. */
@Mod(value = BCLib.MODID, dist = Dist.CLIENT)
public class BCLibClient {

    public BCLibClient(IEventBus modBus) {
        ReloadableRegistryManager manager = ReloadableRegistryManager.RESOURCE_PACKS;
        BuildCraftRegistryManager.managerResourcePacks = manager;
        manager.registerRegistry(GuidePageRegistry.INSTANCE);

        DetachedRenderer.INSTANCE.addRenderer(RenderMatrixType.FROM_WORLD_ORIGIN, MarkerRenderer.INSTANCE);
        DetachedRenderer.INSTANCE.addRenderer(RenderMatrixType.FROM_WORLD_ORIGIN, DebugRenderHelper.INSTANCE);
        // various sprite registers
        BCLibSprites.fmlPreInitClient();
        BCLibConfig.configChangeListeners.add(LibConfigChangeListener.INSTANCE);

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
        NeoForge.EVENT_BUS.register(BCLibClient.class);
    }

    private static void clientSetup(FMLClientSetupEvent event) {
        MessageManager.setHandler(MessageMarker.class, MessageMarker.HANDLER, LogicalSide.CLIENT);
        MessageManager.setHandler(MessageObjectCacheResponse.class, MessageObjectCacheResponse.HANDLER, LogicalSide.CLIENT);
        MessageManager.setHandler(MessageDebugResponse.class, MessageDebugResponse.HANDLER, LogicalSide.CLIENT);
        event.enqueueWork(GuiConfigManager::loadFromConfigFile);
    }

    private static void addReloadListeners(AddClientReloadListenersEvent event) {
        if (BCLibItems.isGuideEnabled()) {
            event.addListener(Identifier.fromNamespaceAndPath(BCLib.MODID, "guide"), GuideManager.INSTANCE);
        }
    }

    private static void textureStitchPost(TextureAtlasStitchedEvent event) {
        SpriteHolderRegistry.onTextureStitchPost();
        FluidRenderer.onTextureStitchPost(event.getAtlas());
    }

    private static void modelBake(ModelEvent.BakingCompleted event) {
        SpriteHolderRegistry.exportTextureMap();
        LaserRenderer_BC8.clearModels();
        ModelHolderRegistry.onModelBake();
        ModelVariableData.onModelBake();
    }

    @SubscribeEvent
    public static void onConnectToServer(ClientPlayerNetworkEvent.LoggingIn event) {
        BuildCraftObjectCaches.onClientJoinServer();
    }

    @SubscribeEvent
    public static void renderWorldLast(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;
        float partialTicks = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        DetachedRenderer.INSTANCE.renderWorldLastEvent(event, player, partialTicks);
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
}
