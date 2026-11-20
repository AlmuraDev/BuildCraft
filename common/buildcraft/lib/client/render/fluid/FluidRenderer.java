/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.client.render.fluid;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.FluidModel;

import buildcraft.lib.client.model.ModelUtil;
import buildcraft.lib.client.sprite.SpriteHolderRegistry;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;


import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import buildcraft.api.transfer.IFluidTank;
import net.neoforged.fml.LogicalSide;

import buildcraft.lib.client.model.MutableVertex;
import buildcraft.lib.misc.MathUtil;
import buildcraft.lib.misc.VecUtil;

/** Can render 3D fluid cuboid's, up to 1x1x1 in size. Note that they *must* be contained within the 1x1x1 block space -
 * you can't use this to render off large multiblocks. Not thread safe -- this uses static variables so you should only
 * call this from the main client thread. */
// TODO: thread safety (per thread context?)
// Perhaps move this into IModelRenderer? And that way we get the buffer, force shaders to cope with fluids (?!), etc
public class FluidRenderer {

    private static final EnumMap<FluidSpriteType, Map<String, TextureAtlasSprite>> fluidSprites
        = new EnumMap<>(FluidSpriteType.class);
    public static final MutableVertex vertex = new MutableVertex();
    private static final PoseStack.Pose IDENTITY_POSE = new PoseStack().last();
    private static final boolean[] DEFAULT_FACES = { true, true, true, true, true, true };

    private static final Map<String, Integer> fluidAvgColours = new HashMap<>();

    // Cached fields that prevent lots of arguments on most methods
    private static VertexConsumer bb;
    private static TextureAtlasSprite sprite;
    private static TexMap texmap;
    private static boolean invertU, invertV;
    private static double xTexDiff, yTexDiff, zTexDiff;

    static {
        // TODO: allow the caller to change the light level
        vertex.lighti(0xF, 0xF);
        for (FluidSpriteType type : FluidSpriteType.values()) {
            fluidSprites.put(type, new HashMap<>());
        }
    }

    /** Forgets the sprites and colours looked up so far (resources reloaded). */
    public static void onResourcesReloaded() {
        for (FluidSpriteType type : FluidSpriteType.values()) {
            fluidSprites.get(type).clear();
        }
        fluidAvgColours.clear();
    }

    /** Renders a fluid cuboid to the given vertex buffer. The cube shouldn't cross over any {@literal 0->1} boundary
     * (so the cube must be contained within a block).
     * 
     * @param type The type of sprite to use. See {@link FluidSpriteType} for more details.
     * @param tank The fluid tank that should be rendered.
     * @param min The minimum coordinate that the tank should be rendered from
     * @param max The maximum coordinate that the tank will be rendered to.
     * @param bbIn The {@link VertexConsumer} that the fluid will be rendered into.
     * @param sideRender A size 6 boolean array that determines if the face will be rendered. If it is null then all
     *            faces will be rendered. The indexes are determined by what {@link Direction#ordinal()} returns.
     * @see #renderFluid(FluidSpriteType, FluidStack, double, double, Vec3, Vec3, VertexConsumer, boolean[]) */
    public static void renderFluid(
        FluidSpriteType type, IFluidTank tank, Vec3 min, Vec3 max, VertexConsumer bbIn, boolean[] sideRender
    ) {
        renderFluid(type, tank.getFluid(), tank.getCapacity(), min, max, bbIn, sideRender);
    }

    /** Render's a fluid cuboid to the given vertex buffer. The cube shouldn't cross over any {@literal 0->1} boundary
     * (so the cube must be contained within a block).
     * 
     * @param type The type of sprite to use. See {@link FluidSpriteType} for more details.
     * @param fluid The stack that represents the fluid to render
     * @param cap The maximum amount of fluid that could be in the stack. Usually the capacity of the tank.
     * @param min The minimum coordinate that the tank should be rendered from
     * @param max The maximum coordinate that the tank will be rendered to.
     * @param bbIn The {@link VertexConsumer} that the fluid will be rendered into.
     * @param sideRender A size 6 boolean array that determines if the face will be rendered. If it is null then all
     *            faces will be rendered. The indexes are determined by what {@link Direction#ordinal()} returns. */
    public static void renderFluid(
        FluidSpriteType type, FluidStack fluid, int cap, Vec3 min, Vec3 max, VertexConsumer bbIn, boolean[] sideRender
    ) {
        renderFluid(type, fluid, fluid == null ? 0 : fluid.getAmount(), cap, min, max, bbIn, sideRender);
    }

