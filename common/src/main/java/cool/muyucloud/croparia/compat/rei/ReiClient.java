package cool.muyucloud.croparia.compat.rei;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.compat.rei.category.SimpleCategory;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;

public class ReiClient implements REIClientPlugin {
    public void registerCategories(CategoryRegistry registry) {
        CropariaIf.LOGGER.debug("Registering rei recipe categories...");
        ReiCommon.forEach(proxy -> {
            SimpleCategory<?> category = proxy.getCategory().get().orElseThrow();
            registry.add(category);
            registry.addWorkstations(proxy.getId(), category.stations());
        });
    }
}
