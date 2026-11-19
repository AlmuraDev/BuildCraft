package buildcraft.api.transport.pipe;

import java.util.List;

import buildcraft.lib.client.model.MutableQuad;

/** Quads are BuildCraft lib's MutableQuads (26.3 baked quads can't be drawn from a block entity renderer). */
public interface IPipeFlowBaker<F extends PipeFlow> {
    List<MutableQuad> bake(F flow);
}
