/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.client.guide.node;

import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.ItemStack;

import buildcraft.api.core.BCLog;
import buildcraft.api.core.NbtStacks;

/** Helpers the guide's page loaders used from 1.12.2's TextFormatting and item APIs, which 26.3 no longer has. */
public final class GuideFormatting {
    private GuideFormatting() {}

    /** @return The formatting named like 1.12.2's friendly names ("dark_green", "darkgreen", "bold"), or null. */
    @Nullable
    public static ChatFormatting byName(@Nullable String name) {
        if (name == null) {
            return null;
        }
        String wanted = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
        for (ChatFormatting format : ChatFormatting.values()) {
            if (format.name().toLowerCase(Locale.ROOT).replace("_", "").equals(wanted)) {
                return format;
            }
        }
        return null;
    }

    /** @return True for the 16 colours (false for bold, italic, reset...). */
    public static boolean isColor(ChatFormatting format) {
        return format.ordinal() < ChatFormatting.OBFUSCATED.ordinal();
    }

    public static String friendlyName(ChatFormatting format) {
        return format.name().toLowerCase(Locale.ROOT);
    }

    /** Reads a guide page's item reference: a 1.12.2 item id, count, metadata and NBT, upgraded to a 26.3 stack the way
     * old saved stacks are (so metadata maps where the data fixer knows it).
     *
     * @param nbt 1.12.2 NBT in text form, or null.
     * @return The stack, or empty if the item doesn't exist. */
    public static ItemStack legacyStack(String id, int count, int meta, @Nullable String nbt) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id.trim());
        tag.putByte("Count", (byte) count);
        tag.putShort("Damage", (short) Math.max(0, meta));
        if (nbt != null && !nbt.isBlank()) {
            try {
                tag.put("tag", TagParser.parseCompoundFully(nbt));
            } catch (Exception e) {
                BCLog.logger.warn("[lib.guide.loader] " + nbt + " was not a valid nbt tag: " + e.getMessage());
            }
        }
        try {
            return NbtStacks.readItemStack(tag);
        } catch (RuntimeException e) {
            BCLog.logger.warn("[lib.guide.loader] Couldn't read the item " + tag + ": " + e.getMessage());
            return ItemStack.EMPTY;
        }
    }
}
