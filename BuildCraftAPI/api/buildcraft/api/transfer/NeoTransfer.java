package buildcraft.api.transfer;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** Adapters between BuildCraft's simulate-flag handlers ({@link IItemHandler}, {@link IFluidHandler},
 * {@link IEnergyStorage}) and NeoForge's transaction based ones.
 * <p>
 * BuildCraft to NeoForge: an {@link IItemHandlerModifiable} is changed for real inside the transaction and put back if the
 * transaction is aborted, which is exact. Anything else can only be asked with a simulate flag, so the change is checked
 * by simulating it and only made when the outermost transaction commits; amounts already promised in the same
 * transaction are counted, so the promise holds unless the handler changes in between.
 * <p>
 * NeoForge to BuildCraft: a simulated call runs in a transaction that's aborted, a real one in a transaction that's
 * committed. */
public final class NeoTransfer {
    private NeoTransfer() {}

    // ################
    //
    // NeoForge -> BuildCraft
    //
    // ################

    /** @return A BuildCraft view of the given NeoForge item handler, or null if it's null. */
    @Nullable
    public static IItemHandlerModifiable fromNeoItems(@Nullable ResourceHandler<ItemResource> handler) {
        if (handler == null) {
            return null;
        }
        if (handler instanceof ItemsToNeo wrapper) {
            if (wrapper.handler instanceof IItemHandlerModifiable modifiable) {
                return modifiable;
            }
        }
        return new ItemsFromNeo(handler);
    }

    /** @return A BuildCraft view of the given NeoForge fluid handler, or null if it's null. */
    @Nullable
    public static IFluidHandler fromNeoFluids(@Nullable ResourceHandler<FluidResource> handler) {
        if (handler == null) {
            return null;
        }
        if (handler instanceof FluidsToNeo wrapper) {
            return wrapper.handler;
        }
        return new FluidsFromNeo(handler);
    }

    /** @return A BuildCraft view of the given NeoForge energy handler, or null if it's null. */
    @Nullable
    public static IEnergyStorage fromNeoEnergy(@Nullable EnergyHandler handler) {
        if (handler == null) {
            return null;
        }
        if (handler instanceof EnergyToNeo wrapper) {
            return wrapper.storage;
        }
        return new EnergyFromNeo(handler);
    }

    /** @return A fluid handler for the given (single) fluid container stack, whose {@link IFluidHandlerItem#getContainer()}
     *         is the container after filling or draining; or null if the stack can't hold fluid. The given stack itself
     *         isn't changed. */
    @Nullable
    public static IFluidHandlerItem fluidHandlerOf(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        ItemAccess access = ItemAccess.forStack(stack.copy());
        ResourceHandler<FluidResource> handler = stack.getCapability(Capabilities.Fluid.ITEM, access);
        if (handler == null) {
            return null;
        }
        IFluidHandler fluids = fromNeoFluids(handler);
        return new IFluidHandlerItem() {
            @Override
            public IFluidTankProperties[] getTankProperties() {
                return fluids.getTankProperties();
            }

            @Override
            public int fill(FluidStack resource, boolean doFill) {
                return fluids.fill(resource, doFill);
            }

            @Nullable
            @Override
            public FluidStack drain(FluidStack resource, boolean doDrain) {
                return fluids.drain(resource, doDrain);
            }

            @Nullable
            @Override
            public FluidStack drain(int maxDrain, boolean doDrain) {
                return fluids.drain(maxDrain, doDrain);
            }

            @Nonnull
            @Override
            public ItemStack getContainer() {
                ItemResource resource = access.getResource();
                return resource.isEmpty() ? ItemStack.EMPTY : resource.toStack(access.getAmount());
            }
        };
    }

    static final class ItemsFromNeo implements IItemHandlerModifiable {
        final ResourceHandler<ItemResource> handler;

        ItemsFromNeo(ResourceHandler<ItemResource> handler) {
            this.handler = handler;
        }

        @Override
        public int getSlots() {
            return handler.size();
        }

        @Nonnull
        @Override
        public ItemStack getStackInSlot(int slot) {
            ItemResource resource = handler.getResource(slot);
            if (resource.isEmpty()) {
                return ItemStack.EMPTY;
            }
            return resource.toStack(handler.getAmountAsInt(slot));
        }

