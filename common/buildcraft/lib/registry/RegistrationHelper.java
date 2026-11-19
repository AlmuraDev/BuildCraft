package buildcraft.lib.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegisterEvent;

import buildcraft.lib.block.BlockBCBase_Neptune;
import buildcraft.lib.item.IItemBuildCraft;
import buildcraft.lib.item.ItemBlockBC_Neptune;
import buildcraft.lib.registry.TagManager.EnumTagType;
import buildcraft.lib.tile.TileBC_Neptune;

/** Registers one BuildCraft module's blocks, items and tiles. On 26.3 blocks and items can only be created while their
 * registry is open, and need their id before they're constructed, so this takes factories rather than instances: each is
 * called during NeoForge's registration with properties that already carry the id from {@link TagManager}, and the
 * created object is handed to a setter (usually assigning a module's static field).
 * <p>
 * Objects disabled in the module's object config (see {@link RegistryConfig}) are not registered, and their setter is
 * never called. Create one helper per module, in its constructor, after its tags and config are set up. */
public final class RegistrationHelper {

    private final String modId;
    private final List<Runnable> blockTasks = new ArrayList<>();
    private final List<Runnable> itemTasks = new ArrayList<>();
    private final List<Runnable> tileTasks = new ArrayList<>();
    private RegisterEvent currentEvent;

    public RegistrationHelper(String modId, IEventBus modBus) {
        this.modId = modId;
        modBus.addListener(this::onRegister);
    }

    private void onRegister(RegisterEvent event) {
        currentEvent = event;
        try {
            if (event.getRegistryKey().equals(Registries.BLOCK)) {
                blockTasks.forEach(Runnable::run);
            } else if (event.getRegistryKey().equals(Registries.ITEM)) {
                itemTasks.forEach(Runnable::run);
            } else if (event.getRegistryKey().equals(Registries.BLOCK_ENTITY_TYPE)) {
                tileTasks.forEach(Runnable::run);
            }
        } finally {
            currentEvent = null;
        }
    }

    public String getModId() {
        return modId;
    }

    private static Identifier registryName(String tagId) {
        return Identifier.parse(TagManager.getTag(tagId, EnumTagType.REGISTRY_NAME));
    }

    /** @return The 1.12.2 translation key for the tag ("tile.&lt;locale name&gt;.name" for blocks, "item.&lt;locale
     *         name&gt;.name" for items), which BuildCraft's language files use; or null if the tag has no locale name. */
    @Nullable
    public static String translationKey(String tagId, boolean block) {
        if (!TagManager.hasTag(tagId, EnumTagType.UNLOCALIZED_NAME)) {
            return null;
        }
        return (block ? "tile." : "item.") + TagManager.getTag(tagId, EnumTagType.UNLOCALIZED_NAME) + ".name";
    }

    private static BlockBehaviour.Properties blockProperties(String tagId, Identifier name) {
        BlockBehaviour.Properties props = defaultBlockProperties().setId(ResourceKey.create(Registries.BLOCK, name));
        String key = translationKey(tagId, true);
        return key == null ? props : props.overrideDescription(key);
    }

    private static Item.Properties itemProperties(String tagId, Identifier name) {
        Item.Properties props = new Item.Properties().setId(ResourceKey.create(Registries.ITEM, name));
        String key = translationKey(tagId, false);
        return key == null ? props : props.overrideDescription(key);
    }

    // #######################
    //
    // Blocks
    //
    // #######################

    /** The properties every BuildCraft block starts with (1.12.2's BlockBCBase_Neptune defaults: hardness 5, resistance
     * 10, metal sounds). */
    public static BlockBehaviour.Properties defaultBlockProperties() {
        return BlockBCBase_Neptune.defaultProperties();
    }

    /** @param factory Creates the block from its properties (already carrying its id) and its tag id.
     * @param setter Given the block once it's been created and registered. */
    public <B extends Block> void addBlock(String tagId, BiFunction<BlockBehaviour.Properties, String, B> factory,
        Consumer<B> setter) {
        addBlock(tagId, false, factory, setter);
    }

    public <B extends Block> void addBlock(String tagId, boolean force, BiFunction<BlockBehaviour.Properties, String, B> factory,
        Consumer<B> setter) {
        Identifier name = registryName(tagId);
        if (!force && !RegistryConfig.isEnabled(modId, "blocks", name.getPath(), "tile." + tagId + ".name")) {
            return;
        }
        blockTasks.add(() -> {
            B block = factory.apply(blockProperties(tagId, name), tagId);
            currentEvent.register(Registries.BLOCK, name, () -> block);
            setter.accept(block);
        });
    }

    public <B extends BlockBCBase_Neptune> void addBlockAndItem(String tagId,
        BiFunction<BlockBehaviour.Properties, String, B> factory, Consumer<B> setter) {
        addBlockAndItem(tagId, false, factory, ItemBlockBC_Neptune::new, setter);
    }

