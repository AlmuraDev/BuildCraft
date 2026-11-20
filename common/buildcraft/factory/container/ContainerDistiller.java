/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.container;

import net.minecraft.world.inventory.MenuType;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import buildcraft.lib.gui.ContainerBCTile;
import buildcraft.lib.gui.widget.WidgetFluidTank;

import buildcraft.factory.tile.TileDistiller_BC8;

public class ContainerDistiller extends ContainerBCTile<TileDistiller_BC8> {
    public final WidgetFluidTank widgetInputTank;
    public final WidgetFluidTank widgetOutputGasTank;
    public final WidgetFluidTank widgetOutputLiquidTank;

    public ContainerDistiller(MenuType<?> type, int containerId, Player player, TileDistiller_BC8 tank) {
        super(type, containerId, player, tank);

        addFullPlayerInventory(79);

        widgetInputTank = addWidget(new WidgetFluidTank(this, tank.tankIn));
        widgetOutputGasTank = addWidget(new WidgetFluidTank(this, tank.tankGasOut));
        widgetOutputLiquidTank = addWidget(new WidgetFluidTank(this, tank.tankLiquidOut));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // The only slots are player slots -- try to interact with the tank

        if (!player.level().isClientSide()) {
            Slot slot = slots.get(index);
            ItemStack stack = slot.getItem();
            ItemStack original = stack.copy();
            stack = tile.tankIn.transferStackToTank(this, stack);
            if (!ItemStack.matches(stack, original)) {
                slot.set(stack);
                broadcastChanges();
                return ItemStack.EMPTY;
            }
        }

        return super.quickMoveStack(player, index);
    }
}
