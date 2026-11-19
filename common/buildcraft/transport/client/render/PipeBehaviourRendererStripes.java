/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.client.render;

import buildcraft.api.transport.pipe.IPipeRenderOutput;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.Direction;

import net.neoforged.fml.LogicalSide;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import buildcraft.api.transport.pipe.IPipeBehaviourRenderer;

import buildcraft.lib.client.model.MutableQuad;

import buildcraft.transport.BCTransportModels;
import buildcraft.transport.pipe.behaviour.PipeBehaviourStripes;

@OnlyIn(Dist.CLIENT)
public enum PipeBehaviourRendererStripes implements IPipeBehaviourRenderer<PipeBehaviourStripes> {
    INSTANCE;

    @Override
    public void render(PipeBehaviourStripes stripes, float partialTicks, IPipeRenderOutput out) {
        VertexConsumer bb = out.cutout();
        Direction dir = stripes.direction;
        if (dir == null) return;
        MutableQuad[] quads = BCTransportModels.getStripesDynQuads(dir);
        int light = out.light();
        for (MutableQuad q : quads) {
            q.multShade();
            q.lighti(light);
            q.render(bb);
        }
    }
}
