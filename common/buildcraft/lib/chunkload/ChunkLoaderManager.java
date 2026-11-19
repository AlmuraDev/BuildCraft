/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.chunkload;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import it.unimi.dsi.fastutil.longs.LongSet;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.common.world.chunk.TicketHelper;
import net.neoforged.neoforge.common.world.chunk.TicketSet;

import buildcraft.lib.BCLib;
import buildcraft.lib.BCLibConfig;
import buildcraft.lib.misc.data.WorldPos;

/** Keeps chunks loaded for BuildCraft tiles that want it (quarries, builders...), through NeoForge's ticket controller.
 * Tickets are owned by the tile's position and saved by NeoForge; when a world loads, tickets of tiles that are gone
 * or may no longer load chunks are dropped. */
public class ChunkLoaderManager {
    public static final TicketController CONTROLLER = new TicketController(
        Identifier.fromNamespaceAndPath(BCLib.MODID, "chunk_loader"), ChunkLoaderManager::validateTickets);

    /** The chunks each tile currently holds loaded. */
    private static final Map<WorldPos, Set<ChunkPos>> FORCED = new HashMap<>();

    /** Must be on buildcraftlib's mod event bus. */
    public static void onRegisterTicketControllers(RegisterTicketControllersEvent event) {
        event.register(CONTROLLER);
    }

    /**
     * This should be called when a tile entity that might be able to load chunks is loaded, or changes what it wants
     * loaded. A check is performed to see if the config allows it.
     */
    public static <T extends BlockEntity & IChunkLoadingTile> void loadChunksForTile(T tile) {
        if (!canLoadFor(tile)) {
            releaseChunksFor(tile);
            return;
        }
        updateChunksFor(tile);
    }

    public static <T extends BlockEntity & IChunkLoadingTile> void releaseChunksFor(T tile) {
        if (!(tile.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Set<ChunkPos> forced = FORCED.remove(new WorldPos(tile));
        if (forced != null) {
            for (ChunkPos pos : forced) {
                CONTROLLER.forceChunk(level, tile.getBlockPos(), pos.x(), pos.z(), false, true);
            }
        }
    }

    private static <T extends BlockEntity & IChunkLoadingTile> void updateChunksFor(T tile) {
        if (!(tile.getLevel() instanceof ServerLevel level)) {
            return;
        }
        WorldPos wPos = new WorldPos(tile);
        Set<ChunkPos> forced = FORCED.computeIfAbsent(wPos, k -> new HashSet<>());
        Set<ChunkPos> chunks = getChunksToLoad(tile);
        BlockPos owner = tile.getBlockPos();
        for (ChunkPos pos : new HashSet<>(forced)) {
            if (!chunks.contains(pos)) {
                CONTROLLER.forceChunk(level, owner, pos.x(), pos.z(), false, true);
                forced.remove(pos);
            }
        }
        for (ChunkPos pos : chunks) {
            if (forced.add(pos)) {
                CONTROLLER.forceChunk(level, owner, pos.x(), pos.z(), true, true);
            }
        }
    }

    public static <T extends BlockEntity & IChunkLoadingTile> Set<ChunkPos> getChunksToLoad(T tile) {
        Set<ChunkPos> chunksToLoad = tile.getChunksToLoad();
        Set<ChunkPos> chunkPoses = new HashSet<>(chunksToLoad != null ? chunksToLoad : Collections.emptyList());
        chunkPoses.add(ChunkPos.containing(tile.getBlockPos()));
        return chunkPoses;
    }

    /** Called by NeoForge when a world loads, with the tickets it saved. */
    private static void validateTickets(ServerLevel level, TicketHelper helper) {
        for (Map.Entry<BlockPos, TicketSet> entry : new HashMap<>(helper.getBlockTickets()).entrySet()) {
            BlockPos pos = entry.getKey();
            if (BCLibConfig.chunkLoadingLevel == BCLibConfig.ChunkLoaderLevel.NONE) {
                helper.removeAllTickets(pos);
                continue;
            }
            Set<ChunkPos> forced = FORCED.computeIfAbsent(new WorldPos(level, pos), k -> new HashSet<>());
            for (boolean ticking : new boolean[] { true, false }) {
                LongSet chunks = entry.getValue().getChunks(ticking);
                chunks.forEach((long chunk) -> forced.add(ChunkPos.unpack(chunk)));
            }
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean canLoadFor(IChunkLoadingTile tile) {
        return BCLibConfig.chunkLoadingLevel.canLoad(tile.getLoadType());
    }
}
