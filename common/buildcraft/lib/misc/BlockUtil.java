/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.misc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.fluids.FluidStack;

import buildcraft.api.core.BuildCraftAPI;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.transfer.IFluidHandler;
import buildcraft.api.transfer.NeoTransfer;

import buildcraft.lib.BCLibConfig;
import buildcraft.lib.compat.CompatManager;
import buildcraft.lib.inventory.TransactorEntityItem;
import buildcraft.lib.inventory.filter.StackFilter;

import buildcraft.core.BCCoreConfig;

public final class BlockUtil {

    /** @return A list of itemstacks that are dropped from the block, or null if the block is air */
    @Nullable
    public static NonNullList<ItemStack> getItemStackFromBlock(ServerLevel world, BlockPos pos, GameProfile owner) {
        BlockState state = world.getBlockState(pos);
        if (state.isAir()) {
            return null;
        }

        Player fakePlayer = BuildCraftAPI.fakePlayerProvider.getFakePlayer(world, owner, pos);
        List<ItemStack> drops = Block.getDrops(state, world, pos, world.getBlockEntity(pos), fakePlayer, ItemStack.EMPTY);

        NonNullList<ItemStack> returnList = NonNullList.create();
        returnList.addAll(drops);
        return returnList;
    }

    public static boolean breakBlock(ServerLevel world, BlockPos pos, BlockPos ownerPos, GameProfile owner) {
        return breakBlock(world, pos, BCLibConfig.itemLifespan * 20, ownerPos, owner);
    }

    public static boolean breakBlock(ServerLevel world, BlockPos pos, int forcedLifespan, BlockPos ownerPos,
        GameProfile owner) {
        NonNullList<ItemStack> items = NonNullList.create();

        if (breakBlock(world, pos, items, ownerPos, owner)) {
            for (ItemStack item : items) {
                dropItem(world, pos, forcedLifespan, item);
            }
            return true;
        }
        return false;
    }

    /** @return true if the break isn't cancelled (by protection plugins etc). */
    private static boolean fireBreakEvent(ServerLevel world, BlockPos pos, Player player) {
        BreakBlockEvent event = CommonHooks.fireBlockBreak(world, GameType.SURVIVAL, player, pos, world.getBlockState(pos));
        return !event.isCanceled();
    }

    public static boolean harvestBlock(ServerLevel world, BlockPos pos, @Nonnull ItemStack tool, GameProfile owner) {
        FakePlayer fakePlayer = getFakePlayerWithTool(world, tool, owner);
        if (!fireBreakEvent(world, pos, fakePlayer)) {
            return false;
        }

        BlockState state = world.getBlockState(pos);

        if (!state.canHarvestBlock(world, pos, fakePlayer)) {
            return false;
        }

        // As vanilla's ServerPlayerGameMode.destroyBlock: the block drops its items (through playerDestroy) itself
        BlockEntity tile = world.getBlockEntity(pos);
        Block block = state.getBlock();
        BlockState destroyed = block.playerWillDestroy(world, pos, state, fakePlayer);
        if (world.removeBlock(pos, false)) {
            block.destroy(world, pos, destroyed);
            block.playerDestroy(world, fakePlayer, pos, destroyed, tile, tool);
        }
        return true;
    }

    public static boolean destroyBlock(ServerLevel world, BlockPos pos, @Nonnull ItemStack tool, GameProfile owner) {
        FakePlayer fakePlayer = getFakePlayerWithTool(world, tool, owner);
        if (!fireBreakEvent(world, pos, fakePlayer)) {
            return false;
        }

        world.destroyBlock(pos, true);

        return true;
    }

    public static FakePlayer getFakePlayerWithTool(ServerLevel world, @Nonnull ItemStack tool, GameProfile owner) {
        FakePlayer player = BuildCraftAPI.fakePlayerProvider.getFakePlayer(world, owner);
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        return player;
    }

    public static boolean breakBlock(ServerLevel world, BlockPos pos, NonNullList<ItemStack> drops, BlockPos ownerPos,
        GameProfile owner) {
        FakePlayer fakePlayer = BuildCraftAPI.fakePlayerProvider.getFakePlayer(world, owner, ownerPos);
        if (!fireBreakEvent(world, pos, fakePlayer)) {
            return false;
        }

        if (!world.isEmptyBlock(pos) && world.getGameRules().get(GameRules.BLOCK_DROPS)) {
            NonNullList<ItemStack> fromBlock = getItemStackFromBlock(world, pos, owner);
            if (fromBlock != null) {
                drops.addAll(fromBlock);
            }
        }
        world.removeBlock(pos, false);

        return true;
    }

