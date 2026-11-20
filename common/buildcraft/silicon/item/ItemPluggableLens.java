/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.item;

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import buildcraft.api.transport.IItemPluggable;
import buildcraft.api.transport.pipe.IFlowItems;
import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.api.transport.pluggable.PipePluggable;
import buildcraft.api.transport.pluggable.PluggableDefinition;

import buildcraft.lib.item.ItemBC_Neptune;
import buildcraft.lib.misc.ColourUtil;
import buildcraft.lib.misc.LocaleUtil;
import buildcraft.lib.misc.SoundUtil;

import buildcraft.silicon.BCSiliconItems;
import buildcraft.silicon.BCSiliconPlugs;
import buildcraft.silicon.plug.PluggableLens;

/** Lenses and filters, clear or in a colour. The variant is the "buildcraftsilicon:lens" component (1.12.2 used the
 * damage: colour's dye damage, +16 for a filter; 32 clear lens, 33 clear filter; see {@link LensData#fromDamage}). */
public class ItemPluggableLens extends ItemBC_Neptune implements IItemPluggable {

    public ItemPluggableLens(Item.Properties properties, String id) {
        super(properties, id);
    }

    public static LensData getData(ItemStack stack) {
        LensData data = stack.get(BCSiliconItems.LENS);
        return data == null ? LensData.CLEAR_LENS : data;
    }

    @Nonnull
    public ItemStack getStack(@Nullable DyeColor colour, boolean isFilter) {
        return getStack(new LensData(colour, isFilter));
    }

    @Nonnull
    public ItemStack getStack(LensData variant) {
        ItemStack stack = new ItemStack(this);
        stack.set(BCSiliconItems.LENS, variant);
        return stack;
    }

    @Override
    public PipePluggable onPlace(@Nonnull ItemStack stack, IPipeHolder holder, Direction side, Player player,
        InteractionHand hand) {
        IPipe pipe = holder.getPipe();
        if (pipe == null || !(pipe.getFlow() instanceof IFlowItems)) {
            return null;
        }
        LensData data = getData(stack);
        SoundUtil.playBlockPlace(holder.getPipeWorld(), holder.getPipePos(), Blocks.STONE.defaultBlockState());
        PluggableDefinition def = BCSiliconPlugs.lens;
        return new PluggableLens(def, holder, side, data.colour, data.isFilter);
    }

    @Override
    public Component getName(ItemStack stack) {
        LensData data = getData(stack);
        String colour = data.colour == null ? LocaleUtil.localize("color.clear")
            : ColourUtil.getTextFullTooltipSpecial(data.colour);
        String first = LocaleUtil.localize(data.isFilter ? "item.Filter.name" : "item.Lens.name");
        return Component.literal(colour + " " + first);
    }

    @Override
    public void addSubItems(NonNullList<ItemStack> subItems) {
        for (int i = 0; i < 34; i++) {
            subItems.add(getStack(LensData.fromDamage(i)));
        }
    }

    public static final class LensData {
        public static final LensData CLEAR_LENS = new LensData(null, false);

        public static final Codec<LensData> CODEC = RecordCodecBuilder.create(i -> i.group(
            DyeColor.CODEC.optionalFieldOf("colour").forGetter(d -> Optional.ofNullable(d.colour)),
            Codec.BOOL.optionalFieldOf("filter", false).forGetter(d -> d.isFilter)
        ).apply(i, (colour, filter) -> new LensData(colour.orElse(null), filter)));

        public static final StreamCodec<RegistryFriendlyByteBuf, LensData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(DyeColor.STREAM_CODEC), d -> Optional.ofNullable(d.colour),
            ByteBufCodecs.BOOL, d -> d.isFilter,
            (colour, filter) -> new LensData(colour.orElse(null), filter));

        @Nullable
        public final DyeColor colour;
        public final boolean isFilter;

        public LensData(@Nullable DyeColor colour, boolean isFilter) {
            this.colour = colour;
            this.isFilter = isFilter;
        }

        /** The variant a 1.12.2 lens damage value meant (for converting 3.5 stacks). */
        public static LensData fromDamage(int damage) {
            if (damage >= 32) {
                return new LensData(null, damage == 33);
            }
            return new LensData(ColourUtil.COLOURS[15 - (damage & 15)], damage >= 16);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof LensData other && other.colour == colour && other.isFilter == isFilter;
        }

        @Override
        public int hashCode() {
            return (colour == null ? 16 : colour.ordinal()) * 2 + (isFilter ? 1 : 0);
        }
    }

    /** The "buildcraftsilicon:lens" component type (registered in BCSiliconItems). */
    public static DataComponentType<LensData> createComponent() {
        return DataComponentType.<LensData> builder().persistent(LensData.CODEC)
            .networkSynchronized(LensData.STREAM_CODEC).build();
    }
}
