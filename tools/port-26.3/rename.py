"""
First, mechanical pass of the BuildCraft 1.12.2 -> Minecraft 26.3 (NeoForge) port: rewrites MCP-named imports, class names
and a handful of unambiguous method names to their Mojang/NeoForge 26.3 equivalents. Everything with no one-to-one
replacement is left alone for the compiler to point at, and is ported by hand.

Run on the files of one batch at a time:  python tools/port-26.3/rename.py <file-or-dir>...
Running it twice is harmless.
"""
import os
import re
import sys

# Old fully-qualified class -> new fully-qualified class. Simple names are renamed in a file only when it imports the
# old class, so BuildCraft classes that share a simple name are untouched.
CLASSES = {
    # core / math
    'net.minecraft.util.EnumFacing': 'net.minecraft.core.Direction',
    'net.minecraft.util.EnumFacing.Axis': 'net.minecraft.core.Direction.Axis',
    'net.minecraft.util.EnumFacing.AxisDirection': 'net.minecraft.core.Direction.AxisDirection',
    'net.minecraft.util.EnumFacing.Plane': 'net.minecraft.core.Direction.Plane',
    'net.minecraft.util.math.BlockPos': 'net.minecraft.core.BlockPos',
    'net.minecraft.util.math.BlockPos.MutableBlockPos': 'net.minecraft.core.BlockPos.MutableBlockPos',
    'net.minecraft.util.math.Vec3i': 'net.minecraft.core.Vec3i',
    'net.minecraft.util.math.Vec3d': 'net.minecraft.world.phys.Vec3',
    'net.minecraft.util.math.AxisAlignedBB': 'net.minecraft.world.phys.AABB',
    'net.minecraft.util.math.RayTraceResult': 'net.minecraft.world.phys.HitResult',
    'net.minecraft.util.math.ChunkPos': 'net.minecraft.world.level.ChunkPos',
    'net.minecraft.util.math.MathHelper': 'net.minecraft.util.Mth',
    'net.minecraft.util.NonNullList': 'net.minecraft.core.NonNullList',
    'net.minecraft.util.ResourceLocation': 'net.minecraft.resources.Identifier',
    'net.minecraft.util.EnumHand': 'net.minecraft.world.InteractionHand',
    'net.minecraft.util.EnumActionResult': 'net.minecraft.world.InteractionResult',
    'net.minecraft.util.Rotation': 'net.minecraft.world.level.block.Rotation',
    'net.minecraft.util.Mirror': 'net.minecraft.world.level.block.Mirror',
    'net.minecraft.util.IStringSerializable': 'net.minecraft.util.StringRepresentable',
    'net.minecraft.util.JsonUtils': 'net.minecraft.util.GsonHelper',
    'net.minecraft.util.SoundCategory': 'net.minecraft.sounds.SoundSource',
    'net.minecraft.util.SoundEvent': 'net.minecraft.sounds.SoundEvent',
    'net.minecraft.init.SoundEvents': 'net.minecraft.sounds.SoundEvents',
    'net.minecraft.util.DamageSource': 'net.minecraft.world.damagesource.DamageSource',
    'net.minecraft.util.StringUtils': 'net.minecraft.util.StringUtil',
    'net.minecraft.util.ReportedException': 'net.minecraft.ReportedException',
    'net.minecraft.crash.CrashReport': 'net.minecraft.CrashReport',
    'net.minecraft.crash.CrashReportCategory': 'net.minecraft.CrashReportCategory',
    'net.minecraft.util.text.TextFormatting': 'net.minecraft.ChatFormatting',
    'net.minecraft.util.text.ITextComponent': 'net.minecraft.network.chat.Component',
    'net.minecraft.util.text.Style': 'net.minecraft.network.chat.Style',
    'net.minecraft.profiler.Profiler': 'net.minecraft.util.profiling.ProfilerFiller',
    # nbt
    'net.minecraft.nbt.NBTTagCompound': 'net.minecraft.nbt.CompoundTag',
    'net.minecraft.nbt.NBTTagList': 'net.minecraft.nbt.ListTag',
    'net.minecraft.nbt.NBTBase': 'net.minecraft.nbt.Tag',
    'net.minecraft.nbt.NBTTagString': 'net.minecraft.nbt.StringTag',
    'net.minecraft.nbt.NBTTagInt': 'net.minecraft.nbt.IntTag',
    'net.minecraft.nbt.NBTTagByte': 'net.minecraft.nbt.ByteTag',
    'net.minecraft.nbt.NBTTagShort': 'net.minecraft.nbt.ShortTag',
    'net.minecraft.nbt.NBTTagLong': 'net.minecraft.nbt.LongTag',
    'net.minecraft.nbt.NBTTagFloat': 'net.minecraft.nbt.FloatTag',
    'net.minecraft.nbt.NBTTagDouble': 'net.minecraft.nbt.DoubleTag',
    'net.minecraft.nbt.NBTTagByteArray': 'net.minecraft.nbt.ByteArrayTag',
    'net.minecraft.nbt.NBTTagIntArray': 'net.minecraft.nbt.IntArrayTag',
    'net.minecraft.nbt.NBTTagLongArray': 'net.minecraft.nbt.LongArrayTag',
    'net.minecraft.nbt.NBTPrimitive': 'net.minecraft.nbt.NumericTag',
    'net.minecraft.nbt.NBTUtil': 'net.minecraft.nbt.NbtUtils',
    'net.minecraft.nbt.CompressedStreamTools': 'net.minecraft.nbt.NbtIo',
    'net.minecraft.nbt.JsonToNBT': 'net.minecraft.nbt.TagParser',
    # world
    'net.minecraft.world.World': 'net.minecraft.world.level.Level',
    'net.minecraft.world.WorldServer': 'net.minecraft.server.level.ServerLevel',
    'net.minecraft.world.IBlockAccess': 'net.minecraft.world.level.BlockGetter',
    'net.minecraft.world.Explosion': 'net.minecraft.world.level.Explosion',
    'net.minecraft.world.chunk.Chunk': 'net.minecraft.world.level.chunk.LevelChunk',
    'net.minecraft.world.biome.Biome': 'net.minecraft.world.level.biome.Biome',
    'net.minecraft.world.storage.WorldSavedData': 'net.minecraft.world.level.saveddata.SavedData',
    'net.minecraft.world.GameType': 'net.minecraft.world.level.GameType',
    'net.minecraft.world.EnumSkyBlock': 'net.minecraft.world.level.LightLayer',
    'net.minecraft.server.MinecraftServer': 'net.minecraft.server.MinecraftServer',
    # blocks
    'net.minecraft.block.Block': 'net.minecraft.world.level.block.Block',
    'net.minecraft.block.state.IBlockState': 'net.minecraft.world.level.block.state.BlockState',
    'net.minecraft.block.state.BlockStateContainer': 'net.minecraft.world.level.block.state.StateDefinition',
    'net.minecraft.block.properties.IProperty': 'net.minecraft.world.level.block.state.properties.Property',
    'net.minecraft.block.properties.PropertyEnum': 'net.minecraft.world.level.block.state.properties.EnumProperty',
    'net.minecraft.block.properties.PropertyBool': 'net.minecraft.world.level.block.state.properties.BooleanProperty',
    'net.minecraft.block.properties.PropertyInteger': 'net.minecraft.world.level.block.state.properties.IntegerProperty',
    'net.minecraft.block.SoundType': 'net.minecraft.world.level.block.SoundType',
    'net.minecraft.block.material.MapColor': 'net.minecraft.world.level.material.MapColor',
    'net.minecraft.block.BlockLiquid': 'net.minecraft.world.level.block.LiquidBlock',
    'net.minecraft.init.Blocks': 'net.minecraft.world.level.block.Blocks',
    'net.minecraft.tileentity.TileEntity': 'net.minecraft.world.level.block.entity.BlockEntity',
    'net.minecraft.tileentity.TileEntityFurnace': 'net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity',
    'net.minecraft.tileentity.TileEntityChest': 'net.minecraft.world.level.block.entity.ChestBlockEntity',
    # items
    'net.minecraft.item.Item': 'net.minecraft.world.item.Item',
    'net.minecraft.item.ItemStack': 'net.minecraft.world.item.ItemStack',
    'net.minecraft.item.ItemBlock': 'net.minecraft.world.item.BlockItem',
    'net.minecraft.item.EnumDyeColor': 'net.minecraft.world.item.DyeColor',
    'net.minecraft.init.Items': 'net.minecraft.world.item.Items',
    'net.minecraft.client.util.ITooltipFlag': 'net.minecraft.world.item.TooltipFlag',
    'net.minecraft.item.crafting.Ingredient': 'net.minecraft.world.item.crafting.Ingredient',
    'net.minecraft.item.crafting.IRecipe': 'net.minecraft.world.item.crafting.Recipe',
    'net.minecraft.inventory.EntityEquipmentSlot': 'net.minecraft.world.entity.EquipmentSlot',
    # entities
    'net.minecraft.entity.Entity': 'net.minecraft.world.entity.Entity',
    'net.minecraft.entity.EntityLivingBase': 'net.minecraft.world.entity.LivingEntity',
    'net.minecraft.entity.EntityLiving': 'net.minecraft.world.entity.Mob',
    'net.minecraft.entity.player.EntityPlayer': 'net.minecraft.world.entity.player.Player',
    'net.minecraft.entity.player.EntityPlayerMP': 'net.minecraft.server.level.ServerPlayer',
    'net.minecraft.entity.player.InventoryPlayer': 'net.minecraft.world.entity.player.Inventory',
    'net.minecraft.entity.item.EntityItem': 'net.minecraft.world.entity.item.ItemEntity',
    # inventory / menus (simultaneous: IInventory -> Container and Container -> AbstractContainerMenu)
    'net.minecraft.inventory.IInventory': 'net.minecraft.world.Container',
    'net.minecraft.inventory.Container': 'net.minecraft.world.inventory.AbstractContainerMenu',
    'net.minecraft.inventory.Slot': 'net.minecraft.world.inventory.Slot',
    'net.minecraft.inventory.ClickType': 'net.minecraft.world.inventory.ContainerInput',
    'net.minecraft.inventory.InventoryCrafting': 'net.minecraft.world.inventory.CraftingContainer',
    'net.minecraft.inventory.InventoryBasic': 'net.minecraft.world.SimpleContainer',
    'net.minecraft.inventory.ISidedInventory': 'net.minecraft.world.WorldlyContainer',
    # network
    'net.minecraft.network.PacketBuffer': 'net.minecraft.network.FriendlyByteBuf',
    # client
    'net.minecraft.client.Minecraft': 'net.minecraft.client.Minecraft',
    'net.minecraft.client.gui.FontRenderer': 'net.minecraft.client.gui.Font',
    'net.minecraft.client.gui.GuiScreen': 'net.minecraft.client.gui.screens.Screen',
    'net.minecraft.client.gui.inventory.GuiContainer': 'net.minecraft.client.gui.screens.inventory.AbstractContainerScreen',
    'net.minecraft.client.resources.I18n': 'net.minecraft.client.resources.language.I18n',
    'net.minecraft.client.renderer.texture.TextureAtlasSprite': 'net.minecraft.client.renderer.texture.TextureAtlasSprite',
    'net.minecraft.client.renderer.block.model.BakedQuad': 'net.minecraft.client.resources.model.geometry.BakedQuad',
    'net.minecraft.client.multiplayer.WorldClient': 'net.minecraft.client.multiplayer.ClientLevel',
    'net.minecraft.client.entity.EntityPlayerSP': 'net.minecraft.client.player.LocalPlayer',
    'net.minecraft.client.renderer.BufferBuilder': 'com.mojang.blaze3d.vertex.BufferBuilder',
    'net.minecraft.client.renderer.vertex.VertexFormat': 'com.mojang.renderpearl.api.vertex.VertexFormat',
    'net.minecraft.client.renderer.vertex.DefaultVertexFormats': 'com.mojang.blaze3d.vertex.DefaultVertexFormat',
    'net.minecraft.client.renderer.vertex.VertexFormatElement': 'com.mojang.renderpearl.api.vertex.VertexFormatElement',
    'net.minecraft.client.renderer.texture.TextureManager': 'net.minecraft.client.renderer.texture.TextureManager',
    'net.minecraft.client.renderer.texture.DynamicTexture': 'net.minecraft.client.renderer.texture.DynamicTexture',
    'net.minecraft.client.renderer.color.IItemColor': 'net.minecraft.client.color.item.ItemTintSource',
    'net.minecraft.client.renderer.color.BlockColors': 'net.minecraft.client.color.block.BlockColors',
    'net.minecraft.client.multiplayer.ClientLevel': 'net.minecraft.client.multiplayer.ClientLevel',
    'net.minecraft.client.settings.GameSettings': 'net.minecraft.client.Options',
    'net.minecraft.client.shader.Framebuffer': 'com.mojang.blaze3d.pipeline.RenderTarget',
    # forge -> neoforge
    # BuildCraft uses Side as the logical side (client/server world); @SideOnly(Side.X) becomes @OnlyIn(Dist.X) below.
    'net.minecraftforge.fml.relauncher.Side': 'net.neoforged.fml.LogicalSide',
    'net.minecraftforge.fml.common.network.simpleimpl.MessageContext': 'net.neoforged.neoforge.network.handling.IPayloadContext',
    'net.minecraftforge.fml.common.network.simpleimpl.IMessage': 'buildcraft.lib.net.IMessage',
    'net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler': 'buildcraft.lib.net.IMessageHandler',
    'net.minecraft.util.ITickable': 'buildcraft.lib.tile.ITickable',
    'net.minecraftforge.fml.relauncher.SideOnly': 'net.neoforged.api.distmarker.OnlyIn',
    'net.minecraftforge.fml.common.eventhandler.SubscribeEvent': 'net.neoforged.bus.api.SubscribeEvent',
    'net.minecraftforge.fml.common.eventhandler.EventPriority': 'net.neoforged.bus.api.EventPriority',
    'net.minecraftforge.fml.common.eventhandler.Event': 'net.neoforged.bus.api.Event',
    'net.minecraftforge.fml.common.eventhandler.Cancelable': 'net.neoforged.bus.api.ICancellableEvent',
    'net.minecraftforge.common.MinecraftForge': 'net.neoforged.neoforge.common.NeoForge',
    'net.minecraftforge.common.capabilities.ICapabilityProvider': 'buildcraft.api.core.ICapabilityProvider',
    'net.minecraftforge.fml.common.Loader': 'net.neoforged.fml.ModList',
    'net.minecraftforge.fml.common.Mod': 'net.neoforged.fml.common.Mod',
    'net.minecraftforge.fml.common.ModContainer': 'net.neoforged.fml.ModContainer',
    'net.minecraftforge.common.util.FakePlayer': 'net.neoforged.neoforge.common.util.FakePlayer',
    'net.minecraftforge.common.util.INBTSerializable': 'buildcraft.api.core.INBTSerializable',
    'net.minecraftforge.fluids.FluidStack': 'net.neoforged.neoforge.fluids.FluidStack',
    'net.minecraftforge.fluids.Fluid': 'net.minecraft.world.level.material.Fluid',
    # BuildCraft keeps 1.12-shaped handlers internally (buildcraft.api.transfer); lib adapts them to NeoForge's.
    'net.minecraftforge.items.IItemHandler': 'buildcraft.api.transfer.IItemHandler',
    'net.minecraftforge.items.IItemHandlerModifiable': 'buildcraft.api.transfer.IItemHandlerModifiable',
    'net.minecraftforge.fluids.capability.IFluidHandler': 'buildcraft.api.transfer.IFluidHandler',
    'net.minecraftforge.fluids.capability.IFluidHandlerItem': 'buildcraft.api.transfer.IFluidHandlerItem',
    'net.minecraftforge.fluids.capability.IFluidTankProperties': 'buildcraft.api.transfer.IFluidTankProperties',
    'net.minecraftforge.fluids.capability.FluidTankProperties': 'buildcraft.api.transfer.FluidTankProperties',
    'net.minecraftforge.fluids.IFluidTank': 'buildcraft.api.transfer.IFluidTank',
    'net.minecraftforge.energy.IEnergyStorage': 'buildcraft.api.transfer.IEnergyStorage',
    'net.minecraft.util.ActionResult': 'buildcraft.api.core.ActionResult',
    # javax.vecmath (gone) -> JOML, which 26.3 uses
    'javax.vecmath.Vector3f': 'org.joml.Vector3f',
    'javax.vecmath.Point3f': 'org.joml.Vector3f',
    'javax.vecmath.Tuple3f': 'org.joml.Vector3f',
    'javax.vecmath.Vector4f': 'org.joml.Vector4f',
    'javax.vecmath.Point4f': 'org.joml.Vector4f',
    'javax.vecmath.Tuple4f': 'org.joml.Vector4f',
    'javax.vecmath.Vector2f': 'org.joml.Vector2f',
    'javax.vecmath.Point2f': 'org.joml.Vector2f',
    'javax.vecmath.Tuple2f': 'org.joml.Vector2f',
    'javax.vecmath.Matrix4f': 'org.joml.Matrix4f',
    'javax.vecmath.Matrix3f': 'org.joml.Matrix3f',
    'javax.vecmath.Vector3d': 'org.joml.Vector3d',
    'javax.vecmath.Point3d': 'org.joml.Vector3d',
    'javax.vecmath.Tuple3d': 'org.joml.Vector3d',
    'javax.vecmath.Matrix4d': 'org.joml.Matrix4d',
    'javax.vecmath.AxisAngle4f': 'org.joml.AxisAngle4f',
    'javax.vecmath.Quat4f': 'org.joml.Quaternionf',
    'net.minecraftforge.common.config.Configuration': 'buildcraft.lib.config.Configuration',
    'net.minecraftforge.common.config.Property': 'buildcraft.lib.config.Property',
    'net.minecraftforge.common.config.ConfigCategory': 'buildcraft.lib.config.ConfigCategory',
    'net.minecraft.util.BlockRenderLayer': 'buildcraft.api.core.BCRenderLayer',
    'net.minecraftforge.fluids.FluidUtil': 'net.neoforged.neoforge.transfer.fluid.FluidUtil',
    'net.minecraftforge.common.util.BlockSnapshot': 'net.neoforged.neoforge.common.util.BlockSnapshot',
    'net.minecraftforge.event.world.BlockEvent': 'net.neoforged.neoforge.event.level.BlockEvent',
    'net.minecraftforge.event.world.WorldEvent': 'net.neoforged.neoforge.event.level.LevelEvent',
    'net.minecraftforge.event.entity.player.ItemTooltipEvent': 'net.neoforged.neoforge.event.entity.player.ItemTooltipEvent',
    'net.minecraftforge.event.ForgeEventFactory': 'net.neoforged.neoforge.event.EventHooks',
    'net.minecraftforge.common.ForgeHooks': 'net.neoforged.neoforge.common.CommonHooks',
    'net.minecraftforge.fml.common.ObfuscationReflectionHelper': 'net.neoforged.fml.util.ObfuscationReflectionHelper',
}

