/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.core;

import net.neoforged.bus.api.IEventBus;

import buildcraft.api.enums.EnumEngineType;

import buildcraft.lib.BCLib;
import buildcraft.lib.registry.RegistrationHelper;

import buildcraft.core.block.BlockDecoration;
import buildcraft.core.block.BlockEngine_BC8;
import buildcraft.core.block.BlockMarkerPath;
import buildcraft.core.block.BlockMarkerVolume;
import buildcraft.core.block.BlockPowerConsumerTester;
import buildcraft.core.block.BlockSpring;
import buildcraft.core.item.ItemBlockDecorated;
import buildcraft.core.item.ItemBlockSpring;
import buildcraft.core.item.ItemEngine_BC8;
import buildcraft.core.tile.TileEngineCreative;
import buildcraft.core.tile.TileEngineRedstone_BC8;
import buildcraft.core.tile.TileMarkerPath;
import buildcraft.core.tile.TileMarkerVolume;
import buildcraft.core.tile.TilePowerConsumerTester;

public class BCCoreBlocks {

    public static BlockEngine_BC8 engine;
    public static BlockSpring spring;
    public static BlockDecoration decorated;
    public static BlockMarkerVolume markerVolume;
    public static BlockMarkerPath markerPath;
    public static BlockPowerConsumerTester powerTester;

    public static void preInit(IEventBus modBus) {
        RegistrationHelper helper = new RegistrationHelper(BCCore.MODID, modBus);
        helper.addBlockAndItem("block.spring", false, BlockSpring::new, ItemBlockSpring::new, b -> spring = b);
        helper.addBlockAndItem("block.decorated", false, BlockDecoration::new, ItemBlockDecorated::new, b -> decorated = b);
        helper.addBlockAndItem("block.marker.volume", BlockMarkerVolume::new, b -> markerVolume = b);
        helper.addBlockAndItem("block.marker.path", BlockMarkerPath::new, b -> markerPath = b);
        if (BCLib.DEV) {
            helper.addBlockAndItem("block.power_tester", BlockPowerConsumerTester::new, b -> powerTester = b);
        }

        helper.addBlockAndItem("block.engine.bc", false, BlockEngine_BC8::new, ItemEngine_BC8<EnumEngineType>::new,
            b -> engine = b);
        helper.registerEngineTile("tile.engine.wood", "engine", EnumEngineType.WOOD, TileEngineRedstone_BC8::new,
            () -> engine);
        helper.registerEngineTile("tile.engine.creative", "engine", EnumEngineType.CREATIVE, TileEngineCreative::new,
            () -> engine);

        helper.registerTile("tile.marker.volume", TileMarkerVolume::new, () -> markerVolume);
        helper.registerTile("tile.marker.path", TileMarkerPath::new, () -> markerPath);
        if (BCLib.DEV) {
            helper.registerTile("tile.power_tester", TilePowerConsumerTester::new, () -> powerTester);
        }
    }
}
