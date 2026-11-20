/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.block;

import net.minecraft.world.level.block.state.BlockBehaviour;

import buildcraft.lib.block.BlockBCTile_Neptune;
import buildcraft.lib.block.IBlockWithFacing;

public class BlockLaser extends BlockBCTile_Neptune implements IBlockWithFacing {
    public BlockLaser(BlockBehaviour.Properties properties, String id) {
        super(properties.noOcclusion(), id);
    }

    @Override
    public boolean canFaceVertically() {
        return true;
    }
}
