/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.client.guide.loader;

import buildcraft.lib.client.guide.node.GuideFormatting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.TagParser;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.resources.Identifier;


import buildcraft.api.core.BCDebugging;
import buildcraft.api.core.BCLog;
import buildcraft.api.registry.IScriptableRegistry.OptionallyDisabled;

import buildcraft.lib.client.guide.entry.PageEntry;
import buildcraft.lib.client.guide.parts.GuidePageFactory;

public enum MarkdownPageLoader implements IPageLoaderText {
    INSTANCE;

    public static final boolean DEBUG = BCDebugging.shouldDebugLog("lib.markdown");

    public static ItemStack loadComplexItemStack(String line) {
        OptionallyDisabled<ItemStack> stackq = parseItemStack(line);
        if (stackq.isPresent()) {
            return stackq.get();
        }
        BCLog.logger.warn("[lib.guide.loader.markdown] " + stackq.getDisabledReason());
        return ItemStack.EMPTY;
    }

    /** Reads "id[,count[,meta[,nbt]]]" (1.12.2's format; the metadata and NBT are upgraded like old saved stacks). */
    public static OptionallyDisabled<ItemStack> parseItemStack(String line) {
        String[] args = line.split(",", 4);
        if (args.length == 0 || args[0].isBlank()) {
            return new OptionallyDisabled<>(line + " was not a valid complex item string!");
        }
        int stackSize = 1;
        int meta = 0;
        try {
            if (args.length > 1) {
                stackSize = Integer.parseInt(args[1].trim());
            }
            if (args.length > 2) {
                meta = Integer.parseInt(args[2].trim());
            }
        } catch (NumberFormatException nfe) {
            return new OptionallyDisabled<>(line + " has an invalid number: " + nfe.getLocalizedMessage());
        }
        ItemStack stack = GuideFormatting.legacyStack(args[0], stackSize, meta, args.length > 3 ? args[3] : null);
        if (stack.isEmpty()) {
            return new OptionallyDisabled<>(args[0] + " was not a valid item!");
        }
        return new OptionallyDisabled<>(stack);
    }

    @Override
    public GuidePageFactory loadPage(BufferedReader reader, Identifier name, PageEntry<?> entry, ProfilerFiller prof)
        throws IOException {
        prof.push("md");
        StringBuilder replaced = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            // First replace special tags (at the start of a line) with xml ones
            line = replaceSpecialForXml(line);
            replaced.append(line);
            replaced.append('\n');
        }

        BufferedReader nReader = new BufferedReader(new StringReader(replaced.toString()));
        prof.pop();
        return XmlPageLoader.INSTANCE.loadPage(nReader, name, entry, prof);
    }

    private static String replaceSpecialForXml(String line) {
        if (line.startsWith("$[special.") && line.indexOf(']') > 0) {
            int end = line.indexOf(']');
            String post = line.substring("$[special.".length(), end);
            switch (post) {
                case "new_page": {
                    BCLog.logger.warn(
                        "[lib.guide.markdown] Found deprecated element '" + line
                            + "', it should be replaced with '<new_page/>'"
                    );
                    return "<new_page/>";
                }
                case "all_crafting": {
                    String stack = line.substring(end + 1);
                    String additional = "";
                    if (stack.startsWith("\"") && stack.endsWith("\"")) {
                        stack = stack.substring(1, stack.length() - 1);
                    }
                    if (stack.startsWith("(") && stack.endsWith(")")) {
                        stack = stack.substring(1, stack.length() - 1);
                    } else if (stack.startsWith("{") && stack.contains("}")) {
                        int curlyStart = stack.indexOf('}');
                        stack = stack.substring(1, curlyStart);
                        String[] split = stack.split(",");
                        if (split.length > 0) {
                            stack = split[0];
                        }
                        if (split.length > 1) {
                            additional += " count=\"" + split[1] + "\"";
                        }
                        if (split.length > 2) {
                            additional += " data=\"" + split[2] + "\"";
                        }
                    }
                    String str = "<recipes_usages stack=\"" + stack + "\"" + additional + "/>";
                    BCLog.logger.warn(
                        "[lib.guide.markdown] Found deprecated element '" + line + "', it should be replaced with '"
                            + str + "'"
                    );
                    return str;
                }
                default:
            }
        } else if (line.startsWith("#")) {
            int level = -1;
            while (line.startsWith("#")) {
                line = line.substring(1);
                level++;
            }
            line = line.trim();
            if (level == 0) {
                return "<chapter name=\"" + line + "\"/>";
            } else {
                return "<chapter name=\"" + line + "\" level=\"" + level + "\"/>";
            }
        }
        return line;
    }
}
