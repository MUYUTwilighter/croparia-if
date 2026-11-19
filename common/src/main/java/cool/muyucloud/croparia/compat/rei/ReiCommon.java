package cool.muyucloud.croparia.compat.rei;

import cool.muyucloud.croparia.registry.CropariaComponents;
import cool.muyucloud.croparia.registry.CropariaItems;
import me.shedaniel.rei.api.common.entry.comparison.EntryComparator;
import me.shedaniel.rei.api.common.entry.comparison.ItemComparatorRegistry;
import me.shedaniel.rei.api.common.plugins.REICommonPlugin;

import java.util.Objects;

public class ReiCommon implements REICommonPlugin {
    @Override
    public void registerItemComparators(ItemComparatorRegistry registry) {
        // Recipe queries compare crop identity; exact comparisons still include all components.
        registry.register(EntryComparator.itemComponents().onlyExact()
                .then((_, stack) -> Objects.hashCode(stack.get(CropariaComponents.CROP_ID.get()))),
            CropariaItems.CROP_SEED.get(), CropariaItems.CROP_FRUIT.get(),
            CropariaItems.MELON_SEED.get(), CropariaItems.MELON_ITEM.get());
    }
}
