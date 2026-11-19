package buildcraft.api.core;

/** The render layers BuildCraft's models are split into. Shared (server-safe) code uses this rather than Minecraft's
 * client-only layer class; buildcraft lib maps it to the client's layers when rendering. */
public enum BCRenderLayer {
    SOLID,
    CUTOUT_MIPPED,
    CUTOUT,
    TRANSLUCENT;

    public static final BCRenderLayer[] VALUES = values();
}
