/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.net;

import java.io.IOException;

import buildcraft.lib.net.IMessage;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public interface IPayloadReceiver {
    IMessage receivePayload(IPayloadContext ctx, PacketBufferBC buffer) throws IOException;
}
