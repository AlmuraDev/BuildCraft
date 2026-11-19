/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.debug;

import net.minecraft.world.entity.player.Player;

/** Something drawn in the world apart from any block or entity (debug overlays, laser previews). Only ever called on
 * the client; it lives outside the client packages so that tiles can hand one out without referencing client code. */
public interface IDetachedRenderer {
    void render(Player player, float partialTicks);
}
