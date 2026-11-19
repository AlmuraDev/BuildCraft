/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.gui;

import javax.annotation.Nullable;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;

import buildcraft.lib.tile.TileBC_Neptune;

public abstract class ContainerBCTile<T extends TileBC_Neptune> extends ContainerBC_Neptune {
    public final T tile;

    public ContainerBCTile(@Nullable MenuType<?> type, int containerId, Player player, T tile) {
        super(type, containerId, player);
        this.tile = tile;
        if (!tile.getWorld().isClientSide()) {
            tile.onPlayerOpen(player);
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        tile.onPlayerClose(player);
    }

    @Override
    public final boolean stillValid(Player player) {
        return tile.canInteractWith(player);
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        tile.sendNetworkGuiTick(this.player);
    }
}