    public <B extends BlockBCBase_Neptune> void addBlockAndItem(String tagId, boolean force,
        BiFunction<BlockBehaviour.Properties, String, B> factory, Consumer<B> setter) {
        addBlockAndItem(tagId, force, factory, ItemBlockBC_Neptune::new, setter);
    }

    public <B extends BlockBCBase_Neptune, I extends BlockItem & IItemBuildCraft> void addBlockAndItem(String tagId,
        BiFunction<BlockBehaviour.Properties, String, B> factory, BiFunction<B, Item.Properties, I> itemFactory,
        Consumer<B> setter) {
        addBlockAndItem(tagId, false, factory, itemFactory, setter);
    }

    /** Registers a block and its item. The item's tag id is "item." + the block's tag id, as on 1.12.2. */
    public <B extends BlockBCBase_Neptune, I extends BlockItem & IItemBuildCraft> void addBlockAndItem(String tagId,
        boolean force, BiFunction<BlockBehaviour.Properties, String, B> factory,
        BiFunction<B, Item.Properties, I> itemFactory, Consumer<B> setter) {
        Identifier name = registryName(tagId);
        if (!force && !RegistryConfig.isEnabled(modId, "blocks", name.getPath(), "tile." + tagId + ".name")) {
            RegistryConfig.setDisabled("items", name.getPath());
            return;
        }
        List<B> created = new ArrayList<>(1);
        blockTasks.add(() -> {
            B block = factory.apply(blockProperties(tagId, name), tagId);
            currentEvent.register(Registries.BLOCK, name, () -> block);
            created.add(block);
            setter.accept(block);
        });
        itemTasks.add(() -> {
            B block = created.get(0);
            // Block items used their block's name on 1.12.2
            Item.Properties props = new Item.Properties().setId(ResourceKey.create(Registries.ITEM, name));
            String key = translationKey(tagId, true);
            I item = itemFactory.apply(block, key == null ? props.useBlockDescriptionPrefix() : props.overrideDescription(key));
            currentEvent.register(Registries.ITEM, name, () -> item);
            CreativeTabManager.addToTab(item);
        });
    }

    // #######################
    //
    // Items
    //
    // #######################

    /** @param factory Creates the item from its properties (already carrying its id) and its tag id.
     * @param setter Given the item once it's been created and registered. */
    public <I extends Item> void addItem(String tagId, BiFunction<Item.Properties, String, I> factory, Consumer<I> setter) {
        addItem(tagId, false, factory, setter);
    }

    public <I extends Item> void addItem(String tagId, boolean force, BiFunction<Item.Properties, String, I> factory,
        Consumer<I> setter) {
        addItem(tagId, force ? null : "items", factory, setter);
    }

    /** @param category The object config category this item can be disabled in ("items", "pipes"), or null if it can't
     *            be. */
    public <I extends Item> void addItem(String tagId, @Nullable String category, BiFunction<Item.Properties, String, I> factory,
        Consumer<I> setter) {
        Identifier name = registryName(tagId);
        if (category != null && !RegistryConfig.isEnabled(modId, category, name.getPath(), tagId + ".name")) {
            return;
        }
        itemTasks.add(() -> {
            I item = factory.apply(itemProperties(tagId, name), tagId);
            if (item == null) {
                return;
            }
            currentEvent.register(Registries.ITEM, name, () -> item);
            CreativeTabManager.addToTab(item);
            setter.accept(item);
        });
    }

    // #######################
    //
    // Tiles
    //
    // #######################

    /** Registers the tile type that the given blocks create. Tiles get their type from their block (see
     * {@link TileTypes}), so each block may only create one type of tile.
     *
     * @param tagId The tile's tag id ("tile.quarry"), whose registry name becomes the tile type's id.
     * @param factory The tile's (BlockPos, BlockState) constructor.
     * @param blocks Suppliers of the blocks that create this tile (they may be null if the block was disabled). */
    @SafeVarargs
    public final <T extends TileBC_Neptune> void registerTile(String tagId, BlockEntityType.BlockEntitySupplier<T> factory,
        Supplier<? extends Block>... blocks) {
        Identifier name = registryName(tagId);
        tileTasks.add(() -> {
            List<Block> valid = new ArrayList<>();
            for (Supplier<? extends Block> supplier : blocks) {
                Block block = supplier.get();
                if (block != null) {
                    valid.add(block);
                }
            }
            if (valid.isEmpty()) {
                return;
            }
            Block[] array = valid.toArray(new Block[0]);
            BlockEntityType<T> type = new BlockEntityType<>(factory, array);
            currentEvent.register(Registries.BLOCK_ENTITY_TYPE, name, () -> type);
            TileTypes.bind(type, array);
        });
    }

    /** Lets other registries (sounds, fluids, menus...) be filled with the same deferred approach. */
    public static <T> DeferredRegister<T> createRegister(ResourceKey<? extends net.minecraft.core.Registry<T>> registry,
        String modId, IEventBus modBus) {
        DeferredRegister<T> register = DeferredRegister.create(registry, modId);
        register.register(modBus);
        return register;
    }
}
