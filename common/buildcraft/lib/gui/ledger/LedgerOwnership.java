/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.gui.ledger;

import com.mojang.authlib.GameProfile;

import net.minecraft.resources.Identifier;

import buildcraft.api.core.render.ISprite;

import buildcraft.lib.gui.BuildCraftGui;
import buildcraft.lib.gui.GuiIcon;
import buildcraft.lib.gui.config.GuiConfigManager;
import buildcraft.lib.misc.SpriteUtil;
import buildcraft.lib.tile.TileBC_Neptune;

public class LedgerOwnership extends Ledger_Neptune {

    private final TileBC_Neptune tile;

    public LedgerOwnership(BuildCraftGui gui, TileBC_Neptune tile, boolean expandPositive) {
        super(gui, 0xFF_E0_F0_FF, expandPositive);
        this.title = "gui.ledger.ownership";
        this.tile = tile;

        appendText(this::getOwnerName, 0);

        calculateMaxSize();
        setOpenProperty(GuiConfigManager.getOrAddBoolean(Identifier.parse("buildcraftlib:base"),
            "ledger.owner.is_open", false));
    }

    @Override
    protected void drawIcon(double x, double y) {
        net.minecraft.world.entity.player.PlayerSkin skin = SpriteUtil.getSkin(tile.getOwner());
        net.minecraft.client.gui.GuiGraphicsExtractor graphics = buildcraft.lib.gui.GuiDraw.graphics();
        if (skin == null || graphics == null) {
            GuiIcon.draw(buildcraft.lib.BCLibSprites.HELP, x, y, x + 16, y + 16);
            return;
        }
        // The face with its hat layer (1.12.2 drew the two layers as sprites)
        net.minecraft.client.gui.components.PlayerFaceExtractor.extractRenderState(graphics, skin, (int) x, (int) y,
            16);
    }

    private String getOwnerName() {
        GameProfile owner = tile.getOwner();
        if (owner == null) {
            return "no-one";
        }
        return owner.name();
    }
}
