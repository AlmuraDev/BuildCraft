/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import buildcraft.api.core.ActionResult;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

import net.minecraft.network.chat.Component;

import buildcraft.lib.BCLib;
import buildcraft.lib.BCLibProxy;
import buildcraft.lib.guide.GuideBook;
import buildcraft.lib.guide.GuideBookRegistry;
import buildcraft.lib.misc.AdvancementUtil;
import buildcraft.lib.misc.NBTUtilBC;

public class ItemGuide extends ItemBC_Neptune {
    private static final String DEFAULT_BOOK = "buildcraftcore:main";
    private static final Identifier ADVANCEMENT = Identifier.parse("buildcraftcore:guide");
    private static final String TAG_BOOK_NAME = "BookName";

    public ItemGuide(Item.Properties properties, String id) {
        super(properties, id);
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        AdvancementUtil.unlockAdvancement(player, ADVANCEMENT);
        if (world.isClientSide()) {
            BCLibProxy.getProxy().openGuide(getBookName(player.getItemInHand(hand)));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void addSubItems(NonNullList<ItemStack> items) {
        for (GuideBook book : GuideBookRegistry.INSTANCE.getAllEntries()) {
            ItemStack stack = new ItemStack(this);
            if (!book.name.toString().equals(ItemGuide.DEFAULT_BOOK)) {
                setBookName(stack, book.name.toString());
            }
            items.add(stack);
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        String bookName = getBookName(stack);
        GuideBook book = GuideBookRegistry.INSTANCE.getBook(bookName);
        if (book != null) {
            return book.title;
        }
        return super.getName(stack);
    }

    public static String getBookName(ItemStack stack) {
        CompoundTag nbt = NBTUtilBC.getItemData(stack);
        if (nbt == null || !nbt.contains(TAG_BOOK_NAME)) {
            // So that existing guide books continue to work
            return ItemGuide.DEFAULT_BOOK;
        }
        return nbt.getStringOr(TAG_BOOK_NAME, "");
    }

    public static void setBookName(ItemStack stack, String book) {
        NBTUtilBC.updateItemData(stack, nbt -> nbt.putString(TAG_BOOK_NAME, book));
    }
}
