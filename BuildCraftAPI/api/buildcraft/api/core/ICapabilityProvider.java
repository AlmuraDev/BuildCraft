package buildcraft.api.core;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.Direction;

import net.neoforged.neoforge.capabilities.BlockCapability;

/** Something that can be asked for a capability on a side, the way Forge's capability providers could on 1.12. BuildCraft's
 * tiles, pipes, pipe behaviours, flows and pluggables all implement this; tiles then hand their answers to NeoForge's
 * capability system. */
public interface ICapabilityProvider {

    /** @return The capability instance for the given side (null for no side), or null if this doesn't provide it. */
    @Nullable
    <T> T getCapability(@Nonnull BlockCapability<T, Direction> capability, @Nullable Direction facing);

    default boolean hasCapability(@Nonnull BlockCapability<?, Direction> capability, @Nullable Direction facing) {
        return getCapability(capability, facing) != null;
    }
}
