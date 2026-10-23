/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.wire;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.base.Predicates;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

import net.minecraftforge.common.util.Constants;

import buildcraft.api.transport.EnumWirePart;
import buildcraft.api.transport.IWireEmitter;
import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.api.transport.pluggable.PipePluggable;

import buildcraft.lib.net.MessageManager;

public class WorldSavedDataWireSystems extends WorldSavedData {
    public static final String DATA_NAME = "buildcraft_wire_systems";
    public World world;
    public final Map<WireSystem, Boolean> wireSystems = new HashMap<>();
    public boolean gatesChanged = true;
    public boolean structureChanged = true;
    public final List<WireSystem> changedSystems = new ArrayList<>();
    public final List<EntityPlayerMP> changedPlayers = new ArrayList<>();
    public final Map<WireSystem.WireElement, IWireEmitter> emittersCache = new HashMap<>();

    public WorldSavedDataWireSystems() {
        super(DATA_NAME);
    }

    public WorldSavedDataWireSystems(String name) {
        super(name);
    }

    public void markStructureChanged() {
        structureChanged = true;
        gatesChanged = true;
        emittersCache.clear();
    }

    public List<WireSystem> getWireSystemsWithElement(WireSystem.WireElement element) {
        return wireSystems.keySet().stream().filter(wireSystem -> wireSystem.hasElement(element)).collect(Collectors.toList());
    }

    public void removeWireSystem(WireSystem wireSystem) {
        wireSystems.remove(wireSystem);
        markStructureChanged();
    }

    public void buildAndAddWireSystem(WireSystem.WireElement element) {
        WireSystem wireSystem = new WireSystem().build(this, element);
        if(!wireSystem.isEmpty()) {
            wireSystems.put(wireSystem, false);
            wireSystems.put(wireSystem, wireSystem.update(this));
        }
        markStructureChanged();
    }

    public void rebuildWireSystemsAround(IPipeHolder holder) {
        Arrays.stream(EnumWirePart.values())
                .flatMap(part -> WireSystem.getConnectedElementsOfElement(world, new WireSystem.WireElement(holder.getPipePos(), part)).stream())
                .distinct()
                .forEach(this::buildAndAddWireSystem);
    }

    public IWireEmitter getEmitter(WireSystem.WireElement element) {
        if (element.type == WireSystem.WireElement.Type.EMITTER_SIDE) {
            if (!emittersCache.containsKey(element)) {
                if (!world.isBlockLoaded(element.blockPos)) {
                    // Almura: don't load the chunk to find it (it used to, logging "Ghost loading ..."); see isEmitterEmitting
                    return null;
                }
                TileEntity tile = world.getTileEntity(element.blockPos);
                if (tile instanceof IPipeHolder) {
                    IPipeHolder holder = (IPipeHolder) tile;
                    PipePluggable plug = holder.getPluggable(element.emitterSide);
                    if(plug instanceof IWireEmitter) {
                        emittersCache.put(element, (IWireEmitter) plug);
                    }
                }
                if (!emittersCache.containsKey(element)) {
                    throw new IllegalStateException("Tried to get a wire element when none existed! THIS IS A BUG " + element);
                }
            }
            return emittersCache.get(element);
        }
        return null;
    }

    public boolean isEmitterEmitting(WireSystem.WireElement element, EnumDyeColor color) {
        if (!world.isBlockLoaded(element.blockPos)) {
            // Almura: don't load the emitter's chunk just to ask (it used to, logging "Ghost loading ..."). A gate in an unloaded chunk
            // can't change what it emits, so its last known state holds; one never seen counts as off until its chunk loads and the gate
            // ticks, which marks the gates changed if it emits anything.
            IWireEmitter cached = emittersCache.get(element);
            return cached != null && cached.isEmitting(color);
        }
        TileEntity tile = world.getTileEntity(element.blockPos);
        if(tile instanceof IPipeHolder) {
            IPipeHolder holder = (IPipeHolder) tile;
            if (holder.getPluggable(element.emitterSide) instanceof IWireEmitter) {
                return getEmitter(element).isEmitting(color);
            }
        }
        return false;
    }

    public void tick() {
        if(gatesChanged) {
            wireSystems.keySet().stream()
                    .filter(wireSystem -> {
                        boolean newPowered = wireSystem.update(this);
                        return wireSystems.put(wireSystem, newPowered) != newPowered;
                    })
                    .forEach(changedSystems::add);
            // Almura: never cleared before, so after the first gate change every wire system was updated every tick, checking (and so
            // loading) every emitter's chunk. Gates set it again whenever what they emit changes.
            gatesChanged = false;
        }
        world.getPlayers(EntityPlayerMP.class, Predicates.alwaysTrue()).forEach(player -> {
            Map<Integer, WireSystem> changedWires = this.wireSystems.keySet().stream()
                    .filter(wireSystem -> wireSystem.isPlayerWatching(player) && (structureChanged || changedPlayers.contains(player)))
                    .collect(Collectors.toMap(WireSystem::getWiresHashCode, Function.identity()));
            if(!changedWires.isEmpty()) {
                MessageManager.sendTo(new MessageWireSystems(changedWires), player);
            }
            Map<Integer, Boolean> hashesPowered = this.wireSystems.entrySet().stream()
                    .filter(systemPower ->
                            systemPower.getKey().isPlayerWatching(player) &&
                                    (structureChanged || changedSystems.contains(systemPower.getKey()) || changedPlayers.contains(player))
                    )
                    .map(systemPowered -> Pair.of(systemPowered.getKey().getWiresHashCode(), systemPowered.getValue()))
                    .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
            if(!hashesPowered.isEmpty()) {
                MessageManager.sendTo(new MessageWireSystemsPowered(hashesPowered), player);
            }
        });
        if(structureChanged || !changedSystems.isEmpty()) {
            markDirty();
        }
        structureChanged = false;
        changedSystems.clear();
        changedPlayers.clear();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        NBTTagList entriesList = new NBTTagList();
        wireSystems.forEach((wireSystem, powered) -> {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setTag("wireSystem", wireSystem.writeToNBT());
            entry.setBoolean("powered", powered);
            entriesList.appendTag(entry);
        });
        nbt.setTag("entries", entriesList);
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        wireSystems.clear();
        NBTTagList entriesList = nbt.getTagList("entries", Constants.NBT.TAG_COMPOUND);
        for(int i = 0; i < entriesList.tagCount(); i++) {
            NBTTagCompound entry = entriesList.getCompoundTagAt(i);
            wireSystems.put(new WireSystem().readFromNBT(entry.getCompoundTag("wireSystem")), entry.getBoolean("powered"));
        }
    }

    public static WorldSavedDataWireSystems get(World world) {
        if(world.isRemote) {
            throw new UnsupportedOperationException("Attempted to get WorldSavedDataWireSystems on the client!");
        }
        MapStorage storage = world.getPerWorldStorage();
        WorldSavedDataWireSystems instance = (WorldSavedDataWireSystems) storage.getOrLoadData(WorldSavedDataWireSystems.class, DATA_NAME);
        if (instance == null) {
            instance = new WorldSavedDataWireSystems();
            storage.setData(DATA_NAME, instance);
        }
        instance.world = world;
        return instance;
    }
}
