package buildcraft.api.transport.pipe;


public interface IPipeFlowRenderer<F extends PipeFlow> {
    void render(F flow, float partialTicks, IPipeRenderOutput out);
}
