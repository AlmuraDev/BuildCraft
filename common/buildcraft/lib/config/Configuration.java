/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import buildcraft.api.core.BCLog;

/** A config file in the format of Forge 1.12's Configuration (categories in braces, "T:name=value" lines, lists in angle
 * brackets), so BuildCraft's 1.12.2 config files keep working. Only what BuildCraft uses is implemented. */
public class Configuration {
    public static final String CATEGORY_GENERAL = "general";
    public static final String CATEGORY_SPLITTER = ".";
    private static final String NEW_LINE = System.lineSeparator();
    private static final Pattern NEEDS_QUOTES = Pattern.compile(".*[^A-Za-z0-9._\\-].*");

    private final File file;
    private final Map<String, ConfigCategory> categories = new TreeMap<>();
    private boolean changed = false;

    public Configuration(File file) {
        this.file = file;
        load();
    }

    public File getConfigFile() {
        return file;
    }

    // #######################
    //
    // Categories
    //
    // #######################

    public ConfigCategory getCategory(String category) {
        ConfigCategory existing = categories.get(category);
        if (existing != null) {
            return existing;
        }
        ConfigCategory parent = null;
        String name = category;
        int split = category.lastIndexOf(CATEGORY_SPLITTER);
        if (split > 0) {
            parent = getCategory(category.substring(0, split));
            name = category.substring(split + 1);
        }
        ConfigCategory created = new ConfigCategory(name, parent);
        categories.put(category, created);
        changed = true;
        return created;
    }

    public boolean hasCategory(String category) {
        return categories.containsKey(category);
    }

    public Set<String> getCategoryNames() {
        return categories.keySet();
    }

    public void removeCategory(ConfigCategory category) {
        for (ConfigCategory child : new ArrayList<>(category.getChildren())) {
            removeCategory(child);
        }
        if (categories.remove(category.getQualifiedName()) != null) {
            if (category.getParent() != null) {
                category.getParent().getChildren().remove(category);
            }
            changed = true;
        }
    }

    public Configuration setCategoryComment(String category, String comment) {
        getCategory(category).setComment(comment);
        return this;
    }

    public Configuration setCategoryRequiresMcRestart(String category, boolean requires) {
        getCategory(category).setRequiresMcRestart(requires);
        return this;
    }

    public Configuration setCategoryRequiresWorldRestart(String category, boolean requires) {
        getCategory(category).setRequiresWorldRestart(requires);
        return this;
    }

    public Configuration setCategoryLanguageKey(String category, String langKey) {
        getCategory(category).setLanguageKey(langKey);
        return this;
    }

    public boolean hasKey(String category, String key) {
        ConfigCategory cat = categories.get(category);
        return cat != null && cat.containsKey(key);
    }

    public boolean renameProperty(String category, String oldName, String newName) {
        ConfigCategory cat = categories.get(category);
        if (cat == null || !cat.containsKey(oldName) || oldName.equals(newName)) {
            return false;
        }
        Property old = cat.remove(oldName);
        Property renamed = copyAs(old, newName);
        cat.put(newName, renamed);
        changed = true;
        return true;
    }

    public boolean moveProperty(String oldCategory, String name, String newCategory) {
        ConfigCategory from = categories.get(oldCategory);
        if (from == null || !from.containsKey(name) || oldCategory.equals(newCategory)) {
            return false;
        }
        ConfigCategory to = getCategory(newCategory);
        to.put(name, from.remove(name));
        changed = true;
        return true;
    }

    private static Property copyAs(Property old, String name) {
        Property prop = old.isList() ? new Property(name, old.getStringList(), old.getType())
            : new Property(name, old.getString(), old.getType());
        prop.setComment(old.getComment());
        prop.wasRead = old.wasRead();
        return prop;
    }

    // #######################
    //
    // Getting properties
    //
    // #######################

    public Property get(String category, String key, String defaultValue) {
        return get(category, key, defaultValue, null, Property.Type.STRING);
    }

    public Property get(String category, String key, String defaultValue, @Nullable String comment) {
        return get(category, key, defaultValue, comment, Property.Type.STRING);
    }

    public Property get(String category, String key, String defaultValue, @Nullable String comment, String[] validValues) {
        Property prop = get(category, key, defaultValue, comment, Property.Type.STRING);
        prop.setValidValues(validValues);
        return prop;
    }

    public Property get(String category, String key, boolean defaultValue) {
        return get(category, key, defaultValue, null);
    }

    public Property get(String category, String key, boolean defaultValue, @Nullable String comment) {
        Property prop = get(category, key, Boolean.toString(defaultValue), comment, Property.Type.BOOLEAN);
        if (!prop.isBooleanValue()) {
            prop.setValue(defaultValue);
        }
        return prop;
    }

