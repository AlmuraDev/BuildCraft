/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.net;

import java.lang.reflect.Constructor;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.annotation.Nullable;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import buildcraft.api.IBuildCraftMod;
import buildcraft.api.core.BCDebugging;
import buildcraft.api.core.BCLog;

import buildcraft.lib.BCLibProxy;
import buildcraft.lib.misc.MessageUtil;

/** Registers BuildCraft's {@link IMessage}s and sends them. Each message class becomes its own NeoForge play payload,
 * "&lt;modid&gt;:&lt;lowercase class name&gt;", which carries the bytes the message writes. Handlers run on the main thread.
 * <p>
 * Messages must be registered (usually from a module's constructor) before NeoForge fires its payload registration event;
 * {@link #onRegisterPayloads(RegisterPayloadHandlersEvent)} must be on the mod event bus. */
public class MessageManager {
    public static final boolean DEBUG = BCDebugging.shouldDebugLog("lib.messages");

    /** Bumped whenever a message's format changes, so mismatched clients and servers refuse each other. */
    public static final String PROTOCOL_VERSION = "2";

    private static final Map<String, SortedMap<String, PerMessageInfo<?>>> MOD_MESSAGES = new TreeMap<>();
    private static final Map<Class<? extends IMessage>, PerMessageInfo<?>> MESSAGE_HANDLERS = new HashMap<>();
    private static boolean registered = false;

    /** The payload that carries one BuildCraft message. */
    public static final class BcPayload implements CustomPacketPayload {
        final PerMessageInfo<?> info;
        final IMessage message;

        BcPayload(PerMessageInfo<?> info, IMessage message) {
            this.info = info;
            this.message = message;
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return info.type;
        }
    }

    static final class PerMessageInfo<I extends IMessage> {
        final String modId;
        final Class<I> messageClass;
        final CustomPacketPayload.Type<BcPayload> type;
        final Constructor<I> constructor;

        @Nullable
        IMessageHandler<I, ?> clientHandler, serverHandler;

        PerMessageInfo(String modId, Class<I> messageClass) {
            this.modId = modId;
            this.messageClass = messageClass;
            String path = messageClass.getSimpleName().toLowerCase(Locale.ROOT);
            this.type = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(modId, path));
            try {
                this.constructor = messageClass.getDeclaredConstructor();
                this.constructor.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException("Message " + messageClass + " needs a no-argument constructor!", e);
            }
        }

        I create() {
            try {
                return constructor.newInstance();
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Couldn't create " + messageClass, e);
            }
        }

        StreamCodec<RegistryFriendlyByteBuf, BcPayload> codec() {
            return StreamCodec.of(
                (buf, payload) -> payload.message.toBytes(buf),
                buf -> {
                    I message = create();
                    message.fromBytes(buf);
                    return new BcPayload(this, message);
                }
            );
        }

