/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.addon;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import buildcraft.lib.client.sprite.SpriteHolderRegistry;

import buildcraft.builders.snapshot.Template;
import buildcraft.core.marker.volume.AddonDefaultRenderer;
import buildcraft.core.marker.volume.IFastAddonRenderer;

/** Shows the blocks a filler planner's pattern would fill as small see-through cubes. */
@OnlyIn(Dist.CLIENT)
public class AddonRendererFillerPlanner implements IFastAddonRenderer<AddonFillerPlanner> {
    @Override
    public void renderAddonFast(AddonFillerPlanner addon, Player player, float partialTicks, VertexConsumer vb,
        PoseStack.Pose pose) {
        Template.BuildingInfo info = addon.buildingInfo;
        if (info == null) {
            return;
        }
        List<BlockPos> list = new ArrayList<>();
        for (BlockPos blockPos : BlockPos.betweenClosed(info.box.min(), info.box.max())) {
            if (info.getSnapshot().data.get(info.getSnapshot().posToIndex(info.fromWorld(blockPos)))
                && player.level().isEmptyBlock(blockPos)) {
                list.add(blockPos.immutable());
            }
        }
        Vec3 eye = player.position();
        list.sort(Comparator.<BlockPos>comparingDouble(p -> eye.distanceToSqr(Vec3.atCenterOf(p))).reversed());

        TextureAtlasSprite s = SpriteHolderRegistry.whiteSprite();
        for (BlockPos p : list) {
            AABB bb = new AABB(p).inflate(-0.1);
            AddonDefaultRenderer.renderCube(vb, pose, bb, s, 127);
        }
    }
}
