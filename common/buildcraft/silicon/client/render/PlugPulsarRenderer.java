/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.client.render;


import buildcraft.api.transport.pipe.IPipeRenderOutput;
import net.minecraft.util.LightCoordsUtil;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.neoforged.fml.LogicalSide;

import buildcraft.api.transport.pluggable.IPlugDynamicRenderer;

import buildcraft.lib.client.model.AdvModelCache;
import buildcraft.lib.client.model.MutableQuad;

import buildcraft.silicon.BCSiliconModels;
import buildcraft.silicon.plug.PluggablePulsar;

public enum PlugPulsarRenderer implements IPlugDynamicRenderer<PluggablePulsar> {
    INSTANCE;

    private static final AdvModelCache cache =
        new AdvModelCache(BCSiliconModels.PULSAR_DYNAMIC, PluggablePulsar.MODEL_VAR_INFO);

    public static void onModelBake() {
        cache.reset();
    }

    @Override
    public void render(PluggablePulsar pulsar, float partialTicks, IPipeRenderOutput out) {
        VertexConsumer bb = out.cutout();
        int light = out.light();
        if (pulsar.clientModelData.hasNoNodes()) {
            pulsar.clientModelData.setNodes(BCSiliconModels.PULSAR_DYNAMIC.createTickableNodes());
        }
        pulsar.setModelVariables(partialTicks);
        pulsar.clientModelData.refresh();
        MutableQuad copy = new MutableQuad();
        for (MutableQuad q : cache.getCutoutQuads()) {
            copy.copyFrom(q);
            copy.maxLighti(LightCoordsUtil.block(light), LightCoordsUtil.sky(light));
            copy.render(bb);
        }
    }
}
