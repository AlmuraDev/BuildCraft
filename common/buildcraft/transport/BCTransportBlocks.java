/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport;

import net.neoforged.bus.api.IEventBus;

import buildcraft.lib.registry.RegistrationHelper;

import buildcraft.transport.block.BlockFilteredBuffer;
import buildcraft.transport.block.BlockPipeHolder;
import buildcraft.transport.tile.TileFilteredBuffer;
import buildcraft.transport.tile.TilePipeHolder;

public class BCTransportBlocks {

    public static BlockFilteredBuffer filteredBuffer;
    public static BlockPipeHolder pipeHolder;

    public static void preInit(IEventBus modBus) {
        RegistrationHelper helper = new RegistrationHelper(BCTransport.MODID, modBus);
        helper.addBlockAndItem("block.filtered_buffer", BlockFilteredBuffer::new, b -> filteredBuffer = b);
        helper.addBlock("block.pipe_holder", true, BlockPipeHolder::new, b -> pipeHolder = b);

        helper.registerTile("tile.filtered_buffer", TileFilteredBuffer::new, () -> filteredBuffer);
        helper.registerTile("tile.pipe_holder", TilePipeHolder::new, () -> pipeHolder);
    }
}