        @SuppressWarnings("unchecked")
        void handle(BcPayload payload, IPayloadContext ctx, @Nullable IMessageHandler<I, ?> handler) {
            LogicalSide side = MessageUtil.getSide(ctx);
            if (handler == null) {
                if (side == LogicalSide.SERVER) {
                    // Bad/Buggy client
                    ServerPlayer player = (ServerPlayer) ctx.player();
                    BCLog.logger.warn("[lib.messages] The client " + player.getName().getString() + " (ID = "
                        + player.getGameProfile().id() + ") sent an invalid message " + messageClass
                        + ", when they should only receive them!");
                } else {
                    BCLog.logger.warn("[lib.messages] Received message " + messageClass
                        + " on the client, when it should only be sent by the client and received on the server!");
                }
                return;
            }
            if (BCLibProxy.getProxy().getPlayerForContext(ctx) == null) {
                return;
            }
            IMessage reply = handler.onMessage((I) payload.message, ctx);
            if (reply != null) {
                MessageUtil.sendReturnMessage(ctx, reply);
            }
        }
    }

    public static <I extends IMessage> void registerMessageClass(IBuildCraftMod module, Class<I> clazz, LogicalSide... sides) {
        registerMessageClass(module, clazz, null, sides);
    }

    @SuppressWarnings("unchecked")
    public static <I extends IMessage> void registerMessageClass(IBuildCraftMod module, Class<I> messageClass,
        IMessageHandler<I, ?> messageHandler, LogicalSide... sides) {
        String modId = module.getModId();
        PerMessageInfo<I> messageInfo = (PerMessageInfo<I>) MESSAGE_HANDLERS.get(messageClass);
        if (messageInfo == null) {
            if (registered) {
                throw new IllegalStateException("Too late to register the message " + messageClass);
            }
            messageInfo = new PerMessageInfo<>(modId, messageClass);
            MOD_MESSAGES.computeIfAbsent(modId, k -> new TreeMap<>(Comparator.naturalOrder()))
                .put(messageClass.getName(), messageInfo);
            MESSAGE_HANDLERS.put(messageClass, messageInfo);
        }

        if (messageHandler == null) {
            if (DEBUG) {
                BCLog.logger.info("[lib.messages] Registered message " + messageClass + " for " + modId);
            }
            return;
        }

        LogicalSide specificSide = sides != null && sides.length == 1 ? sides[0] : null;

        if (specificSide == null || specificSide == LogicalSide.CLIENT) {
            if (messageInfo.clientHandler != null && DEBUG) {
                BCLog.logger.info("[lib.messages] Replacing existing client handler for " + modId + " " + messageClass
                    + " " + messageInfo.clientHandler + " with " + messageHandler);
            }
            messageInfo.clientHandler = messageHandler;
        }
        if (specificSide == null || specificSide == LogicalSide.SERVER) {
            if (messageInfo.serverHandler != null && DEBUG) {
                BCLog.logger.info("[lib.messages] Replacing existing server handler for " + modId + " " + messageClass
                    + " " + messageInfo.serverHandler + " with " + messageHandler);
            }
            messageInfo.serverHandler = messageHandler;
        }
    }

    /** Sets the handler of an already registered message (client handlers are usually set from client-only code). */
    @SuppressWarnings("unchecked")
    public static <I extends IMessage> void setHandler(Class<I> messageClass, IMessageHandler<I, ?> messageHandler,
        LogicalSide side) {
        PerMessageInfo<I> messageInfo = (PerMessageInfo<I>) MESSAGE_HANDLERS.get(messageClass);
        if (messageInfo == null) {
            throw new IllegalArgumentException("Cannot set handler for unregistered message: " + messageClass);
        }
        if (side == LogicalSide.CLIENT) {
            messageInfo.clientHandler = messageHandler;
        } else {
            messageInfo.serverHandler = messageHandler;
        }
    }

    /** Registers every BuildCraft message as a NeoForge payload. Must be on the mod event bus (of buildcraftlib). */
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        registered = true;
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        for (Map.Entry<String, SortedMap<String, PerMessageInfo<?>>> mod : MOD_MESSAGES.entrySet()) {
            if (DEBUG) {
                BCLog.logger.info("[lib.messages]  - Module: " + mod.getKey());
            }
            for (PerMessageInfo<?> info : mod.getValue().values()) {
                registerPayload(registrar, info);
            }
        }
    }

    private static <I extends IMessage> void registerPayload(PayloadRegistrar registrar, PerMessageInfo<I> info) {
        // Handlers are looked up when a message arrives, so client handlers set later (from client setup) still apply.
        registrar.playBidirectional(info.type, info.codec(),
            (payload, ctx) -> info.handle(payload, ctx, info.serverHandler),
            (payload, ctx) -> info.handle(payload, ctx, info.clientHandler));
        if (DEBUG) {
            BCLog.logger.info("[lib.messages]      " + info.type.id() + ": " + info.messageClass);
        }
    }

    public static CustomPacketPayload toPayload(IMessage message) {
        PerMessageInfo<?> info = MESSAGE_HANDLERS.get(message.getClass());
        if (info == null) {
            throw new IllegalArgumentException("Cannot send unregistered message " + message.getClass());
        }
        return new BcPayload(info, message);
    }

    public static void sendToAll(IMessage message) {
        PacketDistributor.sendToAllPlayers(toPayload(message));
    }

    public static void sendTo(IMessage message, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, toPayload(message));
    }

    public static void sendToAllAround(IMessage message, ServerLevel level, double x, double y, double z, double radius) {
        PacketDistributor.sendToPlayersNear(level, null, x, y, z, radius, toPayload(message));
    }

    public static void sendToPlayersTrackingChunk(IMessage message, ServerLevel level, ChunkPos pos) {
        PacketDistributor.sendToPlayersTrackingChunk(level, pos, toPayload(message));
    }

    public static void sendToDimension(IMessage message, ServerLevel level) {
        PacketDistributor.sendToPlayersInDimension(level, toPayload(message));
    }

    /** Sends to every player in the given dimension of the running server (if it has that dimension). */
    public static void sendToDimension(IMessage message, ResourceKey<Level> dimension) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        ServerLevel level = server == null ? null : server.getLevel(dimension);
        if (level != null) {
            sendToDimension(message, level);
        }
    }

    /** Only callable on the client. */
    public static void sendToServer(IMessage message) {
        BCLibProxy.getProxy().sendToServer(toPayload(message));
    }
}
