/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.misc.data;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** A position in a particular dimension (1.12.2 used dimension numbers; 26.3 has dimension keys). */
public final class WorldPos {
    @SuppressWarnings("WeakerAccess")
    public final ResourceKey<Level> dimension;
    public final BlockPos pos;

    @SuppressWarnings("WeakerAccess")
    public WorldPos(ResourceKey<Level> dimension, BlockPos pos) {
        this.dimension = dimension;
        this.pos = pos.immutable();
    }

    public WorldPos(Level world, BlockPos pos) {
        this(world.dimension(), pos);
    }

    public WorldPos(BlockEntity tile) {
        this(tile.getLevel(), tile.getBlockPos());
    }

    @Override
    public boolean equals(Object o) {
        return this == o ||
            o != null &&
                getClass() == o.getClass() &&
                dimension.equals(((WorldPos) o).dimension) &&
                pos.equals(((WorldPos) o).pos);

    }

    @Override
    public int hashCode() {
        return 31 * dimension.hashCode() + pos.hashCode();
    }
}
