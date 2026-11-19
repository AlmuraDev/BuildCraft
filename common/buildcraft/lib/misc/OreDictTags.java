/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.misc;

import java.util.Locale;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Turns 1.12.2 ore dictionary names into the 26.3 item tags that replaced them: "gearIron" -> "c:gears/iron",
 * "ingotCopper" -> "c:ingots/copper", "blockGold" -> "c:storage_blocks/gold". Names that are already tag ids
 * ("c:ingots/iron", "#minecraft:planks") are used as they are. */
public final class OreDictTags {
    private OreDictTags() {}

    /** 1.12.2 prefixes -> common tag folders. */
    private static final Map<String, String> PREFIXES = Map.ofEntries(
        Map.entry("gear", "gears"),
        Map.entry("ingot", "ingots"),
        Map.entry("nugget", "nuggets"),
        Map.entry("dust", "dusts"),
        Map.entry("plate", "plates"),
        Map.entry("gem", "gems"),
        Map.entry("ore", "ores"),
        Map.entry("block", "storage_blocks"),
        Map.entry("rod", "rods"),
        Map.entry("crop", "crops"),
        Map.entry("seed", "seeds"),
        Map.entry("dye", "dyes"),
        Map.entry("glass", "glass_blocks"),
        Map.entry("pane", "glass_panes"));

    /** Whole 1.12.2 names that don't follow the prefix pattern. */
    private static final Map<String, String> SPECIAL = Map.ofEntries(
        Map.entry("stickWood", "c:rods/wooden"),
        Map.entry("plankWood", "minecraft:planks"),
        Map.entry("logWood", "minecraft:logs"),
        Map.entry("slabWood", "minecraft:wooden_slabs"),
        Map.entry("stairWood", "minecraft:wooden_stairs"),
        Map.entry("treeSapling", "minecraft:saplings"),
        Map.entry("treeLeaves", "minecraft:leaves"),
        Map.entry("chest", "c:chests"),
        Map.entry("chestWood", "c:chests/wooden"),
        Map.entry("cobblestone", "c:cobblestones"),
        Map.entry("stone", "c:stones"),
        Map.entry("sand", "c:sands"),
        Map.entry("gravel", "c:gravels"),
        Map.entry("obsidian", "c:obsidians"),
        Map.entry("netherrack", "c:netherracks"),
        Map.entry("wool", "minecraft:wool"),
        Map.entry("string", "c:strings"),
        Map.entry("leather", "c:leathers"),
        Map.entry("feather", "c:feathers"),
        Map.entry("gunpowder", "c:gunpowders"),
        Map.entry("enderpearl", "c:ender_pearls"),
        Map.entry("slimeball", "c:slime_balls"),
        Map.entry("paper", "minecraft:paper"),
        Map.entry("blockGlass", "c:glass_blocks"),
        Map.entry("blockGlassColorless", "c:glass_blocks/colorless"),
        Map.entry("paneGlassColorless", "c:glass_panes/colorless"),
        Map.entry("paneGlass", "c:glass_panes"));

    /** @return The tag for the given ore dictionary name or tag id, or null if it doesn't look like either. */
    @Nullable
    public static TagKey<Item> toTag(String name) {
        String id = toTagId(name);
        if (id == null) {
            return null;
        }
        Identifier loc = Identifier.tryParse(id);
        return loc == null ? null : TagKey.create(Registries.ITEM, loc);
    }

    @Nullable
    public static String toTagId(String name) {
        if (name.startsWith("#")) {
            name = name.substring(1);
        }
        if (name.contains(":")) {
            return name;
        }
        String special = SPECIAL.get(name);
        if (special != null) {
            return special;
        }
        for (Map.Entry<String, String> entry : PREFIXES.entrySet()) {
            String prefix = entry.getKey();
            if (name.length() > prefix.length() && name.startsWith(prefix)
                && Character.isUpperCase(name.charAt(prefix.length()))) {
                String material = toSnakeCase(name.substring(prefix.length()));
                return "c:" + entry.getValue() + "/" + material;
            }
        }
        return null;
    }

    /** "NetherQuartz" -> "nether_quartz" */
    private static String toSnakeCase(String camel) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < camel.length(); i++) {
            char c = camel.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                sb.append('_');
            }
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString().toLowerCase(Locale.ROOT);
    }

    /** @return true if the stack has the tag that the given ore dictionary name or tag id became. */
    public static boolean matches(ItemStack stack, String name) {
        TagKey<Item> tag = toTag(name);
        return tag != null && stack.is(tag);
    }
}
