/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport;

import net.minecraft.world.entity.Entity;

import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.loading.FMLEnvironment;

import buildcraft.api.BCModules;

import buildcraft.lib.net.MessageManager;

import buildcraft.transport.net.MessageMultiPipeItem;
import buildcraft.transport.tile.TilePipeHolder;
import buildcraft.transport.wire.MessageWireSystems;
import buildcraft.transport.wire.MessageWireSystemsPowered;

/** Things transport does differently on the client and the server. The client's version is
 * buildcraft.transport.client.BCTransportClientProxy, which is only loaded on the client (by name, so the server never
 * sees it). */
public abstract class BCTransportProxy {
    private static final BCTransportProxy proxy = createProxy();

    private static BCTransportProxy createProxy() {
        if (FMLEnvironment.getDist().isClient()) {
            try {
                return (BCTransportProxy) Class.forName("buildcraft.transport.client.BCTransportClientProxy")
                    .getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Couldn't create the client proxy!", e);
            }
        }
        return new ServerProxy();
    }

    public static BCTransportProxy getProxy() {
        return proxy;
    }

    public void fmlPreInit() {
        MessageManager.registerMessageClass(BCModules.TRANSPORT, MessageWireSystems.class, LogicalSide.CLIENT);
        MessageManager.registerMessageClass(BCModules.TRANSPORT, MessageWireSystemsPowered.class, LogicalSide.CLIENT);
        MessageManager.registerMessageClass(BCModules.TRANSPORT, MessageMultiPipeItem.class, LogicalSide.CLIENT);
    }

    public void fmlInit() {}

    public void fmlPostInit() {}

    /** Spawns the particles of an entity landing on a pipe (client only). */
    public void spawnLandingParticles(TilePipeHolder pipe, double x, double y, double z, int count) {}

    /** Spawns the particles of an entity running over a pipe (client only). */
    public boolean addRunningEffects(TilePipeHolder pipe, Entity entity) {
        return false;
    }

    public static class ServerProxy extends BCTransportProxy {}
}
