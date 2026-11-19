/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport;

import java.util.function.Consumer;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.DyeColor;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import buildcraft.api.transport.pipe.PipeDefinition;

import buildcraft.lib.item.ItemBC_Neptune;
import buildcraft.lib.item.ItemPluggableSimple;
import buildcraft.lib.registry.RegistrationHelper;

import buildcraft.transport.item.ItemPipeHolder;
import buildcraft.transport.item.ItemWire;
import buildcraft.transport.pipe.PipeRegistry;

public class BCTransportItems {
    /** The colour of pipe and wire stacks: "buildcrafttransport:colour" (1.12.2 used the damage value). */
    public static final DataComponentType<DyeColor> COLOUR = DataComponentType.<DyeColor> builder()
        .persistent(DyeColor.CODEC)
        .networkSynchronized(DyeColor.STREAM_CODEC)
        .build();

    public static ItemBC_Neptune waterproof;

    public static ItemPipeHolder pipeStructure;

    public static ItemPipeHolder pipeItemWood;
    public static ItemPipeHolder pipeFluidWood;
    public static ItemPipeHolder pipePowerWood;
    public static ItemPipeHolder pipeRfWood;

    public static ItemPipeHolder pipeItemStone;
    public static ItemPipeHolder pipeFluidStone;
    public static ItemPipeHolder pipePowerStone;
    public static ItemPipeHolder pipeRfStone;

    public static ItemPipeHolder pipeItemCobble;
    public static ItemPipeHolder pipeFluidCobble;
    public static ItemPipeHolder pipePowerCobble;
    public static ItemPipeHolder pipeRfCobble;

    public static ItemPipeHolder pipeItemQuartz;
    public static ItemPipeHolder pipeFluidQuartz;
    public static ItemPipeHolder pipePowerQuartz;
    public static ItemPipeHolder pipeRfQuartz;

    public static ItemPipeHolder pipeItemGold;
    public static ItemPipeHolder pipeFluidGold;
    public static ItemPipeHolder pipePowerGold;
    public static ItemPipeHolder pipeRfGold;

    public static ItemPipeHolder pipeItemSandstone;
    public static ItemPipeHolder pipeFluidSandstone;
    public static ItemPipeHolder pipePowerSandstone;
    public static ItemPipeHolder pipeRfSandstone;

    public static ItemPipeHolder pipeItemIron;
    public static ItemPipeHolder pipeFluidIron;
    public static ItemPipeHolder pipePowerIron;
    public static ItemPipeHolder pipeRfIron;

    public static ItemPipeHolder pipeItemDiamond;
    public static ItemPipeHolder pipeFluidDiamond;
    public static ItemPipeHolder pipePowerDiamond;
    public static ItemPipeHolder pipeRfDiamond;

    public static ItemPipeHolder pipeItemDiaWood;
    public static ItemPipeHolder pipeFluidDiaWood;
    public static ItemPipeHolder pipePowerDiaWood;
    public static ItemPipeHolder pipeRfDiaWood;

    public static ItemPipeHolder pipeItemClay;
    public static ItemPipeHolder pipeFluidClay;

    public static ItemPipeHolder pipeItemVoid;
    public static ItemPipeHolder pipeFluidVoid;

    public static ItemPipeHolder pipeItemObsidian;
    public static ItemPipeHolder pipeFluidObsidian;

    public static ItemPipeHolder pipeItemLapis;
    public static ItemPipeHolder pipeItemDaizuli;
    public static ItemPipeHolder pipeItemEmzuli;
    public static ItemPipeHolder pipeItemStripes;

    public static ItemPluggableSimple plugBlocker;
    public static ItemPluggableSimple plugPowerAdaptor;

    public static ItemWire wire;

