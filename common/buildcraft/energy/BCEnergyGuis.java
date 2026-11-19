/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

import buildcraft.api.core.BCLog;

public enum BCEnergyGuis {
    ENGINE_STONE,
    ENGINE_IRON,
    ENGINE_RF,
    DYNAMO_MJ;

    public static final BCEnergyGuis[] VALUES = values();

    public static BCEnergyGuis get(int id) {
        if (id < 0 || id >= VALUES.length) return null;
        return VALUES[id];
    }

    // TODO(26.3 GUIs batch): open the engine and dynamo menus (energy/container, energy/client/gui) once containers
    // have menu types.
    public void openGUI(Player player) {
        BCLog.logger.debug("[energy.gui] " + name() + " isn't ported to 26.3 yet");
    }

    public void openGUI(Player player, BlockPos pos) {
        openGUI(player);
    }
}
