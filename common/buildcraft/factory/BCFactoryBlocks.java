/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.factory;

import net.neoforged.bus.api.IEventBus;

import buildcraft.lib.registry.RegistrationHelper;

import buildcraft.factory.block.BlockAutoWorkbenchItems;
import buildcraft.factory.block.BlockChute;
import buildcraft.factory.block.BlockDistiller;
import buildcraft.factory.block.BlockFloodGate;
import buildcraft.factory.block.BlockHeatExchange;
import buildcraft.factory.block.BlockMiningWell;
import buildcraft.factory.block.BlockPump;
import buildcraft.factory.block.BlockTank;
import buildcraft.factory.block.BlockTube;
import buildcraft.factory.block.BlockWaterGel;
import buildcraft.factory.tile.TileAutoWorkbenchItems;
import buildcraft.factory.tile.TileChute;
import buildcraft.factory.tile.TileDistiller_BC8;
import buildcraft.factory.tile.TileFloodGate;
import buildcraft.factory.tile.TileHeatExchange;
import buildcraft.factory.tile.TileMiningWell;
import buildcraft.factory.tile.TilePump;
import buildcraft.factory.tile.TileTank;

public class BCFactoryBlocks {

    public static BlockAutoWorkbenchItems autoWorkbenchItems;
    public static BlockMiningWell miningWell;
    public static BlockPump pump;
    public static BlockTube tube;
    public static BlockFloodGate floodGate;
    public static BlockTank tank;
    public static BlockChute chute;
    public static BlockDistiller distiller;
    public static BlockHeatExchange heatExchange;
    // public static BlockAutoWorkbenchFluids autoWorkbenchFluids;
    public static BlockWaterGel waterGel;

    public static void fmlPreInit(IEventBus modBus) {
        RegistrationHelper helper = new RegistrationHelper(BCFactory.MODID, modBus);
        helper.addBlockAndItem("block.autoworkbench.item", BlockAutoWorkbenchItems::new, b -> autoWorkbenchItems = b);
        helper.addBlockAndItem("block.mining_well", BlockMiningWell::new, b -> miningWell = b);
        helper.addBlockAndItem("block.pump", BlockPump::new, b -> pump = b);
        helper.addBlock("block.tube", BlockTube::new, b -> tube = b);
        helper.addBlockAndItem("block.flood_gate", BlockFloodGate::new, b -> floodGate = b);
        helper.addBlockAndItem("block.tank", BlockTank::new, b -> tank = b);
        helper.addBlockAndItem("block.chute", BlockChute::new, b -> chute = b);
        helper.addBlockAndItem("block.distiller", BlockDistiller::new, b -> distiller = b);
        helper.addBlockAndItem("block.heat_exchange", BlockHeatExchange::new, b -> heatExchange = b);
        helper.addBlock("block.water_gel", BlockWaterGel::new, b -> waterGel = b);

        helper.registerTile("tile.autoworkbench.item", TileAutoWorkbenchItems::new, () -> autoWorkbenchItems);
        helper.registerTile("tile.mining_well", TileMiningWell::new, () -> miningWell);
        helper.registerTile("tile.pump", TilePump::new, () -> pump);
        helper.registerTile("tile.flood_gate", TileFloodGate::new, () -> floodGate);
        helper.registerTile("tile.tank", TileTank::new, () -> tank);
        helper.registerTile("tile.chute", TileChute::new, () -> chute);
        helper.registerTile("tile.distiller", TileDistiller_BC8::new, () -> distiller);
        helper.registerTile("tile.heat_exchange", TileHeatExchange::new, () -> heatExchange);
    }
}
