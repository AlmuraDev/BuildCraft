/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders;

import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.loading.FMLEnvironment;

import buildcraft.api.BCModules;

import buildcraft.lib.net.MessageManager;

import buildcraft.builders.snapshot.MessageSnapshotRequest;
import buildcraft.builders.snapshot.MessageSnapshotResponse;

/** Builders' proxy. The client one ({@code buildcraft.builders.client.BCBuildersClientProxy}) is created by name so the
 * server never loads it. */
public abstract class BCBuildersProxy {
    private static final BCBuildersProxy proxy = createProxy();

    private static BCBuildersProxy createProxy() {
        if (FMLEnvironment.getDist().isClient()) {
            try {
                return (BCBuildersProxy) Class.forName("buildcraft.builders.client.BCBuildersClientProxy")
                    .getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Couldn't create the client proxy!", e);
            }
        }
        return new ServerProxy();
    }

    public static BCBuildersProxy getProxy() {
        return proxy;
    }

    public void fmlPreInit() {
        MessageManager.registerMessageClass(BCModules.BUILDERS, MessageSnapshotRequest.class,
            MessageSnapshotRequest.HANDLER, LogicalSide.SERVER);
        MessageManager.registerMessageClass(BCModules.BUILDERS, MessageSnapshotResponse.class, LogicalSide.CLIENT);
    }

    public void fmlInit() {}

    public void fmlPostInit() {}

    public static class ServerProxy extends BCBuildersProxy {}
}
