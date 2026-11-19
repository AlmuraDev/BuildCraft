/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.core;

import java.util.List;

import net.minecraft.world.level.Level;

import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.loading.FMLEnvironment;

import buildcraft.api.BCModules;

import buildcraft.lib.net.MessageManager;

import buildcraft.core.marker.volume.MessageVolumeBoxes;
import buildcraft.core.marker.volume.VolumeBox;
import buildcraft.core.marker.volume.WorldSavedDataVolumeBoxes;

/** Things core does differently on the client and the server. The client's version is
 * buildcraft.core.client.BCCoreClientProxy, which is only loaded on the client (by name, so the server never sees it). */
public abstract class BCCoreProxy {
    private static final BCCoreProxy proxy = createProxy();

    private static BCCoreProxy createProxy() {
        if (FMLEnvironment.getDist().isClient()) {
            try {
                return (BCCoreProxy) Class.forName("buildcraft.core.client.BCCoreClientProxy").getDeclaredConstructor()
                    .newInstance();
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Couldn't create the client proxy!", e);
            }
        }
        return new ServerProxy();
    }

    public static BCCoreProxy getProxy() {
        return proxy;
    }

    public void fmlPreInit() {
        MessageManager.registerMessageClass(BCModules.CORE, MessageVolumeBoxes.class, LogicalSide.CLIENT);
    }

    public void fmlInit() {}

    public void fmlPostInit() {}

    public List<VolumeBox> getVolumeBoxes(Level world) {
        return WorldSavedDataVolumeBoxes.get(world).volumeBoxes;
    }

    public static class ServerProxy extends BCCoreProxy {}
}
