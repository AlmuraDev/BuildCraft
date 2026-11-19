/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.energy;

import net.neoforged.bus.api.IEventBus;

import buildcraft.api.enums.EnumEngineType;
import buildcraft.api.enums.EnumSpring;

import buildcraft.lib.registry.RegistrationHelper;

import buildcraft.core.BCCoreBlocks;
import buildcraft.energy.blocks.BlockDynamoMJ;
import buildcraft.energy.tile.TileDynamoMJ;
import buildcraft.energy.tile.TileEngineIron_BC8;
import buildcraft.energy.tile.TileEngineRF;
import buildcraft.energy.tile.TileEngineStone_BC8;
import buildcraft.energy.tile.TileSpringOil;

public class BCEnergyBlocks {

    public static BlockDynamoMJ mjDynamo;

    public static void preInit(IEventBus modBus) {
        RegistrationHelper helper = new RegistrationHelper(BCEnergy.MODID, modBus);

        // The engines are states of core's engine block
        helper.registerEngineTile("tile.engine.stone", "engine", EnumEngineType.STONE, TileEngineStone_BC8::new,
            () -> BCCoreBlocks.engine);
        helper.registerEngineTile("tile.engine.iron", "engine", EnumEngineType.IRON, TileEngineIron_BC8::new,
            () -> BCCoreBlocks.engine);
        if (BCEnergyConfig.enableRfEngine) {
            helper.registerEngineTile("tile.engine.rf", "engine", EnumEngineType.RF, TileEngineRF::new,
                () -> BCCoreBlocks.engine);
        }

        if (BCEnergyConfig.enableMjDynamo) {
            helper.addBlockAndItem("block.mj_dynamo", true, BlockDynamoMJ::new, b -> mjDynamo = b);
            helper.registerTile("tile.mj_dynamo", TileDynamoMJ::new, () -> mjDynamo);
        }

        // The oil spring is a state of core's spring block
        helper.registerStateTile("tile.spring.oil", TileSpringOil::new, () -> BCCoreBlocks.spring,
            type -> EnumSpring.OIL.tileType = type);
    }

    /** Common setup: the fluid blocks exist now. */
    public static void init() {
        EnumSpring.OIL.liquidBlock = BCEnergyFluids.crudeOil[0].getBlock().defaultBlockState();
    }
}
