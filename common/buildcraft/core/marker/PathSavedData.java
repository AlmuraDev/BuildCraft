/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.core.marker;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedDataType;

import buildcraft.lib.marker.MarkerSavedData;
import buildcraft.lib.misc.data.NbtSavedData;

public class PathSavedData extends MarkerSavedData<PathSubCache, PathConnection> {
    public static final String NAME = "buildcraft_marker_path";
    public static final SavedDataType<PathSavedData> TYPE =
        NbtSavedData.type(Identifier.fromNamespaceAndPath("buildcraftcore", "marker_path"), PathSavedData::new);

    public PathSavedData(String name) {
        super(name);
    }

    public PathSavedData() {
        this(NAME);
    }

    public void loadInto(PathSubCache subCache) {
        setCache(subCache);
        for (BlockPos p : markerPositions) {
            subCache.loadMarker(p, null);
        }
        for (List<BlockPos> list : markerConnections) {
            subCache.addConnection(new PathConnection(subCache, list));
        }
    }
}
