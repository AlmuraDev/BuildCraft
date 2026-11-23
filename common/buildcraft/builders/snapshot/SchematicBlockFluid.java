/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.snapshot;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

import buildcraft.api.core.InvalidInputDataException;
import buildcraft.api.schematics.ISchematicBlock;
import buildcraft.api.schematics.SchematicBlockContext;

import buildcraft.lib.misc.BlockUtil;

public class SchematicBlockFluid implements ISchematicBlock {
    private BlockState blockState;
    private boolean isFlowing;

    @SuppressWarnings("unused")
    public static boolean predicate(SchematicBlockContext context) {
        return BlockUtil.getFluidWithFlowing(context.world, context.pos) != null &&
            (BlockUtil.getFluid(context.world, context.pos) == null ||
                BlockUtil.getFluidWithoutFlowing(context.world.getBlockState(context.pos)) != null);
    }

    @Override
    public void init(SchematicBlockContext context) {
        blockState = context.blockState;
        isFlowing = BlockUtil.getFluid(context.world, context.pos) == null;
    }

    @Nonnull
    @Override
    public Set<BlockPos> getRequiredBlockOffsets() {
        return Stream.concat(Direction.Plane.HORIZONTAL.stream(), Stream.of(Direction.DOWN))
            .map(BlockPos.ZERO::relative)
            .collect(Collectors.toSet());
    }

    @Nonnull
    @Override
    public List<FluidStack> computeRequiredFluids() {
        return Optional.ofNullable(BlockUtil.getFluidWithoutFlowing(blockState))
            .map(fluid -> new FluidStack(fluid, net.neoforged.neoforge.fluids.FluidType.BUCKET_VOLUME))
            .map(Collections::singletonList)
            .orElseGet(Collections::emptyList);
    }

    @Override
    public SchematicBlockFluid getRotated(Rotation rotation) {
        SchematicBlockFluid schematicBlock = SchematicBlockManager.createCleanCopy(this);
        schematicBlock.blockState = blockState;
        schematicBlock.isFlowing = isFlowing;
        return schematicBlock;
    }

    @Override
    public boolean canBuild(Level world, BlockPos blockPos) {
        return world.isEmptyBlock(blockPos) ||
            BlockUtil.getFluidWithFlowing(world, blockPos) == BlockUtil.getFluidWithFlowing(blockState.getBlock()) &&
                BlockUtil.getFluid(world, blockPos) == null;
    }

    @Override
    public boolean build(Level world, BlockPos blockPos) {
        if (isFlowing) {
            return true;
        }
        if (world.setBlock(blockPos, blockState, 11)) {
            Stream.concat(
                Stream.of(Direction.values())
                    .map(BlockPos.ZERO::relative),
                Stream.of(BlockPos.ZERO)
            )
                .map(blockPos::offset)
                .forEach(updatePos -> world.updateNeighborsAt(updatePos, blockState.getBlock()));
            return true;
        }
        return false;
    }

    @Override
    public boolean buildWithoutChecks(Level world, BlockPos blockPos) {
        return world.setBlock(blockPos, blockState, 0);
    }

    @Override
    public boolean isBuilt(Level world, BlockPos blockPos) {
        return isFlowing || BlockUtil.blockStatesEqual(blockState, world.getBlockState(blockPos));
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.put("blockState", NbtUtils.writeBlockState(blockState));
        nbt.putBoolean("isFlowing", isFlowing);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) throws InvalidInputDataException {
        blockState = NbtUtils.readBlockState(net.minecraft.core.registries.BuiltInRegistries.BLOCK,
            buildcraft.lib.misc.LegacyBlockStates.upgrade(nbt.getCompoundOrEmpty("blockState")));
        isFlowing = nbt.getBooleanOr("isFlowing", false);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SchematicBlockFluid that = (SchematicBlockFluid) o;

        return isFlowing == that.isFlowing && blockState.equals(that.blockState);
    }

    @Override
    public int hashCode() {
        int result = blockState.hashCode();
        result = 31 * result + (isFlowing ? 1 : 0);
        return result;
    }

    /** The state the GUI previews draw (client only reads it). */
    public BlockState getPreviewState() {
        return blockState;
    }
}
