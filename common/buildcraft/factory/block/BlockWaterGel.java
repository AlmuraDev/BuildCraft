/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.block;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;

import buildcraft.lib.block.BlockBCBase_Neptune;
import buildcraft.lib.misc.SoundUtil;

/** Water gel: spreads through still water and sets into gel. Its drops (gelled water, 1-2 while spreading) are in its
 * loot table (data/buildcraftfactory/loot_table/blocks/water_gel.json). */
public class BlockWaterGel extends BlockBCBase_Neptune {
    public enum GelStage implements StringRepresentable {
        SPREAD_0(0.3f, true, 3),
        SPREAD_1(0.4f, true, 3),
        SPREAD_2(0.6f, true, 3),
        SPREAD_3(0.8f, true, 3),
        GELLING_0(1.0f, false, 0.6f),
        GELLING_1(1.2f, false, 0.6f),
        GEL(1.5f, false, 0.1f);

        public static final GelStage[] VALUES = values();

        public final SoundType soundType;
        public final String modelName = name().toLowerCase(Locale.ROOT);
        public final boolean spreading;
        public final float hardness;

        GelStage(float pitch, boolean spreading, float hardness) {
            this.soundType = new SoundType(//
                SoundType.SLIME_BLOCK.getVolume(), //
                pitch, //
                SoundEvents.SLIME_BLOCK_BREAK, //
                SoundEvents.SLIME_BLOCK_STEP, //
                SoundEvents.SLIME_BLOCK_PLACE, //
                SoundEvents.SLIME_BLOCK_HIT, //
                SoundEvents.SLIME_BLOCK_FALL//
            );
            this.spreading = spreading;
            this.hardness = hardness;
        }

        @Override
        public String getSerializedName() {
            return getName();
        }

        public String getName() {
            return modelName;
        }

        public GelStage next() {
            if (this == SPREAD_0) return SPREAD_1;
            if (this == SPREAD_1) return SPREAD_2;
            if (this == SPREAD_2) return SPREAD_3;
            if (this == SPREAD_3) return GELLING_0;
            if (this == GELLING_0) return GELLING_1;
            return GEL;
        }
    }

    public static final EnumProperty<GelStage> PROP_STAGE = EnumProperty.create("stage", GelStage.class);

    public BlockWaterGel(BlockBehaviour.Properties properties, String id) {
        super(properties.sound(SoundType.SLIME_BLOCK).strength(0.1f), id);
    }

    // BlockState

    @Override
    protected void addProperties(List<Property<?>> properties) {
        super.addProperties(properties);
        properties.add(PROP_STAGE);
    }

    // Logic

    @Override
    protected void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource rand) {
        GelStage stage = state.getValue(PROP_STAGE);
        GelStage next = stage.next();
        BlockState nextState = state.setValue(PROP_STAGE, next);
        if (stage.spreading) {
            Deque<BlockPos> openQueue = new ArrayDeque<>();
            Set<BlockPos> seenSet = new HashSet<>();
            List<BlockPos> changeable = new ArrayList<>();
            List<Direction> faces = new ArrayList<>();
            Collections.addAll(faces, Direction.values());
            Collections.shuffle(faces);
            seenSet.add(pos);
            for (Direction face : faces) {
                openQueue.add(pos.relative(face));
            }
            Collections.shuffle(faces);
            int tries = 0;

            while (openQueue.size() > 0 && changeable.size() < 3 && tries < 10_000) {
                BlockPos test = openQueue.removeFirst();

                boolean water = isWater(world, test);
                boolean spreadable = water || canSpread(world, test);

                if (water && world.getBlockState(test).getValue(LiquidBlock.LEVEL) == 0) {
                    changeable.add(test);
                }
                if (spreadable) {
                    Collections.shuffle(faces);
                    for (Direction face : faces) {
                        BlockPos n = test.relative(face);
                        if (seenSet.add(n)) {
                            openQueue.add(n);
                        }
                    }
                }
                tries++;
            }
            final int time = next.spreading ? 200 : 400;
            if (changeable.size() == 3 || world.getRandom().nextDouble() < 0.5) {
                for (BlockPos p : changeable) {
                    world.setBlockAndUpdate(p, nextState);
                    world.scheduleTick(p, this, rand.nextInt(150) + time);
                }
                world.setBlockAndUpdate(pos, nextState);
                SoundUtil.playBlockPlace(world, pos);
            }
            world.scheduleTick(pos, this, rand.nextInt(150) + time);
        } else if (stage != next) {
            if (notTouchingWater(world, pos)) {
                world.setBlockAndUpdate(pos, nextState);
                world.scheduleTick(pos, this, rand.nextInt(150) + 400);
            } else {
                world.scheduleTick(pos, this, rand.nextInt(150) + 600);
            }
        }
    }

    private static boolean notTouchingWater(Level world, BlockPos pos) {
        for (Direction face : Direction.values()) {
            if (isWater(world, pos.relative(face))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isWater(Level world, BlockPos pos) {
        return world.getBlockState(pos).getBlock() == Blocks.WATER;
    }

    private boolean canSpread(Level world, BlockPos pos) {
        return world.getBlockState(pos).getBlock() == this;
    }

    // Misc

    @Override
    public SoundType getSoundType(BlockState state, LevelReader world, BlockPos pos, @Nullable Entity entity) {
        return state.getValue(PROP_STAGE).soundType;
    }

    /** Each stage has its own hardness (1.12.2's getBlockHardness). */
    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter world, BlockPos pos) {
        float hardness = state.getValue(PROP_STAGE).hardness;
        int modifier = net.neoforged.neoforge.event.EventHooks.doPlayerHarvestCheck(player, state, world, pos) ? 30 : 100;
        return player.getDestroySpeed(state, pos) / hardness / modifier;
    }
}
