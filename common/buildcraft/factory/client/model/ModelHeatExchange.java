/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.client.model;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
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

import buildcraft.lib.block.BlockBCBase_Neptune;
import buildcraft.lib.client.model.MutableQuad;
import buildcraft.lib.client.sprite.SpriteHolderRegistry;
import buildcraft.lib.expression.DefaultContexts;
import buildcraft.lib.expression.FunctionContext;
import buildcraft.lib.expression.api.NodeType;
import buildcraft.lib.expression.api.NodeTypes;
import buildcraft.lib.expression.node.value.NodeVariableBoolean;
import buildcraft.lib.expression.node.value.NodeVariableObject;

import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.BCFactoryModels;
import buildcraft.factory.block.BlockHeatExchange.EnumExchangePart;
import buildcraft.factory.tile.TileHeatExchange;

/** The heat exchanger's block model: its variable model (models/tiles/heat_exchange_static.json) baked for every
 * part, facing and left/right connection. The part comes from the tile's model data, the connections from the
 * neighbours' states. The variants are worked out on the first use after each reload, as the variable model is only
 * loaded once models are baked. */
public final class ModelHeatExchange implements DynamicBlockStateModel {
    public static final ModelHeatExchange INSTANCE = new ModelHeatExchange();
    public static final Identifier TYPE = Identifier.fromNamespaceAndPath("buildcraftfactory", "heat_exchange");

    public static final NodeType<EnumExchangePart> TYPE_EXCHANGE_PART;
    public static final FunctionContext FUNCTION_CONTEXT;
    public static final NodeVariableBoolean VAR_CONNECTED_LEFT;
    public static final NodeVariableBoolean VAR_CONNECTED_RIGHT;
    public static final NodeVariableBoolean VAR_CONNECTED_UP;
    public static final NodeVariableBoolean VAR_CONNECTED_DOWN;
    public static final NodeVariableObject<EnumExchangePart> VAR_PART;
    public static final NodeVariableObject<Direction> VAR_DIRECTION;

    static {
        TYPE_EXCHANGE_PART = new NodeType<>("HeatExchangePart", EnumExchangePart.MIDDLE);
        NodeTypes.addType(TYPE_EXCHANGE_PART);
        for (EnumExchangePart part : EnumExchangePart.values()) {
            TYPE_EXCHANGE_PART.putConstant(part.getName(), part);
        }
        FUNCTION_CONTEXT = new FunctionContext("heat_exchange", DefaultContexts.createWithAll(), TYPE_EXCHANGE_PART);
        VAR_CONNECTED_LEFT = FUNCTION_CONTEXT.putVariableBoolean("connected_left");
        VAR_CONNECTED_RIGHT = FUNCTION_CONTEXT.putVariableBoolean("connected_right");
        VAR_CONNECTED_UP = FUNCTION_CONTEXT.putVariableBoolean("connected_up");
        VAR_CONNECTED_DOWN = FUNCTION_CONTEXT.putVariableBoolean("connected_down");
        VAR_PART = FUNCTION_CONTEXT.putVariableObject("part", EnumExchangePart.class);
        VAR_DIRECTION = FUNCTION_CONTEXT.putVariableObject("direction", Direction.class);
    }

    private static final int VARIANTS = 4 * 4 * 3;

    @Nullable
    private static Part[] cache;

    private ModelHeatExchange() {}

    private record Part(List<BakedQuad> quads, Material.Baked particle, int flags) implements BlockStateModelPart {
        @Override
        public List<BakedQuad> getQuads(@Nullable Direction direction) {
            return direction == null ? quads : List.of();
        }

        @Override
        public boolean useAmbientOcclusion() {
            return false;
        }

        @Override
        public Material.Baked particleMaterial() {
            return particle;
        }

        @Override
        public int materialFlags() {
            return flags;
        }
    }

