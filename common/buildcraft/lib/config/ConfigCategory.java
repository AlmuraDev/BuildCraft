/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

/** A named group of {@link Property properties} in a {@link Configuration}, possibly inside another category ("worldgen.oil"
 * is "oil" inside "worldgen"). */
public class ConfigCategory {
    private final String name;
    @Nullable
    private final ConfigCategory parent;
    private final List<ConfigCategory> children = new ArrayList<>();
    private final Map<String, Property> properties = new LinkedHashMap<>();
    private String comment = "";
    private String langKey;
    private boolean requiresMcRestart;
    private boolean requiresWorldRestart;
    private boolean showInGui = true;

    public ConfigCategory(String name, @Nullable ConfigCategory parent) {
        this.name = name;
        this.parent = parent;
        this.langKey = name;
        if (parent != null) {
            parent.children.add(this);
        }
    }

    public String getName() {
        return name;
    }

    public String getQualifiedName() {
        return parent == null ? name : parent.getQualifiedName() + Configuration.CATEGORY_SPLITTER + name;
    }

    @Nullable
    public ConfigCategory getParent() {
        return parent;
    }

    public boolean isChild() {
        return parent != null;
    }

    public List<ConfigCategory> getChildren() {
        return children;
    }

    public Map<String, Property> getValues() {
        return properties;
    }

    public Collection<Property> getOrderedValues() {
        return properties.values();
    }

    public Set<String> keySet() {
        return properties.keySet();
    }

    public boolean containsKey(String key) {
        return properties.containsKey(key);
    }

    public Property get(String key) {
        return properties.get(key);
    }

    public Property put(String key, Property value) {
        return properties.put(key, value);
    }

    public Property remove(String key) {
        return properties.remove(key);
    }

    public boolean isEmpty() {
        return properties.isEmpty();
    }

    public String getComment() {
        return comment;
    }

    public ConfigCategory setComment(String comment) {
        this.comment = comment == null ? "" : comment;
        return this;
    }

    public String getLanguagekey() {
        return langKey;
    }

    public ConfigCategory setLanguageKey(String langKey) {
        this.langKey = langKey;
        return this;
    }

    public ConfigCategory setRequiresMcRestart(boolean requiresMcRestart) {
        this.requiresMcRestart = requiresMcRestart;
        return this;
    }

    public boolean requiresMcRestart() {
        return requiresMcRestart;
    }

    public ConfigCategory setRequiresWorldRestart(boolean requiresWorldRestart) {
        this.requiresWorldRestart = requiresWorldRestart;
        return this;
    }

    public boolean requiresWorldRestart() {
        return requiresWorldRestart;
    }

    public ConfigCategory setShowInGui(boolean showInGui) {
        this.showInGui = showInGui;
        return this;
    }

    public boolean showInGui() {
        return showInGui;
    }

    public boolean hasChanged() {
        for (Property prop : properties.values()) {
            if (prop.hasChanged()) {
                return true;
            }
        }
        for (ConfigCategory child : children) {
            if (child.hasChanged()) {
                return true;
            }
        }
        return false;
    }

    void resetChangedState() {
        for (Property prop : properties.values()) {
            prop.resetChangedState();
        }
        for (ConfigCategory child : children) {
            child.resetChangedState();
        }
    }

    @Override
    public String toString() {
        return getQualifiedName();
    }
}
