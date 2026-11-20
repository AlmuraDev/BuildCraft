/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders;

import net.neoforged.bus.api.IEventBus;

import buildcraft.lib.registry.RegistrationHelper;

import buildcraft.builders.item.ItemFillerPlanner;
import buildcraft.builders.item.ItemSchematicSingle;
import buildcraft.builders.item.ItemSnapshot;

public class BCBuildersItems {

    /** Blueprint or template: "buildcraftbuilders:snapshot_type" (1.12.2 used the snapshot item's damage). */
    public static final net.minecraft.core.component.DataComponentType<buildcraft.api.enums.EnumSnapshotType> SNAPSHOT_TYPE =
        ItemSnapshot.createTypeComponent();

    public static ItemSnapshot snapshot;
    public static ItemSchematicSingle schematicSingle;
    public static ItemFillerPlanner addonFillerPlanner;

    public static void fmlPreInit(IEventBus modBus) {
        net.neoforged.neoforge.registries.DeferredRegister<net.minecraft.core.component.DataComponentType<?>> components =
            RegistrationHelper.createRegister(net.minecraft.core.registries.Registries.DATA_COMPONENT_TYPE,
                BCBuilders.MODID, modBus);
        components.register("snapshot_type", () -> SNAPSHOT_TYPE);

        RegistrationHelper helper = new RegistrationHelper(BCBuilders.MODID, modBus);
        helper.addItem("item.snapshot", ItemSnapshot::new, i -> snapshot = i);
        helper.addItem("item.schematic.single", ItemSchematicSingle::new, i -> schematicSingle = i);
        helper.addItem("item.filler_planner", ItemFillerPlanner::new, i -> addonFillerPlanner = i);
    }
}
