/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.transport;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.bus.api.IEventBus;

import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.api.transport.pipe.PipeBehaviour;

import buildcraft.lib.gui.BCMenu;
import buildcraft.lib.gui.ContainerBC_Neptune;

import buildcraft.transport.container.ContainerDiamondPipe;
import buildcraft.transport.container.ContainerDiamondWoodPipe;
import buildcraft.transport.container.ContainerEmzuliPipe_BC8;
import buildcraft.transport.container.ContainerFilteredBuffer_BC8;
import buildcraft.transport.pipe.behaviour.PipeBehaviourDiamond;
import buildcraft.transport.pipe.behaviour.PipeBehaviourEmzuli;
import buildcraft.transport.pipe.behaviour.PipeBehaviourWoodDiamond;
import buildcraft.transport.tile.TileFilteredBuffer;

public enum BCTransportGuis {
    FILTERED_BUFFER,
    PIPE_DIAMOND,
    PIPE_DIAMOND_WOOD,
    PIPE_EMZULI;

    public static final BCTransportGuis[] VALUES = values();

    public static BCMenu<ContainerFilteredBuffer_BC8> filteredBuffer;
    public static BCMenu<ContainerDiamondPipe> pipeDiamond;
    public static BCMenu<ContainerDiamondWoodPipe> pipeDiamondWood;
    public static BCMenu<ContainerEmzuliPipe_BC8> pipeEmzuli;

    public static BCTransportGuis get(int id) {
        if (id < 0 || id >= VALUES.length) return null;
        return VALUES[id];
    }

    /** A factory for the container of one pipe behaviour class. */
    @FunctionalInterface
    private interface PipeFactory<B, C extends ContainerBC_Neptune> {
        C create(MenuType<C> type, int containerId, Player player, B behaviour);
    }

    public static void fmlPreInit(IEventBus modBus) {
        var register = BCMenu.createRegister(BCTransport.MODID, modBus);
        filteredBuffer = BCMenu.tile(register, "filtered_buffer", TileFilteredBuffer.class,
            ContainerFilteredBuffer_BC8::new);
        pipeDiamond = pipe(register, "pipe_diamond", PipeBehaviourDiamond.class, ContainerDiamondPipe::new);
        pipeDiamondWood = pipe(register, "pipe_diamond_wood", PipeBehaviourWoodDiamond.class,
            ContainerDiamondWoodPipe::new);
        pipeEmzuli = pipe(register, "pipe_emzuli", PipeBehaviourEmzuli.class, ContainerEmzuliPipe_BC8::new);
    }

    private static <B, C extends ContainerBC_Neptune> BCMenu<C> pipe(
        net.neoforged.neoforge.registries.DeferredRegister<MenuType<?>> register, String name, Class<B> behaviourClass,
        PipeFactory<B, C> factory) {
        return BCMenu.register(register, name, (type, id, player, pos, data) -> {
            B behaviour = getBehaviour(player.level().getBlockEntity(pos), behaviourClass);
            return behaviour == null ? null : factory.create(type, id, player, behaviour);
        });
    }

    @Nullable
    private static <B> B getBehaviour(@Nullable BlockEntity tile, Class<B> behaviourClass) {
        if (tile instanceof IPipeHolder holder) {
            IPipe pipe = holder.getPipe();
            if (pipe != null) {
                PipeBehaviour behaviour = pipe.getBehaviour();
                if (behaviourClass.isInstance(behaviour)) {
                    return behaviourClass.cast(behaviour);
                }
            }
        }
        return null;
    }

    public void openGui(Player player) {
        openGui(player, player.blockPosition());
    }

    public void openGui(Player player, BlockPos pos) {
        switch (this) {
            case FILTERED_BUFFER:
                filteredBuffer.open(player, pos);
                break;
            case PIPE_DIAMOND:
                pipeDiamond.open(player, pos);
                break;
            case PIPE_DIAMOND_WOOD:
                pipeDiamondWood.open(player, pos);
                break;
            case PIPE_EMZULI:
                pipeEmzuli.open(player, pos);
                break;
        }
    }

    public void openGui(Player player, int x, int y, int z) {
        openGui(player, new BlockPos(x, y, z));
    }
}
