/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.StringRepresentable;

import net.neoforged.neoforge.common.conditions.ICondition;

import buildcraft.api.enums.EnumEngineType;
import buildcraft.api.properties.BuildCraftProperties;

import buildcraft.core.BCCoreBlocks;

/** A recipe condition that holds when the given engine type exists: {"type": "buildcraftcore:engine_enabled",
 * "engine": "rf"}. Engines can be turned off in the config (the RF engine is off by default), and a disabled engine
 * has no tile type, so its recipe must not load. */
public record EngineEnabledCondition(EnumEngineType engine) implements ICondition {
    public static final MapCodec<EngineEnabledCondition> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        StringRepresentable.fromEnum(EnumEngineType::values).fieldOf("engine").forGetter(EngineEnabledCondition::engine)
    ).apply(i, EngineEnabledCondition::new));

    @Override
    public boolean test(IContext context) {
        return BCCoreBlocks.engine != null && BCCoreBlocks.engine.getTileType(
            BCCoreBlocks.engine.defaultBlockState().setValue(BuildCraftProperties.ENGINE_TYPE, engine)) != null;
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }
}
