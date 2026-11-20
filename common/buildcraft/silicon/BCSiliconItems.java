/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import buildcraft.api.enums.EnumRedstoneChipset;
import buildcraft.api.facades.FacadeAPI;

import buildcraft.lib.item.ItemPluggableSimple;
import buildcraft.lib.registry.RegistrationHelper;

import buildcraft.silicon.item.ItemGateCopier;
import buildcraft.silicon.item.ItemPluggableFacade;
import buildcraft.silicon.item.ItemPluggableGate;
import buildcraft.silicon.item.ItemPluggableLens;
import buildcraft.silicon.item.ItemPluggableLens.LensData;
import buildcraft.silicon.item.ItemRedstoneChipset;
import buildcraft.silicon.plug.PluggablePulsar;

public class BCSiliconItems {
    /** A lens' colour and whether it is a filter: "buildcraftsilicon:lens" (1.12.2 used the damage). */
    public static final DataComponentType<LensData> LENS = ItemPluggableLens.createComponent();

    /** One item per chipset type ("redstone_chipset_iron"; 1.12.2 had one item with the type in its damage). */
    public static final Map<EnumRedstoneChipset, ItemRedstoneChipset> redstoneChipsets =
        new EnumMap<>(EnumRedstoneChipset.class);
    public static ItemGateCopier gateCopier;

    public static ItemPluggableGate plugGate;
    public static ItemPluggableLens plugLens;
    public static ItemPluggableSimple plugPulsar;
    public static ItemPluggableSimple plugLightSensor;
    public static ItemPluggableSimple plugTimer;
    public static ItemPluggableFacade plugFacade;

    public static void preInit(IEventBus modBus) {
        DeferredRegister<DataComponentType<?>> components =
            RegistrationHelper.createRegister(Registries.DATA_COMPONENT_TYPE, BCSilicon.MODID, modBus);
        components.register("lens", () -> LENS);

        RegistrationHelper helper = new RegistrationHelper(BCSilicon.MODID, modBus);
        for (EnumRedstoneChipset type : EnumRedstoneChipset.values()) {
            helper.addItem("item.redstone_chipset." + type.getName(),
                (p, id) -> new ItemRedstoneChipset(p, id, type), i -> redstoneChipsets.put(type, i));
        }
        helper.addItem("item.gate_copier", ItemGateCopier::new, i -> gateCopier = i);
        helper.addItem("item.plug.gate", ItemPluggableGate::new, i -> plugGate = i);
        helper.addItem("item.plug.lens", ItemPluggableLens::new, i -> plugLens = i);
        helper.addItem("item.plug.pulsar", (p, id) -> new ItemPluggableSimple(p, id, BCSiliconPlugs.pulsar,
            PluggablePulsar::new, ItemPluggableSimple.PIPE_BEHAVIOUR_ACCEPTS_RS_POWER), i -> plugPulsar = i);
        helper.addItem("item.plug.light_sensor", (p, id) -> new ItemPluggableSimple(p, id, BCSiliconPlugs.lightSensor),
            i -> plugLightSensor = i);
        helper.addItem("item.plug.timer", (p, id) -> new ItemPluggableSimple(p, id, BCSiliconPlugs.timer),
            i -> plugTimer = i);
        helper.addItem("item.plug.facade", ItemPluggableFacade::new, i -> {
            plugFacade = i;
            FacadeAPI.facadeItem = i;
        });
    }

    static String lower(Enum<?> e) {
        return e.name().toLowerCase(Locale.ROOT);
    }
}
