/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.client.render;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import buildcraft.lib.client.render.ItemRenderUtil;
import buildcraft.lib.client.render.tile.RecordedRenderState;

/** The recorded lasers of a builder or filler plus the items it is flying to the blocks being placed. */
@OnlyIn(Dist.CLIENT)
public class BuilderRenderState extends RecordedRenderState {
    public List<ItemRenderUtil.PlacedItem> items = new ArrayList<>();
    public int itemLight;

    @Override
    public void reset() {
        super.reset();
        items = new ArrayList<>();
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector) {
        super.submit(poseStack, collector);
        if (!items.isEmpty()) {
            ItemRenderUtil.submitItems(items, poseStack, collector, itemLight);
        }
    }
}
