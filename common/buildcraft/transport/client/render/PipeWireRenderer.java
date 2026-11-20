/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.client.render;

import java.util.EnumMap;
import java.util.Map;

import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.DyeColor;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.phys.Vec3;


import buildcraft.api.core.render.ISprite;
import buildcraft.api.transport.EnumWirePart;

import buildcraft.lib.client.model.ModelUtil;
import buildcraft.lib.client.model.ModelUtil.UvFaceData;
import buildcraft.lib.client.model.MutableQuad;
import buildcraft.lib.client.model.MutableVertex;
import buildcraft.lib.client.sprite.SpriteHolderRegistry;
import buildcraft.lib.misc.ColourUtil;
import buildcraft.lib.misc.VecUtil;

import buildcraft.transport.tile.TilePipeHolder;
import buildcraft.transport.wire.EnumWireBetween;

public class PipeWireRenderer {

    private static final Map<EnumWirePart, MutableQuad[]> partQuads = new EnumMap<>(EnumWirePart.class);
    private static final Map<EnumWireBetween, MutableQuad[]> betweenQuads = new EnumMap<>(EnumWireBetween.class);

    private static final Map<DyeColor, SpriteHolderRegistry.SpriteHolder> wireSprites =
        new EnumMap<>(DyeColor.class);
    static {
        for (DyeColor color : DyeColor.values()) {
            wireSprites.put(color, SpriteHolderRegistry.getHolder("buildcrafttransport:wires/" + color.getName()));
        }

        for (EnumWirePart part : EnumWirePart.VALUES) {
            partQuads.put(part, getQuads(part));
        }
        for (EnumWireBetween part : EnumWireBetween.VALUES) {
            betweenQuads.put(part, getQuads(part));
        }
    }

    /** Wires are drawn straight from their quads each frame (26.3 has no display lists), so there is nothing to clear. */
    public static void clearWireCache() {}

    public static SpriteHolderRegistry.SpriteHolder getWireSprite(DyeColor colour) {
        return wireSprites.get(colour);
    }

    private static MutableQuad[] getQuads(EnumWirePart part) {
        MutableQuad[] quads = new MutableQuad[6];

        Vector3f center = new Vector3f(//
            0.5f + (part.x.getStep() * 4.51f / 16f), //
            0.5f + (part.y.getStep() * 4.51f / 16f), //
            0.5f + (part.z.getStep() * 4.51f / 16f) //
        );
        Vector3f radius = new Vector3f(1 / 32f, 1 / 32f, 1 / 32f);
        UvFaceData uvs = new UvFaceData();
        int off = func(part.x) * 4 + func(part.y) * 2 + func(part.z);
        uvs.minU = off / 16f;
        uvs.maxU = (off + 1) / 16f;
        uvs.minV = 0;
        uvs.maxV = 1 / 16f;
        for (Direction face : Direction.values()) {
            quads[face.ordinal()] = ModelUtil.createFace(face, center, radius, uvs);
        }
        return quads;
    }

    private static int func(Direction.AxisDirection dir) {
        return dir == AxisDirection.POSITIVE ? 1 : 0;
    }

