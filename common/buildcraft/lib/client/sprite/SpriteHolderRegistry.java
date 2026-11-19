/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.client.sprite;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import buildcraft.api.core.BCDebugging;
import buildcraft.api.core.BCLog;
import buildcraft.api.core.render.ISprite;

/** Named references to sprites on the block atlas. 26.3 stitches the atlas from its JSON sources
 * (assets/minecraft/atlases/blocks.json), so a holder only looks its sprite up, lazily and again after every resource
 * reload. Holders are plain objects: common code (statements) may hand them out, but only the client resolves them. */
public class SpriteHolderRegistry {
    public static final boolean DEBUG = BCDebugging.shouldDebugLog("lib.sprite.holder");

    private static final Map<Identifier, SpriteHolder> HOLDER_MAP = new HashMap<>();
    /** Bumped on every resource reload so holders look their sprite up again. */
    private static int generation = 0;

    public static synchronized SpriteHolder getHolder(Identifier location) {
        SpriteHolder holder = HOLDER_MAP.get(location);
        if (holder == null) {
            holder = new SpriteHolder(location);
            HOLDER_MAP.put(location, holder);
            if (DEBUG) {
                BCLog.logger.info("[lib.sprite.holder] Created a new sprite holder for " + location);
            }
        }
        return holder;
    }

    public static SpriteHolder getHolder(String location) {
        return getHolder(Identifier.parse(location));
    }

    /** Called by the client after resources (and so the atlases) reload. */
    @OnlyIn(Dist.CLIENT)
    public static void onResourcesReloaded() {
        generation++;
    }

    /** Holds a reference to a sprite on the block atlas, refreshed when the resource packs are reloaded. */
    public static class SpriteHolder implements ISprite {
        public final Identifier spriteLocation;
        private Object sprite;
        private int spriteGeneration = -1;

        private SpriteHolder(Identifier spriteLocation) {
            this.spriteLocation = spriteLocation;
        }

        @Nullable
        @OnlyIn(Dist.CLIENT)
        public TextureAtlasSprite getSprite() {
            if (spriteGeneration != generation || sprite == null) {
                Minecraft mc = Minecraft.getInstance();
                if (mc == null || mc.getAtlasManager() == null) {
                    return null;
                }
                sprite = mc.getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(spriteLocation);
                spriteGeneration = generation;
            }
            return (TextureAtlasSprite) sprite;
        }

        @Override
        public double getInterpU(double u) {
            TextureAtlasSprite s = getSprite();
            return s == null ? u : s.getU0() + u * (s.getU1() - s.getU0());
        }

        @Override
        public double getInterpV(double v) {
            TextureAtlasSprite s = getSprite();
            return s == null ? v : s.getV0() + v * (s.getV1() - s.getV0());
        }

        @Override
        public void bindTexture() {
            // 26.3 draws through render pipelines that name their textures; nothing is bound globally any more.
        }

        @Override
        public String toString() {
            return "SpriteHolder[" + spriteLocation + "]";
        }
    }
}
