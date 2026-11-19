/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.pipe;

import buildcraft.transport.item.ItemPipeHolder;
import java.util.List;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import buildcraft.api.core.InvalidInputDataException;
import buildcraft.api.schematics.ISchematicBlock;
import buildcraft.api.schematics.SchematicBlockContext;
import buildcraft.api.transport.pipe.PipeApi;
import buildcraft.api.transport.pipe.PipeDefinition;

import buildcraft.lib.misc.NBTUtilBC;

import buildcraft.transport.BCTransportBlocks;

public class SchematicBlockPipe implements ISchematicBlock {
    private CompoundTag tileNbt;
    private Rotation tileRotation = Rotation.NONE;

    public static boolean predicate(SchematicBlockContext context) {
        return context.world.getBlockState(context.pos).getBlock() == BCTransportBlocks.pipeHolder;
    }

    @Override
    public void init(SchematicBlockContext context) {
        BlockEntity tileEntity = context.world.getBlockEntity(context.pos);
        if (tileEntity == null) {
            throw new IllegalStateException();
        }
        tileNbt = tileEntity.saveWithFullMetadata(context.world.registryAccess());
    }

    @Nonnull
    @Override
    public List<ItemStack> computeRequiredItems() {
        try {
            ImmutableList.Builder<ItemStack> builder = ImmutableList.builder();
            PipeDefinition definition = PipeRegistry.INSTANCE.loadDefinition(
                tileNbt.getCompoundOrEmpty("pipe").getStringOr("def", "")
            );
            DyeColor color = NBTUtilBC.readEnum(
                tileNbt.getCompoundOrEmpty("pipe").get("col"),
                DyeColor.class
            );
            if (PipeApi.pipeRegistry.getItemForPipe(definition) instanceof ItemPipeHolder item) {
                builder.add(item.createStack(color));
            }
            return builder.build();
        } catch (InvalidInputDataException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public SchematicBlockPipe getRotated(Rotation rotation) {
        SchematicBlockPipe schematicBlock = new SchematicBlockPipe();
        schematicBlock.tileNbt = tileNbt;
        schematicBlock.tileRotation = tileRotation.getRotated(rotation);
        return schematicBlock;
    }

    @Override
    public boolean canBuild(Level world, BlockPos blockPos) {
        return world.isEmptyBlock(blockPos);
    }

    @SuppressWarnings("Duplicates")
    @Override
    public boolean build(Level world, BlockPos blockPos) {
        if (world.setBlock(blockPos, BCTransportBlocks.pipeHolder.defaultBlockState(), 11)) {
            BlockEntity tileEntity = BlockEntity.loadStatic(blockPos, BCTransportBlocks.pipeHolder.defaultBlockState(),
                tileNbt, world.registryAccess());
            if (tileEntity != null) {
                // Pipe holders don't rotate (1.12.2's TileEntity.rotate did nothing for them either)
                world.setBlockEntity(tileEntity);
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("Duplicates")
    @Override
    public boolean buildWithoutChecks(Level world, BlockPos blockPos) {
        if (world.setBlock(blockPos, BCTransportBlocks.pipeHolder.defaultBlockState(), 0)) {
            BlockEntity tileEntity = BlockEntity.loadStatic(blockPos, BCTransportBlocks.pipeHolder.defaultBlockState(),
                tileNbt, world.registryAccess());
            if (tileEntity != null) {
                // Pipe holders don't rotate (1.12.2's TileEntity.rotate did nothing for them either)
                world.setBlockEntity(tileEntity);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isBuilt(Level world, BlockPos blockPos) {
        return world.getBlockState(blockPos).getBlock() == BCTransportBlocks.pipeHolder;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.put("tileNbt", tileNbt);
        nbt.put("tileRotation", NBTUtilBC.writeEnum(tileRotation));
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) throws InvalidInputDataException {
        tileNbt = nbt.getCompoundOrEmpty("tileNbt");
        tileRotation = NBTUtilBC.readEnum(nbt.get("tileRotation"), Rotation.class);
    }
}
