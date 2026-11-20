/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nonnull;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import net.neoforged.fml.LogicalSide;

import buildcraft.api.core.IAreaProvider;
import buildcraft.api.core.IBox;
import buildcraft.api.core.IPathProvider;
import buildcraft.api.core.IZone;
import buildcraft.api.items.IMapLocation;

import buildcraft.lib.item.ItemBC_Neptune;
import buildcraft.lib.misc.LocaleUtil;
import buildcraft.lib.misc.NBTUtilBC;
import buildcraft.lib.misc.StackUtil;
import buildcraft.lib.misc.StringUtilBC;
import buildcraft.lib.misc.data.Box;

public class ItemMapLocation extends ItemBC_Neptune implements IMapLocation {
    private static final String[] STORAGE_TAGS = "x,y,z,side,xMin,xMax,yMin,yMax,zMin,zMax,path,chunkMapping,name".split(",");

    /** Reads the zone of a ZONE map location from the stack's data. Robotics (which owns ZonePlan) sets this. */
    public static Function<CompoundTag, IZone> zoneReader = nbt -> null;

    public ItemMapLocation(Item.Properties properties, String id) {
        super(properties.stacksTo(16), id);
    }

    /** Sets the stack's location type. Clean maps stack to 16, written ones don't stack (1.12.2's
     * getItemStackLimit). */
    private static void setType(@Nonnull ItemStack stack, MapLocationType type) {
        type.setToStack(stack);
        if (type == MapLocationType.CLEAN) {
            stack.remove(DataComponents.MAX_STACK_SIZE);
        } else {
            stack.set(DataComponents.MAX_STACK_SIZE, 1);
        }
    }

