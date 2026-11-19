package buildcraft.api.core;

import net.minecraft.nbt.Tag;

/** Something that saves to and loads from NBT, like Forge 1.12's INBTSerializable. BuildCraft keeps its saved data as
 * NBT (laid out as on 1.12.2, so old worlds convert cleanly); tiles bridge it to 26.3's value input/output. */
public interface INBTSerializable<T extends Tag> {
    T serializeNBT();

    void deserializeNBT(T nbt);
}
