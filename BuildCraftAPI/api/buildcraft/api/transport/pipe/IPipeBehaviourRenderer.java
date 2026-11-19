package buildcraft.api.transport.pipe;



import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface IPipeBehaviourRenderer<B extends PipeBehaviour> {
    void render(B behaviour, float partialTicks, IPipeRenderOutput out);
}
