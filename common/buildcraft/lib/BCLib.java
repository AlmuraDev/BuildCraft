/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib;

import java.util.function.Consumer;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import buildcraft.api.BCModules;
import buildcraft.api.core.BCLog;

import buildcraft.lib.block.VanillaPaintHandlers;
import buildcraft.lib.block.VanillaRotationHandlers;
import buildcraft.lib.chunkload.ChunkLoaderManager;
import buildcraft.lib.command.CommandBuildCraft;
import buildcraft.lib.expression.ExpressionDebugManager;
import buildcraft.lib.list.VanillaListHandlers;
import buildcraft.lib.marker.MarkerCache;
import buildcraft.lib.misc.ExpressionCompat;
import buildcraft.lib.net.MessageManager;
import buildcraft.lib.net.cache.BuildCraftObjectCaches;
import buildcraft.lib.registry.CreativeTabManager;
import buildcraft.lib.registry.TagManager;
import buildcraft.lib.registry.TagManager.EnumTagType;
import buildcraft.lib.registry.TagManager.TagEntry;
import buildcraft.lib.registry.TileTypes;
import buildcraft.lib.script.ReloadableRegistryManager;

/** BuildCraft Lib. 1.12.2's pre-init happens in the constructor, init in common setup and post-init once loading
 * completes. Client-only setup lives in {@link buildcraft.lib.client.BCLibClient}. */
@Mod(BCLib.MODID)
public class BCLib {
    public static final String MODID = "buildcraftlib";
    /** This jar's version (from neoforge.mods.toml), set when the mod is constructed. */
    public static String VERSION = "unknown";
    public static final String MC_VERSION = "26.3";

    public static final boolean DEV = !FMLEnvironment.isProduction() || Boolean.getBoolean("buildcraft.dev");

    public static BCLib INSTANCE;
    public static ModContainer MOD_CONTAINER;
    public static IEventBus MOD_BUS;

    public BCLib(IEventBus modBus, ModContainer container) {
        INSTANCE = this;
        MOD_CONTAINER = container;
        MOD_BUS = modBus;
        VERSION = container.getModInfo().getVersion().toString();

        try {
            BCLog.logger.info("");
        } catch (NoSuchFieldError e) {
            throw throwBadClass(e, BCLog.class);
        }
        BCLog.logger.info("Starting BuildCraft " + BCLib.VERSION);
        BCLog.logger.info("Copyright (c) the BuildCraft team, 2011-2018");
        BCLog.logger.info("https://www.mod-buildcraft.com");
        BCLog.logger.info("");
        BCLog.logger.info("Loaded Modules:");
        for (BCModules module : BCModules.VALUES) {
            if (module.isLoaded()) {
                BCLog.logger.info("  - " + module.lowerCaseName);
            }
        }
        BCLog.logger.info("Missing Modules:");
        for (BCModules module : BCModules.VALUES) {
            if (!module.isLoaded()) {
                BCLog.logger.info("  - " + module.lowerCaseName);
            }
        }
        BCLog.logger.info("");

        ExpressionDebugManager.logger = BCLog.logger::info;
        ExpressionCompat.setup();
        BCLibRegistries.fmlPreInit();
        BCLibProxy.getProxy().fmlPreInit();
        BCLibItems.fmlPreInit(modBus);
        BuildCraftObjectCaches.fmlPreInit();

        NeoForge.EVENT_BUS.register(BCLibEventDist.class);
        NeoForge.EVENT_BUS.addListener(BCLib::registerCommands);

        modBus.addListener(MessageManager::onRegisterPayloads);
        modBus.addListener(TileTypes::onRegisterCapabilities);
        modBus.addListener(CreativeTabManager::onRegister);
        modBus.addListener(CreativeTabManager::onBuildContents);
        modBus.addListener(ChunkLoaderManager::onRegisterTicketControllers);
        modBus.addListener(BCLib::init);
        modBus.addListener(BCLib::postInit);
    }

    public static Error throwBadClass(Error e, Class<?> cls) throws Error {
        throw new Error(
            "Bad " + cls + " loaded from " + cls.getClassLoader() + " domain: " + cls.getProtectionDomain(), e
        );
    }

    private static void init(FMLCommonSetupEvent evt) {
        evt.enqueueWork(() -> {
            BCLibProxy.getProxy().fmlInit();
            BCLibRegistries.fmlInit();
            VanillaListHandlers.fmlInit();
            VanillaPaintHandlers.fmlInit();
            VanillaRotationHandlers.fmlInit();
        });
    }

    private static void postInit(FMLLoadCompleteEvent evt) {
        evt.enqueueWork(() -> {
            ReloadableRegistryManager.loadAll();
            BCLibProxy.getProxy().fmlPostInit();
            BuildCraftObjectCaches.fmlPostInit();
            VanillaListHandlers.fmlPostInit();
            MarkerCache.postInit();
        });
    }

    private static void registerCommands(RegisterCommandsEvent event) {
        CommandBuildCraft.register(event.getDispatcher());
    }

    static {
        startBatch();
        registerTag("item.guide").reg("guide").locale("buildcraft.guide").model("guide").tab("vanilla.misc");
        registerTag("item.guide.note").reg("guide_note").locale("buildcraft.guide_note").model("guide_note")
            .tab("vanilla.misc");
        registerTag("item.debugger").reg("debugger").locale("debugger").model("debugger").tab("vanilla.misc");
        endBatch(TagManager.prependTags("buildcraftlib:", EnumTagType.REGISTRY_NAME, EnumTagType.MODEL_LOCATION));
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
