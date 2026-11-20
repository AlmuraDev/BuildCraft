/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.client.render.tile;

import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;


import buildcraft.lib.client.model.MutableVertex;
import buildcraft.lib.client.sprite.SpriteHolderRegistry;

/** A variable sized element (like LED) that can render somewhere in a tile renderer. Its centre vertex sets the
 * location, colour, lightmap and texture of the single element (one texture for every face). Vertices are written in
 * the entity format, relative to the block. */
public class RenderPartCube {
    /** The centre of this element. */
    public final MutableVertex center = new MutableVertex();
    public double sizeX = 1 / 16.0, sizeY = 1 / 16.0, sizeZ = 1 / 16.0;

    /** Constructs a simple cube configured for a LED. */
    public RenderPartCube() {
        this(1 / 16.0, 1 / 16.0, 1 / 16.0);
    }

    public RenderPartCube(double x, double y, double z) {
        center.positiond(x, y, z);
    }

    /** Uses the middle of the white sprite (call once the atlas exists). */
    public void setWhiteTex() {
        TextureAtlasSprite sprite = SpriteHolderRegistry.whiteSprite();
        center.texf(sprite.getU(0.5f), sprite.getV(0.5f));
    }

    /** Renders the element, without changing the vertex. This ignores the normal of the centre vertex. */
    public void render(VertexConsumer vc) {
        Vector3f pos = center.positionvf();
        float x = pos.x;
        float y = pos.y;
        float z = pos.z;
        float rX = (float) (sizeX / 2);
        float rY = (float) (sizeY / 2);
        float rZ = (float) (sizeZ / 2);

        // up
        vertex(vc, x - rX, y + rY, z + rZ, Direction.UP);
        vertex(vc, x + rX, y + rY, z + rZ, Direction.UP);
        vertex(vc, x + rX, y + rY, z - rZ, Direction.UP);
        vertex(vc, x - rX, y + rY, z - rZ, Direction.UP);
        // down
        vertex(vc, x - rX, y - rY, z - rZ, Direction.DOWN);
        vertex(vc, x + rX, y - rY, z - rZ, Direction.DOWN);
        vertex(vc, x + rX, y - rY, z + rZ, Direction.DOWN);
        vertex(vc, x - rX, y - rY, z + rZ, Direction.DOWN);
        // west
        vertex(vc, x - rX, y - rY, z + rZ, Direction.WEST);
        vertex(vc, x - rX, y + rY, z + rZ, Direction.WEST);
        vertex(vc, x - rX, y + rY, z - rZ, Direction.WEST);
        vertex(vc, x - rX, y - rY, z - rZ, Direction.WEST);
        // east
        vertex(vc, x + rX, y - rY, z - rZ, Direction.EAST);
        vertex(vc, x + rX, y + rY, z - rZ, Direction.EAST);
        vertex(vc, x + rX, y + rY, z + rZ, Direction.EAST);
        vertex(vc, x + rX, y - rY, z + rZ, Direction.EAST);
        // north
        vertex(vc, x - rX, y - rY, z - rZ, Direction.NORTH);
        vertex(vc, x - rX, y + rY, z - rZ, Direction.NORTH);
        vertex(vc, x + rX, y + rY, z - rZ, Direction.NORTH);
        vertex(vc, x + rX, y - rY, z - rZ, Direction.NORTH);
        // south
        vertex(vc, x + rX, y - rY, z + rZ, Direction.SOUTH);
        vertex(vc, x + rX, y + rY, z + rZ, Direction.SOUTH);
        vertex(vc, x - rX, y + rY, z + rZ, Direction.SOUTH);
        vertex(vc, x - rX, y - rY, z + rZ, Direction.SOUTH);
    }

    private void vertex(VertexConsumer vc, float x, float y, float z, Direction face) {
        vc.addVertex(x, y, z)
            .setColor(center.colour_r, center.colour_g, center.colour_b, center.colour_a)
            .setUv(center.tex_u, center.tex_v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(center.lightc())
            .setNormal(face.getStepX(), face.getStepY(), face.getStepZ());
    }
}
