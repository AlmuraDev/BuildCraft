/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.gui;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import buildcraft.api.core.BCLog;

/** One BuildCraft GUI as a 26.3 menu type (1.12.2 opened GUIs by number through an IGuiHandler with the block's
 * position). Opening a menu sends the position and one extra int ("data": a side for gates, 0 otherwise) to the client,
 * where the same factory builds the client copy of the container from them.
 *
 * @param <C> The container */
public final class BCMenu<C extends AbstractContainerMenu> {
    /** Builds the container on either side. Returning null (the tile is gone, or is the wrong type) opens nothing. */
    @FunctionalInterface
    public interface Factory<C extends AbstractContainerMenu> {
        @Nullable
        C create(MenuType<C> type, int containerId, Player player, BlockPos pos, int data);
    }

    /** A factory for containers of one tile class. */
    @FunctionalInterface
    public interface TileFactory<T, C extends AbstractContainerMenu> {
        C create(MenuType<C> type, int containerId, Player player, T tile);
    }

    private final String name;
    private final Factory<C> factory;
    private final DeferredHolder<MenuType<?>, MenuType<C>> holder;

    private BCMenu(DeferredRegister<MenuType<?>> register, String name, Factory<C> factory) {
        this.name = name;
        this.factory = factory;
        this.holder = register.register(name, () -> IMenuTypeExtension.create((id, inventory, buffer) -> {
            BlockPos pos = buffer.readBlockPos();
            int data = buffer.readVarInt();
            C container = factory.create(type(), id, inventory.player, pos, data);
            if (container == null) {
                throw new IllegalStateException("The " + name + " gui at " + pos + " couldn't be opened on the client");
            }
            return container;
        }));
    }

    public static DeferredRegister<MenuType<?>> createRegister(String modId, IEventBus modBus) {
        DeferredRegister<MenuType<?>> register = DeferredRegister.create(Registries.MENU, modId);
        register.register(modBus);
        return register;
    }

    public static <C extends AbstractContainerMenu> BCMenu<C> register(DeferredRegister<MenuType<?>> register,
        String name, Factory<C> factory) {
        return new BCMenu<>(register, name, factory);
    }

    /** Registers a menu for the tile at the opened position (the 1.12.2 "tile instanceof X" check). */
    public static <T, C extends AbstractContainerMenu> BCMenu<C> tile(DeferredRegister<MenuType<?>> register,
        String name, Class<T> tileClass, TileFactory<T, C> factory) {
        return new BCMenu<>(register, name, (type, id, player, pos, data) -> {
            BlockEntity tile = player.level().getBlockEntity(pos);
            if (tileClass.isInstance(tile)) {
                return factory.create(type, id, player, tileClass.cast(tile));
            }
            return null;
        });
    }

    public MenuType<C> type() {
        return holder.get();
    }

    public Supplier<MenuType<C>> supplier() {
        return holder;
    }

    public void open(Player player, BlockPos pos) {
        open(player, pos, 0);
    }

    /** Opens the menu for the player (server side; does nothing on the client). */
    public void open(Player player, BlockPos pos, int data) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        SimpleMenuProvider provider = new SimpleMenuProvider(
            (id, inventory, p) -> factory.create(type(), id, p, pos, data), Component.empty());
        if (serverPlayer.openMenu(provider, buffer -> {
            buffer.writeBlockPos(pos);
            buffer.writeVarInt(data);
        }).isEmpty()) {
            BCLog.logger.debug("[lib.gui] Couldn't open the " + name + " gui at " + pos);
        }
    }
}
