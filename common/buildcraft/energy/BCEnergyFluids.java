/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.energy;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.MapColor;

import net.neoforged.bus.api.IEventBus;

import buildcraft.lib.fluid.BCFluid;
import buildcraft.lib.fluid.FluidManager;

/** Oil and its products, each in three heat values (the factory module is always present on 26.3, so all of them are
 * registered). Registry names are 1.12.2's: fluids "oil", "oil_heat_1", blocks "fluid_block_oil_heat_0".
 * <p>
 * The textures are baked from the grey heat textures by tools/port-26.3/fluid_textures.py (1.12.2 recoloured them at
 * runtime); keep its table in step with this one. */
public class BCEnergyFluids {
    public static BCFluid[] crudeOil;
    /** All 3 fuels (no residue) */
    public static BCFluid[] oilDistilled;
    /** The 3 heaviest components (fuelLight, fuelDense and oilResidue) */
    public static BCFluid[] oilHeavy;
    /** The 2 lightest fuels (no dense fuel) */
    public static BCFluid[] fuelMixedLight;
    /** The 2 heaviest fuels (no gaseous fuel) */
    public static BCFluid[] fuelMixedHeavy;
    /** The 2 heaviest products (fuelDense and oilResidue) */
    public static BCFluid[] oilDense;

    // End products in order from least to most dense
    public static BCFluid[] fuelGaseous;
    public static BCFluid[] fuelLight;
    public static BCFluid[] fuelDense;
    public static BCFluid[] oilResidue;

    public static final List<BCFluid> allFluids = new ArrayList<>();

    public static void preInit(IEventBus modBus) {
        int[][] data = { //@formatter:off
            // Tabular form of all the fluid values
            // density, viscosity, boil, spread,  tex_light,   tex_dark, sticky, flammable
            {      900,      2000,    3,      6, 0x50_50_50, 0x05_05_05,      1,         1 },// Crude Oil
            {     1200,      4000,    3,      4, 0x10_0F_10, 0x42_10_42,      1,         0 },// Residue
            {      850,      1800,    3,      6, 0xA0_8F_1F, 0x42_35_20,      1,         1 },// Heavy Oil
            {      950,      1600,    3,      5, 0x87_6E_77, 0x42_24_24,      1,         1 },// Dense Oil
            {      750,      1400,    2,      8, 0xE4_AF_78, 0xB4_7F_00,      0,         1 },// Distilled Oil
            {      600,       800,    2,      7, 0xFF_AF_3F, 0xE0_7F_00,      0,         1 },// Dense Fuel
            {      700,      1000,    2,      7, 0xF2_A7_00, 0xC4_87_00,      0,         1 },// Mixed Heavy Fuels
            {      400,       600,    1,      8, 0xFF_FF_30, 0xE4_CF_00,      0,         1 },// Light Fuel
            {      650,       900,    1,      9, 0xF6_D7_00, 0xC4_B7_00,      0,         1 },// Mixed Light Fuels
            {      300,       500,    0,     10, 0xFA_F6_30, 0xE0_D9_00,      0,         1 },// Gas Fuel
        };//@formatter:on
        int index = 0;

        crudeOil = defineFluids(modBus, data[index++], "oil");
        oilResidue = defineFluids(modBus, data[index++], "oil_residue");
        oilHeavy = defineFluids(modBus, data[index++], "oil_heavy");
        oilDense = defineFluids(modBus, data[index++], "oil_dense");
        oilDistilled = defineFluids(modBus, data[index++], "oil_distilled");
        fuelDense = defineFluids(modBus, data[index++], "fuel_dense");
        fuelMixedHeavy = defineFluids(modBus, data[index++], "fuel_mixed_heavy");
        fuelLight = defineFluids(modBus, data[index++], "fuel_light");
        fuelMixedLight = defineFluids(modBus, data[index++], "fuel_mixed_light");
        fuelGaseous = defineFluids(modBus, data[index++], "fuel_gaseous");
    }

    private static BCFluid[] defineFluids(IEventBus modBus, int[] data, String name) {
        BCFluid[] arr = new BCFluid[3];
        for (int h = 0; h < 3; h++) {
            arr[h] = defineFluid(modBus, data, h, name);
        }
        return arr;
    }

    private static BCFluid defineFluid(IEventBus modBus, int[] data, int heat, String name) {
        final int density = data[0];
        final int baseViscosity = data[1];
        final int boilPoint = data[2];
        final int baseQuanta = data[3];
        final int texLight = data[4];
        final int texDark = data[5];
        final boolean sticky = BCEnergyConfig.oilIsSticky && data[6] == 1;
        final boolean flammable = BCEnergyConfig.enableOilBurn && data[7] == 1;

        String fullName = name + (heat == 0 ? "" : "_heat_" + heat);
        int tempAdjustedViscosity = baseViscosity * (4 - heat) / 4;
        int boilAdjustedDensity = density * (heat >= boilPoint ? -1 : 1);

        String fluidTexture = "buildcraftenergy:blocks/fluids/" + name + "_heat_" + heat;
        BCFluid def = new BCFluid(fullName, Identifier.parse(fluidTexture + "_still"),
            Identifier.parse(fluidTexture + "_flow"));
        def.setBlockName(name + "_heat_" + heat);
        def.setMapColour(getMapColor(texDark));
        def.setFlammable(flammable);
        def.setHeat(heat);
        def.setUnlocalizedName(name);
        def.setTemperature(300 + 20 * heat);
        def.setViscosity(tempAdjustedViscosity);
        def.setDensity(boilAdjustedDensity);
        def.setGaseous(def.getDensity() < 0);
        def.setColour(texLight, texDark);
        def.setHeatable(true);
        def.setLightOpacity(3);
        def.setSticky(sticky);
        // Distance that the fluid will travel: 1->16
        // Higher heat values travel a little further
        def.setSpreadDistance(baseQuanta + (baseQuanta > 6 ? heat : heat / 2));
        FluidManager.register(BCEnergy.MODID, modBus, def);

        allFluids.add(def);
        return def;
    }

    private static MapColor getMapColor(int color) {
        MapColor bestMapColor = MapColor.COLOR_BLACK;
        int currentDifference = Integer.MAX_VALUE;

        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        for (int id = 1; id < 64; id++) {
            MapColor mapColor = MapColor.byId(id);
            if (mapColor == null || mapColor == MapColor.NONE || mapColor.col == 0) {
                continue;
            }
            int mr = (mapColor.col >> 16) & 0xFF;
            int mg = (mapColor.col >> 8) & 0xFF;
            int mb = mapColor.col & 0xFF;

            int dr = mr - r;
            int dg = mg - g;
            int db = mb - b;

            int difference = dr * dr + dg * dg + db * db;

            if (difference < currentDifference) {
                currentDifference = difference;
                bestMapColor = mapColor;
            }
        }
        return bestMapColor;
    }
}
