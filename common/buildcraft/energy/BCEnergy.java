/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.energy;

import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import buildcraft.lib.registry.RegistrationHelper;
import buildcraft.lib.registry.RegistryConfig;
import buildcraft.lib.registry.TagManager;
import buildcraft.lib.registry.TagManager.EnumTagType;
import buildcraft.lib.registry.TagManager.TagEntry;

import buildcraft.core.BCCore;
import buildcraft.energy.generation.OilWellFeature;

@Mod(BCEnergy.MODID)
public class BCEnergy {
    public static final String MODID = "buildcraftenergy";

    public static BCEnergy INSTANCE;
    public static IEventBus MOD_BUS;

    public BCEnergy(IEventBus modBus, ModContainer container) {
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
        BCEnergyConfig.preInit();
        BCEnergyFluids.preInit(modBus);
        BCEnergyBlocks.preInit(modBus);
        BCEnergyItems.preInit(modBus);

        DeferredRegister<com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.levelgen.feature.Feature>> features =
            RegistrationHelper.createRegister(Registries.FEATURE_TYPE, MODID, modBus);
        features.register("oil_well", () -> OilWellFeature.CODEC);

        BCEnergyGuis.fmlPreInit(modBus);
        BCEnergyProxy.getProxy().fmlPreInit();

        NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) -> BCEnergyConfig.validateBiomeNames(event.getServer()));

        modBus.addListener(BCEnergy::init);
        // Fuels and coolants hold stacks, which 26.3 can only make once item components are bound (world data loaded)
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.DefaultDataComponentsBoundEvent event) -> BCEnergyRecipes.initOnce());
        modBus.addListener(BCEnergy::postInit);
    }

    private static void init(FMLCommonSetupEvent evt) {
        evt.enqueueWork(() -> {
            BCEnergyBlocks.init();
            BCEnergyProxy.getProxy().fmlInit();
        });
    }

    private static void postInit(FMLLoadCompleteEvent evt) {
        evt.enqueueWork(() -> BCEnergyProxy.getProxy().fmlPostInit());
    }

    private static void registerTags() {
        startBatch();
        // Items
        registerTag("item.glob.oil").reg("glob_of_oil").oldReg("glob_oil").locale("globOil").model("glob_oil");

        // Item Blocks
        registerTag("item.block.mj_dynamo").reg("mj_dynamo").locale("mjDynamo").model("mj_dynamo");

        // Blocks
        registerTag("block.mj_dynamo").reg("mj_dynamo").locale("mjDynamo").model("mj_dynamo");

        // Tiles
        registerTag("tile.engine.stone").reg("engine.stone");
        registerTag("tile.engine.iron").reg("engine.iron");
        registerTag("tile.engine.rf").reg("engine.rf");
        registerTag("tile.spring.oil").reg("spring.oil");
        registerTag("tile.mj_dynamo").reg("mj_dynamo");

        endBatch(TagManager.prependTags("buildcraftenergy:", EnumTagType.REGISTRY_NAME, EnumTagType.MODEL_LOCATION)
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
