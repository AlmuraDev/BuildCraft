/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.snapshot;

import net.minecraft.core.registries.BuiltInRegistries;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.StreamSupport;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;

public class ItemBlocks {
    private static final List<Item> LIST = new ArrayList<>();

    static {
        StreamSupport.stream(BuiltInRegistries.ITEM.spliterator(), false)
            // 1.12.2 also listed beds, doors, skulls and signs, which weren't ItemBlocks then; on 26.3 they are
            .filter(item -> item instanceof BlockItem)
            .forEach(ItemBlocks::add);
    }

    public static void add(Item... items) {
        LIST.addAll(Arrays.asList(items));
    }

    public static List<Item> getList() {
        return Collections.unmodifiableList(LIST);
    }
}
