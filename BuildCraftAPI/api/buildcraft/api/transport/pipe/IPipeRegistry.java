package buildcraft.api.transport.pipe;

import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.resources.Identifier;

public interface IPipeRegistry {
    PipeDefinition getDefinition(Identifier identifier);

    void registerPipe(PipeDefinition definition);

    /** Maps the given {@link PipeDefinition} to an {@link IItemPipe}. This acts exactly akin to
     * {@link Map#put(Object, Object)}. */
    void setItemForPipe(PipeDefinition definition, @Nullable IItemPipe item);

    IItemPipe getItemForPipe(PipeDefinition definition);

    /** Queues an {@link IItemPipe} for the given {@link PipeDefinition}: on 26.3 items can only be created while items
     * are being registered, so the item is made then (with BuildCraft's tags for its name) and given to
     * {@code onCreated}. If the {@link PipeDefinition} has been registered with {@link #registerPipe(PipeDefinition)}
     * then the item will also be registered with {@link #setItemForPipe(PipeDefinition, IItemPipe)}. */
    void createItemForPipe(PipeDefinition definition, Consumer<IItemPipe> onCreated);

    Iterable<PipeDefinition> getAllRegisteredPipes();
}
