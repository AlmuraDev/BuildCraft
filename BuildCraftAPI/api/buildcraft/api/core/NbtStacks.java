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

    /** Runs the vanilla data fixer for 1.12.2 stacks, one at a time, off whatever thread asked. */
    private static final java.util.concurrent.ExecutorService FIXER_THREAD =
        java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "BuildCraft legacy item fixer");
            t.setDaemon(true);
            return t;
        });

    /** Set once the data fixer has hung or failed: from then on every 1.12.2 stack takes the direct upgrade. */
    private static volatile boolean fixerBroken = false;

    /** How long a caller waits for the data fixer before giving up on it (the first upgrade builds its rules). */
    private static final long FIXER_TIMEOUT_FIRST_MS = 20_000;
    private static final long FIXER_TIMEOUT_MS = 5_000;
    private static volatile boolean fixerUsed = false;

    /** @return The 26.3 form of a stack saved by Minecraft 1.12.2.
     *
     *          <p>The vanilla data fixer is never run on the calling thread: on a 26.3 dedicated server a fixer rule that
     *          another thread left pending blocks DFU's Type.rewrite forever, which froze the server thread while it
     *          loaded a 1.12.2 diamond pipe. The fixer runs on its own thread with a time limit; if it hangs or fails
     *          the stack is upgraded directly (flattened id, count, the 1.12.2 tag kept as custom data). */
    @Nonnull
    public static CompoundTag upgradeLegacyItemStack(@Nonnull CompoundTag nbt) {
        CompoundTag fixed = legacyItemFixer.apply(nbt.copy());
        if (!fixerBroken) {
            java.util.concurrent.Future<CompoundTag> future = FIXER_THREAD.submit(() -> runDataFixer(fixed.copy()));
            long timeout = fixerUsed ? FIXER_TIMEOUT_MS : FIXER_TIMEOUT_FIRST_MS;
            try {
                CompoundTag result = future.get(timeout, java.util.concurrent.TimeUnit.MILLISECONDS);
                fixerUsed = true;
                return result;
            } catch (java.util.concurrent.TimeoutException e) {
                fixerBroken = true;
                future.cancel(true);
                BCLog.logger.error("[api.nbt] The data fixer didn't upgrade a 1.12.2 item stack within " + timeout
                    + " ms; upgrading 1.12.2 stacks directly from now on. Stack: " + nbt);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (java.util.concurrent.ExecutionException e) {
                BCLog.logger.warn("[api.nbt] The data fixer failed on a 1.12.2 item stack, upgrading it directly: " + nbt,
                    e.getCause());
            }
        }
        return upgradeDirectly(fixed);
    }

    private static CompoundTag runDataFixer(CompoundTag fixed) {
        int current = SharedConstants.getCurrentVersion().dataVersion().version();
        Dynamic<Tag> upgraded = DataFixers.getDataFixer().update(References.ITEM_STACK, new Dynamic<>(NbtOps.INSTANCE, fixed),
            DATA_VERSION_1_12_2, current);
        Tag tag = upgraded.getValue();
        return tag instanceof CompoundTag compound ? compound : new CompoundTag();
    }

    /** A 1.12.2 stack upgraded without the data fixer: the 1.13 flattening of id + damage, the count, and the 1.12.2
     * tag kept whole under minecraft:custom_data (so nothing is lost; names and enchantments aren't converted). */
    private static CompoundTag upgradeDirectly(CompoundTag legacy) {
        String id = legacy.getStringOr("id", "minecraft:air");
        int damage = legacy.getShortOr("Damage", (short) 0);
        if (!id.contains(":")) {
            id = "minecraft:" + id;
        }
        String flattened = net.minecraft.util.datafix.fixes.ItemStackTheFlatteningFix.updateItem(id, damage);
        CompoundTag out = new CompoundTag();
        out.putString("id", flattened != null ? flattened : id);
        out.putInt("count", Math.max(1, legacy.getByteOr("Count", (byte) 1)));
        CompoundTag tag = legacy.getCompoundOrEmpty("tag");
        if (!tag.isEmpty()) {
            CompoundTag components = new CompoundTag();
            components.put("minecraft:custom_data", tag.copy());
            out.put("components", components);
        }
        return out;
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
