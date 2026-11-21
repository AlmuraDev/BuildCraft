/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.misc;

import java.util.Map;

import com.mojang.serialization.Dynamic;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.fixes.BlockStateData;

/** Upgrades block states that BuildCraft saved in Minecraft 1.12.2 to the form 26.3's NbtUtils.readBlockState reads.
 * <ul>
 * <li>26.3 keys a state as {id, properties}; 1.12.2 (and every version up to 1.21) used {Name, Properties}, which 26.3
 * reads as air.</li>
 * <li>1.12.2 names are pre-flattening (minecraft:stonebrick variant=stonebrick): they go through vanilla's static
 * 1.12 -> 1.13 table ({@link BlockStateData}) directly, never the data fixer, so this is safe on any thread.</li>
 * </ul> */
public final class LegacyBlockStates {
    private LegacyBlockStates() {}

    /** Blocks renamed after 1.13 (the flattening table gives 1.13 names). */
    private static final Map<String, String> RENAMED_AFTER_1_13 = Map.of(
        "minecraft:grass_path", "minecraft:dirt_path",
        "minecraft:grass", "minecraft:short_grass");

    /** @return The 26.3 form ({id, properties}) of a block state tag. Tags already in that form are returned as is. */
    public static CompoundTag upgrade(CompoundTag stateTag) {
        if (stateTag.contains("id") || !stateTag.contains("Name")) {
            return stateTag;
        }
        // Flatten first: the table is keyed by the old {Name, Properties} form
        Tag upgraded = BlockStateData.upgradeBlockStateTag(new Dynamic<>(NbtOps.INSTANCE, stateTag)).convert(NbtOps.INSTANCE)
            .getValue();
        CompoundTag flat = upgraded instanceof CompoundTag c ? c : stateTag;
        String name = flat.getStringOr("Name", "");
        if (name.equals(stateTag.getStringOr("Name", "")) && !known(name)) {
            // Not in the table as a whole state (e.g. some property set it doesn't list): try the 1.12 name alone
            String flattened = BlockStateData.upgradeBlock(name);
            if (!flattened.equals(name)) {
                flat = new CompoundTag();
                flat.putString("Name", flattened);
                name = flattened;
            }
        }
        String renamed = RENAMED_AFTER_1_13.get(name);
        if (renamed != null && !known(name)) {
            name = renamed;
        }

        CompoundTag out = new CompoundTag();
        out.putString("id", name);
        CompoundTag properties = flat.getCompoundOrEmpty("Properties");
        if (!properties.isEmpty()) {
            out.put("properties", properties.copy());
        }
        return out;
    }

    private static boolean known(String name) {
        Identifier id = Identifier.tryParse(name);
        return id != null && BuiltInRegistries.BLOCK.containsKey(id);
    }
}