    public static void dropItem(ServerLevel world, BlockPos pos, int forcedLifespan, ItemStack stack) {
        float var = 0.7F;
        double dx = world.getRandom().nextFloat() * var + (1.0F - var) * 0.5D;
        double dy = world.getRandom().nextFloat() * var + (1.0F - var) * 0.5D;
        double dz = world.getRandom().nextFloat() * var + (1.0F - var) * 0.5D;
        ItemEntity entityitem = new ItemEntity(world, pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz, stack);

        entityitem.lifespan = forcedLifespan;
        entityitem.setDefaultPickUpDelay();

        world.addFreshEntity(entityitem);
    }

    public static Optional<List<ItemStack>> breakBlockAndGetDrops(ServerLevel world, BlockPos pos,
        @Nonnull ItemStack tool, GameProfile owner) {
        return breakBlockAndGetDrops(world, pos, tool, owner, false);
    }

    /** @param grabAll If true then this will pickup every item in range of the position, false to only get the items
     *            that the dropped while breaking the block. */
    public static Optional<List<ItemStack>> breakBlockAndGetDrops(ServerLevel world, BlockPos pos,
        @Nonnull ItemStack tool, GameProfile owner, boolean grabAll) {
        AABB aabb = new AABB(pos).inflate(1);
        Set<Entity> entities;
        if (grabAll) {
            entities = Collections.emptySet();
        } else {
            entities = Sets.newIdentityHashSet();
            entities.addAll(world.getEntitiesOfClass(ItemEntity.class, aabb));
        }
        if (!harvestBlock(world, pos, tool, owner)) {
            if (!destroyBlock(world, pos, tool, owner)) {
                return Optional.empty();
            }
        }
        List<ItemStack> stacks = new ArrayList<>();
        for (ItemEntity entity : world.getEntitiesOfClass(ItemEntity.class, aabb)) {
            if (entities.contains(entity)) {
                continue;
            }
            TransactorEntityItem transactor = new TransactorEntityItem(entity);
            ItemStack stack;
            while (!(stack = transactor.extract(StackFilter.ALL, 0, Integer.MAX_VALUE, false)).isEmpty()) {
                stacks.add(stack);
            }
        }
        return Optional.of(stacks);
    }

    public static boolean canChangeBlock(Level world, BlockPos pos, GameProfile owner) {
        return canChangeBlock(world.getBlockState(pos), world, pos, owner);
    }

    public static boolean canChangeBlock(BlockState state, Level world, BlockPos pos, GameProfile owner) {
        if (state == null) return true;

        if (state.isAir()) {
            return true;
        }

        if (isUnbreakableBlock(world, pos, state, owner)) {
            return false;
        }

        FluidState fluid = state.getFluidState();
        if (fluid.is(FluidTags.LAVA)) {
            return false;
        } else if (!fluid.isEmpty() && fluid.getFluidType().getDensity() >= 3000) {
            return false;
        }

        return true;
    }

    public static float getBlockHardnessMining(Level world, BlockPos pos, BlockState state, GameProfile owner) {
        if (world instanceof ServerLevel) {
            Player fakePlayer = BuildCraftAPI.fakePlayerProvider.getFakePlayer((ServerLevel) world, owner);
            float relativeHardness = state.getDestroyProgress(fakePlayer, world, pos);
            if (relativeHardness <= 0.0F) {
                // The relative hardness is 0 if the block can't be broken at all.
                return -1.0F;
            }
        }
        return state.getDestroySpeed(world, pos);
    }

    public static boolean isUnbreakableBlock(Level world, BlockPos pos, BlockState state, GameProfile owner) {
        return getBlockHardnessMining(world, pos, state, owner) < 0;
    }

    public static boolean isUnbreakableBlock(Level world, BlockPos pos, GameProfile owner) {
        return isUnbreakableBlock(world, pos, world.getBlockState(pos), owner);
    }

    /** Returns true if a block cannot be harvested without a tool. */
    public static boolean isToughBlock(Level world, BlockPos pos) {
        return world.getBlockState(pos).requiresCorrectToolForDrops();
    }

    public static boolean isFullFluidBlock(Level world, BlockPos pos) {
        return isFullFluidBlock(world.getBlockState(pos), world, pos);
    }

    public static boolean isFullFluidBlock(BlockState state, Level world, BlockPos pos) {
        return state.getFluidState().isSource();
    }

    @Nullable
    public static Fluid getFluid(Level world, BlockPos pos) {
        FluidStack fluid = drainBlock(world, pos, false);
        return fluid != null ? fluid.getFluid() : null;
    }

