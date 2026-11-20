/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core.list;

import buildcraft.lib.misc.NBTUtilBC;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.Identifier;

import buildcraft.api.lists.ListMatchHandler;

import buildcraft.lib.gui.GuiBC8;
import buildcraft.lib.gui.GuiIcon;
import buildcraft.lib.gui.IGuiElement;
import buildcraft.lib.gui.button.GuiImageButton;
import buildcraft.lib.gui.button.IButtonBehaviour;
import buildcraft.lib.gui.button.IButtonClickEventListener;
import buildcraft.lib.gui.button.IButtonClickEventTrigger;
import buildcraft.lib.gui.elem.ToolTip;
import buildcraft.lib.gui.pos.GuiRectangle;
import buildcraft.lib.gui.pos.IGuiArea;
import buildcraft.lib.list.ListHandler;
import buildcraft.lib.misc.StackUtil;

import buildcraft.core.BCCoreItems;
import buildcraft.core.item.ItemList_BC8;
import buildcraft.core.list.ContainerList.WidgetListSlot;

public class GuiList extends GuiBC8<ContainerList> implements IButtonClickEventListener {
    private static final Identifier TEXTURE_BASE =
        Identifier.parse("buildcraftcore:textures/gui/list_new.png");
    private static final int SIZE_X = 176, SIZE_Y = 191;
    private static final GuiIcon ICON_GUI = new GuiIcon(TEXTURE_BASE, 0, 0, SIZE_X, SIZE_Y);
    private static final GuiIcon ICON_HIGHLIGHT = new GuiIcon(TEXTURE_BASE, 176, 0, 16, 16);
    private static final GuiIcon ICON_ONE_STACK = new GuiIcon(TEXTURE_BASE, 0, 191, 20, 20);
    private static final int BUTTON_COUNT = 3;

    private final Map<Integer, Map<ListMatchHandler.Type, NonNullList<ItemStack>>> exampleCache = new HashMap<>();
    private EditBox textField;

    public GuiList(ContainerList container) {
        super(container);
        xSize = SIZE_X;
        ySize = SIZE_Y;
    }

    @Override
    protected void init() {
        super.init();
        mainGui.shownElements.removeIf(e -> e instanceof GuiImageButton || e instanceof WidgetListSlot.GuiElementPhantomSlot);

        for (int line = 0; line < container.slots.length; line++) {
            WidgetListSlot[] arr = container.slots[line];
            for (int slot = 0; slot < arr.length; slot++) {
                final WidgetListSlot listSlot = arr[slot];
                GuiRectangle rectangle = new GuiRectangle(8 + slot * 18, 32 + line * 34, 16, 16);

                IGuiArea phantomSlotArea = rectangle.offset(mainGui.rootElement);
                mainGui.shownElements.add(listSlot.new GuiElementPhantomSlot(mainGui, phantomSlotArea) {
                    @Override
                    protected boolean shouldDrawHighlight() {
                        if (listSlot.slotIndex == 0) {
                            return true;
                        }
                        return !GuiList.this.container.lines[listSlot.lineIndex].isOneStackMode();
                    }

                    @Override
                    public void drawBackground(float partialTicks) {
                        if (!shouldDrawHighlight()) {
                            ICON_HIGHLIGHT.drawAt(this);
                        }
                    }

                    @Nonnull
                    @Override
                    public ItemStack getStack() {
                        if (shouldDrawHighlight()) {
                            return super.getStack();
                        } else {
                            NonNullList<ItemStack> data = GuiList.this.getExamplesList(listSlot.lineIndex,
                                container.lines[listSlot.lineIndex].getSortingType());
                            if (data.size() >= listSlot.slotIndex) {
                                return data.get(listSlot.slotIndex - 1);
                            } else {
                                return StackUtil.EMPTY;
                            }
                        }
                    }

                    @Override
                    public void onMouseClicked(int button) {
                        super.onMouseClicked(button);
                        if (contains(gui.mouse)) {
                            clearExamplesCache(listSlot.lineIndex);
                        }
                    }
                });
            }
        }

        for (int sy = 0; sy < ListHandler.HEIGHT; sy++) {
            int bOff = sy * BUTTON_COUNT;
            int bOffX = this.guiLeft + 8 + ListHandler.WIDTH * 18 - BUTTON_COUNT * 11;
            int bOffY = this.guiTop + 32 + sy * 34 + 18;

            GuiImageButton buttonPrecise =
                new GuiImageButton(mainGui, bOff + 0, bOffX, bOffY, 11, TEXTURE_BASE, 176, 16, 176, 28);
            buttonPrecise.setToolTip(ToolTip.createLocalized("gui.list.nbt"));
            buttonPrecise.setBehaviour(IButtonBehaviour.TOGGLE);
            mainGui.shownElements.add(buttonPrecise);

            GuiImageButton buttonType =
                new GuiImageButton(mainGui, bOff + 1, bOffX + 11, bOffY, 11, TEXTURE_BASE, 176, 16, 185, 28);
            buttonType.setToolTip(ToolTip.createLocalized("gui.list.metadata"));
            buttonType.setBehaviour(IButtonBehaviour.TOGGLE);
            mainGui.shownElements.add(buttonType);

            GuiImageButton buttonMaterial =
                new GuiImageButton(mainGui, bOff + 2, bOffX + 22, bOffY, 11, TEXTURE_BASE, 176, 16, 194, 28);
            buttonMaterial.setToolTip(ToolTip.createLocalized("gui.list.oredict"));
            buttonMaterial.setBehaviour(IButtonBehaviour.TOGGLE);
            mainGui.shownElements.add(buttonMaterial);
        }

        for (IGuiElement elem : mainGui.shownElements) {
            if (elem instanceof GuiImageButton) {
                GuiImageButton b = (GuiImageButton) elem;
                int id = Integer.parseInt(b.id);
                int lineId = id / BUTTON_COUNT;
                int buttonId = id % BUTTON_COUNT;
                if (container.lines[lineId].getOption(buttonId)) {
                    b.activate();
                }

                b.registerListener(this);
            }
        }

        textField = new EditBox(this.fontRenderer, guiLeft + 10, guiTop + 10, 156, 12, Component.empty());
        textField.setMaxLength(32);
        textField.setValue(BCCoreItems.list.getLabel(container.getListItemStack()));
        textField.setResponder(container::setLabel);
        textField.setFocused(false);
        addRenderableWidget(textField);
    }

