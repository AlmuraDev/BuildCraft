package buildcraft.energy.tile;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.authlib.GameProfile;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;


import buildcraft.api.tiles.IDebuggable;

import buildcraft.lib.misc.AdvancementUtil;

import buildcraft.core.tile.ITileOilSpring;

// We don't extend TileBC here because we have no need of any of its functions.
public class TileSpringOil extends BlockEntity implements IDebuggable, ITileOilSpring {

    private static final Identifier ADVANCEMENT_PUMP_LARGE_OIL_WELL = Identifier.parse("buildcraftfactory:black_gold");

    private final Map<GameProfile, PlayerPumpInfo> pumpProgress = new ConcurrentHashMap<>();

    /** An approximation of the total number of oil source blocks in the oil spring. The actual number will be less than
     * this, so this is taken as an approximation.
     * <p>
     * Note that this SHOULD NEVER be set! (Except by the generator, and readFromNbt) */
    public int totalSources;

    @Override
    public void onPumpOil(GameProfile profile, BlockPos oilPos) {
        if (profile == null) {
            // BCLog.logger.warn("Unknown owner for pump at " + pump.getPos());
            return;
        }
        PlayerPumpInfo info = pumpProgress.computeIfAbsent(profile, PlayerPumpInfo::new);
        info.lastPumpTick = world.getGameTime();
        info.sourcesPumped++;

        // BCLog.logger.info("Pumped " + info.sourcesPumped + " / " + totalSources + " at " + oilPos + " (for " +
        // System.identityHashCode(this) + ", "+getPos()+")");
        if (info.sourcesPumped >= totalSources * 7 / 8) {
            // BCLog.logger.info("Pumped nearly all oil blocks!");
            if (oilPos.equals(getPos().up())) {
                AdvancementUtil.unlockAdvancement(profile.getId(), ADVANCEMENT_PUMP_LARGE_OIL_WELL);
            }
        }
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        ListTag list = nbt.getListOrEmpty("pumpProgress");
        for (int i = 0; i < list.size(); i++) {
            PlayerPumpInfo info = new PlayerPumpInfo(list.getCompoundOrEmpty(i));
            pumpProgress.put(info.profile, info);
        }
    }

    @Override
    public CompoundTag writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        nbt.putInt("totalSources", totalSources);
        ListTag list = new ListTag();
        for (PlayerPumpInfo info : pumpProgress.values()) {
            list.add(info.writeToNbt());
        }
        nbt.put("pumpProgress", list);
        return nbt;
    }

    @Override
    public void getDebugInfo(List<String> left, List<String> right, Direction side) {
        left.add("totalSources = " + totalSources);
        boolean added = false;
        for (PlayerPumpInfo info : pumpProgress.values()) {
            if (!added) {
                left.add("Player Progress:");
                added = true;
            }
            left.add("  " + info.profile.getName() + " = " + info.sourcesPumped + " ( "
                + (world.getGameTime() - info.lastPumpTick) / 20 + "s )");
        }
    }

    static class PlayerPumpInfo {
        final GameProfile profile;
        long lastPumpTick = -1;
        int sourcesPumped = 0;

        public PlayerPumpInfo(GameProfile profile) {
            this.profile = profile;
        }

        public PlayerPumpInfo(CompoundTag nbt) {
            profile = NbtUtils.readGameProfileFromNBT(nbt.getCompoundOrEmpty("profile"));
            lastPumpTick = nbt.getLongOr("lastPumpTick", 0L);
            sourcesPumped = nbt.getIntOr("sourcesPumped", 0);
        }

        public CompoundTag writeToNbt() {
            CompoundTag nbt = new CompoundTag();
            nbt.put("profile", NbtUtils.writeGameProfile(new CompoundTag(), profile));
            nbt.putLong("lastPumpTick", lastPumpTick);
            nbt.putInt("sourcesPumped", sourcesPumped);
            return nbt;
        }
    }
}
