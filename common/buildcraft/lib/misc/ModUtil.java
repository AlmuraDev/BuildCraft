package buildcraft.lib.misc;

import javax.annotation.Nullable;

import net.neoforged.fml.ModList;
import net.neoforged.fml.ModContainer;

public class ModUtil {

    @Nullable
    public static String getNameOfMod(String domain) {
        return ModList.get().getModContainerById(domain).map(mod -> mod.getModInfo().getDisplayName()).orElse(null);
    }
}
