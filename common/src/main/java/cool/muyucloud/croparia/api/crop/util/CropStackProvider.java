package cool.muyucloud.croparia.api.crop.util;

import cool.muyucloud.croparia.registry.CropariaComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface CropStackProvider {
    @NotNull ItemStack getDefaultInstance();

    default ItemStack getCropItem(Identifier id) {
        ItemStack stack = this.getDefaultInstance();
        stack.set(CropariaComponents.CROP_ID.get(), id);
        return stack;
    }
}
