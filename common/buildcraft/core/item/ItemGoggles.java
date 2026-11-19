/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core.item;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;

import buildcraft.lib.item.ItemBC_Neptune;

/** Goggles worn on the head that give no armour and never wear out (dev only). */
public class ItemGoggles extends ItemBC_Neptune {
    public ItemGoggles(Item.Properties properties, String id) {
        super(properties.equippable(EquipmentSlot.HEAD).stacksTo(1), id);
    }
}