        @Nonnull
        @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
            int inserted;
            try (Transaction tx = Transaction.openRoot()) {
                inserted = handler.insert(slot, ItemResource.of(stack), stack.getCount(), tx);
                if (!simulate) {
                    tx.commit();
                }
            }
            if (inserted <= 0) {
                return stack;
            }
            return inserted >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - inserted);
        }

        @Nonnull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (amount <= 0) {
                return ItemStack.EMPTY;
            }
            ItemResource resource = handler.getResource(slot);
            if (resource.isEmpty()) {
                return ItemStack.EMPTY;
            }
            int extracted;
            try (Transaction tx = Transaction.openRoot()) {
                extracted = handler.extract(slot, resource, amount, tx);
                if (!simulate) {
                    tx.commit();
                }
            }
            return extracted <= 0 ? ItemStack.EMPTY : resource.toStack(extracted);
        }

        @Override
        public int getSlotLimit(int slot) {
            return handler.getCapacityAsInt(slot, handler.getResource(slot));
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return handler.isValid(slot, ItemResource.of(stack));
        }

        /** NeoForge handlers can't be set directly: empties the slot and inserts the stack instead. */
        @Override
        public void setStackInSlot(int slot, @Nonnull ItemStack stack) {
            try (Transaction tx = Transaction.openRoot()) {
                ItemResource current = handler.getResource(slot);
                if (!current.isEmpty()) {
                    handler.extract(slot, current, handler.getAmountAsInt(slot), tx);
                }
                if (!stack.isEmpty()) {
                    handler.insert(slot, ItemResource.of(stack), stack.getCount(), tx);
                }
                tx.commit();
            }
        }
    }

    static final class FluidsFromNeo implements IFluidHandler {
        final ResourceHandler<FluidResource> handler;

        FluidsFromNeo(ResourceHandler<FluidResource> handler) {
            this.handler = handler;
        }

        @Override
        public IFluidTankProperties[] getTankProperties() {
            int size = handler.size();
            IFluidTankProperties[] props = new IFluidTankProperties[size];
            for (int i = 0; i < size; i++) {
                FluidResource resource = handler.getResource(i);
                FluidStack contents = resource.isEmpty() ? null : resource.toStack(handler.getAmountAsInt(i));
                props[i] = new FluidTankProperties(contents, handler.getCapacityAsInt(i, resource));
            }
            return props;
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            if (resource == null || resource.isEmpty()) {
                return 0;
            }
            try (Transaction tx = Transaction.openRoot()) {
                int filled = handler.insert(FluidResource.of(resource), resource.getAmount(), tx);
                if (doFill) {
                    tx.commit();
                }
                return filled;
            }
        }

        @Nullable
        @Override
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            if (resource == null || resource.isEmpty()) {
                return null;
            }
            FluidResource fluid = FluidResource.of(resource);
            try (Transaction tx = Transaction.openRoot()) {
                int drained = handler.extract(fluid, resource.getAmount(), tx);
                if (doDrain) {
                    tx.commit();
                }
                return drained <= 0 ? null : fluid.toStack(drained);
            }
        }

        @Nullable
        @Override
        public FluidStack drain(int maxDrain, boolean doDrain) {
            if (maxDrain <= 0) {
                return null;
            }
            for (int i = 0; i < handler.size(); i++) {
                FluidResource fluid = handler.getResource(i);
                if (fluid.isEmpty() || handler.getAmountAsInt(i) <= 0) {
                    continue;
                }
                try (Transaction tx = Transaction.openRoot()) {
                    int drained = handler.extract(fluid, maxDrain, tx);
                    if (drained > 0) {
                        if (doDrain) {
                            tx.commit();
                        }
                        return fluid.toStack(drained);
                    }
                }
            }
            return null;
        }
    }

    static final class EnergyFromNeo implements IEnergyStorage {
        final EnergyHandler handler;

        EnergyFromNeo(EnergyHandler handler) {
            this.handler = handler;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (maxReceive <= 0) {
                return 0;
            }
            try (Transaction tx = Transaction.openRoot()) {
                int received = handler.insert(maxReceive, tx);
                if (!simulate) {
                    tx.commit();
                }
                return received;
            }
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            if (maxExtract <= 0) {
                return 0;
            }
            try (Transaction tx = Transaction.openRoot()) {
                int extracted = handler.extract(maxExtract, tx);
                if (!simulate) {
                    tx.commit();
                }
                return extracted;
            }
        }

        @Override
        public int getEnergyStored() {
            return handler.getAmountAsInt();
        }

        @Override
        public int getMaxEnergyStored() {
            return handler.getCapacityAsInt();
        }

        @Override
        public boolean canExtract() {
            return extractEnergy(1, true) > 0 || getEnergyStored() == 0;
        }

        @Override
        public boolean canReceive() {
            return receiveEnergy(1, true) > 0 || getEnergyStored() >= getMaxEnergyStored();
        }
    }

    // ################
    //
    // BuildCraft -> NeoForge
    //
    // ################

    /** @return A NeoForge view of the given BuildCraft item handler, or null if it's null. */
    @Nullable
    public static ResourceHandler<ItemResource> toNeo(@Nullable IItemHandler handler) {
        if (handler == null) {
            return null;
        }
        if (handler instanceof ItemsFromNeo wrapper) {
            return wrapper.handler;
        }
        return new ItemsToNeo(handler);
    }

    /** @return A NeoForge view of the given BuildCraft fluid handler, or null if it's null. */
    @Nullable
    public static ResourceHandler<FluidResource> toNeo(@Nullable IFluidHandler handler) {
        if (handler == null) {
            return null;
        }
        if (handler instanceof FluidsFromNeo wrapper) {
            return wrapper.handler;
        }
        return new FluidsToNeo(handler);
    }

    /** @return A NeoForge view of the given BuildCraft energy storage, or null if it's null. */
    @Nullable
    public static EnergyHandler toNeo(@Nullable IEnergyStorage storage) {
        if (storage == null) {
            return null;
        }
        if (storage instanceof EnergyFromNeo wrapper) {
            return wrapper.handler;
        }
        return new EnergyToNeo(storage);
    }

    /** Changes made by simulating them first and then only on the outermost commit. Each entry also records an amount,
     * so later calls in the same transaction can take what's already been promised into account. */
    static final class DeferredJournal extends SnapshotJournal<Integer> {
        record Pending(Object key, int amount, Runnable action) {}

        final List<Pending> pending = new ArrayList<>();

        void add(TransactionContext tx, Object key, int amount, Runnable action) {
            updateSnapshots(tx);
            pending.add(new Pending(key, amount, action));
        }

        int promised(Object key) {
            int total = 0;
            for (Pending p : pending) {
                if (p.key.equals(key)) {
                    total += p.amount;
                }
            }
            return total;
        }

        @Override
        protected Integer createSnapshot() {
            return pending.size();
        }

        @Override
        protected void revertToSnapshot(Integer snapshot) {
            while (pending.size() > snapshot) {
                pending.remove(pending.size() - 1);
            }
        }

        @Override
        protected void onRootCommit(Integer originalState) {
            List<Pending> actions = new ArrayList<>(pending);
            pending.clear();
            for (Pending p : actions) {
                p.action.run();
            }
        }
    }

    static final class ItemsToNeo implements ResourceHandler<ItemResource> {
        final IItemHandler handler;
        /** Only used for handlers that aren't {@link IItemHandlerModifiable}. */
        final DeferredJournal deferred = new DeferredJournal();
        final List<SlotJournal> slots = new ArrayList<>();

        ItemsToNeo(IItemHandler handler) {
            this.handler = handler;
        }

        /** Puts a modifiable handler's slot back the way it was when a transaction is aborted. */
        final class SlotJournal extends SnapshotJournal<ItemStack> {
            final int slot;

            SlotJournal(int slot) {
                this.slot = slot;
            }

            @Override
            protected ItemStack createSnapshot() {
                return handler.getStackInSlot(slot).copy();
            }

            @Override
            protected void revertToSnapshot(ItemStack snapshot) {
                ((IItemHandlerModifiable) handler).setStackInSlot(slot, snapshot);
            }
        }

        SlotJournal slot(int index) {
            while (slots.size() <= index) {
                slots.add(new SlotJournal(slots.size()));
            }
            return slots.get(index);
        }

        @Override
        public int size() {
            return handler.getSlots();
        }

        @Override
        public ItemResource getResource(int index) {
            return ItemResource.of(handler.getStackInSlot(index));
        }

        @Override
        public long getAmountAsLong(int index) {
            return handler.getStackInSlot(index).getCount();
        }

        @Override
        public long getCapacityAsLong(int index, ItemResource resource) {
            int limit = handler.getSlotLimit(index);
            return resource.isEmpty() ? limit : Math.min(limit, resource.getMaxStackSize());
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            return handler.isItemValid(index, resource.toStack());
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            if (resource.isEmpty() || amount <= 0) {
                return 0;
            }
            ItemStack stack = resource.toStack(amount);
            if (handler instanceof IItemHandlerModifiable) {
                slot(index).updateSnapshots(transaction);
                ItemStack left = handler.insertItem(index, stack, false);
                return amount - left.getCount();
            }
            Object key = index;
            int promised = deferred.promised(key);
            ItemStack left = handler.insertItem(index, resource.toStack(amount + promised), true);
            int inserted = Math.max(0, Math.min(amount, amount + promised - left.getCount()));
            if (inserted > 0) {
                deferred.add(transaction, key, inserted, () -> handler.insertItem(index, resource.toStack(inserted), false));
            }
            return inserted;
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            if (resource.isEmpty() || amount <= 0) {
                return 0;
            }
            ItemStack current = handler.getStackInSlot(index);
            if (!resource.matches(current)) {
                return 0;
            }
            if (handler instanceof IItemHandlerModifiable) {
                slot(index).updateSnapshots(transaction);
                return handler.extractItem(index, amount, false).getCount();
            }
            Object key = -1 - index;
            int promised = deferred.promised(key);
            int extracted = Math.max(0, Math.min(amount, handler.extractItem(index, amount + promised, true).getCount() - promised));
            if (extracted > 0) {
                deferred.add(transaction, key, extracted, () -> handler.extractItem(index, extracted, false));
            }
            return extracted;
        }
    }

    static final class FluidsToNeo implements ResourceHandler<FluidResource> {
        final IFluidHandler handler;
        final DeferredJournal deferred = new DeferredJournal();

        FluidsToNeo(IFluidHandler handler) {
            this.handler = handler;
        }

        private IFluidTankProperties props(int index) {
            IFluidTankProperties[] all = handler.getTankProperties();
            return all == null || index >= all.length ? null : all[index];
        }

        @Override
        public int size() {
            IFluidTankProperties[] all = handler.getTankProperties();
            return all == null ? 0 : all.length;
        }

        @Override
        public FluidResource getResource(int index) {
            IFluidTankProperties p = props(index);
            FluidStack contents = p == null ? null : p.getContents();
            return contents == null ? FluidResource.EMPTY : FluidResource.of(contents);
        }

        @Override
        public long getAmountAsLong(int index) {
            IFluidTankProperties p = props(index);
            FluidStack contents = p == null ? null : p.getContents();
            return contents == null ? 0 : contents.getAmount();
        }

        @Override
        public long getCapacityAsLong(int index, FluidResource resource) {
            IFluidTankProperties p = props(index);
            return p == null ? 0 : p.getCapacity();
        }

        @Override
        public boolean isValid(int index, FluidResource resource) {
            IFluidTankProperties p = props(index);
            return p != null && (resource.isEmpty() || p.canFillFluidType(resource.toStack(1)));
        }

        /** BuildCraft fluid handlers fill and drain as a whole, not per tank: everything goes through tank 0. */
        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            return index == 0 ? insert(resource, amount, transaction) : 0;
        }

        @Override
        public int insert(FluidResource resource, int amount, TransactionContext transaction) {
            if (resource.isEmpty() || amount <= 0) {
                return 0;
            }
            Object key = List.of("fill", resource);
            int promised = deferred.promised(key);
            int filled = handler.fill(resource.toStack(amount + promised), false) - promised;
            filled = Math.max(0, Math.min(amount, filled));
            if (filled > 0) {
                int toFill = filled;
                deferred.add(transaction, key, toFill, () -> handler.fill(resource.toStack(toFill), true));
            }
            return filled;
        }

        @Override
        public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
            return index == 0 ? extract(resource, amount, transaction) : 0;
        }

        @Override
        public int extract(FluidResource resource, int amount, TransactionContext transaction) {
            if (resource.isEmpty() || amount <= 0) {
                return 0;
            }
            Object key = List.of("drain", resource);
            int promised = deferred.promised(key);
            FluidStack drained = handler.drain(resource.toStack(amount + promised), false);
            int got = drained == null ? 0 : drained.getAmount() - promised;
            got = Math.max(0, Math.min(amount, got));
            if (got > 0) {
                int toDrain = got;
                deferred.add(transaction, key, toDrain, () -> handler.drain(resource.toStack(toDrain), true));
            }
            return got;
        }
    }

    static final class EnergyToNeo implements EnergyHandler {
        final IEnergyStorage storage;
        final DeferredJournal deferred = new DeferredJournal();

        EnergyToNeo(IEnergyStorage storage) {
            this.storage = storage;
        }

        @Override
        public long getAmountAsLong() {
            return storage.getEnergyStored();
        }

        @Override
        public long getCapacityAsLong() {
            return storage.getMaxEnergyStored();
        }

        @Override
        public int insert(int amount, TransactionContext transaction) {
            if (amount <= 0 || !storage.canReceive()) {
                return 0;
            }
            int promised = deferred.promised("in");
            int received = Math.max(0, Math.min(amount, storage.receiveEnergy(amount + promised, true) - promised));
            if (received > 0) {
                deferred.add(transaction, "in", received, () -> storage.receiveEnergy(received, false));
            }
            return received;
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            if (amount <= 0 || !storage.canExtract()) {
                return 0;
            }
            int promised = deferred.promised("out");
            int extracted = Math.max(0, Math.min(amount, storage.extractEnergy(amount + promised, true) - promised));
            if (extracted > 0) {
                deferred.add(transaction, "out", extracted, () -> storage.extractEnergy(extracted, false));
            }
            return extracted;
        }
    }
}
