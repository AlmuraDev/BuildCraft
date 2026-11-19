/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.energy.tile;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;

import buildcraft.api.tiles.IDebuggable;

import buildcraft.lib.misc.AdvancementUtil;
import buildcraft.lib.tile.TileBC_Neptune;

import buildcraft.core.tile.ITileOilSpring;

/** The oil spring under an oil well: counts how much of the well each player pumped, for the "black gold"
 * advancement. (1.12.2 extended BlockEntity directly; this uses TileBC_Neptune for its 1.12 NBT bridge, which the
 * 3.5 world converter relies on.) */
public class TileSpringOil extends TileBC_Neptune implements IDebuggable, ITileOilSpring {

    private static final Identifier ADVANCEMENT_PUMP_LARGE_OIL_WELL = Identifier.parse("buildcraftfactory:black_gold");

    private final Map<UUID, PlayerPumpInfo> pumpProgress = new ConcurrentHashMap<>();

    /** An approximation of the total number of oil source blocks in the oil spring. The actual number will be less than
     * this, so this is taken as an approximation.
     * <p>
     * Note that this SHOULD NEVER be set! (Except by the generator, and readFromNbt) */
    public int totalSources;

    public TileSpringOil(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @Override
    public void onPumpOil(GameProfile profile, BlockPos oilPos) {
        if (profile == null || level == null) {
            return;
        }
        PlayerPumpInfo info = pumpProgress.computeIfAbsent(profile.id(), id -> new PlayerPumpInfo(id, profile.name()));
        info.lastPumpTick = level.getGameTime();
        info.sourcesPumped++;
        setChanged();

        if (info.sourcesPumped >= totalSources * 7 / 8) {
            if (oilPos.equals(worldPosition.above())) {
                AdvancementUtil.unlockAdvancement(profile.id(), ADVANCEMENT_PUMP_LARGE_OIL_WELL);
            }
        }
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        // 1.12.2 wrote this but never read it back (so wells reset to 0 after a reload); read it now.
        totalSources = nbt.getIntOr("totalSources", 0);
        pumpProgress.clear();
        ListTag list = nbt.getListOrEmpty("pumpProgress");
        for (int i = 0; i < list.size(); i++) {
            PlayerPumpInfo info = PlayerPumpInfo.read(list.getCompoundOrEmpty(i));
            if (info != null) {
                pumpProgress.put(info.id, info);
            }
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
        long now = level == null ? 0 : level.getGameTime();
        for (PlayerPumpInfo info : pumpProgress.values()) {
            if (!added) {
                left.add("Player Progress:");
                added = true;
            }
            left.add("  " + info.name + " = " + info.sourcesPumped + " ( " + (now - info.lastPumpTick) / 20 + "s )");
        }
    }

    /** One player's progress. Stored as 1.12.2's NBTUtil game profile layout: profile = {Id: uuid string, Name}. */
    static class PlayerPumpInfo {
        final UUID id;
        final String name;
        long lastPumpTick = -1;
        int sourcesPumped = 0;

        PlayerPumpInfo(UUID id, String name) {
            this.id = id;
            this.name = name;
        }

        static PlayerPumpInfo read(CompoundTag nbt) {
            CompoundTag profile = nbt.getCompoundOrEmpty("profile");
            String idString = profile.getStringOr("Id", "");
            UUID id;
            try {
                id = UUID.fromString(idString);
            } catch (IllegalArgumentException e) {
                return null;
            }
            PlayerPumpInfo info = new PlayerPumpInfo(id, profile.getStringOr("Name", ""));
            info.lastPumpTick = nbt.getLongOr("lastPumpTick", 0L);
            info.sourcesPumped = nbt.getIntOr("sourcesPumped", 0);
            return info;
        }

        CompoundTag writeToNbt() {
            CompoundTag nbt = new CompoundTag();
            CompoundTag profile = new CompoundTag();
            profile.putString("Id", id.toString());
            profile.putString("Name", name);
            nbt.put("profile", profile);
            nbt.putLong("lastPumpTick", lastPumpTick);
            nbt.putInt("sourcesPumped", sourcesPumped);
            return nbt;
        }
    }
}
