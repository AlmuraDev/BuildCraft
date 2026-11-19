/*
 * Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.lib.client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import buildcraft.lib.BCLibProxy;
import buildcraft.lib.misc.MessageUtil;

/** Lib's client proxy: created by name from {@link BCLibProxy} on the client only. */
@OnlyIn(Dist.CLIENT)
public class BCLibClientProxy extends BCLibProxy {

    @Override
    public Level getClientWorld() {
        return Minecraft.getInstance().level;
    }

    @Override
    public Player getClientPlayer() {
        return Minecraft.getInstance().player;
    }

    @Override
    public Player getPlayerForContext(IPayloadContext ctx) {
        if (MessageUtil.getSide(ctx) == LogicalSide.SERVER) {
            return super.getPlayerForContext(ctx);
        }
        return getClientPlayer();
    }

    @Override
    public void addScheduledTask(Level world, Runnable task) {
        if (world instanceof ClientLevel) {
            Minecraft.getInstance().execute(task);
        } else {
            super.addScheduledTask(world, task);
        }
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        ClientPacketDistributor.sendToServer(payload);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends BlockEntity> T getServerTile(T tile) {
        if (tile != null && tile.hasLevel()) {
            Level world = tile.getLevel();
            IntegratedServer integrated = Minecraft.getInstance().getSingleplayerServer();
            if (world.isClientSide() && integrated != null) {
                ServerLevel server = integrated.getLevel(world.dimension());
                if (server == null) return tile;
                BlockEntity atServer = server.getBlockEntity(tile.getBlockPos());
                if (atServer == null) return tile;
                if (atServer.getClass() == tile.getClass()) {
                    return (T) atServer;
                }
            }
        }
        return tile;
    }

    @Override
    public void openGuide(@Nullable String bookName) {
        // TODO(26.3 GUIs+guide batch): open GuiGuide.
    }

    @Override
    public File getGameDirectory() {
        return Minecraft.getInstance().gameDirectory;
    }

    @Override
    @Nullable
    public InputStream getStreamForIdentifier(Identifier identifier) throws IOException {
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(identifier);
        return resource.isPresent() ? resource.get().open() : null;
    }
}
