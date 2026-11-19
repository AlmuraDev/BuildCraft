/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.gui.slot;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;

public class SlotHidden extends Slot {

    private boolean hidden = false;

    public SlotHidden(Container inv, int index, int x, int y) {
        super(inv, index, x, y);
    }

    // 26.3 slot positions are final, so hiding switches the slot off instead of moving it away.
    public void show() {
        hidden = false;
    }

    public void hide() {
        hidden = true;
    }

    @Override
    public boolean isActive() {
        return !hidden;
    }
}
