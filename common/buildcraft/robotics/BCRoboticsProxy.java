/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.robotics;

import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.loading.FMLEnvironment;

import buildcraft.api.BCModules;

import buildcraft.lib.net.MessageManager;

import buildcraft.robotics.zone.MessageZoneMapRequest;
import buildcraft.robotics.zone.MessageZoneMapResponse;

/** Robotics' proxy. The client one ({@code buildcraft.robotics.client.BCRoboticsClientProxy}) is created by name so the
 * server never loads it. */
public abstract class BCRoboticsProxy {
    private static final BCRoboticsProxy proxy = createProxy();

    private static BCRoboticsProxy createProxy() {
        if (FMLEnvironment.getDist().isClient()) {
            try {
                return (BCRoboticsProxy) Class.forName("buildcraft.robotics.client.BCRoboticsClientProxy")
                    .getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Couldn't create the client proxy!", e);
            }
        }
        return new ServerProxy();
    }

    public static BCRoboticsProxy getProxy() {
        return proxy;
    }

    public void fmlPreInit() {
        MessageManager.registerMessageClass(BCModules.ROBOTICS, MessageZoneMapRequest.class,
            MessageZoneMapRequest.HANDLER, LogicalSide.SERVER);
        MessageManager.registerMessageClass(BCModules.ROBOTICS, MessageZoneMapResponse.class, LogicalSide.CLIENT);
    }

    public void fmlInit() {}

    public void fmlPostInit() {}

    public static class ServerProxy extends BCRoboticsProxy {}
}
