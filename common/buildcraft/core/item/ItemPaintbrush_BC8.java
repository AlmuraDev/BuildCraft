/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.core.item;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import buildcraft.api.blocks.CustomPaintHelper;

import buildcraft.lib.item.ItemBC_Neptune;
import buildcraft.lib.misc.ColourUtil;
import buildcraft.lib.misc.NBTUtilBC;
import buildcraft.lib.misc.ParticleUtil;
import buildcraft.lib.misc.SoundUtil;
import buildcraft.lib.misc.StackUtil;
import buildcraft.lib.misc.VecUtil;

/** The paintbrush. Its colour is the "buildcraftcore:paint_colour" component (1.12.2 used the damage value: 0 for clean,
 * colour + 1 otherwise) and its uses are kept in its data as on 1.12.2. */
public class ItemPaintbrush_BC8 extends ItemBC_Neptune {
    private static final String DAMAGE = "damage";
    private static final int MAX_USES = 64;

    /** Registered by core as "buildcraftcore:paint_colour". */
    public static final DataComponentType<DyeColor> COLOUR = DataComponentType.<DyeColor> builder()
        .persistent(DyeColor.CODEC)
        .networkSynchronized(DyeColor.STREAM_CODEC)
        .build();

    public ItemPaintbrush_BC8(Item.Properties properties, String id) {
        super(properties.stacksTo(1), id);
    }

    @Override
    public void addSubItems(NonNullList<ItemStack> subItems) {
        subItems.add(new ItemStack(this));
        for (DyeColor colour : DyeColor.values()) {
            subItems.add(new Brush(colour).save());
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        InteractionHand hand = context.getHand();
        Direction facing = context.getClickedFace();
        float hitX = (float) (context.getClickLocation().x - pos.getX());
        float hitY = (float) (context.getClickLocation().y - pos.getY());
        float hitZ = (float) (context.getClickLocation().z - pos.getZ());
        if (player == null) {
            return InteractionResult.PASS;
        }
        ItemStack stack = StackUtil.asNonNull(player.getItemInHand(hand));
        Brush brush = new Brush(stack);
        Vec3 hitPos = VecUtil.add(new Vec3(hitX, hitY, hitZ), pos);
        if (brush.useOnBlock(world, pos, world.getBlockState(pos), hitPos, facing, player)) {
            ItemStack newStack = brush.save(stack);
            if (!newStack.isEmpty()) {
                player.setItemInHand(hand, newStack);
            }
            // We just changed the damage NBT value
            player.inventoryMenu.broadcastChanges();
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }

    public Brush getBrushFromStack(ItemStack stack) {
        return new Brush(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        Brush brush = getBrushFromStack(stack);
        Component name = super.getName(stack);
        if (brush.colour == null) {
            return name;
        }
        return Component.literal(ColourUtil.getTextFullTooltip(brush.colour) + " ").append(name);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        Brush brush = new Brush(stack);
        return brush.colour != null && brush.usesLeft < MAX_USES;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        Brush brush = new Brush(stack);
        return Mth.clamp(Math.round(13.0F * brush.usesLeft / MAX_USES), 0, 13);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        Brush brush = new Brush(stack);
        return Mth.hsvToRgb(Math.max(0.0F, brush.usesLeft / (float) MAX_USES) / 3.0F, 1.0F, 1.0F);
    }

    /** Delegate class for handling */
    public class Brush {
        @Nullable
        public DyeColor colour;
        public int usesLeft;

        public Brush(DyeColor colour) {
            this.colour = colour;
            usesLeft = MAX_USES;
        }

        public Brush(ItemStack stack) {
            colour = stack.get(COLOUR);
            if (colour != null) {
                usesLeft = MAX_USES - NBTUtilBC.getItemData(stack).getByteOr(DAMAGE, (byte) 0);
            } else {
                usesLeft = 0;
            }
        }

        @Nonnull
        public ItemStack save() {
            return save(StackUtil.EMPTY);
        }

        /** @return A new stack if the existing one couldn't hold this brush, or empty if it was changed in place. */
        @Nonnull
        public ItemStack save(@Nonnull ItemStack existing) {
            ItemStack stack = existing.isEmpty() ? new ItemStack(ItemPaintbrush_BC8.this) : existing;
            DyeColor savedColour = usesLeft <= 0 ? null : colour;
            if (savedColour == null) {
                stack.remove(COLOUR);
            } else {
                stack.set(COLOUR, savedColour);
            }
            NBTUtilBC.updateItemData(stack, nbt -> {
                if (usesLeft != MAX_USES && savedColour != null) {
                    nbt.putByte(DAMAGE, (byte) (MAX_USES - usesLeft));
                } else {
                    nbt.remove(DAMAGE);
                }
            });
            return stack == existing ? StackUtil.EMPTY : stack;
        }

        public boolean useOnBlock(Level world, BlockPos pos, BlockState state, Vec3 hitPos, Direction side, Player player) {
            if (colour != null && usesLeft <= 0) {
                return false;
            }

            InteractionResult result = CustomPaintHelper.INSTANCE.attemptPaintBlock(world, pos, state, hitPos, side, colour);

            if (result == InteractionResult.SUCCESS) {
                ParticleUtil.showChangeColour(world, hitPos, colour);
                SoundUtil.playChangeColour(world, pos, colour);

                if (!player.isCreative()) {
                    usesLeft--;
                }

                if (usesLeft <= 0) {
                    colour = null;
                    usesLeft = 0;
                }
                return true;
            }
            return false;
        }

        @Override
        public String toString() {
            return "[" + usesLeft + " of " + (colour == null ? "nothing" : colour.getName()) + "]";
        }
    }
}
