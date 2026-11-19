/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.misc.data;

import java.util.function.Supplier;

import com.mojang.serialization.Codec;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** World saved data that reads and writes itself as NBT, as 1.12.2's WorldSavedData did. 26.3 saved data is found by a
 * {@link SavedDataType}: make one with {@link #type(Identifier, Supplier)} and get the data with
 * {@code serverLevel.getDataStorage().computeIfAbsent(TYPE)}. */
public abstract class NbtSavedData extends SavedData {

    public abstract void readFromNBT(CompoundTag nbt);

    public abstract CompoundTag writeToNBT(CompoundTag nbt);

    public static <T extends NbtSavedData> SavedDataType<T> type(Identifier id, Supplier<T> constructor) {
        Codec<T> codec = CompoundTag.CODEC.xmap(nbt -> {
            T data = constructor.get();
            data.readFromNBT(nbt);
            return data;
        }, data -> data.writeToNBT(new CompoundTag()));
        return new SavedDataType<>(id, constructor, codec);
    }
}
