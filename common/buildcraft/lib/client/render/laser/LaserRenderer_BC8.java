/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.client.render.laser;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.Level;

import net.neoforged.fml.LogicalSide;

import buildcraft.lib.client.render.WorldRenderContext;
import buildcraft.lib.client.render.laser.LaserData_BC8.LaserType;

public class LaserRenderer_BC8 {
    private static final Map<LaserType, CompiledLaserType> COMPILED_LASER_TYPES = new HashMap<>();
    /** Lasers are baked into vertex lists (1.12.2 also compiled GL display lists for static ones; 26.3 has none). */
    private static final LoadingCache<LaserData_BC8, LaserCompiledBuffer> COMPILED_LASERS;

    static {
        COMPILED_LASERS = CacheBuilder.newBuilder()//
            .expireAfterWrite(5, TimeUnit.SECONDS)//
            .build(CacheLoader.from(LaserRenderer_BC8::makeLaserBuffer));
    }

    public static void clearModels() {
        COMPILED_LASER_TYPES.clear();
    }

    private static CompiledLaserType compileType(LaserType laserType) {
        if (!COMPILED_LASER_TYPES.containsKey(laserType)) {
            COMPILED_LASER_TYPES.put(laserType, new CompiledLaserType(laserType));
        }
        return COMPILED_LASER_TYPES.get(laserType);
    }

    private static LaserCompiledBuffer makeLaserBuffer(LaserData_BC8 data) {
        LaserCompiledBuffer.Builder renderer = new LaserCompiledBuffer.Builder(data.enableDiffuse);
        makeLaser(data, renderer);
        return renderer.build();
    }

    private static void makeLaser(LaserData_BC8 data, ILaserRenderer renderer) {
        LaserContext ctx = new LaserContext(renderer, data, data.enableDiffuse, data.doubleFace);
        CompiledLaserType type = compileType(data.laserType);
        type.bakeFor(ctx);
    }

    public static int computeLightmap(double x, double y, double z, int minBlockLight) {
        Level world = Minecraft.getInstance().level;
        if (world == null) return 0;
        int blockLight =
            minBlockLight >= 15 ? 15 : Math.max(minBlockLight, getLightFor(world, LightLayer.BLOCK, x, y, z));
        int skyLight = getLightFor(world, LightLayer.SKY, x, y, z);
        return skyLight << 20 | blockLight << 4;
    }

    private static int getLightFor(Level world, LightLayer type, double x, double y, double z) {
        int max = 0;
        int count = 0;
        int sum = 0;

        boolean ao = Minecraft.getInstance().options.ambientOcclusion().get();

        double xn = (x % 1 + 1) % 1;
        double yn = (y % 1 + 1) % 1;
        double zn = (z % 1 + 1) % 1;

        final double lowerBound = 0.3;
        final double upperBound = 1 - lowerBound;

        int xl = ao ? (xn < lowerBound ? -1 : 0) : -1;
        int yl = ao ? (yn < lowerBound ? -1 : 0) : -1;
        int zl = ao ? (zn < lowerBound ? -1 : 0) : -1;
        int xu = ao ? (xn > upperBound ? 1 : 0) : 1;
        int yu = ao ? (yn > upperBound ? 1 : 0) : 1;
        int zu = ao ? (zn > upperBound ? 1 : 0) : 1;

        for (int xp = xl; xp <= xu; xp++) {
            for (int yp = yl; yp <= yu; yp++) {
                for (int zp = zl; zp <= zu; zp++) {
                    int light = world.getBrightness(type, BlockPos.containing(x + xp, y + yp, z + zp));
                    if (light > 0) {
                        sum += light;
                        count++;
                    }
                    max = Math.max(max, light);
                }
            }
        }

        if (ao) {
            return count == 0 ? 0 : sum / count;
        } else {
            return max;
        }
    }

    /** Draws a laser (in world coordinates) into the current detached render pass, if there is one. */
    public static void renderLaserStatic(LaserData_BC8 data) {
        PoseStack stack = WorldRenderContext.poseStack();
        VertexConsumer buffer = WorldRenderContext.buffer(WorldRenderContext.TRANSLUCENT_BLOCK_ATLAS);
        if (stack != null && buffer != null) {
            renderLaser(data, buffer, stack.last());
        }
    }

    /** Draws a laser into the given consumer (a translucent, entity-format, block-atlas one) through the pose. */
    public static void renderLaser(LaserData_BC8 data, VertexConsumer buffer, PoseStack.Pose pose) {
        COMPILED_LASERS.getUnchecked(data).render(buffer, pose);
    }

    /** 1.12.2 cached "dynamic" lasers separately; they are the same as static ones now. */
    public static void renderLaserDynamic(LaserData_BC8 data, VertexConsumer buffer, PoseStack.Pose pose) {
        renderLaser(data, buffer, pose);
    }
}
