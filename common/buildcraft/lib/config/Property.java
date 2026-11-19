/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib.config;

import java.util.Arrays;

import javax.annotation.Nullable;

/** One value of a {@link Configuration}: a string, int, double or boolean, or a list of strings. Values are kept as strings
 * (as in the file) and parsed when read, like Forge 1.12's Property, which BuildCraft's configs were written for. */
public class Property {
    public enum Type {
        STRING('S'),
        INTEGER('I'),
        BOOLEAN('B'),
        DOUBLE('D');

        public final char id;

        Type(char id) {
            this.id = id;
        }

        @Nullable
        public static Type fromId(char id) {
            for (Type type : values()) {
                if (type.id == id) {
                    return type;
                }
            }
            return null;
        }
    }

    private final String name;
    private Type type;
    private final boolean isList;
    private String value;
    private String[] values;
    private String defaultValue;
    private String[] defaultValues;
    private String comment = "";
    private String langKey;
    private String minValue;
    private String maxValue;
    private String[] validValues;
    private boolean requiresMcRestart;
    private boolean requiresWorldRestart;
    private boolean showInGui = true;
    private boolean changed;
    boolean wasRead;

    public Property(String name, String value, Type type) {
        this.name = name;
        this.type = type;
        this.isList = false;
        this.value = value;
        this.defaultValue = value;
        this.langKey = name;
    }

    public Property(String name, String[] values, Type type) {
        this.name = name;
        this.type = type;
        this.isList = true;
        this.values = values.clone();
        this.defaultValues = values.clone();
        this.value = "";
        this.defaultValue = "";
        this.langKey = name;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    void setType(Type type) {
        this.type = type;
    }

    public boolean isList() {
        return isList;
    }

    public boolean hasChanged() {
        return changed;
    }

    void resetChangedState() {
        changed = false;
    }

    public boolean wasRead() {
        return wasRead;
    }

    // Comments, limits and display info

    public String getComment() {
        return comment;
    }

    public Property setComment(String comment) {
        this.comment = comment == null ? "" : comment;
        return this;
    }

    public String getLanguageKey() {
        return langKey;
    }

    public Property setLanguageKey(String langKey) {
        this.langKey = langKey;
        return this;
    }

    public Property setMinValue(int min) {
        this.minValue = Integer.toString(min);
        return this;
    }

    public Property setMaxValue(int max) {
        this.maxValue = Integer.toString(max);
        return this;
    }

    public Property setMinValue(double min) {
        this.minValue = Double.toString(min);
        return this;
    }

    public Property setMaxValue(double max) {
        this.maxValue = Double.toString(max);
        return this;
    }

    public String getMinValue() {
        return minValue;
    }

    public String getMaxValue() {
        return maxValue;
    }

    public Property setValidValues(String[] validValues) {
        this.validValues = validValues;
        return this;
    }

    public String[] getValidValues() {
        return validValues;
    }

    public Property setRequiresMcRestart(boolean requiresMcRestart) {
        this.requiresMcRestart = requiresMcRestart;
        return this;
    }

    public boolean requiresMcRestart() {
        return requiresMcRestart;
    }

    public Property setRequiresWorldRestart(boolean requiresWorldRestart) {
        this.requiresWorldRestart = requiresWorldRestart;
        return this;
    }

    public boolean requiresWorldRestart() {
        return requiresWorldRestart;
    }

    public Property setShowInGui(boolean showInGui) {
        this.showInGui = showInGui;
        return this;
    }

    public boolean showInGui() {
        return showInGui;
    }

    // Defaults

    public String getDefault() {
        return defaultValue;
    }

    public String[] getDefaults() {
        return defaultValues == null ? new String[0] : defaultValues.clone();
    }

    public Property setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public Property setDefaultValue(int defaultValue) {
        return setDefaultValue(Integer.toString(defaultValue));
    }

    public Property setDefaultValue(double defaultValue) {
        return setDefaultValue(Double.toString(defaultValue));
    }

    public Property setDefaultValue(boolean defaultValue) {
        return setDefaultValue(Boolean.toString(defaultValue));
    }

    public Property setDefaultValues(String[] defaultValues) {
        this.defaultValues = defaultValues.clone();
        return this;
    }

    // Getters

    public String getString() {
        return value;
    }

    public String[] getStringList() {
        return values == null ? new String[0] : values.clone();
    }

    public int getInt() {
        return getInt(parseIntOr(defaultValue, 0));
    }

    public int getInt(int fallback) {
        return parseIntOr(value, fallback);
    }

    public long getLong() {
        return getLong(parseLongOr(defaultValue, 0));
    }

    public long getLong(long fallback) {
        return parseLongOr(value, fallback);
    }

    public double getDouble() {
        return getDouble(parseDoubleOr(defaultValue, 0));
    }

    public double getDouble(double fallback) {
        return parseDoubleOr(value, fallback);
    }

    public boolean getBoolean() {
        return getBoolean(Boolean.parseBoolean(defaultValue));
    }

    public boolean getBoolean(boolean fallback) {
        if ("true".equalsIgnoreCase(value)) {
            return true;
        } else if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        return fallback;
    }

    public boolean isIntValue() {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public boolean isDoubleValue() {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public boolean isBooleanValue() {
        return "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
    }

    private static int parseIntOr(String s, int fallback) {
        try {
            return Integer.parseInt(s.trim());
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private static long parseLongOr(String s, long fallback) {
        try {
            return Long.parseLong(s.trim());
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private static double parseDoubleOr(String s, double fallback) {
        try {
            return Double.parseDouble(s.trim());
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    // Setters

    public Property setValue(String value) {
        if (!value.equals(this.value)) {
            changed = true;
        }
        this.value = value;
        return this;
    }

    public Property setValue(int value) {
        return setValue(Integer.toString(value));
    }

    public Property setValue(long value) {
        return setValue(Long.toString(value));
    }

    public Property setValue(double value) {
        return setValue(Double.toString(value));
    }

    public Property setValue(boolean value) {
        return setValue(Boolean.toString(value));
    }

    public Property setValues(String[] values) {
        if (!Arrays.equals(values, this.values)) {
            changed = true;
        }
        this.values = values.clone();
        return this;
    }

    public void set(String value) {
        setValue(value);
    }

    public void set(int value) {
        setValue(value);
    }

    public void set(long value) {
        setValue(value);
    }

    public void set(double value) {
        setValue(value);
    }

    public void set(boolean value) {
        setValue(value);
    }

    public void set(String[] values) {
        setValues(values);
    }

    /** Sets the value as read from the file (not marking it changed). */
    void read(String value) {
        this.value = value;
        this.wasRead = true;
    }

    void readList(String[] values) {
        this.values = values;
        this.wasRead = true;
    }

    @Override
    public String toString() {
        return isList ? Arrays.toString(values) : value;
    }
}