    /** @return The fluid in the block at the given position, source or flowing (as its source fluid), or null. */
    @Nullable
    public static Fluid getFluidWithFlowing(Level world, BlockPos pos) {
        return getFluidWithFlowing(world.getBlockState(pos));
    }

    @Nullable
    public static Fluid getFluid(Block block) {
        FluidState fluid = block.defaultBlockState().getFluidState();
        return fluid.isEmpty() ? null : sourceOf(fluid.getType());
    }

    /** @return The fluid of the given state if it's a source block, else null. */
    @Nullable
    public static Fluid getFluidWithoutFlowing(BlockState state) {
        FluidState fluid = state.getFluidState();
        if (fluid.isEmpty() || !fluid.isSource()) {
            return null;
        }
        return sourceOf(fluid.getType());
    }

    @Nullable
    public static Fluid getFluidWithFlowing(BlockState state) {
        FluidState fluid = state.getFluidState();
        return fluid.isEmpty() ? null : sourceOf(fluid.getType());
    }

    @Nullable
    public static Fluid getFluidWithFlowing(Block block) {
        return getFluid(block);
    }

    private static Fluid sourceOf(Fluid fluid) {
        if (fluid instanceof net.minecraft.world.level.material.FlowingFluid flowing) {
            return flowing.getSource();
        }
        return fluid;
    }

