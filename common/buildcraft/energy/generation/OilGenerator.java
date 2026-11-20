/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.energy.generation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.function.Predicate;

import com.google.common.collect.ImmutableList;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;

import buildcraft.api.core.BCDebugging;
import buildcraft.api.core.BCLog;

import buildcraft.lib.misc.RandUtil;
import buildcraft.lib.misc.VecUtil;
import buildcraft.lib.misc.data.Box;

import buildcraft.core.BCCoreBlocks;
import buildcraft.energy.BCEnergyConfig;
import buildcraft.energy.generation.OilGenStructure.GenByPredicate;
import buildcraft.energy.generation.OilGenStructure.ReplaceType;

/** Oil wells, lakes and spouts (1.12.2 generated them on PopulateChunkEvent.Pre; 26.3 runs {@link OilWellFeature} once
 * per chunk). Wells are worked out from each chunk's own random, so neighbouring chunks agree on them; each generating
 * chunk places the parts of all wells within {@link #MAX_CHUNK_RADIUS} chunks that fall inside it. */
public class OilGenerator {
    private OilGenerator() {}

    /** Random number, used to differentiate generators */
    private static final long MAGIC_GEN_NUMBER = 0xD0_46_B4_E4_0C_7D_07_CFL;

    /** The distance that oil generation will be checked to see if their structures overlap with the currently
     * generating chunk. This should be large enough that all oil generation can fit inside this radius. If this number
     * is too big then oil generation will be slightly slower */
    private static final int MAX_CHUNK_RADIUS = 5;

    public static final boolean DEBUG_OILGEN_BASIC = BCDebugging.shouldDebugLog("energy.oilgen");
    public static final boolean DEBUG_OILGEN_ALL = BCDebugging.shouldDebugComplex("energy.oilgen");

    private enum GenType {
        LARGE,
        MEDIUM,
        LAKE,
        NONE
    }

    /** Generates the parts of nearby oil wells that are in the given chunk. */
    public static void generateChunk(WorldGenLevel world, int chunkX, int chunkZ) {
        ResourceKey<Level> dimension = world.getLevel().dimension();
        if (!BCEnergyConfig.isDimensionAllowed(dimension)) {
            if (DEBUG_OILGEN_BASIC) {
                BCLog.logger.info("[energy.oilgen] Not generating oil in " + dimension.identifier() + " chunk "
                    + chunkX + ", " + chunkZ + " because its dimension is disabled.");
            }
            return;
        }

        int x = chunkX * 16;
        int z = chunkZ * 16;
        Box box = new Box(new BlockPos(x, world.getMinY(), z), new BlockPos(x + 15, world.getMaxY(), z + 15));

        for (int cdx = -MAX_CHUNK_RADIUS; cdx <= MAX_CHUNK_RADIUS; cdx++) {
            for (int cdz = -MAX_CHUNK_RADIUS; cdz <= MAX_CHUNK_RADIUS; cdz++) {
                int cx = chunkX + cdx;
                int cz = chunkZ + cdz;
                List<OilGenStructure> structures = getStructures(world, cx, cz, cdx == 0 && cdz == 0);
                OilGenStructure.Spring spring = null;
                for (OilGenStructure struct : structures) {
                    struct.generate(world, box);
                    if (struct instanceof OilGenStructure.Spring) {
                        spring = (OilGenStructure.Spring) struct;
                    }
                }
                if (spring != null && box.contains(spring.pos)) {
                    int count = 0;
                    // The spout shares the spring's column, so it was generated just above
                    for (OilGenStructure struct : structures) {
                        count += struct.countOilBlocks();
                    }
                    spring.generate(world, count);
                }
            }
        }
    }

    private static Holder<Biome> biomeAt(WorldGenLevel world, int x, int y, int z) {
        net.minecraft.server.level.ServerChunkCache chunks = world.getLevel().getChunkSource();
        return chunks.getGenerator().getBiomeSource().createUncachedResolver(chunks.randomState()).getNoiseBiome(
            net.minecraft.core.QuartPos.fromBlock(x), net.minecraft.core.QuartPos.fromBlock(y),
            net.minecraft.core.QuartPos.fromBlock(z));
    }

    public static List<OilGenStructure> getStructures(WorldGenLevel world, int cx, int cz) {
        return getStructures(world, cx, cz, false);
    }

