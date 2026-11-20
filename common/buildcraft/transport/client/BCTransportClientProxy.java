/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.client;

import net.neoforged.fml.LogicalSide;

import buildcraft.api.transport.pipe.PipeApiClient;

import buildcraft.lib.net.MessageManager;

import buildcraft.transport.BCTransport;
import buildcraft.transport.BCTransportGuis;
import buildcraft.transport.BCTransportModels;
import buildcraft.transport.BCTransportProxy;
import buildcraft.transport.BCTransportSprites;
import buildcraft.transport.client.render.PipeWireRenderer;
import buildcraft.transport.net.MessageMultiPipeItem;
import buildcraft.transport.wire.MessageWireSystems;
import buildcraft.transport.wire.MessageWireSystemsPowered;

/** Transport's client proxy, created by name from {@link BCTransportProxy} so the server never loads it.
 * TODO(26.3 GUIs batch): the GUIs of the filtered buffer and the diamond, wooden diamond and emzuli pipes. */
public class BCTransportClientProxy extends BCTransportProxy {
    @Override
    public void fmlPreInit() {
        super.fmlPreInit();
        BCTransportSprites.fmlPreInit();
        BCTransport.MOD_BUS.addListener(BCTransportClientProxy::registerScreens);
        BCTransportModels.fmlPreInit();
        PipeApiClient.registry = PipeRegistryClient.INSTANCE;
        PipeWireRenderer.init();

        MessageManager.setHandler(MessageWireSystems.class, MessageWireSystems.HANDLER, LogicalSide.CLIENT);
        MessageManager.setHandler(MessageWireSystemsPowered.class, MessageWireSystemsPowered.HANDLER, LogicalSide.CLIENT);
        MessageManager.setHandler(MessageMultiPipeItem.class, MessageMultiPipeItem.HANDLER, LogicalSide.CLIENT);
    }

    @Override
    public void fmlInit() {
        super.fmlInit();
        BCTransportModels.fmlInit();
    }

    @Override
    public void fmlPostInit() {
        super.fmlPostInit();
        BCTransportModels.fmlPostInit();
    }

    private static void registerScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        buildcraft.lib.client.BCMenuScreens.register(event, BCTransportGuis.filteredBuffer,
            buildcraft.transport.gui.GuiFilteredBuffer::new);
        buildcraft.lib.client.BCMenuScreens.register(event, BCTransportGuis.pipeDiamond,
            buildcraft.transport.gui.GuiDiamondPipe::new);
        buildcraft.lib.client.BCMenuScreens.register(event, BCTransportGuis.pipeDiamondWood,
            buildcraft.transport.gui.GuiDiamondWoodPipe::new);
        buildcraft.lib.client.BCMenuScreens.register(event, BCTransportGuis.pipeEmzuli,
            buildcraft.transport.gui.GuiEmzuliPipe_BC8::new);
    }
}
