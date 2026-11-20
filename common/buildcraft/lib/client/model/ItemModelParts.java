/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.client.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.MapCodec;

import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import buildcraft.lib.client.sprite.SpriteHolderRegistry;

/** An item model drawn from BuildCraft's code-built quads (1.12.2 used ModelItemSimple / ModelPluggableItem with an
 * item override). A {@link Source} turns the stack into a key and the key into quads; the quads are baked on first use
 * after each resource reload (BuildCraft's own models load after item models bake), on the render thread.
 *
 * <p>Each source gets its own item model type: register {@link #codec(Source, ItemTransforms)} on
 * RegisterItemModelsEvent and name the type in the item definition. */
public final class ItemModelParts<K> implements ItemModel {

    /** Turns a stack into the quads it shows. */
    public interface Source<K> {
        /** @return The key for this stack: equal keys must give equal quads. */
        K key(ItemStack stack);

        /** Adds the quads for a key. Called on the render thread. */
        void addQuads(K key, QuadSink sink);
    }

    public interface QuadSink {
        void add(MutableQuad[] quads, boolean translucent);
    }

    // 1.12.2 ModelItemSimple transforms (values from minecraft:models/block/block.json and BuildCraft's plug tweaks)

    public static final ItemTransforms TRANSFORM_BLOCK;
    public static final ItemTransforms TRANSFORM_PLUG_AS_ITEM;
    public static final ItemTransforms TRANSFORM_PLUG_AS_ITEM_BIGGER;
    public static final ItemTransforms TRANSFORM_PLUG_AS_BLOCK;

    static {
        ItemTransform thirdpLeft = def(75, 45, 0, 0, 2.5, 0, 0.375);
        ItemTransform thirdpRight = def(75, 225, 0, 0, 2.5, 0, 0.375);
        ItemTransform firstpLeft = def(0, 225, 0, 0, 0, 0, 0.4);
        ItemTransform firstpRight = def(0, 45, 0, 0, 0, 0, 0.4);
        ItemTransform head = def(0, 0, 0, 0, 0, 0, 1);
        ItemTransform gui = def(30, 225, 0, 0, 0, 0, 0.625);
        ItemTransform ground = def(0, 0, 0, 0, 3, 0, 0.25);
        ItemTransform fixed = def(0, 0, 0, 0, 0, 0, 0.5);
        TRANSFORM_BLOCK = transforms(thirdpLeft, thirdpRight, firstpLeft, firstpRight, head, gui, ground, fixed);

        ItemTransform itemHead = def(0, 0, 0, 0, 0, 0, 1);
        ItemTransform itemGui = def(0, 90, 0, 0, 0, 0, 1);
        ItemTransform itemGround = def(0, 0, 0, 0, 3, 0, 0.5);
        ItemTransform itemFixed = def(0, 0, 0, 0, 0, 0, 0.85);
        firstpLeft = def(0, 225, 0, 0, 0, -4, 0.4);
        firstpRight = def(0, 45, 0, 0, 0, -4, 0.4);
        TRANSFORM_PLUG_AS_ITEM = transforms(thirdpLeft, thirdpRight, firstpLeft, firstpRight, itemHead, itemGui,
            itemGround, itemFixed);
        TRANSFORM_PLUG_AS_ITEM_BIGGER = scale(TRANSFORM_PLUG_AS_ITEM, 1.8f);

        thirdpLeft = def(75, 45, 0, 0, 2.5, 0, 0.375);
        thirdpRight = def(75, 225, 0, 0, 2.5, 0, 0.375);
        firstpLeft = def(0, 45, 0, 0, 0, 0, 0.4);
        firstpRight = def(0, 225, 0, 0, 0, 0, 0.4);
        gui = def(30, 135, 0, -3, 1.5, 0, 0.625);
        TRANSFORM_PLUG_AS_BLOCK = transforms(thirdpLeft, thirdpRight, firstpLeft, firstpRight, head, gui, ground, fixed);
    }

    private static ItemTransform def(double rx, double ry, double rz, double tx, double ty, double tz, double scale) {
        return new ItemTransform(new Vector3f((float) rx, (float) ry, (float) rz),
            new Vector3f((float) tx / 16f, (float) ty / 16f, (float) tz / 16f),
            new Vector3f((float) scale, (float) scale, (float) scale));
    }

