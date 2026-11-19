/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.core.client.render;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import buildcraft.lib.client.render.MarkerRenderer;
import buildcraft.lib.client.render.laser.LaserBoxRenderer;
import buildcraft.lib.client.render.laser.LaserData_BC8;
import buildcraft.lib.client.render.laser.LaserRenderer_BC8;
import buildcraft.lib.misc.VecUtil;

import buildcraft.core.client.BuildCraftLaserManager;
import buildcraft.core.marker.PathConnection;
import buildcraft.core.marker.VolumeConnection;

/** Draws core's marker connections (moved here from the common connection classes). */
public final class RenderMarkerConnections {
    private static final double RENDER_SCALE = 1 / 16.05;
    private static final Vec3 VEC_HALF = new Vec3(0.5, 0.5, 0.5);

    private RenderMarkerConnections() {}

    public static void register() {
        MarkerRenderer.registerRenderer(PathConnection.class, RenderMarkerConnections::renderPath);
        MarkerRenderer.registerRenderer(VolumeConnection.class, RenderMarkerConnections::renderVolume);
    }

    private static void renderVolume(VolumeConnection connection) {
        LaserBoxRenderer.renderLaserBoxStatic(connection.getBox(), BuildCraftLaserManager.MARKER_VOLUME_CONNECTED, true);
    }

    /** A looping path's marker positions end with its first marker again, so this draws the closing laser too. */
    private static void renderPath(PathConnection connection) {
        BlockPos last = null;
        for (BlockPos p : connection.getMarkerPositions()) {
            if (last != null) {
                renderLaser(VecUtil.add(VEC_HALF, last), VecUtil.add(VEC_HALF, p));
            }
            last = p;
        }
    }

    private static void renderLaser(Vec3 from, Vec3 to) {
        Vec3 one = offset(from, to);
        Vec3 two = offset(to, from);
        LaserData_BC8 data = new LaserData_BC8(BuildCraftLaserManager.MARKER_PATH_CONNECTED, one, two, RENDER_SCALE);
        LaserRenderer_BC8.renderLaserStatic(data);
    }

    private static Vec3 offset(Vec3 from, Vec3 to) {
        Vec3 dir = to.subtract(from).normalize();
        return from.add(VecUtil.scale(dir, 0.125));
    }
}
