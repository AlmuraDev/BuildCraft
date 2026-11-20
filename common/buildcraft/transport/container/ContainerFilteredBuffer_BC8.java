/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.transport.container;

import net.minecraft.world.inventory.MenuType;

import net.minecraft.world.entity.player.Player;

import buildcraft.lib.gui.ContainerBCTile;
import buildcraft.lib.gui.slot.SlotBase;
import buildcraft.lib.gui.slot.SlotPhantom;

import buildcraft.transport.tile.TileFilteredBuffer;

public class ContainerFilteredBuffer_BC8 extends ContainerBCTile<TileFilteredBuffer> {
    public ContainerFilteredBuffer_BC8(MenuType<?> type, int containerId, Player player, TileFilteredBuffer tile) {
        super(type, containerId, player, tile);
        addFullPlayerInventory(86);

        for (int i = 0; i < 9; i++) {
            // Filtered Buffer filter slots
            // (1.12.2 gave these an empty-slot background sprite; GuiFilteredBuffer draws it)
            addSlot(new SlotPhantom(tile.invFilter, i, 8 + i * 18, 27) {
                @Override
                public boolean canAdjustCount() {
                    return false;
                }
            });
            // Filtered Buffer inventory slots
            addSlot(new SlotBase(tile.invMain, i, 8 + i * 18, 61));
        }
    }
}
