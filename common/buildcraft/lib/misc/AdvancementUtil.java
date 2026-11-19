package buildcraft.lib.misc;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.Identifier;

import net.neoforged.neoforge.server.ServerLifecycleHooks;

import buildcraft.api.core.BCLog;

public class AdvancementUtil {
    private static final Set<Identifier> UNKNOWN_ADVANCEMENTS = new HashSet<>();

    public static void unlockAdvancement(Player player, Identifier advancementName) {
        if (player instanceof ServerPlayer) {
            ServerPlayer playerMP = (ServerPlayer) player;
            ServerAdvancementManager advancementManager = playerMP.level().getServer().getAdvancements();
            AdvancementHolder advancement = advancementManager.get(advancementName);
            if (advancement != null) {
                // never assume the advancement exists, we create them but they are removable by datapacks
                playerMP.getAdvancements().award(advancement, "code_trigger");
            } else if (UNKNOWN_ADVANCEMENTS.add(advancementName)) {
                BCLog.logger.warn("[lib.advancement] Attempted to trigger undefined advancement: " + advancementName);
            }
        }
    }

    public static boolean unlockAdvancement(UUID player, Identifier advancementName) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        Entity entity = server == null ? null : server.getPlayerList().getPlayer(player);
        if (entity != null && entity instanceof ServerPlayer) {
            unlockAdvancement((Player) entity, advancementName);
            return true;
        }
        return false;
    }
}
