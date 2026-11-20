/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.client;

import buildcraft.silicon.BCSilicon;
import buildcraft.silicon.BCSiliconGuis;
import buildcraft.silicon.BCSiliconModels;
import buildcraft.silicon.BCSiliconProxy;

/** Silicon's client proxy, created by name from {@link BCSiliconProxy} so the server never loads it.
 * TODO(26.3 GUIs batch): the laser table and gate GUIs. */
public class BCSiliconClientProxy extends BCSiliconProxy {
    @Override
    public void fmlPreInit() {
        super.fmlPreInit();
        BCSiliconModels.fmlPreInit();
        BCSilicon.MOD_BUS.addListener(BCSiliconClientProxy::registerScreens);
    }

    @Override
    public void fmlInit() {
        super.fmlInit();
        BCSiliconModels.fmlInit();
    }

    private static void registerScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        buildcraft.lib.client.BCMenuScreens.register(event, BCSiliconGuis.assemblyTable,
            buildcraft.silicon.gui.GuiAssemblyTable::new);
        buildcraft.lib.client.BCMenuScreens.register(event, BCSiliconGuis.advancedCraftingTable,
            buildcraft.silicon.gui.GuiAdvancedCraftingTable::new);
        buildcraft.lib.client.BCMenuScreens.register(event, BCSiliconGuis.integrationTable,
            buildcraft.silicon.gui.GuiIntegrationTable::new);
        buildcraft.lib.client.BCMenuScreens.register(event, BCSiliconGuis.gate, buildcraft.silicon.gui.GuiGate::new);
    }
}
