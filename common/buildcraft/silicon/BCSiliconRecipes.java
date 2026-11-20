/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon;

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;

import net.neoforged.neoforge.common.crafting.DataComponentIngredient;

import buildcraft.api.enums.EnumEngineType;
import buildcraft.api.enums.EnumRedstoneChipset;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.recipes.AssemblyRecipe;
import buildcraft.api.recipes.AssemblyRecipeBasic;
import buildcraft.api.recipes.IngredientStack;

import buildcraft.lib.misc.ColourUtil;
import buildcraft.lib.misc.StringUtilBC;
import buildcraft.lib.recipe.AssemblyRecipeRegistry;

import buildcraft.core.BCCoreBlocks;
import buildcraft.core.BCCoreItems;
import buildcraft.silicon.gate.EnumGateLogic;
import buildcraft.silicon.gate.EnumGateMaterial;
import buildcraft.silicon.gate.EnumGateModifier;
import buildcraft.silicon.gate.GateVariant;
import buildcraft.silicon.recipe.FacadeAssemblyRecipes;

/** Silicon's assembly table recipes. They live in BuildCraft's own registry and use item tags, so they are added each
 * time tags are loaded ({@link net.neoforged.neoforge.event.TagsUpdatedEvent}).
 * <p>
 * The crafting table recipes (basic gates, gate modifiers, and/or swaps, the facade hollow swap) are data now, written
 * by tools/port-26.3/recipes.py. TODO(26.3): 1.12.2 also read assembly recipes from json files in
 * assets/&lt;mod&gt;/assembly_recipes_pre_mj and config/buildcraft/assembly_recipes_pre_mj; that isn't ported. */
public class BCSiliconRecipes {

    /** Assembly recipes are registered outside any mod's loading context, so they name their namespace. */
    private static String id(String name) {
        return "buildcraftsilicon:" + name;
    }

