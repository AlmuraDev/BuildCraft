package buildcraft.core.item;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.world.item.TooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.level.Level;

import net.neoforged.neoforge.capabilities.BlockCapability;
import buildcraft.lib.misc.FluidUtilBC;
import buildcraft.api.core.ICapabilityProvider;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import buildcraft.api.transfer.FluidTankProperties;
import buildcraft.api.transfer.IFluidHandlerItem;
import buildcraft.api.transfer.IFluidTankProperties;
import net.neoforged.fml.LogicalSide;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import buildcraft.api.items.IItemFluidShard;

import buildcraft.lib.fluid.BCFluid;
import buildcraft.lib.item.ItemBC_Neptune;
import buildcraft.lib.misc.LocaleUtil;
import buildcraft.lib.misc.NBTUtilBC;
import buildcraft.lib.misc.StackUtil;

public class ItemFragileFluidContainer extends ItemBC_Neptune implements IItemFluidShard {

    // Half of a bucket
    public static final int MAX_FLUID_HELD = 500;

    public ItemFragileFluidContainer(String id) {
        super(id);
        setMaxStackSize(1);
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, CompoundTag nbt) {
        return new FragileFluidHandler(stack);
    }

    @Override
    protected void addSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        // Never allow this to be displayed in a creative tab -- we don't want to list every single fluid...
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        FluidStack fluid = getFluid(stack);

        String localized;

        if (fluid == null) {
            localized = "ERROR! NULL FLUID!";
        } else if (fluid.getFluid() instanceof BCFluid) {
            BCFluid bcFluid = (BCFluid) fluid.getFluid();
            if (bcFluid.isHeatable()) {
                // Add the heatable bit to the end of the name
                localized = bcFluid.getBareLocalizedName(fluid);
                String whole = LocaleUtil.localize(getDescriptionId() + ".name", localized);
                return LocaleUtil.localize("buildcraft.fluid.heat_" + bcFluid.getHeatValue(), whole);
            } else {
                localized = fluid.getHoverName();
            }
        } else {
            localized = fluid.getHoverName();
        }
        return LocaleUtil.localize(getDescriptionId() + ".name", localized);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable Level worldIn, List<String> tooltip, TooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        CompoundTag fluidTag = stack.getSubCompound("fluid");
        if (fluidTag != null) {
            FluidStack fluid = FluidStack.loadFluidStackFromNBT(fluidTag);
            if (fluid != null && fluid.getAmount() > 0) {
                tooltip.add(LocaleUtil.localizeFluidStaticAmount(fluid.getAmount(), MAX_FLUID_HELD));
            }
        }
    }

    @Override
    public void addFluidDrops(NonNullList<ItemStack> toDrop, FluidStack fluid) {
        if (fluid == null) {
            return;
        }
        int amount = fluid.getAmount();
        if (amount >= MAX_FLUID_HELD) {
            FluidStack fluid2 = fluid.copy();
            fluid2.setAmount(MAX_FLUID_HELD);
            while (amount >= MAX_FLUID_HELD) {
                ItemStack stack = new ItemStack(this);
                setFluid(stack, fluid2);
                amount -= MAX_FLUID_HELD;
                toDrop.add(stack);
            }
        }
        if (amount > 0) {
            ItemStack stack = new ItemStack(this);
            setFluid(stack, (fluid).copyWithAmount(amount));
            toDrop.add(stack);
        }
    }

    static void setFluid(ItemStack container, FluidStack fluid) {
        CompoundTag nbt = NBTUtilBC.getItemData(container);
        nbt.put("fluid", fluid.writeToNBT(new CompoundTag()));
    }

    @Nullable
    static FluidStack getFluid(ItemStack container) {
        if (container.isEmpty()) {
            return null;
        }
        CompoundTag fluidNbt = container.getSubCompound("fluid");
        if (fluidNbt == null) {
            return null;
        }
        return FluidStack.loadFluidStackFromNBT(fluidNbt);
    }

    public class FragileFluidHandler implements IFluidHandlerItem, ICapabilityProvider {

        @Nonnull
        private ItemStack container;

        public FragileFluidHandler(@Nonnull ItemStack container) {
            this.container = container;
        }

        @Override
        public boolean hasCapability(BlockCapability<?, Direction> capability, Direction facing) {
            return getCapability(capability, facing) != null;
        }

        @Override
        public <T> T getCapability(BlockCapability<T, Direction> capability, Direction facing) {
            if (capability == CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY
                || capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
                return (T) this;
            }
            return null;
        }

        @Override
        public IFluidTankProperties[] getTankProperties() {
            return new IFluidTankProperties[] {
                new FluidTankProperties(getFluid(container), MAX_FLUID_HELD, false, true) };
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            FluidStack fluid = ItemFragileFluidContainer.getFluid(container);
            if (fluid == null || resource == null) {
                return null;
            }
            if (!FluidUtilBC.isFluidEqual(fluid, resource)) {
                return null;
            }
            return drain(resource.getAmount(), doDrain);
        }

        @Override
        public FluidStack drain(int maxDrain, boolean doDrain) {
            FluidStack fluid = ItemFragileFluidContainer.getFluid(container);
            if (fluid == null || maxDrain <= 0) {
                return null;
            }
            int toDrain = Math.min(maxDrain, fluid.getAmount());
            FluidStack f = (fluid).copyWithAmount(toDrain);
            if (doDrain) {
                fluid.shrink(toDrain);
                if (fluid.getAmount() <= 0) {
                    fluid = null;
                    container = StackUtil.EMPTY;
                } else {
                    setFluid(container, fluid);
                }
            }
            return f;
        }

        @Override
        public ItemStack getContainer() {
            return container;
        }
    }
}
