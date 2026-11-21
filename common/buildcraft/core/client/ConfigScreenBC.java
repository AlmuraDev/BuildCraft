/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import buildcraft.core.BCCore;
import buildcraft.core.BCCoreConfig;
import buildcraft.lib.config.ConfigCategory;
import buildcraft.lib.config.Configuration;
import buildcraft.lib.config.Property;

/** The in-game config screen (1.12.2 used Forge's GuiConfig over {@link BCCoreConfig#config} and
 * {@link BCCoreConfig#objConfig}). The root screen lists the top level categories; each category screen edits its
 * values and opens its child categories. Changes are kept until Done, then saved and handed to the reload listeners. */
public final class ConfigScreenBC {
    private ConfigScreenBC() {}

    public static void register() {
        ModList.get().getModContainerById(BCCore.MODID).ifPresent(container -> container
            .registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> new RootScreen(parent)));
    }

    static Component label(String langKey, String fallback) {
        if (langKey != null && Language.getInstance().has(langKey)) {
            return Component.translatable(langKey);
        }
        return Component.literal(fallback);
    }

    static Component categoryLabel(ConfigCategory cat) {
        String name = cat.getName();
        return label(cat.getLanguagekey() != null ? cat.getLanguagekey() : "config." + cat.getQualifiedName(), name);
    }

    static boolean isTopLevel(String name) {
        return !name.contains(Configuration.CATEGORY_SPLITTER);
    }

    /** Lists the top level categories of both config files. */
    static class RootScreen extends Screen {
        private final Screen parent;
        private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
        private boolean edited = false;

        RootScreen(Screen parent) {
            super(label("config.buildcraft", "BuildCraft Configuration"));
            this.parent = parent;
        }

        @Override
        protected void init() {
            layout.addTitleHeader(title, font);
            OptionList list = layout.addToContents(new OptionList(minecraft, width, layout));
            addCategories(list, BCCoreConfig.config, false);
            addCategories(list, BCCoreConfig.objConfig, true);
            layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).width(200).build());
            layout.visitWidgets(this::addRenderableWidget);
            repositionElements();
        }

        private void addCategories(OptionList list, Configuration cfg, boolean objects) {
            for (String name : cfg.getCategoryNames()) {
                if (!isTopLevel(name)) continue;
                ConfigCategory cat = cfg.getCategory(name);
                if (!cat.showInGui()) continue;
                Component text = categoryLabel(cat);
                if (objects) {
                    // objects.cfg: enables and disables blocks and items (needs a game restart)
                    text = Component.literal("Objects: ").append(text);
                }
                list.addWide(Button.builder(text, b -> minecraft.gui.setScreen(new CategoryScreen(this, cat)))
                    .build());
            }
        }

        void markEdited() {
            edited = true;
        }

        @Override
        protected void repositionElements() {
            layout.arrangeElements();
            for (GuiEventListener child : children()) {
                if (child instanceof OptionList l) l.updateSize(width, layout);
            }
        }

        @Override
        public void onClose() {
            if (edited) {
                BCCoreConfig.saveConfigs();
                BCCoreConfig.onConfigChange(BCCore.MODID);
            }
            minecraft.gui.setScreen(parent);
        }
    }

    /** Edits the values of one category. */
    static class CategoryScreen extends Screen {
        private final Screen parent;
        private final RootScreen root;
        private final ConfigCategory category;
        private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
        /** Values typed or picked on this screen, applied on Done. Kept across re-inits (resizes). */
        private final Map<Property, Object> pending = new LinkedHashMap<>();
        private final List<EditBox> invalid = new ArrayList<>();
        private Button doneButton;

        CategoryScreen(Screen parent, ConfigCategory category) {
            super(categoryLabel(category));
            this.parent = parent;
            this.root = parent instanceof RootScreen r ? r : ((CategoryScreen) parent).root;
            this.category = category;
        }

        @Override
        protected void init() {
            invalid.clear();
            layout.addTitleHeader(title, font);
            OptionList list = layout.addToContents(new OptionList(minecraft, width, layout));

            for (ConfigCategory child : category.getChildren()) {
                if (!child.showInGui()) continue;
                list.addWide(Button.builder(categoryLabel(child), b -> {
                    apply();
                    minecraft.gui.setScreen(new CategoryScreen(this, child));
                }).build());
            }
            for (Property prop : category.getOrderedValues()) {
                if (!prop.showInGui()) continue;
                list.addRow(nameOf(prop), tooltipOf(prop), widgetFor(prop));
            }

            LinearLayout footer = layout.addToFooter(LinearLayout.horizontal().spacing(8));
            doneButton = footer.addChild(Button.builder(CommonComponents.GUI_DONE, b -> {
                apply();
                minecraft.gui.setScreen(parent);
            }).width(150).build());
            footer.addChild(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose()).width(150).build());
            layout.visitWidgets(this::addRenderableWidget);
            repositionElements();
            updateDone();
        }

        private Component nameOf(Property prop) {
            return label(prop.getLanguageKey(), prop.getName());
        }

        private Tooltip tooltipOf(Property prop) {
            MutableComponent text = Component.empty();
            String comment = prop.getComment();
            if (comment != null && !comment.isBlank()) {
                text.append(Component.literal(comment.trim())).append("\n");
            }
            text.append(Component.literal(prop.getName()).withStyle(ChatFormatting.GRAY));
            if (prop.isList()) {
                text.append(Component.literal("\nDefault: " + String.join(", ", prop.getDefaults()))
                    .withStyle(ChatFormatting.GRAY));
            } else {
                text.append(Component.literal("\nDefault: " + prop.getDefault()).withStyle(ChatFormatting.GRAY));
            }
            String min = prop.getMinValue(), max = prop.getMaxValue();
            if (prop.getType() == Property.Type.INTEGER || prop.getType() == Property.Type.DOUBLE) {
                text.append(Component.literal("\nRange: " + min + " .. " + max).withStyle(ChatFormatting.GRAY));
            }
            if (prop.requiresMcRestart()) {
                text.append(Component.literal("\nNeeds a game restart").withStyle(ChatFormatting.RED));
            } else if (prop.requiresWorldRestart()) {
                text.append(Component.literal("\nNeeds a world restart").withStyle(ChatFormatting.GOLD));
            }
            return Tooltip.create(text);
        }

        private AbstractWidget widgetFor(Property prop) {
            if (prop.isList()) {
                String[] current = pending.containsKey(prop) ? (String[]) pending.get(prop) : prop.getStringList();
                EditBox box = new EditBox(font, 150, 20, Component.literal(prop.getName()));
                box.setMaxLength(32767);
                box.setValue(String.join(", ", current));
                box.setResponder(s -> pending.put(prop, splitList(s)));
                return box;
            }
            if (prop.getType() == Property.Type.BOOLEAN) {
                boolean current = pending.containsKey(prop) ? (Boolean) pending.get(prop) : prop.getBoolean();
                return CycleButton.onOffBuilder(current).displayOnlyValue()
                    .create(0, 0, 150, 20, Component.literal(prop.getName()), (b, v) -> pending.put(prop, v));
            }
            String[] valid = prop.getValidValues();
            String current = pending.containsKey(prop) ? (String) pending.get(prop) : prop.getString();
            if (valid != null && valid.length > 0) {
                List<String> values = new ArrayList<>(Arrays.asList(valid));
                if (!values.contains(current)) values.add(0, current);
                return CycleButton.<String> builder(Component::literal, current).withValues(values).displayOnlyValue()
                    .create(0, 0, 150, 20, Component.literal(prop.getName()), (b, v) -> pending.put(prop, v));
            }
            EditBox box = new EditBox(font, 150, 20, Component.literal(prop.getName()));
            box.setMaxLength(32767);
            box.setValue(current);
            box.setResponder(s -> {
                if (isValid(prop, s)) {
                    invalid.remove(box);
                    box.setTextColor(0xFFE0E0E0);
                    pending.put(prop, s.trim());
                } else {
                    if (!invalid.contains(box)) invalid.add(box);
                    box.setTextColor(0xFFFF5555);
                    pending.remove(prop);
                }
                updateDone();
            });
            return box;
        }

        private static String[] splitList(String s) {
            return Arrays.stream(s.split(",")).map(String::trim).filter(v -> !v.isEmpty()).toArray(String[]::new);
        }

        private static boolean isValid(Property prop, String s) {
            String v = s.trim();
            try {
                switch (prop.getType()) {
                    case INTEGER: {
                        int i = Integer.parseInt(v);
                        return i >= Integer.parseInt(prop.getMinValue()) && i <= Integer.parseInt(prop.getMaxValue());
                    }
                    case DOUBLE: {
                        double d = Double.parseDouble(v);
                        return d >= Double.parseDouble(prop.getMinValue())
                            && d <= Double.parseDouble(prop.getMaxValue());
                    }
                    default:
                        return true;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }

        private void updateDone() {
            if (doneButton != null) doneButton.active = invalid.isEmpty();
        }

        private void apply() {
            if (!invalid.isEmpty()) return;
            for (Map.Entry<Property, Object> e : pending.entrySet()) {
                Property prop = e.getKey();
                Object v = e.getValue();
                if (v instanceof String[] list) {
                    if (!Arrays.equals(list, prop.getStringList())) prop.set(list);
                } else if (v instanceof Boolean b) {
                    if (b != prop.getBoolean()) prop.set(b);
                } else if (!v.equals(prop.getString())) {
                    prop.set((String) v);
                }
            }
            if (!pending.isEmpty()) root.markEdited();
            pending.clear();
        }

        @Override
        protected void repositionElements() {
            layout.arrangeElements();
            for (GuiEventListener child : children()) {
                if (child instanceof OptionList l) l.updateSize(width, layout);
            }
        }

        @Override
        public void onClose() {
            // Cancel / escape: drop this screen's changes
            pending.clear();
            minecraft.gui.setScreen(parent);
        }
    }

    /** A scrolling list of rows: a wide button, or a name on the left and a widget on the right. */
    static class OptionList extends ContainerObjectSelectionList<OptionList.Row> {
        OptionList(Minecraft mc, int width, HeaderAndFooterLayout layout) {
            super(mc, width, layout.getContentHeight(), layout.getHeaderHeight(), 24);
            this.centerListVertically = false;
        }

        void addWide(AbstractWidget widget) {
            widget.setWidth(310);
            addEntry(new Row(null, null, widget));
        }

        void addRow(Component name, Tooltip tooltip, AbstractWidget widget) {
            widget.setWidth(150);
            widget.setTooltip(tooltip);
            addEntry(new Row(name, tooltip, widget));
        }

        @Override
        public int getRowWidth() {
            return 310;
        }

        static class Row extends ContainerObjectSelectionList.Entry<Row> {
            private final Component name;
            private final AbstractWidget widget;

            Row(Component name, Tooltip tooltip, AbstractWidget widget) {
                this.name = name;
                this.widget = widget;
            }

            @Override
            public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
                Minecraft mc = Minecraft.getInstance();
                int x = getContentX();
                int y = getContentY();
                if (name == null) {
                    widget.setPosition(x, y);
                } else {
                    String text = mc.font.plainSubstrByWidth(name.getString(), 154);
                    graphics.text(mc.font, text, x, y + 6, 0xFFFFFFFF);
                    widget.setPosition(x + 160, y);
                }
                widget.extractRenderState(graphics, mouseX, mouseY, a);
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return List.of(widget);
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return List.of(widget);
            }
        }
    }
}
