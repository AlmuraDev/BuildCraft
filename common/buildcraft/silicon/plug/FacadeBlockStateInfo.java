package buildcraft.silicon.plug;

import net.minecraft.world.level.block.TintedGlassBlock;
import net.minecraft.world.level.block.HoneyBlock;
import net.minecraft.world.level.block.SlimeBlock;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Objects;

import com.google.common.collect.ImmutableSet;

import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;

import buildcraft.api.facades.IFacadeState;

import buildcraft.lib.world.SingleBlockAccess;

public class FacadeBlockStateInfo implements IFacadeState {
    public final BlockState state;
    public final ItemStack requiredStack;
    public final ImmutableSet<Property<?>> varyingProperties;
    public final boolean isTransparent;
    public final boolean isVisible;
    public final boolean[] isSideSolid = new boolean[6];
    /** True if the block draws in the translucent layer (stained glass, ice...). 26.3 picks a block's layer from its
     * textures on the client; this is the server-safe guess that pipes use for the facade model's layer. */
    public final boolean isTranslucentLayer;

    public FacadeBlockStateInfo(BlockState state, ItemStack requiredStack,
        ImmutableSet<Property<?>> varyingProperties) {
        this.state = Objects.requireNonNull(state, "state must not be null!");
        Objects.requireNonNull(state.getBlock(), "state.getBlock must not be null!");
        Objects.requireNonNull(BuiltInRegistries.BLOCK.getKey(state.getBlock()),
            "state.getBlock.getRegistryName() must not be null!");
        this.requiredStack = requiredStack;
        this.varyingProperties = varyingProperties;
        this.isTransparent = !state.canOcclude();
        this.isVisible = !requiredStack.isEmpty();
        Block block = state.getBlock();
        this.isTranslucentLayer = block instanceof StainedGlassBlock || block instanceof StainedGlassPaneBlock
            || block instanceof IceBlock || block instanceof SlimeBlock || block instanceof HoneyBlock
            || block instanceof TintedGlassBlock;
        BlockGetter access = new SingleBlockAccess(state);
        for (Direction side : Direction.values()) {
            isSideSolid[side.ordinal()] = state.isFaceSturdy(access, BlockPos.ZERO, side);
        }
    }

    // Helper methods

    public FacadePhasedState createPhased(DyeColor activeColour) {
        return new FacadePhasedState(this, activeColour);
    }

    @Override
    public String toString() {
        return "StateInfo [id=" + System.identityHashCode(this) + ", block = " + state.getBlock() + ", state =  "
            + state.toString() + "]";
    }

    // IFacadeState

    @Override
    public BlockState getBlockState() {
        return state;
    }

    @Override
    public boolean isTransparent() {
        return isTransparent;
    }

    @Override
    public ItemStack getRequiredStack() {
        return requiredStack;
    }
}
