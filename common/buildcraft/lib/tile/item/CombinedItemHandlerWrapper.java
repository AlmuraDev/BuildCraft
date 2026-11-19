package buildcraft.lib.tile.item;

import javax.annotation.Nonnull;

import net.minecraft.world.item.ItemStack;

import buildcraft.api.inventory.IItemHandlerFiltered;
import buildcraft.api.transfer.IItemHandlerModifiable;

/** Combines several {@link IItemHandlerModifiable} into one, slot ranges laid end to end (1.12.2 Forge's
 * CombinedInvWrapper, which NeoForge 26.3 no longer has for BuildCraft's handler interfaces).
 * <p>
 * Also provides {@link IItemHandlerFiltered#getFilter(int)} if the wrapped handlers support it. */
public class CombinedItemHandlerWrapper implements IItemHandlerModifiable, IItemHandlerFiltered {

    private final IItemHandlerModifiable[] handlers;
    /** The first combined slot of each handler, plus the total slot count at the end. */
    private final int[] baseIndex;

    public CombinedItemHandlerWrapper(IItemHandlerModifiable... handlers) {
        this.handlers = handlers;
        this.baseIndex = new int[handlers.length + 1];
        int index = 0;
        for (int i = 0; i < handlers.length; i++) {
            baseIndex[i] = index;
            index += handlers[i].getSlots();
        }
        baseIndex[handlers.length] = index;
    }

    /** @return The index of the handler holding the combined slot, or -1 if the slot is out of range. */
    protected int getIndexForSlot(int slot) {
        if (slot < 0) {
            return -1;
        }
        for (int i = 0; i < handlers.length; i++) {
            if (slot < baseIndex[i + 1]) {
                return i;
            }
        }
        return -1;
    }

    protected int getSlotFromIndex(int slot, int index) {
        return slot - baseIndex[index];
    }

    @Override
    public int getSlots() {
        return baseIndex[handlers.length];
    }

    @Nonnull
    @Override
    public ItemStack getStackInSlot(int slot) {
        int index = getIndexForSlot(slot);
        return index < 0 ? ItemStack.EMPTY : handlers[index].getStackInSlot(getSlotFromIndex(slot, index));
    }

    @Override
    public void setStackInSlot(int slot, @Nonnull ItemStack stack) {
        int index = getIndexForSlot(slot);
        if (index >= 0) {
            handlers[index].setStackInSlot(getSlotFromIndex(slot, index), stack);
        }
    }

    @Nonnull
    @Override
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        int index = getIndexForSlot(slot);
        return index < 0 ? stack : handlers[index].insertItem(getSlotFromIndex(slot, index), stack, simulate);
    }

    @Nonnull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        int index = getIndexForSlot(slot);
        return index < 0 ? ItemStack.EMPTY : handlers[index].extractItem(getSlotFromIndex(slot, index), amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        int index = getIndexForSlot(slot);
        return index < 0 ? 0 : handlers[index].getSlotLimit(getSlotFromIndex(slot, index));
    }

    @Override
    public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
        int index = getIndexForSlot(slot);
        return index >= 0 && handlers[index].isItemValid(getSlotFromIndex(slot, index), stack);
    }

    @Override
    public ItemStack getFilter(int slot) {
        int index = getIndexForSlot(slot);
        if (index < 0) {
            return ItemStack.EMPTY;
        }
        IItemHandlerModifiable handler = handlers[index];
        slot = getSlotFromIndex(slot, index);
        if (handler instanceof IItemHandlerFiltered) {
            return ((IItemHandlerFiltered) handler).getFilter(slot);
        }
        return handler.getStackInSlot(slot);
    }
}
