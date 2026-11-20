/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.item;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import buildcraft.lib.item.ItemBC_Neptune;
import buildcraft.lib.misc.LocaleUtil;
import buildcraft.lib.misc.NBTUtilBC;

/** Copies a gate's settings to paste onto another gate. The copied data is kept in the stack's custom data under
 * "gate_data", as on 1.12.2; the item model shows "full" while the stack has custom data. */
public class ItemGateCopier extends ItemBC_Neptune {
    private static final String NBT_DATA = "gate_data";

    public ItemGateCopier(Item.Properties properties, String id) {
        super(properties.stacksTo(1), id);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable Level world, List<String> tooltip, TooltipFlag flag) {
        if (getCopiedGateData(stack) != null) {
            tooltip.add(LocaleUtil.localize("buildcraft.item.nonclean.usage"));
        }
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (world.isClientSide()) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            return clearData(stack);
        }
        return InteractionResult.PASS;
    }

    private InteractionResult clearData(@Nonnull ItemStack stack) {
        if (getCopiedGateData(stack) == null) {
            return InteractionResult.PASS;
        }
        CompoundTag nbt = NBTUtilBC.getItemData(stack);
        nbt.remove(NBT_DATA);
        NBTUtilBC.setItemData(stack, nbt.isEmpty() ? null : nbt);
        return InteractionResult.SUCCESS;
    }

    @Nullable
    public static CompoundTag getCopiedGateData(ItemStack stack) {
        CompoundTag nbt = NBTUtilBC.getItemData(stack);
        return nbt.contains(NBT_DATA) ? nbt.getCompoundOrEmpty(NBT_DATA) : null;
    }

    public static void setCopiedGateData(ItemStack stack, CompoundTag nbt) {
        NBTUtilBC.updateItemData(stack, data -> data.put(NBT_DATA, nbt));
    }
}
