/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.core;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

import buildcraft.api.core.BCLog;

public enum BCCoreGuis {
    LIST;

    // TODO(26.3 GUIs batch): open the list's menu (ContainerList) once containers have menu types.
    public void openGUI(Player player) {
        BCLog.logger.debug("[core.gui] " + name() + " isn't ported to 26.3 yet");
    }

    public void openGUI(Player player, BlockPos pos) {
        openGUI(player);
    }
}
