package cool.muyucloud.croparia.compat.rei;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.compat.rei.category.*;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;

import java.util.ArrayList;

public class ReiClient implements REIClientPlugin {
    public static final ArrayList<ReiCategory<?>> CATEGORIES = new ArrayList<>();

    static {
        CATEGORIES.add(new ReiInfusorRecipe(ReiCommon.INFUSOR));
        CATEGORIES.add(new ReiRitualRecipe(ReiCommon.RITUAL));
        CATEGORIES.add(new ReiRitualStructure(ReiCommon.RITUAL_STRUCTURE));
        CATEGORIES.add(new ReiSoakRecipe(ReiCommon.SOAK));
    }

    public void registerCategories(CategoryRegistry registry) {
        CropariaIf.LOGGER.debug("Registering rei recipe categories...");
        CATEGORIES.forEach(category -> {
            registry.add(category);
            registry.addWorkstations(category.getCategoryIdentifier(), category.stations());
        });
    }
}
