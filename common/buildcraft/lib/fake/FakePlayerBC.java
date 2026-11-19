/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.fake;

import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import net.neoforged.neoforge.common.util.FakePlayer;

public class FakePlayerBC extends FakePlayer {
    public FakePlayerBC(ServerLevel world, GameProfile name) {
        super(world, name);
    }

    /** Moves the fake player to the given world and position (1.12.2 set its fields directly). */
    public void moveTo(ServerLevel world, BlockPos pos) {
        if (level() != world) {
            setLevel(world);
        }
        setPos(pos.getX(), pos.getY(), pos.getZ());
    }
}
