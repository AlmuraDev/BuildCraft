package buildcraft.api.transfer;

import javax.annotation.Nullable;

import net.neoforged.neoforge.fluids.FluidStack;

/** What one tank of an {@link IFluidHandler} holds and accepts. */
public interface IFluidTankProperties {

    /** @return A copy of the contents, or null if the tank is empty. */
    @Nullable
    FluidStack getContents();

    int getCapacity();

    boolean canFill();

    boolean canDrain();

    boolean canFillFluidType(FluidStack fluidStack);

    boolean canDrainFluidType(FluidStack fluidStack);
}
