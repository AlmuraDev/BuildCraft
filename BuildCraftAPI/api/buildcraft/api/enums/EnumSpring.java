package buildcraft.api.enums;

import java.util.Locale;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.util.StringRepresentable;

import buildcraft.api.properties.BuildCraftProperties;

public enum EnumSpring implements StringRepresentable {
    WATER(5, -1, Blocks.WATER.defaultBlockState()),
    OIL(6000, 8, null); // Set in BuildCraftEnergy

    public static final EnumSpring[] VALUES = values();

    public final int tickRate, chance;
    public BlockState liquidBlock;
    public boolean canGen = true;
    /** The tile type springs of this type create, or null for none. Set by the module that adds the tile (energy, for
     * oil springs). */
    public BlockEntityType<?> tileType;

    private final String lowerCaseName = name().toLowerCase(Locale.ROOT);

    EnumSpring(int tickRate, int chance, BlockState liquidBlock) {
        this.tickRate = tickRate;
        this.chance = chance;
        this.liquidBlock = liquidBlock;
    }

    public static EnumSpring fromState(BlockState state) {
        return state.getValue(BuildCraftProperties.SPRING_TYPE);
    }

    @Override
    public String getSerializedName() {
        return getName();
    }

    public String getName() {
        return lowerCaseName;
    }
}
