/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.registry;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** Records the names blocks and items had in older BuildCraft versions. 1.12.2 remapped them when a world loaded; on 26.3
 * old worlds go through a separate converter, which can read these maps ({@link #getItemMigrations()},
 * {@link #getBlockMigrations()}) instead. */
public enum MigrationManager {
    INSTANCE;

    private final Map<String, Item> itemMigrations = new HashMap<>();
    private final Map<String, Block> blockMigrations = new HashMap<>();

    public void addItemMigration(Item to, String... oldNames) {
        // If we mistakenly try to migrate null then it must have been disabled.
        if (to == null) {
            return;
        }
        for (String old : oldNames) {
            itemMigrations.put(old.toLowerCase(Locale.ROOT), to);
        }
    }

    public void addBlockMigration(Block to, String... oldNames) {
        if (to == null) {
            return;
        }
        for (String old : oldNames) {
            blockMigrations.put(old.toLowerCase(Locale.ROOT), to);
        }
    }

    /** @return Old (lower case) registry paths to the items they became. */
    public Map<String, Item> getItemMigrations() {
        return Collections.unmodifiableMap(itemMigrations);
    }

    /** @return Old (lower case) registry paths to the blocks they became. */
    public Map<String, Block> getBlockMigrations() {
        return Collections.unmodifiableMap(blockMigrations);
    }
}
