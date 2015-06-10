package buildcraft.transport.pipes;

import net.minecraft.item.Item;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import buildcraft.core.lib.RFBattery;
import buildcraft.transport.BuildCraftTransport;
import buildcraft.transport.PipeIconProvider;

public class PipePowerEmerald extends PipePowerWood {

    protected int standardIconIndex = PipeIconProvider.TYPE.PipePowerEmerald_Standard.ordinal();
    protected int solidIconIndex = PipeIconProvider.TYPE.PipeAllEmerald_Solid.ordinal();

    public PipePowerEmerald(Item item) {
        super(item);

        battery = new RFBattery(2560 * 50, 2560, 0);

        transport.initFromPipe(this.getClass());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public TextureAtlasSpriteProvider getIconProvider() {
        return BuildCraftTransport.instance.pipeIconProvider;
    }

    @Override
    public int getIconIndex(EnumFacing direction) {
        if (direction != EnumFacing.UNKNOWN && powerSources[direction.ordinal()]) {
            return solidIconIndex;
        } else {
            return standardIconIndex;
        }
    }
}