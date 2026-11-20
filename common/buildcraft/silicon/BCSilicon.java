/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.silicon;

import java.util.function.Consumer;

import net.minecraft.world.item.ItemStack;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.fml.event.lifecycle.InterModProcessEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

import buildcraft.api.facades.FacadeAPI;

import buildcraft.lib.registry.CreativeTabManager;
import buildcraft.lib.registry.RegistryConfig;
import buildcraft.lib.registry.TagManager;
import buildcraft.lib.registry.TagManager.EnumTagType;
import buildcraft.lib.registry.TagManager.TagEntry;

import buildcraft.core.BCCore;
import buildcraft.silicon.plug.FacadeInstance;
import buildcraft.silicon.plug.FacadeStateManager;
import buildcraft.silicon.plug.FilterEventHandler;
import buildcraft.transport.tile.TilePipeHolder;

@Mod(BCSilicon.MODID)
public class BCSilicon {
    public static final String MODID = "buildcraftsilicon";

    public static BCSilicon INSTANCE = null;
    public static IEventBus MOD_BUS;

    public BCSilicon(IEventBus modBus, ModContainer container) {
        // Construct one module at a time (see BCModConstruction)
        synchronized (buildcraft.lib.BCModConstruction.LOCK) {
            registerTags();
            construct(modBus, container);
        }
    }

    private void construct(IEventBus modBus, ModContainer container) {
        INSTANCE = this;
        MOD_BUS = modBus;
        RegistryConfig.useOtherModConfigFor(MODID, BCCore.MODID);

        CreativeTabManager.createTab("buildcraft.plugs");
        CreativeTabManager.createTab("buildcraft.facades");
        CreativeTabManager.setItemStack("buildcraft.facades", () -> {
            if (BCSiliconItems.plugFacade == null || FacadeStateManager.previewState == null) {
                return new ItemStack(net.minecraft.world.item.Items.STONE);
            }
            FacadeInstance inst = FacadeInstance.createSingle(FacadeStateManager.previewState, false);
            return BCSiliconItems.plugFacade.createItemStack(inst);
        });

        FacadeAPI.registry = FacadeStateManager.INSTANCE;

        BCSiliconConfig.preInit();
        BCSiliconBlocks.preInit(modBus);
        BCSiliconPlugs.preInit();
        BCSiliconItems.preInit(modBus);
        BCSiliconStatements.preInit();

        net.neoforged.neoforge.registries.DeferredRegister<net.minecraft.world.item.crafting.RecipeSerializer<?>> serializers =
            buildcraft.lib.registry.RegistrationHelper.createRegister(
                net.minecraft.core.registries.Registries.RECIPE_SERIALIZER, MODID, modBus);
        serializers.register("facade_swap", () -> buildcraft.silicon.recipe.FacadeSwapRecipe.SERIALIZER);

        // The transport module's pipes look for silicon's filter handler (lens filters on item pipes)
        TilePipeHolder.ITEM_PIPE_EVENT_HANDLERS.add(FilterEventHandler.class);

        BCSiliconGuis.fmlPreInit(modBus);
        BCSiliconProxy.getProxy().fmlPreInit();

        NeoForge.EVENT_BUS.addListener((TagsUpdatedEvent event) -> {
            // The facade scan makes stacks, which 26.3 can only do once a world's data (components) has loaded
            FacadeStateManager.initOnce();
            BCSiliconRecipes.registerAssemblyRecipes();
        });

        modBus.addListener(BCSilicon::init);
        modBus.addListener(BCSilicon::postInit);
        modBus.addListener(BCSilicon::processImc);
    }

    private static void init(FMLCommonSetupEvent evt) {
        evt.enqueueWork(() -> {
            BCSiliconProxy.getProxy().fmlInit();
        });
    }

    private static void postInit(FMLLoadCompleteEvent evt) {
        evt.enqueueWork(() -> BCSiliconProxy.getProxy().fmlPostInit());
    }

