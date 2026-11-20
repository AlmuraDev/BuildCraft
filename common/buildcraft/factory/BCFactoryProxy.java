/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory;

import net.neoforged.fml.loading.FMLEnvironment;

/** Factory's proxy. The client one ({@code buildcraft.factory.client.BCFactoryClientProxy}) is created by name so the
 * server never loads it. */
public abstract class BCFactoryProxy {
    private static final BCFactoryProxy proxy = createProxy();

    private static BCFactoryProxy createProxy() {
        if (FMLEnvironment.getDist().isClient()) {
            try {
                return (BCFactoryProxy) Class.forName("buildcraft.factory.client.BCFactoryClientProxy")
                    .getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Couldn't create the client proxy!", e);
            }
        }
        return new ServerProxy();
    }

    public static BCFactoryProxy getProxy() {
        return proxy;
    }

    public void fmlPreInit() {}

    public void fmlInit() {}

    public void fmlPostInit() {}

    public static class ServerProxy extends BCFactoryProxy {}
}
