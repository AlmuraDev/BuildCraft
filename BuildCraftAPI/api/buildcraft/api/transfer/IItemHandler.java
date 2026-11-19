package buildcraft.api.transfer;

import javax.annotation.Nonnull;

import net.minecraft.world.item.ItemStack;

/** A slot based inventory, like Forge 1.12's IItemHandler. Stacks returned from {@link #getStackInSlot(int)} must not be
 * modified. */
public interface IItemHandler {

    int getSlots();

    @Nonnull
    ItemStack getStackInSlot(int slot);

    /** Inserts a stack into the given slot.
     *
     * @return The remainder that couldn't be inserted (empty if all of it was), never the passed stack modified. */
    @Nonnull
    ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate);

    /** Extracts up to the given amount from the given slot.
     *
     * @return The extracted stack (empty if nothing was). */
    @Nonnull
    ItemStack extractItem(int slot, int amount, boolean simulate);

    int getSlotLimit(int slot);

    /** @return true if the given stack would be allowed in the slot, ignoring what's in it now. */
    default boolean isItemValid(int slot, @Nonnull ItemStack stack) {
        return true;
    }
}