    private static void processImc(InterModProcessEvent evt) {
        evt.getIMCStream().forEach(FacadeStateManager::receiveInterModComms);
    }

    private static void registerTags() {
        startBatch();
        // Items
        for (String chipset : new String[] { "red", "iron", "gold", "quartz", "diamond" }) {
            // 1.12.2: one "redstone_chipset" item, the type in its damage
            registerTag("item.redstone_chipset." + chipset).reg("redstone_chipset_" + chipset)
                .locale("redstone_" + chipset + "_chipset").model("redstone_chipset/" + chipset);
        }
        registerTag("item.gate_copier").reg("gate_copier").locale("gateCopier")
            .model("gatecopier_");
        registerTag("item.plug.gate").reg("plug_gate").locale("gate").model("pluggable/gate").tab("buildcraft.plugs")
            .oldReg("plug_gate");
        registerTag("item.plug.lens").reg("plug_lens").locale("lens").model("pluggable/lens").tab("buildcraft.plugs")
            .oldReg("plug_lens");
        registerTag("item.plug.pulsar").reg("plug_pulsar").locale("pulsar").model("plug_pulsar").tab("buildcraft.plugs")
            .oldReg("plug_pulsar");
        registerTag("item.plug.light_sensor").reg("plug_light_sensor").locale("light_sensor").model("plug_light_sensor")
            .tab("buildcraft.plugs").oldReg("plug_light_sensor");
        registerTag("item.plug.timer").reg("plug_timer").locale("timer").model("plug_timer")
            .tab("buildcraft.plugs").oldReg("plug_timer");
        registerTag("item.plug.facade").reg("plug_facade").locale("Facade").model("plug_facade")
            .tab("buildcraft.facades").oldReg("plug_facade");
        // Item Blocks
        registerTag("item.block.laser").reg("laser").locale("laserBlock").model("laser");
        registerTag("item.block.assembly_table").reg("assembly_table").locale("assemblyTableBlock")
            .model("assembly_table");
        registerTag("item.block.advanced_crafting_table").reg("advanced_crafting_table")
            .locale("assemblyWorkbenchBlock").model("advanced_crafting_table");
        registerTag("item.block.integration_table").reg("integration_table").locale("integrationTableBlock")
            .model("integration_table");
        registerTag("item.block.charging_table").reg("charging_table").locale("chargingTableBlock")
            .model("charging_table");
        registerTag("item.block.programming_table").reg("programming_table").locale("programmingTableBlock")
            .model("programming_table");
        // Blocks
        registerTag("block.laser").reg("laser").oldReg("laserBlock").locale("laserBlock").model("laser");
        registerTag("block.assembly_table").reg("assembly_table").oldReg("assemblyTableBlock")
            .locale("assemblyTableBlock").model("assembly_table");
        registerTag("block.advanced_crafting_table").reg("advanced_crafting_table").oldReg("advancedCraftingTableBlock")
            .locale("assemblyWorkbenchBlock").model("advanced_crafting_table");
        registerTag("block.integration_table").reg("integration_table").oldReg("integrationTableBlock")
            .locale("integrationTableBlock").model("integration_table");
        registerTag("block.charging_table").reg("charging_table").oldReg("chargingTableBlock")
            .locale("chargingTableBlock").model("charging_table");
        registerTag("block.programming_table").reg("programming_table").oldReg("programmingTableBlock")
            .locale("programmingTableBlock").model("programming_table");
        // Tiles
        registerTag("tile.laser").reg("laser");
        registerTag("tile.assembly_table").reg("assembly_table");
        registerTag("tile.advanced_crafting_table").reg("advanced_crafting_table");
        registerTag("tile.integration_table").reg("integration_table");
        registerTag("tile.charging_table").reg("charging_table");
        registerTag("tile.programming_table").reg("programming_table");

        endBatch(TagManager.prependTags("buildcraftsilicon:", EnumTagType.REGISTRY_NAME, EnumTagType.MODEL_LOCATION)
            .andThen(TagManager.setTab("buildcraft.main")));
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