    @Override
    protected void drawBackgroundLayer(float partialTicks) {
        ICON_GUI.drawAt(mainGui.rootElement);

        for (int i = 0; i < 2; i++) {
            if (container.lines[i].isOneStackMode()) {
                ICON_ONE_STACK.drawAt(guiLeft + 6, guiTop + 30 + i * 34);
            }
        }
    }

    private boolean isCarryingNonEmptyList() {
        ItemStack stack = container.getCarried();
        return !stack.isEmpty() && stack.getItem() instanceof ItemList_BC8 && NBTUtilBC.getItemData(stack) != null;
    }

    private boolean hasListEquipped() {
        return !container.getListItemStack().isEmpty();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (textField.isFocused() && event.key() != InputConstants.KEY_ESCAPE) {
            textField.keyPressed(event);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (textField.isFocused()) {
            return textField.charTyped(event);
        }
        return super.charTyped(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        boolean handled = super.mouseClicked(event, doubleClick);
        if (isCarryingNonEmptyList() || !hasListEquipped()) {
            textField.setFocused(false);
            return handled;
        }
        boolean inField = textField.isMouseOver(event.x(), event.y());
        textField.setFocused(inField);
        return handled || inField;
    }

    @Override
    public void handleButtonClick(IButtonClickEventTrigger sender, int buttonKey) {
        if (!(sender instanceof GuiImageButton)) {
            return;
        }
        int id = Integer.parseInt(((GuiImageButton) sender).id);
        int buttonId = id % BUTTON_COUNT;
        int lineId = id / BUTTON_COUNT;

        container.switchButton(lineId, buttonId);
        clearExamplesCache(lineId);
    }

    private void clearExamplesCache(int lineId) {
        Map<ListMatchHandler.Type, NonNullList<ItemStack>> exampleList = exampleCache.get(lineId);
        if (exampleList != null) {
            exampleList.clear();
        }
    }

    private NonNullList<ItemStack> getExamplesList(int lineId, ListMatchHandler.Type type) {
        Map<ListMatchHandler.Type, NonNullList<ItemStack>> exampleList =
            exampleCache.computeIfAbsent(lineId, k -> new EnumMap<>(ListMatchHandler.Type.class));

        if (!exampleList.containsKey(type)) {
            NonNullList<ItemStack> examples = container.lines[lineId].getExamples();
            ItemStack input = container.lines[lineId].stacks.get(0);
            if (!input.isEmpty()) {
                NonNullList<ItemStack> repetitions = NonNullList.create();
                for (ItemStack is : examples) {
                    if (StackUtil.isMatchingItem(input, is, true, false)) {
                        repetitions.add(is);
                    }
                }
                examples.removeAll(repetitions);
            }
            exampleList.put(type, examples);
        }
        return exampleList.get(type);
    }
}
