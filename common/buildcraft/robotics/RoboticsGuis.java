/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.robotics;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

import net.neoforged.bus.api.IEventBus;

import buildcraft.lib.gui.BCMenu;

import buildcraft.robotics.container.ContainerZonePlanner;
import buildcraft.robotics.tile.TileZonePlanner;

public enum RoboticsGuis {
    ZONE_PLANTER;

    public static BCMenu<ContainerZonePlanner> zonePlanner;

    public static void fmlPreInit(IEventBus modBus) {
        var register = BCMenu.createRegister(BCRobotics.MODID, modBus);
        zonePlanner = BCMenu.tile(register, "zone_planner", TileZonePlanner.class, ContainerZonePlanner::new);
    }

    public void openGUI(Player player) {
        openGUI(player, player.blockPosition());
    }

    public void openGUI(Player player, BlockPos pos) {
        zonePlanner.open(player, pos);
    }
}
