/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.core;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

import net.neoforged.bus.api.IEventBus;

import buildcraft.lib.gui.BCMenu;

import buildcraft.core.list.ContainerList;

public enum BCCoreGuis {
    LIST;

    /** The list's menu (it reads the list from the player's hands, so it needs no position). */
    public static BCMenu<ContainerList> listMenu;

    public static void fmlPreInit(IEventBus modBus) {
        var register = BCMenu.createRegister(BCCore.MODID, modBus);
        listMenu = BCMenu.register(register, "list", (type, id, player, pos, data) -> new ContainerList(type, id, player));
    }

    public void openGUI(Player player) {
        openGUI(player, player.blockPosition());
    }

    public void openGUI(Player player, BlockPos pos) {
        switch (this) {
            case LIST:
                listMenu.open(player, pos);
                break;
        }
    }
}
