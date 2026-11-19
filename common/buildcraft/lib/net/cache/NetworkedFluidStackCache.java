/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.net.cache;

import java.io.IOException;
import java.util.Objects;

import net.minecraft.world.level.material.Fluid;
import buildcraft.api.core.NbtStacks;
import net.neoforged.neoforge.fluids.FluidStack;

import buildcraft.lib.net.PacketBufferBC;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap;

public class NetworkedFluidStackCache extends NetworkedObjectCache<FluidStack> {
    private static final int FLUID_AMOUNT = 1;

    public NetworkedFluidStackCache() {
        // Use water for our base stack as it might not be too bad of an assumption
        super(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, FLUID_AMOUNT));
    }

    @Override
    protected Object2IntMap<FluidStack> createObject2IntMap() {
        return new Object2IntOpenCustomHashMap<>(new Hash.Strategy<FluidStack>() {
            @Override
            public int hashCode(FluidStack o) {
                if (o == null) {
                    return 0;
                }
                return FluidStack.hashFluidAndComponents(o);
            }

            @Override
            public boolean equals(FluidStack a, FluidStack b) {
                if (a == null || b == null) {
                    return a == b;
                }
                return FluidStack.isSameFluidSameComponents(a, b);
            }
        });
    }

    @Override
    protected FluidStack copyOf(FluidStack object) {
        return object.copy();
    }

    @Override
    protected void writeObject(FluidStack obj, PacketBufferBC buffer) {
        buffer.writeNbt(NbtStacks.writeFluidStack(obj.copyWithAmount(FLUID_AMOUNT)));
    }

    @Override
    protected FluidStack readObject(PacketBufferBC buffer) throws IOException {
        FluidStack stack = NbtStacks.readFluidStack(buffer.readNbt());
        return stack == null ? new FluidStack(net.minecraft.world.level.material.Fluids.WATER, FLUID_AMOUNT) : stack;
    }

    @Override
    protected String getCacheName() {
        return "FluidStack";
    }
}
