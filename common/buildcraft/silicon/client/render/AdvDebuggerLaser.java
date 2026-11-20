/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.client.render;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

import buildcraft.api.properties.BuildCraftProperties;

import buildcraft.lib.debug.DebugRenderHelper;
import buildcraft.lib.debug.IDetachedRenderer;
import buildcraft.lib.misc.VolumeUtil;

import buildcraft.silicon.BCSiliconBlocks;
import buildcraft.silicon.tile.TileLaser;

/** Shows which blocks a laser can see (green) or not (blue), for the advanced debugger. */
public class AdvDebuggerLaser implements IDetachedRenderer {
    private static final int COLOUR_VISIBLE = 0xFF_99_FF_99;
    private static final int COLOUR_NOT_VISIBLE = 0xFF_11_11_99;

    private final BlockPos pos;
    private final Direction face;

    public AdvDebuggerLaser(TileLaser tile) {
        pos = tile.getBlockPos();
        BlockState state = tile.getBlockState();
        face = state.getBlock() == BCSiliconBlocks.laser ? state.getValue(BuildCraftProperties.BLOCK_FACING_6) : null;
    }

    @Override
    public void render(Player player, float partialTicks) {
        if (pos == null || face == null) {
            return;
        }
        VolumeUtil.iterateCone(player.level(), pos, face, 6, true, (world, start, p, visible) -> {
            int colour = visible ? COLOUR_VISIBLE : COLOUR_NOT_VISIBLE;
            DebugRenderHelper.renderSmallCuboid(p, colour);
        });
    }
}
