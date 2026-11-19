/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.particle;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ParticleStatus;

import net.neoforged.fml.loading.FMLEnvironment;

public enum ParticleCountMultiplier implements IParticlePositionPipe {
    MINIMAL(2),
    DECREASED(7),
    ALL(13);

    /** @return The multiplier for the client's particle setting, or {@link #MINIMAL} anywhere else (a dedicated server
     *         has no particle setting, and must never touch the client classes behind it). */
    public static ParticleCountMultiplier getForOption() {
        if (!FMLEnvironment.getDist().isClient()) {
            return MINIMAL;
        }
        return ClientOption.get();
    }

    /** Only loaded on the client, by {@link #getForOption()}. */
    private static final class ClientOption {
        static ParticleCountMultiplier get() {
            ParticleStatus status = Minecraft.getInstance().options.particles().get();
            switch (status) {
                case ALL:
                    return ALL;
                case DECREASED:
                    return DECREASED;
                default:
                    return MINIMAL;
            }
        }
    }

    public static IParticlePositionPipe getOptionProvider() {
        return pos -> getForOption().pipe(pos);
    }

    private final int numExpanses;

    ParticleCountMultiplier(int numExpanses) {
        this.numExpanses = numExpanses;
    }

    @Override
    public List<ParticlePosition> pipe(ParticlePosition pos) {
        List<ParticlePosition> list = new ArrayList<>();

        for (int i = 0; i < numExpanses; i++) {
            list.add(new ParticlePosition(pos.position, pos.motion));
        }

        return list;
    }

}
