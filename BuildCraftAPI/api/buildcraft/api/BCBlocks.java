package buildcraft.api;

import net.minecraft.world.level.block.Block;

import net.minecraft.resources.Identifier;

import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

public class BCBlocks {

    public static class Core {
        public static final DeferredBlock<Block> SPRING = DeferredBlock.createBlock(id("buildcraftcore", "spring"));
        public static final DeferredBlock<Block> DECORATED = DeferredBlock.createBlock(id("buildcraftcore", "decorated"));
        public static final DeferredBlock<Block> ENGINE = DeferredBlock.createBlock(id("buildcraftcore", "engine"));
        public static final DeferredBlock<Block> MARKER_VOLUME = DeferredBlock.createBlock(id("buildcraftcore", "marker_volume"));
        public static final DeferredBlock<Block> MARKER_PATH = DeferredBlock.createBlock(id("buildcraftcore", "marker_path"));
    }

    public static class Builders {
        public static final DeferredBlock<Block> ARCHITECT = DeferredBlock.createBlock(id("buildcraftbuilders", "architect"));
        public static final DeferredBlock<Block> BUILDER = DeferredBlock.createBlock(id("buildcraftbuilders", "builder"));
        public static final DeferredBlock<Block> FILLER = DeferredBlock.createBlock(id("buildcraftbuilders", "filler"));
        public static final DeferredBlock<Block> LIBRARY = DeferredBlock.createBlock(id("buildcraftbuilders", "library"));
        public static final DeferredBlock<Block> REPLACER = DeferredBlock.createBlock(id("buildcraftbuilders", "replacer"));
        public static final DeferredBlock<Block> QUARRY = DeferredBlock.createBlock(id("buildcraftbuilders", "quarry"));
        public static final DeferredBlock<Block> FRAME = DeferredBlock.createBlock(id("buildcraftbuilders", "frame"));
    }

    public static class Energy {
        // Fluid blocks can be accessed ~somewhere else~
    }

    public static class Factory {
        public static final DeferredBlock<Block> AUTOWORKBENCH_ITEM = DeferredBlock.createBlock(id("buildcraftfactory", "autoworkbench_item"));
        public static final DeferredBlock<Block> MINING_WELL = DeferredBlock.createBlock(id("buildcraftfactory", "mining_well"));
        public static final DeferredBlock<Block> PUMP = DeferredBlock.createBlock(id("buildcraftfactory", "pump"));
        public static final DeferredBlock<Block> TUBE = DeferredBlock.createBlock(id("buildcraftfactory", "tube"));
        public static final DeferredBlock<Block> FLOOD_GATE = DeferredBlock.createBlock(id("buildcraftfactory", "flood_gate"));
        public static final DeferredBlock<Block> TANK = DeferredBlock.createBlock(id("buildcraftfactory", "tank"));
        public static final DeferredBlock<Block> CHUTE = DeferredBlock.createBlock(id("buildcraftfactory", "chute"));
        public static final DeferredBlock<Block> WATER_GEL = DeferredBlock.createBlock(id("buildcraftfactory", "water_gel"));
        public static final DeferredBlock<Block> DISTILLER = DeferredBlock.createBlock(id("buildcraftfactory", "distiller"));
        public static final DeferredBlock<Block> HEAT_EXCHANGE = DeferredBlock.createBlock(id("buildcraftfactory", "heat_exchange"));
    }

    public static class Transport {
        public static final DeferredBlock<Block> FILTERED_BUFFER = DeferredBlock.createBlock(id("buildcrafttransport", "filtered_buffer"));
        public static final DeferredBlock<Block> PIPE_HOLDER = DeferredBlock.createBlock(id("buildcrafttransport", "pipe_holder"));
    }

    public static class Silicon {
        public static final DeferredBlock<Block> LASER = DeferredBlock.createBlock(id("buildcraftsilicon", "laser"));
        public static final DeferredBlock<Block> ASSEMBLY_TABLE = DeferredBlock.createBlock(id("buildcraftsilicon", "assembly_table"));
        public static final DeferredBlock<Block> ADVANCED_CRAFTING_TABLE = DeferredBlock.createBlock(id("buildcraftsilicon", "advanced_crafting_table"));
        public static final DeferredBlock<Block> INTEGRATION_TABLE = DeferredBlock.createBlock(id("buildcraftsilicon", "integration_table"));
        public static final DeferredBlock<Block> CHARGING_TABLE = DeferredBlock.createBlock(id("buildcraftsilicon", "charging_table"));
        public static final DeferredBlock<Block> PROGRAMMING_TABLE = DeferredBlock.createBlock(id("buildcraftsilicon", "programming_table"));
    }

    public static class Robotics {

    }

    private static Identifier id(String namespace, String path) {
        return Identifier.fromNamespaceAndPath(namespace, path);
    }
}
