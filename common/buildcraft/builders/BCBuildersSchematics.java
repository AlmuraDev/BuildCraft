package buildcraft.builders;

import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import buildcraft.api.schematics.ISchematicBlock;
import buildcraft.api.schematics.SchematicBlockContext;
import buildcraft.api.schematics.SchematicBlockFactoryRegistry;
import buildcraft.api.schematics.SchematicEntityFactoryRegistry;

import buildcraft.builders.snapshot.SchematicBlockAir;
import buildcraft.builders.snapshot.SchematicBlockDefault;
import buildcraft.builders.snapshot.SchematicBlockFluid;
import buildcraft.builders.snapshot.SchematicEntityDefault;

public class BCBuildersSchematics {
    public static void preInit() {
        registerSchematicFactory("air", 0, SchematicBlockAir::predicate, SchematicBlockAir::new);
        registerSchematicFactory("default", 100, SchematicBlockDefault::predicate, SchematicBlockDefault::new);
        registerSchematicFactory("fluid", 200, SchematicBlockFluid::predicate, SchematicBlockFluid::new);

        // 1.12.2 also had a "banner" schematic that required a banner with the placed one's patterns; 26.3 banners use
        // the default schematic (a plain banner). TODO(26.3): keep the patterns (banner_patterns component).
        registerSchematicFactory("vine", 300, c -> c.block instanceof VineBlock, BCBuildersSchematics::getVine);

        SchematicEntityFactoryRegistry.registerFactory("default", 100, SchematicEntityDefault::predicate,
            SchematicEntityDefault::new);
    }

    private static <S extends ISchematicBlock> void registerSchematicFactory(String name, int priority,
        Predicate<SchematicBlockContext> predicate, Supplier<S> supplier) {
        SchematicBlockFactoryRegistry.registerFactory(name, priority, predicate, supplier);
    }

    private static SchematicBlockDefault getVine() {
        return new SchematicBlockDefault() {
            @Override
            public boolean isReadyToBuild(Level world, BlockPos blockPos) {
                return super.isReadyToBuild(world, blockPos)
                    && (world.getBlockState(blockPos.above()).getBlock() instanceof VineBlock
                        || Direction.Plane.HORIZONTAL.stream().map(blockPos::relative)
                            .map(world::getBlockState)
                            .anyMatch(BlockState::isSolid));
            }
        };
    }
}
