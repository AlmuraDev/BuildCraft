/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.block;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;

import buildcraft.api.properties.BuildCraftProperties;


public class BlockBCBase_Neptune extends Block {
    public static final Property<Direction> PROP_FACING = BuildCraftProperties.BLOCK_FACING;
    public static final Property<Direction> BLOCK_FACING_6 = BuildCraftProperties.BLOCK_FACING_6;

    public final String id;

    /** The properties every BuildCraft block starts with: hardness 5, resistance 10, metal sounds. */
    public static BlockBehaviour.Properties defaultProperties() {
        return BlockBehaviour.Properties.of().strength(5.0F, 10.0F).sound(SoundType.METAL);
    }

    /** @param properties The block's properties. They must already carry the block's id (see
     *            {@link buildcraft.lib.registry.RegistrationHelper}, which creates BuildCraft's blocks).
     * @param id The block's tag id ("block.quarry"), or null for none. */
    public BlockBCBase_Neptune(BlockBehaviour.Properties properties, @Nullable String id) {
        super(properties);
        if (id == null) {
            id = "";
        }
        this.id = id;

        if (this instanceof IBlockWithFacing) {
            Property<Direction> facingProp = ((IBlockWithFacing) this).getFacingProperty();
            registerDefaultState(defaultBlockState().setValue(facingProp, Direction.NORTH));
        }
    }

    // BlockState

    /** Adds this block's state properties. Called from the {@link Block} constructor, so it can't use instance fields. */
    protected void addProperties(List<Property<?>> properties) {
        if (this instanceof IBlockWithFacing) {
            properties.add(((IBlockWithFacing) this).getFacingProperty());
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        List<Property<?>> properties = new ArrayList<>();
        addProperties(properties);
        builder.add(properties.toArray(new Property<?>[0]));
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rot) {
        if (this instanceof IBlockWithFacing) {
            Property<Direction> prop = ((IBlockWithFacing) this).getFacingProperty();
            Direction facing = state.getValue(prop);
            state = state.setValue(prop, rot.rotate(facing));
        }
        return state;
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        if (this instanceof IBlockWithFacing) {
            Property<Direction> prop = ((IBlockWithFacing) this).getFacingProperty();
            Direction facing = state.getValue(prop);
            state = state.setValue(prop, mirror.mirror(facing));
        }
        return state;
    }

    // Others

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state != null && this instanceof IBlockWithFacing) {
            Direction orientation = context.getHorizontalDirection();
            IBlockWithFacing b = (IBlockWithFacing) this;
            Player placer = context.getPlayer();
            BlockPos pos = context.getClickedPos();
            if (b.canFaceVertically() && placer != null) {
                if (Math.abs((float) placer.getX() - pos.getX()) < 2.0F
                    && Math.abs((float) placer.getZ() - pos.getZ()) < 2.0F) {
                    double y = placer.getY() + placer.getEyeHeight();

                    if (y - pos.getY() > 2.0D) {
                        orientation = Direction.DOWN;
                    }

                    if (pos.getY() - y > 0.0D) {
                        orientation = Direction.UP;
                    }
                }
            }
            state = state.setValue(b.getFacingProperty(), orientation.getOpposite());
        }
        return state;
    }

    /** Rotates the block about the given axis, as wrenches do (1.12.2's Block.rotateBlock). By default only rotates blocks
     * with a facing. */
    public boolean rotateBlock(Level world, BlockPos pos, Direction axis) {
        BlockState state = world.getBlockState(pos);
        if (this instanceof IBlockWithFacing) {
            if (!((IBlockWithFacing) this).canBeRotated(world, pos, state)) {
                return false;
            }
            return ((IBlockWithFacing) this).attemptRotation(world, pos, state, axis).consumesAction();
        }
        return false;
    }
}
