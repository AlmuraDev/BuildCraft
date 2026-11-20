/*
 * Copyright (c) 2016 SpaceToad and the BuildCraft team
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package buildcraft.lib.fluid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ForwardingList;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.NonNullList;
import net.minecraft.core.BlockPos;

import buildcraft.api.core.INBTSerializable;
import net.neoforged.neoforge.fluids.FluidStack;
import buildcraft.api.transfer.IFluidTankProperties;
import net.neoforged.fml.LogicalSide;

import buildcraft.api.core.IFluidFilter;
import buildcraft.api.core.IFluidHandlerAdv;
import buildcraft.api.items.FluidItemDrops;

import buildcraft.lib.misc.FluidUtilBC;
import buildcraft.lib.net.PacketBufferBC;

/** Provides a simple way to save+load and send+receive data for any number of tanks. This also attempts to fill all of
 * the tanks one by one via the {@link #fill(FluidStack, boolean)} and {@link #drain(FluidStack, boolean)} methods. */
public class TankManager extends ForwardingList<Tank> implements IFluidHandlerAdv, INBTSerializable<CompoundTag> {

    private final List<Tank> tanks = new ArrayList<>();

    public TankManager() {}

    public TankManager(Tank... tanks) {
        addAll(Arrays.asList(tanks));
    }

    @Override
    protected List<Tank> delegate() {
        return tanks;
    }

    public void addAll(Tank... values) {
        Collections.addAll(this, values);
    }

    public void addDrops(NonNullList<ItemStack> toDrop) {
        FluidItemDrops.addFluidDrops(toDrop, toArray(new Tank[0]));
    }

    public boolean onActivated(Player player, BlockPos pos, InteractionHand hand) {
        return FluidUtilBC.onTankActivated(player, pos, hand, this);
    }

    private List<Tank> getFillOrderTanks() {
        List<Tank> list = new ArrayList<>();
        for (Tank t : tanks) {
            if (t.canFill() && !t.canDrain()) {
                list.add(t);
            }
        }
        for (Tank t : tanks) {
            if (t.canFill() && t.canDrain()) {
                list.add(t);
            }
        }
        return list;
    }

    private List<Tank> getDrainOrderTanks() {
        List<Tank> list = new ArrayList<>();
        for (Tank t : tanks) {
            if (!t.canFill() && t.canDrain()) {
                list.add(t);
            }
        }
        for (Tank t : tanks) {
            if (t.canFill() && t.canDrain()) {
                list.add(t);
            }
        }
        return list;
    }

    @Override
    public int fill(FluidStack resource, boolean doFill) {
        int filled = 0;
        for (Tank tank : getFillOrderTanks()) {
            int used = tank.fill(resource, doFill);
            if (used > 0) {
                resource = resource.copy();
                resource.shrink(used);
                filled += used;
                if (resource.getAmount() <= 0) {
                    return filled;
                }
            }
        }
        return filled;
    }

    @Override
    public FluidStack drain(FluidStack resource, boolean doDrain) {
        if (resource == null) {
            return null;
        }
        FluidStack draining = (resource).copyWithAmount(0);
        int left = resource.getAmount();
        for (Tank tank : getDrainOrderTanks()) {
            if (!FluidUtilBC.isFluidEqual(draining, tank.getFluid())) {
                continue;
            }
            FluidStack drained = tank.drain(left, doDrain);
            if (drained != null && drained.getAmount() > 0) {
                draining.grow(drained.getAmount());
                left -= drained.getAmount();
            }
        }
        return draining.getAmount() <= 0 ? null : draining;
    }

    @Override
    public FluidStack drain(int maxDrain, boolean doDrain) {
        FluidStack draining = null;
        for (Tank tank : getDrainOrderTanks()) {
            if (draining == null) {
                FluidStack drained = tank.drain(maxDrain, doDrain);
                if (drained != null && drained.getAmount() > 0) {
                    draining = drained;
                    maxDrain -= drained.getAmount();
                }
            } else if (FluidUtilBC.isFluidEqual(draining, tank.getFluid())) {
                FluidStack drained = tank.drain(maxDrain, doDrain);
                if (drained != null && drained.getAmount() > 0) {
                    draining.grow(drained.getAmount());
                    maxDrain -= drained.getAmount();
                }
            }
        }
        return draining;
    }

    @Override
    public FluidStack drain(IFluidFilter filter, int maxDrain, boolean doDrain) {
        if (filter == null) {
            return null;
        }
        FluidStack draining = null;
        for (Tank tank : getDrainOrderTanks()) {
            if (!filter.matches(tank.getFluid())) {
                continue;
            }
            if (draining == null) {
                FluidStack drained = tank.drain(maxDrain, doDrain);
                if (drained != null && drained.getAmount() > 0) {
                    draining = drained;
                    maxDrain -= drained.getAmount();
                }
            } else if (FluidUtilBC.isFluidEqual(draining, tank.getFluid())) {
                FluidStack drained = tank.drain(maxDrain, doDrain);
                if (drained != null && drained.getAmount() > 0) {
                    draining.grow(drained.getAmount());
                    maxDrain -= drained.getAmount();
                }
            }
        }
        return draining;
    }

    @Override
    public IFluidTankProperties[] getTankProperties() {
        IFluidTankProperties[] info = new IFluidTankProperties[size()];
        for (int i = 0; i < size(); i++) {
            info[i] = get(i).getTankProperties()[0];
        }
        return info;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        for (Tank t : tanks) {
            nbt.put(t.getTankName(), t.serializeNBT());
        }
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        for (Tank t : tanks) {
            t.readFromNBT(nbt.getCompoundOrEmpty(t.getTankName()));
        }
    }

    public void writeData(PacketBufferBC buffer) {
        for (Tank tank : tanks) {
            tank.writeToBuffer(buffer);
        }
    }

    public void readData(PacketBufferBC buffer) {
        for (Tank tank : tanks) {
            tank.readFromBuffer(buffer);
        }
    }
}
