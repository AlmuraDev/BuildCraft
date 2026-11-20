/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;


/** The tint of a facade's block, as the block itself would be tinted (grass, leaves...). Kept out of PluggableFacade so
 * the server never sees the client colour classes. */
public final class FacadeTints {
    private FacadeTints() {}

    public static int getColour(BlockState state, Level world, BlockPos pos, int tintIndex) {
        BlockTintSource source = Minecraft.getInstance().getBlockColors().getTintSource(state, tintIndex);
        if (source == null) {
            return -1;
        }
        if (world instanceof BlockAndTintGetter tintLevel) {
            return source.colorInWorld(state, tintLevel, pos);
        }
        return source.color(state);
    }
}
