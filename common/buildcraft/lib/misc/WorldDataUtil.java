/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.misc;

import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/** Saving and loading tiles and entities as NBT (1.12.2's TileEntity/Entity serializeNBT and TileEntity.create /
 * EntityList.createEntityFromNBT), which 26.3 does through value inputs and outputs and the level's registries. */
public final class WorldDataUtil {
    private WorldDataUtil() {}

    /** @return The tile's full NBT, with its id and position (1.12.2's TileEntity.serializeNBT). */
    public static CompoundTag saveTile(BlockEntity tile) {
        Level level = tile.getLevel();
        if (level == null) {
            throw new IllegalStateException("The tile " + tile + " isn't in a level!");
        }
        return tile.saveWithFullMetadata(level.registryAccess());
    }

    /** Creates the tile its NBT describes (with "id") at the given position and puts it in the level (1.12.2's
     * TileEntity.create + World.setTileEntity).
     *
     * @return The tile, or null if the NBT didn't describe a tile valid for the block there. */
    @Nullable
    public static BlockEntity placeTile(Level level, BlockPos pos, BlockState state, CompoundTag nbt) {
        BlockEntity tile = BlockEntity.loadStatic(pos, state, nbt, level.registryAccess());
        if (tile != null) {
            level.setBlockEntity(tile);
        }
        return tile;
    }

    /** @return The entity's NBT with its id, or null if it can't be saved (1.12.2's Entity.serializeNBT). */
    @Nullable
    public static CompoundTag saveEntity(Entity entity) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING,
            entity.level().registryAccess());
        if (!entity.save(output)) {
            return null;
        }
        return output.buildResult();
    }

    /** Creates the entity its NBT describes (with "id"), without adding it to the level. */
    public static Optional<Entity> createEntity(Level level, CompoundTag nbt) {
        return EntityType.create(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), nbt), level,
            new net.minecraft.world.entity.EntitySpawnRequest(EntitySpawnReason.LOAD, true));
    }
}
