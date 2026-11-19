/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.misc;

import java.util.BitSet;
import java.util.EnumSet;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.Sets;

import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import java.util.function.Consumer;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;

import buildcraft.api.core.BCLog;

public final class NBTUtilBC {
    @SuppressWarnings("WeakerAccess")
    public static final CompoundTag NBT_NULL = new CompoundTag();

    public static <N extends Tag> Optional<N> toOptional(N value) {
        return value == NBTUtilBC.NBT_NULL ? Optional.empty() : Optional.of(value);
    }

    public static Tag merge(Tag destination, Tag source) {
        if (source == null) {
            return null;
        }
        if (destination == null) {
            return source;
        }
        if (destination.getId() == Tag.TAG_COMPOUND && source.getId() == Tag.TAG_COMPOUND) {
            CompoundTag result = new CompoundTag();
            for (String key : Sets.union(
                ((CompoundTag) destination).keySet(),
                ((CompoundTag) source).keySet()
            )) {
                if (!((CompoundTag) source).contains(key)) {
                    result.put(key, ((CompoundTag) destination).get(key));
                } else if (((CompoundTag) source).get(key) != NBT_NULL) {
                    if (!((CompoundTag) destination).contains(key)) {
                        result.put(key, ((CompoundTag) source).get(key));
                    } else {
                        result.put(
                            key,
                            merge(
                                ((CompoundTag) destination).get(key),
                                ((CompoundTag) source).get(key)
                            )
                        );
                    }
                }
            }
            return result;
        }
        return source;
    }

    /** Reads or writes a whole compound as the fields of a value input/output, so a tile's 26.3 save holds the same keys as
     * its 1.12.2 NBT did. */
    public static final MapCodec<CompoundTag> WHOLE_COMPOUND = MapCodec.assumeMapUnsafe(CompoundTag.CODEC);

    /** @return A copy of the stack's custom data (1.12.2's stack tag). Changing it does nothing to the stack: use
     *         {@link #setItemData(ItemStack, CompoundTag)} or {@link #updateItemData(ItemStack, Consumer)} to change it. */
    public static CompoundTag getItemData(@Nonnull ItemStack stack) {
        if (stack.isEmpty()) {
            return new CompoundTag();
        }
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    /** @return true if the stack has any custom data (1.12.2's hasTagCompound()). */
    public static boolean hasItemData(@Nonnull ItemStack stack) {
        return !stack.isEmpty() && !stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).isEmpty();
    }

    /** Replaces the stack's custom data (removing it if the given compound is null or empty). */
    public static void setItemData(@Nonnull ItemStack stack, @Nullable CompoundTag nbt) {
        if (stack.isEmpty()) {
            return;
        }
        if (nbt == null || nbt.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
        }
    }

    /** Changes the stack's custom data in place. */
    public static void updateItemData(@Nonnull ItemStack stack, Consumer<CompoundTag> changer) {
        CompoundTag nbt = getItemData(stack);
        changer.accept(nbt);
        setItemData(stack, nbt);
    }

    /** Writes a game profile the way 1.12.2 did ("Name" and "Id" as a string), so saved owners carry over. */
    public static CompoundTag writeGameProfile(GameProfile profile) {
        CompoundTag nbt = new CompoundTag();
        if (profile.name() != null && !profile.name().isEmpty()) {
            nbt.putString("Name", profile.name());
        }
        if (profile.id() != null) {
            nbt.putString("Id", profile.id().toString());
        }
        return nbt;
    }

    /** @return The game profile written by {@link #writeGameProfile(GameProfile)} (or 1.12.2), or null if it has no id. */
    @Nullable
    public static GameProfile readGameProfile(CompoundTag nbt) {
        String name = nbt.getStringOr("Name", "");
        String id = nbt.getStringOr("Id", "");
        if (id.isEmpty()) {
            return null;
        }
        try {
            return new GameProfile(UUID.fromString(id), name);
        } catch (IllegalArgumentException e) {
            BCLog.logger.warn("[lib.nbt] Ignoring a game profile with an invalid id: " + nbt);
            return null;
        }
    }

    public static IntArrayTag writeBlockPos(BlockPos pos) {
        if (pos == null) {
            throw new NullPointerException("Cannot return a null NBTTag -- pos was null!");
        }
        return new IntArrayTag(new int[] { pos.getX(), pos.getY(), pos.getZ() });
    }

    @SuppressWarnings("unused")
    public static CompoundTag writeBlockPosAsCompound(BlockPos pos) {
        if (pos == null) {
            throw new NullPointerException("Cannot return a null NBTTag -- pos was null!");
        }
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("x", pos.getX());
        nbt.putInt("y", pos.getY());
        nbt.putInt("z", pos.getZ());
        return nbt;
    }

