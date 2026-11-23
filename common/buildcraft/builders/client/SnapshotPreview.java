/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.common.NeoForge;

import com.mojang.datafixers.util.Either;

import buildcraft.api.schematics.ISchematicBlock;

import buildcraft.builders.BCBuildersItems;
import buildcraft.builders.item.ItemSchematicSingle;
import buildcraft.builders.snapshot.Blueprint;
import buildcraft.builders.snapshot.ClientSnapshots;
import buildcraft.builders.snapshot.SchematicBlockDefault;
import buildcraft.builders.snapshot.SchematicBlockFluid;
import buildcraft.builders.snapshot.Snapshot;
import buildcraft.builders.snapshot.Template;

/** Draws a slowly turning 3D view of a snapshot into a GUI rectangle: the replacer's preview and the preview under
 * blueprint, template and single schematic tooltips. 1.12.2 built a fake world of the snapshot and drew it with a
 * perspective projection; 26.3 GUIs draw 3D through picture-in-picture renderers, so this bakes each snapshot's
 * visible blocks into block model states once and submits those into an orthographic view. Block entity renderers
 * and entities are not drawn (1.12.2 drew them through the fake world). */
public final class SnapshotPreview {
    private SnapshotPreview() {}

    /** Snapshots with more blocks than this are not drawn (1.12.2 had no limit, but rebuilt nothing per frame). */
    private static final int MAX_VOLUME = 128 * 128 * 128;
    private static final BlockDisplayContext DISPLAY_CONTEXT = BlockDisplayContext.create();