# Unambiguous method/field renames, applied everywhere: (regex, replacement). Only names that don't exist with another
# meaning in 26.3; anything unsure is left for the compiler.
METHODS = [
    # Batch 2 (transport) additions
    (r'\b(face|side|facing|dir|direction|f|emitterSide|from|to|d)\.getIndex\(\)', r'\1.get3DDataValue()'),
    (r'(Direction\.\w+)\.getIndex\(\)', r'\1.get3DDataValue()'),
    (r'\bDirection\.byIndex\(', 'Direction.from3DDataValue('),
    (r'\bDyeColor\.byMetadata\(', 'DyeColor.byId('),
    (r'\b(colour|color|c|col|dye)\.getMetadata\(\)', r'\1.getId()'),
    (r'\bMinecraft\.getMinecraft\(\)', 'Minecraft.getInstance()'),
    (r'@SideOnly\(\s*Side\.CLIENT\s*\)', '@OnlyIn(Dist.CLIENT)'),
    (r'@SideOnly\(\s*Side\.SERVER\s*\)', '@OnlyIn(Dist.DEDICATED_SERVER)'),
    (r'@OnlyIn\(\s*LogicalSide\.CLIENT\s*\)', '@OnlyIn(Dist.CLIENT)'),
    (r'@OnlyIn\(\s*LogicalSide\.SERVER\s*\)', '@OnlyIn(Dist.DEDICATED_SERVER)'),
    (r'\bMinecraftForge\.EVENT_BUS\b', 'NeoForge.EVENT_BUS'),
    (r'\.isRemote\b(?!\s*\()', '.isClientSide()'),
    (r'\.getTileEntity\(', '.getBlockEntity('),
    (r'\.markDirty\(\)', '.setChanged()'),
    (r'\.getDefaultState\(\)', '.defaultBlockState()'),
    (r'\.withProperty\(', '.setValue('),
    (r'\.getTotalWorldTime\(\)', '.getGameTime()'),
    (r'\.isBlockLoaded\(', '.isLoaded('),
    (r'\.isAirBlock\(', '.isEmptyBlock('),
    (r'\.spawnEntity\(', '.addFreshEntity('),
    (r'\.getEntitiesWithinAABB\(', '.getEntitiesOfClass('),
    (r'\.notifyBlockUpdate\(', '.sendBlockUpdated('),
    # BuildCraft's WorldEventListenerAdapter keeps the call's new name, so its overrides follow.
    (r'\bpublic void notifyBlockUpdate\(', 'public void sendBlockUpdated('),
    (r'\.getHeldItemMainhand\(\)', '.getMainHandItem()'),
    (r'\.getHeldItemOffhand\(\)', '.getOffhandItem()'),
    (r'\.getHeldItem\(', '.getItemInHand('),
    (r'(?<!getModInfo\(\))\.getDisplayName\(\)', '.getHoverName()'),
    (r'\bItemStack\.areItemsEqual\(', 'ItemStack.isSameItem('),
    (r'\bItemStack\.areItemStackTagsEqual\(', 'ItemStack.isSameItemSameComponents('),
    # nbt
    (r'\.setInteger\(', '.putInt('),
    (r'\.setString\(', '.putString('),
    (r'\.setBoolean\(', '.putBoolean('),
    (r'\.setByte\(', '.putByte('),
    (r'\.setShort\(', '.putShort('),
    (r'\.setLong\(', '.putLong('),
    (r'\.setFloat\(', '.putFloat('),
    (r'\.setDouble\(', '.putDouble('),
    (r'\.setIntArray\(', '.putIntArray('),
    (r'\.setByteArray\(', '.putByteArray('),
    (r'\.setLongArray\(', '.putLongArray('),
    (r'\.setTag\(', '.put('),
    (r'\.hasKey\(', '.contains('),
    (r'\.removeTag\(', '.remove('),
    (r'\.getKeySet\(\)', '.keySet()'),
    (r'\.getCompoundTag\(', '.getCompoundOrEmpty('),
    (r'\.tagCount\(\)', '.size()'),
    (r'\.appendTag\(', '.add('),
    (r'\.writeCompoundTag\(', '.writeNbt('),
    (r'\.hasNoTags\(\)', '.isEmpty()'),
    (r'\.readCompoundTag\(\)', '.readNbt()'),
    (r'\bConstants\.NBT\.TAG_', 'Tag.TAG_'),
    (r'\.getOffset\(\)', '.getStep()'),
    (r'\.getTagId\(\)', '.getId()'),
    # vectors / directions
    (r'\.addVector\(', '.add('),
    (r'\.lengthVector\(\)', '.length()'),
    (r'\.squareDistanceTo\(', '.distanceToSqr('),
    (r'\.crossProduct\(', '.cross('),
    (r'\.dotProduct\(', '.dot('),
    (r'\bDirection\.VALUES\b', 'Direction.values()'),
    (r'\bDirection\.getFront\(', 'Direction.from3DDataValue('),
    (r'\bDirection\.HORIZONTALS\b', 'Direction.Plane.HORIZONTAL'),
    # registries
    (r'\bForgeRegistries\.ITEMS\b', 'BuiltInRegistries.ITEM'),
    (r'\bForgeRegistries\.BLOCKS\b', 'BuiltInRegistries.BLOCK'),
    (r'\bForgeRegistries\.FLUIDS\b', 'BuiltInRegistries.FLUID'),
    (r'\bForgeRegistries\.ENTITIES\b', 'BuiltInRegistries.ENTITY_TYPE'),
    (r'\bForgeRegistries\.SOUND_EVENTS\b', 'BuiltInRegistries.SOUND_EVENT'),
    (r'\bItem\.REGISTRY\b', 'BuiltInRegistries.ITEM'),
    (r'\bBlock\.REGISTRY\b', 'BuiltInRegistries.BLOCK'),
    (r'import net\.minecraftforge\.fml\.common\.registry\.ForgeRegistries;',
     'import net.minecraft.core.registries.BuiltInRegistries;'),
    (r'([\w.()]+)\.getSubItems\(CreativeTabs\.\w+, (\w+)\)', r'StackUtil.addSubItems(\1, \2)'),
    (r'\bFluidUtil\.getFluidContained\(', 'FluidUtilBC.getFluidContained('),
    # json
    (r'\bGsonHelper\.getString\(', 'GsonHelper.getAsString('),
    (r'\bGsonHelper\.getInt\(', 'GsonHelper.getAsInt('),
    (r'\bGsonHelper\.getLong\(', 'GsonHelper.getAsLong('),
    (r'\bGsonHelper\.getFloat\(', 'GsonHelper.getAsFloat('),
    (r'\bGsonHelper\.getDouble\(', 'GsonHelper.getAsDouble('),
    (r'\bGsonHelper\.getBoolean\(', 'GsonHelper.getAsBoolean('),
    (r'\bGsonHelper\.getJsonObject\(', 'GsonHelper.getAsJsonObject('),
    (r'\bGsonHelper\.getJsonArray\(', 'GsonHelper.getAsJsonArray('),
    (r'\bGsonHelper\.hasField\(', 'GsonHelper.isValidNode('),
    (r'\bGsonHelper\.isString\(', 'GsonHelper.isStringValue('),
    (r'\bGsonHelper\.isNumber\(', 'GsonHelper.isNumberValue('),
    (r'\bGsonHelper\.isBoolean\(', 'GsonHelper.isBooleanValue('),
    (r'\bGsonHelper\.isJsonArray\(', 'GsonHelper.isArrayNode('),
    (r'\bGsonHelper\.isJsonPrimitive\(', 'GsonHelper.isValidPrimitive('),
    # text
    (r'\bnew TextComponentTranslation\(', 'Component.translatable('),
    (r'\bnew TextComponentString\(', 'Component.literal('),
    (r'import net\.minecraft\.util\.text\.(TextComponentTranslation|TextComponentString);',
     'import net.minecraft.network.chat.Component;'),
    (r'\bChatFormatting\.fromColorIndex\(', 'ChatFormatting.getById('),
    # positions
    (r'\bBlockPos\.ORIGIN\b', 'BlockPos.ZERO'),
    # world block-change listeners (see buildcraft.lib.world.WorldEventListeners)
    (r'([\w.]+(?:\(\))?)\.addEventListener\(', r'WorldEventListeners.add(\1, '),
    (r'([\w.]+(?:\(\))?)\.removeEventListener\(', r'WorldEventListeners.remove(\1, '),
    (r'private final IWorldEventListener (\w+) = new WorldEventListenerAdapter\(\)',
     r'private final WorldEventListenerAdapter \1 = new WorldEventListenerAdapter()'),
    (r'import net\.minecraft\.world\.IWorldEventListener;', 'import buildcraft.lib.world.WorldEventListeners;'),
    # 1.12.2 sound names had a BLOCK_/ITEM_/ENTITY_ prefix ("BLOCK_LEVER_CLICK" -> "LEVER_CLICK")
    (r'\bSoundEvents\.(?:BLOCK|ITEM|ENTITY)_(\w+)', r'SoundEvents.\1'),
    (r'\bBlockPos\.getAllInBox\(', 'BlockPos.betweenClosed('),
    (r'\bBlockPos\.getAllInBoxMutable\(', 'BlockPos.betweenClosed('),
    (r'\bworld\.spawnParticle\(EnumParticleTypes\.', 'world.addParticle(ParticleTypes.'),
    (r'import net\.minecraft\.util\.EnumParticleTypes;', 'import net.minecraft.core.particles.ParticleTypes;'),
    (r'\bCapabilityEnergy\.ENERGY\b', 'CapUtil.CAP_RF'),
    (r'\bCapabilityItemHandler\.ITEM_HANDLER_CAPABILITY\b', 'CapUtil.CAP_ITEMS'),
    (r'\bCapabilityFluidHandler\.FLUID_HANDLER_CAPABILITY\b', 'CapUtil.CAP_FLUIDS'),
    (r'import net\.minecraftforge\.(energy\.CapabilityEnergy|items\.CapabilityItemHandler|fluids\.capability\.CapabilityFluidHandler);',
     'import buildcraft.lib.misc.CapUtil;'),
    (r'\bnew ChunkPos\(([\w.()]+)\)', r'ChunkPos.containing(\1)'),
    (r'\.getFrontOffsetX\(\)', '.getStepX()'),
    (r'\.getFrontOffsetY\(\)', '.getStepY()'),
    (r'\.getFrontOffsetZ\(\)', '.getStepZ()'),
    (r'\.getHorizontalIndex\(\)', '.get2DDataValue()'),
    (r'\.rotateYCCW\(\)', '.getCounterClockWise()'),
    (r'\.rotateY\(\)', '.getClockWise()'),
    (r'\.getDirectionVec\(\)', '.getUnitVec3i()'),
    (r'\bTextFormatting\.', 'ChatFormatting.'),
    # fml
    (r'\bModList\.isModLoaded\(', 'ModList.get().isLoaded('),
    (r'\bModList\.instance\(\)', 'ModList.get()'),
    (r'\bModList\.get\(\)\.activeModContainer\(\)', 'ModLoadingContext.get().getActiveContainer()'),
    # entities / players
    # (reads only: assignments to these fields need hand porting, to setLevel / setPos)
    (r'\b(player|entityPlayer|entity|placer|playerIn|thePlayer|ep|living|entityLiving|livingBase)\.world\b(?!\s*\()(?!\s*=[^=])',
     r'\1.level()'),
    (r'\b(player|entityPlayer|entity|placer|playerIn|thePlayer|ep|living)\.posX\b(?!\s*=[^=])', r'\1.getX()'),
    (r'\b(player|entityPlayer|entity|placer|playerIn|thePlayer|ep|living)\.posY\b(?!\s*=[^=])', r'\1.getY()'),
    (r'\b(player|entityPlayer|entity|placer|playerIn|thePlayer|ep|living)\.posZ\b(?!\s*=[^=])', r'\1.getZ()'),
    (r'\.openContainer\b', '.containerMenu'),
    (r'\.windowId\b', '.containerId'),
    (r'\.getServerHandler\(\)\.player\b', '.player()'),
    (r'\.capabilities\.isCreativeMode\b', '.isCreative()'),
    (r'\.getDistanceSq\(', '.distanceToSqr('),
    # buffers
    (r'\.writeString\(', '.writeUtf('),
    (r'\.readString\(', '.readUtf('),
    (r'\.writeUniqueId\(', '.writeUUID('),
    (r'\.readUniqueId\(\)', '.readUUID()'),
    (r'\.writeEnumValue\(', '.writeEnum('),
    (r'\.readEnumValue\(', '.readEnum('),
    (r'\bMth\.log2DeBruijn\(', 'Mth.ceillog2('),
    # items / vanilla containers
    (r'\.splitStack\(', '.split('),
    (r'\.getCompoundTagAt\(', '.getCompoundOrEmpty('),
    (r'\.getSizeInventory\(\)', '.getContainerSize()'),
    (r'\.setInventorySlotContents\(', '.setItem('),
    (r'\.decrStackSize\(', '.removeItem('),
    (r'\.removeStackFromSlot\(', '.removeItemNoUpdate('),
    (r'\.getInventoryStackLimit\(\)', '.getMaxStackSize()'),
    (r'\.isItemValidForSlot\(', '.canPlaceItem('),
    (r'\.isUsableByPlayer\(', '.stillValid('),
    # crash reports
    (r'\.makeCategory\(', '.addCategory('),
    (r'\.addCrashSection\(', '.setDetail('),
]


