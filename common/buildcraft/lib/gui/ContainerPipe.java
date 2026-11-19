package buildcraft.lib.gui;

import javax.annotation.Nullable;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;

import buildcraft.api.transport.pipe.IPipeHolder;

public abstract class ContainerPipe extends ContainerBC_Neptune {

    public final IPipeHolder pipeHolder;

    public ContainerPipe(@Nullable MenuType<?> type, int containerId, Player player, IPipeHolder pipeHolder) {
        super(type, containerId, player);
        this.pipeHolder = pipeHolder;
    }

    @Override
    public final boolean stillValid(Player player) {
        return pipeHolder.canPlayerInteract(player);
    }
}
