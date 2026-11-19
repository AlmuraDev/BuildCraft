package buildcraft.transport.client.model.key;

import buildcraft.api.core.BCRenderLayer;
import net.minecraft.core.Direction;

import buildcraft.api.transport.pluggable.PluggableModelKey;

public class KeyPlugPowerAdaptor extends PluggableModelKey {
    public KeyPlugPowerAdaptor(Direction side) {
        super(BCRenderLayer.CUTOUT, side);
    }
}