    public static void preInit(IEventBus modBus) {
        DeferredRegister<DataComponentType<?>> components =
            RegistrationHelper.createRegister(Registries.DATA_COMPONENT_TYPE, BCTransport.MODID, modBus);
        components.register("colour", () -> COLOUR);

        RegistrationHelper helper = new RegistrationHelper(BCTransport.MODID, modBus);
        helper.addItem("item.waterproof", ItemBC_Neptune::new, i -> waterproof = i);

        makePipeItem(BCTransportPipes.structure, i -> pipeStructure = i);

        // Register them in order of type -- item, fluid, power
        makePipeItem(BCTransportPipes.woodItem, i -> pipeItemWood = i);
        makePipeItem(BCTransportPipes.cobbleItem, i -> pipeItemCobble = i);
        makePipeItem(BCTransportPipes.stoneItem, i -> pipeItemStone = i);
        makePipeItem(BCTransportPipes.quartzItem, i -> pipeItemQuartz = i);
        makePipeItem(BCTransportPipes.ironItem, i -> pipeItemIron = i);
        makePipeItem(BCTransportPipes.goldItem, i -> pipeItemGold = i);
        makePipeItem(BCTransportPipes.clayItem, i -> pipeItemClay = i);
        makePipeItem(BCTransportPipes.sandstoneItem, i -> pipeItemSandstone = i);
        makePipeItem(BCTransportPipes.voidItem, i -> pipeItemVoid = i);
        makePipeItem(BCTransportPipes.obsidianItem, i -> pipeItemObsidian = i);
        makePipeItem(BCTransportPipes.diamondItem, i -> pipeItemDiamond = i);
        makePipeItem(BCTransportPipes.diaWoodItem, i -> pipeItemDiaWood = i);
        makePipeItem(BCTransportPipes.lapisItem, i -> pipeItemLapis = i);
        makePipeItem(BCTransportPipes.daizuliItem, i -> pipeItemDaizuli = i);
        makePipeItem(BCTransportPipes.emzuliItem, i -> pipeItemEmzuli = i);
        makePipeItem(BCTransportPipes.stripesItem, i -> pipeItemStripes = i);

        makePipeItem(BCTransportPipes.woodFluid, i -> pipeFluidWood = i);
        makePipeItem(BCTransportPipes.cobbleFluid, i -> pipeFluidCobble = i);
        makePipeItem(BCTransportPipes.stoneFluid, i -> pipeFluidStone = i);
        makePipeItem(BCTransportPipes.quartzFluid, i -> pipeFluidQuartz = i);
        makePipeItem(BCTransportPipes.goldFluid, i -> pipeFluidGold = i);
        makePipeItem(BCTransportPipes.ironFluid, i -> pipeFluidIron = i);
        makePipeItem(BCTransportPipes.clayFluid, i -> pipeFluidClay = i);
        makePipeItem(BCTransportPipes.sandstoneFluid, i -> pipeFluidSandstone = i);
        makePipeItem(BCTransportPipes.voidFluid, i -> pipeFluidVoid = i);
        makePipeItem(BCTransportPipes.diamondFluid, i -> pipeFluidDiamond = i);
        makePipeItem(BCTransportPipes.diaWoodFluid, i -> pipeFluidDiaWood = i);
        // makePipeItem(BCTransportPipes.obsidianFluid, i -> pipeFluidObsidian = i);

        makePipeItem(BCTransportPipes.woodPower, i -> pipePowerWood = i);
        makePipeItem(BCTransportPipes.cobblePower, i -> pipePowerCobble = i);
        makePipeItem(BCTransportPipes.stonePower, i -> pipePowerStone = i);
        makePipeItem(BCTransportPipes.quartzPower, i -> pipePowerQuartz = i);
        makePipeItem(BCTransportPipes.ironPower, i -> pipePowerIron = i);
        makePipeItem(BCTransportPipes.goldPower, i -> pipePowerGold = i);
        makePipeItem(BCTransportPipes.sandstonePower, i -> pipePowerSandstone = i);
        makePipeItem(BCTransportPipes.diamondPower, i -> pipePowerDiamond = i);
        makePipeItem(BCTransportPipes.diaWoodPower, i -> pipePowerDiaWood = i);

        if (!BCTransportConfig.disableRfPipe) {
            makePipeItem(BCTransportPipes.woodRf, i -> pipeRfWood = i);
            makePipeItem(BCTransportPipes.cobbleRf, i -> pipeRfCobble = i);
            makePipeItem(BCTransportPipes.stoneRf, i -> pipeRfStone = i);
            makePipeItem(BCTransportPipes.quartzRf, i -> pipeRfQuartz = i);
            makePipeItem(BCTransportPipes.ironRf, i -> pipeRfIron = i);
            makePipeItem(BCTransportPipes.goldRf, i -> pipeRfGold = i);
            makePipeItem(BCTransportPipes.sandstoneRf, i -> pipeRfSandstone = i);
            makePipeItem(BCTransportPipes.diamondRf, i -> pipeRfDiamond = i);
            makePipeItem(BCTransportPipes.diaWoodRf, i -> pipeRfDiaWood = i);
        }

        helper.addItem("item.plug.blocker",
            (props, id) -> new ItemPluggableSimple(props, id, BCTransportPlugs.blocker), i -> plugBlocker = i);
        helper.addItem("item.plug.power_adaptor", (props, id) -> new ItemPluggableSimple(props, id,
            BCTransportPlugs.powerAdaptor, ItemPluggableSimple.PIPE_BEHAVIOUR_ACCEPTS_RS_POWER), i -> plugPowerAdaptor = i);

        helper.addItem("item.wire", ItemWire::new, i -> wire = i);
    }

    /** Queues the pipe's item; the setter is given it when items are registered. */
    public static void makePipeItem(PipeDefinition def, Consumer<ItemPipeHolder> setter) {
        PipeRegistry.INSTANCE.createItemForPipe(def, item -> setter.accept((ItemPipeHolder) item));
    }
}