    /** 1.12.2's NBTUtil.createPosTag layout: {X, Y, Z}. */
    public static CompoundTag writeLegacyPosTag(BlockPos pos) {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("X", pos.getX());
        nbt.putInt("Y", pos.getY());
        nbt.putInt("Z", pos.getZ());
        return nbt;
    }

    /** Reads 1.12.2's NBTUtil.createPosTag layout: {X, Y, Z}. */
    public static BlockPos readLegacyPosTag(CompoundTag nbt) {
        return new BlockPos(nbt.getIntOr("X", 0), nbt.getIntOr("Y", 0), nbt.getIntOr("Z", 0));
    }

    /** 1.12.2's NBTUtil.createUUIDTag layout: {M, L}. */
    public static CompoundTag writeLegacyUUIDTag(java.util.UUID id) {
        CompoundTag nbt = new CompoundTag();
        nbt.putLong("M", id.getMostSignificantBits());
        nbt.putLong("L", id.getLeastSignificantBits());
        return nbt;
    }

    /** Reads 1.12.2's NBTUtil.createUUIDTag layout: {M, L}. */
    public static java.util.UUID readLegacyUUIDTag(CompoundTag nbt) {
        return new java.util.UUID(nbt.getLongOr("M", 0), nbt.getLongOr("L", 0));
    }

    /** 1.12.2's NBTTagCompound.setUniqueId: two longs, key + "Most" and key + "Least". */
    public static void putLegacyUniqueId(CompoundTag nbt, String key, java.util.UUID id) {
        nbt.putLong(key + "Most", id.getMostSignificantBits());
        nbt.putLong(key + "Least", id.getLeastSignificantBits());
    }

    /** Reads 1.12.2's NBTTagCompound.setUniqueId layout, or null if it isn't there. */
    @Nullable
    public static java.util.UUID getLegacyUniqueId(CompoundTag nbt, String key) {
        if (!nbt.contains(key + "Most") || !nbt.contains(key + "Least")) {
            return null;
        }
        return new java.util.UUID(nbt.getLongOr(key + "Most", 0), nbt.getLongOr(key + "Least", 0));
    }

    @Nullable
    public static BlockPos readBlockPos(Tag base) {
        if (base == null) {
            return null;
        }
        switch (base.getId()) {
            case Tag.TAG_INT_ARRAY: {
                int[] array = ((IntArrayTag) base).getAsIntArray();
                if (array.length == 3){
                    return new BlockPos(array[0], array[1], array[2]);
                }
                return null;
            }
            case Tag.TAG_COMPOUND: {
                CompoundTag nbt = (CompoundTag) base;
                BlockPos pos = null;
                if (nbt.contains("i")) {
                    int i = nbt.getIntOr("i", 0);
                    int j = nbt.getIntOr("j", 0);
                    int k = nbt.getIntOr("k", 0);
                    pos = new BlockPos(i, j, k);
                } else if (nbt.contains("x")) {
                    int x = nbt.getIntOr("x", 0);
                    int y = nbt.getIntOr("y", 0);
                    int z = nbt.getIntOr("z", 0);
                    pos = new BlockPos(x, y, z);
                } else if (nbt.contains("X")) {
                    pos = readLegacyPosTag(nbt);
                } else if (nbt.contains("pos")) {
                    return readBlockPos(nbt.get("pos"));
                } else {
                    BCLog.logger.warn("Attempted to read a block positions from a compound tag without the correct sub-tags! (" + base + ")", new Throwable());
                }
                return pos;
            }
        }
        BCLog.logger.warn("Attempted to read a block position from an invalid tag! (" + base + ")", new Throwable());
        return null;
    }

    public static ListTag writeVec3d(Vec3 vec3) {
        ListTag list = new ListTag();
        list.add(new DoubleTag(vec3.x));
        list.add(new DoubleTag(vec3.y));
        list.add(new DoubleTag(vec3.z));
        return list;
    }

    @Nullable
    public static Vec3 readVec3d(Tag nbt) {
        if (nbt instanceof ListTag) {
            return readVec3d((ListTag) nbt);
        }
        return null;
    }

    public static Vec3 readVec3d(ListTag list) {
        return new Vec3(list.getDoubleOr(0, 0), list.getDoubleOr(1, 0), list.getDoubleOr(2, 0));
    }

    private static final String NULL_ENUM_STRING = "_NULL";