    private static List<OilGenStructure> getStructures(WorldGenLevel world, int cx, int cz, boolean log) {
        Random rand = RandUtil.createRandomForChunk(world.getSeed(), cx, cz, MAGIC_GEN_NUMBER);

        // shift to world coordinates
        int x = cx * 16 + 8 + rand.nextInt(16);
        int z = cz * 16 + 8 + rand.nextInt(16);

        // The well's centre is up to MAX_CHUNK_RADIUS chunks away, outside what a 26.3 feature may read, so the biome
        // comes from the biome source (which needs no chunk) rather than the world-gen region
        Holder<Biome> biome = biomeAt(world, x, world.getSeaLevel(), z);
        Identifier biomeName = biome.unwrapKey().map(ResourceKey::identifier).orElse(null);

        // Do not generate oil in excluded biomes
        boolean isExcludedBiome = biomeName != null && BCEnergyConfig.excludedBiomes.contains(biomeName);
        if (isExcludedBiome == BCEnergyConfig.excludedBiomesIsBlackList) {
            if (DEBUG_OILGEN_BASIC & log) {
                BCLog.logger.info("[energy.oilgen] Not generating oil in chunk " + cx + ", " + cz
                    + " because the biome we found (" + biomeName + ") is disabled!");
            }
            return ImmutableList.of();
        }

        if (biome.is(BiomeTags.IS_END) && (Math.abs(x) < 1200 || Math.abs(z) < 1200)) {
            if (DEBUG_OILGEN_BASIC & log) {
                BCLog.logger.info("[energy.oilgen] Not generating oil in chunk " + cx + ", " + cz
                    + " because it's the end biome and we're within 1200 blocks of the ender dragon fight");
            }
            return ImmutableList.of();
        }

        boolean oilBiome = biomeName != null && BCEnergyConfig.surfaceDepositBiomes.contains(biomeName);

        double bonus = oilBiome ? 3.0 : 1.0;
        bonus *= BCEnergyConfig.oilWellGenerationRate;
        if (biomeName != null && BCEnergyConfig.excessiveBiomes.contains(biomeName)) {
            bonus *= 30.0;
        }
        final GenType type;

        if (rand.nextDouble() <= BCEnergyConfig.largeOilGenProb * bonus) {
            // 0.04%
            type = GenType.LARGE;
        } else if (rand.nextDouble() <= BCEnergyConfig.mediumOilGenProb * bonus) {
            // 0.1%
            type = GenType.MEDIUM;
        } else if (oilBiome && rand.nextDouble() <= BCEnergyConfig.smallOilGenProb * bonus) {
            // 2%
            type = GenType.LAKE;
        } else {
            if (DEBUG_OILGEN_ALL & log) {
                BCLog.logger.info("[energy.oilgen] Not generating oil in chunk " + cx + ", " + cz
                    + " because none of the random numbers were above the thresholds for generation");
            }
            return ImmutableList.of();
        }
        if (DEBUG_OILGEN_BASIC & log) {
            BCLog.logger.info("[energy.oilgen] Generating an oil well (" + type.name().toLowerCase(Locale.ROOT)
                + ") in chunk " + cx + ", " + cz + " at " + x + ", " + z);
        }

        // 1.12.2 measured everything from y = 0 (the bedrock floor); 26.3 worlds start at their min y
        int floor = world.getMinY();
        int maxY = world.getMaxY();

        List<OilGenStructure> structures = new ArrayList<>();
        int lakeRadius;
        int tendrilRadius;
        if (type == GenType.LARGE) {
            lakeRadius = 4;
            tendrilRadius = 25 + rand.nextInt(20);
        } else if (type == GenType.LAKE) {
            lakeRadius = 6;
            tendrilRadius = 25 + rand.nextInt(20);
        } else {
            lakeRadius = 2;
            tendrilRadius = 5 + rand.nextInt(10);
        }
        structures.add(createTendril(new BlockPos(x, world.getSeaLevel(), z), floor + 1, maxY, lakeRadius,
            tendrilRadius, rand));

        if (type != GenType.LAKE) {
            // Generate a spherical cave deposit
            int wellY = floor + 20 + rand.nextInt(10);

            int radius;
            if (type == GenType.LARGE) {
                radius = 8 + rand.nextInt(9);
            } else {
                radius = 4 + rand.nextInt(4);
            }

            structures.add(createSphere(new BlockPos(x, wellY, z), radius));

            // Generate a spout
            if (BCEnergyConfig.enableOilSpouts) {
                int maxHeight, minHeight;

                if (type == GenType.LARGE) {
                    minHeight = BCEnergyConfig.largeSpoutMinHeight;
                    maxHeight = BCEnergyConfig.largeSpoutMaxHeight;
                    radius = 1;
                } else {
                    minHeight = BCEnergyConfig.smallSpoutMinHeight;
                    maxHeight = BCEnergyConfig.smallSpoutMaxHeight;
                    radius = 0;
                }
                final int height;
                if (maxHeight == minHeight) {
                    height = maxHeight;
                } else {
                    if (maxHeight < minHeight) {
                        int t = maxHeight;
                        maxHeight = minHeight;
                        minHeight = t;
                    }
                    height = minHeight + rand.nextInt(maxHeight - minHeight);
                }
                structures.add(new OilGenStructure.Spout(new BlockPos(x, wellY, z), maxY, ReplaceType.ALWAYS, radius,
                    height));
            }

            // Generate a spring at the very bottom
            if (type == GenType.LARGE) {
                structures.add(createTube(new BlockPos(x, floor + 1, z), wellY - floor, radius, Axis.Y));
                if (BCCoreBlocks.spring != null) {
                    structures.add(createSpring(new BlockPos(x, floor, z)));
                }
            }
        }
        return structures;
    }

