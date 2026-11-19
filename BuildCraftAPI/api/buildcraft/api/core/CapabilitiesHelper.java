package buildcraft.api.core;

import java.util.Locale;

import javax.annotation.Nonnull;

import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

import net.neoforged.neoforge.capabilities.BlockCapability;

/** Creates BuildCraft's capabilities. On NeoForge every BuildCraft capability is a sided {@link BlockCapability}: block
 * entities expose them to the world through NeoForge's capability registration, and pipes, pipe behaviours, flows and
 * pluggables use the same keys among themselves. Mods wishing to add compatibility should refer to the various fields in
 * buildcraft api classes containing the capability instances. */
public class CapabilitiesHelper {

    /** The namespace every BuildCraft API capability is created in. */
    public static final String NAMESPACE = "buildcraftlib";

    /** Creates (or returns the existing) sided block capability for the given type. Its name is the type's simple name in
     * lower case, in the {@link #NAMESPACE} namespace.
     *
     * @param clazz The type that all instances must derive from.
     * @return The capability */
    @Nonnull
    public static <T> BlockCapability<T, Direction> registerCapability(Class<T> clazz) {
        String path = clazz.getSimpleName().toLowerCase(Locale.ROOT);
        return BlockCapability.createSided(Identifier.fromNamespaceAndPath(NAMESPACE, path), clazz);
    }
}