    private static ItemTransforms transforms(ItemTransform thirdpLeft, ItemTransform thirdpRight,
        ItemTransform firstpLeft, ItemTransform firstpRight, ItemTransform head, ItemTransform gui,
        ItemTransform ground, ItemTransform fixed) {
        return new ItemTransforms(thirdpLeft, thirdpRight, firstpLeft, firstpRight, head, gui, ground, fixed,
            fixed, ImmutableMap.of());
    }

    private static ItemTransform scale(ItemTransform from, float by) {
        return new ItemTransform(from.rotation(), from.translation(), new Vector3f(from.scale()).mul(by),
            from.rightRotation());
    }

    private static ItemTransforms scale(ItemTransforms from, float by) {
        return transforms(scale(from.thirdPersonLeftHand(), by), scale(from.thirdPersonRightHand(), by),
            scale(from.firstPersonLeftHand(), by), scale(from.firstPersonRightHand(), by), scale(from.head(), by),
            scale(from.gui(), by), scale(from.ground(), by), scale(from.fixed(), by));
    }

    /** @return The item model type codec for a source. Register it once on RegisterItemModelsEvent. */
    public static <K> MapCodec<ItemModel.Unbaked> codec(Source<K> source, ItemTransforms transforms) {
        return codec(source, transforms, false);
    }

    /** @param blockLight true to light the item like a block (1.12.2's isGui3d), false for flat item lighting. */
    public static <K> MapCodec<ItemModel.Unbaked> codec(Source<K> source, ItemTransforms transforms,
        boolean blockLight) {
        Unbaked<K> unbaked = new Unbaked<>(source, transforms, blockLight);
        unbaked.codec = MapCodec.unit((ItemModel.Unbaked) unbaked);
        return unbaked.codec;
    }

    private record Geometry(ItemQuads quads, Supplier<Vector3fc[]> extents, Material.Baked particle) {}

    private final Source<K> source;
    private final ItemTransforms transforms;
    private final boolean blockLight;
    private final Matrix4fc transformation;
    private final Map<K, Geometry> cache = new ConcurrentHashMap<>();

    private ItemModelParts(Source<K> source, ItemTransforms transforms, boolean blockLight, Matrix4fc transformation) {
        this.source = source;
        this.transforms = transforms;
        this.blockLight = blockLight;
        this.transformation = transformation;
    }

    @Override
    public void update(ItemStackRenderState output, ItemStack item, ItemModelResolver resolver,
        ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        K key = source.key(item);
        Geometry geometry = cache.computeIfAbsent(key, this::bake);

        output.appendModelIdentityElement(this);
        output.appendModelIdentityElement(key);
        ItemStackRenderState.LayerRenderState layer = output.newLayer();
        if (item.hasFoil()) {
            layer.setFoilType(ItemStackRenderState.FoilType.STANDARD);
            output.setAnimated();
            output.appendModelIdentityElement(ItemStackRenderState.FoilType.STANDARD);
        }
        layer.setExtents(geometry.extents);
        layer.setLocalTransform(transformation);
        new ModelRenderProperties(blockLight, geometry.particle, transforms).applyToLayer(layer, displayContext);
        layer.setQuads(geometry.quads);
    }

    private Geometry bake(K key) {
        List<BakedQuad> quads = new ArrayList<>();
        source.addQuads(key, (from, translucent) -> {
            if (from == null) {
                return;
            }
            for (MutableQuad q : from) {
                if (q != null) {
                    quads.add(q.toBakedQuad(translucent));
                }
            }
        });
        List<BakedQuad> all = List.copyOf(quads);
        TextureAtlasSprite particle = all.isEmpty() ? SpriteHolderRegistry.missingSprite()
            : all.get(0).materialInfo().sprite();
        return new Geometry(ItemQuads.split(all), Suppliers.memoize(() -> CuboidItemModelWrapper.computeExtents(all)),
            new Material.Baked(particle, false));
    }

    private static final class Unbaked<K> implements ItemModel.Unbaked {
        private final Source<K> source;
        private final ItemTransforms transforms;
        private final boolean blockLight;
        MapCodec<ItemModel.Unbaked> codec;

        Unbaked(Source<K> source, ItemTransforms transforms, boolean blockLight) {
            this.source = source;
            this.transforms = transforms;
            this.blockLight = blockLight;
        }

        @Override
        public MapCodec<? extends ItemModel.Unbaked> type() {
            return codec;
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {}

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            return new ItemModelParts<>(source, transforms, blockLight, transformation);
        }
    }
}
