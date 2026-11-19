/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.client.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.resources.Identifier;


import buildcraft.api.core.BCDebugging;
import buildcraft.api.core.BCLog;


public class ModelHolderRegistry {
    public static final boolean DEBUG = BCDebugging.shouldDebugLog("lib.model.holder");

    static final List<ModelHolder> HOLDERS = new ArrayList<>();

    /** Reloads and bakes every model holder. Called once 26.3 has baked its own models, so the atlases (stitched from
     * their JSON sources, which must list the models' texture folders) are ready. */
    public static void onModelBake() {
        Set<Identifier> unused = new HashSet<>();
        for (ModelHolder holder : HOLDERS) {
            holder.onTextureStitchPre(unused);
        }
        for (ModelHolder holder : HOLDERS) {
            holder.onModelBake();
        }
        if (DEBUG) {
            BCLog.logger.info("[lib.model.holder] List of registered Models:");
            List<ModelHolder> holders = new ArrayList<>();
            holders.addAll(HOLDERS);
            holders.sort(Comparator.comparing(a -> a.modelLocation.toString()));

            for (ModelHolder holder : holders) {
                String status = "  ";
                if (holder.failReason != null) {
                    status += "(" + holder.failReason + ")";
                } else if (!holder.hasBakedQuads()) {
                    status += "(Model was registered too late)";
                }

                BCLog.logger.info("  - " + holder.modelLocation + status);
            }
            BCLog.logger.info("[lib.model.holder] Total of " + HOLDERS.size() + " models");
        }
    }
}
