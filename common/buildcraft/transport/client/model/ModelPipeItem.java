/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.client.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.google.common.base.Suppliers;
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
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import buildcraft.api.transport.pipe.EnumPipeColourType;
import buildcraft.api.transport.pipe.PipeDefinition;
import buildcraft.api.transport.pipe.PipeFaceTex;

import buildcraft.lib.client.model.ModelUtil;
import buildcraft.lib.client.model.ModelUtil.UvFaceData;
import buildcraft.lib.client.model.MutableQuad;
import buildcraft.lib.client.sprite.SpriteHolderRegistry;
import buildcraft.lib.misc.ColourUtil;

import buildcraft.transport.BCTransportSprites;
import buildcraft.transport.item.ItemPipeHolder;

/** A pipe item: a short upright pipe drawn from its pipe's sprites, like 1.12.2's ModelPipeItem. Item definitions name
 * it as {@code {"type": "buildcrafttransport:pipe"}}; the pipe (and its colour) come from the stack. */
public final class ModelPipeItem implements ItemModel {
    public static final Identifier TYPE = Identifier.fromNamespaceAndPath("buildcrafttransport", "pipe");
    /** Pipe items use the block display transforms (1.12.2's ModelItemSimple.TRANSFORM_BLOCK). */
    private static final Identifier TRANSFORMS_FROM = Identifier.withDefaultNamespace("block/block");

    private static final int INDEX_TOP = 0;
    private static final int INDEX_CENTER = 1;
    private static final int INDEX_BOTTOM = 2;

    private static final MutableQuad[] QUADS_SAME;
    private static final MutableQuad[][] QUADS_DIFFERENT;
    private static final MutableQuad[] QUADS_COLOUR;

    static {
        // Same sprite for all 3 sections
        QUADS_SAME = cube(new Vector3f(0.5f, 0.5f, 0.5f), new Vector3f(0.25f, 0.5f, 0.25f),
            UvFaceData.from16(4, 4, 12, 12), UvFaceData.from16(4, 0, 12, 16), null);

        // A different sprite for each of the 3 sections
        QUADS_DIFFERENT = new MutableQuad[3][];
        QUADS_DIFFERENT[INDEX_TOP] = cube(new Vector3f(0.5f, 0.875f, 0.5f), new Vector3f(0.25f, 0.125f, 0.25f),
            UvFaceData.from16(4, 4, 12, 12), UvFaceData.from16(4, 0, 12, 4), Direction.DOWN);
        QUADS_DIFFERENT[INDEX_CENTER] = cube(new Vector3f(0.5f, 0.5f, 0.5f), new Vector3f(0.25f, 0.25f, 0.25f),
            UvFaceData.from16(4, 4, 12, 12), UvFaceData.from16(4, 4, 12, 12), null);
        // the center section has no top or bottom
        QUADS_DIFFERENT[INDEX_CENTER][Direction.UP.ordinal()] = null;
        QUADS_DIFFERENT[INDEX_CENTER][Direction.DOWN.ordinal()] = null;
        QUADS_DIFFERENT[INDEX_BOTTOM] = cube(new Vector3f(0.5f, 0.125f, 0.5f), new Vector3f(0.25f, 0.125f, 0.25f),
            UvFaceData.from16(4, 4, 12, 12), UvFaceData.from16(4, 12, 12, 16), Direction.UP);

        // Translucent coloured pipes
        QUADS_COLOUR = cube(new Vector3f(0.5f, 0.5f, 0.5f), new Vector3f(0.24f, 0.49f, 0.24f),
            UvFaceData.from16(4, 4, 12, 12), UvFaceData.from16(4, 0, 12, 16), null);
    }

    private static MutableQuad[] cube(Vector3f center, Vector3f radius, UvFaceData uvsY, UvFaceData uvsXZ,
        @Nullable Direction skip) {
        MutableQuad[] cube = new MutableQuad[6];
        for (Direction face : Direction.values()) {
            if (face == skip) {
                continue;
            }
            UvFaceData uvs = face.getAxis() == Axis.Y ? uvsY : uvsXZ;
            cube[face.ordinal()] = ModelUtil.createFace(face, center, radius, uvs);
        }
        return cube;
    }

    private record Key(@Nullable PipeDefinition definition, @Nullable DyeColor colour) {}

    private record Geometry(ItemQuads quads, Supplier<Vector3fc[]> extents, Material.Baked particle) {}

    private final ItemTransforms transforms;
    private final Matrix4fc transformation;
    private final Map<Key, Geometry> cache = new ConcurrentHashMap<>();

    private ModelPipeItem(ItemTransforms transforms, Matrix4fc transformation) {
        this.transforms = transforms;
        this.transformation = transformation;
    }

