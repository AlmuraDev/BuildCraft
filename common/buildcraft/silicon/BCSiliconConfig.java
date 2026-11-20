/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon;

import buildcraft.lib.config.Configuration;
import buildcraft.lib.config.EnumRestartRequirement;
import buildcraft.lib.config.Property;

import buildcraft.core.BCCoreConfig;

public class BCSiliconConfig {

    public static boolean renderLaserBeams = true;

    private static Property propRenderLaserBeams;

    public static void preInit() {
        Configuration config = BCCoreConfig.config;
        propRenderLaserBeams = config.get("display", "renderLaserBeams", true,
            "When false laser beams will not be visible while transmitting power without wearing Goggles");

        reloadConfig(EnumRestartRequirement.NONE);
        BCCoreConfig.addReloadListener(BCSiliconConfig::reloadConfig);
    }

    public static void reloadConfig(EnumRestartRequirement restarted) {
        renderLaserBeams = propRenderLaserBeams.getBoolean();
    }
}
