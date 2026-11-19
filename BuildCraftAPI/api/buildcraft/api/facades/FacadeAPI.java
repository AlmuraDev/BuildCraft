package buildcraft.api.facades;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtUtils;

import net.neoforged.fml.InterModComms;

import buildcraft.api.core.NbtStacks;

public final class FacadeAPI {
    public static final String IMC_MOD_TARGET = "buildcraftsilicon";
    public static final String IMC_FACADE_DISABLE = "facade_disable_block";
    public static final String IMC_FACADE_CUSTOM = "facade_custom_map_block_item";
    public static final String NBT_CUSTOM_BLOCK_REG_KEY = "block_registry_name";
    /** Only used on 1.12.2: 26.3 sends the whole state as {@link #NBT_CUSTOM_BLOCK_STATE}. */
    @Deprecated
    public static final String NBT_CUSTOM_BLOCK_META = "block_meta";
    public static final String NBT_CUSTOM_BLOCK_STATE = "block_state";
    public static final String NBT_CUSTOM_ITEM_STACK = "item_stack";

    public static IFacadeItem facadeItem;
    public static IFacadeRegistry registry;

    private FacadeAPI() {

    }

    public static void disableBlock(Block block) {
        InterModComms.sendTo(IMC_MOD_TARGET, IMC_FACADE_DISABLE, () -> BuiltInRegistries.BLOCK.getKey(block));
    }

    public static void mapStateToStack(BlockState state, ItemStack stack) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString(NBT_CUSTOM_BLOCK_REG_KEY, BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
        nbt.put(NBT_CUSTOM_BLOCK_STATE, NbtUtils.writeBlockState(state));
        nbt.put(NBT_CUSTOM_ITEM_STACK, NbtStacks.writeItemStack(stack));
        InterModComms.sendTo(IMC_MOD_TARGET, IMC_FACADE_CUSTOM, () -> nbt);
    }

    public static boolean isFacadeMessageId(String id) {
        return IMC_FACADE_CUSTOM.equals(id) //
            || IMC_FACADE_DISABLE.equals(id);
    }
}
