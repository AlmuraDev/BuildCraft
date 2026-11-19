/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.tile;

/** A tile that does something every tick, on both sides, like 1.12's ITickable. BuildCraft's tile blocks give such tiles a
 * ticker that calls {@link #update()}. */
public interface ITickable {
    void update();
}
