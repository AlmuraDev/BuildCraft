/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.energy.generation;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.feature.Feature;

import buildcraft.core.BCCoreConfig;
import buildcraft.energy.BCEnergyConfig;

/** Runs {@link OilGenerator} once for each generated chunk. The placed feature
 * (data/buildcraftenergy/worldgen/placed_feature/oil_well.json) has no placement modifiers, so the origin is the chunk's
 * corner; a biome modifier adds it to every biome in the "lakes" step (1.12.2 generated before decoration), and the
 * dimension, biome and flat-world checks happen here, as they did on 1.12.2. */
public record OilWellFeature() implements Feature {
    public static final MapCodec<OilWellFeature> CODEC = MapCodec.unit(OilWellFeature::new);

    @Override
    public MapCodec<OilWellFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        if (!BCCoreConfig.worldGen || !BCEnergyConfig.enableOilGeneration) {
            return false;
        }
        if (chunkGenerator instanceof FlatLevelSource) {
            return false;
        }
        OilGenerator.generateChunk(level, origin.getX() >> 4, origin.getZ() >> 4);
        return true;
    }
}
