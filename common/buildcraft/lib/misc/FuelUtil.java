/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.misc;

import java.util.Optional;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.SlotProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.common.loot.NeoForgeLootContextParams;

/** Furnace fuel for BuildCraft machines (1.12.2's TileEntityFurnace.getItemBurnTime). 26.3 keeps burn times in the
 * cooking_fuel item component, resolved through a loot context as a furnace does. */
public final class FuelUtil {
    private static final SlotProvider NO_SLOTS = slot -> null;

    private FuelUtil() {}

    /** @return The furnace burn time of the stack in ticks, or 0 if it isn't a fuel. */
    public static int getBurnTime(BlockEntity tile, ItemStack stack) {
        if (stack.isEmpty() || !stack.has(DataComponents.COOKING_FUEL)
            || !(tile.getLevel() instanceof ServerLevel level)) {
            return 0;
        }
        LootContext context = new LootContext.Builder(new LootParams.Builder(level)
            .withParameter(LootContextParams.BLOCK_STATE, tile.getBlockState())
            .withParameter(LootContextParams.BLOCK_ENTITY, tile)
            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(tile.getBlockPos()))
            .withParameter(LootContextParams.CONTAINER, NO_SLOTS)
            .withOptionalParameter(NeoForgeLootContextParams.QUERIED_STACK, stack)
            .create(LootContextParamSets.CONTAINER_PROCESS)).create(Optional.empty());
        return ResolvableInt.getFromItem(stack, DataComponents.COOKING_FUEL, CookingFuel::burnTime, context, 0);
    }

    /** @return What is left of the stack once burnt (a bucket for a lava bucket), or an empty stack. */
    public static ItemStack getRemainder(ItemStack stack) {
        ItemStackTemplate remainder = stack.getCraftingRemainder();
        return remainder == null ? ItemStack.EMPTY : remainder.create();
    }
}
