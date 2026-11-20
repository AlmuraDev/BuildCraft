/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.client.render;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import buildcraft.lib.client.render.laser.LaserBoxRenderer;
import buildcraft.lib.client.render.laser.LaserData_BC8;
import buildcraft.lib.client.render.laser.LaserRenderer_BC8;
import buildcraft.lib.client.render.tile.RecordedRenderState;
import buildcraft.lib.client.sprite.SpriteHolderRegistry;
import buildcraft.lib.misc.VecUtil;

import buildcraft.builders.tile.TileQuarry;
import buildcraft.core.client.BuildCraftLaserManager;

/** The quarry's frame box, its moving gantry and drill, and the laser it breaks blocks with before the frame is
 * done. */
@OnlyIn(Dist.CLIENT)
public class RenderQuarry implements BlockEntityRenderer<TileQuarry, RecordedRenderState> {
    public static final LaserData_BC8.LaserType FRAME;
    public static final LaserData_BC8.LaserType FRAME_BOTTOM;
    public static final LaserData_BC8.LaserType DRILL;
    public static final LaserData_BC8.LaserType LASER;

    static {
        {
            SpriteHolderRegistry.SpriteHolder sprite =
                SpriteHolderRegistry.getHolder("buildcraftbuilders:blocks/frame/default");
            LaserData_BC8.LaserRow capStart = new LaserData_BC8.LaserRow(sprite, 0, 0, 0, 0);
            LaserData_BC8.LaserRow[] middle = { new LaserData_BC8.LaserRow(sprite, 0, 4, 16, 12) };
            LaserData_BC8.LaserRow end = new LaserData_BC8.LaserRow(sprite, 0, 4, 16, 12);
            LaserData_BC8.LaserRow capEnd = new LaserData_BC8.LaserRow(sprite, 0, 0, 0, 0);
            FRAME = new LaserData_BC8.LaserType(capStart, null, middle, end, capEnd);
        }
        {
            SpriteHolderRegistry.SpriteHolder sprite =
                SpriteHolderRegistry.getHolder("buildcraftbuilders:blocks/frame/default");
            LaserData_BC8.LaserRow capStart = new LaserData_BC8.LaserRow(sprite, 0, 0, 0, 0);
            LaserData_BC8.LaserRow[] middle = { new LaserData_BC8.LaserRow(sprite, 0, 4, 16, 12) };
            LaserData_BC8.LaserRow end = new LaserData_BC8.LaserRow(sprite, 0, 4, 16, 12);
            LaserData_BC8.LaserRow capEnd = new LaserData_BC8.LaserRow(sprite, 4, 4, 12, 12);
            FRAME_BOTTOM = new LaserData_BC8.LaserType(capStart, null, middle, end, capEnd);
        }
        {
            SpriteHolderRegistry.SpriteHolder sprite =
                SpriteHolderRegistry.getHolder("buildcraftbuilders:blocks/quarry/drill");
            LaserData_BC8.LaserRow capStart = new LaserData_BC8.LaserRow(sprite, 6, 0, 10, 4);
            LaserData_BC8.LaserRow[] middle = { new LaserData_BC8.LaserRow(sprite, 0, 0, 16, 4) };
            LaserData_BC8.LaserRow capEnd = new LaserData_BC8.LaserRow(sprite, 6, 0, 10, 4);
            DRILL = new LaserData_BC8.LaserType(capStart, null, middle, null, capEnd);
        }
        LASER = BuildCraftLaserManager.POWER_LOW;
    }

    @Override
    public RecordedRenderState createRenderState() {
        return new RecordedRenderState();
    }

