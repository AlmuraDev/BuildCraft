/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.core;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import buildcraft.api.items.FluidItemDrops;
import buildcraft.api.items.IMapLocation.MapLocationType;

import buildcraft.lib.BCLib;
import buildcraft.lib.item.ItemBC_Neptune;
import buildcraft.lib.registry.RegistrationHelper;

import buildcraft.core.item.ItemFragileFluidContainer;
import buildcraft.core.item.ItemGoggles;
import buildcraft.core.item.ItemList_BC8;
import buildcraft.core.item.ItemMapLocation;
import buildcraft.core.item.ItemMarkerConnector;
import buildcraft.core.item.ItemPaintbrush_BC8;
import buildcraft.core.item.ItemVolumeBox;
import buildcraft.core.item.ItemWrench_Neptune;

public class BCCoreItems {

    public static ItemWrench_Neptune wrench;
    public static ItemBC_Neptune gearWood;
    public static ItemBC_Neptune gearStone;
    public static ItemBC_Neptune gearIron;
    public static ItemBC_Neptune gearGold;
    public static ItemBC_Neptune gearDiamond;
    public static ItemPaintbrush_BC8 paintbrush;
    public static ItemList_BC8 list;
    public static ItemMapLocation mapLocation;
    public static ItemMarkerConnector markerConnector;
    public static ItemVolumeBox volumeBox;
    public static ItemFragileFluidContainer fragileFluidShard;
    public static ItemGoggles goggles;

    public static void preInit(IEventBus modBus) {
        DeferredRegister<DataComponentType<?>> components =
            RegistrationHelper.createRegister(Registries.DATA_COMPONENT_TYPE, BCCore.MODID, modBus);
        components.register("map_location_type", () -> MapLocationType.COMPONENT);
        components.register("paint_colour", () -> ItemPaintbrush_BC8.COLOUR);
        components.register("fluid", () -> ItemFragileFluidContainer.FLUID);
        modBus.addListener((RegisterCapabilitiesEvent event) -> {
            if (fragileFluidShard != null) {
                fragileFluidShard.registerCapabilities(event);
            }
        });

        RegistrationHelper helper = new RegistrationHelper(BCCore.MODID, modBus);
        helper.addItem("item.wrench", ItemWrench_Neptune::new, i -> wrench = i);
        helper.addItem("item.gear.wood", ItemBC_Neptune::new, i -> gearWood = i);
        helper.addItem("item.gear.stone", ItemBC_Neptune::new, i -> gearStone = i);
        helper.addItem("item.gear.iron", ItemBC_Neptune::new, i -> gearIron = i);
        helper.addItem("item.gear.gold", ItemBC_Neptune::new, i -> gearGold = i);
        helper.addItem("item.gear.diamond", ItemBC_Neptune::new, i -> gearDiamond = i);
        helper.addItem("item.paintbrush", ItemPaintbrush_BC8::new, i -> paintbrush = i);
        helper.addItem("item.list", ItemList_BC8::new, i -> list = i);
        helper.addItem("item.map_location", ItemMapLocation::new, i -> mapLocation = i);
        helper.addItem("item.marker_connector", ItemMarkerConnector::new, i -> markerConnector = i);
        helper.addItem("item.volume_box", ItemVolumeBox::new, i -> volumeBox = i);
        helper.addItem("item.fragile_fluid_shard", ItemFragileFluidContainer::new, i -> {
            fragileFluidShard = i;
            FluidItemDrops.item = i;
        });
        if (BCLib.DEV) {
            helper.addItem("item.goggles", ItemGoggles::new, i -> goggles = i);
        }
    }
}
