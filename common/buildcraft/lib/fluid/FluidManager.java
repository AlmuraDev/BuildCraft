/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.fluid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;


import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Registers BuildCraft's {@link BCFluid fluids}. For each one: a fluid type and source fluid "&lt;modid&gt;:&lt;name&gt;", a
 * flowing fluid "&lt;name&gt;_flowing", a block "fluid_block_&lt;block name&gt;" (its 1.12.2 name) and a bucket
 * "bucket_&lt;name&gt;" (1.12.2 used Forge's universal bucket). */
public class FluidManager {
    private static final Map<String, List<BCFluid>> byMod = new HashMap<>();
    private static final List<BCFluid> all = new ArrayList<>();
    private static final Map<String, BCFluid> byLegacyName = new HashMap<>();

    /** Registers the fluid in the given mod (call from its constructor). */
    public static <F extends BCFluid> F register(String modId, IEventBus modBus, F fluid) {
        List<BCFluid> list = byMod.get(modId);
        if (list == null) {
            list = new ArrayList<>();
            byMod.put(modId, list);
            List<BCFluid> modFluids = list;
            modBus.addListener((RegisterEvent event) -> onRegister(event, modId, modFluids));
        }
        list.add(fluid);
        all.add(fluid);
        byLegacyName.put(fluid.getName(), fluid);
        return fluid;
    }

    public static List<BCFluid> getAll() {
        return Collections.unmodifiableList(all);
    }

    /** @return The fluid that had the given name on 1.12.2, or null. */
    @Nullable
    public static BCFluid getByLegacyName(String name) {
        return byLegacyName.get(name);
    }

    private static void onRegister(RegisterEvent event, String modId, List<BCFluid> fluids) {
        if (event.getRegistryKey().equals(NeoForgeRegistries.Keys.FLUID_TYPES)) {
            for (BCFluid fluid : fluids) {
                FluidType type = new FluidType(FluidType.Properties.create()
                    .descriptionId(fluid.getUnlocalizedName())
                    .density(fluid.getDensity())
                    .viscosity(fluid.getViscosity())
                    .temperature(fluid.getTemperature())
                    .lightLevel(fluid.getLuminosity())
                    .canConvertToSource(false));
                fluid.fluidType = type;
                event.register(NeoForgeRegistries.Keys.FLUID_TYPES, id(modId, fluid.getName()), () -> type);
            }
        } else if (event.getRegistryKey().equals(Registries.FLUID)) {
            for (BCFluid fluid : fluids) {
                // Higher spread distances flow further: 16 -> 1 level per block, 1 -> 8 levels per block
                int decrease = Math.max(1, Math.min(8, Math.round(8f / fluid.getSpreadDistance())));
                BaseFlowingFluid.Properties props = new BaseFlowingFluid.Properties(() -> fluid.fluidType,
                    () -> fluid.source, () -> fluid.flowingFluid)
                    .block(() -> fluid.block)
                    .bucket(() -> fluid.bucket)
                    .levelDecreasePerBlock(decrease)
                    .slopeFindDistance(Math.max(1, Math.min(4, fluid.getSpreadDistance() / 2)))
                    .tickRate(5 + fluid.getViscosity() / 200);
                fluid.source = new BaseFlowingFluid.Source(props);
                fluid.flowingFluid = new BaseFlowingFluid.Flowing(props);
                event.register(Registries.FLUID, id(modId, fluid.getName()), () -> fluid.source);
                event.register(Registries.FLUID, id(modId, fluid.getName() + "_flowing"), () -> fluid.flowingFluid);
            }
        } else if (event.getRegistryKey().equals(Registries.BLOCK)) {
            for (BCFluid fluid : fluids) {
                Identifier name = id(modId, "fluid_block_" + fluid.getBlockName());
                BlockBehaviour.Properties props = BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, name))
                    .mapColor(fluid.getMapColour())
                    .replaceable()
                    .noCollision()
                    .strength(100.0F)
                    .pushReaction(PushReaction.DESTROY)
                    .noLootTable()
                    .liquid();
                if (fluid.isFlammable()) {
                    props = props.ignitedByLava();
                }
                fluid.block = new BCFluidBlock(fluid, fluid.source, props);
                event.register(Registries.BLOCK, name, () -> fluid.block);
            }
        } else if (event.getRegistryKey().equals(Registries.ITEM)) {
            for (BCFluid fluid : fluids) {
                Identifier name = id(modId, "bucket_" + fluid.getName());
                Item.Properties props = new Item.Properties().setId(ResourceKey.create(Registries.ITEM, name))
                    .craftRemainder(Items.BUCKET).stacksTo(1);
                fluid.bucket = new BucketItem(fluid.source, props);
                event.register(Registries.ITEM, name, () -> fluid.bucket);
            }
        }
    }

    private static Identifier id(String modId, String path) {
        return Identifier.fromNamespaceAndPath(modId, path);
    }

    /** Unused on 26.3 (blocks always use their fluid's model): kept for callers. */
    @Deprecated
    public static List<Block> getFluidBlocks() {
        List<Block> blocks = new ArrayList<>();
        for (BCFluid fluid : all) {
            if (fluid.block != null) {
                blocks.add(fluid.block);
            }
        }
        return blocks;
    }
}