    /** Drains a bucket's worth of fluid from the block at the given position: from its fluid handler if it has one,
     * otherwise from a source fluid block (which drains it to air).
     *
     * @return What was (or would be) drained, or null for nothing. */
    @Nullable
    public static FluidStack drainBlock(Level world, BlockPos pos, boolean doDrain) {
        IFluidHandler handler = NeoTransfer.fromNeoFluids(world.getCapability(Capabilities.Fluid.BLOCK, pos, null));
        if (handler != null) {
            return handler.drain(net.neoforged.neoforge.fluids.FluidType.BUCKET_VOLUME, doDrain);
        }
        BlockState state = world.getBlockState(pos);
        FluidState fluid = state.getFluidState();
        if (fluid.isEmpty() || !fluid.isSource() || fluid.getType() == Fluids.EMPTY) {
            return null;
        }
        FluidStack stack = new FluidStack(sourceOf(fluid.getType()), net.neoforged.neoforge.fluids.FluidType.BUCKET_VOLUME);
        if (doDrain) {
            if (state.getBlock() instanceof BucketPickup pickup) {
                pickup.pickupBlock(null, world, pos, state);
            } else {
                world.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        return stack;
    }

    /** Create an explosion which only affects a single block: breaks it (dropping its items) with the effects of an
     * explosion. */
    public static void explodeBlock(Level world, BlockPos pos) {
        if (!(world instanceof ServerLevel server)) {
            return;
        }

        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;

        server.destroyBlock(pos, true);
        server.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 1, 0, 0, 0, 0);
        server.playSound(null, pos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 4.0F,
            (1.0F + (server.getRandom().nextFloat() - server.getRandom().nextFloat()) * 0.2F) * 0.7F);
    }

    public static long computeBlockBreakPower(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        float hardness = state.getDestroySpeed(world, pos);
        return (long) Math.floor(16 * MjAPI.MJ * ((hardness + 1) * 2) * BCCoreConfig.miningMultiplier);
    }

    /** The following functions let you avoid unnecessary chunk loads, which is nice. */
    public static BlockEntity getTileEntity(Level world, BlockPos pos) {
        return getTileEntity(world, pos, false);
    }

    public static BlockEntity getTileEntity(Level world, BlockPos pos, boolean force) {
        return CompatManager.getTile(world, pos, force);
    }

    /** The 26.3 name for {@link #getTileEntity(Level, BlockPos, boolean)}. */
    public static BlockEntity getBlockEntity(Level world, BlockPos pos, boolean force) {
        return getTileEntity(world, pos, force);
    }

    public static BlockState getBlockState(Level world, BlockPos pos) {
        return getBlockState(world, pos, false);
    }

    public static BlockState getBlockState(Level world, BlockPos pos, boolean force) {
        return CompatManager.getState(world, pos, force);
    }

    public static boolean useItemOnBlock(Level world, Player player, ItemStack stack, BlockPos pos,
        Direction direction) {
        Vec3 hit = Vec3.atCenterOf(pos);
        UseOnContext context = new UseOnContext(world, player, InteractionHand.MAIN_HAND, stack,
            new BlockHitResult(hit, direction, pos, false));
        boolean done = stack.onItemUseFirst(context).consumesAction();

        if (!done) {
            done = stack.useOn(context).consumesAction();
        }
        return done;
    }

    public static void onComparatorUpdate(Level world, BlockPos pos, Block block) {
        world.updateNeighbourForOutputSignal(pos, block);
    }

    @Nullable
    public static ChestBlockEntity getOtherDoubleChest(BlockEntity inv) {
        if (inv instanceof ChestBlockEntity chest && chest.getLevel() != null) {
            BlockState state = chest.getBlockState();
            if (!state.hasProperty(ChestBlock.TYPE) || state.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
                return null;
            }
            BlockPos other = chest.getBlockPos().relative(ChestBlock.getConnectedDirection(state));
            if (chest.getLevel().getBlockEntity(other) instanceof ChestBlockEntity adjacent) {
                return adjacent;
            }
        }
        return null;
    }

    public static <T extends Comparable<T>> BlockState copyProperty(Property<T> property, BlockState dst,
        BlockState src) {
        return dst.getProperties().contains(property) ? dst.setValue(property, src.getValue(property)) : dst;
    }

    public static <T extends Comparable<T>> int compareProperty(Property<T> property, BlockState a, BlockState b) {
        return a.getValue(property).compareTo(b.getValue(property));
    }

    public static <T extends Comparable<T>> String getPropertyStringValue(BlockState blockState,
        Property<T> property) {
        return property.getName(blockState.getValue(property));
    }

    public static Map<String, String> getPropertiesStringMap(BlockState blockState,
        Collection<Property<?>> properties) {
        ImmutableMap.Builder<String, String> mapBuilder = new ImmutableMap.Builder<>();
        for (Property<?> property : properties) {
            mapBuilder.put(property.getName(), getPropertyStringValue(blockState, property));
        }
        return mapBuilder.build();
    }

    public static Map<String, String> getPropertiesStringMap(BlockState blockState) {
        return getPropertiesStringMap(blockState, blockState.getProperties());
    }

    public static Comparator<BlockState> blockStateComparator() {
        return (blockStateA, blockStateB) -> {
            Block blockA = blockStateA.getBlock();
            Block blockB = blockStateB.getBlock();
            if (blockA != blockB) {
                return BuiltInRegistries.BLOCK.getKey(blockA).toString().compareTo(BuiltInRegistries.BLOCK.getKey(blockB).toString());
            }
            for (Property<?> property : Sets.intersection(new HashSet<>(blockStateA.getProperties()),
                new HashSet<>(blockStateB.getProperties()))) {
                int compareResult = BlockUtil.compareProperty(property, blockStateA, blockStateB);
                if (compareResult != 0) {
                    return compareResult;
                }
            }
            return 0;
        };
    }

    public static boolean blockStatesWithoutBlockEqual(BlockState a, BlockState b,
        Collection<Property<?>> ignoredProperties) {
        return Sets.intersection(new HashSet<>(a.getProperties()), new HashSet<>(b.getProperties())).stream()
            .filter(property -> !ignoredProperties.contains(property))
            .allMatch(property -> Objects.equals(a.getValue(property), b.getValue(property)));
    }

    public static boolean blockStatesWithoutBlockEqual(BlockState a, BlockState b) {
        return Sets.intersection(new HashSet<>(a.getProperties()), new HashSet<>(b.getProperties())).stream()
            .allMatch(property -> Objects.equals(a.getValue(property), b.getValue(property)));
    }

    public static boolean blockStatesEqual(BlockState a, BlockState b, Collection<Property<?>> ignoredProperties) {
        return a.getBlock() == b.getBlock()
            && Sets.intersection(new HashSet<>(a.getProperties()), new HashSet<>(b.getProperties())).stream()
                .filter(property -> !ignoredProperties.contains(property))
                .allMatch(property -> Objects.equals(a.getValue(property), b.getValue(property)));
    }

    public static boolean blockStatesEqual(BlockState a, BlockState b) {
        return a.getBlock() == b.getBlock()
            && Sets.intersection(new HashSet<>(a.getProperties()), new HashSet<>(b.getProperties())).stream()
                .allMatch(property -> Objects.equals(a.getValue(property), b.getValue(property)));
    }

    public static Comparator<BlockPos> uniqueBlockPosComparator(Comparator<BlockPos> parent) {
        return (a, b) -> {
            int parentValue = parent.compare(a, b);
            if (parentValue != 0) {
                return parentValue;
            } else if (a.getX() != b.getX()) {
                return Integer.compare(a.getX(), b.getX());
            } else if (a.getY() != b.getY()) {
                return Integer.compare(a.getY(), b.getY());
            } else if (a.getZ() != b.getZ()) {
                return Integer.compare(a.getZ(), b.getZ());
            } else {
                return 0;
            }
        };
    }
}
