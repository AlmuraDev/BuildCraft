package buildcraft.api.transport.pipe;




public interface IPipeBehaviourRenderer<B extends PipeBehaviour> {
    void render(B behaviour, float partialTicks, IPipeRenderOutput out);
}
