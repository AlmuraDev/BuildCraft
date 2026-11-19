/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.pipe;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;

import net.minecraft.resources.Identifier;

import buildcraft.api.core.InvalidInputDataException;
import buildcraft.api.transport.pipe.IItemPipe;
import buildcraft.api.transport.pipe.IPipeRegistry;
import buildcraft.api.transport.pipe.PipeDefinition;

import buildcraft.lib.registry.RegistrationHelper;

import buildcraft.transport.BCTransportBlocks;
import buildcraft.transport.item.ItemPipeHolder;

public enum PipeRegistry implements IPipeRegistry {
    INSTANCE;

    private RegistrationHelper helper;
    private final Map<Identifier, PipeDefinition> definitions = new HashMap<>();
    private final Map<PipeDefinition, IItemPipe> pipeItems = new IdentityHashMap<>();

    /** Lets pipe items be registered on transport's mod bus. */
    public void init(RegistrationHelper transportHelper) {
        this.helper = transportHelper;
    }

    @Override
    public void registerPipe(PipeDefinition definition) {
        definitions.put(definition.identifier, definition);
    }

    @Override
    public void setItemForPipe(PipeDefinition definition, @Nullable IItemPipe item) {
        if (definition == null) {
            throw new NullPointerException("definition");
        }
        if (item == null) {
            pipeItems.remove(definition);
        } else {
            pipeItems.put(definition, item);
        }
    }

    @Override
    public void createItemForPipe(PipeDefinition definition, Consumer<IItemPipe> onCreated) {
        if (helper == null) {
            throw new IllegalStateException("Pipe items can only be made while transport is being constructed");
        }
        String tagId = ItemPipeHolder.tagIdFor(definition);
        helper.addItem(tagId, (String) null,
            (props, id) -> new ItemPipeHolder(BCTransportBlocks.pipeHolder, definition, props, id), item -> {
                if (definitions.values().contains(definition)) {
                    setItemForPipe(definition, item);
                }
                onCreated.accept(item);
            });
    }

    @Override
    public IItemPipe getItemForPipe(PipeDefinition definition) {
        return pipeItems.get(definition);
    }

    @Override
    @Nullable
    public PipeDefinition getDefinition(Identifier identifier) {
        return definitions.get(identifier);
    }

    @Nonnull
    public PipeDefinition loadDefinition(String identifier) throws InvalidInputDataException {
        PipeDefinition def = getDefinition(Identifier.parse(identifier));
        if (def == null) {
            throw new InvalidInputDataException("Unknown pipe definition " + identifier);
        }
        return def;
    }

    @Override
    public Iterable<PipeDefinition> getAllRegisteredPipes() {
        return ImmutableList.copyOf(definitions.values());
    }
}
