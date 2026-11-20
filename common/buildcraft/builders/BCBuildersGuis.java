/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

import net.neoforged.bus.api.IEventBus;

import buildcraft.lib.gui.BCMenu;

import buildcraft.builders.container.ContainerArchitectTable;
import buildcraft.builders.container.ContainerBuilder;
import buildcraft.builders.container.ContainerElectronicLibrary;
import buildcraft.builders.container.ContainerFiller;
import buildcraft.builders.container.ContainerFillerPlanner;
import buildcraft.builders.container.ContainerReplacer;
import buildcraft.builders.tile.TileArchitectTable;
import buildcraft.builders.tile.TileBuilder;
import buildcraft.builders.tile.TileElectronicLibrary;
import buildcraft.builders.tile.TileFiller;
import buildcraft.builders.tile.TileReplacer;

public enum BCBuildersGuis {
    ARCHITECT,
    BUILDER,
    FILLER,
    LIBRARY,
    REPLACER,
    FILLER_PLANNER;

    public static BCMenu<ContainerArchitectTable> architect;
    public static BCMenu<ContainerBuilder> builder;
    public static BCMenu<ContainerFiller> filler;
    public static BCMenu<ContainerElectronicLibrary> library;
    public static BCMenu<ContainerReplacer> replacer;
    /** The filler planner addon of the volume box the player is looking at (no position needed). */
    public static BCMenu<ContainerFillerPlanner> fillerPlanner;

    public static void fmlPreInit(IEventBus modBus) {
        var register = BCMenu.createRegister(BCBuilders.MODID, modBus);
        architect = BCMenu.tile(register, "architect", TileArchitectTable.class, ContainerArchitectTable::new);
        builder = BCMenu.tile(register, "builder", TileBuilder.class, ContainerBuilder::new);
        filler = BCMenu.tile(register, "filler", TileFiller.class, ContainerFiller::new);
        library = BCMenu.tile(register, "library", TileElectronicLibrary.class, ContainerElectronicLibrary::new);
        replacer = BCMenu.tile(register, "replacer", TileReplacer.class, ContainerReplacer::new);
        fillerPlanner = BCMenu.register(register, "filler_planner",
            (type, id, player, pos, data) -> new ContainerFillerPlanner(type, id, player));
    }

    public void openGUI(Player player) {
        openGUI(player, player.blockPosition());
    }

    public void openGUI(Player player, BlockPos pos) {
        switch (this) {
            case ARCHITECT:
                architect.open(player, pos);
                break;
            case BUILDER:
                builder.open(player, pos);
                break;
            case FILLER:
                filler.open(player, pos);
                break;
            case LIBRARY:
                library.open(player, pos);
                break;
            case REPLACER:
                replacer.open(player, pos);
                break;
            case FILLER_PLANNER:
                fillerPlanner.open(player, pos);
                break;
        }
    }
}
