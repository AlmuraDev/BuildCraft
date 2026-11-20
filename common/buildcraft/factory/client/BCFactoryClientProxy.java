/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.client;

import buildcraft.factory.BCFactoryModels;
import buildcraft.factory.BCFactoryProxy;

/** Factory's client proxy, created by name from {@link BCFactoryProxy} so the server never loads it.
 * TODO(26.3 GUIs batch): the auto workbench, chute, tank and distiller GUIs. */
public class BCFactoryClientProxy extends BCFactoryProxy {
    @Override
    public void fmlPreInit() {
        super.fmlPreInit();
        BCFactoryModels.fmlPreInit();
    }
}
