/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.recipes;

import net.minecraft.world.item.crafting.Ingredient;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public final class IngredientStack {
    public final Ingredient ingredient;
    public final int count;

    public IngredientStack(Ingredient ingredient, int count) {
        this.ingredient = ingredient;
        this.count = count;
    }

    public IngredientStack(Ingredient ingredient) {
        this(ingredient, 1);
    }

    /** @param o An {@link Ingredient}, {@link ItemStack}, {@link ItemLike}, item {@link TagKey}, or a string naming an item
     *            tag ("c:ingots/iron", or "#c:ingots/iron"). (1.12.2 took ore dictionary names here.) */
    @SuppressWarnings("unchecked")
    public static IngredientStack of(Object o) {
        return new IngredientStack(toIngredient(o));
    }

    @SuppressWarnings("unchecked")
    public static Ingredient toIngredient(Object o) {
        if (o instanceof Ingredient ingredient) {
            return ingredient;
        }
        if (o instanceof ItemStack stack) {
            return Ingredient.of(stack.getItem());
        }
        if (o instanceof ItemLike item) {
            return Ingredient.of(item);
        }
        if (o instanceof TagKey<?> tag) {
            return Ingredient.of(BuiltInRegistries.ITEM.getOrThrow((TagKey<Item>) tag));
        }
        if (o instanceof String name) {
            String id = name.startsWith("#") ? name.substring(1) : name;
            return Ingredient.of(BuiltInRegistries.ITEM.getOrThrow(TagKey.create(Registries.ITEM, Identifier.parse(id))));
        }
        throw new IllegalArgumentException("Don't know how to make an ingredient from " + o);
    }
}
