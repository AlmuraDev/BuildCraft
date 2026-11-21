/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.net;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.FurnaceRecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;

/** The guide book's crafting and smelting panels need every recipe, and 26.3 clients only get what the recipe book
 * shows. The client asks once per connection ({@link Request}); the server answers with the displays of all its
 * crafting and smelting recipes ({@link Response}). */
public final class MessageGuideRecipes {
    private MessageGuideRecipes() {}

    /** Recipe displays per {@link Response}; a few hundred bytes at most each. */
    static final int PER_MESSAGE = 500;

    /** Client to server: "send me the recipes". */
    public static class Request implements IMessage {
        @Override
        public void toBytes(ByteBuf buf) {}

        @Override
        public void fromBytes(ByteBuf buf) {}

        public static final IMessageHandler<Request, Response> HANDLER = (message, ctx) -> {
            if (!(ctx.player() instanceof ServerPlayer player)) {
                return null;
            }
            MinecraftServer server = player.level().getServer();
            if (server == null) {
                return null;
            }
            List<RecipeDisplay> displays = new ArrayList<>();
            for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
                for (RecipeDisplay display : holder.value().display()) {
                    if (isShown(display)) {
                        displays.add(display);
                    }
                }
            }
            // Sent in parts: a custom payload to the client is capped at 1 MiB
            for (int start = 0; start < displays.size(); start += PER_MESSAGE) {
                int end = Math.min(displays.size(), start + PER_MESSAGE);
                MessageManager.sendTo(new Response(displays.subList(start, end), end == displays.size()), player);
            }
            if (displays.isEmpty()) {
                MessageManager.sendTo(new Response(List.of(), true), player);
            }
            return null;
        };
    }

    /** Server to client: every crafting and smelting recipe display. */
    public static class Response implements IMessage {
        public List<RecipeDisplay> displays;
        /** True on the last part. */
        public boolean last;

        @SuppressWarnings("unused")
        public Response() {
            displays = new ArrayList<>();
        }

        public Response(List<RecipeDisplay> displays, boolean last) {
            this.displays = displays;
            this.last = last;
        }

        @Override
        public void toBytes(ByteBuf buf) {
            RegistryFriendlyByteBuf rbuf = (RegistryFriendlyByteBuf) buf;
            rbuf.writeBoolean(last);
            rbuf.writeVarInt(displays.size());
            for (RecipeDisplay display : displays) {
                RecipeDisplay.STREAM_CODEC.encode(rbuf, display);
            }
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            RegistryFriendlyByteBuf rbuf = (RegistryFriendlyByteBuf) buf;
            last = rbuf.readBoolean();
            int count = rbuf.readVarInt();
            displays = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                displays.add(RecipeDisplay.STREAM_CODEC.decode(rbuf));
            }
        }
    }

    /** @return True for the kinds of display the guide draws (crafting grids and furnaces). */
    public static boolean isShown(RecipeDisplay display) {
        return display instanceof ShapedCraftingRecipeDisplay || display instanceof ShapelessCraftingRecipeDisplay
            || display instanceof FurnaceRecipeDisplay;
    }
}
