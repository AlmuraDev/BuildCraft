/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.world.phys.Vec3;


import buildcraft.lib.client.render.laser.LaserData_BC8;
import buildcraft.lib.client.render.laser.LaserData_BC8.LaserType;
import buildcraft.lib.client.render.laser.LaserRenderer_BC8;
import buildcraft.lib.client.render.tile.RecordedRenderState;

import buildcraft.factory.tile.TileMiner;

/** The tube that a mining well or pump lowers, drawn as a laser from the block down to the tube's end. */
public final class RenderTube {
    private RenderTube() {}

    public static void renderTube(TileMiner tile, LaserType laserType, float partialTicks, VertexConsumer out) {
        if (tile.isComplete()) {
            return;
        }
        double length = tile.getLength(partialTicks);
        Vec3 start = new Vec3(0.5, 0, 0.5);
        Vec3 end = new Vec3(0.5, -length, 0.5);
        LaserData_BC8 data = new LaserData_BC8(laserType, start, end, 1 / 16.0);
        LaserRenderer_BC8.renderLaserDynamic(data, out, RecordedRenderState.IDENTITY);
    }
}
