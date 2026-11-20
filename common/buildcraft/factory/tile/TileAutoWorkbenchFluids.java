/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.tile;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;

import net.minecraft.world.level.material.Fluid;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.tiles.IDebuggable;

import buildcraft.lib.fluid.Tank;
import buildcraft.lib.misc.CapUtil;

public class TileAutoWorkbenchFluids extends TileAutoWorkbenchBase implements IDebuggable {
    private final Tank tank1 = new Tank("tank1", net.neoforged.neoforge.fluids.FluidType.BUCKET_VOLUME * 6, this);
    private final Tank tank2 = new Tank("tank2", net.neoforged.neoforge.fluids.FluidType.BUCKET_VOLUME * 6, this);

    public TileAutoWorkbenchFluids(BlockPos pos, BlockState state) {
        super(pos, state, 2, 2);
        tankManager.addAll(tank1, tank2);
        caps.addCapabilityInstance(CapUtil.CAP_FLUIDS, tankManager, EnumPipePart.CENTER);
        caps.addCapabilityInstance(CapUtil.CAP_FLUIDS, tank1, EnumPipePart.DOWN, EnumPipePart.NORTH, EnumPipePart.WEST);
        caps.addCapabilityInstance(CapUtil.CAP_FLUIDS, tank2, EnumPipePart.UP, EnumPipePart.SOUTH, EnumPipePart.EAST);
    }

    @Override
    public void getDebugInfo(List<String> left, List<String> right, Direction side) {
        left.add("Tanks:");
        left.add("  " + tank1.getContentsString());
        left.add("  " + tank2.getContentsString());
    }
}