    @Override
    public void extractRenderState(TileQuarry tile, RecordedRenderState state, float partialTicks,
        Vec3 cameraPosition, @Nullable ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tile, state, partialTicks, cameraPosition, breakProgress);
        state.reset();
        if (tile.frameBox.isInitialized() && tile.getLevel() != null) {
            render(tile, partialTicks, state.translucent, RecordedRenderState.worldPose(tile.getBlockPos()));
        }
        state.finish();
    }

    private static void render(TileQuarry tile, float partialTicks, VertexConsumer buffer, PoseStack.Pose pose) {
        final BlockPos min = tile.frameBox.min();
        final BlockPos max = tile.frameBox.max();
        double yOffset = 1 + 4 / 16D;

        if (tile.currentTask instanceof TileQuarry.TaskBreakBlock taskBreakBlock) {
            BlockPos pos = taskBreakBlock.breakPos;

            if (tile.drillPos == null) {
                if (taskBreakBlock.clientPower != 0) {
                    // Don't render a laser before we have any power
                    Vec3 from = VecUtil.convertCenter(tile.getBlockPos());
                    Vec3 to = VecUtil.convertCenter(pos);
                    LaserRenderer_BC8.renderLaserDynamic(new LaserData_BC8(LASER, from, to, 1 / 16.0), buffer, pose);
                }
            } else {
                long power = (long) (taskBreakBlock.prevClientPower
                    + (taskBreakBlock.clientPower - taskBreakBlock.prevClientPower) * (double) partialTicks);
                VoxelShape shape = tile.getLevel().getBlockState(pos).getShape(tile.getLevel(), pos);
                AABB aabb = shape.isEmpty() ? new AABB(0, 0, 0, 1, 1, 1) : shape.bounds();
                double value = (double) power / taskBreakBlock.getTarget();
                if (value < 0.9) {
                    value = 1 - value / 0.9;
                } else {
                    value = (value - 0.9) / 0.1;
                }
                double scaleMin = 1 - (1 - aabb.maxY) - (aabb.maxY - aabb.minY) / 2;
                double scaleMax = 1 + 4 / 16D;
                yOffset = scaleMin + value * (scaleMax - scaleMin);
            }
        }

        if (tile.clientDrillPos != null && tile.prevClientDrillPos != null) {
            Vec3 ip = tile.prevClientDrillPos
                .add(tile.clientDrillPos.subtract(tile.prevClientDrillPos).scale(partialTicks));
            double top = max.getY() + 0.5;

            laser(buffer, pose, FRAME, new Vec3(ip.x + 0.5, top, ip.z), new Vec3(ip.x + 0.5, top, max.getZ() + 12 / 16D));
            laser(buffer, pose, FRAME, new Vec3(ip.x + 0.5, top, ip.z), new Vec3(ip.x + 0.5, top, min.getZ() + 4 / 16D));
            laser(buffer, pose, FRAME, new Vec3(ip.x, top, ip.z + 0.5), new Vec3(max.getX() + 12 / 16D, top, ip.z + 0.5));
            laser(buffer, pose, FRAME, new Vec3(ip.x, top, ip.z + 0.5), new Vec3(min.getX() + 4 / 16D, top, ip.z + 0.5));
            laser(buffer, pose, FRAME_BOTTOM, new Vec3(ip.x + 0.5, ip.y + 1 + 4 / 16D, ip.z + 0.5),
                new Vec3(ip.x + 0.5, top, ip.z + 0.5));
            laser(buffer, pose, DRILL, new Vec3(ip.x + 0.5, ip.y + 1 + yOffset, ip.z + 0.5),
                new Vec3(ip.x + 0.5, ip.y + yOffset, ip.z + 0.5));
        } else {
            LaserBoxRenderer.renderLaserBoxDynamic(tile.frameBox, BuildCraftLaserManager.STRIPES_WRITE, buffer, pose,
                true);
        }
    }

    private static void laser(VertexConsumer buffer, PoseStack.Pose pose, LaserData_BC8.LaserType type, Vec3 from,
        Vec3 to) {
        LaserRenderer_BC8.renderLaserDynamic(new LaserData_BC8(type, from, to, 1 / 16D, true, true, 0), buffer, pose);
    }

    @Override
    public void submit(RecordedRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
        CameraRenderState camera) {
        state.submit(poseStack, collector);
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(TileQuarry tile) {
        return AABB.INFINITE;
    }

    public static void init() {}
}
