/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.client.guide.parts;

import java.io.IOException;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;

import buildcraft.api.core.BCLog;
import buildcraft.api.core.render.ISprite;

import buildcraft.lib.client.guide.GuiGuide;
import buildcraft.lib.client.sprite.SpriteAtlas;
import buildcraft.lib.client.sprite.SpriteHolderRegistry;
import buildcraft.lib.client.sprite.SpriteRaw;

public class GuideImageFactory implements GuidePartFactory {
    private final ISprite sprite;
    private final int srcWidth, srcHeight;
    private final int width, height;

    public GuideImageFactory(String location) {
        this(location, -1, -1);
    }

    public GuideImageFactory(String location, int width, int height) {
        TextureAtlasSprite stitched = SpriteHolderRegistry.lookupSprite(Identifier.parse(location));
        TextureAtlasSprite missing = SpriteHolderRegistry.missingSprite();
        if (stitched != missing) {
            this.sprite = new SpriteAtlas(stitched);
            this.srcWidth = stitched.contents().width();
            this.srcHeight = stitched.contents().height();
        } else {
            ISprite s;
            int sw, sh;
            Identifier resLoc = Identifier.parse(location);
            java.util.Optional<net.minecraft.server.packs.resources.Resource> resource =
                Minecraft.getInstance().getResourceManager().getResource(resLoc);
            try (java.io.InputStream stream = resource.isPresent() ? resource.get().open() : null) {
                if (stream == null) {
                    throw new java.io.FileNotFoundException(resLoc.toString());
                }
                net.minecraft.util.PngInfo size = net.minecraft.util.PngInfo.fromStream(stream);
                s = new SpriteRaw(resLoc, 0, 0, 1, 1);
                sw = size.width();
                sh = size.height();
            } catch (IOException io) {
                BCLog.logger.warn("[lib.guide.loader.image] Couldn't load image '" + resLoc + "' because " + io.getMessage());
                s = new SpriteAtlas(missing);
                sw = missing.contents().width();
                sh = missing.contents().height();
            }
            this.sprite = s;
            this.srcWidth = sw;
            this.srcHeight = sh;
        }
        this.width = width <= 0 ? srcWidth : width;
        this.height = height <= 0 ? srcHeight : height;
    }

    @Override
    public GuideImage createNew(GuiGuide gui) {
        return new GuideImage(gui, sprite, srcWidth, srcHeight, width, height);
    }
}
