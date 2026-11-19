/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.world;

import javax.annotation.Nonnull;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Listens for block changes in a world (1.12.2's IWorldEventListener, of which BuildCraft only used block updates).
 * Register with {@link WorldEventListeners}. */
public class WorldEventListenerAdapter {
    /** Called after a block in the world changed and told its neighbours. 26.3 doesn't say what was there before, so
     * oldState is the new state too. */
    public void sendBlockUpdated(@Nonnull Level world, @Nonnull BlockPos pos, @Nonnull BlockState oldState,
        @Nonnull BlockState newState, int flags) {}
}
