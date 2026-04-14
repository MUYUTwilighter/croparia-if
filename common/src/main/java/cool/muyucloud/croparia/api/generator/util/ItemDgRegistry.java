package cool.muyucloud.croparia.api.generator.util;

import cool.muyucloud.croparia.CropariaIf;
import cool.muyucloud.croparia.api.crop.item.CropFruit;
import cool.muyucloud.croparia.api.crop.item.CropSeed;
import cool.muyucloud.croparia.api.crop.item.MelonItem;
import cool.muyucloud.croparia.api.crop.item.MelonSeed;
import cool.muyucloud.croparia.api.element.item.ElementalBucket;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Optional;

public class ItemDgRegistry implements DgRegistry<ItemDgEntry> {
    private static boolean needsDedicatedClientItemInfo(Item item) {
        return item instanceof CropSeed
            || item instanceof CropFruit
            || item instanceof MelonSeed
            || item instanceof MelonItem
            || item instanceof ElementalBucket;
    }

    @Override
    public @NotNull Iterator<ItemDgEntry> iterator() {
        return BuiltInRegistries.ITEM
            .keySet()
            .stream()
            .filter(id -> id.getNamespace().equals(CropariaIf.MOD_ID))
            .filter(id -> !needsDedicatedClientItemInfo(BuiltInRegistries.ITEM.getValue(id)))
            .map(ItemDgEntry::new)
            .iterator();
    }

    @Override
    public Optional<ItemDgEntry> forName(Identifier id) {
        if (BuiltInRegistries.ITEM.containsKey(id)
            && id.getNamespace().equals(CropariaIf.MOD_ID)
            && !needsDedicatedClientItemInfo(BuiltInRegistries.ITEM.getValue(id))) {
            return Optional.of(new ItemDgEntry(id));
        }
        return Optional.empty();
    }
}
