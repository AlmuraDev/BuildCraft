/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon;

import net.neoforged.bus.api.IEventBus;

import buildcraft.api.enums.EnumLaserTableType;

import buildcraft.lib.BCLib;
import buildcraft.lib.registry.RegistrationHelper;

import buildcraft.silicon.block.BlockLaser;
import buildcraft.silicon.block.BlockLaserTable;
import buildcraft.silicon.tile.TileAdvancedCraftingTable;
import buildcraft.silicon.tile.TileAssemblyTable;
import buildcraft.silicon.tile.TileChargingTable;
import buildcraft.silicon.tile.TileIntegrationTable;
import buildcraft.silicon.tile.TileLaser;
import buildcraft.silicon.tile.TileProgrammingTable_Neptune;

public class BCSiliconBlocks {

    public static BlockLaser laser;

    public static BlockLaserTable assemblyTable;
    public static BlockLaserTable advancedCraftingTable;
    public static BlockLaserTable integrationTable;
    public static BlockLaserTable chargingTable;
    public static BlockLaserTable programmingTable;

    public static void preInit(IEventBus modBus) {
        RegistrationHelper helper = new RegistrationHelper(BCSilicon.MODID, modBus);
        helper.addBlockAndItem("block.laser", BlockLaser::new, b -> laser = b);
        helper.addBlockAndItem("block.assembly_table",
            (p, id) -> new BlockLaserTable(EnumLaserTableType.ASSEMBLY_TABLE, p, id), b -> assemblyTable = b);
        helper.addBlockAndItem("block.advanced_crafting_table",
            (p, id) -> new BlockLaserTable(EnumLaserTableType.ADVANCED_CRAFTING_TABLE, p, id),
            b -> advancedCraftingTable = b);
        helper.addBlockAndItem("block.integration_table",
            (p, id) -> new BlockLaserTable(EnumLaserTableType.INTEGRATION_TABLE, p, id), b -> integrationTable = b);
        if (BCLib.DEV) {
            helper.addBlockAndItem("block.charging_table",
                (p, id) -> new BlockLaserTable(EnumLaserTableType.CHARGING_TABLE, p, id), b -> chargingTable = b);
            helper.addBlockAndItem("block.programming_table",
                (p, id) -> new BlockLaserTable(EnumLaserTableType.PROGRAMMING_TABLE, p, id),
                b -> programmingTable = b);
        }

        helper.registerTile("tile.laser", TileLaser::new, () -> laser);
        helper.registerTile("tile.assembly_table", TileAssemblyTable::new, () -> assemblyTable);
        helper.registerTile("tile.advanced_crafting_table", TileAdvancedCraftingTable::new, () -> advancedCraftingTable);
        helper.registerTile("tile.integration_table", TileIntegrationTable::new, () -> integrationTable);
        if (BCLib.DEV) {
            helper.registerTile("tile.charging_table", TileChargingTable::new, () -> chargingTable);
            helper.registerTile("tile.programming_table", TileProgrammingTable_Neptune::new, () -> programmingTable);
        }
    }
}