    /** Render's a fluid cuboid to the given vertex buffer. The cube shouldn't cross over any {@literal 0->1} boundary
     * (so the cube must be contained within a block).
     * 
     * @param type The type of sprite to use. See {@link FluidSpriteType} for more details.
     * @param fluid The stack that represents the fluid to render. Note that the amount from the stack is NOT used.
     * @param amount The actual amount of fluid in the stack. Is a "double" rather than an "int" as then you can
     *            interpolate between frames.
     * @param cap The maximum amount of fluid that could be in the stack. Usually the capacity of the tank.
     * @param min The minimum coordinate that the tank should be rendered from
     * @param max The maximum coordinate that the tank will be rendered to.
     * @param bbIn The {@link VertexConsumer} that the fluid will be rendered into.
     * @param sideRender A size 6 boolean array that determines if the face will be rendered. If it is null then all
     *            faces will be rendered. The indexes are determined by what {@link Direction#ordinal()} returns. */
    public static void renderFluid(
        FluidSpriteType type, FluidStack fluid, double amount, double cap, Vec3 min, Vec3 max, VertexConsumer bbIn,
        boolean[] sideRender
    ) {
        if (fluid == null || fluid.getFluid() == null || amount <= 0) {
            return;
        }
        if (sideRender == null) {
            sideRender = DEFAULT_FACES;
        }

        double height = Mth.clamp(amount / cap, 0, 1);
        final Vec3 realMin, realMax;
        if (fluid.getFluid().getFluidType().isLighterThanAir()) {
            realMin = VecUtil.replaceValue(min, Axis.Y, MathUtil.interp(1 - height, min.y, max.y));
            realMax = max;
        } else {
            realMin = min;
            realMax = VecUtil.replaceValue(max, Axis.Y, MathUtil.interp(height, min.y, max.y));
        }

        bb = bbIn;

        if (type == null) {
            type = FluidSpriteType.STILL;
        }
        sprite = getFluidSprite(type, fluid);

        final double xs = realMin.x;
        final double ys = realMin.y;
        final double zs = realMin.z;

        final double xb = realMax.x;
        final double yb = realMax.y;
        final double zb = realMax.z;

        if (type == FluidSpriteType.FROZEN) {
            if (min.x > 1) {
                xTexDiff = Math.floor(min.x);
            } else if (min.x < 0) {
                xTexDiff = Math.floor(min.x);
            } else {
                xTexDiff = 0;
            }
            if (min.y > 1) {
                yTexDiff = Math.floor(min.y);
            } else if (min.y < 0) {
                yTexDiff = Math.floor(min.y);
            } else {
                yTexDiff = 0;
            }
            if (min.z > 1) {
                zTexDiff = Math.floor(min.z);
            } else if (min.z < 0) {
                zTexDiff = Math.floor(min.z);
            } else {
                zTexDiff = 0;
            }
        } else {
            xTexDiff = 0;
            yTexDiff = 0;
            zTexDiff = 0;
        }

        vertex.colouri(ModelUtil.swapARGBforABGR(getFluidTint(fluid)));

        texmap = TexMap.XZ;
        // TODO: Enable/disable inversion for the correct faces
        invertU = false;
        invertV = false;
        if (sideRender[Direction.UP.ordinal()]) {
            vertex(xs, yb, zb);
            vertex(xb, yb, zb);
            vertex(xb, yb, zs);
            vertex(xs, yb, zs);
        }

        if (sideRender[Direction.DOWN.ordinal()]) {
            vertex(xs, ys, zs);
            vertex(xb, ys, zs);
            vertex(xb, ys, zb);
            vertex(xs, ys, zb);
        }

        texmap = TexMap.ZY;
        if (sideRender[Direction.WEST.ordinal()]) {
            vertex(xs, ys, zs);
            vertex(xs, ys, zb);
            vertex(xs, yb, zb);
            vertex(xs, yb, zs);
        }

        if (sideRender[Direction.EAST.ordinal()]) {
            vertex(xb, yb, zs);
            vertex(xb, yb, zb);
            vertex(xb, ys, zb);
            vertex(xb, ys, zs);
        }

        texmap = TexMap.XY;
        if (sideRender[Direction.NORTH.ordinal()]) {
            vertex(xs, yb, zs);
            vertex(xb, yb, zs);
            vertex(xb, ys, zs);
            vertex(xs, ys, zs);
        }

        if (sideRender[Direction.SOUTH.ordinal()]) {
            vertex(xs, ys, zb);
            vertex(xb, ys, zb);
            vertex(xb, yb, zb);
            vertex(xs, yb, zb);
        }

        sprite = null;
        texmap = null;
        bb = null;
    }

    public static TextureAtlasSprite getFluidSprite(FluidSpriteType type, FluidStack fluid) {
        return getFluidSprite(type, fluid.getFluid());
    }

