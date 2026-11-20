/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.core;

import java.io.File;
import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredRegister;

import buildcraft.lib.BCLibItems;
import buildcraft.lib.marker.MarkerCache;
import buildcraft.lib.registry.CreativeTabManager;
import buildcraft.lib.registry.RegistrationHelper;
import buildcraft.lib.registry.TagManager;
import buildcraft.lib.registry.TagManager.EnumTagType;
import buildcraft.lib.registry.TagManager.TagEntry;

import buildcraft.core.gen.WaterSpringFeature;
import buildcraft.core.marker.PathCache;
import buildcraft.core.marker.VolumeCache;

/** BuildCraft Core. 1.12.2's pre-init happens in the constructor, init in common setup and post-init once loading
 * completes. */
@Mod(BCCore.MODID)
public class BCCore {
    public static final String MODID = "buildcraftcore";

    public static BCCore INSTANCE = null;
    public static IEventBus MOD_BUS;

    static {
        BCLibItems.enableGuide();
        BCLibItems.enableDebugger();
    }

    public BCCore(IEventBus modBus, ModContainer container) {
        // Construct one module at a time (see BCModConstruction)
        synchronized (buildcraft.lib.BCModConstruction.LOCK) {
            registerTags();
            construct(modBus, container);
        }
    }

    private void construct(IEventBus modBus, ModContainer container) {
        INSTANCE = this;
        MOD_BUS = modBus;

        File cfgFolder = new File(FMLPaths.CONFIGDIR.get().toFile(), "buildcraft");
        BCCoreConfig.preInit(cfgFolder);

        CreativeTabManager.createTab("buildcraft.main");
        CreativeTabManager.setItemStack("buildcraft.main",
            () -> new net.minecraft.world.item.ItemStack(BCCoreItems.wrench));
        CreativeTabManager.overrideTab("item.guide", "buildcraft.main");
        CreativeTabManager.overrideTab("item.guide.note", "buildcraft.main");
        CreativeTabManager.overrideTab("item.debugger", "buildcraft.main");

        BCCoreBlocks.preInit(modBus);
        BCCoreItems.preInit(modBus);
        BCCoreStatements.preInit();

        DeferredRegister<com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.levelgen.feature.Feature>> features =
            RegistrationHelper.createRegister(Registries.FEATURE_TYPE, MODID, modBus);
        features.register("water_spring", () -> WaterSpringFeature.CODEC);

        DeferredRegister<com.mojang.serialization.MapCodec<? extends net.neoforged.neoforge.common.conditions.ICondition>> conditions =
            RegistrationHelper.createRegister(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.CONDITION_CODECS,
                MODID, modBus);
        conditions.register("engine_enabled", () -> buildcraft.core.recipe.EngineEnabledCondition.CODEC);

        BCCoreGuis.fmlPreInit(modBus);
        BCCoreProxy.getProxy().fmlPreInit();

        NeoForge.EVENT_BUS.register(BCCoreEventDist.INSTANCE);
        BCCoreConfig.saveConfigs();

        modBus.addListener(BCCore::init);
        modBus.addListener(BCCore::postInit);
    }

