/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.bus.api.IEventBus;

import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.api.transport.pluggable.PipePluggable;

import buildcraft.lib.gui.BCMenu;
import buildcraft.lib.misc.MessageUtil;

import buildcraft.silicon.container.ContainerAdvancedCraftingTable;
import buildcraft.silicon.container.ContainerAssemblyTable;
import buildcraft.silicon.container.ContainerGate;
import buildcraft.silicon.container.ContainerIntegrationTable;
import buildcraft.silicon.plug.PluggableGate;
import buildcraft.silicon.tile.TileAdvancedCraftingTable;
import buildcraft.silicon.tile.TileAssemblyTable;
import buildcraft.silicon.tile.TileIntegrationTable;

public enum BCSiliconGuis {
    ASSEMBLY_TABLE,
    ADVANCED_CRAFTING_TABLE,
    INTEGRATION_TABLE,
    GATE;

    public static BCMenu<ContainerAssemblyTable> assemblyTable;
    public static BCMenu<ContainerAdvancedCraftingTable> advancedCraftingTable;
    public static BCMenu<ContainerIntegrationTable> integrationTable;
    /** The gate on the pipe at the position, on the side given as the menu's data. */
    public static BCMenu<ContainerGate> gate;

    public static void fmlPreInit(IEventBus modBus) {
        var register = BCMenu.createRegister(BCSilicon.MODID, modBus);
        assemblyTable = BCMenu.tile(register, "assembly_table", TileAssemblyTable.class, ContainerAssemblyTable::new);
        advancedCraftingTable = BCMenu.tile(register, "advanced_crafting_table", TileAdvancedCraftingTable.class,
            ContainerAdvancedCraftingTable::new);
        integrationTable = BCMenu.tile(register, "integration_table", TileIntegrationTable.class,
            ContainerIntegrationTable::new);
        gate = BCMenu.register(register, "gate", (type, id, player, pos, data) -> {
            BlockEntity tile = player.level().getBlockEntity(pos);
            if (!(tile instanceof IPipeHolder holder) || data < 0 || data >= 6) {
                return null;
            }
            PipePluggable plug = holder.getPluggable(Direction.from3DDataValue(data));
            if (!(plug instanceof PluggableGate gatePlug)) {
                return null;
            }
            ContainerGate container = new ContainerGate(type, id, player, gatePlug.logic);
            if (!player.level().isClientSide()) {
                MessageUtil.doDelayedServer(() -> container.sendMessage(ContainerGate.ID_VALID_STATEMENTS));
            }
            return container;
        });
    }

    public void openGUI(Player player) {
        openGUI(player, player.blockPosition());
    }

    public void openGUI(Player player, BlockPos pos) {
        openGui(player, pos, 0);
    }

    public void openGui(Player player, BlockPos pos, int data) {
        switch (this) {
            case ASSEMBLY_TABLE:
                assemblyTable.open(player, pos, data);
                break;
            case ADVANCED_CRAFTING_TABLE:
                advancedCraftingTable.open(player, pos, data);
                break;
            case INTEGRATION_TABLE:
                integrationTable.open(player, pos, data);
                break;
            case GATE:
                gate.open(player, pos, data);
                break;
        }
    }
}
