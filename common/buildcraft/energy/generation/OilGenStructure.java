/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.energy.generation;

import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import buildcraft.api.core.BCLog;
import buildcraft.api.enums.EnumSpring;

import buildcraft.lib.BCLib;
import buildcraft.lib.misc.VecUtil;
import buildcraft.lib.misc.data.Box;

import buildcraft.core.BCCoreBlocks;
import buildcraft.core.block.BlockSpring;
import buildcraft.energy.BCEnergyFluids;
import buildcraft.energy.tile.TileSpringOil;

/** One part of an oil well (lake, tendrils, deposit sphere, spout, spring). Structures are worked out for a whole well
 * from its chunk's random, and each chunk that generates only places the parts inside its own box. */
public abstract class OilGenStructure {
    public final Box box;
    public final ReplaceType replaceType;

    public OilGenStructure(Box containingBox, ReplaceType replaceType) {
        this.box = containingBox;
        this.replaceType = replaceType;
    }

    public final void generate(WorldGenLevel world, Box within) {
        Box intersect = box.getIntersect(within);
        if (intersect != null) {
            generateWithin(world, intersect);
        }
    }

    /** Generates this structure in the world, but only between the given coordinates. */
    protected abstract void generateWithin(WorldGenLevel world, Box intersect);

    /** @return The number of oil blocks that this structure will set. Note that this is called *after*
     *         {@link #generateWithin(WorldGenLevel, Box)}, by the Spring type, so this can store the number set. */
    protected abstract int countOilBlocks();

    public void setOilIfCanReplace(WorldGenLevel world, BlockPos pos) {
        if (canReplaceForOil(world, pos)) {
            setOil(world, pos);
        }
    }

    public boolean canReplaceForOil(WorldGenLevel world, BlockPos pos) {
        return replaceType.canReplace(world, pos);
    }

    public static void setOil(WorldGenLevel world, BlockPos pos) {
        world.setBlock(pos, BCEnergyFluids.crudeOil[0].getBlock().defaultBlockState(), 2);
    }

    public enum ReplaceType {
        ALWAYS {
            @Override
            public boolean canReplace(WorldGenLevel world, BlockPos pos) {
                // 26.3: keep the bedrock floor (1.12.2's wells started above it at y = 0)
                return !world.getBlockState(pos).is(Blocks.BEDROCK);
            }
        },
        IS_FOR_LAKE {
            @Override
            public boolean canReplace(WorldGenLevel world, BlockPos pos) {
                return ALWAYS.canReplace(world, pos);
            }
        };

        public abstract boolean canReplace(WorldGenLevel world, BlockPos pos);
    }

    public static class GenByPredicate extends OilGenStructure {
        public final Predicate<BlockPos> predicate;

        public GenByPredicate(Box containingBox, ReplaceType replaceType, Predicate<BlockPos> predicate) {
            super(containingBox, replaceType);
            this.predicate = predicate;
        }

        @Override
        protected void generateWithin(WorldGenLevel world, Box intersect) {
            for (BlockPos pos : BlockPos.betweenClosed(intersect.min(), intersect.max())) {
                if (predicate.test(pos)) {
                    setOilIfCanReplace(world, pos);
                }
            }
        }

        @Override
        protected int countOilBlocks() {
            int count = 0;
            for (BlockPos pos : BlockPos.betweenClosed(box.min(), box.max())) {
                if (predicate.test(pos)) {
                    count++;
                }
            }
            return count;
        }
    }

    /** Oil lakes and tendrils on the surface, following the terrain height. */
    public static class PatternTerrainHeight extends OilGenStructure {
        private final boolean[][] pattern;
        private final int depth;

        private PatternTerrainHeight(Box containingBox, ReplaceType replaceType, boolean[][] pattern, int depth) {
            super(containingBox, replaceType);
            this.pattern = pattern;
            this.depth = depth;
        }

        public static PatternTerrainHeight create(BlockPos start, int minY, int maxY, ReplaceType replaceType,
            boolean[][] pattern, int depth) {
            BlockPos min = VecUtil.replaceValue(start, Axis.Y, minY);
            BlockPos max = new BlockPos(min.getX() + pattern.length - 1, maxY,
                min.getZ() + (pattern.length == 0 ? 0 : pattern[0].length - 1));
            return new PatternTerrainHeight(new Box(min, max), replaceType, pattern, depth);
        }