    private static void init(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            BCCoreConfig.saveConfigs();
            BCCoreProxy.getProxy().fmlInit();

            MarkerCache.registerCache(VolumeCache.INSTANCE);
            MarkerCache.registerCache(PathCache.INSTANCE);
        });
    }

    private static void postInit(FMLLoadCompleteEvent event) {
        event.enqueueWork(() -> {
            BCCoreConfig.saveConfigs();
            BCCoreProxy.getProxy().fmlPostInit();
            BCCoreConfig.postInit();
        });
    }

    private static void registerTags() {
        startBatch();
        // Items
        registerTag("item.wrench").reg("wrench").locale("wrenchItem").oldReg("wrenchItem").model("wrench");
        registerTag("item.diamond.shard").reg("diamond_shard").locale("diamondShard").model("diamond_shard").tab("vanilla.materials");
        registerTag("item.gear.wood").reg("gear_wood").locale("woodenGearItem").oreDict("gearWood").oldReg("woodenGearItem").model("gears/wood");
        registerTag("item.gear.stone").reg("gear_stone").locale("stoneGearItem").oreDict("gearStone").oldReg("stoneGearItem").model("gears/stone");
        registerTag("item.gear.iron").reg("gear_iron").locale("ironGearItem").oreDict("gearIron").oldReg("ironGearItem").model("gears/iron");
        registerTag("item.gear.gold").reg("gear_gold").locale("goldGearItem").oreDict("gearGold").oldReg("goldGearItem").model("gears/gold");
        registerTag("item.gear.diamond").reg("gear_diamond").locale("diamondGearItem").oreDict("gearDiamond").oldReg("diamondGearItem").model("gears/diamond");
        registerTag("item.list").reg("list").locale("list").oldReg("listItem").model("list_");
        registerTag("item.map_location").reg("map_location").locale("mapLocation").oldReg("mapLocation").model("map_location/");
        registerTag("item.paintbrush").reg("paintbrush").locale("paintbrush").model("paintbrush/");
        registerTag("item.marker_connector").reg("marker_connector").locale("markerConnector").model("marker_connector");
        registerTag("item.volume_box").reg("volume_box").locale("volume_box").model("volume_box");
        registerTag("item.goggles").reg("goggles").locale("goggles").model("goggles");
        registerTag("item.fragile_fluid_shard").reg("fragile_fluid_shard").locale("fragile_fluid_shard").model("fragile_fluid_shard");
        // Item Blocks
        registerTag("item.block.marker.volume").reg("marker_volume").locale("markerBlock").oldReg("markerBlock").model("marker_volume");
        registerTag("item.block.marker.path").reg("marker_path").locale("pathMarkerBlock").oldReg("pathMarkerBlock").model("marker_path");
        registerTag("item.block.spring").reg("spring").locale("spring").model("spring");
        registerTag("item.block.power_tester").reg("power_tester").locale("power_tester").model("power_tester");
        registerTag("item.block.decorated").reg("decorated").locale("decorated").model("decorated/");
        TagEntry engine = registerTag("item.block.engine.bc").reg("engine").locale("engineBlock");
        // Blocks
        registerTag("block.spring").reg("spring").locale("spring");
        registerTag("block.decorated").reg("decorated").locale("decorated");
        registerTag("block.engine.bc").reg("engine").locale("engineBlock").oldReg("engineBlock");
        registerTag("block.engine.bc.wood").locale("engineWood");
        registerTag("block.engine.bc.stone").locale("engineStone");
        registerTag("block.engine.bc.iron").locale("engineIron");
        registerTag("block.engine.bc.creative").locale("engineCreative");
        registerTag("block.engine.bc.rf").locale("engineRf");
        registerTag("block.marker.volume").reg("marker_volume").locale("markerBlock").oldReg("markerBlock").model("marker_volume");
        registerTag("block.marker.path").reg("marker_path").locale("pathMarkerBlock").oldReg("pathMarkerBlock").model("marker_path");
        registerTag("block.power_tester").reg("power_tester").locale("power_tester").oldReg("power_tester").model("power_tester");
        // Tiles
        registerTag("tile.marker.volume").reg("marker.volume").oldReg("buildcraft.builders.Marker", "Marker");
        registerTag("tile.marker.path").reg("marker.path");
        registerTag("tile.engine.wood").reg("engine.wood");
        registerTag("tile.engine.creative").reg("engine.creative");
        registerTag("tile.power_tester").reg("power_tester");

        endBatch(TagManager.prependTags("buildcraftcore:", EnumTagType.REGISTRY_NAME, EnumTagType.MODEL_LOCATION).andThen(TagManager.setTab("buildcraft.main")));
        engine.model("");// Clear model so that subtypes can set it properly
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
