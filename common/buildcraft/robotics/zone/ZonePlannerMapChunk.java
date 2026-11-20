/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.robotics.zone;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

/** The map colour and height of the top block (below the key's height band) of every column in a chunk. */
public class ZonePlannerMapChunk {
    private final MapColourData[][] data = new MapColourData[16][16];

    public ZonePlannerMapChunk(Level world, ZonePlannerMapChunkKey key) {
        LevelChunk chunk = world.getChunk(key.chunkPos.x(), key.chunkPos.z());
        int top = Math.min(key.level * ZonePlannerMapChunkKey.LEVEL_HEIGHT, world.getMaxY());
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                // Scan down from the top of the band until we find a block
                for (int y = top; y > world.getMinY(); y--) {
                    pos.set(key.chunkPos.getMinBlockX() + x, y, key.chunkPos.getMinBlockZ() + z);
                    int colour = chunk.getBlockState(pos).getMapColor(world, pos).col;
                    if (colour != 0) {
                        data[x][z] = new MapColourData(y, colour);
                        break;
                    }
                }
            }
        }
    }

    public ZonePlannerMapChunk(FriendlyByteBuf buffer) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                boolean present = buffer.readBoolean();
                if (present) {
                    int posY = buffer.readInt();
                    int colour = buffer.readInt();
                    data[x][z] = new MapColourData(posY, colour);
                }
            }
        }
    }

    public void write(FriendlyByteBuf buffer) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                MapColourData colour = data[x][z];
                buffer.writeBoolean(colour != null);
                if (colour != null) {
                    buffer.writeInt(colour.posY);
                    buffer.writeInt(colour.colour);
                }
            }
        }
    }

    public int getColour(int x, int z) {
        MapColourData col = getData(x, z);
        return col == null ? -1 : col.colour;
    }

    @Nullable
    public MapColourData getData(int x, int z) {
        return data[x & 15][z & 15];
    }

    public static final class MapColourData {
        public final int posY;
        public final int colour;

        public MapColourData(int posY, int colour) {
            this.posY = posY;
            this.colour = colour;
        }
    }
}
