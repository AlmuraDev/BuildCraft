/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import buildcraft.lib.debug.BCAdvDebugging;
import buildcraft.lib.marker.MarkerCache;
import buildcraft.lib.misc.FakePlayerProvider;
import buildcraft.lib.misc.MessageUtil;

/** Lib's game events that both sides need. Client-only ones are in {@link buildcraft.lib.client.BCLibClient}. */
public enum BCLibEventDist {
    INSTANCE;

    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof ServerPlayer) {
            ServerPlayer playerMP = (ServerPlayer) entity;
            // Delay sending join messages to player as it makes it work when in single-player
            MessageUtil.doDelayedServer(() -> MarkerCache.onPlayerJoinWorld(playerMP));
        }
    }

    @SubscribeEvent
    public static void onWorldUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof net.minecraft.world.level.Level level) {
            MarkerCache.onWorldUnload(level);
        }
        if (event.getLevel() instanceof ServerLevel) {
            FakePlayerProvider.INSTANCE.unloadWorld((ServerLevel) event.getLevel());
        }
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post event) {
        BCAdvDebugging.INSTANCE.onServerPostTick();
        MessageUtil.postServerTick();
    }
}
