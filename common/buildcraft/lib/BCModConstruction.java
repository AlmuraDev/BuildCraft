/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib;

/** NeoForge constructs mods in parallel, but BuildCraft's modules share global state while they set up (tag batches,
 * the shared config files, message, statement and registry maps), which 1.12.2's one-at-a-time loading never had to
 * guard. Every BuildCraft module constructs itself while holding {@link #LOCK}. */
public final class BCModConstruction {
    public static final Object LOCK = new Object();

    private BCModConstruction() {}
}
