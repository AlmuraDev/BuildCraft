/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * The BuildCraft API is distributed under the terms of the MIT License. Please check the contents of the license, which
 * should be located as "LICENSE.API" in the BuildCraft source code distribution. */
package buildcraft.api.core;

import java.util.HashMap;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import net.minecraft.world.level.block.Block;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import net.neoforged.fml.ModList;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;

public final class BuildCraftAPI {
    public static IFakePlayerProvider fakePlayerProvider;

    public static final Set<Block> softBlocks = Sets.newHashSet();
    public static final HashMap<String, IWorldProperty> worldProperties = Maps.newHashMap();

    /** Deactivate constructor */
    private BuildCraftAPI() {}

    public static String getVersion() {
        return ModList.get().getModContainerById("buildcraftlib")
            .map(container -> container.getModInfo().getVersion().toString())
            .orElse("UNKNOWN VERSION");
    }

    public static IWorldProperty getWorldProperty(String name) {
        return worldProperties.get(name);
    }

    public static void registerWorldProperty(String name, IWorldProperty property) {
        if (worldProperties.containsKey(name)) {
            BCLog.logger.warn("The WorldProperty key '" + name + "' is being overridden with " + property.getClass().getSimpleName() + "!");
        }
        worldProperties.put(name, property);
    }

    public static boolean isSoftBlock(Level world, BlockPos pos) {
        return worldProperties.get("soft").get(world, pos);
    }

    /** 1.12.2 recipe names were free-form ("gate-and-IRON-NO_MODIFIER"); 26.3 ids are lowercase [a-z0-9/._-] only. */
    private static String sanitisePath(String path) {
        return path.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9/._-]", "_");
    }

    public static Identifier nameToResourceLocation(String name) {
        int colon = name.indexOf(':');
        if (colon > 0) {
            return Identifier.fromNamespaceAndPath(name.substring(0, colon), sanitisePath(name.substring(colon + 1)));
        }
        name = sanitisePath(name);
        ModContainer modContainer = ModLoadingContext.get().getActiveContainer();
        if (modContainer == null) {
            throw new IllegalStateException("Illegal recipe name " + name + ". Provide domain id to register it correctly.");
        }
        return Identifier.fromNamespaceAndPath(modContainer.getModId(), name);
    }
}
