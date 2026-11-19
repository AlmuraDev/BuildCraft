/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib;

import net.neoforged.bus.api.IEventBus;

import buildcraft.lib.item.ItemDebugger;
import buildcraft.lib.item.ItemGuide;
import buildcraft.lib.item.ItemGuideNote;
import buildcraft.lib.registry.RegistrationHelper;

public class BCLibItems {

    public static ItemGuide guide;
    public static ItemGuideNote guideNote;
    public static ItemDebugger debugger;

    private static boolean enableGuide, enableDebugger;

    /** Must be called (by core) before lib's items are registered. */
    public static void enableGuide() {
        enableGuide = true;
    }

    public static void enableDebugger() {
        enableDebugger = true;
    }

    public static boolean isGuideEnabled() {
        return enableGuide;
    }

    public static boolean isDebuggerEnabled() {
        return enableDebugger;
    }

    /** Registers lib's items. They're only created (during registration) if a module enabled them by then. */
    public static void fmlPreInit(IEventBus modBus) {
        RegistrationHelper helper = new RegistrationHelper(BCLib.MODID, modBus);
        helper.addItem("item.guide", (String) null, (props, id) -> isGuideEnabled() ? new ItemGuide(props, id) : null,
            item -> guide = item);
        helper.addItem("item.guide.note", (String) null,
            (props, id) -> isGuideEnabled() ? new ItemGuideNote(props, id) : null, item -> guideNote = item);
        helper.addItem("item.debugger", (String) null,
            (props, id) -> isDebuggerEnabled() ? new ItemDebugger(props, id) : null, item -> debugger = item);
    }
}
