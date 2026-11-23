/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.client.model;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.google.gson.JsonSyntaxException;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

public class ResourceLoaderContext {
    private final Set<Identifier> loaded = new HashSet<>();
    private final Deque<Identifier> loadingStack = new ArrayDeque<>();

    public InputStreamReader startLoading(Identifier location) throws IOException {
        if (!loaded.add(location)) {
            throw new JsonSyntaxException("Already loaded " + location + " from " + loadingStack.peek());
        }
        loadingStack.push(location);
        ResourceManager resources = Minecraft.getInstance().getResourceManager();
        // BuildCraft's variable models (expressions such as "4 + size" in place of numbers) are kept under variable_models/, which
        // 26.3's model manager doesn't read as ordinary block models (it would fail on every one): they are still asked for by their
        // models/ location, and read from variable_models/ when they are there.
        if (location.getPath().startsWith("models/")) {
            Identifier variable = Identifier.fromNamespaceAndPath(location.getNamespace(),
                "variable_models/" + location.getPath().substring("models/".length()));
            Optional<Resource> found = resources.getResource(variable);
            if (found.isPresent()) {
                return new InputStreamReader(found.get().open(), StandardCharsets.UTF_8);
            }
        }
        return new InputStreamReader(resources.getResourceOrThrow(location).open(), StandardCharsets.UTF_8);
    }

    public void finishLoading() {
        loadingStack.pop();
    }
}
