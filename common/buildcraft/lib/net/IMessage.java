/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.net;

import io.netty.buffer.ByteBuf;

/** A BuildCraft network message: it writes and reads its own bytes, like Forge 1.12's IMessage. {@link MessageManager} sends
 * each registered message class as its own NeoForge payload. Message classes need a public no-argument constructor. */
public interface IMessage {
    void fromBytes(ByteBuf buf);

    void toBytes(ByteBuf buf);
}