SWAPPED_CALLS = {
    # 1.12.2 name -> 26.3 name, for two-argument calls whose arguments swapped
    'Direction.getFacingFromAxis': 'Direction.fromAxisAndDirection',
}


def rewrite_swapped_calls(text):
    for old, new in SWAPPED_CALLS.items():
        out = []
        pos = 0
        pattern = re.compile(re.escape(old) + r'\(')
        while True:
            m = pattern.search(text, pos)
            if not m:
                out.append(text[pos:])
                break
            res = split_call(text, m.end() - 1)
            if res is None or len(res[1]) != 2:
                out.append(text[pos:m.end()])
                pos = m.end()
                continue
            end, args = res
            out.append(text[pos:m.start()])
            out.append('%s(%s, %s)' % (new, args[1].strip(), args[0].strip()))
            pos = end
        text = ''.join(out)
    return text


def rewrite_identifiers(text):
    """new ResourceLocation(ns, path) -> Identifier.fromNamespaceAndPath(ns, path); new ResourceLocation(s) -> Identifier.parse(s)."""
    out = []
    pos = 0
    pattern = re.compile(r'new Identifier\(')
    while True:
        m = pattern.search(text, pos)
        if not m:
            out.append(text[pos:])
            return ''.join(out)
        res = split_call(text, m.end() - 1)
        if res is None or len(res[1]) not in (1, 2) or not res[1][0].strip():
            out.append(text[pos:m.end()])
            pos = m.end()
            continue
        end, args = res
        out.append(text[pos:m.start()])
        if len(args) == 2:
            out.append('Identifier.fromNamespaceAndPath(%s, %s)' % (args[0].strip(), args[1].strip()))
        else:
            out.append('Identifier.parse(%s)' % args[0].strip())
        pos = end

