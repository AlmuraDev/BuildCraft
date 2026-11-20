/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.robotics.client.render;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;


import buildcraft.api.properties.BuildCraftProperties;

import buildcraft.lib.misc.data.WorldPos;

import buildcraft.robotics.BCRoboticsBlocks;
import buildcraft.robotics.tile.TileZonePlanner;
import buildcraft.robotics.zone.ZonePlannerMapChunk;
import buildcraft.robotics.zone.ZonePlannerMapChunkKey;
import buildcraft.robotics.zone.ZonePlannerMapDataClient;

/** The small map on the zone planner's screen: a 10x8 texture of the map colours around the block, one pixel every 4
 * blocks, at the height band the viewing player is in. */
public class RenderZonePlanner implements BlockEntityRenderer<TileZonePlanner, RenderZonePlanner.State> {
    private static final Cache<WorldPos, MapTexture> TEXTURES = CacheBuilder.newBuilder()
        .expireAfterWrite(5, TimeUnit.MINUTES).removalListener(RenderZonePlanner::onRemove).build();
    private static final int TEXTURE_WIDTH = 10;
    private static final int TEXTURE_HEIGHT = 8;
    private static final int FULL_BRIGHT = LightCoordsUtil.pack(15, 15);
    private static int nextTextureId = 0;

    public static class State extends BlockEntityRenderState {
        @Nullable
        Identifier texture;
        Direction side = Direction.NORTH;
    }

    private record MapTexture(Identifier id, DynamicTexture texture) {}

    private static void onRemove(RemovalNotification<WorldPos, MapTexture> notification) {
        MapTexture texture = notification.getValue();
        if (texture != null) {
            Minecraft.getInstance().execute(() -> Minecraft.getInstance().getTextureManager().release(texture.id));
        }
    }

