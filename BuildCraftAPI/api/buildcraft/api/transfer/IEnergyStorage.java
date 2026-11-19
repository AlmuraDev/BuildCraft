package buildcraft.api.transfer;

/** RF-style energy storage with a simulate flag, like Forge 1.12's IEnergyStorage. BuildCraft's RF support uses this;
 * {@link NeoTransfer} turns it into NeoForge's energy handler and back. */
public interface IEnergyStorage {

    /** @return How much energy was (or would have been, if simulated) accepted. */
    int receiveEnergy(int maxReceive, boolean simulate);

    /** @return How much energy was (or would have been, if simulated) extracted. */
    int extractEnergy(int maxExtract, boolean simulate);

    int getEnergyStored();

    int getMaxEnergyStored();

    boolean canExtract();

    boolean canReceive();
}
