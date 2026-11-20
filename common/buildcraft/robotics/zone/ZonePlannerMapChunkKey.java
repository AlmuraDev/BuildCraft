/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.robotics.zone;

import java.util.Objects;

import io.netty.buffer.ByteBuf;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

/** One chunk column of the zone planner map, for one dimension and one height band ("level", 32 blocks each). 1.12.2
 * keyed dimensions by number; 26.3 uses the dimension's key. */
public class ZonePlannerMapChunkKey {
    public static final int LEVEL_HEIGHT = 32;

    public final ChunkPos chunkPos;
    public final ResourceKey<Level> dimension;
    public final int level;
    private final int hash;

    public ZonePlannerMapChunkKey(ChunkPos chunkPos, ResourceKey<Level> dimension, int level) {
        this.chunkPos = chunkPos;
        this.dimension = dimension;
        this.level = level;
        hash = Objects.hash(chunkPos.x(), chunkPos.z(), dimension, level);
    }

    public ZonePlannerMapChunkKey(ByteBuf buf) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(buf);
        chunkPos = new ChunkPos(buffer.readInt(), buffer.readInt());
        dimension = ResourceKey.create(Registries.DIMENSION, Identifier.parse(buffer.readUtf()));
        level = buffer.readInt();
        hash = Objects.hash(chunkPos.x(), chunkPos.z(), dimension, level);
    }

    public void toBytes(ByteBuf buf) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(buf);
        buffer.writeInt(chunkPos.x());
        buffer.writeInt(chunkPos.z());
        buffer.writeUtf(dimension.identifier().toString());
        buffer.writeInt(level);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (o == null || o.getClass() != getClass()) return false;
        ZonePlannerMapChunkKey other = (ZonePlannerMapChunkKey) o;
        return level == other.level && dimension.equals(other.dimension) && chunkPos.x() == other.chunkPos.x()
            && chunkPos.z() == other.chunkPos.z();
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
