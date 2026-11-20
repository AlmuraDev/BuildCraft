/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.client.render;

import java.util.Collections;

import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import buildcraft.lib.client.model.ModelUtil;
import buildcraft.lib.client.render.ItemRenderUtil;
import buildcraft.lib.client.render.laser.LaserData_BC8;
import buildcraft.lib.client.render.laser.LaserRenderer_BC8;
import buildcraft.lib.client.render.tile.RecordedRenderState;
import buildcraft.lib.misc.MathUtil;

import buildcraft.builders.BCBuildersSprites;
import buildcraft.builders.snapshot.ITileForSnapshotBuilder;
import buildcraft.builders.snapshot.SnapshotBuilder;
import buildcraft.core.client.BuildCraftLaserManager;

/** The builder robot, its break lasers and the items flying to placed blocks, for the builder and the filler. */
@OnlyIn(Dist.CLIENT)
public class RenderSnapshotBuilder {
    public static <T extends ITileForSnapshotBuilder> void extract(SnapshotBuilder<T> snapshotBuilder, Level world,
        BlockPos tilePos, float partialTicks, BuilderRenderState state) {
        PoseStack.Pose pose = RecordedRenderState.worldPose(tilePos);
        state.itemLight = RecordedRenderState.light(world, tilePos);

        for (SnapshotBuilder<T>.PlaceTask placeTask : snapshotBuilder.clientPlaceTasks) {
            Vec3 prevPos = snapshotBuilder.prevClientPlaceTasks.stream()
                .filter(renderTaskLocal -> renderTaskLocal.pos.equals(placeTask.pos))
                .map(snapshotBuilder::getPlaceTaskItemPos)
                .findFirst()
                .orElse(snapshotBuilder.getPlaceTaskItemPos(
                    snapshotBuilder.new PlaceTask(tilePos, Collections.emptyList(), 0L)));
            Vec3 pos = prevPos.add(snapshotBuilder.getPlaceTaskItemPos(placeTask).subtract(prevPos).scale(partialTicks));
            for (ItemStack item : placeTask.items) {
                ItemRenderUtil.extractItemStack(state.items, world, pos.x - tilePos.getX(), pos.y - tilePos.getY(),
                    pos.z - tilePos.getZ(), item, item.getCount(), Direction.SOUTH);
            }
        }

        Vec3 robotPos = snapshotBuilder.robotPos;
        if (robotPos != null) {
            if (snapshotBuilder.prevRobotPos != null) {
                robotPos = snapshotBuilder.prevRobotPos
                    .add(robotPos.subtract(snapshotBuilder.prevRobotPos).scale(partialTicks));
            }

            int light = RecordedRenderState.light(world, BlockPos.containing(robotPos));
            int i = 0;
            for (Direction face : Direction.values()) {
                ModelUtil.createFace(face, new Vector3f((float) robotPos.x, (float) robotPos.y, (float) robotPos.z),
                    new Vector3f(4 / 16F, 4 / 16F, 4 / 16F),
                    new ModelUtil.UvFaceData(BCBuildersSprites.ROBOT.getInterpU((i * 8) / 64D),
                        BCBuildersSprites.ROBOT.getInterpV(0 / 64D),
                        BCBuildersSprites.ROBOT.getInterpU(((i + 1) * 8) / 64D),
                        BCBuildersSprites.ROBOT.getInterpV(8 / 64D)))
                    .lighti(light)
                    .render(state.cutout, pose, OverlayTexture.NO_OVERLAY);
                i++;
            }

            for (SnapshotBuilder<T>.BreakTask breakTask : snapshotBuilder.clientBreakTasks) {
                int index = (int) Math.round(MathUtil.clamp(breakTask.power * 1D / breakTask.getTarget(), 0D, 1D)
                    * (BuildCraftLaserManager.POWERS.length - 1));
                LaserRenderer_BC8.renderLaserDynamic(new LaserData_BC8(BuildCraftLaserManager.POWERS[index],
                    robotPos.subtract(new Vec3(0, 0.27, 0)), Vec3.atCenterOf(breakTask.pos), 1 / 16D),
                    state.translucent, pose);
            }
        }
    }
}
