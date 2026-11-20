/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.client;

import java.util.function.Function;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;

import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import buildcraft.lib.gui.BCMenu;

/** Binds BuildCraft menus to their screens (1.12.2's IGuiHandler.getClientGuiElement). */
public final class BCMenuScreens {
    private BCMenuScreens() {}

    public static <C extends AbstractContainerMenu, S extends Screen & MenuAccess<C>> void register(
        RegisterMenuScreensEvent event, BCMenu<C> menu, Function<C, S> screen) {
        event.register(menu.type(), (MenuScreens.ScreenConstructor<C, S>) (container, inventory, title) -> screen
            .apply(container));
    }
}
