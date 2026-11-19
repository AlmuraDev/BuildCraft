package buildcraft.api.transport.pluggable;

import java.util.Objects;

import net.minecraft.core.Direction;

import buildcraft.api.core.BCRenderLayer;

public abstract class PluggableModelKey {
    public final BCRenderLayer layer;
    public final Direction side;
    private final int hash;

    public PluggableModelKey(BCRenderLayer layer, Direction side) {
        if (layer != BCRenderLayer.CUTOUT && layer != BCRenderLayer.TRANSLUCENT) {
            throw new IllegalArgumentException("Can only use CUTOUT or TRANSLUCENT at the moment (was " + layer + ")");
        }
        if (side == null) throw new NullPointerException("side");
        this.layer = layer;
        this.side = side;
        this.hash = Objects.hash(layer, side);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        PluggableModelKey other = (PluggableModelKey) obj;
        if (layer != other.layer) return false;
        if (side != other.side) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
