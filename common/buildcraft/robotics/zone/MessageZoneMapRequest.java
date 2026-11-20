/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.robotics.zone;

import io.netty.buffer.ByteBuf;

import buildcraft.lib.net.IMessage;
import buildcraft.lib.net.IMessageHandler;

import buildcraft.lib.misc.MessageUtil;

public class MessageZoneMapRequest implements IMessage {
    private ZonePlannerMapChunkKey key;

    @SuppressWarnings("unused")
    public MessageZoneMapRequest() {
    }

    public MessageZoneMapRequest(ZonePlannerMapChunkKey key) {
        this.key = key;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        key = new ZonePlannerMapChunkKey(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        key.toBytes(buf);
    }

    /** Answers with the map chunk, read from the asking player's own dimension (1.12.2 did the same). */
    public static final IMessageHandler<MessageZoneMapRequest, IMessage> HANDLER = (message, ctx) -> {
        net.minecraft.world.level.Level world = ctx.player().level();
        if (!world.dimension().equals(message.key.dimension)) {
            return null;
        }
        MessageUtil.sendReturnMessage(ctx,
            new MessageZoneMapResponse(message.key, ZonePlannerMapDataServer.INSTANCE.getChunk(world, message.key)));
        return null;
    };
}
