package buildcraft.api.transfer;

import javax.annotation.Nonnull;

import net.minecraft.world.item.ItemStack;

/** An {@link IFluidHandler} for a fluid container item; filling or draining may change the container itself. */
public interface IFluidHandlerItem extends IFluidHandler {
    @Nonnull
    ItemStack getContainer();
}