    public Property get(String category, String key, int defaultValue) {
        return get(category, key, defaultValue, null, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    public Property get(String category, String key, int defaultValue, @Nullable String comment) {
        return get(category, key, defaultValue, comment, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    public Property get(String category, String key, int defaultValue, @Nullable String comment, int min, int max) {
        Property prop = get(category, key, Integer.toString(defaultValue), comment, Property.Type.INTEGER);
        prop.setDefaultValue(defaultValue);
        prop.setMinValue(min);
        prop.setMaxValue(max);
        if (!prop.isIntValue()) {
            prop.setValue(defaultValue);
        }
        return prop;
    }

    public Property get(String category, String key, double defaultValue) {
        return get(category, key, defaultValue, null, -Double.MAX_VALUE, Double.MAX_VALUE);
    }

    public Property get(String category, String key, double defaultValue, @Nullable String comment) {
        return get(category, key, defaultValue, comment, -Double.MAX_VALUE, Double.MAX_VALUE);
    }

    public Property get(String category, String key, double defaultValue, @Nullable String comment, double min, double max) {
        Property prop = get(category, key, Double.toString(defaultValue), comment, Property.Type.DOUBLE);
        prop.setDefaultValue(defaultValue);
        prop.setMinValue(min);
        prop.setMaxValue(max);
        if (!prop.isDoubleValue()) {
            prop.setValue(defaultValue);
        }
        return prop;
    }

    public Property get(String category, String key, String[] defaultValues) {
        return get(category, key, defaultValues, null);
    }

    public Property get(String category, String key, String[] defaultValues, @Nullable String comment) {
        ConfigCategory cat = getCategory(category);
        Property prop = cat.get(key);
        if (prop == null || !prop.isList()) {
            Property created = new Property(key, defaultValues, Property.Type.STRING);
            if (prop != null) {
                created.readList(new String[] { prop.getString() });
            }
            prop = created;
            cat.put(key, prop);
            changed = true;
        }
        prop.setDefaultValues(defaultValues);
        if (comment != null) {
            prop.setComment(comment);
        }
        return prop;
    }

    private Property get(String category, String key, String defaultValue, @Nullable String comment, Property.Type type) {
        ConfigCategory cat = getCategory(category);
        Property prop = cat.get(key);
        if (prop == null || prop.isList()) {
            prop = new Property(key, defaultValue, type);
            cat.put(key, prop);
            changed = true;
        } else {
            prop.setType(type);
        }
        prop.setDefaultValue(defaultValue);
        if (comment != null) {
            prop.setComment(comment);
        }
        return prop;
    }

    // #######################
    //
    // Loading and saving
    //
    // #######################

    public boolean hasChanged() {
        if (changed) {
            return true;
        }
        for (ConfigCategory cat : categories.values()) {
            if (cat.hasChanged()) {
                return true;
            }
        }
        return false;
    }

    public void load() {
        if (!file.exists()) {
            return;
        }
        Deque<String> stack = new ArrayDeque<>();
        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            String line;
            Property list = null;
            List<String> listValues = null;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String trimmed = line.trim();
                if (list != null) {
                    if (trimmed.equals(">")) {
                        list.readList(listValues.toArray(new String[0]));
                        list = null;
                        listValues = null;
                    } else if (!trimmed.isEmpty()) {
                        listValues.add(trimmed);
                    }
                    continue;
                }
                if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("~")) {
                    continue;
                }
                if (trimmed.endsWith("{")) {
                    String name = unquote(trimmed.substring(0, trimmed.length() - 1).trim());
                    String qualified = stack.isEmpty() ? name : stack.peek() + CATEGORY_SPLITTER + name;
                    getCategory(qualified);
                    stack.push(qualified);
                    continue;
                }
                if (trimmed.equals("}")) {
                    if (!stack.isEmpty()) {
                        stack.pop();
                    }
                    continue;
                }
                if (stack.isEmpty() || trimmed.length() < 3 || trimmed.charAt(1) != ':') {
                    BCLog.logger.warn("[lib.config] Ignoring line " + lineNumber + " of " + file + ": " + trimmed);
                    continue;
                }
                Property.Type type = Property.Type.fromId(trimmed.charAt(0));
                if (type == null) {
                    type = Property.Type.STRING;
                }
                String rest = trimmed.substring(2);
                ConfigCategory cat = getCategory(stack.peek());
                if (rest.endsWith("<")) {
                    String name = unquote(rest.substring(0, rest.length() - 1).trim());
                    list = new Property(name, new String[0], type);
                    listValues = new ArrayList<>();
                    cat.put(name, list);
                    continue;
                }
                int eq = findEquals(rest);
                if (eq < 0) {
                    BCLog.logger.warn("[lib.config] Ignoring line " + lineNumber + " of " + file + ": " + trimmed);
                    continue;
                }
                String name = unquote(rest.substring(0, eq).trim());
                Property prop = new Property(name, "", type);
                prop.read(rest.substring(eq + 1));
                cat.put(name, prop);
            }
        } catch (IOException e) {
            BCLog.logger.warn("[lib.config] Couldn't read " + file, e);
        }
        for (ConfigCategory cat : categories.values()) {
            cat.resetChangedState();
        }
        changed = false;
    }

    private static int findEquals(String rest) {
        boolean quoted = false;
        for (int i = 0; i < rest.length(); i++) {
            char c = rest.charAt(i);
            if (c == '"') {
                quoted = !quoted;
            } else if (c == '=' && !quoted) {
                return i;
            }
        }
        return -1;
    }

    private static String unquote(String name) {
        if (name.length() >= 2 && name.startsWith("\"") && name.endsWith("\"")) {
            return name.substring(1, name.length() - 1);
        }
        return name;
    }

    private static String quote(String name) {
        return NEEDS_QUOTES.matcher(name).matches() ? "\"" + name + "\"" : name;
    }

    public void save() {
        try {
            File parent = file.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            try (BufferedWriter out = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
                out.write("# Configuration file" + NEW_LINE + NEW_LINE);
                for (ConfigCategory cat : categories.values()) {
                    if (!cat.isChild()) {
                        writeCategory(out, cat, 0);
                        out.write(NEW_LINE);
                    }
                }
            }
            for (ConfigCategory cat : categories.values()) {
                cat.resetChangedState();
            }
            changed = false;
        } catch (IOException e) {
            BCLog.logger.warn("[lib.config] Couldn't save " + file, e);
        }
    }

    private static String indent(int depth) {
        return "    ".repeat(depth);
    }

    private void writeCategory(BufferedWriter out, ConfigCategory cat, int depth) throws IOException {
        String pad = indent(depth);
        if (!cat.getComment().isEmpty()) {
            out.write(pad + "####################" + NEW_LINE);
            out.write(pad + "# " + cat.getName() + NEW_LINE);
            out.write(pad + "#===================" + NEW_LINE);
            for (String line : cat.getComment().split("\r?\n")) {
                out.write(pad + "# " + line + NEW_LINE);
            }
            out.write(pad + "####################" + NEW_LINE + NEW_LINE);
        }
        out.write(pad + quote(cat.getName()) + " {" + NEW_LINE);
        String inner = indent(depth + 1);
        boolean first = true;
        for (Property prop : cat.getOrderedValues()) {
            if (!first) {
                out.write(NEW_LINE);
            }
            first = false;
            String comment = describe(prop);
            if (!comment.isEmpty()) {
                for (String line : comment.split("\r?\n")) {
                    out.write(inner + "# " + line + NEW_LINE);
                }
            }
            String name = quote(prop.getName());
            if (prop.isList()) {
                out.write(inner + prop.getType().id + ":" + name + " <" + NEW_LINE);
                for (String value : prop.getStringList()) {
                    out.write(indent(depth + 2) + value + NEW_LINE);
                }
                out.write(inner + " >" + NEW_LINE);
            } else {
                out.write(inner + prop.getType().id + ":" + name + "=" + prop.getString() + NEW_LINE);
            }
        }
        for (ConfigCategory child : cat.getChildren()) {
            out.write(NEW_LINE);
            writeCategory(out, child, depth + 1);
        }
        out.write(pad + "}" + NEW_LINE + NEW_LINE);
    }

    private static String describe(Property prop) {
        StringBuilder sb = new StringBuilder(prop.getComment());
        String min = prop.getMinValue();
        String max = prop.getMaxValue();
        boolean hasRange = min != null && max != null && !isUnbounded(min) && !isUnbounded(max);
        if (hasRange) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append("[range: ").append(min).append(" ~ ").append(max).append(", default: ").append(prop.getDefault()).append(']');
        }
        String[] valid = prop.getValidValues();
        if (valid != null && valid.length > 0) {
            sb.append(NEW_LINE).append("[valid: ").append(String.join(", ", valid)).append(']');
        }
        return sb.toString();
    }

    private static boolean isUnbounded(String value) {
        return value.equals(Integer.toString(Integer.MIN_VALUE)) || value.equals(Integer.toString(Integer.MAX_VALUE))
            || value.equals(Double.toString(Double.MAX_VALUE)) || value.equals(Double.toString(-Double.MAX_VALUE));
    }
}