# NBT getters that return Optional in 26.3: getX(key) -> getXOr(key, default). Only in files that use CompoundTag.
NBT_GETTERS = {
    'getInteger': ('getIntOr', '0'),
    'getBoolean': ('getBooleanOr', 'false'),
    'getByte': ('getByteOr', '(byte) 0'),
    'getShort': ('getShortOr', '(short) 0'),
    'getLong': ('getLongOr', '0L'),
    'getFloat': ('getFloatOr', '0F'),
    'getDouble': ('getDoubleOr', '0D'),
    'getString': ('getStringOr', '""'),
}


def split_call(text, start):
    """text[start] is '('. Returns (end index after ')', list of top-level argument strings) or None."""
    depth = 0
    args = []
    cur = start + 1
    i = start
    in_str = None
    while i < len(text):
        c = text[i]
        if in_str:
            if c == '\\':
                i += 2
                continue
            if c == in_str:
                in_str = None
        elif c in '"\'':
            in_str = c
        elif c in '([{':
            depth += 1
        elif c in ')]}':
            depth -= 1
            if depth == 0:
                args.append(text[cur:i])
                return i + 1, args
        elif c == ',' and depth == 1:
            args.append(text[cur:i])
            cur = i + 1
        i += 1
    return None


CAP_IMPORT = 'import net.minecraftforge.common.capabilities.Capability;'


