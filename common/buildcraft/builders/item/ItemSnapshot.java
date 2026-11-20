/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.item;

import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import buildcraft.api.enums.EnumSnapshotType;

import buildcraft.lib.item.ItemBC_Neptune;
import buildcraft.lib.misc.HashUtil;
import buildcraft.lib.misc.LocaleUtil;
import buildcraft.lib.misc.NBTUtilBC;

import buildcraft.builders.snapshot.Snapshot;
import buildcraft.builders.snapshot.Snapshot.Header;

/** Blueprints and templates. Which one a stack is, is the "buildcraftbuilders:snapshot_type" component (1.12.2 used the
 * damage: 0/1 template clean/used, 2/3 blueprint clean/used, see {@link EnumItemSnapshotType}); a used one has the
 * snapshot's header in its custom data and stacks to 1. */
public class ItemSnapshot extends ItemBC_Neptune {
    public static final Codec<EnumSnapshotType> TYPE_CODEC = Codec.STRING.xmap(
        s -> "template".equals(s) ? EnumSnapshotType.TEMPLATE : EnumSnapshotType.BLUEPRINT,
        t -> t.name().toLowerCase(Locale.ROOT));

    public static DataComponentType<EnumSnapshotType> createTypeComponent() {
        return DataComponentType.<EnumSnapshotType> builder().persistent(TYPE_CODEC)
            .networkSynchronized(ByteBufCodecs.idMapper(i -> EnumSnapshotType.values()[i], Enum::ordinal)).build();
    }

    public ItemSnapshot(Item.Properties properties, String id) {
        super(properties.stacksTo(16), id);
    }

    public static EnumSnapshotType getType(ItemStack stack) {
        EnumSnapshotType type = stack.get(buildcraft.builders.BCBuildersItems.SNAPSHOT_TYPE);
        return type == null ? EnumSnapshotType.BLUEPRINT : type;
    }

    public ItemStack getClean(EnumSnapshotType snapshotType) {
        ItemStack stack = new ItemStack(this);
        stack.set(buildcraft.builders.BCBuildersItems.SNAPSHOT_TYPE, snapshotType);
        return stack;
    }

    public ItemStack getUsed(EnumSnapshotType snapshotType, Header header) {
        CompoundTag nbt = new CompoundTag();
        nbt.put("header", header.serializeNBT());
        ItemStack stack = getClean(snapshotType);
        NBTUtilBC.setItemData(stack, nbt);
        stack.set(DataComponents.MAX_STACK_SIZE, 1);
        return stack;
    }

    @Nullable
    public Header getHeader(ItemStack stack) {
        if (stack.getItem() instanceof ItemSnapshot) {
            CompoundTag nbt = NBTUtilBC.getItemData(stack);
            if (nbt.contains("header")) {
                return new Header(nbt.getCompoundOrEmpty("header"));
            }
        }
        return null;
    }

    @Override
    public void addSubItems(NonNullList<ItemStack> subItems) {
        subItems.add(getClean(EnumSnapshotType.BLUEPRINT));
        subItems.add(getClean(EnumSnapshotType.TEMPLATE));
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(getType(stack) == EnumSnapshotType.BLUEPRINT ? "item.blueprintItem.name"
            : "item.templateItem.name");
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable Level world, List<String> tooltip, TooltipFlag flag) {
        Snapshot.Header header = getHeader(stack);
        if (header == null) {
            tooltip.add(LocaleUtil.localize("item.blueprint.blank"));
        } else {
            tooltip.add(header.name);
            Player owner = world == null ? null : header.getOwnerPlayer(world);
            if (owner != null) {
                tooltip.add(LocaleUtil.localize("item.blueprint.author") + " " + owner.getName().getString());
            }
            if (flag.isAdvanced()) {
                tooltip.add("Hash: " + HashUtil.convertHashToString(header.key.hash));
                tooltip.add("Date: " + header.created);
                tooltip.add("Owner UUID: " + header.owner);
            }
        }
    }

    /** 1.12.2's damage values (for converting 3.5 stacks). */
    public enum EnumItemSnapshotType implements StringRepresentable {
        TEMPLATE_CLEAN(EnumSnapshotType.TEMPLATE, false),
        TEMPLATE_USED(EnumSnapshotType.TEMPLATE, true),
        BLUEPRINT_CLEAN(EnumSnapshotType.BLUEPRINT, false),
        BLUEPRINT_USED(EnumSnapshotType.BLUEPRINT, true);

        public final EnumSnapshotType snapshotType;
        public final boolean used;

        EnumItemSnapshotType(EnumSnapshotType snapshotType, boolean used) {
            this.snapshotType = snapshotType;
            this.used = used;
        }

        @Override
        public String getSerializedName() {
            return getName();
        }

        public String getName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static EnumItemSnapshotType fromDamage(int damage) {
            return values()[Math.abs(damage) % values().length];
        }
    }
}
