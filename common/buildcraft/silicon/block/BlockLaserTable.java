/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import buildcraft.api.enums.EnumLaserTableType;
import buildcraft.api.mj.ILaserTargetBlock;

import buildcraft.lib.block.BlockBCTile_Neptune;

import buildcraft.silicon.BCSiliconGuis;

/** The assembly, advanced crafting, integration, charging and programming tables (each its own block). */
public class BlockLaserTable extends BlockBCTile_Neptune implements ILaserTargetBlock {
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 9, 16);

    private final EnumLaserTableType type;

    public BlockLaserTable(EnumLaserTableType type, BlockBehaviour.Properties properties, String id) {
        super(properties.noOcclusion(), id);
        this.type = type;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player,
        BlockHitResult hit) {
        BCSiliconGuis gui;
        switch (type) {
            case ASSEMBLY_TABLE:
                gui = BCSiliconGuis.ASSEMBLY_TABLE;
                break;
            case ADVANCED_CRAFTING_TABLE:
                gui = BCSiliconGuis.ADVANCED_CRAFTING_TABLE;
                break;
            case INTEGRATION_TABLE:
                gui = BCSiliconGuis.INTEGRATION_TABLE;
                break;
            default:
                return InteractionResult.PASS;
        }
        if (!world.isClientSide()) {
            gui.openGUI(player, pos);
        }
        return InteractionResult.SUCCESS;
    }
}
