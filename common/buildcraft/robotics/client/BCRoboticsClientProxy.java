/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.robotics.client;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import buildcraft.lib.net.MessageManager;
import buildcraft.lib.registry.TileTypes;

import buildcraft.robotics.BCRobotics;
import buildcraft.robotics.BCRoboticsBlocks;
import buildcraft.robotics.BCRoboticsProxy;
import buildcraft.robotics.client.render.RenderZonePlanner;
import buildcraft.robotics.zone.MessageZoneMapResponse;

/** Robotics' client proxy, created by name from {@link BCRoboticsProxy} so the server never loads it.
 */
public class BCRoboticsClientProxy extends BCRoboticsProxy {
    @Override
    public void fmlPreInit() {
        super.fmlPreInit();
        MessageManager.setHandler(MessageZoneMapResponse.class, MessageZoneMapResponse.HANDLER, LogicalSide.CLIENT);
        BCRobotics.MOD_BUS.addListener(BCRoboticsClientProxy::registerRenderers);
        BCRobotics.MOD_BUS.addListener((net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) ->
            buildcraft.lib.client.BCMenuScreens.register(event, buildcraft.robotics.RoboticsGuis.zonePlanner,
                buildcraft.robotics.gui.GuiZonePlanner::new));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        if (BCRoboticsBlocks.zonePlanner == null) {
            return;
        }
        BlockEntityType type = TileTypes.forState(BCRoboticsBlocks.zonePlanner.defaultBlockState());
        event.registerBlockEntityRenderer(type, (BlockEntityRendererProvider) ctx -> new RenderZonePlanner());
    }
}
