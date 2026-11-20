/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

import net.neoforged.bus.api.IEventBus;

import buildcraft.lib.gui.BCMenu;

import buildcraft.energy.container.ContainerDynamoMJ;
import buildcraft.energy.container.ContainerEngineIron_BC8;
import buildcraft.energy.container.ContainerEngineRF;
import buildcraft.energy.container.ContainerEngineStone_BC8;
import buildcraft.energy.tile.TileDynamoMJ;
import buildcraft.energy.tile.TileEngineIron_BC8;
import buildcraft.energy.tile.TileEngineRF;
import buildcraft.energy.tile.TileEngineStone_BC8;

public enum BCEnergyGuis {
    ENGINE_STONE,
    ENGINE_IRON,
    ENGINE_RF,
    DYNAMO_MJ;

    public static final BCEnergyGuis[] VALUES = values();

    public static BCMenu<ContainerEngineStone_BC8> engineStone;
    public static BCMenu<ContainerEngineIron_BC8> engineIron;
    public static BCMenu<ContainerEngineRF> engineRf;
    public static BCMenu<ContainerDynamoMJ> dynamoMj;

    public static BCEnergyGuis get(int id) {
        if (id < 0 || id >= VALUES.length) return null;
        return VALUES[id];
    }

    public static void fmlPreInit(IEventBus modBus) {
        var register = BCMenu.createRegister(BCEnergy.MODID, modBus);
        engineStone = BCMenu.tile(register, "engine_stone", TileEngineStone_BC8.class, ContainerEngineStone_BC8::new);
        engineIron = BCMenu.tile(register, "engine_iron", TileEngineIron_BC8.class, ContainerEngineIron_BC8::new);
        engineRf = BCMenu.tile(register, "engine_rf", TileEngineRF.class, ContainerEngineRF::new);
        dynamoMj = BCMenu.tile(register, "dynamo_mj", TileDynamoMJ.class, ContainerDynamoMJ::new);
    }

    public BCMenu<?> menu() {
        switch (this) {
            case ENGINE_STONE:
                return engineStone;
            case ENGINE_IRON:
                return engineIron;
            case ENGINE_RF:
                return engineRf;
            case DYNAMO_MJ:
            default:
                return dynamoMj;
        }
    }

    public void openGUI(Player player) {
        openGUI(player, player.blockPosition());
    }

    public void openGUI(Player player, BlockPos pos) {
        menu().open(player, pos);
    }
}