    /** The height band of the map the local player sees: the first block above them whose bottom is solid (1.12.2's
     * TileZonePlanner.getLevel). */
    public static int getMapLevel() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return 0;
        }
        Level world = mc.level;
        BlockPos.MutableBlockPos pos = mc.player.blockPosition().mutable();
        while (!world.getBlockState(pos).isFaceSturdy(world, pos, Direction.DOWN) && pos.getY() < world.getMaxY()) {
            pos.move(Direction.UP);
        }
        return Math.floorDiv(pos.getY(), ZonePlannerMapChunkKey.LEVEL_HEIGHT);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(TileZonePlanner tile, State state, float partialTicks, Vec3 cameraPosition,
        @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);
        state.texture = null;
        BlockState blockState = tile.getBlockState();
        if (blockState.getBlock() != BCRoboticsBlocks.zonePlanner || tile.getLevel() == null) {
            return;
        }
        state.side = blockState.getValue(BuildCraftProperties.BLOCK_FACING).getOpposite();
        MapTexture texture = getTexture(tile, state.side);
        state.texture = texture == null ? null : texture.id;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        Identifier texture = state.texture;
        if (texture == null) {
            return;
        }
        Direction side = state.side;
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(texture),
            (pose, buffer) -> renderScreen(buffer, pose, side));
    }

    private static void renderScreen(VertexConsumer buffer, PoseStack.Pose pose, Direction side) {
        double offset = 0.001;
        double minX = 3 / 16D - offset;
        double maxX = 13 / 16D + offset;
        double minY = 5 / 16D - offset;
        double maxY = 13 / 16D + offset;
        double minZ = -offset;
        double maxZ = 1 + offset;

        Vec3 min;
        Vec3 max;
        switch (side) {
            case NORTH:
                min = new Vec3(minX, minY, maxZ);
                max = new Vec3(maxX, maxY, maxZ);
                break;
            case EAST:
                min = new Vec3(minZ, minY, minX);
                max = new Vec3(minZ, maxY, maxX);
                break;
            case SOUTH:
                min = new Vec3(minX, minY, minZ);
                max = new Vec3(maxX, maxY, minZ);
                break;
            case WEST:
            default:
                min = new Vec3(maxZ, minY, minX);
                max = new Vec3(maxZ, maxY, maxX);
                break;
        }
        Direction normal = side.getOpposite();
        // Both windings, as 1.12.2 drew it with culling off
        vertex(buffer, pose, normal, min.x, min.y, min.z, 0, 0);
        vertex(buffer, pose, normal, max.x, min.y, max.z, 1, 0);
        vertex(buffer, pose, normal, max.x, max.y, max.z, 1, 1);
        vertex(buffer, pose, normal, min.x, max.y, min.z, 0, 1);

        vertex(buffer, pose, normal, min.x, max.y, min.z, 0, 1);
        vertex(buffer, pose, normal, max.x, max.y, max.z, 1, 1);
        vertex(buffer, pose, normal, max.x, min.y, max.z, 1, 0);
        vertex(buffer, pose, normal, min.x, min.y, min.z, 0, 0);
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, Direction normal, double x, double y,
        double z, float u, float v) {
        buffer.addVertex(pose, (float) x, (float) y, (float) z)
            .setColor(-1)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(FULL_BRIGHT)
            .setNormal(pose, normal.getStepX(), normal.getStepY(), normal.getStepZ());
    }

    @Nullable
    private static MapTexture getTexture(TileZonePlanner tile, Direction side) {
        WorldPos key = new WorldPos(tile);
        MapTexture texture = TEXTURES.getIfPresent(key);
        if (texture == null) {
            NativeImage image = createImage(tile, side);
            if (image != null) {
                Identifier id = Identifier.fromNamespaceAndPath("buildcraftrobotics", "zone_planner_map_" + nextTextureId++);
                DynamicTexture dynamic = new DynamicTexture(id::toString, image);
                Minecraft.getInstance().getTextureManager().register(id, dynamic);
                texture = new MapTexture(id, dynamic);
                TEXTURES.put(key, texture);
            }
        }
        return texture;
    }

    /** @return The map image, or null while any of the chunks it shows hasn't arrived from the server yet. */
    @Nullable
    private static NativeImage createImage(TileZonePlanner tile, Direction side) {
        Level world = tile.getLevel();
        BlockPos tilePos = tile.getBlockPos();
        int mapLevel = getMapLevel();
        int[] colours = new int[TEXTURE_WIDTH * TEXTURE_HEIGHT];
        for (int textureX = 0; textureX < TEXTURE_WIDTH; textureX++) {
            for (int textureY = 0; textureY < TEXTURE_HEIGHT; textureY++) {
                int posX;
                int posZ;
                int scale = 4;
                int offset1 = (textureX - TEXTURE_WIDTH / 2) * scale;
                int offset2 = (textureY - TEXTURE_HEIGHT / 2) * scale;
                switch (side) {
                    case NORTH:
                        posX = tilePos.getX() + offset1;
                        posZ = tilePos.getZ() - offset2;
                        break;
                    case EAST:
                        posX = tilePos.getX() + offset2;
                        posZ = tilePos.getZ() + offset1;
                        break;
                    case SOUTH:
                        posX = tilePos.getX() + offset1;
                        posZ = tilePos.getZ() + offset2;
                        break;
                    case WEST:
                    default:
                        posX = tilePos.getX() - offset2;
                        posZ = tilePos.getZ() + offset1;
                        break;
                }
                ZonePlannerMapChunkKey key =
                    new ZonePlannerMapChunkKey(new ChunkPos(posX >> 4, posZ >> 4), world.dimension(), mapLevel);
                ZonePlannerMapChunk chunk = ZonePlannerMapDataClient.INSTANCE.getChunk(world, key);
                if (chunk == null) {
                    return null;
                }
                colours[textureX + textureY * TEXTURE_WIDTH] = chunk.getColour(posX, posZ) | 0xFF_00_00_00;
            }
        }
        NativeImage image = new NativeImage(TEXTURE_WIDTH, TEXTURE_HEIGHT, false);
        for (int x = 0; x < TEXTURE_WIDTH; x++) {
            for (int y = 0; y < TEXTURE_HEIGHT; y++) {
                image.setPixel(x, y, colours[x + y * TEXTURE_WIDTH]);
            }
        }
        return image;
    }
}
