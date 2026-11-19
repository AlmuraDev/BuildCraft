/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.core.gen;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

import buildcraft.api.enums.EnumSpring;
import buildcraft.api.properties.BuildCraftProperties;

import buildcraft.core.BCCoreBlocks;
import buildcraft.core.BCCoreConfig;

/** Puts a water spring into the bedrock under the origin column (1.12.2's SpringPopulate). The placed feature
 * (data/buildcraftcore/worldgen/placed_feature/water_spring.json) picks one chunk in 40, and a biome modifier adds it to
 * the overworld only. */
public record WaterSpringFeature() implements Feature {
    public static final MapCodec<WaterSpringFeature> CODEC = MapCodec.unit(WaterSpringFeature::new);

    @Override
    public MapCodec<WaterSpringFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        if (!BCCoreConfig.worldGen || !BCCoreConfig.worldGenWaterSpring || !EnumSpring.WATER.canGen
            || BCCoreBlocks.spring == null) {
            return false;
        }
        int minY = level.getMinY();
        for (int i = 0; i < 5; i++) {
            BlockPos pos = new BlockPos(origin.getX(), minY + i, origin.getZ());
            if (!level.getBlockState(pos).is(Blocks.BEDROCK)) {
                continue;
            }
            // Handle flat bedrock maps
            int y = i > 0 ? minY + i : minY;
            BlockState springState = BCCoreBlocks.spring.defaultBlockState()
                .setValue(BuildCraftProperties.SPRING_TYPE, EnumSpring.WATER);
            level.setBlock(new BlockPos(origin.getX(), y, origin.getZ()), springState, 2);
            // Fill the column above with water up to the first air, so the spring flows straight away
            for (int j = y + 2; j < level.getMaxY(); j++) {
                BlockPos above = new BlockPos(origin.getX(), j, origin.getZ());
                if (level.isEmptyBlock(above)) {
                    break;
                }
                level.setBlock(above, Blocks.WATER.defaultBlockState(), 2);
            }
            return true;
        }
        return false;
    }
}
