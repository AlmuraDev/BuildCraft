/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** A hit on one part of a pipe holder (1.12.2 kept this in RayTraceResult.subHit): 0 is the pipe's centre, 1-6 its
 * connections, 7-12 the pluggables, then the wire parts and the wires between them (see
 * {@link BlockPipeHolder#getPartSideHit}). */
public class PipeHitResult extends BlockHitResult {
    public final int subHit;

    public PipeHitResult(Vec3 location, Direction direction, BlockPos pos, int subHit) {
        super(location, direction, pos, false);
        this.subHit = subHit;
    }
}
