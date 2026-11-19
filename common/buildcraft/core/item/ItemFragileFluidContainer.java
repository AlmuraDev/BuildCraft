package buildcraft.core.item;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.ItemAccessFluidHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import buildcraft.api.items.IItemFluidShard;

import buildcraft.lib.fluid.BCFluid;
import buildcraft.lib.fluid.FluidManager;
import buildcraft.lib.item.ItemBC_Neptune;
import buildcraft.lib.misc.LocaleUtil;

/** A shard holding up to half a bucket of fluid, dropped by tanks and pipes that break while holding fluid. It can only
 * be drained, and breaks once empty. The fluid is the "buildcraftcore:fluid" component (1.12.2 kept it in the stack's
 * "fluid" tag). */
public class ItemFragileFluidContainer extends ItemBC_Neptune implements IItemFluidShard {

    // Half of a bucket
    public static final int MAX_FLUID_HELD = 500;

    /** Registered by core as "buildcraftcore:fluid". */
    public static final DataComponentType<SimpleFluidContent> FLUID = DataComponentType.<SimpleFluidContent> builder()
        .persistent(SimpleFluidContent.CODEC)
        .networkSynchronized(SimpleFluidContent.STREAM_CODEC)
        .build();

    public ItemFragileFluidContainer(Item.Properties properties, String id) {
        super(properties.stacksTo(1), id);
    }

    /** Lets NeoForge (and BuildCraft, through it) drain the shard. Must be on core's mod event bus. */
    public void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(Capabilities.Fluid.ITEM, (stack, access) -> new FragileFluidHandler(access), this);
    }

    @Override
    public void addSubItems(NonNullList<ItemStack> items) {
        // Never allow this to be displayed in a creative tab -- we don't want to list every single fluid...
    }

    @Override
    public Component getName(ItemStack stack) {
        FluidStack fluid = getFluid(stack);

        String localized;

        if (fluid == null) {
            localized = "ERROR! NULL FLUID!";
        } else {
            BCFluid bcFluid = FluidManager.getBCFluid(fluid.getFluid());
            if (bcFluid != null && bcFluid.isHeatable()) {
                // Add the heatable bit to the end of the name
                localized = bcFluid.getBareLocalizedName();
                String whole = LocaleUtil.localize(getDescriptionId() + ".name", localized);
                return Component.literal(LocaleUtil.localize("buildcraft.fluid.heat_" + bcFluid.getHeatValue(), whole));
            } else {
                localized = fluid.getHoverName().getString();
            }
        }
        return Component.literal(LocaleUtil.localize(getDescriptionId() + ".name", localized));
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable Level worldIn, List<String> tooltip, TooltipFlag flagIn) {
        FluidStack fluid = getFluid(stack);
        if (fluid != null && fluid.getAmount() > 0) {
            tooltip.add(LocaleUtil.localizeFluidStaticAmount(fluid.getAmount(), MAX_FLUID_HELD));
        }
    }

    @Override
    public void addFluidDrops(NonNullList<ItemStack> toDrop, FluidStack fluid) {
        if (fluid == null) {
            return;
        }
        int amount = fluid.getAmount();
        if (amount >= MAX_FLUID_HELD) {
            FluidStack fluid2 = fluid.copyWithAmount(MAX_FLUID_HELD);
            while (amount >= MAX_FLUID_HELD) {
                ItemStack stack = new ItemStack(this);
                setFluid(stack, fluid2);
                amount -= MAX_FLUID_HELD;
                toDrop.add(stack);
            }
        }
        if (amount > 0) {
            ItemStack stack = new ItemStack(this);
            setFluid(stack, fluid.copyWithAmount(amount));
            toDrop.add(stack);
        }
    }

    static void setFluid(ItemStack container, FluidStack fluid) {
        container.set(FLUID, SimpleFluidContent.copyOf(fluid));
    }

    @Nullable
    static FluidStack getFluid(ItemStack container) {
        if (container.isEmpty()) {
            return null;
        }
        SimpleFluidContent content = container.get(FLUID);
        return content == null || content.isEmpty() ? null : content.copy();
    }

    /** Drain only; the shard breaks (becomes empty) when the last of its fluid is taken. */
    public final class FragileFluidHandler extends ItemAccessFluidHandler {
        public FragileFluidHandler(ItemAccess access) {
            super(access, FLUID, MAX_FLUID_HELD);
        }

        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            return 0;
        }

        @Override
        protected ItemResource update(ItemResource accessResource, int index, FluidResource newResource, int newAmount) {
            return newAmount <= 0 ? ItemResource.EMPTY : super.update(accessResource, index, newResource, newAmount);
        }
    }
}