    @Override
    public void update(ItemStackRenderState output, ItemStack item, ItemModelResolver resolver,
        ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        ModelPipe.clearCachesIfReloaded();
        PipeDefinition definition = item.getItem() instanceof ItemPipeHolder pipe ? pipe.definition : null;
        DyeColor colour = ItemPipeHolder.getColour(item);
        Key key = new Key(definition, colour);
        Geometry geometry = cache.computeIfAbsent(key, ModelPipeItem::bake);

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
        new ModelRenderProperties(true, geometry.particle, transforms).applyToLayer(layer, displayContext);
        layer.setQuads(geometry.quads);
    }

    private static Geometry bake(Key key) {
        PipeFaceTex center = PipeFaceTex.NO_SPRITE;
        PipeFaceTex top = center;
        PipeFaceTex bottom = center;
        TextureAtlasSprite[] sprites = { SpriteHolderRegistry.missingSprite() };
        EnumPipeColourType type = EnumPipeColourType.TRANSLUCENT;
        if (key.definition != null) {
            top = key.definition.itemModelTop;
            center = key.definition.itemModelCenter;
            bottom = key.definition.itemModelBottom;
            type = key.definition.getColourType();
            sprites = PipeModelCacheBase.generator.getItemSprites(key.definition);
        }

        List<BakedQuad> quads = new ArrayList<>();
        if (center == top && center == bottom) {
            addQuads(QUADS_SAME, sprites, quads, center);
        } else {
            addQuads(QUADS_DIFFERENT[INDEX_BOTTOM], sprites, quads, bottom);
            addQuads(QUADS_DIFFERENT[INDEX_CENTER], sprites, quads, center);
            addQuads(QUADS_DIFFERENT[INDEX_TOP], sprites, quads, top);
        }

        if (key.colour != null) {
            int rgb = 0xFF_00_00_00 | ColourUtil.swapArgbToAbgr(ColourUtil.getLightHex(key.colour));
            if (type == EnumPipeColourType.TRANSLUCENT) {
                addQuadsColoured(QUADS_COLOUR, quads, BCTransportSprites.PIPE_COLOUR.getSprite(), rgb, true);
            } else if (type == EnumPipeColourType.BORDER_OUTER) {
                addQuadsColoured(QUADS_SAME, quads, BCTransportSprites.PIPE_COLOUR_BORDER_OUTER.getSprite(), rgb, false);
            } else if (type == EnumPipeColourType.BORDER_INNER) {
                addQuadsColoured(QUADS_SAME, quads, BCTransportSprites.PIPE_COLOUR_BORDER_INNER.getSprite(), rgb, false);
            }
        }

        List<BakedQuad> all = List.copyOf(quads);
        TextureAtlasSprite particle = all.isEmpty() ? SpriteHolderRegistry.missingSprite()
            : all.get(0).materialInfo().sprite();
        return new Geometry(ItemQuads.split(all), Suppliers.memoize(() -> CuboidItemModelWrapper.computeExtents(all)),
            new Material.Baked(particle, false));
    }

    private static void addQuads(MutableQuad[] from, TextureAtlasSprite[] sprites, List<BakedQuad> to,
        PipeFaceTex face) {
        for (int i = 0; i < face.getCount(); i++) {
            int colour = face.getColour(i);
            TextureAtlasSprite sprite = getSprite(sprites, face.getTexture(i));
            for (MutableQuad f : from) {
                if (f == null) {
                    continue;
                }
                MutableQuad copy = new MutableQuad(f);
                copy.texFromSprite(sprite);
                copy.colouri(colour);
                to.add(copy.toBakedQuad(false));
            }
        }
    }

    private static TextureAtlasSprite getSprite(TextureAtlasSprite[] sprites, int spriteIndex) {
        if (sprites == null || spriteIndex < 0 || spriteIndex >= sprites.length || sprites[spriteIndex] == null) {
            return SpriteHolderRegistry.missingSprite();
        }
        return sprites[spriteIndex];
    }

    private static void addQuadsColoured(MutableQuad[] from, List<BakedQuad> to, TextureAtlasSprite sprite, int colour,
        boolean translucent) {
        for (MutableQuad f : from) {
            if (f == null) {
                continue;
            }
            MutableQuad copy = new MutableQuad(f);
            copy.texFromSprite(sprite);
            copy.colouri(colour);
            to.add(copy.toBakedQuad(translucent));
        }
    }

    public record Unbaked() implements ItemModel.Unbaked {
        public static final MapCodec<Unbaked> CODEC = MapCodec.unit(new Unbaked());

        @Override
        public MapCodec<Unbaked> type() {
            return CODEC;
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            resolver.markDependency(TRANSFORMS_FROM);
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            ItemTransforms transforms = context.blockModelBaker().getModel(TRANSFORMS_FROM).getTopTransforms();
            return new ModelPipeItem(transforms, transformation);
        }
    }
}
