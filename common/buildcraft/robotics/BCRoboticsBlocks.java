/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.robotics;

import net.neoforged.bus.api.IEventBus;

import buildcraft.lib.registry.RegistrationHelper;

import buildcraft.robotics.block.BlockZonePlanner;
import buildcraft.robotics.tile.TileZonePlanner;

public class BCRoboticsBlocks {
    public static BlockZonePlanner zonePlanner;

    public static void fmlPreInit(IEventBus modBus) {
        RegistrationHelper helper = new RegistrationHelper(BCRobotics.MODID, modBus);
        helper.addBlockAndItem("block.zone_planner", BlockZonePlanner::new, b -> zonePlanner = b);
        helper.registerTile("tile.zone_planner", TileZonePlanner::new, () -> zonePlanner);
    }
}
