package buildcraft.api;

import net.minecraft.world.item.Item;

import net.minecraft.resources.Identifier;

import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import buildcraft.api.items.FluidItemDrops;

public class BCItems {

    public static class Lib {
        public static final DeferredItem<Item> GUIDE = DeferredItem.createItem(id("buildcraftlib", "guide"));
        public static final DeferredItem<Item> GUIDE_NOTE = DeferredItem.createItem(id("buildcraftlib", "guide_note"));
        public static final DeferredItem<Item> DEBUGGER = DeferredItem.createItem(id("buildcraftlib", "debugger"));
    }

    public static class Core {
        public static final DeferredItem<Item> GEAR_WOOD = DeferredItem.createItem(id("buildcraftcore", "gear_wood"));
        public static final DeferredItem<Item> GEAR_STONE = DeferredItem.createItem(id("buildcraftcore", "gear_stone"));
        public static final DeferredItem<Item> GEAR_IRON = DeferredItem.createItem(id("buildcraftcore", "gear_iron"));
        public static final DeferredItem<Item> GEAR_GOLD = DeferredItem.createItem(id("buildcraftcore", "gear_gold"));
        public static final DeferredItem<Item> GEAR_DIAMOND = DeferredItem.createItem(id("buildcraftcore", "gear_diamond"));
        public static final DeferredItem<Item> WRENCH = DeferredItem.createItem(id("buildcraftcore", "wrench"));
        public static final DeferredItem<Item> PAINTBRUSH = DeferredItem.createItem(id("buildcraftcore", "paintbrush"));
        public static final DeferredItem<Item> LIST = DeferredItem.createItem(id("buildcraftcore", "list"));
        public static final DeferredItem<Item> MAP_LOCATION = DeferredItem.createItem(id("buildcraftcore", "map_location"));
        public static final DeferredItem<Item> MARKER_CONNECTOR = DeferredItem.createItem(id("buildcraftcore", "marker_connector"));
        public static final DeferredItem<Item> VOLUME_BOX = DeferredItem.createItem(id("buildcraftcore", "volume_box"));
        public static final DeferredItem<Item> GOGGLES = DeferredItem.createItem(id("buildcraftcore", "goggles"));

        /** It is recommended that you refer to {@link FluidItemDrops#item} when creating fluid drops rather than
         * this. */
        public static final DeferredItem<Item> FRAGILE_FLUID_SHARD = DeferredItem.createItem(id("buildcraftcore", "fragile_fluid_shard"));
    }

    public static class Builders {

    }

    public static class Energy {
        public static final DeferredItem<Item> GLOB_OF_OIL = DeferredItem.createItem(id("buildcraftenergy", "glob_of_oil"));
    }

    public static class Factory {
        public static final DeferredItem<Item> PLASTIC_SHEET = DeferredItem.createItem(id("buildcraftfactory", "plastic_sheet"));
        public static final DeferredItem<Item> WATER_GEL = DeferredItem.createItem(id("buildcraftfactory", "water_gel"));
        public static final DeferredItem<Item> GELLED_WATER = DeferredItem.createItem(id("buildcraftfactory", "gelled_water"));
    }

    public static class Transport {
        public static final DeferredItem<Item> PLUG_BLOCKER = DeferredItem.createItem(id("buildcrafttransport", "plug_blocker"));
        public static final DeferredItem<Item> PLUG_POWER_ADAPTOR = DeferredItem.createItem(id("buildcrafttransport", "plug_power_adaptor"));

        public static final DeferredItem<Item> PIPE_STRUCTURE = DeferredItem.createItem(id("buildcrafttransport", "pipe_structure"));
        public static final DeferredItem<Item> PIPE_WOOD_ITEM = DeferredItem.createItem(id("buildcrafttransport", "pipe_wood_item"));
        public static final DeferredItem<Item> PIPE_EMZULI_ITEM = DeferredItem.createItem(id("buildcrafttransport", "pipe_emzuli_item"));
        public static final DeferredItem<Item> PIPE_DIAMOND_WOOD_ITEM = DeferredItem.createItem(id("buildcrafttransport", "pipe_diamond_wood_item"));
        public static final DeferredItem<Item> PIPE_WOOD_FLUID = DeferredItem.createItem(id("buildcrafttransport", "pipe_wood_fluid"));
        public static final DeferredItem<Item> PIPE_DIAMOND_WOOD_FLUID = DeferredItem.createItem(id("buildcrafttransport", "pipe_diamond_wood_fluid"));
    }

    public static class Silicon {
        public static final DeferredItem<Item> REDSTONE_CHIPSET = DeferredItem.createItem(id("buildcraftsilicon", "redstone_chipset"));

        public static final DeferredItem<Item> PLUG_PULSAR = DeferredItem.createItem(id("buildcraftsilicon", "plug_pulsar"));
    }

    public static class Robotics {

    }

    private static Identifier id(String namespace, String path) {
        return Identifier.fromNamespaceAndPath(namespace, path);
    }
}
