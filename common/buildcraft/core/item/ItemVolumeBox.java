/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core.item;

import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import buildcraft.lib.item.ItemBC_Neptune;

import buildcraft.core.marker.volume.VolumeBox;
import buildcraft.core.marker.volume.WorldSavedDataVolumeBoxes;

public class ItemVolumeBox extends ItemBC_Neptune {
    public ItemVolumeBox(Item.Properties properties, String id) {
        super(properties, id);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        InteractionHand hand = context.getHand();
        Direction facing = context.getClickedFace();
        if (world.isClientSide()) {
            return InteractionResult.PASS;
        }

        BlockPos offset = pos.relative(facing);

        WorldSavedDataVolumeBoxes volumeBoxes = WorldSavedDataVolumeBoxes.get(world);
        VolumeBox current = volumeBoxes.getVolumeBoxAt(offset);

        if (current == null) {
            volumeBoxes.addVolumeBox(offset);
            volumeBoxes.markDirtyAndSync();
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.FAIL;
    }
}
