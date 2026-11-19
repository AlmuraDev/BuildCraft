/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.fluid;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.neoforge.fluids.FluidStack;

import buildcraft.api.core.NbtStacks;
import buildcraft.api.transfer.FluidTankProperties;
import buildcraft.api.transfer.IFluidHandler;
import buildcraft.api.transfer.IFluidTank;
import buildcraft.api.transfer.IFluidTankProperties;

/** A single fluid tank, as Forge 1.12's FluidTank was (which BuildCraft's tanks were built on). A null fluid means the tank
 * is empty. Saves as {"fluid": &lt;26.3 fluid stack&gt;}, and reads 1.12.2's form ({FluidName, Amount, Tag}) too. */
public class FluidTank implements IFluidTank, IFluidHandler {
    @Nullable
    protected FluidStack fluid;
    protected int capacity;
    @Nullable
    protected BlockEntity tile;
    protected boolean canFill = true;
    protected boolean canDrain = true;
    protected IFluidTankProperties[] tankProperties;

    public FluidTank(int capacity) {
        this(null, capacity);
    }

    public FluidTank(@Nullable FluidStack fluidStack, int capacity) {
        this.fluid = fluidStack == null || fluidStack.isEmpty() ? null : fluidStack;
        this.capacity = capacity;
    }

    public FluidTank readFromNBT(CompoundTag nbt) {
        FluidStack read;
        if (nbt.contains("fluid")) {
            read = NbtStacks.readFluidStack(nbt.getCompoundOrEmpty("fluid"));
        } else if (nbt.contains("FluidName")) {
            read = NbtStacks.readFluidStack(nbt);
        } else {
            read = null;
        }
        setFluid(read);
        return this;
    }

    public CompoundTag writeToNBT(CompoundTag nbt) {
        if (fluid != null && !fluid.isEmpty()) {
            nbt.put("fluid", NbtStacks.writeFluidStack(fluid));
        } else {
            nbt.putString("Empty", "");
        }
        return nbt;
    }

    @Override
    public IFluidTankProperties[] getTankProperties() {
        if (this.tankProperties == null) {
            this.tankProperties = new IFluidTankProperties[] { new IFluidTankProperties() {
                @Override
                @Nullable
                public FluidStack getContents() {
                    return fluid == null ? null : fluid.copy();
                }

                @Override
                public int getCapacity() {
                    return FluidTank.this.getCapacity();
                }

                @Override
                public boolean canFill() {
                    return FluidTank.this.canFill();
                }

                @Override
                public boolean canDrain() {
                    return FluidTank.this.canDrain();
                }

                @Override
                public boolean canFillFluidType(FluidStack fluidStack) {
                    return FluidTank.this.canFillFluidType(fluidStack);
                }

                @Override
                public boolean canDrainFluidType(FluidStack fluidStack) {
                    return FluidTank.this.canDrainFluidType(fluidStack);
                }
            } };
        }
        return this.tankProperties;
    }

    @Override
    @Nullable
    public FluidStack getFluid() {
        return fluid;
    }

    public void setFluid(@Nullable FluidStack fluid) {
        this.fluid = fluid == null || fluid.isEmpty() ? null : fluid;
    }

    @Override
    public int getFluidAmount() {
        return fluid == null ? 0 : fluid.getAmount();
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public void setTileEntity(@Nullable BlockEntity tile) {
        this.tile = tile;
    }

    public boolean canFill() {
        return canFill;
    }

    public boolean canDrain() {
        return canDrain;
    }

    public void setCanFill(boolean canFill) {
        this.canFill = canFill;
    }

    public void setCanDrain(boolean canDrain) {
        this.canDrain = canDrain;
    }

    public boolean canFillFluidType(FluidStack fluid) {
        return canFill();
    }

    public boolean canDrainFluidType(@Nullable FluidStack fluid) {
        return fluid != null && canDrain();
    }

    @Override
    public int fill(FluidStack resource, boolean doFill) {
        if (!canFillFluidType(resource)) {
            return 0;
        }
        return fillInternal(resource, doFill);
    }

    /** Fills ignoring {@link #canFill()} (for the tile itself). */
    public int fillInternal(@Nullable FluidStack resource, boolean doFill) {
        if (resource == null || resource.isEmpty()) {
            return 0;
        }

        if (!doFill) {
            if (fluid == null) {
                return Math.min(capacity, resource.getAmount());
            }
            if (!FluidStack.isSameFluidSameComponents(fluid, resource)) {
                return 0;
            }
            return Math.min(capacity - fluid.getAmount(), resource.getAmount());
        }

        if (fluid == null) {
            fluid = resource.copyWithAmount(Math.min(capacity, resource.getAmount()));
            onContentsChanged();
            return fluid.getAmount();
        }

        if (!FluidStack.isSameFluidSameComponents(fluid, resource)) {
            return 0;
        }
        int filled = capacity - fluid.getAmount();

        if (resource.getAmount() < filled) {
            fluid.grow(resource.getAmount());
            filled = resource.getAmount();
        } else {
            fluid.setAmount(capacity);
        }

        if (filled > 0) {
            onContentsChanged();
        }
        return filled;
    }

    @Override
    @Nullable
    public FluidStack drain(FluidStack resource, boolean doDrain) {
        if (!canDrainFluidType(getFluid())) {
            return null;
        }
        return drainInternal(resource, doDrain);
    }

    @Override
    @Nullable
    public FluidStack drain(int maxDrain, boolean doDrain) {
        if (!canDrainFluidType(fluid)) {
            return null;
        }
        return drainInternal(maxDrain, doDrain);
    }

    @Nullable
    public FluidStack drainInternal(@Nullable FluidStack resource, boolean doDrain) {
        if (resource == null || resource.isEmpty() || fluid == null || !FluidStack.isSameFluidSameComponents(resource, fluid)) {
            return null;
        }
        return drainInternal(resource.getAmount(), doDrain);
    }

    @Nullable
    public FluidStack drainInternal(int maxDrain, boolean doDrain) {
        if (fluid == null || maxDrain <= 0) {
            return null;
        }

        int drained = maxDrain;
        if (fluid.getAmount() < drained) {
            drained = fluid.getAmount();
        }

        FluidStack stack = fluid.copyWithAmount(drained);
        if (doDrain) {
            fluid.shrink(drained);
            if (fluid.getAmount() <= 0) {
                fluid = null;
            }

            onContentsChanged();
        }
        return stack;
    }

    protected void onContentsChanged() {}
}
