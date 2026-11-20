/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon;

import net.neoforged.fml.loading.FMLEnvironment;

/** Silicon's proxy. The client one ({@code buildcraft.silicon.client.BCSiliconClientProxy}) is created by name so the
 * server never loads it. */
public abstract class BCSiliconProxy {
    private static final BCSiliconProxy proxy = createProxy();

    private static BCSiliconProxy createProxy() {
        if (FMLEnvironment.getDist().isClient()) {
            try {
                return (BCSiliconProxy) Class.forName("buildcraft.silicon.client.BCSiliconClientProxy")
                    .getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Couldn't create the client proxy!", e);
            }
        }
        return new ServerProxy();
    }

    public static BCSiliconProxy getProxy() {
        return proxy;
    }

    public void fmlPreInit() {}

    public void fmlInit() {}

    public void fmlPostInit() {}

    public static class ServerProxy extends BCSiliconProxy {}
}
