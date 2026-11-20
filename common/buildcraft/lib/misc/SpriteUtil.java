/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.misc;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.mojang.authlib.GameProfile;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;


import buildcraft.lib.client.sprite.SpriteHolderRegistry;
import buildcraft.lib.gui.GuiDraw;

public class SpriteUtil {
    private static final Map<GameProfile, Supplier<PlayerSkin>> SKINS = new HashMap<>();

    public static void bindBlockTextureMap() {
        GuiDraw.bindBlockAtlas();
    }

    public static void bindTexture(String identifier) {
        bindTexture(Identifier.parse(identifier));
    }

    public static void bindTexture(Identifier identifier) {
        GuiDraw.bind(identifier);
    }

    /** Transforms the given {@link Identifier}, adding ".png" to the end and prepending "textures/" to its path (a
     * sprite name to its texture file). */
    public static Identifier transformLocation(Identifier location) {
        return Identifier.fromNamespaceAndPath(location.getNamespace(), "textures/" + location.getPath() + ".png");
    }

    /** @return The skin of the given profile (the default skin until it has been downloaded), or null for none. */
    @Nullable
    public static PlayerSkin getSkin(@Nullable GameProfile profile) {
        if (profile == null) {
            return null;
        }
        return SKINS.computeIfAbsent(profile, p -> Minecraft.getInstance().getSkinManager().createLookup(p, false))
            .get();
    }

    public static TextureAtlasSprite missingSprite() {
        return SpriteHolderRegistry.missingSprite();
    }
}
