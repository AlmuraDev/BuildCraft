package buildcraft.api.transport.pluggable;

import java.util.List;

import buildcraft.lib.client.model.MutableQuad;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
/** Quads are BuildCraft lib's MutableQuads (26.3 baked quads can't be drawn from a block entity renderer). */
public interface IPluggableStaticBaker<K extends PluggableModelKey> {
    List<MutableQuad> bake(K key);
}
