package buildcraft.api.items;

import java.util.List;
import java.util.Locale;

import javax.annotation.Nonnull;

import com.mojang.serialization.Codec;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import buildcraft.api.core.IBox;
import buildcraft.api.core.IZone;

/** Created by asie on 2/28/15. */
public interface IMapLocation extends INamedItem {
    /** What a map location holds. Stored on the stack as {@link #COMPONENT} (1.12.2 used the item damage). */
    enum MapLocationType implements StringRepresentable {
        CLEAN,
        SPOT,
        AREA,
        PATH,
        ZONE,
        /** Like PATH but repeats around in a loop. */
        PATH_REPEATING;

        public final int meta = ordinal();

        public static final Codec<MapLocationType> CODEC = StringRepresentable.fromEnum(MapLocationType::values);

        /** Registered by buildcraft core as "buildcraftcore:map_location_type". */
        public static final DataComponentType<MapLocationType> COMPONENT = DataComponentType.<MapLocationType> builder()
            .persistent(CODEC)
            .networkSynchronized(ByteBufCodecs.idMapper(MapLocationType::fromMeta, type -> type.meta))
            .build();

        public static MapLocationType fromMeta(int meta) {
            if (meta < 0 || meta >= values().length) {
                return MapLocationType.CLEAN;
            }
            return values()[meta];
        }

        public static MapLocationType getFromStack(@Nonnull ItemStack stack) {
            return stack.getOrDefault(COMPONENT, CLEAN);
        }

        public void setToStack(@Nonnull ItemStack stack) {
            stack.set(COMPONENT, this);
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** This function can be used for SPOT types.
     * 
     * @param stack
     * @return The point representing the map location. */
    BlockPos getPoint(@Nonnull ItemStack stack);

    /** This function can be used for SPOT and AREA types.
     * 
     * @param stack
     * @return The box representing the map location. */
    IBox getBox(@Nonnull ItemStack stack);

    /** This function can be used for SPOT, AREA and ZONE types. The PATH type needs to be handled separately.
     * 
     * @param stack
     * @return An IZone representing the map location - also an instance of IBox for SPOT and AREA types. */
    IZone getZone(@Nonnull ItemStack stack);

    /** This function can be used for SPOT and PATH types.
     * 
     * @param stack
     * @return A list of BlockPoses representing the path the Map Location stores. */
    List<BlockPos> getPath(@Nonnull ItemStack stack);

    /** This function can be used for SPOT types only.
     * 
     * @param stack
     * @return The side of the spot. */
    Direction getPointSide(@Nonnull ItemStack stack);
}
