/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.core.client;

import java.util.List;

import net.minecraft.world.level.Level;

import net.neoforged.fml.LogicalSide;

import buildcraft.lib.net.MessageManager;


import buildcraft.lib.client.render.DetachedRenderer;
import buildcraft.lib.client.render.DetachedRenderer.RenderMatrixType;

import buildcraft.core.BCCore;
import buildcraft.core.client.render.RenderMarkerConnections;
import buildcraft.core.client.render.RenderVolumeBoxes;
import buildcraft.core.BCCoreModels;
import buildcraft.core.BCCoreProxy;
import buildcraft.core.BCCoreSprites;
import buildcraft.core.marker.volume.ClientVolumeBoxes;
import buildcraft.core.marker.volume.MessageVolumeBoxes;
import buildcraft.core.marker.volume.VolumeBox;

/** Core's client proxy, created by name from {@link BCCoreProxy} so the server never loads it. */
public class BCCoreClientProxy extends BCCoreProxy {
    @Override
    public void fmlPreInit() {
        super.fmlPreInit();
        BCCoreSprites.fmlPreInit();
        BCCoreModels.fmlPreInit();
        ConfigScreenBC.register();
        BCCore.MOD_BUS.addListener(BCCoreModels::registerRenderers);
        DetachedRenderer.INSTANCE.addRenderer(RenderMatrixType.FROM_WORLD_ORIGIN, RenderVolumeBoxes.INSTANCE);
        RenderMarkerConnections.register();
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(buildcraft.core.list.ListTooltipHandler.INSTANCE);
        BCCore.MOD_BUS.addListener((net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) ->
            buildcraft.lib.client.BCMenuScreens.register(event, buildcraft.core.BCCoreGuis.listMenu,
                buildcraft.core.list.GuiList::new));
        MessageManager.setHandler(MessageVolumeBoxes.class, MessageVolumeBoxes.HANDLER, LogicalSide.CLIENT);
    }

    @Override
    public void fmlInit() {
        super.fmlInit();
        DetachedRenderer.INSTANCE.addRenderer(RenderMatrixType.FROM_WORLD_ORIGIN, RenderTickListener.HELD_ITEM_RENDERER);
        BCCore.MOD_BUS.addListener(RenderTickListener::registerDebugEntry);
    }

    @Override
    public List<VolumeBox> getVolumeBoxes(Level world) {
        return world.isClientSide() ? ClientVolumeBoxes.INSTANCE.volumeBoxes : super.getVolumeBoxes(world);
    }
}
