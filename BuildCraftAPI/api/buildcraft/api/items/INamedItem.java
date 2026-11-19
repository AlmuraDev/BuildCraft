package buildcraft.api.items;

import javax.annotation.Nonnull;

import net.minecraft.world.item.ItemStack;

/** An item whose stacks carry a player-given label. (26.3's Item already has a getName(ItemStack) returning the display
 * name, so the 1.12.2 getName/setName became getLabel/setLabel.) */
public interface INamedItem {
    String getLabel(@Nonnull ItemStack stack);

    boolean setLabel(@Nonnull ItemStack stack, String name);
}
