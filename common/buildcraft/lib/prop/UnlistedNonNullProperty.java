/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.prop;

import java.util.Objects;

import net.neoforged.neoforge.model.data.ModelProperty;

/** A model data property that never holds null (1.12.2's unlisted block state property; 26.3 models read these from
 * {@link net.neoforged.neoforge.model.data.ModelData}). */
public class UnlistedNonNullProperty<V> extends ModelProperty<V> {
    public final String name;

    public UnlistedNonNullProperty(String name) {
        super(Objects::nonNull);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String valueToString(V value) {
        return value.toString();
    }
}
