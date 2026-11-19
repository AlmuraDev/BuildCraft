/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.registry;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.resources.Identifier;

import buildcraft.api.core.BCDebugging;

import buildcraft.lib.config.Configuration;
import buildcraft.lib.config.Property;

/** The per-module "objects" config, in which every block, item and pipe can be disabled (BuildCraft's objects.cfg).
 * Modules share core's config, as on 1.12.2. */
public class RegistryConfig {
    public static final boolean DEBUG = BCDebugging.shouldDebugLog("lib.registry");
    private static final Map<String, Configuration> modObjectConfigs = new HashMap<>();
    private static final Map<String, Set<String>> disabled = new HashMap<>();

    // #######################
    //
    // Setup
    //
    // #######################

    public static Configuration setRegistryConfig(String modid, File file) {
        return setRegistryConfig(modid, new Configuration(file));
    }

    public static Configuration setRegistryConfig(String modid, Configuration config) {
        modObjectConfigs.put(modid, config);
        return config;
    }

    /** Makes the mod "from" use the same object config as the mod "to". */
    public static Configuration useOtherModConfigFor(String from, String to) {
        Configuration config = modObjectConfigs.get(to);
        if (config == null) {
            throw new IllegalStateException("Didn't find a config for " + to);
        }
        modObjectConfigs.put(from, config);
        return config;
    }

    // #######################
    //
    // Checking
    //
    // #######################

    /** @return true if the object hasn't been disabled in the given mod's object config. (Adds it to the config, enabled, if
     *         it isn't there yet.) */
    public static boolean isEnabled(String modId, String category, String resourcePath, String langKey) {
        Configuration config = modObjectConfigs.get(modId);
        if (config == null) {
            throw new RuntimeException("No object config exists for the mod " + modId);
        }
        Property prop = config.get(category, resourcePath, true);
        prop.setLanguageKey(langKey);
        prop.setRequiresMcRestart(true);
        prop.setRequiresWorldRestart(true);
        boolean isEnabled = prop.getBoolean(true);
        if (!isEnabled) {
            setDisabled(category, resourcePath);
        }
        return isEnabled;
    }

    public static boolean hasItemBeenDisabled(Identifier loc) {
        return hasObjectBeenDisabled("items", loc) || hasObjectBeenDisabled("pipes", loc);
    }

    public static boolean hasBlockBeenDisabled(Identifier loc) {
        return hasObjectBeenDisabled("blocks", loc);
    }

    /** @return True if the given location has been passed to {@link #isEnabled(String, String, String, String)}, and it
     *         returned false (because it has been disabled in the appropriate mod's config) */
    public static boolean hasObjectBeenDisabled(String category, Identifier loc) {
        Set<String> locations = disabled.get(category);
        return locations != null && locations.contains(loc.getPath());
    }

    static void setDisabled(String category, String resourcePath) {
        disabled.computeIfAbsent(category, k -> new HashSet<>()).add(resourcePath);
    }
}
