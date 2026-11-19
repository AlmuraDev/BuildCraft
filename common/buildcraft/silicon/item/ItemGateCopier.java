package buildcraft.silicon.item;

import java.util.List;

import javax.annotation.Nonnull;

import gnu.trove.map.hash.TIntObjectHashMap;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import buildcraft.api.core.ActionResult;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;

import net.neoforged.fml.LogicalSide;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import buildcraft.lib.item.ItemBC_Neptune;
import buildcraft.lib.misc.LocaleUtil;
import buildcraft.lib.misc.NBTUtilBC;
import buildcraft.lib.misc.StackUtil;

public class ItemGateCopier extends ItemBC_Neptune {
    private static final String NBT_DATA = "gate_data";

    public ItemGateCopier(String id) {
        super(id);
        setMaxStackSize(1);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addModelVariants(TIntObjectHashMap<ModelResourceLocation> variants) {
        addVariant(variants, 0, "empty");
        addVariant(variants, 1, "full");
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, Level world, List<String> tooltip, TooltipFlag flag) {
        super.addInformation(stack, world, tooltip, flag);
        if (getMetadata(stack) != 0) {
            tooltip.add(LocaleUtil.localize("buildcraft.item.nonclean.usage"));
        }
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (world.isClientSide()) {
            return InteractionResult.PASS;
        }
        if (player.isSneaking()) {
            return clearData(StackUtil.asNonNull(stack));
        }
        return InteractionResult.PASS;
    }

    private ActionResult<ItemStack> clearData(@Nonnull ItemStack stack) {
        if (getMetadata(stack) == 0) {
            return InteractionResult.PASS;
        }
        CompoundTag nbt = NBTUtilBC.getItemData(stack);
        nbt.remove(NBT_DATA);
        if (nbt.isEmpty()) {
            stack.setTagCompound(null);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public int getMetadata(ItemStack stack) {
        return getCopiedGateData(stack) != null ? 1 : 0;
    }

    public static CompoundTag getCopiedGateData(ItemStack stack) {
        return stack.getSubCompound(NBT_DATA);
    }

    public static void setCopiedGateData(ItemStack stack, CompoundTag nbt) {
        NBTUtilBC.getItemData(stack).put(NBT_DATA, nbt);
    }
}