    public static OilGenStructure createTubeY(BlockPos base, int height, int radius) {
        return createTube(base, height, radius, Axis.Y);
    }

    public static OilGenStructure createSpring(BlockPos at) {
        return new OilGenStructure.Spring(at);
    }

    public static OilGenStructure createTube(BlockPos center, int length, int radius, Axis axis) {
        int valForAxis = VecUtil.getValue(center, axis);
        BlockPos min = VecUtil.replaceValue(center.offset(-radius, -radius, -radius), axis, valForAxis);
        BlockPos max = VecUtil.replaceValue(center.offset(radius, radius, radius), axis, valForAxis + length);
        double radiusSq = radius * radius;
        int toReplace = valForAxis;
        Predicate<BlockPos> tester = p -> VecUtil.replaceValue(p, axis, toReplace).distSqr(center) <= radiusSq;
        return new GenByPredicate(new Box(min, max), ReplaceType.ALWAYS, tester);
    }

    public static OilGenStructure createSphere(BlockPos center, int radius) {
        Box box = new Box(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius));
        double radiusSq = radius * radius + 0.01;
        Predicate<BlockPos> tester = p -> p.distSqr(center) <= radiusSq;
        return new GenByPredicate(box, ReplaceType.ALWAYS, tester);
    }

    public static OilGenStructure createTendril(BlockPos center, int minY, int maxY, int lakeRadius, int radius,
        Random rand) {
        BlockPos start = center.offset(-radius, 0, -radius);
        int diameter = radius * 2 + 1;
        boolean[][] pattern = new boolean[diameter][diameter];

        int x = radius;
        int z = radius;
        for (int dx = -lakeRadius; dx <= lakeRadius; dx++) {
            for (int dz = -lakeRadius; dz <= lakeRadius; dz++) {
                pattern[x + dx][z + dz] = dx * dx + dz * dz <= lakeRadius * lakeRadius;
            }
        }

        for (int w = 1; w < radius; w++) {
            float proba = (float) (radius - w + 4) / (float) (radius + 4);

            fillPatternIfProba(rand, proba, x, z + w, pattern);
            fillPatternIfProba(rand, proba, x, z - w, pattern);
            fillPatternIfProba(rand, proba, x + w, z, pattern);
            fillPatternIfProba(rand, proba, x - w, z, pattern);

            for (int i = 1; i <= w; i++) {
                fillPatternIfProba(rand, proba, x + i, z + w, pattern);
                fillPatternIfProba(rand, proba, x + i, z - w, pattern);
                fillPatternIfProba(rand, proba, x + w, z + i, pattern);
                fillPatternIfProba(rand, proba, x - w, z + i, pattern);

                fillPatternIfProba(rand, proba, x - i, z + w, pattern);
                fillPatternIfProba(rand, proba, x - i, z - w, pattern);
                fillPatternIfProba(rand, proba, x + w, z - i, pattern);
                fillPatternIfProba(rand, proba, x - w, z - i, pattern);
            }
        }

        int depth = rand.nextDouble() < 0.5 ? 1 : 2;
        return OilGenStructure.PatternTerrainHeight.create(start, minY, maxY, ReplaceType.IS_FOR_LAKE, pattern, depth);
    }

    private static void fillPatternIfProba(Random rand, float proba, int x, int z, boolean[][] pattern) {
        if (rand.nextFloat() <= proba) {
            pattern[x][z] = isSet(pattern, x, z - 1) | isSet(pattern, x, z + 1) //
                | isSet(pattern, x - 1, z) | isSet(pattern, x + 1, z);
        }
    }

    private static boolean isSet(boolean[][] pattern, int x, int z) {
        if (x < 0 || x >= pattern.length) return false;
        if (z < 0 || z >= pattern[x].length) return false;
        return pattern[x][z];
    }
}