def rewrite_capabilities(text):
    """Forge Capability<T> -> NeoForge BlockCapability<T, Direction> (every BuildCraft capability is a sided block one)."""
    if CAP_IMPORT not in text:
        return text
    text = text.replace(CAP_IMPORT, 'import net.neoforged.neoforge.capabilities.BlockCapability;')
    if 'import net.minecraft.core.Direction;' not in text:
        text = text.replace('import net.neoforged.neoforge.capabilities.BlockCapability;',
                            'import net.minecraft.core.Direction;\n\nimport net.neoforged.neoforge.capabilities.BlockCapability;', 1)
    # One level of nested generics is enough for BuildCraft's capability types.
    return re.sub(r'(?<![\w.])Capability<((?:[^<>]|<[^<>]*>)+)>', r'BlockCapability<\1, Direction>', text)


NBT_ARRAYS = {'getIntArray': 'new int[0]', 'getByteArray': 'new byte[0]', 'getLongArray': 'new long[0]'}


def rewrite_nbt_more(text):
    """getTagList(name, type) -> getListOrEmpty(name); getTag(name) -> get(name); getXArray(name) -> Optional.orElse(empty)."""
    out = []
    pos = 0
    pattern = re.compile(r'\.(getTagList|getTag|getIntArray|getByteArray|getLongArray|contains)\(')
    while True:
        m = pattern.search(text, pos)
        if not m:
            out.append(text[pos:])
            return ''.join(out)
        res = split_call(text, m.end() - 1)
        name = m.group(1)
        wanted = 2 if name in ('getTagList', 'contains') else 1
        if (res is None or len(res[1]) != wanted or not res[1][0].strip()
                or (name in NBT_ARRAYS and text.startswith('.orElse(', res[0]))
                or (name == 'contains' and 'TAG_' not in res[1][1])):
            out.append(text[pos:m.end()])
            pos = m.end()
            continue
        end, args = res
        out.append(text[pos:m.start()])
        if name == 'getTagList':
            out.append('.getListOrEmpty(%s)' % args[0].strip())
        elif name == 'contains':
            # 26.3 has no typed contains(); the typed getters check the type instead.
            out.append('.contains(%s)' % args[0].strip())
        elif name == 'getTag':
            out.append('.get(%s)' % args[0].strip())
        else:
            out.append('.%s(%s).orElse(%s)' % (name, args[0].strip(), NBT_ARRAYS[name]))
        pos = end


