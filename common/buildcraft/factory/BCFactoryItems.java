/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.factory;

import net.neoforged.bus.api.IEventBus;

import buildcraft.lib.BCLib;
import buildcraft.lib.item.ItemBC_Neptune;
import buildcraft.lib.registry.RegistrationHelper;

import buildcraft.factory.item.ItemWaterGel;

public class BCFactoryItems {

    public static ItemBC_Neptune plasticSheet;
    public static ItemWaterGel waterGel;
    public static ItemBC_Neptune gelledWater;

    public static void fmlPreInit(IEventBus modBus) {
        RegistrationHelper helper = new RegistrationHelper(BCFactory.MODID, modBus);
        if (BCLib.DEV) {
            helper.addItem("item.plastic.sheet", ItemBC_Neptune::new, i -> plasticSheet = i);
        }
        helper.addItem("item.water_gel_spawn", ItemWaterGel::new, i -> waterGel = i);
        helper.addItem("item.gel", ItemBC_Neptune::new, i -> gelledWater = i);
    }
}
