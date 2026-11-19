package buildcraft.api.enums;

import java.util.Locale;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.StringRepresentable;


public enum EnumRedstoneChipset implements StringRepresentable {
    RED,
    IRON,
    GOLD,
    QUARTZ,
    DIAMOND;

    private final String name = name().toLowerCase(Locale.ROOT);

    /** @return The id of this chipset's item. 1.12.2 had one "redstone_chipset" item with the type in its damage; 26.3 has
     *         one item per type, "redstone_chipset_&lt;type&gt;". */
    public Identifier getItemId() {
        return Identifier.fromNamespaceAndPath("buildcraftsilicon", "redstone_chipset_" + name);
    }

    public ItemStack getStack(int stackSize) {
        Item chipset = BuiltInRegistries.ITEM.getValue(getItemId());
        if (chipset == null || chipset == Items.AIR) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(chipset, stackSize);
    }

    public ItemStack getStack() {
        return getStack(1);
    }

    public static EnumRedstoneChipset fromStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return RED;
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        for (EnumRedstoneChipset type : values()) {
            if (type.getItemId().equals(id)) {
                return type;
            }
        }
        return RED;
    }

    public static EnumRedstoneChipset fromOrdinal(int ordinal) {
        if (ordinal < 0 || ordinal >= values().length) {
            return RED;
        }
        return values()[ordinal];
    }

    @Override
    public String getSerializedName() {
        return getName();
    }

    public String getName() {
        return name;
    }
}
