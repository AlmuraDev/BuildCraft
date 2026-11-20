/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

import buildcraft.api.core.BCLog;

public enum BCSiliconGuis {
    ASSEMBLY_TABLE,
    ADVANCED_CRAFTING_TABLE,
    INTEGRATION_TABLE,
    GATE;

    // TODO(26.3 GUIs batch): open the laser table and gate menus (silicon/container, silicon/gui) once containers
    // have menu types.
    public void openGUI(Player player) {
        BCLog.logger.debug("[silicon.gui] " + name() + " isn't ported to 26.3 yet");
    }

    public void openGUI(Player player, BlockPos pos) {
        openGui(player, pos, 0);
    }

    public void openGui(Player player, BlockPos pos, int data) {
        openGUI(player);
    }
}
