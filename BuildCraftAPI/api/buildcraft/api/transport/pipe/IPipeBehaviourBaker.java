package buildcraft.api.transport.pipe;

import java.util.List;

import buildcraft.lib.client.model.MutableQuad;

/** Quads are BuildCraft lib's MutableQuads (26.3 baked quads can't be drawn from a block entity renderer). */
public interface IPipeBehaviourBaker<B extends PipeBehaviour> {
    List<MutableQuad> bake(B behaviour);
}
