/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

public class ChunkUtil {
    private static final ThreadLocal<LevelChunk> lastChunk = new ThreadLocal<>();

    public static LevelChunk getChunk(Level world, BlockPos pos, boolean force) {
        return getChunk(world, pos.getX() >> 4, pos.getZ() >> 4, force);
    }

    public static LevelChunk getChunk(Level world, ChunkPos pos, boolean force) {
        return getChunk(world, pos.x(), pos.z(), force);
    }

    /** @return true if the given chunk is still the loaded chunk at its position (1.12.2's Chunk.isLoaded()). */
    public static boolean isLoaded(LevelChunk chunk) {
        Level world = chunk.getLevel();
        return world.getChunkSource().getChunkNow(chunk.getPos().x(), chunk.getPos().z()) == chunk;
    }

    public static LevelChunk getChunk(Level world, int x, int z, boolean force) {
        LevelChunk chunk = lastChunk.get();

        if (chunk != null) {
            if (isLoaded(chunk)) {
                if (chunk.getLevel() == world && chunk.getPos().x() == x && chunk.getPos().z() == z) {
                    return chunk;
                }
            } else {
                lastChunk.set(null);
            }
        }

        if (force) {
            chunk = world.getChunk(x, z);
        } else {
            chunk = world.getChunkSource().getChunkNow(x, z);
        }

        if (chunk != null) {
            lastChunk.set(chunk);
        }
        return chunk;
    }
}
