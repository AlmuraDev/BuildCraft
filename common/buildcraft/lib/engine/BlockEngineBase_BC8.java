/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.engine;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.redstone.Orientation;

import buildcraft.api.blocks.ICustomRotationHandler;
import buildcraft.api.core.IEngineType;

import net.minecraft.util.StringRepresentable;

import buildcraft.lib.block.BlockBCTile_Neptune;
import buildcraft.lib.registry.TileTypes;

/** A block holding engines of several types (wood, stone, iron...), each with its own tile. The type is a state property,
 * so one block serves every engine as on 1.12.2; engine items carry their type in the block state component. Engine
 * types are added with {@link buildcraft.lib.registry.RegistrationHelper#registerEngineTile}, possibly from other
 * modules. */
public abstract class BlockEngineBase_BC8<E extends Enum<E> & IEngineType & StringRepresentable> extends BlockBCTile_Neptune
    implements ICustomRotationHandler, TileTypes.IStateTileTypes {
    private final Map<E, BlockEntityType<?>> engineTileTypes = new EnumMap<>(getEngineProperty().getValueClass());

    public BlockEngineBase_BC8(BlockBehaviour.Properties properties, String id) {
        super(properties.noOcclusion(), id);
    }

    // Engine directly related methods

    /** Called by the registration helper once the engine type's tile is registered. */
    public void bindEngine(E type, BlockEntityType<?> tileType) {
        engineTileTypes.put(type, tileType);
    }

    public boolean isRegistered(E type) {
        return engineTileTypes.containsKey(type);
    }

    /** The engine type comes from the stack. 26.3's BlockItem places the default state (wood) and only applies the
     * stack's block_state component afterwards, by which time the wood engine's tile already exists. */
    @Override
    @Nullable
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }
        return state.setValue(getEngineProperty(), getType(context.getItemInHand()));
    }

    @Override
    @Nullable
    public BlockEntityType<?> getTileType(BlockState state) {
        return engineTileTypes.get(state.getValue(getEngineProperty()));
    }

    @Nonnull
    public ItemStack getStack(E type) {
        ItemStack stack = new ItemStack(this);
        stack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(getEngineProperty(), type));
        return stack;
    }

    /** @return The engine type a stack of this block places. */
    public E getType(ItemStack stack) {
        BlockItemStateProperties props = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY);
        return props.apply(defaultBlockState()).getValue(getEngineProperty());
    }

    /** Adds a stack of every registered engine type (for the creative tab). */
    public void addSubStacks(NonNullList<ItemStack> list) {
        for (E engine : getEngineProperty().getPossibleValues()) {
            if (engineTileTypes.containsKey(engine)) {
                list.add(getStack(engine));
            }
        }
    }

    public abstract EnumProperty<E> getEngineProperty();

    public abstract String getUnlocalizedName(E engine);

    // BlockState

    @Override
    protected void addProperties(List<Property<?>> properties) {
        super.addProperties(properties);
        properties.add(getEngineProperty());
    }

    // Misc Block Overrides

    /** Engines are drawn by their tile renderer, which animates them. */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, @Nullable Orientation orientation,
        boolean movedByPiston) {
        super.neighborChanged(state, world, pos, block, orientation, movedByPiston);
        if (world.isClientSide()) return;
        BlockEntity tile = world.getBlockEntity(pos);
        if (tile instanceof TileEngineBase_BC8) {
            TileEngineBase_BC8 engine = (TileEngineBase_BC8) tile;
            engine.rotateIfInvalid();
        }
    }

    // ICustomRotationHandler

    @Override
    public InteractionResult attemptRotation(Level world, BlockPos pos, BlockState state, Direction sideWrenched) {
        BlockEntity tile = world.getBlockEntity(pos);
        if (tile instanceof TileEngineBase_BC8) {
            TileEngineBase_BC8 engine = (TileEngineBase_BC8) tile;
            return engine.attemptRotation();
        }
        return InteractionResult.FAIL;
    }
}
