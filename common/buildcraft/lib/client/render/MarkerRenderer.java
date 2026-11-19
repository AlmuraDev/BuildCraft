/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.client.render;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import net.minecraft.world.entity.player.Player;

import buildcraft.lib.debug.IDetachedRenderer;
import buildcraft.lib.marker.MarkerCache;
import buildcraft.lib.marker.MarkerConnection;
import buildcraft.lib.marker.MarkerSubCache;

public enum MarkerRenderer implements IDetachedRenderer {
    INSTANCE;

    /** How each kind of connection draws itself. Marker connections are common code, so their drawing is registered
     * here by the client. */
    private static final Map<Class<?>, Consumer<MarkerConnection<?>>> RENDERERS = new HashMap<>();

    @SuppressWarnings("unchecked")
    public static <C extends MarkerConnection<C>> void registerRenderer(Class<C> connectionClass, Consumer<C> renderer) {
        RENDERERS.put(connectionClass, connection -> renderer.accept((C) connection));
    }

    @Override
    public void render(Player player, float partialTicks) {
        for (MarkerCache<? extends MarkerSubCache<?>> cache : MarkerCache.CACHES) {
            for (MarkerConnection<?> connection : cache.getSubCache(player.level()).getConnections()) {
                Consumer<MarkerConnection<?>> renderer = RENDERERS.get(connection.getClass());
                if (renderer != null) {
                    renderer.accept(connection);
                }
            }
        }
    }
}
