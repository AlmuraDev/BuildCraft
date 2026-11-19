package buildcraft.api.transport.pluggable;


import com.mojang.blaze3d.vertex.BufferBuilder;

public interface IPlugDynamicRenderer<P extends PipePluggable> {
    void render(P plug, double x, double y, double z, float partialTicks, BufferBuilder bb);
}