    /** FROZEN used a non-animated copy of the still sprite on 1.12.2; 26.3 sprites can't be copied that way, so it is
     * the still sprite. */
    public static TextureAtlasSprite getFluidSprite(FluidSpriteType type, Fluid fluid) {
        if (fluid == null) {
            return SpriteHolderRegistry.missingSprite();
        }
        String name = BuiltInRegistries.FLUID.getKey(fluid).toString();
        return fluidSprites.get(type).computeIfAbsent(name, n -> {
            FluidModel model = getFluidModel(fluid);
            if (model == null) {
                return SpriteHolderRegistry.missingSprite();
            }
            return (type == FluidSpriteType.FLOWING ? model.flowingMaterial() : model.stillMaterial()).sprite();
        });
    }

    /** @return The 26.3 fluid model (sprites and tint) of the fluid, or null if it has none. */
    @Nullable
    private static FluidModel getFluidModel(Fluid fluid) {
        return Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluid.defaultFluidState());
    }

    /** @return The tint (ARGB) the fluid is drawn with. */
    public static int getFluidTint(FluidStack stack) {
        FluidModel model = getFluidModel(stack.getFluid());
        if (model == null || model.fluidTintSource() == null) {
            return -1;
        }
        return model.fluidTintSource().colorAsStack(stack);
    }

    /** @return The average colour of the fluid's still sprite, tinted by the fluid's colour. */
    public static int getAverageFluidColour(Fluid fluid) {
        String name = BuiltInRegistries.FLUID.getKey(fluid).toString();
        return fluidAvgColours.computeIfAbsent(name, n -> {
            TextureAtlasSprite sprite = getFluidSprite(FluidSpriteType.STILL, fluid);
            int w = sprite.contents().width();
            int h = sprite.contents().height();
            double r = 0, g = 0, b = 0;
            int count = 0;
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    int pixel = sprite.getPixelARGB(0, x, y);
                    r += (pixel >> 16) & 0xFF;
                    g += (pixel >> 8) & 0xFF;
                    b += pixel & 0xFF;
                    count++;
                }
            }
            if (count > 0) {
                r /= count;
                g /= count;
                b /= count;
            }
            int colour = getFluidTint(new FluidStack(fluid, 1));
            r *= ((colour >> 16) & 0xFF) / 255.0;
            g *= ((colour >> 8) & 0xFF) / 255.0;
            b *= (colour & 0xFF) / 255.0;
            return 0xFF_00_00_00 | (MathUtil.clamp(r, 0, 0xFF) << 16) | (MathUtil.clamp(g, 0, 0xFF) << 8)
                | MathUtil.clamp(b, 0, 0xFF);
        });
    }

    /** Helper function to add a vertex. */
    private static void vertex(double x, double y, double z) {
        vertex.positiond(x, y, z);
        texmap.apply(x - xTexDiff, y - yTexDiff, z - zTexDiff);
        vertex.render(bb, IDENTITY_POSE, OverlayTexture.NO_OVERLAY);
    }

    // GUIs draw fluids with GuiUtil.drawFluidForGui.

    /** Used to keep track of what position maps to what texture co-ord.
     * <p>
     * For example XY maps X to U and Y to V, and ignores Z */
    private enum TexMap {
        XY(true, true),
        XZ(true, false),
        ZY(false, true);

        /** If true, then X maps to U. Otherwise Z maps to U. */
        private final boolean ux;
        /** If true, then Y maps to V. Otherwise Z maps to V. */
        private final boolean vy;

        TexMap(boolean ux, boolean vy) {
            this.ux = ux;
            this.vy = vy;
        }

        /** Changes the vertex's texture co-ord to be the same as the position, for that face. (Uses {@link #ux} and
         * {@link #vy} to determine how they are mapped). */
        private void apply(double x, double y, double z) {
            double realu = ux ? x : z;
            double realv = vy ? y : z;
            if (invertU) {
                realu = 1 - realu;
            }
            if (invertV) {
                realv = 1 - realv;
            }
            vertex.texf(sprite.getU((float) realu), sprite.getV((float) realv));
        }
    }

    public static class TankSize {
        public final Vec3 min;
        public final Vec3 max;

        public TankSize(int sx, int sy, int sz, int ex, int ey, int ez) {
            this(new Vec3(sx, sy, sz).scale(1 / 16.0), new Vec3(ex, ey, ez).scale(1 / 16.0));
        }

        public TankSize(Vec3 min, Vec3 max) {
            this.min = min;
            this.max = max;
        }

        public TankSize shrink(double by) {
            return shrink(by, by, by);
        }

        public TankSize shrink(double x, double y, double z) {
            return new TankSize(min.add(x, y, z), max.subtract(x, y, z));
        }

        public TankSize shink(Vec3 by) {
            return shrink(by.x, by.y, by.z);
        }

        public TankSize rotateY() {
            Vec3 _min = rotateY(min);
            Vec3 _max = rotateY(max);
            return new TankSize(VecUtil.min(_min, _max), VecUtil.max(_min, _max));
        }

        private static Vec3 rotateY(Vec3 vec) {
            return new Vec3(
                //
                1 - vec.z, //
                vec.y, //
                vec.x//
            );
        }
    }
}