ITEM_USE_RE = re.compile(
    r'public (?:InteractionResult|EnumActionResult) (onItemUse|onItemUseFirst)\(\s*'
    r'(?:(?:ItemStack) (\w+),\s*)?'
    r'Player (\w+),\s*Level (\w+),\s*BlockPos (\w+),\s*(?:InteractionHand (\w+),\s*)?Direction (\w+),\s*'
    r'float (\w+),\s*float (\w+),\s*float (\w+)\s*(?:,\s*InteractionHand (\w+)\s*)?\)\s*\{')


def rewrite_item_use(text):
    """onItemRightClick -> use; onItemUse(player, world, pos, hand, side, hitX...) -> useOn(UseOnContext), unpacking the
    context into the 1.12.2 parameter names so the body stays the same."""
    text = re.sub(r'public ActionResult<ItemStack> onItemRightClick\(\s*Level (\w+),\s*Player (\w+),\s*InteractionHand (\w+)\s*\)',
                  r'public InteractionResult use(Level \1, Player \2, InteractionHand \3)', text)
    if 'public InteractionResult use(Level' in text:
        text = re.sub(r'return new ActionResult<>\(\s*(InteractionResult\.\w+)\s*,[^;]*\);', r'return \1;', text)
        text = re.sub(r'return ActionResult\.newResult\(\s*(InteractionResult\.\w+)\s*,[^;]*\);', r'return \1;', text)

    def repl(m):
        name = m.group(1)
        stack = m.group(2)
        player, world, pos = m.group(3), m.group(4), m.group(5)
        hand = m.group(6) or m.group(11) or 'hand'
        side, hx, hy, hz = m.group(7), m.group(8), m.group(9), m.group(10)
        if name == 'onItemUse':
            head = 'public InteractionResult useOn(UseOnContext context) {'
        else:
            head = 'public InteractionResult onItemUseFirst(ItemStack %s, UseOnContext context) {' % (stack or 'stack')
        unpack = ('\n        Player %s = context.getPlayer();'
                  '\n        Level %s = context.getLevel();'
                  '\n        BlockPos %s = context.getClickedPos();'
                  '\n        InteractionHand %s = context.getHand();'
                  '\n        Direction %s = context.getClickedFace();'
                  '\n        float %s = (float) (context.getClickLocation().x - %s.getX());'
                  '\n        float %s = (float) (context.getClickLocation().y - %s.getY());'
                  '\n        float %s = (float) (context.getClickLocation().z - %s.getZ());') % (
            player, world, pos, hand, side, hx, pos, hy, pos, hz, pos)
        return head + unpack
    new = ITEM_USE_RE.sub(repl, text)
    if new != text and 'import net.minecraft.world.item.context.UseOnContext;' not in new:
        new = new.replace('import net.minecraft.world.item.ItemStack;',
                          'import net.minecraft.world.item.ItemStack;\nimport net.minecraft.world.item.context.UseOnContext;', 1)
    return new


