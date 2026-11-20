/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.factory;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

import buildcraft.api.core.BCLog;

public enum BCFactoryGuis {
    AUTO_WORKBENCH_ITEMS,
    AUTO_WORKBENCH_FLUIDS,
    CHUTE,
    TANK,
    DISTILLER;

    // TODO(26.3 GUIs batch): open the auto workbench, chute, tank and distiller menus (factory/container,
    // factory/gui) once containers have menu types.
    public void openGUI(Player player) {
        BCLog.logger.debug("[factory.gui] " + name() + " isn't ported to 26.3 yet");
    }

    public void openGUI(Player player, BlockPos pos) {
        openGUI(player);
    }
}
