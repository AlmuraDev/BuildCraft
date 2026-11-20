/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.builders;

import java.util.function.Consumer;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import buildcraft.lib.registry.RegistryConfig;
import buildcraft.lib.registry.TagManager;
import buildcraft.lib.registry.TagManager.EnumTagType;
import buildcraft.lib.registry.TagManager.TagEntry;

import buildcraft.core.BCCore;
import buildcraft.builders.snapshot.GlobalSavedDataSnapshots;
import buildcraft.builders.snapshot.RulesLoader;

@Mod(BCBuilders.MODID)
public class BCBuilders {
    public static final String MODID = "buildcraftbuilders";

    public static BCBuilders INSTANCE = null;
    public static IEventBus MOD_BUS;

    public BCBuilders(IEventBus modBus, ModContainer container) {
        INSTANCE = this;
        MOD_BUS = modBus;
        RegistryConfig.useOtherModConfigFor(MODID, BCCore.MODID);

        BCBuildersConfig.preInit();
        BCBuildersRegistries.preInit();
        BCBuildersItems.fmlPreInit(modBus);
        BCBuildersBlocks.fmlPreInit(modBus);
        BCBuildersStatements.preInit();
        BCBuildersSchematics.preInit();

        BCBuildersGuis.fmlPreInit(modBus);
        BCBuildersProxy.getProxy().fmlPreInit();

        NeoForge.EVENT_BUS.addListener((ServerStartingEvent event) -> GlobalSavedDataSnapshots.reInit(LogicalSide.SERVER));

        modBus.addListener(BCBuilders::init);
        modBus.addListener(BCBuilders::postInit);
    }

    private static void init(FMLCommonSetupEvent evt) {
        evt.enqueueWork(() -> {
            BCBuildersProxy.getProxy().fmlInit();
            BCBuildersRegistries.init();
            BCBuildersRecipes.init();
        });
    }

    private static void postInit(FMLLoadCompleteEvent evt) {
        evt.enqueueWork(() -> {
            BCBuildersProxy.getProxy().fmlPostInit();
            RulesLoader.loadAll();
        });
    }

    static {
        startBatch();
        // Items
        registerTag("item.schematic.single").reg("schematic_single").locale("schematicSingle").model("schematic_single/");
        registerTag("item.snapshot").reg("snapshot").locale("snapshot").model("snapshot/");
        registerTag("item.filler_planner").reg("filler_planner").oldReg("filling_planner").locale("buildcraft.filler_planner").model("filler_planner");
        // Item Blocks
        registerTag("item.block.architect").reg("architect").locale("architectBlock").model("architect");
        registerTag("item.block.builder").reg("builder").locale("builderBlock").model("builder");
        registerTag("item.block.filler").reg("filler").locale("fillerBlock").model("filler");
        registerTag("item.block.library").reg("library").locale("libraryBlock").model("library");
        registerTag("item.block.replacer").reg("replacer").locale("replacerBlock").model("replacer");
        registerTag("item.block.frame").reg("frame").locale("frameBlock").model("frame");
        registerTag("item.block.quarry").reg("quarry").locale("quarryBlock").model("quarry");
        // Blocks
        registerTag("block.architect").reg("architect").locale("architectBlock").model("architect");
        registerTag("block.builder").reg("builder").locale("builderBlock").model("builder");
        registerTag("block.filler").reg("filler").locale("fillerBlock").model("filler");
        registerTag("block.library").reg("library").locale("libraryBlock").model("library");
        registerTag("block.replacer").reg("replacer").locale("replacerBlock").model("replacer");
        registerTag("block.frame").reg("frame").locale("frameBlock").model("frame");
        registerTag("block.quarry").reg("quarry").locale("quarryBlock").model("quarry");
        // Tiles
        registerTag("tile.architect").reg("architect");
        registerTag("tile.builder").reg("builder");
        registerTag("tile.library").reg("library");
        registerTag("tile.replacer").reg("replacer");
        registerTag("tile.filler").reg("filler");
        registerTag("tile.quarry").reg("quarry");

        endBatch(TagManager.prependTags("buildcraftbuilders:", EnumTagType.REGISTRY_NAME, EnumTagType.MODEL_LOCATION).andThen(TagManager.setTab("buildcraft.main")));
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
