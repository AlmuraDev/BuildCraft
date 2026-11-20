/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders;

import net.neoforged.bus.api.IEventBus;

import buildcraft.lib.registry.RegistrationHelper;

import buildcraft.builders.block.BlockArchitectTable;
import buildcraft.builders.block.BlockBuilder;
import buildcraft.builders.block.BlockElectronicLibrary;
import buildcraft.builders.block.BlockFiller;
import buildcraft.builders.block.BlockFrame;
import buildcraft.builders.block.BlockQuarry;
import buildcraft.builders.block.BlockReplacer;
import buildcraft.builders.tile.TileArchitectTable;
import buildcraft.builders.tile.TileBuilder;
import buildcraft.builders.tile.TileElectronicLibrary;
import buildcraft.builders.tile.TileFiller;
import buildcraft.builders.tile.TileQuarry;
import buildcraft.builders.tile.TileReplacer;

public class BCBuildersBlocks {

    public static BlockFiller filler;
    public static BlockBuilder builder;
    public static BlockArchitectTable architect;
    public static BlockElectronicLibrary library;
    public static BlockReplacer replacer;
    public static BlockFrame frame;
    public static BlockQuarry quarry;

    public static void fmlPreInit(IEventBus modBus) {
        RegistrationHelper helper = new RegistrationHelper(BCBuilders.MODID, modBus);
        helper.addBlockAndItem("block.filler", BlockFiller::new, b -> filler = b);
        helper.addBlockAndItem("block.builder", BlockBuilder::new, b -> builder = b);
        helper.addBlockAndItem("block.architect", BlockArchitectTable::new, b -> architect = b);
        helper.addBlockAndItem("block.library", BlockElectronicLibrary::new, b -> library = b);
        helper.addBlockAndItem("block.replacer", BlockReplacer::new, b -> replacer = b);
        helper.addBlockAndItem("block.frame", BlockFrame::new, b -> frame = b);
        helper.addBlockAndItem("block.quarry", BlockQuarry::new, b -> quarry = b);

        helper.registerTile("tile.filler", TileFiller::new, () -> filler);
        helper.registerTile("tile.builder", TileBuilder::new, () -> builder);
        helper.registerTile("tile.architect", TileArchitectTable::new, () -> architect);
        helper.registerTile("tile.library", TileElectronicLibrary::new, () -> library);
        helper.registerTile("tile.replacer", TileReplacer::new, () -> replacer);
        helper.registerTile("tile.quarry", TileQuarry::new, () -> quarry);
    }
}
