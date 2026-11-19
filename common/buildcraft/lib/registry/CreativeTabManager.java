/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.registry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import buildcraft.lib.item.IItemBuildCraft;
import buildcraft.lib.registry.TagManager.EnumTagType;

/** BuildCraft's creative tabs. Modules create tabs by name ("buildcraft.main") from their constructors; items join the tab
 * named by their tag (or one of the vanilla tabs, "vanilla.misc" / "vanilla.materials") when they're registered. Tabs are
 * registered as "buildcraftlib:&lt;name with dots as underscores&gt;" and titled "itemGroup.&lt;name&gt;", as on 1.12.2. */
public class CreativeTabManager {
    private static final Map<String, CreativeTabBC> tabMap = new LinkedHashMap<>();
    private static final List<Item> vanillaMisc = new ArrayList<>();
    private static final List<Item> vanillaMaterials = new ArrayList<>();

    public static CreativeTabBC createTab(String name) {
        return tabMap.computeIfAbsent(name, CreativeTabBC::new);
    }

    public static void setItem(String name, Item item) {
        if (item != null) {
            setItemStack(name, () -> new ItemStack(item));
        }
    }

    public static void setItemStack(String name, Supplier<ItemStack> item) {
        CreativeTabBC tab = tabMap.get(name);
        if (tab != null) {
            tab.icon = item;
        }
    }

    /** Adds the item to the tab its tag names (items without a tab tag aren't shown). */
    public static void addToTab(Item item) {
        if (!(item instanceof IItemBuildCraft itemBC) || itemBC.id().isEmpty()) {
            return;
        }
        String id = itemBC.id();
        if (!TagManager.hasTag(id, EnumTagType.CREATIVE_TAB)) {
            return;
        }
        String name = TagManager.getTag(id, EnumTagType.CREATIVE_TAB);
        switch (name) {
            case "vanilla.misc" -> vanillaMisc.add(item);
            case "vanilla.materials" -> vanillaMaterials.add(item);
            default -> {
                CreativeTabBC tab = tabMap.get(name);
                if (tab == null) {
                    throw new IllegalArgumentException("Unknown tab " + name);
                }
                tab.items.add(item);
            }
        }
    }

    static void addStacks(Item item, CreativeModeTab.Output output) {
        NonNullList<ItemStack> stacks = NonNullList.create();
        if (item instanceof IItemBuildCraft itemBC) {
            itemBC.addSubItems(stacks);
        } else {
            stacks.add(new ItemStack(item));
        }
        for (ItemStack stack : stacks) {
            output.accept(stack);
        }
    }

    /** Registers the tabs. Must be on buildcraftlib's mod event bus. */
    public static void onRegister(RegisterEvent event) {
        if (!event.getRegistryKey().equals(Registries.CREATIVE_MODE_TAB)) {
            return;
        }
        for (CreativeTabBC tab : tabMap.values()) {
            event.register(Registries.CREATIVE_MODE_TAB, tab.id, () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup." + tab.name))
                .icon(() -> tab.icon.get())
                .displayItems((params, output) -> tab.items.forEach(item -> addStacks(item, output)))
                .build());
        }
    }

    /** Adds the items meant for vanilla's tabs. Must be on buildcraftlib's mod event bus. */
    public static void onBuildContents(BuildCreativeModeTabContentsEvent event) {
        ResourceKey<CreativeModeTab> key = event.getTabKey();
        if (key == CreativeModeTabs.INGREDIENTS) {
            vanillaMaterials.forEach(item -> addStacks(item, event));
        } else if (key == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            vanillaMisc.forEach(item -> addStacks(item, event));
        }
    }

    public static class CreativeTabBC {
        public final String name;
        public final Identifier id;
        final List<Item> items = new ArrayList<>();
        Supplier<ItemStack> icon = () -> new ItemStack(Items.COMPARATOR); // Temp.

        private CreativeTabBC(String name) {
            this.name = name;
            this.id = Identifier.fromNamespaceAndPath("buildcraftlib", name.replace('.', '_'));
        }

        public void setItem(Item item) {
            if (item != null) {
                icon = () -> new ItemStack(item);
            }
        }

        public void setItem(ItemStack stack) {
            if (stack == null || stack.isEmpty()) return;
            icon = () -> stack;
        }
    }
}
