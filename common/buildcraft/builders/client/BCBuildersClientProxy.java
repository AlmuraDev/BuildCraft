/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.client;

import net.minecraft.client.Minecraft;

import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

import buildcraft.lib.net.MessageManager;

import buildcraft.builders.BCBuildersModels;
import buildcraft.builders.BCBuildersProxy;
import buildcraft.builders.snapshot.MessageSnapshotResponse;

/** Builders' client proxy, created by name from {@link BCBuildersProxy} so the server never loads it.
 * TODO(26.3 GUIs batch): the architect table, builder, filler, library, replacer and filler planner GUIs, and the
 * blueprint previews in tooltips (ClientSnapshots.renderSnapshot). 1.12.2 also turned on the stencil buffer for the
 * architect table's scan preview (BCBuildersConfig.enableStencil); 26.3's main render target always has one. */
public class BCBuildersClientProxy extends BCBuildersProxy {
    @Override
    public void fmlPreInit() {
        super.fmlPreInit();
        BCBuildersModels.fmlPreInit();
        MessageManager.setHandler(MessageSnapshotResponse.class, MessageSnapshotResponse.HANDLER, LogicalSide.CLIENT);
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
            if (!Minecraft.getInstance().isPaused()) {
                ClientArchitectTables.tick();
            }
        });
    }
}
