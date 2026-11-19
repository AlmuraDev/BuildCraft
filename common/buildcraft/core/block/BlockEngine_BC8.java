/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.core.block;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import buildcraft.api.enums.EnumEngineType;
import buildcraft.api.properties.BuildCraftProperties;

import buildcraft.lib.engine.BlockEngineBase_BC8;
import buildcraft.lib.registry.TagManager;
import buildcraft.lib.registry.TagManager.EnumTagType;

public class BlockEngine_BC8 extends BlockEngineBase_BC8<EnumEngineType> {
    public BlockEngine_BC8(BlockBehaviour.Properties properties, String id) {
        super(properties, id);
    }

    @Override
    public EnumProperty<EnumEngineType> getEngineProperty() {
        return BuildCraftProperties.ENGINE_TYPE;
    }

    @Override
    public String getUnlocalizedName(EnumEngineType engine) {
        return TagManager.getTag("block.engine.bc." + engine.unlocalizedTag, EnumTagType.UNLOCALIZED_NAME);
    }
}