    public static void registerAssemblyRecipes() {
        if (BCSiliconItems.plugPulsar != null) {
            ItemStack output = new ItemStack(BCSiliconItems.plugPulsar);

            ItemStack redstoneEngine;
            if (BCCoreBlocks.engine != null) {
                redstoneEngine = BCCoreBlocks.engine.getStack(EnumEngineType.WOOD);
            } else {
                redstoneEngine = new ItemStack(Blocks.REDSTONE_BLOCK);
            }

            Set<IngredientStack> input = new HashSet<>();
            input.add(new IngredientStack(DataComponentIngredient.of(false, redstoneEngine)));
            input.add(new IngredientStack(IngredientStack.toIngredient("c:ingots/iron"), 2));
            register(new AssemblyRecipeBasic(id("plug_pulsar"), 1000 * MjAPI.MJ, input, output));
        }
        if (BCSiliconItems.plugGate != null) {
            IngredientStack lapis = IngredientStack.of("c:gems/lapis");
            makeGateAssembly(20_000, EnumGateMaterial.IRON, EnumGateModifier.NO_MODIFIER, EnumRedstoneChipset.IRON);
            makeGateAssembly(40_000, EnumGateMaterial.NETHER_BRICK, EnumGateModifier.NO_MODIFIER,
                EnumRedstoneChipset.IRON, IngredientStack.of(Blocks.NETHER_BRICKS));
            makeGateAssembly(80_000, EnumGateMaterial.GOLD, EnumGateModifier.NO_MODIFIER, EnumRedstoneChipset.GOLD);

            makeGateModifierAssembly(40_000, EnumGateMaterial.IRON, EnumGateModifier.LAPIS, lapis);
            makeGateModifierAssembly(60_000, EnumGateMaterial.IRON, EnumGateModifier.QUARTZ,
                IngredientStack.of(EnumRedstoneChipset.QUARTZ.getStack()));
            makeGateModifierAssembly(80_000, EnumGateMaterial.IRON, EnumGateModifier.DIAMOND,
                IngredientStack.of(EnumRedstoneChipset.DIAMOND.getStack()));

            makeGateModifierAssembly(80_000, EnumGateMaterial.NETHER_BRICK, EnumGateModifier.LAPIS, lapis);
            makeGateModifierAssembly(100_000, EnumGateMaterial.NETHER_BRICK, EnumGateModifier.QUARTZ,
                IngredientStack.of(EnumRedstoneChipset.QUARTZ.getStack()));
            makeGateModifierAssembly(120_000, EnumGateMaterial.NETHER_BRICK, EnumGateModifier.DIAMOND,
                IngredientStack.of(EnumRedstoneChipset.DIAMOND.getStack()));

            makeGateModifierAssembly(100_000, EnumGateMaterial.GOLD, EnumGateModifier.LAPIS, lapis);
            makeGateModifierAssembly(140_000, EnumGateMaterial.GOLD, EnumGateModifier.QUARTZ,
                IngredientStack.of(EnumRedstoneChipset.QUARTZ.getStack()));
            makeGateModifierAssembly(180_000, EnumGateMaterial.GOLD, EnumGateModifier.DIAMOND,
                IngredientStack.of(EnumRedstoneChipset.DIAMOND.getStack()));
        }

        if (BCSiliconItems.plugLightSensor != null) {
            register(new AssemblyRecipeBasic(id("light-sensor"), 500 * MjAPI.MJ,
                ImmutableSet.of(IngredientStack.of(Blocks.DAYLIGHT_DETECTOR)),
                new ItemStack(BCSiliconItems.plugLightSensor)));
        }

        if (BCSiliconItems.plugTimer != null) {
            register(new AssemblyRecipeBasic(id("timer"), 500 * MjAPI.MJ,
                ImmutableSet.of(IngredientStack.of(Items.CLOCK)), new ItemStack(BCSiliconItems.plugTimer)));
        }

        if (BCSiliconItems.plugFacade != null) {
            register(FacadeAssemblyRecipes.INSTANCE);
        }

        if (BCSiliconItems.plugLens != null) {
            for (DyeColor colour : ColourUtil.COLOURS) {
                String name = StringUtilBC.formatDirect("lens-regular-%s", colour.getName());
                // 26.3 has no per-colour glass tags (1.12.2: "blockGlass<Colour>")
                IngredientStack stainedGlass = IngredientStack.of(net.minecraft.core.registries.BuiltInRegistries.ITEM
                    .getValue(net.minecraft.resources.Identifier.withDefaultNamespace(colour.getName() + "_stained_glass")));
                ImmutableSet<IngredientStack> input = ImmutableSet.of(stainedGlass);
                ItemStack output = BCSiliconItems.plugLens.getStack(colour, false);
                register(new AssemblyRecipeBasic(id(name), 500 * MjAPI.MJ, input, output));

                name = StringUtilBC.formatDirect("lens-filter-%s", colour.getName());
                output = BCSiliconItems.plugLens.getStack(colour, true);
                input = ImmutableSet.of(stainedGlass, IngredientStack.of(Blocks.IRON_BARS));
                register(new AssemblyRecipeBasic(id(name), 500 * MjAPI.MJ, input, output));
            }

            IngredientStack glass = IngredientStack.of("c:glass_blocks");
            ImmutableSet<IngredientStack> input = ImmutableSet.of(glass);
            ItemStack output = BCSiliconItems.plugLens.getStack(null, false);
            register(new AssemblyRecipeBasic(id("lens-regular"), 500 * MjAPI.MJ, input, output));

            output = BCSiliconItems.plugLens.getStack(null, true);
            input = ImmutableSet.of(glass, IngredientStack.of(Blocks.IRON_BARS));
            register(new AssemblyRecipeBasic(id("lens-filter"), 500 * MjAPI.MJ, input, output));
        }

        if (!BCSiliconItems.redstoneChipsets.isEmpty()) {
            IngredientStack redstone = IngredientStack.of("c:dusts/redstone");
            chipset("redstone_chipset", 10000, EnumRedstoneChipset.RED, ImmutableSet.of(redstone));
            chipset("iron_chipset", 20000, EnumRedstoneChipset.IRON,
                ImmutableSet.of(redstone, IngredientStack.of("c:ingots/iron")));
            chipset("gold_chipset", 40000, EnumRedstoneChipset.GOLD,
                ImmutableSet.of(redstone, IngredientStack.of("c:ingots/gold")));
            chipset("quartz_chipset", 60000, EnumRedstoneChipset.QUARTZ,
                ImmutableSet.of(redstone, IngredientStack.of("c:gems/quartz")));
            chipset("diamond_chipset", 80000, EnumRedstoneChipset.DIAMOND,
                ImmutableSet.of(redstone, IngredientStack.of("c:gems/diamond")));
        }

        if (BCSiliconItems.gateCopier != null) {
            ImmutableSet.Builder<IngredientStack> input = ImmutableSet.builder();
            if (BCCoreItems.wrench != null) {
                input.add(IngredientStack.of(BCCoreItems.wrench));
            } else {
                input.add(IngredientStack.of(Items.STICK));
                input.add(IngredientStack.of(Items.IRON_INGOT));
            }

            if (!BCSiliconItems.redstoneChipsets.isEmpty()) {
                input.add(IngredientStack.of(EnumRedstoneChipset.IRON.getStack(1)));
            } else {
                input.add(IngredientStack.of("c:dusts/redstone"));
                input.add(IngredientStack.of("c:dusts/redstone"));
                input.add(IngredientStack.of("c:ingots/gold"));
            }
            register(new AssemblyRecipeBasic(id("gate_copier"), 500 * MjAPI.MJ, input.build(),
                new ItemStack(BCSiliconItems.gateCopier)));
        }
    }

