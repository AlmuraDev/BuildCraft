package buildcraft.core.block;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;

import buildcraft.lib.block.BlockBCTile_Neptune;

import buildcraft.core.tile.TilePowerConsumerTester;

public class BlockPowerConsumerTester extends BlockBCTile_Neptune {

    public BlockPowerConsumerTester(BlockBehaviour.Properties properties, String id) {
        super(properties, id);
    }
}