    private static final Map<Snapshot.Key, Baked> BAKED = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Snapshot.Key, Baked> eldest) {
            return size() > 16;
        }
    };

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterPictureInPictureRenderersEvent event) -> event.register(State.class,
            Renderer::new));
        modBus.addListener((RegisterClientTooltipComponentFactoriesEvent event) -> event.register(TooltipPreview.class,
            TooltipPreview.Client::new));
        modBus.addListener((ModelEvent.BakingCompleted event) -> BAKED.clear());
        NeoForge.EVENT_BUS.addListener(SnapshotPreview::gatherTooltip);
    }

    /** Draws the snapshot in the given header (asking the server for it if needed) into the rectangle. */
    public static void draw(GuiGraphicsExtractor graphics, @Nullable Snapshot.Header header, int x, int y, int w,
        int h) {
        if (header == null) return;
        Snapshot snapshot = ClientSnapshots.INSTANCE.getSnapshot(header.key);
        if (snapshot != null) draw(graphics, snapshot, x, y, w, h);
    }

    public static void draw(GuiGraphicsExtractor graphics, Snapshot snapshot, int x, int y, int w, int h) {
        Baked baked = bake(snapshot);
        if (baked == null || baked.blocks.isEmpty()) return;
        graphics.submitPictureInPictureRenderState(new State(baked, x, y, x + w, y + h,
            graphics.peekScissorStack()));
    }

    @Nullable
    private static Baked bake(Snapshot snapshot) {
        Baked baked = BAKED.get(snapshot.key);
        if (baked != null) return baked;
        BlockPos size = snapshot.size;
        if (size == null || (long) size.getX() * size.getY() * size.getZ() > MAX_VOLUME) return null;

        BlockState[] states = new BlockState[snapshot.getDataSize()];
        for (int i = 0; i < states.length; i++) {
            states[i] = stateAt(snapshot, i);
        }
        BlockModelResolver resolver = new BlockModelResolver(Minecraft.getInstance().getModelManager());
        List<Baked.Block> blocks = new ArrayList<>();
        for (int z = 0; z < size.getZ(); z++) {
            for (int y = 0; y < size.getY(); y++) {
                for (int x = 0; x < size.getX(); x++) {
                    BlockState state = states[snapshot.posToIndex(x, y, z)];
                    if (state == null || state.getRenderShape() == RenderShape.INVISIBLE) continue;
                    if (isHidden(snapshot, states, x, y, z)) continue;
                    BlockModelRenderState renderState = new BlockModelRenderState();
                    resolver.update(renderState, state, DISPLAY_CONTEXT);
                    if (!renderState.isEmpty()) {
                        blocks.add(new Baked.Block(x, y, z, renderState));
                    }
                }
            }
        }
        baked = new Baked(size, blocks);
        BAKED.put(snapshot.key, baked);
        return baked;
    }

    @Nullable
    private static BlockState stateAt(Snapshot snapshot, int index) {
        if (snapshot instanceof Blueprint blueprint) {
            ISchematicBlock block = blueprint.palette.get(blueprint.data[index]);
            if (block.isAir()) return null;
            if (block instanceof SchematicBlockDefault def) return def.getPreviewState();
            if (block instanceof SchematicBlockFluid fluid) return fluid.getPreviewState();
            return null;
        }
        if (snapshot instanceof Template template) {
            return template.data.get(index) ? Blocks.QUARTZ_BLOCK.defaultBlockState() : null;
        }
        return null;
    }

    /** True if all six neighbours are full opaque blocks inside the snapshot, so nothing of this block shows. */
    private static boolean isHidden(Snapshot snapshot, BlockState[] states, int x, int y, int z) {
        BlockPos size = snapshot.size;
        int[][] offsets = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 }, { 0, 0, -1 } };
        for (int[] o : offsets) {
            int nx = x + o[0], ny = y + o[1], nz = z + o[2];
            if (nx < 0 || ny < 0 || nz < 0 || nx >= size.getX() || ny >= size.getY() || nz >= size.getZ()) {
                return false;
            }
            BlockState n = states[snapshot.posToIndex(nx, ny, nz)];
            if (n == null || !n.canOcclude()) return false;
        }
        return true;
    }

    private record Baked(BlockPos size, List<Block> blocks) {
        record Block(int x, int y, int z, BlockModelRenderState state) {}
    }

    public record State(Baked baked, int x0, int y0, int x1, int y1, @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds) implements PictureInPictureRenderState {
        State(Baked baked, int x0, int y0, int x1, int y1, @Nullable ScreenRectangle scissorArea) {
            this(baked, x0, y0, x1, y1, scissorArea,
                PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea));
        }

        @Override
        public float scale() {
            return 1;
        }
    }

    static class Renderer extends PictureInPictureRenderer<State> {
        @Override
        public Class<State> getRenderStateClass() {
            return State.class;
        }

        @Override
        protected void renderToTexture(State state, PoseStack poseStack, SubmitNodeCollector collector) {
            Minecraft.getInstance().gameRenderer.lighting().setupFor(Lighting.Entry.ITEMS_3D);
            BlockPos size = state.baked.size;
            int w = state.x1 - state.x0, h = state.y1 - state.y0;
            // The bounding sphere of the snapshot, so it fits at every angle
            double diagonal = Math.sqrt(
                size.getX() * size.getX() + size.getY() * size.getY() + size.getZ() * size.getZ());
            float fit = (float) (Math.min(w, h) / Math.max(diagonal, 1));
            poseStack.scale(fit, -fit, -fit);
            poseStack.rotateDegrees(Axis.XP, 20);
            poseStack.rotateDegrees(Axis.YP, (System.currentTimeMillis() % 36000) / 100F);
            poseStack.translate(-size.getX() / 2F, -size.getY() / 2F, -size.getZ() / 2F);
            for (Baked.Block block : state.baked.blocks) {
                poseStack.pushPose();
                poseStack.translate(block.x, block.y, block.z);
                block.state.submitMultiLayer(poseStack, collector, 0xF000F0, OverlayTexture.NO_OVERLAY, 0);
                poseStack.popPose();
            }
        }

        @Override
        protected float getTranslateY(int height, int guiScale) {
            return height / 2.0F;
        }

        @Override
        protected String getTextureLabel() {
            return "buildcraft snapshot preview";
        }
    }

    // Tooltips

    private static void gatherTooltip(RenderTooltipEvent.GatherComponents event) {
        Snapshot snapshot = snapshotFor(event.getItemStack());
        if (snapshot != null) {
            event.getTooltipElements().add(Either.right(new TooltipPreview(snapshot)));
        }
    }

    @Nullable
    private static Snapshot snapshotFor(ItemStack stack) {
        if (BCBuildersItems.snapshot != null && stack.getItem() == BCBuildersItems.snapshot) {
            Snapshot.Header header = BCBuildersItems.snapshot.getHeader(stack);
            return header == null ? null : ClientSnapshots.INSTANCE.getSnapshot(header.key);
        }
        if (BCBuildersItems.schematicSingle != null && stack.getItem() == BCBuildersItems.schematicSingle) {
            ISchematicBlock schematicBlock = ItemSchematicSingle.getSchematicSafe(stack);
            if (schematicBlock == null) return null;
            Blueprint blueprint = new Blueprint();
            blueprint.size = new BlockPos(1, 1, 1);
            blueprint.offset = BlockPos.ZERO;
            blueprint.data = new int[] { 0 };
            blueprint.palette.add(schematicBlock);
            blueprint.computeKey();
            return blueprint;
        }
        return null;
    }

    record TooltipPreview(Snapshot snapshot) implements TooltipComponent {
        static class Client implements ClientTooltipComponent {
            private static final int SIZE = 100;
            private final Snapshot snapshot;

            Client(TooltipPreview preview) {
                this.snapshot = preview.snapshot;
            }

            @Override
            public int getHeight(Font font) {
                return SIZE + 4;
            }

            @Override
            public int getWidth(Font font) {
                return SIZE;
            }

            @Override
            public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor graphics) {
                draw(graphics, snapshot, x, y + 4, SIZE, SIZE);
            }
        }
    }
}
