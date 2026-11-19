/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.fluid;

import javax.annotation.Nullable;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

import buildcraft.lib.misc.LocaleUtil;

/** The definition of one of BuildCraft's fluids (oil, fuels...). 1.12.2's BCFluid was a Forge fluid itself; on 26.3 a
 * fluid is several registry objects (a fluid type, source and flowing fluids, a block and a bucket), which
 * {@link FluidManager} creates from this definition when the registries open. Set everything before then. */
public class BCFluid {
    private final String name;
    private final Identifier still, flowing;
    private int colour = 0xFFFFFFFF, light = 0xFF_FF_FF_FF, dark = 0xFF_FF_FF_FF;
    private boolean isFlammable = false;
    private int lightOpacity = 0;
    private MapColor mapColour = MapColor.COLOR_BLACK;
    private int heat;
    private boolean heatable;
    private String blockName;
    private String unlocalizedName;
    private int density = 1000, viscosity = 1000, temperature = 300, luminosity = 0;
    private boolean gaseous;
    private boolean sticky;
    /** How far the fluid flows from a source: 1 to 16 blocks (1.12.2's quanta per block). */
    private int spreadDistance = 8;

    // Set by FluidManager when the registry objects are created
    FluidType fluidType;
    FlowingFluid source, flowingFluid;
    BCFluidBlock block;
    BucketItem bucket;

    public BCFluid(String fluidName, Identifier still, Identifier flowing) {
        this.name = fluidName;
        this.still = still;
        this.flowing = flowing;
        this.blockName = fluidName;
        this.unlocalizedName = fluidName;
    }

    /** @return The 1.12.2 fluid name ("oil", "fuel_light_heat_1"...), which is also this fluid's registry path. */
    public String getName() {
        return name;
    }

    public Identifier getStill() {
        return still;
    }

    public Identifier getFlowing() {
        return flowing;
    }

    // Registry objects (only valid once registered)

    public FluidType getFluidType() {
        return fluidType;
    }

    /** @return The source fluid: what fluid stacks of this fluid hold. */
    public FlowingFluid getSource() {
        return source;
    }

    public FlowingFluid getFlowingFluid() {
        return flowingFluid;
    }

    @Nullable
    public BCFluidBlock getBlock() {
        return block;
    }

    @Nullable
    public BucketItem getBucket() {
        return bucket;
    }

    public FluidStack createStack(int amount) {
        return new FluidStack(source, amount);
    }

    /** @return true if the given fluid is this one (source or flowing). */
    public boolean is(Fluid fluid) {
        return fluid == source || fluid == flowingFluid;
    }

    // Naming

    public void setUnlocalizedName(String name) {
        this.unlocalizedName = name;
    }

    public String getUnlocalizedName() {
        return "fluid." + unlocalizedName;
    }

    public String getBareLocalizedName() {
        return LocaleUtil.localize(getUnlocalizedName());
    }

    /** @return The name shown for the fluid, with its heat ("Hot Oil") for heatable fluids. */
    public String getLocalizedName() {
        if (heat <= 0 && !isHeatable()) return getBareLocalizedName();
        return LocaleUtil.localize("buildcraft.fluid.heat_" + heat, getBareLocalizedName());
    }

    // Properties

    public void setMapColour(MapColor mapColour) {
        this.mapColour = mapColour;
    }

    public final MapColor getMapColour() {
        return this.mapColour;
    }

    public void setFlammable(boolean isFlammable) {
        this.isFlammable = isFlammable;
    }

    public final boolean isFlammable() {
        return isFlammable;
    }

    public void setLightOpacity(int lightOpacity) {
        this.lightOpacity = lightOpacity;
    }

    public final int getLightOpacity() {
        return lightOpacity;
    }

    public void setBlockName(String blockName) {
        this.blockName = blockName;
    }

    public String getBlockName() {
        return blockName;
    }

    public int getColor() {
        return colour;
    }

    public int getLightColour() {
        return light;
    }

    public int getDarkColour() {
        return dark;
    }

    public BCFluid setColour(int colour) {
        this.colour = colour;
        return this;
    }

    public BCFluid setColour(int light, int dark) {
        this.light = light;
        this.dark = dark;
        this.colour = 0xFF_FF_FF_FF;
        return this;
    }

    public BCFluid setHeat(int heat) {
        this.heat = heat;
        return this;
    }

    public int getHeatValue() {
        return heat;
    }

    public BCFluid setHeatable(boolean value) {
        heatable = value;
        return this;
    }

    public boolean isHeatable() {
        return heatable;
    }

    public BCFluid setDensity(int density) {
        this.density = density;
        return this;
    }

    public int getDensity() {
        return density;
    }

    public BCFluid setViscosity(int viscosity) {
        this.viscosity = viscosity;
        return this;
    }

    public int getViscosity() {
        return viscosity;
    }

    public BCFluid setTemperature(int temperature) {
        this.temperature = temperature;
        return this;
    }

    public int getTemperature() {
        return temperature;
    }

    public BCFluid setLuminosity(int luminosity) {
        this.luminosity = luminosity;
        return this;
    }

    public int getLuminosity() {
        return luminosity;
    }

    public BCFluid setGaseous(boolean gaseous) {
        this.gaseous = gaseous;
        return this;
    }

    public boolean isGaseous() {
        return gaseous;
    }

    /** Sticky fluids slow entities in them, like cobwebs. */
    public BCFluid setSticky(boolean sticky) {
        this.sticky = sticky;
        return this;
    }

    public boolean isSticky() {
        return sticky;
    }

    public BCFluid setSpreadDistance(int spreadDistance) {
        this.spreadDistance = Math.max(1, Math.min(16, spreadDistance));
        return this;
    }

    public int getSpreadDistance() {
        return spreadDistance;
    }
}
