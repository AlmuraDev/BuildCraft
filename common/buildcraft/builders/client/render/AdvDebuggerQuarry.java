/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.client.render;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import buildcraft.lib.chunkload.ChunkLoaderManager;
import buildcraft.lib.debug.DebugRenderHelper;
import buildcraft.lib.debug.IDetachedRenderer;

import buildcraft.builders.tile.TileQuarry;

/** Shows the chunks a quarry keeps loaded, for the advanced debugger. */
public class AdvDebuggerQuarry implements IDetachedRenderer {
    private static final int COLOUR_CHUNK = 0x55_99_FF_99;

    private final WeakReference<TileQuarry> tileReference;

    public AdvDebuggerQuarry(TileQuarry tile) {
        tileReference = new WeakReference<>(tile);
    }

    @Override
    public void render(Player player, float partialTicks) {
        TileQuarry tile = tileReference.get();
        if (tile == null || !tile.frameBox.isInitialized()) {
            return;
        }
        Vec3 eye = player.getEyePosition(partialTicks);
        List<ChunkPos> chunkPoses = new ArrayList<>(ChunkLoaderManager.getChunksToLoad(tile));
        chunkPoses.sort(Comparator.comparingDouble(chunkPos -> -eye.distanceTo(new Vec3(
            chunkPos.getMinBlockX() + 0.5 + (chunkPos.getMaxBlockX() - chunkPos.getMinBlockX()) / 2, eye.y,
            chunkPos.getMinBlockZ() + 0.5 + (chunkPos.getMaxBlockZ() - chunkPos.getMinBlockZ()) / 2))));
        for (ChunkPos chunkPos : chunkPoses) {
            DebugRenderHelper.renderAABB(new AABB(chunkPos.getMinBlockX() + 0.5D, tile.frameBox.min().getY() + 0.5D,
                chunkPos.getMinBlockZ() + 0.5D, chunkPos.getMaxBlockX() + 0.5D, tile.frameBox.max().getY() + 0.5D,
                chunkPos.getMaxBlockZ() + 0.5D), COLOUR_CHUNK);
        }
    }
}
