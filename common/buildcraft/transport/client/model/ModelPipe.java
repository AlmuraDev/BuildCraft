/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.client.model;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import net.neoforged.neoforge.model.data.ModelData;
import net.neoforged.neoforge.model.data.ModelProperty;

import buildcraft.api.transport.pluggable.PipePluggable;

import buildcraft.lib.client.model.MutableQuad;
import buildcraft.lib.client.sprite.SpriteHolderRegistry;

import buildcraft.transport.client.model.PipeModelCacheAll.PipeAllCutoutKey;
import buildcraft.transport.client.model.PipeModelCacheAll.PipeAllTranslucentKey;
import buildcraft.transport.client.model.PipeModelCacheBase.PipeBaseCutoutKey;
import buildcraft.transport.client.model.key.PipeModelKey;
import buildcraft.transport.tile.TilePipeHolder;

/** The pipe holder's block model (1.12.2's ModelPipe, an IBakedModel that read the tile from its extended state).
 * <p>
 * The quads come from BuildCraft's model caches, which are not thread safe, so they are baked on the main thread when
 * the tile's model data is collected ({@link #createModelData}) and only read by the chunk meshing threads. Tints are
 * applied there too, as they read the tile's pluggables. */
public final class ModelPipe implements DynamicBlockStateModel {
    public static final ModelPipe INSTANCE = new ModelPipe();
    public static final Identifier TYPE = Identifier.fromNamespaceAndPath("buildcrafttransport", "pipe_holder");

    private static final ModelProperty<Geometry> GEOMETRY = new ModelProperty<>();

    @Nullable
    private static Geometry defaultGeometry;

    /** Set when resources were reloaded (models are baked off the main thread); the caches are cleared on the main
     * thread before the next pipe is baked. TODO(26.3): pipes keep their old model data (old sprites) until they next
     * change. */
    private static volatile boolean reloaded = false;

    private ModelPipe() {}

    /** The baked pipe: equal keys give equal quads. */
    static final class Geometry {
        final Object key;
        final List<BakedQuad> quads;
        final Material.Baked particle;
        final int flags;

        Geometry(Object key, List<BakedQuad> quads) {
            this.key = key;
            this.quads = quads;
            TextureAtlasSprite sprite = quads.isEmpty() ? SpriteHolderRegistry.missingSprite()
                : quads.get(0).materialInfo().sprite();
            this.particle = new Material.Baked(sprite, false);
            int f = 0;
            for (BakedQuad q : quads) {
                f |= q.materialInfo().flags();
            }
            this.flags = f;
        }
    }

    private record GeometryKey(PipeAllCutoutKey cutout, PipeAllTranslucentKey translucent, List<Integer> tints) {}

    private record Part(Geometry geometry) implements BlockStateModelPart {
        @Override
        public List<BakedQuad> getQuads(@Nullable Direction direction) {
            return direction == null ? geometry.quads : List.of();
        }

        @Override
        public boolean useAmbientOcclusion() {
            return false;
        }

        @Override
        public Material.Baked particleMaterial() {
            return geometry.particle;
        }

        @Override
        public int materialFlags() {
            return geometry.flags;
        }
    }

    /** Bakes the tile's current pipe model. Called on the main thread (see {@link TilePipeHolder#getModelData()}). */
    public static ModelData createModelData(TilePipeHolder tile) {
        clearCachesIfReloaded();
        if (tile.getPipe() == null) {
            return ModelData.EMPTY;
        }
        return createModelDataNoReload(tile);
    }

    /** Forgets the pipe models and sprites if the resources were reloaded since the last pipe was baked. Main thread
     * only (block model data and item models). */
    static void clearCachesIfReloaded() {
        if (reloaded) {
            reloaded = false;
            PipeModelCacheBase.generator.onResourcesReloaded();
            PipeModelCacheBase.cacheCutout.clear();
            PipeModelCacheBase.cacheTranslucent.clear();
            PipeModelCachePluggable.cacheCutoutAll.clear();
            PipeModelCachePluggable.cacheTranslucentAll.clear();
            PipeModelCachePluggable.cacheCutoutSingle.clear();
            PipeModelCachePluggable.cacheTranslucentSingle.clear();
            PipeModelCacheAll.clearModels();
            clearCache();
        }
    }

