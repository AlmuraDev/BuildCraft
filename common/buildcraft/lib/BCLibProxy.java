/*
 * Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.lib;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

import javax.annotation.Nullable;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import buildcraft.api.BCModules;

import buildcraft.lib.misc.MessageUtil;
import buildcraft.lib.net.MessageContainer;
import buildcraft.lib.net.MessageDebugRequest;
import buildcraft.lib.net.MessageDebugResponse;
import buildcraft.lib.net.MessageManager;
import buildcraft.lib.net.MessageMarker;
import buildcraft.lib.net.MessageUpdateTile;
import buildcraft.lib.net.cache.MessageObjectCacheRequest;
import buildcraft.lib.net.cache.MessageObjectCacheResponse;

/** Things lib does differently on the client and the server. The client's version is
 * buildcraft.lib.client.BCLibClientProxy, which is only loaded on the client (by name, so the server never sees it). */
public abstract class BCLibProxy {
    private static final BCLibProxy proxy = createProxy();

    private static BCLibProxy createProxy() {
        if (FMLEnvironment.getDist().isClient()) {
            try {
                return (BCLibProxy) Class.forName("buildcraft.lib.client.BCLibClientProxy").getDeclaredConstructor()
                    .newInstance();
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Couldn't create the client proxy!", e);
            }
        }
        return new ServerProxy();
    }

    public static BCLibProxy getProxy() {
        return proxy;
    }

    void fmlPreInit() {
        MessageManager.registerMessageClass(BCModules.LIB, MessageUpdateTile.class, MessageUpdateTile.HANDLER);
        MessageManager.registerMessageClass(BCModules.LIB, MessageContainer.class, MessageContainer.HANDLER);
        MessageManager.registerMessageClass(BCModules.LIB, MessageMarker.class, LogicalSide.CLIENT);
        MessageManager.registerMessageClass(BCModules.LIB, MessageObjectCacheRequest.class,
            MessageObjectCacheRequest.HANDLER, LogicalSide.SERVER);
        MessageManager.registerMessageClass(BCModules.LIB, MessageObjectCacheResponse.class, LogicalSide.CLIENT);
        MessageManager.registerMessageClass(BCModules.LIB, MessageDebugRequest.class, MessageDebugRequest.HANDLER,
            LogicalSide.SERVER);
        MessageManager.registerMessageClass(BCModules.LIB, MessageDebugResponse.class, LogicalSide.CLIENT);
    }

    void fmlInit() {}

    void fmlPostInit() {}

    @Nullable
    public Level getClientWorld() {
        return null;
    }

    @Nullable
    public Player getClientPlayer() {
        return null;
    }

    /** @return The player that sent (on the server) or received (on the client) the message. */
    @Nullable
    public Player getPlayerForContext(IPayloadContext ctx) {
        return ctx.player();
    }

    /** Runs the task on the main thread of the given world's side. */
    public void addScheduledTask(Level world, Runnable task) {
        MinecraftServer server = world.getServer();
        if (server != null) {
            server.execute(task);
        }
    }

    /** Sends a payload to the server. Only works on the client. */
    public void sendToServer(CustomPacketPayload payload) {
        throw new IllegalStateException("Cannot send to the server from the server!");
    }

    /** @return The server's copy of the given (client) tile in single player, or the tile itself. */
    public <T extends BlockEntity> T getServerTile(T tile) {
        return tile;
    }

    @Nullable
    public InputStream getStreamForIdentifier(Identifier identifier) throws IOException {
        return null;
    }

    public File getGameDirectory() {
        return FMLPaths.GAMEDIR.get().toFile();
    }

    public Iterable<File> getLoadedResourcePackFiles() {
        return Collections.emptySet();
    }

    /** @return The side the given context received a message on. */
    public static LogicalSide getSide(IPayloadContext ctx) {
        return MessageUtil.getSide(ctx);
    }

    public static class ServerProxy extends BCLibProxy {
        @Override
        public File getGameDirectory() {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            return server != null ? server.getServerDirectory().toFile() : super.getGameDirectory();
        }
    }
}
