package buildcraft.api.transfer;

import javax.annotation.Nonnull;

import net.minecraft.world.item.ItemStack;

/** An {@link IItemHandler} whose slots can be set directly. */
public interface IItemHandlerModifiable extends IItemHandler {
    void setStackInSlot(int slot, @Nonnull ItemStack stack);
}
