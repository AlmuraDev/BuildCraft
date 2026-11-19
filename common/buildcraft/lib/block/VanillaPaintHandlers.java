/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.block;

import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import buildcraft.api.blocks.CustomPaintHelper;
import buildcraft.api.blocks.ICustomPaintHandler;

/** Paintbrush support for vanilla blocks. 1.12.2 painted blocks by changing a colour property; on 26.3 each colour is its
 * own block ("red_stained_glass"), so painting swaps the block for the same family's block of the new colour (keeping
 * the other state properties, like a pane's connections). Blocks with contents (shulker boxes) or two halves (beds)
 * aren't painted. Clearing paint turns glass, panes and terracotta
 * back to their plain block. */
public class VanillaPaintHandlers {

    public static void fmlInit() {
        registerFamily(Blocks.GLASS, "stained_glass");
        registerFamily(Blocks.GLASS_PANE, "stained_glass_pane");
        registerFamily(Blocks.TERRACOTTA, "terracotta");
        registerFamily(null, "wool");
        registerFamily(null, "carpet");
        registerFamily(null, "concrete");
        registerFamily(null, "concrete_powder");
        registerFamily(null, "glazed_terracotta");
        registerFamily(null, "candle");
    }

    /** @param clear The family's uncoloured block, or null if it has none (so its paint can't be cleared).
     * @param suffix What the family's blocks are called after their colour ("stained_glass"). */
    private static void registerFamily(@Nullable Block clear, String suffix) {
        Map<DyeColor, Block> dyed = new EnumMap<>(DyeColor.class);
        Map<Block, DyeColor> colours = new IdentityHashMap<>();
        for (DyeColor colour : DyeColor.values()) {
            Identifier id = Identifier.withDefaultNamespace(colour.getSerializedName() + "_" + suffix);
            Block block = BuiltInRegistries.BLOCK.getValue(id);
            if (block != null && block != Blocks.AIR) {
                dyed.put(colour, block);
                colours.put(block, colour);
            }
        }
        if (dyed.isEmpty()) {
            return;
        }
        ICustomPaintHandler handler = (world, pos, state, hitPos, hitSide, to) -> {
            Block current = state.getBlock();
            boolean isClear = current == clear;
            if (!isClear && !colours.containsKey(current)) {
                return InteractionResult.PASS;
            }
            if (to == null ? (isClear || clear == null) : to == colours.get(current)) {
                return InteractionResult.FAIL;
            }
            Block target = to == null ? clear : dyed.get(to);
            if (target == null) {
                return InteractionResult.FAIL;
            }
            BlockState painted = target.withPropertiesOf(state);
            world.setBlockAndUpdate(pos, painted);
            return InteractionResult.SUCCESS;
        };
        if (clear != null) {
            CustomPaintHelper.INSTANCE.registerHandler(clear, handler);
        }
        for (Block block : dyed.values()) {
            CustomPaintHelper.INSTANCE.registerHandler(block, handler);
        }
    }
}
