/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.factory;

import java.util.function.Consumer;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;

import buildcraft.lib.registry.RegistryConfig;
import buildcraft.lib.registry.TagManager;
import buildcraft.lib.registry.TagManager.EnumTagType;
import buildcraft.lib.registry.TagManager.TagEntry;

import buildcraft.core.BCCore;

@Mod(BCFactory.MODID)
public class BCFactory {
    public static final String MODID = "buildcraftfactory";

    public static BCFactory INSTANCE = null;
    public static IEventBus MOD_BUS;

    public BCFactory(IEventBus modBus, ModContainer container) {
        INSTANCE = this;
        MOD_BUS = modBus;
        RegistryConfig.useOtherModConfigFor(MODID, BCCore.MODID);
        BCFactoryBlocks.fmlPreInit(modBus);
        BCFactoryItems.fmlPreInit(modBus);
        BCFactoryGuis.fmlPreInit(modBus);
        BCFactoryProxy.getProxy().fmlPreInit();

        modBus.addListener(BCFactory::init);
        modBus.addListener(BCFactory::postInit);
    }

    private static void init(FMLCommonSetupEvent evt) {
        evt.enqueueWork(() -> BCFactoryProxy.getProxy().fmlInit());
    }

    private static void postInit(FMLLoadCompleteEvent evt) {
        evt.enqueueWork(() -> BCFactoryProxy.getProxy().fmlPostInit());
    }

    static {
        startBatch();// factory
        // BC Factory Items
        registerTag("item.plastic.sheet").reg("plastic_sheet").locale("plasticSheet").oldReg("plasticSheet").model("plastic_sheet");
        registerTag("item.water_gel_spawn").reg("water_gel_spawn").locale("waterGel").model("water_gel");
        registerTag("item.gel").reg("gel").locale("gel").model("gel");
        // BC Factory Item Blocks
        registerTag("item.block.plastic").reg("plastic_block").locale("plasticBlock").model("plastic_block/");
        registerTag("item.block.autoworkbench.item").reg("autoworkbench_item").locale("autoWorkbenchBlock").model("autoworkbench_item");
        registerTag("item.block.mining_well").reg("mining_well").locale("miningWellBlock").model("mining_well");
        registerTag("item.block.pump").reg("pump").locale("pumpBlock").model("pump");
        registerTag("item.block.flood_gate").reg("flood_gate").locale("floodGateBlock").model("flood_gate");
        registerTag("item.block.tank").reg("tank").locale("tankBlock").model("tank");
        registerTag("item.block.chute").reg("chute").locale("chuteBlock").model("chute");
        registerTag("item.block.distiller").reg("distiller").locale("distiller").model("distiller");
        TagEntry tag = registerTag("item.block.heat_exchange").reg("heat_exchange").locale("heat_exchange");
        tag.model("heat_exchange").oldReg("heat_exchange_start", "heat_exchange_middle", "heat_exchange_end");
        // BC Factory Blocks
        registerTag("block.autoworkbench.item").reg("autoworkbench_item").oldReg("autoWorkbenchBlock").locale("autoWorkbenchBlock").model("autoworkbench_item");
        registerTag("block.mining_well").reg("mining_well").oldReg("miningWellBlock").locale("miningWellBlock").model("mining_well");
        registerTag("block.pump").reg("pump").oldReg("pumpBlock").locale("pumpBlock").model("pump");
        registerTag("block.tube").reg("tube").oldReg("tubeBlock").locale("tubeBlock").model("tube");
        registerTag("block.flood_gate").reg("flood_gate").oldReg("floodGateBlock").locale("floodGateBlock").model("flood_gate");
        registerTag("block.tank").reg("tank").oldReg("tankBlock").locale("tankBlock").model("tank");
        registerTag("block.chute").reg("chute").oldReg("chuteBlock").locale("chuteBlock").model("chute");
        registerTag("block.water_gel").reg("water_gel").locale("waterGel").model("water_gel");
        registerTag("block.distiller").reg("distiller").locale("distiller").model("distiller");
        tag = registerTag("block.heat_exchange").reg("heat_exchange").locale("heat_exchange").model("heat_exchange");
        tag.oldReg("heat_exchange_start", "heat_exchange_middle", "heat_exchange_end");
        // BC Factory Tiles
        registerTag("tile.autoworkbench.item").reg("autoworkbench_item");
        registerTag("tile.mining_well").reg("mining_well");
        registerTag("tile.pump").reg("pump");
        registerTag("tile.flood_gate").reg("flood_gate");
        registerTag("tile.tank").reg("tank");
        registerTag("tile.chute").reg("chute");
        registerTag("tile.distiller").reg("distiller");
        registerTag("tile.heat_exchange").reg("heat_exchange").oldReg("heat_exchange.start", "heat_exchange.end");

        endBatch(TagManager.prependTags("buildcraftfactory:", EnumTagType.REGISTRY_NAME, EnumTagType.MODEL_LOCATION).andThen(TagManager.setTab("buildcraft.main")));
    }

    private static TagEntry registerTag(String id) {
        return TagManager.registerTag(id);
    }

    private static void startBatch() {
        TagManager.startBatch();
    }

    private static void endBatch(Consumer<TagEntry> consumer) {
        TagManager.endBatch(consumer);
    }
}
