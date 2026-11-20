/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.client.render.laser;

import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.texture.OverlayTexture;

import net.neoforged.fml.LogicalSide;

public class LaserCompiledBuffer {
    private static final int DOUBLE_STRIDE = 5;
    private static final int INT_STRIDE = 2;
    private final int vertices;
    private final double[] da;
    private final int[] ia;

    public LaserCompiledBuffer(int vertices, double[] da, int[] ia) {
        this.vertices = vertices;
        this.da = da;
        this.ia = ia;
    }

    /** Draws the laser's quads through the given pose (into a quad-mode, entity-format consumer). */
    public void render(VertexConsumer buffer, PoseStack.Pose pose) {
        for (int i = 0; i < vertices; i++) {
            int c = ia[INT_STRIDE * i + 0];
            buffer.addVertex(pose, (float) da[DOUBLE_STRIDE * i + 0], (float) da[DOUBLE_STRIDE * i + 1],
                    (float) da[DOUBLE_STRIDE * i + 2])
                .setColor(c & 0xFF, (c >> 8) & 0xFF, (c >> 16) & 0xFF, (c >> 24) & 0xFF)
                .setUv((float) da[DOUBLE_STRIDE * i + 3], (float) da[DOUBLE_STRIDE * i + 4])
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(ia[INT_STRIDE * i + 1])
                .setNormal(pose, 0, 1, 0);
        }
    }

    public static class Builder implements ILaserRenderer {
        private final boolean useNormalColour;
        private final TDoubleArrayList doubleData = new TDoubleArrayList();
        private final TIntArrayList intData = new TIntArrayList();
        private int vertices = 0;

        public Builder(boolean useNormalColour) {
            this.useNormalColour = useNormalColour;
        }

        @Override
        public void vertex(double x, double y, double z, double u, double v, int lmap, float nx, float ny, float nz, float diffuse) {
            // POSITION_3F
            doubleData.add(x);
            doubleData.add(y);
            doubleData.add(z);

            // COLOR_4UB
            if (useNormalColour) {
                int c = (int) (diffuse * 0xFF);
                intData.add(c | c << 8 | c << 16 | 0xFF << 24);
            } else {
                intData.add(0xFF_FF_FF_FF);
            }

            // TEX_2F
            doubleData.add(u);
            doubleData.add(v);

            // TEX_2S
            intData.add(lmap);

            vertices++;
        }

        public LaserCompiledBuffer build() {
            return new LaserCompiledBuffer(vertices, doubleData.toArray(), intData.toArray());
        }
    }
}
