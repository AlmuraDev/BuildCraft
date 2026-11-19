/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core.marker.volume;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.entity.player.Player;

public interface IFastAddonRenderer<T extends Addon> {
    /** Draws the addon in world coordinates (a translucent, entity-format, block-atlas consumer). */
    void renderAddonFast(T addon, Player player, float partialTicks, VertexConsumer bb, PoseStack.Pose pose);

    default IFastAddonRenderer<T> then(IFastAddonRenderer<? super T> after) {
        return (addon, player, partialTicks, bb, pose) -> {
            renderAddonFast(addon, player, partialTicks, bb, pose);
            after.renderAddonFast(addon, player, partialTicks, bb, pose);
        };
    }
}
