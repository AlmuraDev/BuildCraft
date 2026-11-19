package buildcraft.api.transfer;

import javax.annotation.Nullable;

import net.neoforged.neoforge.fluids.FluidStack;

/** Something holding fluid that can be filled and drained, like Forge 1.12's IFluidHandler. A null {@link FluidStack} means
 * "no fluid" wherever this returns one. */
public interface IFluidHandler {

    IFluidTankProperties[] getTankProperties();

    /** @return How much of the resource was (or would be) filled. */
    int fill(FluidStack resource, boolean doFill);

    /** Drains the given fluid, up to its amount.
     *
     * @return What was (or would be) drained, or null for nothing. */
    @Nullable
    FluidStack drain(FluidStack resource, boolean doDrain);

    /** Drains any fluid, up to the given amount.
     *
     * @return What was (or would be) drained, or null for nothing. */
    @Nullable
    FluidStack drain(int maxDrain, boolean doDrain);
}
