package buildcraft.api.core;

import javax.annotation.Nonnull;

import net.minecraft.world.InteractionResult;

/** A result with a value, like 1.12's ActionResult (which 26.3 no longer has). */
public class ActionResult<T> {
    private final InteractionResult type;
    private final T result;

    public ActionResult(@Nonnull InteractionResult type, T result) {
        this.type = type;
        this.result = result;
    }

    public static <T> ActionResult<T> newResult(@Nonnull InteractionResult type, T result) {
        return new ActionResult<>(type, result);
    }

    @Nonnull
    public InteractionResult getType() {
        return type;
    }

    public T getResult() {
        return result;
    }
}
