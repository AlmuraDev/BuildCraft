package buildcraft.api.transfer;

import javax.annotation.Nullable;

import net.neoforged.neoforge.fluids.FluidStack;

/** A fixed {@link IFluidTankProperties}. */
public class FluidTankProperties implements IFluidTankProperties {
    public static final FluidTankProperties[] EMPTY = new FluidTankProperties[0];

    @Nullable
    private final FluidStack contents;
    private final int capacity;
    private final boolean canFill;
    private final boolean canDrain;

    public FluidTankProperties(@Nullable FluidStack contents, int capacity) {
        this(contents, capacity, true, true);
    }

    public FluidTankProperties(@Nullable FluidStack contents, int capacity, boolean canFill, boolean canDrain) {
        this.contents = contents == null || contents.isEmpty() ? null : contents;
        this.capacity = capacity;
        this.canFill = canFill;
        this.canDrain = canDrain;
    }

    @Override
    @Nullable
    public FluidStack getContents() {
        return contents == null ? null : contents.copy();
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    @Override
    public boolean canFill() {
        return canFill;
    }

    @Override
    public boolean canDrain() {
        return canDrain;
    }

    @Override
    public boolean canFillFluidType(FluidStack fluidStack) {
        return canFill;
    }

    @Override
    public boolean canDrainFluidType(FluidStack fluidStack) {
        return canDrain;
    }
}
