/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.world;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Per-world {@link WorldEventListenerAdapter block change listeners} (1.12.2's world.addEventListener). Fed by NeoForge's
 * neighbour notify event, which fires whenever a block changes and updates its neighbours. Registered on the game event
 * bus by buildcraftlib. */
public final class WorldEventListeners {
    private WorldEventListeners() {}

    private static final Map<Level, List<WorldEventListenerAdapter>> LISTENERS = new WeakHashMap<>();

    public static synchronized void add(Level world, WorldEventListenerAdapter listener) {
        LISTENERS.computeIfAbsent(world, w -> new CopyOnWriteArrayList<>()).add(listener);
    }

    public static synchronized void remove(Level world, WorldEventListenerAdapter listener) {
        List<WorldEventListenerAdapter> list = LISTENERS.get(world);
        if (list != null) {
            list.remove(listener);
        }
    }

    @SubscribeEvent
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (!(event.getLevel() instanceof Level world)) {
            return;
        }
        List<WorldEventListenerAdapter> list;
        synchronized (WorldEventListeners.class) {
            list = LISTENERS.get(world);
        }
        if (list == null || list.isEmpty()) {
            return;
        }
        for (WorldEventListenerAdapter listener : list) {
            listener.sendBlockUpdated(world, event.getPos(), event.getState(), event.getState(), Block.UPDATE_ALL);
        }
    }
}