    public static <E extends Enum<E>> Tag writeEnum(E value) {
        if (value == null) {
            return new StringTag(NULL_ENUM_STRING);
        }
        return new StringTag(value.name());
    }

    public static <E extends Enum<E>> E readEnum(Tag nbt, Class<E> clazz) {
        if (nbt instanceof StringTag) {
            String value = ((StringTag) nbt).value();
            if (NULL_ENUM_STRING.equals(value)) {
                return null;
            }
            try {
                return Enum.valueOf(clazz, value);
            } catch (Throwable t) {
                // In case we didn't find the constant
                BCLog.logger.warn("Tried and failed to read the value(" + value + ") from " + clazz.getSimpleName(), t);
                return null;
            }
        } else if (nbt instanceof ByteTag) {
            byte value = ((ByteTag) nbt).byteValue();
            if (value < 0 || value >= clazz.getEnumConstants().length) {
                return null;
            } else {
                return clazz.getEnumConstants()[value];
            }
        } else if (nbt == null) {
            return null;
        } else {
            BCLog.logger.warn(new IllegalArgumentException("Tried to read an enum value when it was not a string! This is probably not good!"));
            return null;
        }
    }

    public static Tag writeDoubleArray(double[] data) {
        ListTag list = new ListTag();
        for (double d : data) {
            list.add(new DoubleTag(d));
        }
        return list;
    }

    public static double[] readDoubleArray(Tag tag, int intendedLength) {
        double[] arr = new double[intendedLength];
        if (tag instanceof ListTag) {
            ListTag list = (ListTag) tag;
            for (int i = 0; i < list.size() && i < intendedLength; i++) {
                arr[i] = list.getDoubleOr(i, 0);
            }
        }
        return arr;
    }

    /** Writes an {@link EnumSet} to an {@link Tag}. The returned type will either be {@link ByteTag} or
     * {@link ByteArrayTag}.
     * 
     * @param clazz The class that the {@link EnumSet} is of. This is required as we have no way of getting the class
     *            from the set. */
    public static <E extends Enum<E>> Tag writeEnumSet(EnumSet<E> set, Class<E> clazz) {
        E[] constants = clazz.getEnumConstants();
        if (constants == null) throw new IllegalArgumentException("Not an enum type " + clazz);
        BitSet bitset = new BitSet();
        for (E e : constants) {
            if (set.contains(e)) {
                bitset.set(e.ordinal());
            }
        }
        byte[] bytes = bitset.toByteArray();
        if (bytes.length == 1) {
            return new ByteTag(bytes[0]);
        } else {
            return new ByteArrayTag(bytes);
        }
    }

    public static <E extends Enum<E>> EnumSet<E> readEnumSet(Tag tag, Class<E> clazz) {
        E[] constants = clazz.getEnumConstants();
        if (constants == null) throw new IllegalArgumentException("Not an enum type " + clazz);
        byte[] bytes;
        if (tag instanceof ByteTag) {
            bytes = new byte[] { ((ByteTag) tag).byteValue() };
        } else if (tag instanceof ByteArrayTag) {
            bytes = ((ByteArrayTag) tag).getAsByteArray();
        } else {
            bytes = new byte[] {};
            BCLog.logger.warn("[lib.nbt] Tried to read an enum set from " + tag);
        }
        BitSet bitset = BitSet.valueOf(bytes);
        EnumSet<E> set = EnumSet.noneOf(clazz);
        for (E e : constants) {
            if (bitset.get(e.ordinal())) {
                set.add(e);
            }
        }
        return set;
    }

    public static ListTag writeCompoundList(Stream<CompoundTag> stream) {
        ListTag list = new ListTag();
        stream.forEach(list::add);
        return list;
    }

    public static Stream<CompoundTag> readCompoundList(Tag list) {
        if (list == null) {
            return Stream.empty();
        }
        if (!(list instanceof ListTag)) {
            throw new IllegalArgumentException();
        }
        return IntStream.range(0, ((ListTag) list).size()).mapToObj(((ListTag) list)::getCompoundOrEmpty);
    }

    public static ListTag writeStringList(Stream<String> stream) {
        ListTag list = new ListTag();
        stream.map(StringTag::valueOf).forEach(list::add);
        return list;
    }

    public static Stream<String> readStringList(Tag list) {
        if (list == null) {
            return Stream.empty();
        }
        if (!(list instanceof ListTag)) {
            throw new IllegalArgumentException();
        }
        return IntStream.range(0, ((ListTag) list).size()).mapToObj(i -> ((ListTag) list).getStringOr(i, ""));
    }
}