        @Override
        protected void generateWithin(WorldGenLevel world, Box intersect) {
            for (int x = intersect.min().getX(); x <= intersect.max().getX(); x++) {
                int px = x - box.min().getX();

                for (int z = intersect.min().getZ(); z <= intersect.max().getZ(); z++) {
                    int pz = z - box.min().getZ();

                    if (pattern[px][pz]) {
                        int top = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
                        BlockPos upper = new BlockPos(x, top - 1, z);
                        if (canReplaceForOil(world, upper)) {
                            for (int y = 0; y < 5; y++) {
                                world.removeBlock(upper.above(y), false);
                            }
                            for (int y = 0; y < depth; y++) {
                                setOilIfCanReplace(world, upper.below(y));
                            }
                        }
                    }
                }
            }
        }

        @Override
        protected int countOilBlocks() {
            int count = 0;
            for (boolean[] row : pattern) {
                for (boolean set : row) {
                    if (set) {
                        count++;
                    }
                }
            }
            return count * depth;
        }
    }

    /** The column of oil from the deposit up to the surface, and the fountain above it. */
    public static class Spout extends OilGenStructure {
        public final BlockPos start;
        public final int radius;
        public final int height;
        private int count = 0;

        public Spout(BlockPos start, int maxY, ReplaceType replaceType, int radius, int height) {
            // Only a 1 wide column, so only the chunk containing the start generates it
            super(new Box(start, VecUtil.replaceValue(start, Axis.Y, maxY)), replaceType);
            this.start = start;
            this.radius = radius;
            this.height = height;
        }

        @Override
        protected void generateWithin(WorldGenLevel world, Box intersect) {
            count = 0;
            int top = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, start.getX(), start.getZ());
            BlockPos worldTop = new BlockPos(start.getX(), top + 1, start.getZ());
            for (int y = top; y >= start.getY(); y--) {
                worldTop = worldTop.below();
                BlockState state = world.getBlockState(worldTop);
                if (state.isAir()) {
                    continue;
                }
                if (!state.getFluidState().isEmpty()) {
                    break;
                }
                if (state.isSolid()) {
                    break;
                }
            }
            OilGenStructure tubeY = OilGenerator.createTube(start, worldTop.getY() - start.getY(), radius, Axis.Y);
            tubeY.generate(world, tubeY.box);
            count += tubeY.countOilBlocks();
            BlockPos base = worldTop;
            for (int r = radius; r >= 0; r--) {
                OilGenStructure struct = OilGenerator.createTube(base, height, r, Axis.Y);
                struct.generate(world, struct.box);
                base = base.offset(0, height, 0);
                count += struct.countOilBlocks();
            }
        }

        @Override
        protected int countOilBlocks() {
            if (count == 0) {
                throw new IllegalStateException("Called countOilBlocks before calling generateWithin!");
            }
            return count;
        }
    }

    /** The oil spring at the bottom of a large well. */
    public static class Spring extends OilGenStructure {
        public final BlockPos pos;

        public Spring(BlockPos pos) {
            super(new Box(pos, pos), ReplaceType.ALWAYS);
            this.pos = pos;
        }

        @Override
        protected void generateWithin(WorldGenLevel world, Box intersect) {
            // NO-OP (this one is called separately)
        }

        @Override
        protected int countOilBlocks() {
            return 0;
        }

        public void generate(WorldGenLevel world, int count) {
            BlockState state = BCCoreBlocks.spring.defaultBlockState().setValue(BlockSpring.SPRING_TYPE, EnumSpring.OIL);
            world.setBlock(pos, state, 2);
            BlockEntity tile = world.getBlockEntity(pos);
            if (tile instanceof TileSpringOil spring) {
                spring.totalSources = count;
                spring.setChanged();
                if (BCLib.DEV) {
                    BCLog.logger.info("[energy.gen.oil] Generated an oil spring at " + pos + " (" + count + " sources)");
                }
            } else {
                BCLog.logger.warn("[energy.gen.oil] Setting the oil spring didn't also create its tile at " + pos);
            }
        }
    }
}
