/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.item;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.fluids.FluidStack;

import buildcraft.api.core.BCLog;
import buildcraft.api.core.InvalidInputDataException;
import buildcraft.api.schematics.ISchematicBlock;
import buildcraft.api.schematics.SchematicBlockContext;

import buildcraft.lib.inventory.InventoryWrapper;
import buildcraft.lib.item.ItemBC_Neptune;
import buildcraft.lib.misc.NBTUtilBC;
import buildcraft.lib.misc.SoundUtil;
import buildcraft.lib.misc.StackUtil;

import buildcraft.builders.snapshot.SchematicBlockManager;

/** Copies one block (shift-click clears it). A used schematic has the block's schematic in its custom data under
 * "schematic" and stacks to 1; 1.12.2 also marked it with damage 1. */
public class ItemSchematicSingle extends ItemBC_Neptune {
    public static final String NBT_KEY = "schematic";

    public ItemSchematicSingle(Item.Properties properties, String id) {
        super(properties.stacksTo(16), id);
    }

    public static boolean isUsed(ItemStack stack) {
        return NBTUtilBC.getItemData(stack).contains(NBT_KEY);
    }

    private static void clear(ItemStack stack) {
        CompoundTag itemData = NBTUtilBC.getItemData(stack);
        itemData.remove(NBT_KEY);
        NBTUtilBC.setItemData(stack, itemData.isEmpty() ? null : itemData);
        stack.remove(DataComponents.MAX_STACK_SIZE);
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = StackUtil.asNonNull(player.getItemInHand(hand));
        if (world.isClientSide()) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            clear(stack);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        InteractionHand hand = context.getHand();
        Direction side = context.getClickedFace();
        if (world.isClientSide() || player == null) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            clear(stack);
            return InteractionResult.SUCCESS;
        }
        if (!isUsed(stack)) {
            BlockState state = world.getBlockState(pos);
            ISchematicBlock schematicBlock = SchematicBlockManager.getSchematicBlock(new SchematicBlockContext(
                world,
                pos,
                pos,
                state,
                state.getBlock()
            ));
            if (schematicBlock.isAir()) {
                return InteractionResult.FAIL;
            }
            CompoundTag schematicNbt = SchematicBlockManager.writeToNBT(schematicBlock);
            NBTUtilBC.updateItemData(stack, nbt -> nbt.put(NBT_KEY, schematicNbt));
            stack.set(DataComponents.MAX_STACK_SIZE, 1);
            return InteractionResult.SUCCESS;
        } else {
            BlockPos placePos = pos;
            boolean replaceable = world.getBlockState(pos).canBeReplaced();
            if (!replaceable) {
                placePos = placePos.relative(side);
            }
            if (!world.getBlockState(placePos).canBeReplaced()) {
                return InteractionResult.FAIL;
            }
            if (replaceable && !world.isEmptyBlock(placePos)) {
                world.removeBlock(placePos, false);
            }
            try {
                ISchematicBlock schematicBlock = getSchematic(stack);
                if (schematicBlock != null) {
                    if (!schematicBlock.isBuilt(world, placePos) && schematicBlock.canBuild(world, placePos)) {
                        List<FluidStack> requiredFluids = schematicBlock.computeRequiredFluids();
                        List<ItemStack> requiredItems = schematicBlock.computeRequiredItems();
                        if (requiredFluids.isEmpty()) {
                            InventoryWrapper itemTransactor = new InventoryWrapper(player.getInventory());
                            if (StackUtil.mergeSameItems(requiredItems).stream().noneMatch(s ->
                                itemTransactor.extract(
                                    extracted -> StackUtil.canMerge(s, extracted),
                                    s.getCount(),
                                    s.getCount(),
                                    true
                                ).isEmpty()
                            )) {
                                if (schematicBlock.build(world, placePos)) {
                                    StackUtil.mergeSameItems(requiredItems).forEach(s ->
                                        itemTransactor.extract(
                                            extracted -> StackUtil.canMerge(s, extracted),
                                            s.getCount(),
                                            s.getCount(),
                                            false
                                        )
                                    );
                                    SoundUtil.playBlockPlace(world, placePos);
                                    player.swing(hand, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
                                    return InteractionResult.SUCCESS;
                                }
                            } else {
                                player.sendOverlayMessage(Component.literal("Not enough items. Total needed: "
                                    + StackUtil.mergeSameItems(requiredItems).stream()
                                        .map(s -> s.getHoverName().getString() + " x " + s.getCount())
                                        .collect(Collectors.joining(", "))));
                            }
                        } else {
                            player.sendOverlayMessage(Component.literal("Schematic requires fluids"));
                        }
                    }
                }
            } catch (InvalidInputDataException e) {
                player.sendOverlayMessage(Component.literal("Invalid schematic: " + e.getMessage()));
                e.printStackTrace();
            }
            return InteractionResult.FAIL;
        }
    }

    public static ISchematicBlock getSchematic(@Nonnull ItemStack stack) throws InvalidInputDataException {
        if (stack.getItem() instanceof ItemSchematicSingle) {
            return SchematicBlockManager.readFromNBT(NBTUtilBC.getItemData(stack).getCompoundOrEmpty(NBT_KEY));
        }
        return null;
    }

    public static ISchematicBlock getSchematicSafe(@Nonnull ItemStack stack) {
        try {
            return getSchematic(stack);
        } catch (InvalidInputDataException e) {
            BCLog.logger.warn("Invalid schematic " + e.getMessage());
            return null;
        }
    }
}