def rewrite_set_block_state(text):
    """setBlockState(pos, state) -> setBlockAndUpdate(pos, state); setBlockState(pos, state, flags) -> setBlock(...);
    setBlockToAir(pos) -> removeBlock(pos, false)."""
    out = []
    pos = 0
    pattern = re.compile(r'\.(setBlockState|setBlockToAir)\(')
    while True:
        m = pattern.search(text, pos)
        if not m:
            out.append(text[pos:])
            return ''.join(out)
        res = split_call(text, m.end() - 1)
        if res is None:
            out.append(text[pos:m.end()])
            pos = m.end()
            continue
        end, args = res
        args = [a.strip() for a in args]
        out.append(text[pos:m.start()])
        if m.group(1) == 'setBlockToAir' and len(args) == 1:
            out.append('.removeBlock(%s, false)' % args[0])
        elif m.group(1) == 'setBlockState' and len(args) == 2:
            out.append('.setBlockAndUpdate(%s, %s)' % tuple(args))
        elif m.group(1) == 'setBlockState' and len(args) == 3:
            out.append('.setBlock(%s, %s, %s)' % tuple(args))
        else:
            out.append(text[m.start():end])
        pos = end


def rewrite_nbt_getters(text):
    out = []
    pos = 0
    pattern = re.compile(r'\.(' + '|'.join(NBT_GETTERS) + r')\(')
    while True:
        m = pattern.search(text, pos)
        if not m:
            out.append(text[pos:])
            return ''.join(out)
        res = split_call(text, m.end() - 1)
        if res is None or len(res[1]) != 1 or not res[1][0].strip():
            out.append(text[pos:m.end()])
            pos = m.end()
            continue
        end, args = res
        new_name, default = NBT_GETTERS[m.group(1)]
        out.append(text[pos:m.start()])
        out.append('.%s(%s, %s)' % (new_name, args[0].strip(), default))
        pos = end


