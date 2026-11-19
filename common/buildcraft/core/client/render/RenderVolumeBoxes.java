/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core.client.render;

import buildcraft.lib.debug.IDetachedRenderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.world.entity.player.Player;

import buildcraft.lib.client.render.WorldRenderContext;

import buildcraft.lib.client.render.DetachedRenderer;
import buildcraft.lib.client.render.laser.LaserBoxRenderer;
import buildcraft.lib.client.render.laser.LaserData_BC8.LaserType;

import buildcraft.core.client.BuildCraftLaserManager;
import buildcraft.core.marker.volume.Addon;
import buildcraft.core.marker.volume.ClientVolumeBoxes;
import buildcraft.core.marker.volume.IFastAddonRenderer;
import buildcraft.core.marker.volume.Lock;

public enum RenderVolumeBoxes implements IDetachedRenderer {
    INSTANCE;

    @SuppressWarnings("unchecked")
    @Override
    public void render(Player player, float partialTicks) {
        PoseStack stack = WorldRenderContext.poseStack();
        VertexConsumer bb = WorldRenderContext.buffer(WorldRenderContext.TRANSLUCENT_BLOCK_ATLAS);
        if (stack == null || bb == null) {
            return;
        }
        PoseStack.Pose pose = stack.last();

        ClientVolumeBoxes.INSTANCE.volumeBoxes.forEach(volumeBox -> {
            LaserType type;
            if (volumeBox.isEditingBy(player)) {
                type = BuildCraftLaserManager.MARKER_VOLUME_SIGNAL;
            } else {
                type = volumeBox.getLockTargetsStream()
                    .filter(Lock.Target.TargetUsedByMachine.class::isInstance)
                    .map(Lock.Target.TargetUsedByMachine.class::cast)
                    .map(target -> target.type)
                    .map(t -> t == Lock.Target.TargetUsedByMachine.EnumType.STRIPES_WRITE
                        ? BuildCraftLaserManager.STRIPES_WRITE : BuildCraftLaserManager.STRIPES_READ)
                    .findFirst()
                    .orElse(BuildCraftLaserManager.MARKER_VOLUME_CONNECTED);
            }
            LaserBoxRenderer.renderLaserBoxDynamic(volumeBox.box, type, bb, pose, false);

            volumeBox.addons.values().forEach(addon ->
                ((IFastAddonRenderer<Addon>) addon.getRenderer()).renderAddonFast(addon, player, partialTicks, bb, pose)
            );
        });

    }
}
