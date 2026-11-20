/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.factory;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

import net.neoforged.bus.api.IEventBus;

import buildcraft.api.core.BCLog;

import buildcraft.lib.gui.BCMenu;

import buildcraft.factory.container.ContainerAutoCraftItems;
import buildcraft.factory.container.ContainerChute;
import buildcraft.factory.container.ContainerDistiller;
import buildcraft.factory.container.ContainerTank;
import buildcraft.factory.tile.TileAutoWorkbenchItems;
import buildcraft.factory.tile.TileChute;
import buildcraft.factory.tile.TileDistiller_BC8;
import buildcraft.factory.tile.TileTank;

public enum BCFactoryGuis {
    AUTO_WORKBENCH_ITEMS,
    AUTO_WORKBENCH_FLUIDS,
    CHUTE,
    TANK,
    DISTILLER;

    public static BCMenu<ContainerAutoCraftItems> autoWorkbenchItems;
    public static BCMenu<ContainerChute> chute;
    public static BCMenu<ContainerTank> tank;
    public static BCMenu<ContainerDistiller> distiller;

    public static void fmlPreInit(IEventBus modBus) {
        var register = BCMenu.createRegister(BCFactory.MODID, modBus);
        autoWorkbenchItems = BCMenu.tile(register, "autoworkbench_item", TileAutoWorkbenchItems.class,
            ContainerAutoCraftItems::new);
        chute = BCMenu.tile(register, "chute", TileChute.class, ContainerChute::new);
        tank = BCMenu.tile(register, "tank", TileTank.class, ContainerTank::new);
        distiller = BCMenu.tile(register, "distiller", TileDistiller_BC8.class, ContainerDistiller::new);
    }

    public void openGUI(Player player) {
        openGUI(player, player.blockPosition());
    }

    public void openGUI(Player player, BlockPos pos) {
        switch (this) {
            case AUTO_WORKBENCH_ITEMS:
                autoWorkbenchItems.open(player, pos);
                break;
            case CHUTE:
                chute.open(player, pos);
                break;
            case TANK:
                tank.open(player, pos);
                break;
            case DISTILLER:
                distiller.open(player, pos);
                break;
            default:
                // 1.12.2 had no fluid auto workbench GUI either
                BCLog.logger.debug("[factory.gui] " + name() + " has no gui");
        }
    }
}
