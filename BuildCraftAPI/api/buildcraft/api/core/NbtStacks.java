package buildcraft.api.core;

import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;

import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/** Saves item and fluid stacks inside BuildCraft's NBT, and reads them back, including stacks written by BuildCraft on
 * Minecraft 1.12.2 (upgraded through Minecraft's own data fixer, plus {@link #legacyItemFixer} for BuildCraft's own
 * flattened items). */
public final class NbtStacks {
    private NbtStacks() {}

    /** The data version of Minecraft 1.12.2. */
    public static final int DATA_VERSION_1_12_2 = 1343;

    /** Where registries come from when encoding stacks. Set by buildcraft lib (client and server); the default asks the
     * running server. */
    public static Supplier<HolderLookup.Provider> registries = () -> {
        var server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? null : server.registryAccess();
    };

    /** Applied to a 1.12.2 stack's NBT (id, Count, Damage, tag) before the vanilla data fixer runs, so buildcraft can turn
     * its own damage-based items into the flattened 26.3 items. Set by buildcraft lib. */
    public static Function<CompoundTag, CompoundTag> legacyItemFixer = Function.identity();

    private static DynamicOps<Tag> ops() {
        HolderLookup.Provider provider = registries.get();
        return provider == null ? NbtOps.INSTANCE : provider.createSerializationContext(NbtOps.INSTANCE);
    }

    /** @return true if the given stack NBT was written by Minecraft 1.12.2 (it has "Count" rather than "count"). */
    public static boolean isLegacyStack(CompoundTag nbt) {
        return nbt.contains("Count") && !nbt.contains("count");
    }

    @Nonnull
    public static CompoundTag writeItemStack(@Nonnull ItemStack stack) {
        if (stack.isEmpty()) {
            return new CompoundTag();
        }
        Tag tag = ItemStack.CODEC.encodeStart(ops(), stack).result().orElse(null);
        return tag instanceof CompoundTag compound ? compound : new CompoundTag();
    }

    @Nonnull
    public static ItemStack readItemStack(@Nullable CompoundTag nbt) {
        if (nbt == null || nbt.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (isLegacyStack(nbt)) {
            nbt = upgradeLegacyItemStack(nbt);
        }
        return ItemStack.OPTIONAL_CODEC.parse(ops(), nbt).result().orElse(ItemStack.EMPTY);
    }

    /** @return The 26.3 form of a stack saved by Minecraft 1.12.2. */
    @Nonnull
    public static CompoundTag upgradeLegacyItemStack(@Nonnull CompoundTag nbt) {
        CompoundTag fixed = legacyItemFixer.apply(nbt.copy());
        int current = SharedConstants.getCurrentVersion().dataVersion().version();
        Dynamic<Tag> upgraded = DataFixers.getDataFixer().update(References.ITEM_STACK, new Dynamic<>(NbtOps.INSTANCE, fixed),
            DATA_VERSION_1_12_2, current);
        Tag tag = upgraded.getValue();
        return tag instanceof CompoundTag compound ? compound : new CompoundTag();
    }

    @Nonnull
    public static CompoundTag writeFluidStack(@Nullable FluidStack stack) {
        if (stack == null || stack.isEmpty()) {
            return new CompoundTag();
        }
        Tag tag = FluidStack.CODEC.encodeStart(ops(), stack).result().orElse(null);
        return tag instanceof CompoundTag compound ? compound : new CompoundTag();
    }

    /** Reads a fluid stack, including 1.12.2's form ({FluidName, Amount, Tag}). Fluids whose names changed are mapped by
     * {@link #legacyFluidNames}.
     *
     * @return The stack, or null for none. */
    @Nullable
    public static FluidStack readFluidStack(@Nullable CompoundTag nbt) {
        if (nbt == null || nbt.isEmpty()) {
            return null;
        }
        if (nbt.contains("FluidName")) {
            String name = legacyFluidNames.apply(nbt.getStringOr("FluidName", ""));
            if (name == null || name.isEmpty()) {
                return null;
            }
            CompoundTag modern = new CompoundTag();
            modern.putString("id", name);
            modern.putInt("amount", Math.max(1, nbt.getIntOr("Amount", 0)));
            nbt = modern;
        }
        FluidStack stack = FluidStack.OPTIONAL_CODEC.parse(ops(), nbt).result().orElse(FluidStack.EMPTY);
        return stack.isEmpty() ? null : stack;
    }

    /** Maps a 1.12.2 fluid name ("water", "oil", "fuel_dense_heat_1", ...) to a 26.3 fluid id, or null if there's none.
     * Set by buildcraft lib; the default handles vanilla fluids only. */
    public static Function<String, String> legacyFluidNames = name -> switch (name) {
        case "water" -> "minecraft:water";
        case "lava" -> "minecraft:lava";
        default -> name.contains(":") ? name : null;
    };
}
