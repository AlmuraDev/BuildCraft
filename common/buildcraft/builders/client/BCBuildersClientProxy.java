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

import buildcraft.builders.BCBuilders;
import buildcraft.builders.BCBuildersGuis;
import buildcraft.builders.BCBuildersModels;
import buildcraft.builders.BCBuildersProxy;
import buildcraft.builders.snapshot.MessageSnapshotResponse;

/** Builders' client proxy, created by name from {@link BCBuildersProxy} so the server never loads it.
 * The replacer and tooltip snapshot previews are drawn by {@link SnapshotPreview}. 1.12.2 also turned on the stencil buffer for the
 * architect table's scan preview (BCBuildersConfig.enableStencil); 26.3's main render target always has one. */
public class BCBuildersClientProxy extends BCBuildersProxy {
    @Override
    public void fmlPreInit() {
        super.fmlPreInit();
        BCBuildersModels.fmlPreInit();
        BCBuilders.MOD_BUS.addListener(BCBuildersClientProxy::registerScreens);
        SnapshotPreview.register(BCBuilders.MOD_BUS);
        MessageManager.setHandler(MessageSnapshotResponse.class, MessageSnapshotResponse.HANDLER, LogicalSide.CLIENT);
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
            if (!Minecraft.getInstance().isPaused()) {
                ClientArchitectTables.tick();
            }
        });
    }

    private static void registerScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        buildcraft.lib.client.BCMenuScreens.register(event, BCBuildersGuis.architect,
            buildcraft.builders.gui.GuiArchitectTable::new);
        buildcraft.lib.client.BCMenuScreens.register(event, BCBuildersGuis.builder, buildcraft.builders.gui.GuiBuilder::new);
        buildcraft.lib.client.BCMenuScreens.register(event, BCBuildersGuis.filler, buildcraft.builders.gui.GuiFiller::new);
        buildcraft.lib.client.BCMenuScreens.register(event, BCBuildersGuis.library,
            buildcraft.builders.gui.GuiElectronicLibrary::new);
        buildcraft.lib.client.BCMenuScreens.register(event, BCBuildersGuis.replacer,
            buildcraft.builders.gui.GuiReplacer::new);
        buildcraft.lib.client.BCMenuScreens.register(event, BCBuildersGuis.fillerPlanner,
            buildcraft.builders.gui.GuiFillerPlanner::new);
    }
}
