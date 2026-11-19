/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.item;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import buildcraft.api.transport.pipe.IItemPipe;
import buildcraft.api.transport.pipe.PipeApi;
import buildcraft.api.transport.pipe.PipeDefinition;

import buildcraft.lib.item.IItemBuildCraft;
import buildcraft.lib.misc.ColourUtil;
import buildcraft.lib.misc.LocaleUtil;

import buildcraft.transport.BCTransportItems;

/** The item of one pipe type: it places the pipe holder block with that pipe. Coloured pipes carry the
 * "buildcrafttransport:colour" component (1.12.2 used the damage value: 0 for none, colour + 1 otherwise). */
public class ItemPipeHolder extends BlockItem implements IItemBuildCraft, IItemPipe {
    public final PipeDefinition definition;
    private final String id;

    /** @param properties Must already carry the item's id (see buildcraft.lib.registry.RegistrationHelper).
     * @param tagId The item's tag id ("item.pipe.buildcrafttransport.wood_item"), or "" for none. */
    public ItemPipeHolder(Block pipeHolder, PipeDefinition definition, Item.Properties properties, String tagId) {
        super(pipeHolder, properties);
        this.definition = definition;
        this.id = tagId;
    }

    /** @return The tag id the pipe's item has in BuildCraft's {@link buildcraft.lib.registry.TagManager}. */
    public static String tagIdFor(PipeDefinition definition) {
        return "item.pipe." + definition.identifier.getNamespace() + "." + definition.identifier.getPath();
    }

    public ItemPipeHolder registerWithPipeApi() {
        PipeApi.pipeRegistry.setItemForPipe(definition, this);
        return this;
    }

    @Nullable
    public static DyeColor getColour(ItemStack stack) {
        return stack.get(BCTransportItems.COLOUR);
    }

    public ItemStack createStack(@Nullable DyeColor colour) {
        ItemStack stack = new ItemStack(this);
        if (colour != null) {
            stack.set(BCTransportItems.COLOUR, colour);
        }
        return stack;
    }

    @Override
    public void addSubItems(NonNullList<ItemStack> items) {
        items.add(new ItemStack(this));
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public PipeDefinition getDefinition() {
        return definition;
    }

    @Override
    public Component getName(ItemStack stack) {
        DyeColor colour = getColour(stack);
        Component name = super.getName(stack);
        if (colour == null) {
            return name;
        }
        return Component.literal(ColourUtil.getTextFullTooltip(colour) + " ").append(name);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
        Consumer<Component> builder, TooltipFlag flags) {
        super.appendHoverText(stack, context, display, builder, flags);
        IItemBuildCraft.appendTooltip(this, stack, context, builder, flags);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable Level world, List<String> tooltip, TooltipFlag flag) {
        String key = getDescriptionId();
        String tipName = "tip." + key.replace(".name", "").replace("item.", "");
        if (LocaleUtil.canLocalize(tipName)) {
            tooltip.add(ChatFormatting.GRAY + LocaleUtil.localize(tipName));
        }
        if (definition.flowType == PipeApi.flowFluids) {
            PipeApi.FluidTransferInfo fti = PipeApi.getFluidTransferInfo(definition);
            tooltip.add(LocaleUtil.localizeFluidFlow(fti.transferPerTick));
        } else if (definition.flowType == PipeApi.flowPower) {
            PipeApi.PowerTransferInfo pti = PipeApi.getPowerTransferInfo(definition);
            tooltip.add(LocaleUtil.localizeMjFlow(pti.transferPerTick));
        } else if (definition.flowType == PipeApi.flowRf && PipeApi.flowRf != null) {
            PipeApi.RedstoneFluxTransferInfo pti = PipeApi.getRfTransferInfo(definition);
            tooltip.add(pti.transferPerTick + " RF/t");
        }
    }
}
