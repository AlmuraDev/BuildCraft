/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.item;

import net.minecraft.world.item.Item;

import buildcraft.api.enums.EnumRedstoneChipset;

import buildcraft.lib.item.ItemBC_Neptune;

/** One redstone chipset type. 1.12.2 had a single "redstone_chipset" item with the type in its damage; 26.3 has one
 * item per type ("redstone_chipset_iron", see {@link EnumRedstoneChipset#getItemId()}). */
public class ItemRedstoneChipset extends ItemBC_Neptune {
    public final EnumRedstoneChipset type;

    public ItemRedstoneChipset(Item.Properties properties, String id, EnumRedstoneChipset type) {
        super(properties, id);
        this.type = type;
    }
}