    /** Index of a variant: left, right, facing (horizontal), part. */
    private static int index(boolean left, boolean right, Direction facing, EnumExchangePart part) {
        return (left ? 1 : 0) | (right ? 2 : 0) | (facing.get2DDataValue() * 4) | (part.ordinal() * 16);
    }

    private static synchronized Part[] getCache() {
        Part[] parts = cache;
        if (parts == null) {
            parts = new Part[VARIANTS];
            for (int i = 0; i < VARIANTS; i++) {
                EnumExchangePart part = EnumExchangePart.values()[i / 16];
                VAR_CONNECTED_LEFT.value = (i & 1) == 1;
                VAR_CONNECTED_RIGHT.value = (i & 2) == 2;
                VAR_CONNECTED_UP.value = false;
                VAR_CONNECTED_DOWN.value = false;
                VAR_PART.value = part;
                VAR_DIRECTION.value = Direction.from2DDataValue((i / 4) & 3);
                List<BakedQuad> quads = new ArrayList<>();
                int flags = 0;
                for (MutableQuad quad : BCFactoryModels.HEAT_EXCHANGE_STATIC.getCutoutQuads()) {
                    MutableQuad copy = new MutableQuad(quad);
                    copy.multShade();
                    BakedQuad baked = copy.toBakedQuad(false);
                    flags |= baked.materialInfo().flags();
                    quads.add(baked);
                }
                Material.Baked particle = new Material.Baked(
                    quads.isEmpty() ? SpriteHolderRegistry.missingSprite() : quads.get(0).materialInfo().sprite(), false);
                parts[i] = new Part(List.copyOf(quads), particle, flags);
            }
            cache = parts;
        }
        return parts;
    }

    /** Forgets the baked variants (the variable model was reloaded). */
    public static synchronized void clearCache() {
        cache = null;
    }

    private static boolean doesNeighbourConnect(BlockAndTintGetter level, BlockPos pos, Direction thisFacing,
        Direction dir) {
        BlockState neighbour = level.getBlockState(pos.relative(dir));
        if (neighbour.getBlock() == BCFactoryBlocks.heatExchange) {
            return neighbour.getValue(BlockBCBase_Neptune.PROP_FACING) == thisFacing;
        }
        return false;
    }

    private static Part getPart(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        Direction facing = state.hasProperty(BlockBCBase_Neptune.PROP_FACING)
            ? state.getValue(BlockBCBase_Neptune.PROP_FACING) : Direction.NORTH;
        EnumExchangePart part = level.getModelData(pos).get(TileHeatExchange.MODEL_PART);
        if (part == null) {
            part = EnumExchangePart.MIDDLE;
        }
        boolean left = doesNeighbourConnect(level, pos, facing, facing.getClockWise());
        boolean right = doesNeighbourConnect(level, pos, facing, facing.getCounterClockWise());
        return getCache()[index(left, right, facing, part)];
    }

    @Override
    public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
        return getPart(level, pos, state);
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random,
        List<BlockStateModelPart> parts) {
        parts.add(getPart(level, pos, state));
    }

    @Override
    public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return getPart(level, pos, state).particle();
    }

    @Override
    public Material.Baked particleMaterial() {
        return getCache()[0].particle();
    }

    @Override
    public int materialFlags() {
        return getCache()[0].flags();
    }

    /** The blockstate file's model: <code>{"type": "buildcraftfactory:heat_exchange"}</code>. */
    public static final class Unbaked implements CustomUnbakedBlockStateModel {
        public static final Unbaked INSTANCE = new Unbaked();
        public static final MapCodec<Unbaked> CODEC = MapCodec.unit(INSTANCE);

        @Override
        public MapCodec<? extends CustomUnbakedBlockStateModel> codec() {
            return CODEC;
        }

        @Override
        public ModelHeatExchange bake(ModelBaker modelBakery) {
            clearCache();
            return ModelHeatExchange.INSTANCE;
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {}
    }
}
