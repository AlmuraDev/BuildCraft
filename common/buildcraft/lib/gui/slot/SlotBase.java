/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.gui.slot;

import javax.annotation.Nonnull;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import buildcraft.api.transfer.IItemHandlerModifiable;

import buildcraft.lib.tile.item.IItemHandlerAdv;
import buildcraft.lib.tile.item.ItemHandlerSimple;

/** A slot backed by one of BuildCraft's item handlers (1.12.2 Forge's SlotItemHandler, for BuildCraft's handler
 * interfaces). */
public class SlotBase extends Slot {
    private static final Container EMPTY_INVENTORY = new SimpleContainer(0);

    public final int handlerIndex;
    public final IItemHandlerAdv itemHandler;

    public SlotBase(IItemHandlerAdv itemHandler, int slotIndex, int posX, int posY) {
        super(EMPTY_INVENTORY, slotIndex, posX, posY);
        this.handlerIndex = slotIndex;
        this.itemHandler = itemHandler;
    }

    public IItemHandlerAdv getItemHandler() {
        return itemHandler;
    }

    public boolean canShift() {
        return true;
    }

    @Override
    public boolean mayPlace(@Nonnull ItemStack stack) {
        return !stack.isEmpty() && itemHandler.canSet(handlerIndex, stack);
    }

    @Override
    public ItemStack getItem() {
        return itemHandler.getStackInSlot(handlerIndex);
    }

    @Override
    public void set(ItemStack stack) {
        if (itemHandler instanceof IItemHandlerModifiable) {
            ((IItemHandlerModifiable) itemHandler).setStackInSlot(handlerIndex, stack);
        }
        setChanged();
    }

    @Override
    public int getMaxStackSize() {
        return itemHandler.getSlotLimit(handlerIndex);
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return Math.min(getMaxStackSize(), stack.getMaxStackSize());
    }

    @Override
    public boolean mayPickup(Player player) {
        return !itemHandler.extractItem(handlerIndex, 1, true).isEmpty();
    }

    @Override
    public ItemStack remove(int amount) {
        return itemHandler.extractItem(handlerIndex, amount, false);
    }

    /** @return The left over. */
    public ItemStack insert(ItemStack stack, boolean simulate) {
        return getItemHandler().insertItem(handlerIndex, stack, simulate);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (itemHandler instanceof ItemHandlerSimple) {
            ((ItemHandlerSimple) itemHandler).setStackInSlot(handlerIndex, getItem());
        }
    }
}
