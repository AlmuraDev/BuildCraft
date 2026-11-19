package buildcraft.api.transfer;

import javax.annotation.Nullable;

import net.neoforged.neoforge.fluids.FluidStack;

/** A single tank, like Forge 1.12's IFluidTank. */
public interface IFluidTank {

    /** @return The fluid in the tank, or null if it's empty. */
    @Nullable
    FluidStack getFluid();

    int getFluidAmount();

    int getCapacity();

    int fill(FluidStack resource, boolean doFill);

    @Nullable
    FluidStack drain(int maxDrain, boolean doDrain);
}