IMPORT_RE = re.compile(r'^import\s+(static\s+)?([\w.]+)\s*;\s*$', re.M)


def port_file(path):
    with open(path, encoding='utf-8') as f:
        text = f.read()
    original = text

    imported = set(m.group(2) for m in IMPORT_RE.finditer(text))
    simple_renames = {}
    for old, new in CLASSES.items():
        if old in imported or old in text:
            old_simple = old.rsplit('.', 1)[1]
            new_simple = new.rsplit('.', 1)[1]
            if old_simple != new_simple and old in imported:
                simple_renames[old_simple] = new_simple

    # Fully-qualified names (imports and code), longest first so nested classes win.
    for old in sorted(CLASSES, key=len, reverse=True):
        text = re.sub(r'(?<![\w.])' + re.escape(old) + r'(?![\w])', CLASSES[old], text)

    # Simple names, all at once so swaps (IInventory -> Container, Container -> AbstractContainerMenu) don't chain.
    if simple_renames:
        names = sorted(simple_renames, key=len, reverse=True)
        body_start = text.find('\n', text.rfind('\nimport ')) if '\nimport ' in text else 0
        head, body = text[:body_start], text[body_start:]
        body = re.sub(r'(?<![\w.])(' + '|'.join(map(re.escape, names)) + r')\b',
                      lambda m: simple_renames[m.group(1)], body)
        text = head + body

    for regex, repl in METHODS:
        text = re.sub(regex, repl, text)

    if 'CompoundTag' in text:
        text = rewrite_nbt_getters(text)
        text = rewrite_nbt_more(text)
    text = rewrite_capabilities(text)
    if 'onItemRightClick' in text or 'onItemUse' in text:
        text = rewrite_item_use(text)
    if '.setBlockState(' in text or '.setBlockToAir(' in text:
        text = rewrite_set_block_state(text)
    if 'Direction.getFacingFromAxis(' in text:
        text = rewrite_swapped_calls(text)
    if 'new Identifier(' in text:
        text = rewrite_identifiers(text)

    # @Cancelable events implement ICancellableEvent instead.
    text = re.sub(r'@ICancellableEvent[ \t]*\r?\n([ \t]*)((?:public |protected |private |static |abstract |final )*class [^{]*?)\s*\{',
                  lambda m: m.group(2).rstrip()
                  + (', ICancellableEvent {' if ' implements ' in m.group(2) else ' implements ICancellableEvent {'),
                  text)

    # FML's @API annotation is gone.
    text = re.sub(r'^@API\([^)]*\)\s*\n', '', text, flags=re.M)
    text = text.replace('import net.minecraftforge.fml.common.API;\n', '')

    # IStringSerializable.getName() -> StringRepresentable.getSerializedName(); keep getName() for BuildCraft's callers.
    if 'StringRepresentable' in text and 'getSerializedName' not in text:
        text = re.sub(r'(\n([ \t]*)@Override[ \t]*\r?\n[ \t]*public String getName\(\))',
                      lambda m: '\n%s@Override\n%spublic String getSerializedName() {\n%s    return getName();\n%s}\n'
                                % (m.group(2), m.group(2), m.group(2), m.group(2))
                                + '\n%spublic String getName()' % m.group(2),
                      text)

    # @OnlyIn needs Dist imported (Side itself became LogicalSide).
    dist_import = 'import net.neoforged.api.distmarker.Dist;'
    if '@OnlyIn(Dist.' in text and dist_import not in text:
        text = text.replace('import net.neoforged.api.distmarker.OnlyIn;', dist_import + '\nimport net.neoforged.api.distmarker.OnlyIn;', 1)

    # Forge's Constants.NBT became Tag's own constants.
    if 'Constants.' not in text:
        text = text.replace('import net.minecraftforge.common.util.Constants;\n', '')
    if re.search(r'(?<![\w.])Tag\.TAG_', text) and not re.search(r'^import net\.minecraft\.nbt\.(Tag|\*);', text, re.M):
        text = re.sub(r'^(package [\w.]+;\s*\n)', r'\1\nimport net.minecraft.nbt.Tag;\n', text, count=1, flags=re.M)

    # Drop duplicate imports created by the renames.
    seen = set()
    lines = []
    for line in text.split('\n'):
        m = IMPORT_RE.match(line)
        if m:
            key = line.strip()
            if key in seen:
                continue
            seen.add(key)
        lines.append(line)
    text = '\n'.join(lines)

    if text != original:
        with open(path, 'w', encoding='utf-8', newline='') as f:
            f.write(text)
        return True
    return False


def main(paths):
    changed = 0
    total = 0
    for p in paths:
        files = []
        if os.path.isdir(p):
            for root, _, names in os.walk(p):
                files += [os.path.join(root, n) for n in names if n.endswith('.java')]
        else:
            files.append(p)
        for f in files:
            total += 1
            if port_file(f):
                changed += 1
    print('%d of %d files changed' % (changed, total))


if __name__ == '__main__':
    main(sys.argv[1:])