    private static void register(AssemblyRecipe recipe) {
        AssemblyRecipeRegistry.register(recipe);
    }

    private static void chipset(String name, int mj, EnumRedstoneChipset type, ImmutableSet<IngredientStack> input) {
        ItemStack output = type.getStack(1);
        if (!output.isEmpty()) {
            register(new AssemblyRecipeBasic(id(name), mj * MjAPI.MJ, input, output));
        }
    }

    /** An ingredient matching the exact gate (or other custom-data) stack (1.12.2's IngredientNBTBC). */
    private static Ingredient exact(ItemStack stack) {
        return DataComponentIngredient.of(false, stack);
    }

    private static void makeGateModifierAssembly(int multiplier, EnumGateMaterial material, EnumGateModifier modifier,
        IngredientStack... mods) {
        for (EnumGateLogic logic : EnumGateLogic.VALUES) {
            String name = StringUtilBC.formatDirect("gate-modifier-%s-%s-%s", logic, material, modifier);
            GateVariant variantFrom = new GateVariant(logic, material, EnumGateModifier.NO_MODIFIER);
            ItemStack toUpgrade = BCSiliconItems.plugGate.getStack(variantFrom);
            ItemStack output = BCSiliconItems.plugGate.getStack(new GateVariant(logic, material, modifier));
            Builder<IngredientStack> inputBuilder = new ImmutableSet.Builder<>();
            inputBuilder.add(new IngredientStack(exact(toUpgrade)));
            inputBuilder.add(mods);
            register(new AssemblyRecipeBasic(id(name), MjAPI.MJ * multiplier, inputBuilder.build(), output));
        }
    }

    private static void makeGateAssembly(int multiplier, EnumGateMaterial material, EnumGateModifier modifier,
        EnumRedstoneChipset chipset, IngredientStack... additional) {
        ImmutableSet.Builder<IngredientStack> temp = ImmutableSet.builder();
        temp.add(new IngredientStack(exact(chipset.getStack())));
        temp.add(additional);
        ImmutableSet<IngredientStack> input = temp.build();

        String name = StringUtilBC.formatDirect("gate-and-%s-%s", material, modifier);
        ItemStack output = BCSiliconItems.plugGate.getStack(new GateVariant(EnumGateLogic.AND, material, modifier));
        register(new AssemblyRecipeBasic(id(name), MjAPI.MJ * multiplier, input, output));

        name = StringUtilBC.formatDirect("gate-or-%s-%s", material, modifier);
        output = BCSiliconItems.plugGate.getStack(new GateVariant(EnumGateLogic.OR, material, modifier));
        register(new AssemblyRecipeBasic(id(name), MjAPI.MJ * multiplier, input, output));
    }
}
