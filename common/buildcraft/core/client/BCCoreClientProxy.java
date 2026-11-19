/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.core.client;

import java.util.List;

import net.minecraft.world.level.Level;

import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.common.NeoForge;

import buildcraft.lib.client.render.DetachedRenderer;
import buildcraft.lib.client.render.DetachedRenderer.RenderMatrixType;
import buildcraft.lib.net.MessageManager;

import buildcraft.core.BCCoreModels;
import buildcraft.core.BCCoreProxy;
import buildcraft.core.BCCoreSprites;
import buildcraft.core.client.render.RenderMarkerConnections;
import buildcraft.core.client.render.RenderVolumeBoxes;
import buildcraft.core.list.ListTooltipHandler;
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
        DetachedRenderer.INSTANCE.addRenderer(RenderMatrixType.FROM_WORLD_ORIGIN, RenderVolumeBoxes.INSTANCE);
        RenderMarkerConnections.register();
        NeoForge.EVENT_BUS.register(ListTooltipHandler.INSTANCE);
        MessageManager.setHandler(MessageVolumeBoxes.class, MessageVolumeBoxes.HANDLER, LogicalSide.CLIENT);
    }

    @Override
    public void fmlInit() {
        super.fmlInit();
        BCCoreModels.fmlInit();
        NeoForge.EVENT_BUS.register(RenderTickListener.class);
    }

    @Override
    public List<VolumeBox> getVolumeBoxes(Level world) {
        return world.isClientSide() ? ClientVolumeBoxes.INSTANCE.volumeBoxes : super.getVolumeBoxes(world);
    }
}
