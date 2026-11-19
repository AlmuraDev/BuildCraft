/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.energy;

import net.neoforged.fml.loading.FMLEnvironment;

/** Energy's proxy. The client one ({@code buildcraft.energy.client.BCEnergyClientProxy}) is created by name so the
 * server never loads it. */
public abstract class BCEnergyProxy {
    private static final BCEnergyProxy proxy = createProxy();

    private static BCEnergyProxy createProxy() {
        if (FMLEnvironment.getDist().isClient()) {
            try {
                return (BCEnergyProxy) Class.forName("buildcraft.energy.client.BCEnergyClientProxy")
                    .getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Couldn't create the client proxy!", e);
            }
        }
        return new ServerProxy();
    }

    public static BCEnergyProxy getProxy() {
        return proxy;
    }

    public void fmlPreInit() {}

    public void fmlInit() {}

    public void fmlPostInit() {}

    public static class ServerProxy extends BCEnergyProxy {}
}