    @Override
    public void addInformation(ItemStack stack, Level world, List<String> strings, TooltipFlag flag) {
        stack = StackUtil.asNonNull(stack);
        CompoundTag cpt = NBTUtilBC.getItemData(stack);

        if (cpt.contains("name")) {
            String name = cpt.getStringOr("name", "");
            if (name.length() > 0) {
                strings.add(name);
            }
        }

        MapLocationType type = MapLocationType.getFromStack(stack);
        switch (type) {
            case SPOT: {
                if (cpt.contains("x") && cpt.contains("y") && cpt.contains("z") && cpt.contains("side")) {
                    int x = cpt.getIntOr("x", 0);
                    int y = cpt.getIntOr("y", 0);
                    int z = cpt.getIntOr("z", 0);
                    Direction side = Direction.values()[cpt.getByteOr("side", (byte) 0)];

                    strings.add(LocaleUtil.localize("{" + x + ", " + y + ", " + z + ", " + side + "}"));
                }
                break;
            }
            case AREA: {
                if (cpt.contains("xMin") && cpt.contains("yMin") && cpt.contains("zMin") && cpt.contains("xMax")
                    && cpt.contains("yMax") && cpt.contains("zMax")) {
                    int x = cpt.getIntOr("xMin", 0);
                    int y = cpt.getIntOr("yMin", 0);
                    int z = cpt.getIntOr("zMin", 0);
                    int xLength = cpt.getIntOr("xMax", 0) - x + 1;
                    int yLength = cpt.getIntOr("yMax", 0) - y + 1;
                    int zLength = cpt.getIntOr("zMax", 0) - z + 1;

                    strings.add(LocaleUtil.localize(
                        "{" + x + ", " + y + ", " + z + "} + {" + xLength + " x " + yLength + " x " + zLength + "}"));
                }
                break;
            }
            case PATH:
            case PATH_REPEATING: {
                if (cpt.contains("path")) {
                    ListTag pathNBT = (ListTag) cpt.get("path");

                    if (pathNBT.size() > 0) {
                        BlockPos first = NBTUtilBC.readBlockPos(pathNBT.get(0));
                        if (first != null) {
                            strings.add("{"+
                                StringUtilBC.blockPosToString(first) + "}, (+" + (pathNBT.size() - 1) + " elements)");
                        }
                    }
                }
                break;
            }
            default: {
                break;
            }
        }
        if (type != MapLocationType.CLEAN) {
            strings.add(LocaleUtil.localize("buildcraft.item.nonclean.usage"));
        }
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (world.isClientSide()) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            return clearMarkerData(StackUtil.asNonNull(stack));
        }
        return InteractionResult.PASS;
    }

    private static InteractionResult clearMarkerData(@Nonnull ItemStack stack) {
        if (MapLocationType.getFromStack(stack) == MapLocationType.CLEAN) {
            return InteractionResult.PASS;
        }
        CompoundTag nbt = NBTUtilBC.getItemData(stack);
        for (String key : STORAGE_TAGS) {
            nbt.remove(key);
        }
        NBTUtilBC.setItemData(stack, nbt);
        setType(stack, MapLocationType.CLEAN);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction side = context.getClickedFace();
        if (world.isClientSide() || player == null) {
            return InteractionResult.PASS;
        }

        if (MapLocationType.getFromStack(stack) != MapLocationType.CLEAN) {
            return InteractionResult.FAIL;
        }

        ItemStack modified = stack;

        if (stack.getCount() > 1) {
            modified = stack.copy();
            stack.setCount(stack.getCount() - 1);
            modified.setCount(1);
        }

        BlockEntity tile = world.getBlockEntity(pos);
        CompoundTag cpt = NBTUtilBC.getItemData(modified);
        MapLocationType type;

        if (tile instanceof IPathProvider) {
            List<BlockPos> path = ((IPathProvider) tile).getPath();

            if (path.size() > 1 && path.get(0).equals(path.get(path.size() - 1))) {
                type = MapLocationType.PATH_REPEATING;
            } else {
                type = MapLocationType.PATH;
            }

            ListTag pathNBT = new ListTag();

            for (BlockPos posInPath : path) {
                pathNBT.add(NBTUtilBC.writeBlockPos(posInPath));
            }

            cpt.put("path", pathNBT);
        } else if (tile instanceof IAreaProvider) {
            type = MapLocationType.AREA;

            IAreaProvider areaTile = (IAreaProvider) tile;

            cpt.putInt("xMin", areaTile.min().getX());
            cpt.putInt("yMin", areaTile.min().getY());
            cpt.putInt("zMin", areaTile.min().getZ());
            cpt.putInt("xMax", areaTile.max().getX());
            cpt.putInt("yMax", areaTile.max().getY());
            cpt.putInt("zMax", areaTile.max().getZ());

        } else {
            type = MapLocationType.SPOT;

            cpt.putByte("side", (byte) side.get3DDataValue());
            cpt.putInt("x", pos.getX());
            cpt.putInt("y", pos.getY());
            cpt.putInt("z", pos.getZ());
        }
        NBTUtilBC.setItemData(modified, cpt);
        setType(modified, type);

        if (modified != stack && !player.getInventory().add(modified)) {
            player.drop(modified, false, net.minecraft.util.Prediction.SERVER_ONLY);
        }
        return InteractionResult.SUCCESS;
    }

    public static IBox getAreaBox(@Nonnull ItemStack item) {
        CompoundTag cpt = NBTUtilBC.getItemData(item);
        int xMin = cpt.getIntOr("xMin", 0);
        int yMin = cpt.getIntOr("yMin", 0);
        int zMin = cpt.getIntOr("zMin", 0);
        BlockPos min = new BlockPos(xMin, yMin, zMin);

        int xMax = cpt.getIntOr("xMax", 0);
        int yMax = cpt.getIntOr("yMax", 0);
        int zMax = cpt.getIntOr("zMax", 0);
        BlockPos max = new BlockPos(xMax, yMax, zMax);

        return new Box(min, max);
    }

    public static IBox getPointBox(@Nonnull ItemStack item) {
        CompoundTag cpt = NBTUtilBC.getItemData(item);
        MapLocationType type = MapLocationType.getFromStack(item);

        switch (type) {
            case SPOT: {
                int x = cpt.getIntOr("x", 0);
                int y = cpt.getIntOr("y", 0);
                int z = cpt.getIntOr("z", 0);

                BlockPos pos = new BlockPos(x, y, z);

                return new Box(pos, pos);
            }
            default: {
                return null;
            }
        }
    }

    public static Direction getPointFace(@Nonnull ItemStack stack) {
        CompoundTag cpt = NBTUtilBC.getItemData(stack);
        return Direction.values()[cpt.getByteOr("side", (byte) 0)];
    }

    @Override
    public IBox getBox(@Nonnull ItemStack item) {
        MapLocationType type = MapLocationType.getFromStack(item);

        switch (type) {
            case AREA: {
                return getAreaBox(item);
            }
            case SPOT: {
                return getPointBox(item);
            }
            default: {
                return null;
            }
        }
    }

    @Override
    public Direction getPointSide(@Nonnull ItemStack item) {
        CompoundTag cpt = NBTUtilBC.getItemData(item);
        MapLocationType type = MapLocationType.getFromStack(item);

        if (type == MapLocationType.SPOT) {
            return Direction.values()[cpt.getByteOr("side", (byte) 0)];
        } else {
            return null;
        }
    }

    @Override
    public BlockPos getPoint(@Nonnull ItemStack item) {
        CompoundTag cpt = NBTUtilBC.getItemData(item);
        MapLocationType type = MapLocationType.getFromStack(item);

        if (type == MapLocationType.SPOT) {
            return new BlockPos(cpt.getIntOr("x", 0), cpt.getIntOr("y", 0), cpt.getIntOr("z", 0));
        } else {
            return null;
        }
    }

    @Override
    public IZone getZone(@Nonnull ItemStack item) {
        CompoundTag cpt = NBTUtilBC.getItemData(item);
        MapLocationType type = MapLocationType.getFromStack(item);
        switch (type) {
            case ZONE: {
                return zoneReader.apply(cpt);
            }
            case AREA: {
                return getBox(item);
            }
            case PATH:
            case PATH_REPEATING: {
                return getPointBox(item);
            }
            default: {
                return null;
            }
        }
    }

    @Override
    public List<BlockPos> getPath(@Nonnull ItemStack item) {
        CompoundTag cpt = NBTUtilBC.getItemData(item);
        MapLocationType type = MapLocationType.getFromStack(item);
        switch (type) {
            case PATH:
            case PATH_REPEATING: {
                List<BlockPos> indexList = new ArrayList<>();
                ListTag pathNBT = (ListTag) cpt.get("path");
                for (int i = 0; i < pathNBT.size(); i++) {
                    BlockPos pos = NBTUtilBC.readBlockPos(pathNBT.get(i));
                    if (pos != null) {
                        indexList.add(pos);
                    }
                }
                return indexList;
            }
            case SPOT: {
                List<BlockPos> indexList = new ArrayList<>();
                indexList.add(new BlockPos(cpt.getIntOr("x", 0), cpt.getIntOr("y", 0), cpt.getIntOr("z", 0)));
                return indexList;
            }
            default: {
                return null;
            }
        }
    }

    /** Makes the stack a ZONE map location.
     *
     * @param planWriter Writes the zone plan into the stack's data (robotics passes ZonePlan::writeToNBT). */
    public static void setZone(@Nonnull ItemStack item, Consumer<CompoundTag> planWriter) {
        NBTUtilBC.updateItemData(item, planWriter);
        setType(item, MapLocationType.ZONE);
    }

    @Override
    public String getLabel(@Nonnull ItemStack item) {
        return NBTUtilBC.getItemData(item).getStringOr("name", "");
    }

    @Override
    public boolean setLabel(@Nonnull ItemStack item, String name) {
        NBTUtilBC.updateItemData(item, cpt -> cpt.putString("name", name));
        return true;
    }
}
