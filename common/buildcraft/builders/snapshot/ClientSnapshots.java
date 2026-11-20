/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.snapshot;

import java.util.ArrayList;
import java.util.List;

import buildcraft.lib.net.MessageManager;

/** The client's cache of snapshots it asked the server for (by key).
 * TODO(26.3 GUIs batch): 1.12.2 also drew snapshots into GUIs (renderSnapshot), through a fake world baked into a
 * vertex buffer; that comes back with the builders GUIs. */
public enum ClientSnapshots {
    INSTANCE;

    private final List<Snapshot> snapshots = new ArrayList<>();
    private final List<Snapshot.Key> pending = new ArrayList<>();

    public Snapshot getSnapshot(Snapshot.Key key) {
        Snapshot found = snapshots.stream().filter(snapshot -> snapshot.key.equals(key)).findFirst().orElse(null);
        if (found == null && !pending.contains(key)) {
            pending.add(key);
            MessageManager.sendToServer(new MessageSnapshotRequest(key));
        }
        return found;
    }

    public void onSnapshotReceived(Snapshot snapshot) {
        pending.remove(snapshot.key);
        snapshots.add(snapshot);
    }
}
