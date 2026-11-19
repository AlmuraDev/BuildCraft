/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.item;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import buildcraft.lib.block.BlockBCBase_Neptune;
import buildcraft.lib.misc.LocaleUtil;

public class ItemBlockBC_Neptune extends BlockItem implements IItemBuildCraft {
    public final String id;

    public ItemBlockBC_Neptune(BlockBCBase_Neptune block, Item.Properties properties) {
        super(block, properties);
        this.id = "item." + block.id;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder,
        TooltipFlag flags) {
        super.appendHoverText(stack, context, display, builder, flags);
        IItemBuildCraft.appendTooltip(this, stack, context, builder, flags);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable Level world, List<String> tooltip, TooltipFlag flags) {
        // "tile.<name>.name" -> "tile.<name>.tip"
        String key = getDescriptionId();
        String tipId = (key.endsWith(".name") ? key.substring(0, key.length() - ".name".length()) : key) + ".tip";
        if (LocaleUtil.canLocalize(tipId)) {
            tooltip.add(ChatFormatting.GRAY + LocaleUtil.localize(tipId));
        } else if (flags.isAdvanced()) {
            tooltip.add(ChatFormatting.GRAY + tipId);
        }
    }
}
