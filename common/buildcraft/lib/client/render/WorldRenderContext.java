/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.client.render;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlas;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** Where BuildCraft's "detached" world rendering (lasers, debug boxes, volume boxes) draws. 1.12.2 drew with GL calls
 * straight into the world matrix; 26.3 only draws what was submitted, so {@link DetachedRenderer} lets the renderers
 * record their quads (in world coordinates) while the level render state is extracted, and submits the recording
 * afterwards. Everything recorded is drawn as {@link #TRANSLUCENT_BLOCK_ATLAS}. */
@OnlyIn(Dist.CLIENT)
public final class WorldRenderContext {
    /** Block-atlas quads with alpha (lasers). */
    public static final RenderType TRANSLUCENT_BLOCK_ATLAS = RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS);

    private static PoseStack poseStack;
    private static Recording recording;

    private WorldRenderContext() {}

    static void begin(PoseStack stack, Recording into) {
        poseStack = stack;
        recording = into;
    }

    static void end() {
        poseStack = null;
        recording = null;
    }

    public static boolean isActive() {
        return poseStack != null;
    }

    /** @return The current pose (world origin based), or null outside the detached render pass. */
    @Nullable
    public static PoseStack poseStack() {
        return poseStack;
    }

    /** @return A consumer recording entity-format vertices, or null outside the detached render pass. Only
     *         {@link #TRANSLUCENT_BLOCK_ATLAS} is supported. */
    @Nullable
    public static VertexConsumer buffer(RenderType type) {
        return recording;
    }

    /** Recorded entity-format vertices (position, colour, uv, overlay, light, normal), replayed when submitting. */
    public static final class Recording implements VertexConsumer {
        private final TFloatArrayList floats = new TFloatArrayList();
        private final TIntArrayList ints = new TIntArrayList();
        private int vertices = 0;

        // Current vertex
        private float x, y, z, u, v, nx, ny = 1, nz;
        private int colour = -1, overlay, light;
        private boolean started;

        private void flush() {
            if (started) {
                floats.add(x);
                floats.add(y);
                floats.add(z);
                floats.add(u);
                floats.add(v);
                floats.add(nx);
                floats.add(ny);
                floats.add(nz);
                ints.add(colour);
                ints.add(overlay);
                ints.add(light);
                vertices++;
            }
        }

        public boolean isEmpty() {
            return vertices == 0 && !started;
        }

        public void finish() {
            flush();
            started = false;
        }

        public void replay(VertexConsumer buffer, PoseStack.Pose pose) {
            for (int i = 0; i < vertices; i++) {
                int f = i * 8;
                int n = i * 3;
                buffer.addVertex(pose, floats.get(f), floats.get(f + 1), floats.get(f + 2))
                    .setColor(ints.get(n))
                    .setUv(floats.get(f + 3), floats.get(f + 4))
                    .setOverlay(ints.get(n + 1))
                    .setLight(ints.get(n + 2))
                    .setNormal(pose, floats.get(f + 5), floats.get(f + 6), floats.get(f + 7));
            }
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            flush();
            started = true;
            this.x = x;
            this.y = y;
            this.z = z;
            colour = -1;
            u = v = 0;
            overlay = 0;
            light = 0;
            nx = nz = 0;
            ny = 1;
            return this;
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            colour = (a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | (b & 255);
            return this;
        }

        @Override
        public VertexConsumer setColor(int argb) {
            colour = argb;
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            this.u = u;
            this.v = v;
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            overlay = (u & 0xFFFF) | (v & 0xFFFF) << 16;
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            light = (u & 0xFFFF) | (v & 0xFFFF) << 16;
            return this;
        }

        @Override
        public VertexConsumer setUv3(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            nx = x;
            ny = y;
            nz = z;
            return this;
        }

        @Override
        public VertexConsumer setLineWidth(float width) {
            return this;
        }
    }
}