    private static ModelData createModelDataNoReload(TilePipeHolder tile) {
        PipeAllCutoutKey cutoutKey = new PipeAllCutoutKey(tile);
        PipeAllTranslucentKey translucentKey = new PipeAllTranslucentKey(tile);
        List<Integer> tints = new ArrayList<>();
        List<BakedQuad> quads = new ArrayList<>();
        bake(PipeModelCacheAll.cacheCutout.bake(cutoutKey), false, tile, tints, quads);
        bake(PipeModelCacheAll.cacheTranslucent.bake(translucentKey), true, tile, tints, quads);
        GeometryKey key = new GeometryKey(cutoutKey, translucentKey, tints);
        return ModelData.of(GEOMETRY, new Geometry(key, List.copyOf(quads)));
    }

    private static void bake(List<MutableQuad> from, boolean translucent, @Nullable TilePipeHolder tile,
        List<Integer> tintsOut, List<BakedQuad> into) {
        for (MutableQuad q : from) {
            int tint = q.getTint();
            if (tint >= 0 && tile != null) {
                int colour = getTintColour(tile, tint);
                tintsOut.add(colour);
                if (colour != -1) {
                    q = new MutableQuad(q);
                    q.multColouri((colour >> 16) & 0xFF, (colour >> 8) & 0xFF, colour & 0xFF, 0xFF);
                }
            }
            into.add(q.toBakedQuad(translucent));
        }
    }

    /** 1.12.2's PipeBlockColours: tint indices are side + 6 * (the pluggable's own tint index). */
    private static int getTintColour(TilePipeHolder tile, int tintIndex) {
        Direction side = Direction.from3DDataValue(tintIndex % 6);
        PipePluggable pluggable = tile.getPluggable(side);
        return pluggable == null ? -1 : pluggable.getBlockColor(tintIndex / 6);
    }

    private static synchronized Geometry getDefaultGeometry() {
        Geometry geometry = defaultGeometry;
        if (geometry == null) {
            List<BakedQuad> quads = new ArrayList<>();
            bake(PipeModelCacheBase.cacheCutout.bake(new PipeBaseCutoutKey(PipeModelKey.DEFAULT_KEY)), false, null,
                new ArrayList<>(), quads);
            geometry = new Geometry("default", List.copyOf(quads));
            defaultGeometry = geometry;
        }
        return geometry;
    }

    /** Forgets the default model (the atlases or the model caches were rebuilt). */
    public static synchronized void clearCache() {
        defaultGeometry = null;
    }

    private static Geometry getGeometry(BlockAndTintGetter level, BlockPos pos) {
        Geometry geometry = level.getModelData(pos).get(GEOMETRY);
        return geometry != null ? geometry : getDefaultGeometry();
    }

    @Override
    public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
        return getGeometry(level, pos).key;
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
        List<BlockStateModelPart> parts) {
        parts.add(new Part(getGeometry(level, pos)));
    }

    @Override
    public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return getGeometry(level, pos).particle;
    }

    @Override
    public Material.Baked particleMaterial() {
        return getDefaultGeometry().particle;
    }

    @Override
    public int materialFlags() {
        return BakedQuad.FLAG_TRANSLUCENT;
    }

    /** The blockstate file's model: <code>{"type": "buildcrafttransport:pipe_holder"}</code>. */
    public static final class Unbaked implements CustomUnbakedBlockStateModel {
        public static final Unbaked INSTANCE = new Unbaked();
        public static final MapCodec<Unbaked> CODEC = MapCodec.unit(INSTANCE);

        @Override
        public MapCodec<? extends CustomUnbakedBlockStateModel> codec() {
            return CODEC;
        }

        @Override
        public ModelPipe bake(ModelBaker modelBakery) {
            reloaded = true;
            return ModelPipe.INSTANCE;
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {}
    }
}