    private static MutableQuad[] getQuads(EnumWireBetween between) {
        // 4 rather than 6 -- don't render the end caps
        MutableQuad[] quads = new MutableQuad[4];

        int i = 0;

        Vec3 center;
        Vec3 radius;

        boolean ax = between.mainAxis == Axis.X;
        boolean ay = between.mainAxis == Axis.Y;
        boolean az = between.mainAxis == Axis.Z;

        if (between.to == null) {
            double cL = 0.5f - 4.51f / 16f;
            double cU = 0.5f + 4.51f / 16f;
            center = new Vec3(//
                ax ? 0.5f : (between.xy ? cU : cL), //
                ay ? 0.5f : ((ax ? between.xy : between.yz) ? cU : cL), //
                az ? 0.5f : (between.yz ? cU : cL) //
            );
            double rC = 4.01f / 16f;
            double rN = 1f / 16f / 2;
            radius = new Vec3(//
                ax ? rC : rN, //
                ay ? rC : rN, //
                az ? rC : rN //
            );
        } else {// we are a connection
            double cL = (8 - 4.51) / 16;
            double cU = (8 + 4.51) / 16;
            radius = new Vec3(//
                ax ? 2.99 / 32 : 1 / 32.0, //
                ay ? 2.99 / 32 : 1 / 32.0, //
                az ? 2.99 / 32 : 1 / 32.0 //
            );
            center = new Vec3(//
                ax ? (0.5 + 6.505 / 16 * between.to.getStepX()) : (between.xy ? cU : cL), //
                ay ? (0.5 + 6.505 / 16 * between.to.getStepY()) : ((ax ? between.xy : between.yz) ? cU : cL), //
                az ? (0.5 + 6.505 / 16 * between.to.getStepZ()) : (between.yz ? cU : cL) //
            );
        }

        UvFaceData uvBase = new UvFaceData();
        uvBase.minU = (float) VecUtil.getValue(center.subtract(radius), between.mainAxis);
        uvBase.maxU = (float) VecUtil.getValue(center.add(radius), between.mainAxis);
        uvBase.minV = 0;
        uvBase.maxV = 1 / 16f;

        Vector3f centerFloat = VecUtil.convertFloat(center);
        Vector3f radiusFloat = VecUtil.convertFloat(radius);

        for (Direction face : Direction.values()) {
            if (face.getAxis() == between.mainAxis) {
                continue;
            }
            UvFaceData uvs = new UvFaceData(uvBase);

            Axis aAxis = between.mainAxis;
            Axis fAxis = face.getAxis();
            boolean fPositive = face.getAxisDirection() == AxisDirection.POSITIVE;

            int rotations = 0;
            boolean swapU = false;
            boolean swapV = false;

            if (aAxis == Axis.X) {
                swapV = fPositive;
            } else if (aAxis == Axis.Y) {
                rotations = 1;
                swapU = (fAxis == Axis.X) != fPositive;
                swapV = fAxis == Axis.Z;
            } else {// aAxis == Axis.Z
                if (fAxis == Axis.Y) {
                    rotations = 1;
                }
                swapU = face == Direction.DOWN;
                swapV = face != Direction.EAST;
            }

            if (swapU) {
                float t = uvs.minU;
                uvs.minU = uvs.maxU;
                uvs.maxU = t;
            }
            if (swapV) {
                float t = uvs.minV;
                uvs.minV = uvs.maxV;
                uvs.maxV = t;
            }

            MutableQuad quad = ModelUtil.createFace(face, centerFloat, radiusFloat, uvs);
            if (rotations > 0) quad.rotateTextureUp(rotations);
            quads[i++] = quad;
        }
        return quads;
    }

    /** Writes the quads with the wire sprite, shading the sides of unpowered wire like 1.12.2 did. The wire textures
     * hold 16 power levels (rows), though pipe wire only uses off (0) and on (15). */
    private static void renderQuads(MutableQuad[] quads, ISprite sprite, int level, int lightCoords,
        VertexConsumer out) {
        float vOffset = (level & 0xF) / 16f;
        for (MutableQuad q : quads) {
            q = new MutableQuad(q);
            if (q.getFace() != Direction.UP && level != 15) {
                float shade = 1 - q.getCalculatedDiffuse();
                shade = shade * (15 - level) / 15;
                shade = 1 - shade;
                q.multColourd(shade);
            }
            for (MutableVertex v : new MutableVertex[] { q.vertex_0, q.vertex_1, q.vertex_2, q.vertex_3 }) {
                v.texf((float) sprite.getInterpU(v.tex_u), (float) sprite.getInterpV(v.tex_v + vOffset));
                v.lighti(lightCoords);
            }
            q.setCalculatedNormal();
            q.render(out);
        }
    }

    public static void renderWires(TilePipeHolder pipe, VertexConsumer out, int lightCoords) {
        int sky = LightCoordsUtil.sky(lightCoords);
        int poweredLight = LightCoordsUtil.pack(15, sky);
        for (Map.Entry<EnumWirePart, DyeColor> partColor : pipe.getWireManager().parts.entrySet()) {
            EnumWirePart part = partColor.getKey();
            boolean isOn = pipe.wireManager.isPowered(part);
            renderQuads(partQuads.get(part), wireSprites.get(partColor.getValue()), isOn ? 15 : 0,
                isOn ? poweredLight : lightCoords, out);
        }
        for (Map.Entry<EnumWireBetween, DyeColor> betweenColor : pipe.getWireManager().betweens.entrySet()) {
            EnumWireBetween between = betweenColor.getKey();
            boolean isOn = pipe.wireManager.isPowered(between.parts[0]);
            renderQuads(betweenQuads.get(between), wireSprites.get(betweenColor.getValue()), isOn ? 15 : 0,
                isOn ? poweredLight : lightCoords, out);
        }
    }

    public static void init() {
        // make sure static runs
    }
}
