/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.energy.client;

import buildcraft.energy.BCEnergy;
import buildcraft.energy.BCEnergyGuis;
import buildcraft.energy.BCEnergyModels;
import buildcraft.energy.BCEnergyProxy;

/** Energy's client proxy, created by name from {@link BCEnergyProxy} so the server never loads it.
 * TODO(26.3 GUIs batch): the engine and dynamo GUIs. */
public class BCEnergyClientProxy extends BCEnergyProxy {
    @Override
    public void fmlPreInit() {
        super.fmlPreInit();
        BCEnergyModels.fmlPreInit();
        BCEnergy.MOD_BUS.addListener(BCEnergyClientProxy::registerScreens);
    }

    private static void registerScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        buildcraft.lib.client.BCMenuScreens.register(event, BCEnergyGuis.engineStone,
            buildcraft.energy.client.gui.GuiEngineStone_BC8::new);
        buildcraft.lib.client.BCMenuScreens.register(event, BCEnergyGuis.engineIron,
            buildcraft.energy.client.gui.GuiEngineIron_BC8::new);
        buildcraft.lib.client.BCMenuScreens.register(event, BCEnergyGuis.engineRf,
            buildcraft.energy.client.gui.GuiEngineRF::new);
        buildcraft.lib.client.BCMenuScreens.register(event, BCEnergyGuis.dynamoMj,
            buildcraft.energy.client.gui.GuiDynamoMJ::new);
    }
}
