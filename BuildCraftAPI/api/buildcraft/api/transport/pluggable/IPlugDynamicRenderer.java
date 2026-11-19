package buildcraft.api.transport.pluggable;

import buildcraft.api.transport.pipe.IPipeRenderOutput;


public interface IPlugDynamicRenderer<P extends PipePluggable> {
    void render(P plug, float partialTicks, IPipeRenderOutput out);
}
