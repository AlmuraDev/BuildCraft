/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.phys.Vec3;

/** The block of a {@link BCFluid}: flammable fluids burn, sticky ones slow entities like cobwebs. */
public class BCFluidBlock extends LiquidBlock {
    private final BCFluid definition;

    public BCFluidBlock(BCFluid definition, FlowingFluid fluid, BlockBehaviour.Properties properties) {
        super(fluid, properties);
        this.definition = definition;
    }

    public BCFluid getDefinition() {
        return definition;
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
        return definition.isFlammable() ? 200 : 0;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
        return definition.isFlammable() ? 200 : 0;
    }

    @Override
    protected void entityInside(BlockState state, Level world, BlockPos pos, Entity entity, InsideBlockEffectApplier effects,
        boolean isPrecise) {
        super.entityInside(state, world, pos, entity, effects, isPrecise);
        if (definition.isSticky()) {
            entity.makeStuckInBlock(state, new Vec3(0.25D, 0.05F, 0.25D));
        }
    }
}
